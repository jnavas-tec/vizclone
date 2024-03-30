package cr.ac.tec.vizClone.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

@Data
public class Clone {
    private int idx;
    private ArrayList<ClonePair> clonePairs = new ArrayList<>();
    private int numberOfClonePairs;
    private ArrayList<CMethod> methods = new ArrayList<>();
    private int maxCognitiveComplexity = 0;
    private int maxNumberOfStatements = 0;
    private int maxWeight = 0;
    private int maxSim = 0;
    private ArrayList<Fragment> sortedFragments = null;
    private boolean sorted = false;

    public String toString() {
        return String.format("idx:%d  #cp:%d", idx, numberOfClonePairs);
    }

    public void fixClonePairs(int clonePairIdx) {
        clonePairs.remove(clonePairIdx);
        for (int cp = clonePairIdx; cp < clonePairs.size(); cp++) {
            clonePairs.get(cp).decIdxOnClone();
        }
    }

    public ArrayList<Fragment> getSortedFragments() {
        if (!sorted) {
            sorted = true;
            sortedFragments = new ArrayList<Fragment>(clonePairs.size() * 2);
            for (ClonePair cp : clonePairs) {
                sortedFragments.add(cp.getFragments().get(0));
                sortedFragments.add(cp.getFragments().get(1));
            }
            Collections.sort(sortedFragments, new Comparator<Fragment>() {
                @Override
                public int compare(Fragment o1, Fragment o2) {
                    if (o1.getIdx() != o2.getIdx())
                        return o1.getIdx() - o2.getIdx();
                    return 0;
                }
            });
        }
        return sortedFragments;
    }
}
