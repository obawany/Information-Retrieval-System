package com.ir.project;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.icu.ICUFoldingFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;

public final class NgramAnalyzer extends StopwordAnalyzerBase {

	private CharArraySet m_stopwords;
	
	/** Builds an analyzer with the given stop words.
	   * @param stopWords stop words */
	  public NgramAnalyzer() {
	    super(StandardAnalyzer.ENGLISH_STOP_WORDS_SET);
	  }

	  /** Builds an analyzer with the given stop words.
	   * @param stopWords stop words */
	  public NgramAnalyzer(CharArraySet stopWords) {
	    super(stopWords);
	  }

	  @Override
	  protected TokenStreamComponents createComponents(final String fieldName) {
	    final StandardTokenizer src = new StandardTokenizer();
	    TokenStream tok = new StandardFilter(src);
	    tok = new LowerCaseFilter(tok);
	    //tok = new ICUFoldingFilter( tok);
	    tok = new StopFilter(tok, stopwords);
	    tok = new NGramTokenFilter( tok, 2, 3);
	    return new TokenStreamComponents(src, tok) {
	      @Override
	      protected void setReader(final Reader reader) {
	        // src.setMaxTokenLength(StandardAnalyzer.this.maxTokenLength);
	        super.setReader(reader);
	      }
	    };
	  }

	  @Override
	  protected TokenStream normalize(String fieldName, TokenStream in) {
	    TokenStream result = new StandardFilter(in);
	    result = new LowerCaseFilter(result);
	    result = new ICUFoldingFilter( result);
	    result = new StopFilter( result, m_stopwords);
	    result = new NGramTokenFilter( result, 2, 3);
	    return result;
	  }
	  
	   public static void main(String[] args)
	    {
			Config config = new Config(Config.defaultsSearch(), System.out);
			config.loadConfigFile(args);
			config.parseArgs(args);

			List<String> stopWords = Arrays.asList("ave","road");
			CharArraySet stopSet = new CharArraySet(stopWords, true);
			Analyzer analyzer = new NgramAnalyzer( stopSet);

			// prepare to read interactive input
			BufferedReader in = null;
			try
			{
				in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
			
				while (true)
				{
					// prompt the user for input
					System.out.print("Enter query: ");
					String line = in.readLine();
					line = line.trim();
					System.out.println();
					
					AnalyzerUtils.displayTokenGraph(analyzer, line);
				}
			}
	    	catch (Exception e)
	    	{
	    		throw new RuntimeException(e);
	    	}
	    }
	}

