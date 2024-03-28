package cr.ac.tec.vizClone.model;

import lombok.Data;

import java.util.ArrayList;

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

    public String toString() {
        return String.format("idx:%d  #cp:%d", idx, numberOfClonePairs);
    }

    public void fixClonePairs(int clonePairIdx) {
        clonePairs.remove(clonePairIdx);
        for (int cp = clonePairIdx; cp < clonePairs.size(); cp++) {
            clonePairs.get(cp).decIdxOnClone();
        }
    }
}
