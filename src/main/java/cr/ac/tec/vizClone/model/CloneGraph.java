package cr.ac.tec.vizClone.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;


public class CloneGraph {
    private int numClones;
    private ArrayList<Clone> clones;
    private int numFragments;
    private ArrayList<Fragment> fragments;
    private static final int MAX_CC_LEVELS = 4;
    private static final int MAX_FRAGMENTS = 5;
    public static final int MAX_WEIGHT = MAX_CC_LEVELS * MAX_FRAGMENTS;

    private final Random r = new Random();

    public CloneGraph(int numFragments, int numClones) {
        this.numFragments = numFragments;
        this.numClones = numClones;
        this.clones = new ArrayList<Clone>();
        this.fragments = new ArrayList<Fragment>();
        for (int c = 0; c < numClones; c++) {
            Clone clone = new Clone();
            this.clones.add(clone);
            clone.setClone(c);
            clone.setNumberOfFragments(getNextRandom(2, MAX_FRAGMENTS + 2));
            clone.setFragments(new ArrayList<Fragment>());
            clone.setCognitiveComplexity(getNextRandom(1, MAX_CC_LEVELS + 1));
            clone.setWeight(
                    (clone.getNumberOfFragments() - 1) *
                    clone.getCognitiveComplexity());
        }
        for (int f = 0; f < numFragments; f++) {
            Fragment fragment = new Fragment();
            this.fragments.add(fragment);
            fragment.setFragment(f);
            fragment.setNumberOfClones(0);
            fragment.setClones(new ArrayList<Clone>());
            fragment.setCognitiveComplexity(getNextRandom(1, MAX_CC_LEVELS + 1));
            fragment.setWeight(
                    (getNextRandom(2, MAX_FRAGMENTS + 2) - 1) *
                            fragment.getCognitiveComplexity());
        }
        ArrayList<Integer> s = getShuffledFragments(numFragments, null);
        int n = 0;
        for (int c = 0; c < numClones; c++) {
            Clone clone = this.clones.get(c);
            for (int f = 0; f < clone.getNumberOfFragments(); f++) {
                if (n == numFragments) { s = getShuffledFragments(numFragments, s); n = 0; }
                Fragment fragment = this.fragments.get(s.get(n));
                fragment.getClones().add(clone);
                fragment.setNumberOfClones(fragment.getNumberOfClones() + 1);
                clone.getFragments().add(fragment);
                n++;
            }
        }
    }

    public CloneGraph(ArrayList<Clone> clones, ArrayList<Fragment> fragments) {
        this.clones = clones;
        this.numClones = clones.size();
        this.fragments = fragments;
        this.numFragments = fragments.size();
    }

    public int getNumClones() {
        return numClones;
    }

    public void setNumClones(int numClones) {
        this.numClones = numClones;
    }

    public ArrayList<Clone> getClones() {
        return clones;
    }

    public void setClones(ArrayList<Clone> clones) {
        this.clones = clones;
    }

    public int getNumFragments() {
        return numFragments;
    }

    public void setNumFragments(int numFragments) {
        this.numFragments = numFragments;
    }

    public ArrayList<Fragment> getFragments() {
        return fragments;
    }

    public void setFragments(ArrayList<Fragment> fragments) {
        this.fragments = fragments;
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
        return ((weight + MAX_FRAGMENTS - 1) / MAX_FRAGMENTS) - 1;
    }
}
