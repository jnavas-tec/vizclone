package cr.ac.tec.vizClone.model;

import com.intellij.openapi.util.text.LineColumn;
import com.intellij.psi.PsiClass;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CClass {
    private Integer idx;
    private String name;
    private String signature;
    private LineColumn fromLineColumn;
    private LineColumn toLineColumn;
    private Integer fromOffset;
    private Integer toOffset;
    private CPackage cPackage;
    private List<CMethod> cMethods = new ArrayList<>();
    private PsiClass psiClass;

    public String toString() {
        return String.format("idx:%d  name:%s", idx, name);
    }
}
