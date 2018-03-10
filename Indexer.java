package com.ir.project;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Indexer 
{
	private Directory m_indexDir;
	private IndexWriter m_writer;
	private Analyzer m_defaultanalyzer;
	protected NgramAnalyzer m_ngramanalyzer;
	private StandardAnalyzer m_standardanalyzer;
	private Analyzer m_analyzer;
	private Map<String,Analyzer> m_aMap;

	static public String  NameNgPrefix = "_";
	
	String m_fields[];
	int m_icol_id;
	public static String m_field_id = "id";
	int m_icol_tweet;
	public static String m_field_tweet = "tweet";
	public static String m_field_generic = "content";

	private Properties m_properties;
	static public String ngrams_field( String s) { return s + "_ngrams"; }

	public Indexer(Properties properties) throws IOException
	{
		m_indexDir = null;
		m_fields = null;
		m_writer = null;
		m_properties = properties;

		m_standardanalyzer = new StandardAnalyzer( );
		m_defaultanalyzer = m_standardanalyzer;
		m_ngramanalyzer = new NgramAnalyzer();
		
		List<String> stopWords = null;
		BufferedReader reader = new BufferedReader(new FileReader(properties.getProperty("stop-words")));
		StringBuffer stringBuffer = new StringBuffer();
		String line;
		while ((line = reader.readLine()) != null) {
			stringBuffer.append(line);
			stringBuffer.append(",");
		}
		reader.close();
		stopWords = Arrays.asList(stringBuffer.toString());

		final CharArraySet stopSet = new CharArraySet(stopWords, false);

		m_aMap = new HashMap<String,Analyzer>();
		m_aMap.put(m_field_id, new StandardAnalyzer( ));
		m_aMap.put(m_field_tweet, new StandardAnalyzer( stopSet));
		m_aMap.put(m_field_generic, new StandardAnalyzer( stopSet));
		m_aMap.put( ngrams_field( m_field_tweet), new NgramAnalyzer( stopSet));
		m_analyzer = new PerFieldAnalyzerWrapper(m_defaultanalyzer, m_aMap );
	}

	public void PrepIndex() throws Exception
	{

		File indexFile = new File( m_properties.getProperty("index-path") + ".index");

		m_indexDir = FSDirectory.open(indexFile.toPath());

		IndexWriterConfig writerConfig = new IndexWriterConfig( m_analyzer);

		OpenMode mode = OpenMode.CREATE;
		if (m_properties.containsKey("update"))
		{
			if (Boolean.parseBoolean(m_properties.getProperty("update")) == true)
				mode = OpenMode.CREATE_OR_APPEND;
		}
		writerConfig.setOpenMode(mode);

		// initialize the IndexWriter
		m_writer = new IndexWriter(m_indexDir, writerConfig);
		m_writer.commit(); // make the index exist - helps link related indexes early-on (i.e. the listing-index-builder's reader on the label-index) 
	}

	public void IndexDirExt(String dirExtName) throws IOException
	{
		System.err.println("indexing directory " + dirExtName);

		File dirExtFile = new File(dirExtName);
		String extName = dirExtFile.getName();
		String dirName = dirExtFile.getParent();
		File dirFile = new File(dirName);
		
		String[] dirFiles = dirFile.list();
		Arrays.sort(dirFiles);

		long msecsStart = System.currentTimeMillis();
		int numDocs=0, numFiles=0;
		for (String fileName: dirFiles)
		{
			if(!fileName.endsWith(extName)) continue;
			String filePath = new File(dirFile, fileName).toString();
			numFiles += 1;
			numDocs += IndexFile(filePath);
		}

		long msecsTaken = System.currentTimeMillis()-msecsStart;
		System.err.printf("indexing done; directory %s; %d files; %d records; %d msecs\n",
				dirExtName, numFiles, numDocs, msecsTaken);
	}

	public int IndexFile(String fileName) throws IOException
	{
		System.err.printf("indexing file %s\n", fileName);

		BufferedReader reader = new BufferedReader(new FileReader(fileName));

		int lineNumber = 0;
		int numDocs = 0;
		while (reader.ready())
		{
			// read the next line and split the columns
			String line = reader.readLine();
			String[] fields = line.split("\t");

			// load the column headers from the first line
			// other lines contain column data.
			if (lineNumber == 0)
				LoadFieldNames(fields);
			else {
				// make this line into a Document
				IndexDocument(fields);
				numDocs += 1;
			}
			// move to the next line
			++lineNumber;
		}
		reader.close();
		
		// flush the index after each file.
		m_writer.commit();
		return numDocs;
	}

	private void IndexDocument(String[] fields) throws IOException
	{
		
		// create the document
		Document doc = new Document();
		doc.add(new TextField( m_field_id, fields[m_icol_id], Field.Store.YES));
		doc.add(new TextField( m_field_tweet, fields[m_icol_tweet], Field.Store.YES));

		if(true) {
			String fldName = ngrams_field( m_field_tweet), fldValue = fields[m_icol_tweet];
			Analyzer analyzer = m_aMap.get( fldName);
			TokenStream tokenStream = analyzer.tokenStream( fldName, new StringReader( fldValue));
			doc.add( new TextField( fldName, tokenStream));
		}

		String content = fields[m_icol_id] + "  " + fields[m_icol_tweet] ;
		doc.add( new TextField( m_field_generic, content, Field.Store.YES));

		// store the Document in the index
		m_writer.addDocument(doc);
	}

	private void LoadFieldNames(String[] fields)
	{
		m_fields = fields;
		List<String> fieldList = Arrays.asList(fields);

		m_icol_id = fieldList.indexOf("id"); 
		m_icol_tweet = fieldList.indexOf("tweet");
		
	}

	public static void main(String[] args) throws Exception
	{
		// read the configuration options
		Config config = new Config( System.getProperties(), System.err);
		config.loadConfigFile(args);
		config.parseArgs(args);

		Indexer indexer = new Indexer( config);
		indexer.PrepIndex();
		indexer.IndexDirExt( config.getProperty("data-path")); 
	}

	public Analyzer analyzer() {
		return m_analyzer;
	}
}