package cr.ac.tec.vizClone.model;

import com.intellij.openapi.util.text.LineColumn;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class Fragment {
    private FragmentKey key;
    private int project;
    private CPackage cPackage;
    private CClass cClass;
    private CMethod cMethod;
    private int idx;
    private ClonePair clonePair;
    private int idxOnClonePair;
    private int cognitiveComplexity = 0;
    private int numberOfStatements = 0;
    private int fromStatement;
    private int toStatement;
    private int fromOffset;
    private int toOffset;
    private LineColumn fromLineColumn;
    private LineColumn toLineColumn;
    private boolean merged = false;

    public String toString() {
        return String.format("idx:%d  merged:%b  clone:%d  clonePair:%d  idxOnClonePair:%d  methodIdx:%d  from:%d  to:%d",
            idx, merged, clonePair.getClone().getIdx(), clonePair.getIdx(), idxOnClonePair, cMethod.getIdx(), fromOffset, toOffset);
    }

    public Clone getClone() {
        Clone clone = null;
        if (clonePair != null) {
            clone = clonePair.getClone();
        }
        return clone;
    }

    public boolean canBeMerged(Fragment f, int overlapPercentage) {
        return
            //this.getClonePair().getIdx() != f.getClonePair().getIdx() &&
            this.getClone().getIdx() != f.getClone().getIdx() &&
            this.getKey().overlap(f.getKey(), overlapPercentage);
    }

    public boolean fromSameMethod(@NotNull Fragment f) {
        return this.getCMethod().getIdx() == f.getCMethod().getIdx();
    }

    public boolean fromSameMethodOnly(@NotNull Fragment f) {
        return this.getCMethod().getIdx() == f.getCMethod().getIdx() &&
            (this.getFromOffset() > f.getToOffset() ||
             this.getToOffset() < f.getFromOffset());
    }
}
