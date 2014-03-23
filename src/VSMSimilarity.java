import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.util.BytesRef;

public class VSMSimilarity extends TFIDFSimilarity {

    public VSMSimilarity() {

    }

    @Override
    public float coord(int i, int i2) {
        return 0;
    }

    @Override
    public float queryNorm(float v) {
        return 0;
    }

    @Override
    public float tf(float v) {
        return 0;
    }

    @Override
    public float idf(long l, long l2) {
        return 0;
    }

    @Override
    public float lengthNorm(FieldInvertState fieldInvertState) {
        return 0;
    }

    @Override
    public float sloppyFreq(int i) {
        return 0;
    }

    @Override
    public float scorePayload(int i, int i2, int i3, BytesRef bytesRef) {
        return 0;
    }

}