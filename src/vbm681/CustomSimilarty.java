package vbm681;

import org.apache.lucene.search.similarities.BM25Similarity;

class CustomSimilarty extends  BM25Similarity {
	
	@Override
	protected float idf(long docFreq, long docCount) {
	    return 1;
	  }
	
	public CustomSimilarty( float b) {
		
		super(1.2f, b);	
		
		
	}

}
