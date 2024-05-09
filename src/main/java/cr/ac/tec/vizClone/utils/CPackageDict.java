package cr.ac.tec.vizClone.utils;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import cr.ac.tec.vizClone.model.CClass;
import cr.ac.tec.vizClone.model.CMethod;
import cr.ac.tec.vizClone.model.CPackage;

import java.util.*;

public class CPackageDict {
    private static List<CPackage> packagesArray = new ArrayList<>();
    private static Map<String, Integer> packagesDict = new Hashtable<>();

    static public void reset() {
        packagesArray.clear();
        packagesDict.clear();
    }

    static public Integer getPackageIdx(PsiPackage psiPackage) {
        String qualifiedName = psiPackage.getQualifiedName();
        if (qualifiedName == null) qualifiedName = psiPackage.getName();
        Integer index = packagesDict.get(qualifiedName);
        if (index == null) {
            // initialize package
            CPackage cPackage = new CPackage();
            cPackage.setPsiPackage(psiPackage);
            cPackage.setName(psiPackage.getName());
            cPackage.setSignature(qualifiedName);
            // add package
            index = packagesArray.size();
            packagesArray.add(cPackage);
            packagesDict.put(qualifiedName, index);
            cPackage.setIdx(index);
        }
        return index;
    }

    static public CPackage getPackage(PsiPackage psiPackage) {
        return packagesArray.get(getPackageIdx(psiPackage));
    }

    static public CPackage getPackage(Integer packageIdx) {
        if (packageIdx >= packagesArray.size())
            return null;
        else
            return packagesArray.get(packageIdx);
    }

    static public CPackage getPackage(String packageSignature) {
        CPackage cPackage = null;
        Integer packageIdx = packagesDict.get(packageSignature);
        if (packageIdx != null) {
            cPackage = packagesArray.get(packageIdx);
        }
        return cPackage;
    }

    static public Map<String, Integer> dict() {
        return packagesDict;
    }

    static public List<CPackage> list() {
        return packagesArray;
    }

    static public void addClass(Integer packageIdx, CClass cClass) {
        CPackage cPackage = getPackage(packageIdx);
        if (cPackage != null) {
            cPackage.getCClasses().add(cClass);
        }
    }
}
