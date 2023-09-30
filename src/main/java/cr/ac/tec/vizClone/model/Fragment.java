package cr.ac.tec.vizClone.model;

import java.util.ArrayList;

public class Fragment {
    private int project;
    private int workspace;
    private int clazz;
    private int method;
    private int fragment;
    private ArrayList<Clone> clones;
    private int numberOfClones;
    private int cognitiveComplexity;
    private int numberOfSentences;
    private int fromLine;
    private int toLine;
    private int weight;

    public int getProject() {
        return project;
    }

    public void setProject(int project) {
        this.project = project;
    }

    public int getWorkspace() {
        return workspace;
    }

    public void setWorkspace(int workspace) {
        this.workspace = workspace;
    }

    public int getClazz() {
        return clazz;
    }

    public void setClazz(int clazz) {
        this.clazz = clazz;
    }

    public int getMethod() {
        return method;
    }

    public void setMethod(int method) {
        this.method = method;
    }

    public int getFragment() {
        return fragment;
    }

    public void setFragment(int fragment) {
        this.fragment = fragment;
    }

    public ArrayList<Clone> getClones() {
        return clones;
    }

    public void setClones(ArrayList<Clone> clones) {
        this.clones = clones;
    }

    public int getNumberOfClones() {
        return numberOfClones;
    }

    public void setNumberOfClones(int numberOfClones) {
        this.numberOfClones = numberOfClones;
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

    public int getFromLine() {
        return fromLine;
    }

    public void setFromLine(int fromLine) {
        this.fromLine = fromLine;
    }

    public int getToLine() {
        return toLine;
    }

    public void setToLine(int toLine) {
        this.toLine = toLine;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
