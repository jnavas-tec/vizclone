package cr.ac.tec.vizClone.utils;

import cr.ac.tec.vizClone.model.*;

import java.util.*;
import java.util.stream.IntStream;

public class FragmentDict {
    private static List<Fragment> fragmentArray = new ArrayList<>();
    private static Map<FragmentKey, Integer> fragmentDict = new Hashtable<>();
    private static List<Method> methodArray = new ArrayList<>();

    static public void reset() {
        fragmentArray.clear();
        fragmentDict.clear();
        methodArray.clear();
    }

    static private void collectFragment(Fragment fragment) {
        FragmentKey fk;
        // add fragment
        fragmentArray.add(fragment);
        fk = new FragmentKey(fragment.getCMethod().getIdx(),
            fragment.getCMethod().getCStatements().get(fragment.getFromStatement()).getFromOffset(),
            fragment.getCMethod().getCStatements().get(fragment.getToStatement()).getToOffset(),
            fragment.getIdx());
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

    static public void indexFragments() {
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
                // create method for ALL METHODS list
                method = new Method();
                method.setIdx(methodIdx++);
                method.setCMethodIdx(lastMethodIdx);
                method.setMaxWeight(clonePair.getWeight());
                method.setMaxSim(clonePair.getSim());
                method.setMaxLevel(clonePair.getLevel());
                method.setMaxCloneType(clonePair.getCloneType());
                method.setCcScore(fragment.getCMethod().getCcScore());
                method.setFromStatement(fragment.getFromStatement());
                method.setToStatement(fragment.getToStatement());
                methodArray.add(method);
                //---------------------------------------------------------------//
                // create method for CLONE METHODS list
                Method cloneMethod = new Method();
                ArrayList<Method> cloneMethods = clonePair.getClone().getMethods();
                cloneMethod.setIdx(method.getIdx());
                cloneMethod.setCMethodIdx(method.getCMethodIdx());
                cloneMethod.setCloneIdx(clonePair.getClone().getIdx());
                cloneMethod.setMaxWeight(clonePair.getWeight());
                cloneMethod.setMaxSim(clonePair.getSim());
                cloneMethod.setMaxLevel(clonePair.getLevel());
                cloneMethod.setMaxCloneType(clonePair.getCloneType());
                //cloneMethod.setCcScore(clonePair.getMaxCognitiveComplexity());
                cloneMethod.setFromStatement(fragment.getFromStatement());
                cloneMethod.setToStatement(fragment.getToStatement());
                cloneMethods.add(cloneMethod);
            }
            else {
                // accummulate method for ALL METHODS list
                method.setMaxWeight(Math.max(method.getMaxWeight(), clonePair.getWeight()));
                method.setMaxSim(Math.max(method.getMaxSim(), clonePair.getSim()));
                method.setMaxLevel(Math.max(method.getMaxLevel(), clonePair.getLevel()));
                method.setMaxCloneType(Math.max(method.getMaxCloneType(), clonePair.getCloneType()));
                method.setFromStatement(Math.min(method.getFromStatement(), fragment.getFromStatement()));
                method.setToStatement(Math.max(method.getToStatement(), fragment.getToStatement()));
                ArrayList<Method> cloneMethods = clonePair.getClone().getMethods();
                //---------------------------------------------------------------//
                if (cloneMethods.contains(method)) {
                    // accummulate method for CLONE METHODS list
                    final Method finalMethod = method;
                    Method cloneMethod = cloneMethods.stream()
                        .filter(m -> m.getCMethodIdx() == finalMethod.getCMethodIdx())
                        .findFirst().get();
                    cloneMethod.setMaxWeight(Math.max(cloneMethod.getMaxWeight(), clonePair.getWeight()));
                    cloneMethod.setMaxSim(Math.max(cloneMethod.getMaxSim(), clonePair.getSim()));
                    cloneMethod.setMaxLevel(Math.max(cloneMethod.getMaxLevel(), clonePair.getLevel()));
                    cloneMethod.setMaxCloneType(Math.max(cloneMethod.getMaxCloneType(), clonePair.getCloneType()));
                    cloneMethod.setFromStatement(Math.min(cloneMethod.getFromStatement(), fragment.getFromStatement()));
                    cloneMethod.setToStatement(Math.max(cloneMethod.getToStatement(), fragment.getToStatement()));
                    //cloneMethod.setCcScore(Math.max(cloneMethod.getCcScore(), clonePair.getMaxCognitiveComplexity()));
                }
                else {
                    // create method for CLONE METHODS list
                    Method cloneMethod = new Method();
                    cloneMethod.setIdx(method.getIdx());
                    cloneMethod.setCMethodIdx(method.getCMethodIdx());
                    cloneMethod.setCloneIdx(clonePair.getClone().getIdx());
                    cloneMethod.setMaxWeight(clonePair.getWeight());
                    cloneMethod.setMaxSim(clonePair.getSim());
                    cloneMethod.setMaxLevel(clonePair.getLevel());
                    cloneMethod.setMaxCloneType(clonePair.getCloneType());
                    //cloneMethod.setCcScore(clonePair.getMaxCognitiveComplexity());
                    cloneMethods.add(cloneMethod);
                }
            }
        }
    }

    static private void fixCloneMethodsCCScore(List<Clone> clones) {
        // fixes clone's method's ccScore to from/to statements score
        clones.parallelStream()
            .forEach(clone -> {
                clone.setMaxCognitiveComplexity(0);
                clone.getMethods().stream()
                    .forEach(method -> {
                        List<CStatement> statements = CMethodDict.list().get(method.getCMethodIdx()).getCStatements();
                        int ccScore = IntStream
                            .range(0, statements.size())
                            .filter(i -> method.getFromStatement() <= i && i <= method.getToStatement())
                            .map(i -> statements.get(i).getCcScore())
                            .reduce(0, Integer::sum);
                        method.setCcScore(ccScore);
                        Method globalMethod = methodArray.get(method.getIdx());
                        globalMethod.setCcScore(Math.max(ccScore, globalMethod.getCcScore()));
                        clone.setMaxCognitiveComplexity(Math.max(ccScore, clone.getMaxCognitiveComplexity()));
                    });
            });
    }

    static public void collectFragments(List<Clone> clones, List<Fragment> fragments, List<Method> methods) {
        reset();
        for (Clone clone : clones) {
            for (ClonePair clonePair : clone.getClonePairs())
                for (Fragment fragment : clonePair.getFragments())
                    collectFragment(fragment);
        }
        sortFragments();
        indexFragments();
        fragments.addAll(fragmentArray);
        collectMethods();
        fixCloneMethodsCCScore(clones);
        methods.addAll(methodArray);
        clones.stream()
            .forEach(clone -> {
                if (clone.getMethods().size() == 0) {
                    boolean ouch = true;
                }
            });
    }
}
