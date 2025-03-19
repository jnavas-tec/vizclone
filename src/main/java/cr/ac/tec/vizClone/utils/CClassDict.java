package cr.ac.tec.vizClone.utils;

import com.intellij.openapi.util.text.LineColumn;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.JavaFileElementType;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.util.PsiUtil;
import cr.ac.tec.vizClone.model.CClass;
import cr.ac.tec.vizClone.model.CMethod;
import cr.ac.tec.vizClone.model.CStatement;

import java.util.*;

public class CClassDict {
    private static List<CClass> classArray = new ArrayList<>();
    private static Map<String, Integer> classDict = new Hashtable<>();
    private static Integer anonCount = 1;

    static public void reset() {
        classArray.clear();
        classDict.clear();
    }

    static private String getFolder(String folderPath) {
        return folderPath.substring(folderPath.indexOf("/src/") + 5).replaceAll("/", ".");
    }

    static private String getParentClasses(PsiElement psiClass) {
        String parentClasses = "";
        if (psiClass.getParent().getNode().getElementType().equals(JavaElementType.CLASS)) {
            parentClasses = getParentClasses(psiClass.getParent());
            if (parentClasses.length() > 0) {
                parentClasses = parentClasses + ".";
            }
        }
        return parentClasses + ((PsiClass)psiClass).getName();
    }

    static private String getQualifiedClassName(PsiClass psiClass) {
        String packageName = ((PsiJavaFileImpl)psiClass.getContainingFile()).getPackageName() + ".";
        String qualifiedClassName = "";
        PsiElement psiElement = psiClass.getParent();
        while (psiElement != null) {
            if (psiElement.getNode() != null && psiElement.getNode().getElementType() == JavaElementType.CLASS) {
                PsiClass clazz = (PsiClass)psiElement;
                if (qualifiedClassName.length() > 0)
                    qualifiedClassName = clazz.getName() + "." + qualifiedClassName;
                else
                    qualifiedClassName = clazz.getName();
            }
            psiElement = psiElement.getParent();
        }
        return packageName + qualifiedClassName;
    }

    static public Integer getClassIdx(PsiClass psiClass, List<LineColumn> lineColumns, boolean folderAsPackage) {
        String qualifiedName = "";
        String className = psiClass.getName() == null ? "ANON" + (anonCount++).toString() : getParentClasses(psiClass);
        if (folderAsPackage) {
            String filename = psiClass.getContainingFile().getName();
            filename = filename.substring(0, filename.length() - 5);
            String directory = getFolder(psiClass.getContainingFile().getContainingDirectory().toString());
            qualifiedName = directory + "." + filename + "." + className;
        }
        else {
            //qualifiedName = psiClass.getQualifiedName();
            if (psiClass.getName() == null)
                qualifiedName = getQualifiedClassName(psiClass) + "." + className;
            else
                qualifiedName = getQualifiedClassName(psiClass) + "." + psiClass.getName();
            //if (qualifiedName == null) qualifiedName = className;
        }
        Integer index = classDict.get(qualifiedName);
        if (index != null) {
            Integer idx = 0;
            String nextName = qualifiedName + "." + idx++;
            index = classDict.get(nextName);
            while (index != null) {
                nextName = qualifiedName + "." + idx++;
                index = classDict.get(nextName);
            }
            qualifiedName = nextName;
        }

        if (index == null) {
            // retrieve package
            String packageName = PsiUtil.getPackageName(psiClass);
            PsiPackage psiPackage = JavaPsiFacade.getInstance(psiClass.getProject()).findPackage(packageName);
            // initialize class
            CClass cClass = new CClass();
            cClass.setCPackage(psiPackage == null ? null : CPackageDict.getPackage(psiPackage));
            cClass.setPsiClass(psiClass);
            cClass.setName(className);
            cClass.setSignature(qualifiedName);
            cClass.setFromOffset(psiClass.getTextRange().getStartOffset());
            cClass.setToOffset(psiClass.getTextRange().getEndOffset() - 1);
            cClass.setFromLineColumn(lineColumns.get(cClass.getFromOffset()));
            cClass.setToLineColumn(lineColumns.get(cClass.getToOffset()));
            if (cClass.getCPackage() != null) CPackageDict.addClass(cClass.getCPackage().getIdx(), cClass);
            // add class
            index = classArray.size();
            classArray.add(cClass);
            classDict.put(qualifiedName, index);
            cClass.setIdx(index);
        }
        return index;
    }

    static public CClass getClass(PsiClass psiClass, List<LineColumn> lineColumns, boolean folderAsPackage) {
        return classArray.get(getClassIdx(psiClass, lineColumns, folderAsPackage));
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

    static public Map<String, Integer> dict() {
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
