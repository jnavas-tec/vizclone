package cr.ac.tec.vizClone.model;

import com.intellij.psi.PsiPackage;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CPackage {
    private Integer idx;
    private String name;
    private String signature;
    private List<CClass> cClasses = new ArrayList<>();
    private PsiPackage psiPackage;

    public String toString() {
        return String.format("idx:%d  signature:%s", idx, signature);
    }
}
