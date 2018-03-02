package com.ir.project;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeSource;

public class AnalyzerUtils
{
	public static final String DEFAULT_FIELD_NAME = "contents";
	public static final String SHINGLE_TOKEN_TYPE = "shingle";
	public static final boolean DEBUG = false;

	public static final int MAX_PERMUTATIONS = 10;
	
    //
    // token-attribute get-methods
    //
    
	/**
	 * helper method to get the offsets-attribute of a token.
	 * 
	 * @param source the token from which to get the offsets
	 * @return the {startOffset, endOffset} of the token
	 */
}