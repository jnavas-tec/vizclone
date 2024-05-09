package cr.ac.tec.vizClone.model;

import com.intellij.openapi.util.text.LineColumn;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

@Data
public class CloneGraph {
    private int numClones;
    private ArrayList<Clone> clones;
    private int numFragments;
    private int numMethods;
    private int stripeHeight;
    private ArrayList<Fragment> fragments;
    private ArrayList<ClonePair> clonePairs;
    private int minWeight = 90;
    private int maxWeight = 100;
    private int weightRange = 100 - 90;
    private int numWeightLevels = 4;
    private static final int MAX_CLONE_PAIRS = 10;
    //private static final int MAX_CC_LEVELS = 4;
    //private static final int MAX_FRAGMENTS = 5;
    //public static final int MAX_WEIGHT = MAX_CC_LEVELS * MAX_FRAGMENTS;

    private boolean selected = false;
    private int selectedClone;

    private boolean hoveredFragments = false;
    private ArrayList<Integer> hoveredFragmentList = new ArrayList<>();

    private final Random r = new Random();

    public CloneGraph(int numFragments, int numMethods, int numClones, int stripeHeight,
                      int minWeight, int maxWeight, int numWeightLevels) {
        this.numFragments = numFragments;
        this.numMethods = numMethods;
        this.numClones = numClones;
        this.stripeHeight = stripeHeight;
        this.setMinWeight(minWeight);
        this.setMaxWeight(maxWeight);
        this.setNumWeightLevels(numWeightLevels);
    }

    public void populateGraph() {
        this.clones = new ArrayList<Clone>();
        this.fragments = new ArrayList<Fragment>();
        this.clonePairs = new ArrayList<>();
        for (int c = 0; c < numClones; c++) {
            Clone clone = new Clone();
            this.clones.add(clone);
            clone.setIdx(c);
            //clone.setNumberOfClonePairs(getNextRandom(1, MAX_CLONE_PAIRS));
            //clone.setClonePairs(new ArrayList<ClonePair>());
            //clone.setMaxCognitiveComplexity(getNextRandom(1, MAX_CC_LEVELS + 1));
            //clone.setMaxSim(getNextRandom(this.minWeight, this.maxWeight));
            //clone.setMaxLevel(Math.min(this.numWeightLevels - 1,
            //    ((clone.getMaxSim() - this.minWeight) * this.numWeightLevels / this.weightRange)));
            //clone.setMaxWeight((clone.getMaxSim() - this.minWeight) * 100 / this.weightRange);
        }
        CMethod method = new CMethod();
        LineColumn from = LineColumn.of(10, 20);
        LineColumn to = LineColumn.of(20, 40);

        method.setName("createNewVisualization");
        for (int f = 0; f < numFragments; f++) {
            Fragment fragment = new Fragment();
            this.fragments.add(fragment);
            fragment.setIdx(f);
            fragment.setCMethod(method);
            fragment.setFromLineColumn(from);
            fragment.setToLineColumn(to);
            //fragment.setNumberOfClonePairs(0);
            //fragment.setClones(new ArrayList<Clone>());
            //fragment.setCognitiveComplexity(getNextRandom(1, MAX_CC_LEVELS + 1));

            //ClonePair clonePair = new ClonePair();
            //clonePair.setIdx(f);
            //this.clonePairs.add(clonePair);
        }
        ArrayList<Integer> s = getShuffledFragments(numFragments, null);
        ArrayList<Integer> cp = getShuffledFragments(numFragments, null);
        int n = 0;
        int c = 0;
        for (int i = 0; i < numFragments / 2; i++) {
            if (c == numClones) { cp = getShuffledFragments(numClones, cp); c = 0; }
            Clone clone = this.clones.get(c++);

            Fragment fragment0 = this.fragments.get(s.get(n++));
            Fragment fragment1 = this.fragments.get(s.get(n++));

            ClonePair clonePair = new ClonePair(); //this.clonePairs.get(cp.get(p++));
            clonePair.setClone(clone);
            clonePair.getFragments().add(fragment0);
            clonePair.getFragments().add(fragment1);
            clonePair.setMaxNumberOfStatements(Math.max(fragment0.getNumberOfStatements(), fragment1.getNumberOfStatements()));
            clonePair.setSim(getNextRandom(this.minWeight, this.maxWeight));
            clonePair.setLevel(Math.min(this.numWeightLevels - 1,
                ((clonePair.getSim() - this.minWeight) * this.numWeightLevels / this.weightRange)));
            clonePair.setWeight((clonePair.getSim() - this.minWeight) * 100 / this.weightRange);

            clone.getClonePairs().add(clonePair);
            clone.setNumberOfClonePairs(clone.getNumberOfClonePairs() + 1);
            clone.setMaxWeight(Math.max(clone.getMaxWeight(), clonePair.getWeight()));
            clone.setMaxSim(Math.max(clone.getMaxSim(), clonePair.getSim()));
            clone.setMaxLevel(Math.max(clone.getMaxLevel(), clonePair.getLevel()));
            clone.setMaxNumberOfStatements(Math.max(clone.getMaxNumberOfStatements(), clonePair.getMaxNumberOfStatements()));

            fragment0.setClonePair(clonePair);
            fragment1.setClonePair(clonePair);
        }
    }

    public CloneGraph(ArrayList<Clone> clones, ArrayList<Fragment> fragments, ArrayList<Method> methods,
                      int minWeight, int maxWeight, int numWeightLevels) {
        this.clones = clones;
        this.numClones = clones.size();
        this.fragments = fragments;
        this.numFragments = fragments.size();
        this.numMethods = methods.size();
        this.setMinWeight(minWeight);
        this.setMaxWeight(maxWeight);
        this.setNumWeightLevels(numWeightLevels);
    }

    public void setMinWeight(int minWeight) {
        this.minWeight = minWeight;
        this.weightRange = (this.maxWeight - this.minWeight);
    }

    public void setMaxWeight(int maxWeight) {
        this.maxWeight = maxWeight;
        this.weightRange = (this.maxWeight - this.minWeight);
    }

    public int getScaledWeight(int weight, int maxHeight) {
        int scaledMaxHeight = maxHeight * 4 / 5;
        int scaledOffset = maxHeight / 5;
        int scaledWeight = weight * scaledMaxHeight / 100 + scaledOffset;
        return scaledWeight;
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
}
