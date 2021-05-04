package edu.virginia.cs.index.similarities;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import java.lang.Math;
public class OkapiBM25 extends SimilarityBase {
    double k1 = 1.5;
    double k2 = 750;
    double b = 1.0;
    /**
     * Returns a score for a single term in the document.
     *
     * @param stats
     *            Provides access to corpus-level statistics
     * @param termFreq
     * @param docLength
     */
    @Override
    protected float score(BasicStats stats, float termFreq, float docLength) {
        double idf = Math.log((stats.getNumberOfDocuments() - stats.getDocFreq() + 0.5)/(stats.getDocFreq() + 0.5));
        double tf_num = (k1 + 1) * termFreq;
        double tf_denom = k1 * (1 - b + (b * (docLength/stats.getAvgFieldLength()))) + termFreq;
        return (float) (idf * (tf_num/tf_denom));
    }

    @Override
    public String toString() {
        return "Okapi BM25";
    }

}
