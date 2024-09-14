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
    private int maxCognitiveComplexity = 0;
    private int maxNumberOfStatements = 0;
    private int maxWeight = 0;
    private int maxSim = 0;
    private int maxLevel = 0;
    private int maxCloneType = 0;
    private ArrayList<Method> methods = new ArrayList<>();
    private boolean sorted = false;

    public String toString() {
        return String.format("idx:%d  #cp:%d", idx, numberOfClonePairs);
    }

    public void addClonePair(ClonePair clonePair) {
        clonePair.setIdxOnClone(this.clonePairs.size());
        this.clonePairs.add(clonePair);
    }

    private void resetMaxValues() {
        this.maxLevel = 0;
        this.maxCognitiveComplexity = 0;
        this.maxSim = 0;
        this.maxWeight = 0;
        this.maxNumberOfStatements = 0;
        this.maxCloneType = 0;
    }

    private void setMaxValues(int maxLevel, int maxCognitiveComplexity, int maxSim, int maxWeight,
                              int maxNumberOfStatements, int maxCloneType) {
        this.maxLevel = Math.max(this.maxLevel, maxLevel);
        this.maxCognitiveComplexity = Math.max(this.maxCognitiveComplexity, maxCognitiveComplexity);
        this.maxSim = Math.max(this.maxSim, maxSim);
        this.maxWeight = Math.max(this.maxWeight, maxWeight);
        this.maxNumberOfStatements = Math.max(this.maxNumberOfStatements, maxNumberOfStatements);
        this.maxCloneType = Math.max(this.maxCloneType, maxCloneType);
    }

    public void fixClone(int idx) {
        this.idx = idx;
        this.numberOfClonePairs = this.clonePairs.size();
        this.resetMaxValues();
        for (int cp = 0; cp < this.clonePairs.size(); cp++) {
            ClonePair clonePair = this.clonePairs.get(cp);
            clonePair.fixClonePair(this, cp);
            this.setMaxValues(clonePair.getLevel(), clonePair.getMaxCognitiveComplexity(), clonePair.getSim(),
                clonePair.getWeight(), clonePair.getMaxNumberOfStatements(), clonePair.getCloneType());
        }
    }

    public void removeAndFixClonePairs(int clonePairIdx) {
        clonePairs.remove(clonePairIdx);
        fixClonePairs(clonePairIdx);
    }

    public void fixClonePairs(int idx) {
        for (int cp = idx; cp < clonePairs.size(); cp++) {
            clonePairs.get(cp).setIdxOnClone(cp);
        }
    }

    public boolean containsMethod(int idx) {
        for (Method method : this.methods) {
            if (method.getIdx() == idx) return true;
        }
        return false;
    }

    public int methodIndex(int idx) {
        for (int m = 0; m < this.methods.size(); m++) {
            if (methods.get(m).getIdx() == idx) return m;
        }
        return -1;
    }
}
