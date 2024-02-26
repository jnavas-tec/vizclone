package cr.ac.tec.vizClone.model;

import lombok.Data;

import java.util.ArrayList;

@Data
public class ClonePair {
    private int idx;
    private Clone clone;
    private ArrayList<Fragment> fragments = new ArrayList<>();
    private int maxCognitiveComplexity = 0;
    private int maxNumberOfStatements = 0;
    private int weight = 0;
}
