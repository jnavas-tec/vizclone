package cr.ac.tec.vizClone.model;

import com.intellij.openapi.util.text.LineColumn;
import com.intellij.psi.PsiMethod;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

@Data
public class CMethod {
    private Integer idx;
    private String name;
    private String signature;
    private LineColumn fromLineColumn;
    private LineColumn toLineColumn;
    private Integer fromOffset;
    private Integer toOffset;
    private Integer numTokens = 0;
    private CClass cClass;
    private List<CStatement> cStatements = new ArrayList<>();
    private TreeSet<Integer> shingleSet = new TreeSet<>();
    private ArrayList<Integer> shingleSignature = new ArrayList<>();
    private PsiMethod psiMethod;
    private int ccScore;

    public String toString() {
        return String.format("idx:%d  name:%s", idx, name);
    }
}
