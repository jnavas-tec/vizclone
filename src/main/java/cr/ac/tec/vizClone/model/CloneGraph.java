package cr.ac.tec.vizClone.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

@Data
public class CloneGraph {
    private int numClones;
    private ArrayList<Clone> clones;
    private int numFragments;
    private ArrayList<Fragment> fragments;
    private ArrayList<ClonePair> clonePairs;
    private int minWeight = 90;
    private int maxWeight = 100;
    private int weightRange = 100 - 90;
    private int numWeightLevels = 4;
    private static final int MAX_CC_LEVELS = 4;
    private static final int MAX_FRAGMENTS = 5;
    public static final int MAX_WEIGHT = MAX_CC_LEVELS * MAX_FRAGMENTS;

    private final Random r = new Random();

    public CloneGraph(int numFragments, int numClones) {
        this.numFragments = numFragments;
        this.numClones = numClones;
        this.clones = new ArrayList<Clone>();
        this.fragments = new ArrayList<Fragment>();
        this.clonePairs = new ArrayList<>();
        for (int c = 0; c < numClones; c++) {
            Clone clone = new Clone();
            this.clones.add(clone);
            clone.setIdx(c);
            clone.setNumberOfClonePairs(getNextRandom(2, MAX_FRAGMENTS + 2));
            clone.setClonePairs(new ArrayList<ClonePair>());
            clone.setMaxCognitiveComplexity(getNextRandom(1, MAX_CC_LEVELS + 1));
            clone.setMaxWeight(
                    (clone.getNumberOfClonePairs() - 1) *
                    clone.getMaxCognitiveComplexity());
        }
        for (int f = 0; f < numFragments; f++) {
            Fragment fragment = new Fragment();
            this.fragments.add(fragment);
            fragment.setIdx(f);
            //fragment.setNumberOfClonePairs(0);
            //fragment.setClones(new ArrayList<Clone>());
            fragment.setCognitiveComplexity(getNextRandom(1, MAX_CC_LEVELS + 1));

            ClonePair clonePair = new ClonePair();
            clonePair.setIdx(f);
            this.clonePairs.add(clonePair);
        }
        ArrayList<Integer> s = getShuffledFragments(numFragments, null);
        ArrayList<Integer> cp = getShuffledFragments(numFragments, null);
        int n = 0;
        int p = 0;
        for (int c = 0; c < numClones; c++) {
            Clone clone = this.clones.get(c);
            for (int f = 0; f < clone.getNumberOfClonePairs(); f++) {
                if (n == numFragments) { s = getShuffledFragments(numFragments, s); n = 0; }
                if (p == numFragments) { cp = getShuffledFragments(numFragments, cp); p = 0; }

                Fragment fragment0 = this.fragments.get(s.get(n++));
                //fragment0.getClones().add(clone);
                //fragment0.setNumberOfClonePairs(fragment0.getNumberOfClonePairs() + 1);
                Fragment fragment1 = this.fragments.get(s.get(n++));
                //fragment1.getClones().add(clone);
                //fragment1.setNumberOfClonePairs(fragment1.getNumberOfClonePairs() + 1);

                ClonePair clonePair = this.clonePairs.get(cp.get(p++));
                clonePair.setClone(clone);
                clonePair.getFragments().add(fragment0);
                clonePair.getFragments().add(fragment1);
                clonePair.setMaxNumberOfStatements(Math.max(fragment0.getNumberOfStatements(), fragment1.getNumberOfStatements()));
                clonePair.setMaxCognitiveComplexity(Math.max(fragment0.getCognitiveComplexity(), fragment1.getCognitiveComplexity()));
                clonePair.setWeight((getNextRandom(2, MAX_FRAGMENTS + 2) - 1) * clonePair.getMaxCognitiveComplexity());

                clone.getClonePairs().add(clonePair);
                clone.setMaxWeight(Math.max(clone.getMaxWeight(), clonePair.getWeight()));
                clone.setMaxCognitiveComplexity(Math.max(clone.getMaxCognitiveComplexity(), clonePair.getMaxCognitiveComplexity()));
                clone.setMaxNumberOfStatements(Math.max(clone.getMaxNumberOfStatements(), clonePair.getMaxNumberOfStatements()));
            }
        }
    }

    public CloneGraph(ArrayList<Clone> clones, ArrayList<Fragment> fragments) {
        this.clones = clones;
        this.numClones = clones.size();
        this.fragments = fragments;
        this.numFragments = fragments.size();
    }

    public void setMinWeight(int minWeight) {
        this.minWeight = minWeight;
        this.weightRange = this.maxWeight - this.minWeight;
    }

    public void setMaxWeight(int maxWeight) {
        this.maxWeight = maxWeight;
        this.weightRange = this.maxWeight - this.minWeight;
    }

    private ArrayList<Integer> getShuffledFragments(int n, ArrayList<Integer> source) {
        if (source == null) {
            source = new ArrayList<>();
            for (int idx = 0; idx < n; idx++) source.add(idx);
        }
        Collections.shuffle(source);
        return source;
    }

    private int getNextRandom(int low, int high) {
        return r.nextInt(high - low) + low;
    }

    public int getFragmentColorIndex(int weight) {
        //int weightLevels = 100 / numWeightLevels;
        //int level = (weight - minWeight) * 100 / weightRange;
        //return Math.min(numWeightLevels - 1, level / weightLevels);
        return ((weight + numWeightLevels) / (numWeightLevels + 1)) - 1;
    }

    public void fixWeights() {
        for (Clone c : clones) {
            c.setMaxSim(c.getMaxWeight());
            c.setMaxWeight(((c.getMaxWeight() - minWeight) * 19 / weightRange) + 1);
            for (ClonePair cp : c.getClonePairs()) {
                cp.setSim(cp.getWeight());
                cp.setWeight(((cp.getWeight() - minWeight) * 19 / weightRange) + 1);
            }
        }
    }
}
