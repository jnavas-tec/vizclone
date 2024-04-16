package cr.ac.tec.vizClone.utils;

import com.intellij.openapi.util.text.LineColumn;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PsiUtil;
import cr.ac.tec.vizClone.model.CClass;
import cr.ac.tec.vizClone.model.CMethod;
import cr.ac.tec.vizClone.model.CStatement;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class CClassDict {
    private static List<CClass> classArray = new ArrayList<>();
    private static Hashtable<String, Integer> classDict = new Hashtable<>();

    static public void reset() {
        classArray = new ArrayList<>();
        classDict = new Hashtable<>();
    }

    static public Integer getClassIdx(PsiClass psiClass, List<LineColumn> lineColumns) {
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) qualifiedName = psiClass.getName();
        Integer index = classDict.get(qualifiedName);
        if (index == null) {
            // retrieve package
            String packageName = PsiUtil.getPackageName(psiClass);
            PsiPackage psiPackage = JavaPsiFacade.getInstance(psiClass.getProject()).findPackage(packageName);
            // initialize class
            CClass cClass = new CClass();
            cClass.setCPackage(psiPackage == null ? null : CPackageDict.getPackage(psiPackage));
            cClass.setPsiClass(psiClass);
            cClass.setName(psiClass.getName());
            cClass.setSignature(qualifiedName);
            cClass.setFromOffset(psiClass.getTextRange().getStartOffset());
            cClass.setToOffset(psiClass.getTextRange().getEndOffset() - 1);
            cClass.setFromLineColumn(lineColumns.get(cClass.getFromOffset()));
            cClass.setToLineColumn(lineColumns.get(cClass.getToOffset()));
            if (cClass.getCPackage() != null) CPackageDict.addClass(cClass.getCPackage().getIdx(), cClass);
            // add class
            classArray.add(cClass);
            classDict.put(qualifiedName, classArray.size() - 1);
            index = classArray.size() - 1;
            cClass.setIdx(index);
        }
        return index;
    }

    static public CClass getClass(PsiClass psiClass, List<LineColumn> lineColumns) {
        return classArray.get(getClassIdx(psiClass, lineColumns));
    }

    static public CClass getClass(Integer classIdx) {
        if (classIdx >= classArray.size())
            return null;
        else
            return classArray.get(classIdx);
    }

    static public CClass getClass(String classSignature) {
        CClass cClass = null;
        Integer classIdx = classDict.get(classSignature);
        if (classIdx != null) {
            cClass = classArray.get(classIdx);
        }
        return cClass;
    }

    static public Hashtable<String, Integer> dict() {
        return classDict;
    }

    static public List<CClass> list() {
        return classArray;
    }

    static public void addMethod(Integer classIdx, CMethod cMethod) {
        CClass cClass = getClass(classIdx);
        if (cClass != null) {
            cClass.getCMethods().add(cMethod);
        }
    }

    static public void removeMethod(CMethod cMethod) {

    }
}
