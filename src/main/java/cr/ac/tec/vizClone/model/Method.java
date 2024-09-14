package cr.ac.tec.vizClone.model;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class Method implements Comparable<Method> {
    private int idx;
    private int cMethodIdx;
    private int cloneIdx;
    private int maxWeight = 0;
    private int maxSim = 0;
    private int maxLevel = 0;
    private int maxCloneType = 0;
    private int ccScore = 0;
    private int fromStatement;
    private int toStatement;

    public int compareTo(@NotNull Method m) {
        // self check
        if (this == m) return 0;
        // fields comparison
        if (cMethodIdx != m.cMethodIdx) return cMethodIdx - m.cMethodIdx;
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        return compareTo((Method) o) == 0;
    }
}
