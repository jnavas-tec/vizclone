package cr.ac.tec.vizClone.utils;

import com.intellij.openapi.util.text.LineColumn;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.source.tree.JavaElementType;
import cr.ac.tec.vizClone.model.CClass;
import cr.ac.tec.vizClone.model.CMethod;
import cr.ac.tec.vizClone.model.CStatement;

import java.util.*;

public class CMethodDict {
    private static List<CMethod> methodArray = new ArrayList<>();
    private static Map<String, Integer> methodDict = new Hashtable<>();

    static public void reset() {
        methodArray.clear();
        methodDict.clear();
    }

    static public Integer getMethodIdx(PsiMethod psiMethod, List<LineColumn> lineColumns, CClass cClass, boolean folderAsPackage) {
        String methodSignature = "";
        CClass cClazz = null;
        if (folderAsPackage) {
            cClazz = cClass;
            methodSignature = cClazz.getSignature() + "." + psiMethod.getName() + psiMethod.getParameterList().getText();
            Integer idx = 0;
            while (methodDict.get(methodSignature) != null)
                methodSignature = cClazz.getSignature() + "." + psiMethod.getName() + idx++ + psiMethod.getParameterList().getText();
        }
        else {
            PsiClass containingClass = psiMethod.getContainingClass();
            String qualifiedName = containingClass.getQualifiedName();
            if (qualifiedName == null) qualifiedName = containingClass.getName();
            methodSignature = (containingClass == null ? "" : qualifiedName + ".")
                + psiMethod.getName() + psiMethod.getParameterList().getText();
            // retrieve class
            cClazz = CClassDict.getClass(psiMethod.getContainingClass(), lineColumns, false);
        }
        Integer index = methodDict.get(methodSignature);
        if (index == null) {
            // initialize method
            CMethod cMethod = new CMethod();
            index = methodArray.size();
            methodArray.add(cMethod);
            cMethod.setIdx(index);
            cMethod.setCClass(cClazz);
            cMethod.setPsiMethod(psiMethod);
            cMethod.setName(psiMethod.getName());
            cMethod.setSignature(methodSignature);
            cMethod.setFromOffset(psiMethod.getTextRange().getStartOffset());
            cMethod.setToOffset(psiMethod.getTextRange().getEndOffset() - 1);
            cMethod.setFromLineColumn(lineColumns.get(cMethod.getFromOffset()));
            cMethod.setToLineColumn(lineColumns.get(cMethod.getToOffset()));
            methodDict.put(methodSignature, cMethod.getIdx());
        }
        return index;
    }

    static public CMethod getMethod(PsiMethod psiMethod, List<LineColumn> lineColumns, CClass cClass, boolean folderAsPackage) {
        return methodArray.get(getMethodIdx(psiMethod, lineColumns, cClass, folderAsPackage));
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

    static public void removeMethod(CMethod method) {
        int idx = methodDict.get(method.getSignature());
        methodArray.remove((int) methodDict.get(method.getSignature()));
        methodDict.remove(method.getSignature());
        for (;idx < methodArray.size(); idx++) {
            methodArray.get(idx).setIdx(idx);
            methodDict.put(method.getSignature(), idx);
        }
    }

    static public Map<String, Integer> dict() {
        return methodDict;
    }

    static public List<CMethod> list() {
        return methodArray;
    }

    static public void addStatement(Integer methodIdx, CStatement cStatement) {
        CMethod cMethod = getMethod(methodIdx);
        if (cMethod != null) {
            cMethod.getCStatements().add(cStatement);
            /*
            int size = cMethod.getCStatements().size();
            if (!JavaElementType.BLOCK_STATEMENT.equals(cStatement.getPsiStatement().getNode().getElementType()) && size > 1) {
                int leftFrom = cMethod.getCStatements().get(size-2).getFromLineColumn().line;
                int leftTo = cMethod.getCStatements().get(size-2).getToLineColumn().line;
                int rightFrom = cMethod.getCStatements().get(size-1).getFromLineColumn().line;
                int rightTo = cMethod.getCStatements().get(size-1).getToLineColumn().line;
                if (leftFrom > rightFrom || leftTo > rightTo || leftFrom > rightTo || leftTo > rightFrom) {
                    boolean WTF = true;
                }
            }
             */
        }
    }

    static public void sumStatementTokens(Integer methodIdx, CStatement cStatement) {
        CMethod cMethod = getMethod(methodIdx);
        if (cMethod != null) {
            cMethod.setNumTokens(cMethod.getNumTokens() + cStatement.getTokens().size());
        }
    }
}
