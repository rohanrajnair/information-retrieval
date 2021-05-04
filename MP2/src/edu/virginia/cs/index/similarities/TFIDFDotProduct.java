package edu.virginia.cs.index.similarities;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import java.lang.Math;

public class TFIDFDotProduct extends SimilarityBase {
    /**
     * Returns a score for a single term in the document.
     *
     * @param stats
     *            Provides access to corpus-level statistics
     * @param termFreq
     * @param docLength
     */
    @Override
    protected float score(BasicStats stats, float termFreq, float docLength){
        double tf = 1 + Math.log(termFreq);
        double idf = Math.log((stats.getNumberOfDocuments() + 1)/stats.getDocFreq());
        return (float) (tf * idf);
    }

    @Override
    public String toString() {
        return "TF-IDF Dot Product";
    }
}
