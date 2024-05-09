package cr.ac.tec.vizClone.model;

import lombok.Data;

@Data
public class Method {
    private int idx;
    private int cMethodIdx;
    private int cloneIdx;
    private int maxWeight = 0;
    private int maxSim = 0;
    private int maxLevel = 0;
    private int maxCloneType = 0;
}
