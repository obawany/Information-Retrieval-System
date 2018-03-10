package com.ir.project;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.search.spell.StringDistance;

/* LciEditDistance means Low Cost Insertions
 * it is an asymettric edit distance where:
 * - the first arg is a reco result or typed text
 * - the second arg is DB entry text.
 * The DB entry text might be long and official, contain non-alphanumeric
 * characters.  It must be less costly to for those words/chars to be inserted.  
 */

/* 
 * TODO:  add .. implements org.apache.lucene.search.spell.StringDistance
 */

public class LciEditDistance implements StringDistance
{
    static public String version() { return "1.6"; }
    public static class EditCost {
		public int cost;
		public int incCost;
		public int bkpt_i;
		public int bkpt_j;
		public Err.ErrType errType;
		public String istr;
		public String jstr;
		
		EditCost() { }
		
		EditCost( int cost, int incCost, int bkpt_i, int bkpt_j, Err.ErrType errType, String istr, String jstr) {
		    this.cost = cost;
		    this.incCost = incCost;
		    this.bkpt_i = bkpt_i;
		    this.bkpt_j = bkpt_j;
		    this.errType = errType;
		    this.istr = istr;
		    this.jstr = jstr;
		}
		String toString1() {
		    String ret = "";
		    ret += " cost "+ cost;
		    ret += " incCost "+ incCost;
		    ret += " bkpt_i "+ bkpt_i;
		    ret += " bkpt_j "+ bkpt_j;
		    ret += " errType " + errType;
		    ret += " istr <"+istr+">";
		    ret += " jstr <"+jstr+">";
		    return ret;
		}
    }
	// TODO: convert this to a long, just a pair of ints
    // 1 int for the cost 
    // 1 int for the error type, which we will enum
   public static class Err {
	   static public enum ErrType { NONE, I, S, C, C_case, WIi, WIs, WIp, csyn, X , D}; 
	   public static int err( int cost, Err.ErrType e)
	   { return cost + e.ordinal()*16777216; }
	   
	   public static ErrType type(int bits) 
	   {  return ErrType.values()[ bits/16777216]; }

	   public static int cost( int bits) 
	   { return bits & (16777215);	}	
   }
    
    /* A result contains the distance and the backpointers
     * to get the path.  We leave it to the application to
     * call getPath() if they so choose.
     */
    public static class DistanceResult {
		public double distance;
	
		EditCost[][] m_aCost;
		int m_I, m_J;
		String m_coreInPath;
		String m_sRef;
		String m_sHyp;
		char[] m_aRef;
		char[] m_aHyp;
		static int NOWHERE = Integer.MAX_VALUE;
		
		
		void _resizeCostArray(int I, int J) {
		    if (I+1>=this.m_aCost.length || J+1>=this.m_aCost[0].length) {
			this.m_aCost = new EditCost[I+1][J+1];
			for(int i=0; i<=I; i++) 
			    for(int j=0; j<=J; j++) 
				this.m_aCost[i][j] = new EditCost();
		    }
		    m_I = I;
		    m_J = J;
		}
		
		public String getCore() {
			// if(true) return "core";
			if( m_coreInPath!=null && m_coreInPath.length()>0) 
				return m_coreInPath;
			else {
				EditCost trash[] = getPath();
				return m_coreInPath;
			}
		}
		public EditCost[] getPath() {
		    List<EditCost> path = new ArrayList<EditCost>();
		    try {
			    this._getPath( path, this.m_I, this.m_J);
			    EditCost path2[] = new EditCost[ path.size()];
			    for(int k=path.size()-1,i=0; k>=0; k--,i++) 
			    	path2[i] = path.get(k);
			    
			    if( true) {
				    /* get the m_coreInPath, i.e. strip off edge word insertions */
				    int st = 0;
				    int en = m_aHyp.length;
				    if( path.size()>0 && path.get(0).errType == Err.ErrType.WIs) 
				    	en=path.get(0).bkpt_j;
				    if( path.size()>1 && path.get(path.size()-1).errType == Err.ErrType.WIp) 
				    	st=path.get(path.size()-2).bkpt_j;
				    m_coreInPath = "";
				    for(int k=st;k<en; k++) m_coreInPath += m_aHyp[k];
			    }
			    return path2;
		    } catch( Exception e) {
		    	EditCost err = new EditCost();
		    	EditCost path2[] = new EditCost[1];
		    	path2[0] = err;
		    	return path2;
		    }
		}
		
		void _getPath( List<EditCost> path, int i, int j) throws Exception {
		    path.add( this.m_aCost[i][j]);
		    int ii = this.m_aCost[i][j].bkpt_i;
		    int jj = this.m_aCost[i][j].bkpt_j;
		    if( !(ii<i || jj<j)) {
		    	throw new Exception("backtrace error");
		    }
		    if(ii>0 || jj>0) {
			this._getPath(path, ii, jj);
		    }
		}
		
    }
    
    /* data members */
    int m_insCost;
    int m_insCostByChar[];
    int m_delCost;
    int m_delCostByChar[];
    int m_subCost;
    int m_maxCost;
    int m_caseCost;
    
    int m_insCostWordInternal;
    int m_insCostWordAtEdges;
    
    int m_csynCostDefault;
    
   class RhsMap 
    {
    	public HashMap<String,Integer> m;
    	public int minLen;
    	public int maxLen;
    	public RhsMap() { 
    			minLen = 99; 
    			maxLen = 0;
    			m = new HashMap<String,Integer>();
     	}
    	public void Add(String s2, int cost) {
    		m.put( s2, cost);
        	minLen = Math.min( minLen,  s2.length() ); 
        	maxLen = Math.max( maxLen,  s2.length() ); 
    	}
    }
 
    HashMap<String,RhsMap> m_mapToRhs;
    int m_mapToRhs_minLen;
    int m_mapToRhs_maxLen;
        
    // TODO what do I and J still mean?
    void _init( int I, int J) {
		m_insCost = 3000;  /* cost of inserting listing words */
		m_delCost = 3000;  /* cost of deleting utterance words */
		m_subCost = 4000;
		m_caseCost = 1000;
		/* todo:
		 * should be costlier to sub a rare utterance word
		 * should be costlier to del a rare utterance word
		 */
		m_insCostWordInternal = m_insCost/10;
		m_insCostWordAtEdges = m_insCost/100;
		
		m_csynCostDefault =  m_insCost/10;
		
		m_mapToRhs = null;
		if( true) {
			m_mapToRhs = new HashMap<String,RhsMap>();
			m_mapToRhs_minLen = 99;
			m_mapToRhs_maxLen = 0;
			add_to_map( " and "," & ",   m_csynCostDefault, true);
			add_to_map( "ck",   "k",     m_csynCostDefault, true);
			add_to_map( "ph",   "f",     m_csynCostDefault, true);
			add_to_map( "ph","f",        m_csynCostDefault, true);
			add_to_map( "ph","F",        m_csynCostDefault, true);
			add_to_map( "Ph","F",        m_csynCostDefault, true);
			add_to_map( "Ph","f",        m_csynCostDefault, true);
			add_to_map( "ea","ee",       m_csynCostDefault, true);
			add_to_map( ". "," ",        m_csynCostDefault, true);
			add_to_map( "mr ","mister ", m_csynCostDefault, true);
			add_to_map( "mr. ","mister ",m_csynCostDefault, true);
			add_to_map( "dr. ","doctor ",m_csynCostDefault, true);
			add_to_map( "dr ","doctor ", m_csynCostDefault, true);
			add_to_map( " two"," 2", m_csynCostDefault, true);
			add_to_map( " Two"," 2", m_csynCostDefault, true);
			add_to_map( " three"," 3", m_csynCostDefault, true);
			add_to_map( " Three"," 3", m_csynCostDefault, true);
			add_to_map( " versus "," vs ", m_csynCostDefault, true);
			add_to_map( " versus "," vs. ", m_csynCostDefault, true);
		}
		
		m_maxCost = 99999;
    }
    void add_to_map( String s1, String s2, int cost, boolean biDir)
    {
    	if( !m_mapToRhs.containsKey( s1)) 
    		m_mapToRhs.put(  s1, new RhsMap());
    	m_mapToRhs.get( s1).Add( s2, cost);
    	m_mapToRhs_minLen = Math.min( m_mapToRhs_minLen,  s1.length() ); 
    	m_mapToRhs_maxLen = Math.max( m_mapToRhs_maxLen,  s1.length() ); 
    	
    	if( biDir == true) {
	       	if( !m_mapToRhs.containsKey( s2)) 
	    		m_mapToRhs.put(  s2, new RhsMap());
	    	m_mapToRhs.get( s2).Add( s1, cost);
	    	
	    	m_mapToRhs_minLen = Math.min( m_mapToRhs_minLen,  s2.length() ); 
	    	m_mapToRhs_maxLen = Math.max( m_mapToRhs_maxLen,  s2.length() ); 

    	}
    }
    
    public LciEditDistance() {
		_init( 2, 2);
		m_insCostByChar = new int[ 65536];
		for(int i=0; i<65536; i++) m_insCostByChar[ i] = m_insCost;
		String freeIns = ":,!'";
		for(char c: freeIns.toCharArray()) m_insCostByChar[c] = m_insCost/100;

		m_delCostByChar = new int[ 65536];
		for(int i=0; i<65536; i++) m_delCostByChar[ i] = m_delCost;
		String freeDel = ":,!'";
		for(char c: freeDel.toCharArray()) m_delCostByChar[c] = m_delCost/100;

    }
    LciEditDistance( int I, int J) {
	_init( I, J);
    }
	int DeletionCost9( char refs[], int fr, int to) {
		if(to-fr > 1) {
		    // return new Err(this.m_maxCost,Err.ErrType.X);
		    return Err.err(this.m_maxCost,Err.ErrType.X);
		    
		} else {
		    /* todo: careful look at refs[0] */
			return Err.err(this.m_delCostByChar[ refs[fr]], Err.ErrType.D);
		    // return new Err(this.m_delCost,"D");
		}
    }
    int InsertionCost9( char[] hyps, int fr, int to) {
		if( to-fr > 1) {
		    if( _is_word_boundary( hyps[to-1])) {
		    	if( fr==0)
				    return Err.err(this.m_insCostWordAtEdges,Err.ErrType.WIp);
		    	else
				    return Err.err(this.m_insCostWordInternal,Err.ErrType.WIi);
		    }
		    if( _is_word_boundary( hyps[fr])) {
		    	if( to == hyps.length)
		    		return Err.err(this.m_insCostWordAtEdges,Err.ErrType.WIs);
		    	else
		    		return Err.err(this.m_insCostWordInternal,Err.ErrType.WIi);
		    }
		    return Err.err(this.m_maxCost,Err.ErrType.X);
		} else {
				/* todo: careful look at hyps[0] */
			    return Err.err(this.m_insCostByChar[ hyps[fr]],Err.ErrType.I);
		}
	}
    int SubstitutionCost9_11( char ref, char hyp) {
    	if( ref == hyp) {
    		return Err.err( 0, Err.ErrType.C);
    	} else if( Character.toLowerCase( ref) ==  Character.toLowerCase( hyp)) {
    	    return Err.err( this.m_caseCost, Err.ErrType.C_case);
    	} else {
    		return Err.err( this.m_subCost, Err.ErrType.S);
    	}
    }

    int SubstitutionCost9( String refs, String hyps) {
		if( refs.equals(hyps)) {
		    if( refs == hyps) {
		    	return Err.err( 0, Err.ErrType.C);
		    } 
		    else if( refs.toLowerCase().equals( hyps.toLowerCase())) {
		    	return Err.err( this.m_caseCost, Err.ErrType.C_case);
		    }
		    else {
		    	return Err.err( this.m_subCost, Err.ErrType.S);
		    }
		} else {
		    return Err.err( this.m_maxCost, Err.ErrType.X);
		}
	
    }
    
    public static String join( List<String> ll) {
		final String sep = " ";
		if (ll.size() == 0) { return ""; }
		String ret = ll.get(0);
		for(int k=1; k<ll.size(); k++) {
		    ret += sep;
		    ret += ll.get(k);
		}
		return ret;
    }
    final boolean _is_word_boundary( final char ch)
    {
    	// return ch == ' ';
		// return Character.isWhitespace(ch);
    	return Character.isSpaceChar(ch);
    }
    void _assignCost00( DistanceResult res, int i, int j) {
		res.m_aCost[i][j].cost = 0;
		res.m_aCost[i][j] = new EditCost(0,0,0,0,Err.ErrType.NONE,"","");
    }
    void _assignCost9( DistanceResult res, int i, int j) {
		// System.out.printf("backtrace from %d,%d\n", i, j);
		
		int best_err = Err.err( this.m_maxCost, Err.ErrType.X);
		int best_cost = this.m_maxCost;
		int best_ii = 0;
		int best_jj = 0;
		
		/* char insertion */
		if(i>=0 && j-1>=0) {
			int err = this.InsertionCost9( res.m_aHyp, j-1, j); 
			int cost = res.m_aCost[i][j-1].cost + Err.cost( err);
			if(cost<best_cost) {
					best_ii = i;
					best_jj = j-1;
					best_err = err;
					best_cost = cost;
			}
		}
		
		/* char deletion */
		if(i-1>=0 && j>=0) {
			int err = this.DeletionCost9( res.m_aRef, i-1, i);
			int cost = res.m_aCost[i-1][j].cost + Err.cost( err);
			if(cost<best_cost) {
				best_ii = i-1;
				best_jj = j;
				best_err = err;
				best_cost = cost;
			}
		}
		
		/* char correct or substitution */
		if(i-1>=0 && j-1>=0) {  
			int err = this.SubstitutionCost9_11( res.m_aRef[i-1], res.m_aHyp[j-1]);
			int cost = res.m_aCost[i-1][j-1].cost + Err.cost( err);
			if(cost<best_cost) {
				best_ii = i-1;
				best_jj = j-1;
				best_err = err;
				best_cost = cost;
			}
		}
	
		boolean im1_is_wb = i>0 && _is_word_boundary( res.m_aRef[i-1]);
		/* prefix word insertion */
		if( ((i == 0 ||  im1_is_wb || i==res.m_aRef.length) )
				&& j>=1 && (j<=res.m_aHyp.length && _is_word_boundary( res.m_aHyp[j-1]) || j==res.m_aHyp.length)) {
			int err = this.InsertionCost9( res.m_aHyp, 0, j); 
			int cost = res.m_aCost[i][0].cost + Err.cost( err);
			if(cost<best_cost) {
					best_ii = i;
					best_jj = 0;
					best_err = err;
					best_cost = cost;
			}
			
			for( int j2=1; j2<=res.m_aHyp.length && j2+1<j; j2++) {
				if( _is_word_boundary( res.m_aHyp[j2]) )  {
						// inserting _and
	    				int err2 = this.InsertionCost9( res.m_aHyp, j2, j); 
						int cost2 = res.m_aCost[i][j2].cost + Err.cost( err2);
						if(cost2<best_cost) {
								best_ii = i;
								best_jj = j2;
								best_err = err2;
								best_cost = cost2;
						}
				}
				
				if( im1_is_wb && _is_word_boundary( res.m_aHyp[j2-1])) {
						// inserting and_
					// srcs.add( new Pair( i, j2));
	   				int err2 = this.InsertionCost9( res.m_aHyp, j2, j); 
					int cost2 = res.m_aCost[i][j2].cost + Err.cost( err2);
					if(cost2<best_cost) {
							best_ii = i;
							best_jj = j2;
							best_err = err2;
							best_cost = cost2;
					}
				}
			}
	
		}
		    
		
		if( m_mapToRhs != null && i>0) {
			for(int ii=Math.max(0,i-m_mapToRhs_maxLen); ii<=i-m_mapToRhs_minLen; ii++) {
				if( m_mapToRhs.containsKey( res.m_sRef.substring(ii,i))) {
					RhsMap rhsMap = m_mapToRhs.get( res.m_sRef.substring(ii,i));
					for( int jj=Math.max(0,j-rhsMap.maxLen); jj<=j-rhsMap.minLen; jj++) {
						if( rhsMap.m.containsKey( res.m_sHyp.substring(jj, j) )) {
							int inc_cost = rhsMap.m.get( res.m_sHyp.substring(jj, j));
							if( res.m_aCost[ii][jj].cost + inc_cost < best_cost) {
								best_ii = ii;
								best_jj = jj;
								best_err = Err.err(inc_cost,Err.ErrType.csyn);
								best_cost = res.m_aCost[ii][jj].cost + inc_cost;
							}
						}
					}
					
				}
			}
		}
			
		res.m_aCost[i][j].cost = best_cost;
		res.m_aCost[i][j].incCost = Err.cost( best_err);
		res.m_aCost[i][j].bkpt_i = best_ii;
		res.m_aCost[i][j].bkpt_j = best_jj;
		res.m_aCost[i][j].errType = Err.type( best_err);
		res.m_aCost[i][j].istr =   res.m_sRef.substring( best_ii, i);
		res.m_aCost[i][j].jstr = res.m_sHyp.substring( best_jj, j);
	
	
	
    }
    
    public DistanceResult getEditDistance( String sRef, String sHyp) 
    {
		DistanceResult res = new DistanceResult();
		// self.log( 'getEditDistance <%s> <%s>\n' %(aRef,aHyp)  )
		res.m_sRef = sRef;
		res.m_sHyp = sHyp;
		res.m_aRef = sRef.toCharArray();
		res.m_aHyp = sHyp.toCharArray();
		int I = res.m_aRef.length;
		int J = res.m_aHyp.length;
		
		res.m_aCost = new EditCost[1][1];
		// res._resizeCostArray(I+1, J+1);
		res._resizeCostArray(I,J);
		
		// # cache I,J
		//res.I = I;
		//res.J = J;
		//
		//    ^
		//    |
		//    J
		//    |
		//    +----I----> 
		//
		for(int i=0; i<=I; i++) {
		    for(int j=0; j<=J; j++) {
			
			if(i==0 && j==0) 
			    this._assignCost00(res, i,j);
			else {
			    this._assignCost9( res, i, j);
			}
		    }
		}
		res.distance = res.m_aCost[I][J].cost;
		return res;
    }
    
    int test(String str1, String str2, double distance, PrintStream out) {
    	
    	// LciEditDistance ruler = new LciEditDistance();
    	LciEditDistance ruler = this;
 	    
 	    DistanceResult res = ruler.getEditDistance( str1, str2);
 	    if(res.distance == distance) {
 	    	if( out!=null)
 	    		out.printf("dist(%s,%s) expected %.3f got %.3f <%s> OK\n", str1, str2, distance, res.distance, res.getCore());
 	    	return 0;
 	    } else {
 	    	if( out!=null)
 	    	   out.printf("dist(%s,%s) expected %.1f got %.2f <%s> FAIL\n", str1, str2, distance, res.distance, res.getCore());
 	    	EditCost path[] = res.getPath();
 	    	if( out!=null) {
 	    		out.printf("  backtrace from END cost %f\n", res.distance);
	 		    for(int k=0; k<path.length; k++) {
	 		    		out.printf("  %s\n", path[k].toString1() );
	 		    }
 	    	}
	 	    return 1;
 	    }
    }
    
    int testmany(PrintStream out) {
    	int nF = 0, nT = 0;
       	nT++; nF += test("Quentin Tarantino","Quentin Tarantino", 0.0, out);
       	nT++; nF += test("Mr. Bean","Mr. Bean", 0.0, out);
    	nT++; nF += test("this and that","this and that", 0.0, out);
    	nT++; nF += test("this that","this and that", m_insCostWordInternal, out);
    	nT++; nF += test("this and this","this and that", m_subCost*2, out);
    	nT++; nF += test("that and that","this and that", m_subCost*2, out);
       	nT++; nF += test("this that jake","this that", m_delCost*5, out);
    	nT++; nF += test("this that","this that joker", m_insCostWordAtEdges, out);
    	nT++; nF += test("this that","this that joker joker", m_insCostWordAtEdges, out);
    	nT++; nF += test("this that","joker this that", m_insCostWordAtEdges, out);
    	nT++; nF += test("this that","joker joker this that", m_insCostWordAtEdges, out);
    	nT++; nF += test("this that","joker this that joker", m_insCostWordAtEdges*2, out);
    	nT++; nF += test("this that","joker this joker that joker", m_insCostWordAtEdges*2+m_insCostWordInternal, out);
    	nT++; nF += test("thisand that","this that", m_delCost*3, out);
    	nT++; nF += test("this that","thisand that", m_insCost*3, out);
    	nT++; nF += test("this that","thisx that", m_insCost, out);
    	nT++; nF += test("this that","this thatx", m_insCost, out);
    	nT++; nF += test("this that","xthis that", m_insCost, out);
    	nT++; nF += test("this that","this xthat", m_insCost, out);
    	nT++; nF += test("this that","xthis that", m_insCost, out);
       	nT++; nF += test("this that","this xthat", m_insCost, out);
      	nT++; nF += test("this that","this: that", m_insCost/100, out);
      	nT++; nF += test("this that","this th:at", m_insCost/100, out);
      	nT++; nF += test("this that","this that!", m_insCost/100, out);
      	nT++; nF += test("mr bean","mr been", m_csynCostDefault, out);
      	nT++; nF += test("mister bean","mr bean", m_csynCostDefault, out);
      	nT++; nF += test("dr bean","doctor bean", m_csynCostDefault, out);
      	nT++; nF += test("foster's","fosters", m_delCost/100, out);
      	nT++; nF += test("fosters","foster's", m_insCost/100, out);      	
      	nT++; nF += test("Rizzoli and Isles","Rizzoli & Isles", m_insCost/10, out);
      	if( out != null) 
    	  out.printf("tested %d failed %d\n", nT, nF);
    	return nF;
    }

    int testmany_timing(PrintStream out) {
    
    	long nanosecSt = System.nanoTime();
    	int nT = 0;
    	for(int i=0; i<2000; i++) {
    		nT++; test("Mickey Mouse Clubhouse", "CHILDRENS' DAY SPECIAL 2014: Mickey Mouse Clubhouse: Mickey's Adventures In Wonderland", 0, null);
       		nT++; test("Jurassic World", "Untitled Jurassic World Sequel", 0, null);
       		nT++; test("Discovery channel", "Discovery channel", 0, null);
       		nT++; test("Syfy", "Sci-Fi", 0, null);
       		nT++; test("NBA basketball", "NBA basketball Grizzlies @ Raptors", 0, null);
       		if(nT % 1000 == 0) {
       	    	long nanosecEn = System.nanoTime();
       	    	long delta = nanosecEn - nanosecSt;
       	    	out.printf("timing: ran %d in %d ns; average %d ns\n", nT, delta, delta/nT);
       		}
    	}
    	long nanosecEn = System.nanoTime();
    	long delta = nanosecEn - nanosecSt;
    	out.printf("timing: ran %d in %d ns; average %d ns\n", nT, delta, delta/nT);
    	return 0;
    }
    
    /**
     * The StringDistance interface actually describes a way to compute
     * similarity, not distance, where 1.0 is an exact match. Here, the result
     * is a cost, not a similarity, so we have to change the range to be compared
     * to other StringDistance implementations.
     * 
     * @TODO make into a FucntionQuery to avoid weird StringDistance semantics?
     */
	@Override
	public float getDistance(String s1, String s2)
	{
		DistanceResult distanceResult = this.getEditDistance(s1, s2);
		return Math.max(0.0f, 1.0f - (float)distanceResult.distance / (float)m_insCost / s1.length());
	}

    /**
     * @param args
     */
    public static void main(String[] args) {
	// TODO Auto-generated method stub
	
    	LciEditDistance ruler = new LciEditDistance();
	
	// ruler.test("mister been","mr been", ruler.m_csynCostDefault, System.out);

	int nFailed = ruler.testmany(  System.out);
	ruler.testmany_timing( System.out);
  }
}

