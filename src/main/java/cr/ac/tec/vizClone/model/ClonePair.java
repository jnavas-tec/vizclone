package cr.ac.tec.vizClone.model;

import lombok.Data;

import java.util.ArrayList;

@Data
public class ClonePair {
    private int idx;
    private Clone clone;
    private int idxOnClone;
    private ArrayList<Fragment> fragments = new ArrayList<>();
    private int maxCognitiveComplexity = 0;
    private int maxNumberOfStatements = 0;
    private int weight = 0;
    private int sim = 0;

    public String toString() {
        return String.format("idx:%d  merged:%b-%b  clone:%d  idxOnClone:%d  #fragments:%d",
            idx, fragments.get(0).isMerged(), fragments.get(1).isMerged(), clone.getIdx(), idxOnClone, fragments.size());
    }

    public void decIdxOnClone() {
        idxOnClone--;
    }
}
