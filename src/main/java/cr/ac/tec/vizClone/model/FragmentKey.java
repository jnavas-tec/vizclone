package cr.ac.tec.vizClone.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Data
public class FragmentKey implements Comparable<FragmentKey> {
    private int methodIdx;
    private String methodSignature;
    private int fromOffset;
    private int toOffset;
    private int idx;

    public String toString() {
        return String.format("methodIdx:%d  fromOffset:%d  toOffset:%d  idx:%d", methodIdx, fromOffset, toOffset, idx);
    }

    public FragmentKey(int methodIdx, String methodSignature, int fromOffset, int toOffset, int idx) {
        this.methodIdx = methodIdx;
        this.methodSignature = methodSignature;
        this.fromOffset = fromOffset;
        this.toOffset = toOffset;
        this.idx = idx;
    }

    public int compareTo(@NotNull FragmentKey fk) {
        // self check
        if (this == fk) return 0;
        // fields comparison
        if (methodIdx != fk.methodIdx) return methodIdx - fk.methodIdx;
        if (fromOffset != fk.fromOffset) return fromOffset - fk.fromOffset;
        if (toOffset != fk.toOffset) return toOffset - fk.toOffset;
        if (idx != fk.idx) return idx - fk.idx;
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        return compareTo((FragmentKey) o) == 0;
    }

    public boolean overlap(@NotNull FragmentKey fk, int overlapPercentage) {
        // self check
        if (this == fk) return true;
        // zero overlap
        if (methodIdx != fk.methodIdx) return false;
        // zero overlap
        if (fk.fromOffset > toOffset || fromOffset > fk.toOffset) return false;
        // find endpoints of intersection
        int left = Math.max(fromOffset, fk.fromOffset);
        int right = Math.min(toOffset, fk.toOffset);
        // calculate intersection
        int intersection = Math.max(right - left, 0);
        // calculate maximum length
        int maxLength = Math.max(fk.toOffset - toOffset, fk.fromOffset - fromOffset);
        // calculate overlap and compare to minPercent
        return intersection * 100 >= overlapPercentage * maxLength;
    }

    public boolean fromSameMethod(@NotNull FragmentKey fk) {
        return this.getMethodIdx() == fk.getMethodIdx();
    }
}
