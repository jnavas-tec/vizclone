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
    private int level = 0;
    private int cloneType = 0;

    public String toString() {
        return String.format("idx:%d  clone:%d  idxOnClone:%d  #fragments:%d",
            idx, clone.getIdx(), idxOnClone, fragments.size());
    }

    public void decIdxOnClone() {
        idxOnClone--;
    }

    public void fixClonePair(Clone clone, int idx) {
        this.clone = clone;
        this.idxOnClone = idx;
        this.maxCognitiveComplexity = Math.max(fragments.get(0).getCognitiveComplexity(), fragments.get(1).getIdxOnClonePair());
        this.maxNumberOfStatements = Math.max(fragments.get(0).getNumberOfStatements(), fragments.get(1).getNumberOfStatements());
        fragments.get(0).setClonePair(this);
        fragments.get(0).setIdxOnClonePair(0);
        fragments.get(1).setClonePair(this);
        fragments.get(1).setIdxOnClonePair(1);
    }

    public void addFragments(CMethod methodA, CMethod methodB) {
        Fragment fragmentA = new Fragment();
        fragmentA.setClonePair(this);
        fragmentA.setCMethod(methodA);
        fragmentA.setIdxOnClonePair(0);

        Fragment fragmentB = new Fragment();
        fragmentB.setClonePair(this);
        fragmentB.setCMethod(methodB);
        fragmentB.setIdxOnClonePair(1);

        this.fragments.add(fragmentA);
        this.fragments.add(fragmentB);
    }
}
