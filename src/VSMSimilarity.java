import org.apache.lucene.search.similarities.DefaultSimilarity;

public class VSMSimilarity extends DefaultSimilarity {

    public final boolean sublinear = true;

    public VSMSimilarity() {

    }

    // Term frequency (tf) in a document is a measure of how often a term appears in the document
    // Using sub-linear term frequency weighting to avoid crappy results.
    @Override
    public float tf(int freq) {
        if (sublinear) {
            return (freq > 0) ? (1 + (float)Math.log(freq)) : 0;
        }
        return freq;
    }

    // A score factor based on term overlap with the query i.e. number of terms found in the query
    @Override
    public float coord(int overlap, int maxOverlap) {
        return 1;
    }
}