package cr.ac.tec.vizClone.utils;

import cr.ac.tec.vizClone.model.CMethod;
import cr.ac.tec.vizClone.model.Fragment;
import cr.ac.tec.vizClone.model.FragmentKey;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class FragmentDict {
    private static List<Fragment> fragmentArray = new ArrayList<>();
    private static Hashtable<FragmentKey, Integer> fragmentDict = new Hashtable<>();

    static public void reset() {
        fragmentArray = new ArrayList<>();
        fragmentDict = new Hashtable<>();
    }

    static public Integer getFragmentIdx(CMethod cMethod, int fromStatement, int toStatement) {
        Integer index = fragmentArray.size();
        FragmentKey fk = new FragmentKey(cMethod.getIdx(), cMethod.getSignature(),
                                         cMethod.getCStatements().get(fromStatement).getFromOffset(),
                                         cMethod.getCStatements().get(toStatement).getToOffset(),
                                         index);
        // initialize fragment
        Fragment fragment = new Fragment();
        fragment.setKey(fk);
        fragment.setCPackage(cMethod.getCClass().getCPackage());
        fragment.setCClass(cMethod.getCClass());
        fragment.setCMethod(cMethod);
        fragment.setIdx(index);
        fragment.setFromStatement(fromStatement);
        fragment.setToStatement(toStatement);
        fragment.setNumberOfStatements(toStatement - fromStatement + 1);
        fragment.setFromOffset(fk.getFromOffset());
        fragment.setToOffset(fk.getToOffset());
        fragment.setFromLineColumn(cMethod.getCStatements().get(fromStatement).getFromLineColumn());
        fragment.setToLineColumn(cMethod.getCStatements().get(toStatement).getToLineColumn());
        // add fragment
        fragmentArray.add(fragment);
        fragmentDict.put(fk, index);
        return index;
    }

    static public Fragment getFragment(CMethod cMethod, int fromSentence, int toSentence) {
        return fragmentArray.get(getFragmentIdx(cMethod, fromSentence, toSentence));
    }

    static public Fragment getFragment(Integer fragmentIdx) {
        if (fragmentIdx >= fragmentArray.size())
            return null;
        else return fragmentArray.get(fragmentIdx);
    }
}
