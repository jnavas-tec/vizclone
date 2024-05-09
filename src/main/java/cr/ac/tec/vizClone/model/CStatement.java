package cr.ac.tec.vizClone.model;

import com.intellij.openapi.util.text.LineColumn;
import com.intellij.psi.PsiStatement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CStatement {
    private Integer statementId;
    private LineColumn fromLineColumn;
    private LineColumn toLineColumn;
    private Integer fromOffset;
    private Integer toOffset;
    private CMethod cMethod;
    private List<Integer> tokens = new ArrayList<>();
    private PsiStatement psiStatement;
    private String text = "";
}
