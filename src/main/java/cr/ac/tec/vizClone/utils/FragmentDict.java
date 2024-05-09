package cr.ac.tec.vizClone.utils;

import cr.ac.tec.vizClone.model.*;

import java.util.*;

public class FragmentDict {
    private static List<Fragment> fragmentArray = new ArrayList<>();
    private static Map<FragmentKey, Integer> fragmentDict = new Hashtable<>();
    private static List<Method> methodArray = new ArrayList<>();

    static public void reset() {
        fragmentArray.clear();
        fragmentDict.clear();
    }

    static private void collectFragment(Fragment fragment) {
        FragmentKey fk;
        // add fragment
        fragmentArray.add(fragment);
        fk = new FragmentKey(fragment.getCMethod().getIdx(),
            fragment.getCMethod().getCStatements().get(fragment.getFromStatement()).getFromOffset(),
            fragment.getCMethod().getCStatements().get(fragment.getToStatement()).getToOffset());
        // initialize fragment
        fragment.setKey(fk);
    }

    static private void sortFragments() {
        Collections.sort(fragmentArray, new Comparator<Fragment>() {
            @Override
            public int compare(Fragment o1, Fragment o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
    }

    static private void indexFragments() {
        for (int f = 0; f < fragmentArray.size(); f++) {
            Fragment fragment = fragmentArray.get(f);
            fragment.setIdx(f);
            fragment.getKey().setIdx(f);
            fragmentDict.put(fragment.getKey(), f);
        }
    }

    static public Fragment getFragment(Integer fragmentIdx) {
        if (fragmentIdx >= fragmentArray.size())
            return null;
        else return fragmentArray.get(fragmentIdx);
    }

    static private void collectMethods() {
        int lastMethodIdx = -1;
        int methodIdx = 0;
        Method method = null;
        for (int f = 0; f < fragmentArray.size(); f++) {
            Fragment fragment = fragmentArray.get(f);
            ClonePair clonePair = fragment.getClonePair();
            if (lastMethodIdx != fragment.getCMethod().getIdx()) {
                lastMethodIdx = fragment.getCMethod().getIdx();
                method = new Method();
                method.setIdx(methodIdx++);
                method.setCMethodIdx(lastMethodIdx);
                method.setCloneIdx(fragment.getClone().getIdx());
                method.setMaxWeight(clonePair.getWeight());
                method.setMaxSim(clonePair.getSim());
                method.setMaxLevel(clonePair.getLevel());
                method.setMaxCloneType(clonePair.getCloneType());
                methodArray.add(method);
            }
            else {
                method.setMaxWeight(Math.max(method.getMaxWeight(), clonePair.getWeight()));
                method.setMaxSim(Math.max(method.getMaxSim(), clonePair.getSim()));
                method.setMaxLevel(Math.max(method.getMaxLevel(), clonePair.getLevel()));
                method.setMaxCloneType(Math.max(method.getMaxCloneType(), clonePair.getCloneType()));
            }
        }
    }

    static public void collectFragments(List<Clone> clones, List<Fragment> fragments, List<Method> methods) {
        reset();
        for (Clone clone : clones)
            for (ClonePair clonePair : clone.getClonePairs())
                for (Fragment fragment : clonePair.getFragments())
                    collectFragment(fragment);
        sortFragments();
        indexFragments();
        fragments.addAll(fragmentArray);
        collectMethods();
        methods.addAll(methodArray);
    }
}
