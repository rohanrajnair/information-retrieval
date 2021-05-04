package edu.virginia.cs.index.similarities;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;

public class PivotedLength extends SimilarityBase {
    double s = 0.75;
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
        double tf_num = 1 + Math.log(1 + Math.log(termFreq));
        double tf_denom = 1 - s + (s * (docLength / stats.getAvgFieldLength()));
        double idf = Math.log((stats.getNumberOfDocuments() + 1) / stats.getDocFreq());

        return (float) ((tf_num/tf_denom) * idf);
    }

    @Override
    public String toString() {
        return "Pivoted Length Normalization";
    }

}
