package cr.ac.tec.vizClone.model;

import com.intellij.openapi.util.text.LineColumn;
import lombok.Data;

import java.util.ArrayList;

@Data
public class Fragment {
    private FragmentKey key;
    private int project;
    private CPackage cPackage;
    private CClass cClass;
    private CMethod cMethod;
    private int idx;
    private ArrayList<Clone> clones = new ArrayList<>();
    private int numberOfClones = 0;
    private int cognitiveComplexity = 0;
    private int numberOfStatements = 0;
    private int fromStatement;
    private int toStatement;
    private int fromOffset;
    private int toOffset;
    private LineColumn fromLineColumn;
    private LineColumn toLineColumn;
}
