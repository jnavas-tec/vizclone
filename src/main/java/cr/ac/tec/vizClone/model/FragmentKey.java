package cr.ac.tec.vizClone.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Data
public class FragmentKey implements Comparable<FragmentKey> {
    private String methodSignature;
    private int fromOffset;
    private int toOffset;

    public FragmentKey(String methodSignature, int fromOffset, int toOffset) {
        this.methodSignature = methodSignature;
        this.fromOffset = fromOffset;
        this.toOffset = toOffset;
    }

    public int compareTo(FragmentKey fk) {
        // self check
        if (this == fk) return 0;
        // null check
        if (fk == null) return -1;
        // field comparison
        if (!Objects.equals(methodSignature, fk.methodSignature)) return methodSignature.compareTo(fk.methodSignature);
        if (fromOffset != fk.fromOffset) return fromOffset - fk.fromOffset;
        if (toOffset != fk.toOffset) return toOffset - fk.toOffset;
        return 0;
    }
}
