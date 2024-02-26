package cr.ac.tec.vizClone.model;

import lombok.Data;

import java.util.ArrayList;

@Data
public class Clone {
    private int idx;
    private ArrayList<ClonePair> clonePairs = new ArrayList<>();
    private ArrayList<CMethod> methods = new ArrayList<>();
    private int numberOfClonePairs = 0;
    private int maxCognitiveComplexity = 0;
    private int maxNumberOfStatements = 0;
    private int maxWeight = 0;
}
