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
	public static int[] getOffsets(AttributeSource source)
	{
    	// debug
    	if (source == null)
    		throw new NullPointerException("cannot get attribute from a null token");
    	
    	OffsetAttribute attr = (OffsetAttribute) source.addAttribute(OffsetAttribute.class);
		return new int[] {attr.startOffset(), attr.endOffset()};
	}

	/**
	 * helper method to get the position-increment attribute of a token.
	 * 
	 * @param source the token from which to get the offsets
	 * @return the position increment of the token
	 */
    public static int getPositionIncrement(AttributeSource source)
    {
    	// debug
    	if (source == null)
    		throw new NullPointerException("cannot get attribute from a null token");
    	
        PositionIncrementAttribute attr = (PositionIncrementAttribute) source.addAttribute(PositionIncrementAttribute.class);
        return attr.getPositionIncrement();
    }
    
	/**
	 * helper method to get the position-length attribute of a token.
	 * 
	 * @param source the token from which to get the offsets
	 * @return the position length of the token
	 */
    public static int getPositionLength(AttributeSource source)
    {
    	// debug
    	if (source == null)
    		throw new NullPointerException("cannot get attribute from a null token");
    	
        PositionLengthAttribute attr = (PositionLengthAttribute) source.addAttribute(PositionLengthAttribute.class);
        return attr.getPositionLength();
    }

	/**
	 * helper method to get the term-attribute of a token.
	 * 
	 * @param source the token from which to get the offsets
	 * @return the term-string of the token
	 */
    public static String getTerm(AttributeSource source)
    {
    	// debug
    	if (source == null)
    		throw new NullPointerException("cannot get attribute from a null token");
    	
    	CharTermAttribute attr = (CharTermAttribute) source.addAttribute(CharTermAttribute.class);
        return attr.toString();
    }

    
	/**
	 * helper method to get the type-attribute of a token.
	 * 
	 * @param source the token from which to get the offsets
	 * @return the term-type of the token
	 */
    public static String getType(AttributeSource source)
    {
    	// debug
    	if (source == null)
    		throw new NullPointerException("cannot get attribute from a null token");
    	
        TypeAttribute attr = (TypeAttribute) source.addAttribute(TypeAttribute.class);
        return attr.type();
    }
    
    //
    // token-attribute set-methods
    //
    
    public static void setOffset(AttributeSource source, int startOffset, int endOffset)
    {
    	// debug
    	if (source == null)
    		throw new NullPointerException("cannot set attribute on a null token");
    	if (startOffset < 0)
    		throw new IllegalArgumentException(String.format("startOffset %d cannot be negative", startOffset));
    	if (endOffset < 0)
    		throw new IllegalArgumentException(String.format("endOffset %d cannot be negative", endOffset));
    	if (startOffset > endOffset)
    		throw new IllegalArgumentException(String.format("startOffset % must not be greater than endOffset %d", startOffset, endOffset));
    	
    	OffsetAttribute attr = (OffsetAttribute)source.addAttribute(OffsetAttribute.class);
    	attr.setOffset(startOffset, endOffset);
    }

    public static void setPositionIncrement(AttributeSource source, int posIncr)
    {
    	// debug
    	if (source == null)
    		throw new NullPointerException("cannot set attribute on a null token");
    	if (posIncr < 0)
    		throw new IllegalArgumentException("position increment %d cannot be negative");
    	
        PositionIncrementAttribute attr = (PositionIncrementAttribute) source.addAttribute(PositionIncrementAttribute.class);
        attr.setPositionIncrement(posIncr);
    }

    public static void setPositionLength(AttributeSource source, int posIncr)
    {
    	// debug
    	if (source == null)
    		throw new NullPointerException("cannot set attribute on a null token");
    	if (posIncr < 0)
    		throw new IllegalArgumentException("position length %d cannot be negative");
    	
    	PositionLengthAttribute attr = (PositionLengthAttribute) source.addAttribute(PositionLengthAttribute.class);
        attr.setPositionLength(posIncr);
    }

    public static void setTerm(AttributeSource source, String term)
    {
    	// debug
    	if (source == null)
    		throw new NullPointerException("cannot set attribute on a null token");
    	
    	CharTermAttribute attr = (CharTermAttribute) source.addAttribute(CharTermAttribute.class);
    	attr.setEmpty();
        attr.append(term);
    }

    public static void setType(AttributeSource source, String type)
    {
    	// debug
    	if (source == null)
    		throw new NullPointerException("cannot set attribute on a null token");
    	
        TypeAttribute attr = (TypeAttribute) source.addAttribute(TypeAttribute.class);
        if (type == null || type.length() == 0)
            type = "<null>"; 

        attr.setType(type);
    }
    
    //
    // token-list methods (String -> List<AttributeSource>)
    //
    
    public static List<AttributeSource> analyzedTokens(String text, Analyzer analyzer) throws IOException
    {
    	return analyzedTokens( text, analyzer, null);
    }

    public static List<AttributeSource> analyzedTokens(String text, Analyzer analyzer, String fieldName) throws IOException
    {
    	// ensure there is text to process
    	if (text == null)
    		throw new NullPointerException("AnalyzerUtils.analyzedTokens cannot work with a null text");
		// ensure there is an Analyzer with which to process
		if (analyzer == null)
			throw new NullPointerException("AnalyzerUtils.analyzedTokens cannot work with a null Analyzer");
		// if there is no field-name, use the default
		if (fieldName == null)
			fieldName = DEFAULT_FIELD_NAME;
		
		StringReader reader = new StringReader(text);
		TokenStream stream = analyzer.tokenStream(fieldName, reader);
    	List<AttributeSource> tokens = analyzedTokens(stream);
    	stream.close();
    	return tokens;
    }
    
    public static List<AttributeSource> analyzedTokens(TokenStream stream) throws IOException
    {
    	// ensure there is a stream to process
    	if (stream == null)
    		throw new NullPointerException("AnalyzerUtils.analyzedTokens cannot work with a null TokenStream");
		
    	List<AttributeSource> tokens = new ArrayList<AttributeSource>();
        
    	stream.reset();
        while (stream.incrementToken())
        	tokens.add(stream.cloneAttributes());
    	stream.end();
    	
    	return tokens;
    }
    
    //
    // term-list methods (String -> List<String>)
    //
    
    public static List<String> analyzedTerms(String text, Analyzer analyzer, String fieldName, String tokenType) throws IOException
    {
    	// ensure there is text to process
    	if (text == null)
    		throw new NullPointerException("AnalyzerUtils.analyzedTerms cannot work with a null text");
		// ensure there is an Analyzer with which to process
		if (analyzer == null)
			throw new NullPointerException("AnalyzerUtils.analyzedTerms cannot work with a null Analyzer");
		// if there is no field-name, use the default
		if (fieldName == null)
			fieldName = DEFAULT_FIELD_NAME;
		
		StringReader reader = new StringReader(text);
		TokenStream stream = analyzer.tokenStream(fieldName, reader);
    	List<String> terms = analyzedTerms(stream, tokenType);
    	
    	return terms;
    }
    
    public static List<String> analyzedTerms(TokenStream stream, String tokenType) throws IOException
    {
		// ensure there is an stream to process
		if (stream == null)
			throw new NullPointerException("AnalyzerUtils.analyzedTerms cannot work with a null TokenStream");
		
		List<AttributeSource> tokens = analyzedTokens(stream);
		
		return analyzedTerms(tokens, tokenType);
    }
    
    public static List<String> analyzedTerms(List<AttributeSource> tokens, String tokenType)
    {
    	// ensure there are tokens to process
    	if (tokens == null)
    		throw new NullPointerException("AnalyzerUtils.analyzedTerms cannot work with null tokens");
    	
    	List<String> terms = new ArrayList<String>(tokens.size());
    	
    	// TODO: fold this in
    	if (tokenType == null)
    	{
    		for (AttributeSource token: tokens)
    			terms.add(getTerm(token));
    	}
    	else
    	{
    		for (AttributeSource token: tokens)
    		{
    			if (getType(token).equals(tokenType))
    				terms.add(getTerm(token));
    		}
    	}
    	
    	return terms;
    }
    
    //
    // term-string methods (String -> String)
    //
    
    public static String analyzedString(String text, Analyzer analyzer, String fieldName) throws IOException
    {
    	// ensure there is text to process
    	if (text == null)
    		throw new NullPointerException("AnalyzerUtils.analyzedString cannot work with a null text");
		// ensure there is an Analyzer with which to process
		if (analyzer == null)
			throw new NullPointerException("AnalyzerUtils.analyzedString cannot work with a null Analyzer");
		// if there is no field-name, use the default
		if (fieldName == null)
			fieldName = DEFAULT_FIELD_NAME;
		
		StringReader reader = new StringReader(text);
		TokenStream stream = analyzer.tokenStream(fieldName, reader);
		String analyzedString = analyzedString(stream);
    	
    	return analyzedString;
    }
    
    public static String analyzedString(TokenStream stream) throws IOException
    {
		// ensure there is a stream to process
		if (stream == null)
			throw new NullPointerException("AnalyzerUtils.analyzedString cannot work with a null TokenStream");
		
		// get the tokens from the stream
    	List<AttributeSource> tokens = analyzedTokens(stream);
    	
    	// prepare to build a string from the term-part of these tokens.
    	StringBuilder builder = new StringBuilder();
    	int i = 0;
    	
    	for (AttributeSource token: tokens)
    	{
    		// don't include shingle-tokens
    		String type = getType(token);
    		if (type.equals(SHINGLE_TOKEN_TYPE))
    			continue;
   
    		// tokens that are not the first are preceded by a space
    		if (i > 0)
    			builder.append(' ');
    			
    		// add the token
    		String term = getTerm(token);
    		builder.append(term);
    			
    		// next token
    		++i;
    	}
    	
    	// make all the parts into a single String
    	return builder.toString();
    }
    
    //
    // permutations methods (String -> List<String>)
    //

    public static List<String> permutationStrings(String text, Analyzer analyzer, String fieldName) throws IOException
    {
    	// ensure there is text to process
    	if (text == null)
    		throw new NullPointerException("AnalyzerUtils.analyzedString cannot work with a null text");
		// ensure there is an Analyzer with which to process
		if (analyzer == null)
			throw new NullPointerException("AnalyzerUtils.analyzedString cannot work with a null Analyzer");
		// if there is no field-name, use the default
		if (fieldName == null)
			fieldName = DEFAULT_FIELD_NAME;
		
		if (DEBUG)
			System.err.println("text: " + text);
		
		StringReader reader = new StringReader(text);
		TokenStream stream = analyzer.tokenStream(fieldName, reader);
		
		return permutationStrings(stream);
    }
    
    public static List<String> permutationStrings(TokenStream stream) throws IOException
    {
		// ensure there is a stream to process
		if (stream == null)
			throw new NullPointerException("AnalyzerUtils.analyzedString cannot work with a null TokenStream");

		// get the tokens from the stream
    	List<AttributeSource> tokens = analyzedTokens(stream);
		
    	return permutationStrings(tokens);
    }
    
    public static List<String> permutationStrings(List<AttributeSource> tokens) throws IOException
    {
    	// generate all the permutations with all the tokens at every position
		List<List<String>> allPosTokens = permutationPos(tokens);
		
		if (DEBUG)
			System.err.println("permutations possible: " + permutationCount(allPosTokens));
		
		List<String> permutations = permutationRec(allPosTokens, ".", 0, MAX_PERMUTATIONS);

		if (DEBUG)
			System.err.println("permutations actually: " + permutations.size());
		
		return permutations;
    }
    
    //
    // permutation helper methods - private
    //
	    
    /** 
     * helper to generate all positioned strings from a list of tokens
     */
	private static List<List<String>> permutationPos(List<AttributeSource> tokens)
	{

		List<List<String>> allPosTokens = new ArrayList<List<String>>();
		List<String> currentPosTokens = null;
		int currentPosition = 0;
		
		CharTermAttribute termAttr;
		PositionIncrementAttribute posIncrAttr;
		
		for (AttributeSource token: tokens)
		{
			// get the relevant attributes from this token
			termAttr = token.getAttribute(CharTermAttribute.class);
			posIncrAttr = token.getAttribute(PositionIncrementAttribute.class);
			
			// new position in this token
			// store all previous tokens at the previous position
			if (posIncrAttr.getPositionIncrement() != 0)
			{
				if (currentPosTokens != null)
					allPosTokens.add(currentPosTokens);
				currentPosTokens = new ArrayList<String>();
				currentPosition += posIncrAttr.getPositionIncrement();
			}
			
			// store this token at the current position
			currentPosTokens.add(termAttr.toString());
		}
		
		// add the tokens at the final position
		if (currentPosTokens != null)
			allPosTokens.add(currentPosTokens);
		
		return allPosTokens;
	}
	
	/** 
	 * helper to generate all the permutations of positioned strings
	 * recursive, so private. 
	 */
	private static List<String> permutationRec(List<List<String>> allPosTokens, String joinBy, int pos, int maxPermutations)
	{
		List<String> posTokens = allPosTokens.get(pos);
		if (pos >= allPosTokens.size() - 1)
			return posTokens;
		
		List<String> nextTokens;
		if (maxPermutations > 0)
			nextTokens = permutationRec(allPosTokens, joinBy, pos+1, maxPermutations / posTokens.size());
		else
			nextTokens = permutationFlat(allPosTokens, joinBy, pos+1, maxPermutations / posTokens.size());
		
		List<String> allTokens = new ArrayList<String>(posTokens.size() * nextTokens.size());
		
		for (String posToken: posTokens)
		{
			for (String nextToken: nextTokens)
				allTokens.add(posToken + joinBy + nextToken);
		}
		
		return allTokens;
	}
	
	// too many combinations 
	// text: Then I'll Be Free To Travel Home-Part 2 The Battle To Save the NY African Burial Ground Institutional Use University College
	// token "home-part" has no prons!
	// permutations: 914457600
	private static List<String> permutationFlat(List<List<String>> allPosTokens, String joinBy, int pos, int maxPermutations)
	{
		List<String> posTokens = allPosTokens.get(pos);
		if (pos >= allPosTokens.size() - 1)
			return posTokens;
		
		List<String> nextTokens = permutationFlat(allPosTokens, joinBy, pos+1, 0);
		List<String> allTokens = new ArrayList<String>(1);
		allTokens.add(posTokens.get(0) + joinBy + nextTokens.get(0));
		
		return allTokens;
		
	}
    
	private static int permutationCount(List<List<String>> allPosTokens)
	{
		int count = 1;
		for (List<String> tokenList: allPosTokens)
			count *= tokenList.size();
		return count;
	}
    
    //
    // term-details methods
    //
    
    // TODO implement this
    public static List<String> analyzedDetails(String text, Analyzer analyzer, String fieldName)
    {
    	throw new UnsupportedOperationException();
    }
    
    // TODO implement this
    public static List<String> analyzedDetails(TokenStream stream)
    {
    	throw new UnsupportedOperationException();
    }
    
    public static List<String> analyzedDetails(List<AttributeSource> tokens)
    {
    	List<String> tokensDetailed = new ArrayList<String>(tokens.size());
    	
    	for (AttributeSource token: tokens)
    	{
    		// get the required token-attributes
    		String term = getTerm(token);
    		String type = getType(token);
    		int[] offsets = getOffsets(token);
    		int length = getPositionLength(token);
    		int increment= getPositionIncrement(token);
    		
    		tokensDetailed.add(String.format("(%d:%d)[%s:%d->%d:%s]", increment, length, term, offsets[0], offsets[1], type));
    	}
    	
    	return tokensDetailed;
    }
    
    //
    // display methods
    //

    public static void displayTokensWithFullDetails(Analyzer analyzer, String text) 
        throws IOException
    {
    	List<AttributeSource> tokens = analyzedTokens(text, analyzer, DEFAULT_FIELD_NAME);

        int position = 0;

        for (int i = 0; i < tokens.size(); i++)
        {
        	// get token
            AttributeSource token = tokens.get(i);

            // get token attributes
            CharTermAttribute term = (CharTermAttribute) token.addAttribute(CharTermAttribute.class);
            PositionIncrementAttribute posIncr = (PositionIncrementAttribute) token.addAttribute(PositionIncrementAttribute.class);
            OffsetAttribute offset = (OffsetAttribute) token.addAttribute(OffsetAttribute.class);
            TypeAttribute type = (TypeAttribute) token.addAttribute(TypeAttribute.class);
            int increment = posIncr.getPositionIncrement();

            // when increment > 0, start a new line
            if (increment > 0)
            {
                position = position + increment;
                System.out.println();
                System.out.print(position + ": ");
            }

            // print the token
            System.out.print("[" + term.toString() + ":" + offset.startOffset() + "->" + offset.endOffset() + ":" + type.type() + "] ");
        }
        
        System.out.println();
    }
    
    // TODO: pass in stream
    public static void displayTokenGraph(Analyzer analyzer, String text) throws IOException
    {
    	List<AttributeSource> tokens = analyzedTokens(text, analyzer, DEFAULT_FIELD_NAME);
        
        CharTermAttribute          termAttr;
        TypeAttribute              typeAttr;
        OffsetAttribute            offsetAttr;
        PositionIncrementAttribute posIncAttr;
        PositionLengthAttribute    posLenAttr;
        
        for (int i = 0; i < tokens.size(); ++i)
        {
        	AttributeSource token = tokens.get(i);
        	
        	termAttr   = (CharTermAttribute)          token.addAttribute(CharTermAttribute.class);
        	typeAttr   = (TypeAttribute)              token.addAttribute(TypeAttribute.class);
        	offsetAttr = (OffsetAttribute)            token.addAttribute(OffsetAttribute.class);
        	posIncAttr = (PositionIncrementAttribute) token.addAttribute(PositionIncrementAttribute.class);
        	posLenAttr = (PositionLengthAttribute)    token.addAttribute(PositionLengthAttribute.class);
        	
        	System.out.printf("%d: \n", i);
        	System.out.println("term\t" + termAttr.toString());
        	System.out.println("type\t" + typeAttr.type());
        	System.out.println("offs\t" + offsetAttr.startOffset() + " - " + offsetAttr.endOffset());
        	System.out.println("posi\t" + posIncAttr.getPositionIncrement());
        	System.out.println("posl\t" + posLenAttr.getPositionLength());
        	System.out.println();
        }
    }
    
}
