package com.ir.project;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Workflow {

	private Searcher m_tweetSearch;
	private String runName;
	
	public Workflow( Config config)
	{
		try 
		{
			m_tweetSearch = new Searcher( config);
			runName = (config.getProperty("runName"));
		} 
		catch( Exception e)
		{
			e.printStackTrace( System.out);
			m_tweetSearch = null;
			runName = "Defaultrun";
		}
	}

	
	public void run_tweets_tsv( String tsvIn, String tsvOut) throws Exception
	{
		String html = null;
		File tsvout = new File(tsvOut);
		BufferedReader reader = new BufferedReader(new FileReader(tsvIn));
		FileWriter nf = new FileWriter(tsvout); 
		BufferedWriter writer = new BufferedWriter(nf);
		writer.write("topic_id\tQ0\tdocno\trank\tscore\ttag");
		 
		StringBuffer stringBuffer = new StringBuffer();
		String line;
		while ((line = reader.readLine()) != null) { // should protect this from trailing whitespace
			stringBuffer.delete(0, stringBuffer.length());
			stringBuffer.append(line);
			html = stringBuffer.toString();
			while(!html.contains("</top>")){
				line = reader.readLine();
				stringBuffer.append(line);
				html = stringBuffer.toString();
			}
			Document doc =  Jsoup.parse(html);
			String title = doc.body().getElementsByTag("title").text();
			String num = doc.body().getElementsByTag("num").text();
			if(num.contains("MB")){
				num = num.substring(11);
				if(num.startsWith("0")){
					num = num.substring(1);
				}
			}
			
			JSONObject results = m_tweetSearch.search_for_tweet(title);
			JSONArray hits = (JSONArray)results.get("hits");
			
			for(int i = 0 ; i < hits.length(); i++){
				JSONObject hit = (JSONObject) hits.get(i);
				String score = hit.getString("score");
				JSONObject docID = (JSONObject) hit.get("doc");
				String docIDString = docID.getString("id");
				int rankNo = i +1;
				writer.write("\n"+ num + "\tQ0\t" + docIDString + "\t" + rankNo  +"\t" + score +"\t" + runName);
				writer.flush();
				nf.flush();
			}
		}

		reader.close();
		writer.close();
		nf.close();
		
	}
	

	
	public static void main(String[] args) throws Exception 
	{
		Config config = new Config( System.getProperties(), System.err);
		config.loadConfigFile( args);
		config.parseArgs( args);
		System.out.printf("%s\n", config.toString());

		Workflow workflow = new Workflow( config);
		String icsvName = config.getProperty("icsv");
		String ocsvName = config.getProperty("ocsv");
		workflow.run_tweets_tsv( icsvName, ocsvName);
	}
}