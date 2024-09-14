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
    private static int nextIdx = 0;

    public void initFragment(CMethod cMethod, int fromStatement, int toStatement) {
        this.setCPackage(cMethod.getCClass().getCPackage());
        this.setCClass(cMethod.getCClass());
        this.setCMethod(cMethod);
        this.setFromStatement(fromStatement);
        this.setToStatement(toStatement);
        this.setNumberOfStatements(toStatement - fromStatement + 1);
        this.setFromOffset(cMethod.getCStatements().get(fromStatement).getFromOffset());
        this.setToOffset(cMethod.getCStatements().get(toStatement).getToOffset());
        this.setFromLineColumn(cMethod.getCStatements().get(fromStatement).getFromLineColumn());
        this.setToLineColumn(cMethod.getCStatements().get(toStatement).getToLineColumn());
        if (this.getFromLineColumn().line > this.getToLineColumn().line) {
            boolean shouldNotStopHere = false;
        }
        int ccScore = 0;
        for (int s = fromStatement; s <= toStatement; s++) ccScore += cMethod.getCStatements().get(s).getCcScore();
        this.setCognitiveComplexity(ccScore);
        this.setKey(new FragmentKey(this.getCMethod().getIdx(),
            this.getCMethod().getCStatements().get(this.getFromStatement()).getFromOffset(),
            this.getCMethod().getCStatements().get(this.getToStatement()).getToOffset(),
            0));
    }

    public String toString() {
        return String.format("idx:%d  clone:%d  clonePair:%d  idxOnClonePair:%d  methodIdx:%d  from:%d  to:%d",
            idx, clonePair.getClone().getIdx(), clonePair.getIdx(), idxOnClonePair, cMethod.getIdx(), fromOffset, toOffset);
    }

    public Clone getClone() {
        Clone clone = null;
        if (clonePair != null) {
            clone = clonePair.getClone();
        }
        return clone;
    }

    public boolean overlaps(Fragment fragment, int overlapPercentage) {
        // self check
        if (this == fragment) return true;
        // zero overlap
        if (!this.getCMethod().getSignature().equals(fragment.getCMethod().getSignature())) return false;
        // zero overlap
        if (fragment.fromOffset > toOffset || fromOffset > fragment.toOffset) return false;
        // find endpoints of intersection
        int left = Math.max(fromOffset, fragment.fromOffset);
        int right = Math.min(toOffset, fragment.toOffset);
        // calculate intersection
        int intersection = Math.max(right - left, 0);
        // calculate maximum length
        int maxLength = Math.max(fragment.toOffset - toOffset, fragment.fromOffset - fromOffset);
        // calculate overlap and compare to minPercent
        return intersection * 100 >= overlapPercentage * maxLength;
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
