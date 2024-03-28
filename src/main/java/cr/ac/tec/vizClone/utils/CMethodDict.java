package cr.ac.tec.vizClone.utils;

import com.intellij.openapi.util.text.LineColumn;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import cr.ac.tec.vizClone.model.CClass;
import cr.ac.tec.vizClone.model.CMethod;
import cr.ac.tec.vizClone.model.CStatement;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class CMethodDict {
    private static List<CMethod> methodArray = new ArrayList<>();
    private static Hashtable<String, Integer> methodDict = new Hashtable<>();

    static public void reset() {
        methodArray = new ArrayList<>();
        methodDict = new Hashtable<>();
    }

    static public Integer getMethodIdx(PsiMethod psiMethod, List<LineColumn> lineColumns) {
        PsiClass containingClass = psiMethod.getContainingClass();
        String methodSignature =  (containingClass == null ? "" : containingClass.getQualifiedName() + ".")
                + psiMethod.getName() + psiMethod.getParameterList().getText();
        Integer index = methodDict.get(methodSignature);
        if (index == null) {
            // retrieve class
            CClass cClass = CClassDict.getClass(psiMethod.getContainingClass(), lineColumns);
            // initialize method
            CMethod cMethod = new CMethod();
            index = methodArray.size();
            cMethod.setIdx(index);
            cMethod.setCClass(cClass);
            cMethod.setPsiMethod(psiMethod);
            cMethod.setName(psiMethod.getName());
            cMethod.setSignature(methodSignature);
            cMethod.setFromOffset(psiMethod.getTextRange().getStartOffset());
            cMethod.setToOffset(psiMethod.getTextRange().getEndOffset());
            cMethod.setFromLineColumn(lineColumns.get(cMethod.getFromOffset()));
            cMethod.setToLineColumn(lineColumns.get(cMethod.getToOffset()));
            // add method
            methodArray.add(cMethod);
            methodDict.put(methodSignature, cMethod.getIdx());
        }
        return index;
    }

    static public CMethod getMethod(PsiMethod psiMethod, List<LineColumn> lineColumns) {
        return methodArray.get(getMethodIdx(psiMethod, lineColumns));
    }

    static public CMethod getMethod(Integer methodIdx) {
        if (methodIdx >= methodArray.size())
            return null;
        else
            return methodArray.get(methodIdx);
    }

    static public CMethod getMethod(String methodSignature) {
        CMethod cMethod = null;
        Integer methodIdx = methodDict.get(methodSignature);
        if (methodIdx != null) {
            cMethod = methodArray.get(methodIdx);
        }
        return cMethod;
    }

    static public Hashtable<String, Integer> dict() {
        return methodDict;
    }

    static public List<CMethod> list() {
        return methodArray;
    }

    static public void addStatement(Integer methodIdx, CStatement cStatement) {
        CMethod cMethod = getMethod(methodIdx);
        if (cMethod != null) {
            cMethod.getCStatements().add(cStatement);
        }
    }

    static public void sumStatementTokens(Integer methodIdx, CStatement cStatement) {
        CMethod cMethod = getMethod(methodIdx);
        if (cMethod != null) {
            cMethod.setNumTokens(cMethod.getNumTokens() + cStatement.getTokens().size());
        }
    }
}
