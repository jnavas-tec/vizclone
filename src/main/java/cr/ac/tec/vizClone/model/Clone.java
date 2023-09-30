package cr.ac.tec.vizClone.model;

import java.util.ArrayList;

public class Clone {
    private int clone;
    private ArrayList<Fragment> fragments;
    private int numberOfFragments;
    private int cognitiveComplexity;
    private int numberOfSentences;
    private int weight;

    public int getClone() {
        return clone;
    }

    public void setClone(int clone) {
        this.clone = clone;
    }

    public ArrayList<Fragment> getFragments() {
        return fragments;
    }

    public void setFragments(ArrayList<Fragment> fragments) {
        this.fragments = fragments;
    }

    public int getNumberOfFragments() {
        return numberOfFragments;
    }

    public void setNumberOfFragments(int numberOfFragments) {
        this.numberOfFragments = numberOfFragments;
    }

    public int getCognitiveComplexity() {
        return cognitiveComplexity;
    }

    public void setCognitiveComplexity(int cognitiveComplexity) {
        this.cognitiveComplexity = cognitiveComplexity;
    }

    public int getNumberOfSentences() {
        return numberOfSentences;
    }

    public void setNumberOfSentences(int numberOfSentences) {
        this.numberOfSentences = numberOfSentences;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
