package com.ir.project;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import org.apache.wink.json4j.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.util.AttributeSource;

public class Searcher
{
	private IndexSearcher m_searcher;
	protected Analyzer m_analyzer;
	private LciEditDistance m_ruler;
	private Indexer m_tweetIndexer;
	private final int maxResults =1000;
	private int m_maxToRescore;
	private int m_maxToReturn;

	public static class MyScoreDoc extends ScoreDoc
	{
		public float myScore;
		public int formerRank;
		public MyScoreDoc(int doc, float score, int shardIndex) {
			super(doc, score, shardIndex);
			myScore = score;
		}
	}

	public static void main(String[] args) throws Exception
	{
		Config config = new Config( System.getProperties(), System.err);
		config.loadConfigFile( args);
		config.parseArgs(args);

		Searcher searcher = new Searcher(config);
		BufferedReader in = null;
		String queriesFn = null;
		in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));

		while (true) {
			if (queriesFn == null) {                        // prompt the user
				System.out.println("Enter query: ");
			} 

			String line = in.readLine();

			if (line == null || line.length() == -1) {
				break;
			}

			line = line.trim();
			if (line.length() == 0) {
				break;
			}
			JSONObject out = searcher.searchTweets_console( line);
			System.out.printf("%s\n", out.toString(2));
		}
		return;
	}

	public Searcher(Properties properties) throws IOException
	{
		// load the parameters from the config
		String indexDir = properties.getProperty("index-path");

		// prepare the index-directory for reading
		File indexFile = new File(indexDir + ".index");

		// ensure the index-directory is readable
		if (indexFile.canRead() == false)
			throw new IllegalArgumentException("index directory \"" + indexDir + "\" cannot be read");

		// open the index-directory for searching
		String dirReader = properties.getProperty("directory-reader");
		
		Directory directory = FSDirectory.open( indexFile.toPath());
		
		IndexReader reader;
		if(properties.contains(dirReader)){
			Directory ramdir = new RAMDirectory(  (FSDirectory) directory, IOContext.READ);
			reader = DirectoryReader.open( ramdir);
		} else {
			reader = DirectoryReader.open(directory);
		}
		m_searcher = new IndexSearcher(reader);
		m_tweetIndexer = new Indexer( properties);
		m_analyzer = m_tweetIndexer.analyzer();
		m_ruler = new LciEditDistance( );
		m_maxToRescore = Integer.parseInt( properties.getProperty("num_tweets_to_rescore", "1000"));
		m_maxToReturn =  Integer.parseInt(properties.getProperty("num_tweets_to_return","1000"));
	}
	
	
	public Query buildTweetQuery(String tweet) throws Exception
	{

		try
		{
			Query tweetQuery = null;
			List<AttributeSource> tokens = AnalyzerUtils.analyzedTokens( tweet, m_tweetIndexer.m_ngramanalyzer, Indexer.ngrams_field(Indexer.m_field_tweet));
			BooleanQuery.Builder q = new BooleanQuery.Builder();

			for( AttributeSource attr: tokens) {
				Term t = new Term(Indexer.ngrams_field(Indexer.m_field_tweet), attr.getAttribute(CharTermAttribute.class).toString());
				Query tq = new TermQuery( t);
				q.add(new BooleanClause(tq, BooleanClause.Occur.SHOULD));
	
			}
			tweetQuery = q.build();
			return tweetQuery;
			
		}
		catch(Exception e) 
		{
			throw e;
		}
	}

	public JSONObject searchTweets_console(String line) throws Exception
	{
		String lineargs[] = line.split("\\s*,\\s*");
		String tweet = null ;

		if( lineargs.length >= 1) tweet = lineargs[0];
		return search_for_tweet(tweet);
	}

	
	public JSONObject search_for_tweet(String tweetString) throws Exception
	{
		long msecsStart = System.currentTimeMillis();
		JSONObject result = new JSONObject();
		TopDocs topDocs = null;
		Query tweetQuery = null;
		
		tweetQuery = buildTweetQuery(tweetString);
		topDocs = m_searcher.search(tweetQuery, maxResults);
		result.put("query", tweetQuery.toString());
		int numHits = topDocs.scoreDocs.length;
		int totalHits = topDocs.totalHits;
		result.put("numHits", numHits);
		result.put("totalHits", totalHits);
		JSONArray json_hits = new JSONArray();
		result.put("hits", json_hits);
		
		long msecsBuildQuery = System.currentTimeMillis();
		
		List<MyScoreDoc> choices = new ArrayList<MyScoreDoc>();
		for(int i=0; i<topDocs.totalHits && i<topDocs.scoreDocs.length && i<m_maxToRescore; i++) {
			ScoreDoc sd = topDocs.scoreDocs[i];
			Document doc = m_searcher.doc( sd.doc);
			String docTweet = doc.get( Indexer.m_field_tweet);
			MyScoreDoc mySd = new MyScoreDoc(sd.doc, sd.score, sd.shardIndex );

			if( docTweet == null) {
				mySd.myScore = 0;
				mySd.formerRank = i;
			} else {
				mySd.myScore = m_ruler.getDistance( tweetString, docTweet);
				mySd.formerRank = i;
			}
			choices.add( mySd);
		}
		
		Collections.sort(choices, new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				float diff = ((MyScoreDoc)o1).myScore - ((MyScoreDoc)o2).myScore;
				if(diff == 0) return 0;
				else if(diff<0) return 1;
				else return -1;
			}
		}); 
		
		for(int i=0; i<choices.size() && i<m_maxToRescore && i<m_maxToReturn ; i++) {
			MyScoreDoc mySd = choices.get(i);
			JSONObject hit = new JSONObject();
			hit.put("score", mySd.myScore);
			hit.put("lucene_score", mySd.score);
			hit.put("lucene_rank", mySd.formerRank);
			int docId = mySd.doc;
			hit.put("docid", docId);
			Document doc = m_searcher.doc(docId);
			List<IndexableField> indexableFields = doc.getFields();
			JSONObject json_doc = new JSONObject();
			for(IndexableField indexableField: indexableFields) {
				json_doc.put( indexableField.name(), indexableField.stringValue());
				json_doc.remove("content");
			}
			hit.put("doc", json_doc);
			json_hits.put(hit);
		}
		
		long msecsEnd = System.currentTimeMillis();
		
		JSONObject timings = new JSONObject();
		timings.put("BuildTweetQuery", msecsBuildQuery-msecsStart);
		timings.put("total", msecsEnd-msecsStart);
		timings.put("name", "search_for_tweet");
		result.put("timings",timings);
		result.put("hits", json_hits);
		return result;
	
	}
}