package cr.ac.tec.vizClone;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiUtilCore;
import cr.ac.tec.vizClone.model.CMethod;
import cr.ac.tec.vizClone.model.Clone;
import cr.ac.tec.vizClone.model.ClonePair;
import cr.ac.tec.vizClone.model.Fragment;
import cr.ac.tec.vizClone.utils.*;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class CloneCollector {

    private ArrayList<Clone> clones = new ArrayList<>();
    private ArrayList<Fragment> fragments = new ArrayList<>();

    private void collectJavaClasses(@NotNull Project project) {
        this.resetDictionaries();
        String projectName = project.getName();
        VirtualFile[] vSourceRoots = ProjectRootManager.getInstance(project)
                .getContentSourceRoots();
        List<VirtualFile> javaVFilesList = Arrays.stream(vSourceRoots)
                .map(vFile -> CloneCollector.collectJavaClassesInFolder(vFile.getPath(), vFile))
                .collect(Collectors.toList())
                .stream()
                .flatMap(l -> l.stream())
                .collect(Collectors.toList());
        List<PsiFile> javaPsiFilesList = PsiUtilCore.toPsiFiles(PsiManager.getInstance(project), javaVFilesList);

        javaPsiFilesList.stream()
                .forEach(javaPsiFile -> javaPsiFile.accept(new JavaCloneRecursiveElementVisitor(this)));
    }

    private static List<VirtualFile> collectJavaClassesInFolder(@NotNull String sourcePath, @NotNull VirtualFile vFolder) {
        VirtualFile[] childrenVFiles = vFolder.getChildren();
        List<VirtualFile> javaVFilesList = Arrays.stream(childrenVFiles)
                .filter(vf -> !vf.isDirectory() && vf.getExtension() != null && vf.getExtension().equals("java"))
                .collect(Collectors.toList());
        javaVFilesList.addAll(
                Arrays.stream(childrenVFiles)
                        .filter(vf -> vf.isDirectory())
                        .map(vFile -> CloneCollector.collectJavaClassesInFolder(sourcePath, vFile))
                        .collect(Collectors.toList())
                        .stream()
                        .flatMap(l -> l.stream())
                        .collect(Collectors.toList()));
        return javaVFilesList;
    }

    public void collectJavaClones(@NotNull Project project) {
        clones = new ArrayList<>();
        fragments = new ArrayList<>();
        collectJavaClasses(project);

        List<CMethod> methods = CMethodDict.list();
        int numMethods = methods.size() - 1;
        SmithWatermanGotoh swg = new SmithWatermanGotoh();
        this.clones = new ArrayList<>();
        Clone clone;
        for (int i = 0; i < numMethods; i++) {
            CMethod methodA = methods.get(i);
            for (int j = i + 1; j <= numMethods; j++) {
                swg.init();
                CMethod methodB = methods.get(j);
                if (swg.config(methodA, methodB, 0.75, 0.75, 50, 10)) {
                    clone = swg.getClone();
                    if (clone != null) {
                        clone.setIdx(clones.size());
                        clones.add(clone);
                    }
                }
                swg.release();
            }
        }
        this.mergeClonePairs(50);
    }

    private void mergeClonePairs(int overlapPercentage) {
        this.collectAndSortFragments();
        this.mergeFragments(overlapPercentage);
    }

    private void collectAndSortFragments() {
        clones
            .stream()
            .map(clone -> clone.getClonePairs())
            .flatMap(l -> l.stream())
            .forEach(clonePair -> {
                fragments.add(clonePair.getFragments().get(0));
                fragments.add(clonePair.getFragments().get(1));
            });
        Collections.sort(fragments, new Comparator<Fragment>() {
            public int compare(Fragment f1, Fragment f2) {
                return f1.getKey().compareTo(f2.getKey());
            }
        });
    }

    private Hashtable<Integer, Integer> collectFragmentsMap() {
        Hashtable<Integer, Integer> fragmentsMap = new Hashtable<>();
        for (int f = 0; f < fragments.size(); f++) {
            Fragment fragment = fragments.get(f);
            int idx = f;
            if (fragmentsMap.containsKey(fragment.getCMethod().getIdx()))
                idx = Math.min(fragmentsMap.get(fragment.getCMethod().getIdx()), f);
            fragmentsMap.put(fragment.getCMethod().getIdx(), idx);
        }
        return fragmentsMap;
    }

    // Pre: fragment list must be sorted by FragmentKey(method, fromOffset, toOffset)
    // Merge:
    //     c1 -> { cp1 -> f1(A) - f2(B) }
    //     c2 -> { cp2 -> f3(A) - f4(C) }
    //     c3 -> { cp3 -> f5(B) - f6(C) }
    // Into:
    //     c1 -> { cp1 -> f1(A) - f2(B),   c2 -> cp2 -> f3(A) - f4(C),   c3 -> cp3 -> f5(B) - f6(C) }
    private void mergeFragments(int overlapPercentage) {
        Hashtable<Integer, Integer> fragmentsMap = this.collectFragmentsMap();
        for (int i = 0; i < fragments.size() - 1; i++) {
            // fragment A from c1 and cp1
            Fragment f1 = fragments.get(i);
            if (f1.isMerged()) continue;
            for (int j = i + 1; j < fragments.size(); j++) {
                // fragment A from c2 and cp2
                Fragment f3 = fragments.get(j);
                if (!f1.fromSameMethod(f3)) break;
                if (f3.isMerged()) continue;
                // if the two fragments can be merged
                if (f1.canBeMerged(f3, overlapPercentage)) {
                    // move second clone pair and methods to first clone and fix references
                    Clone c1 = f1.getClone();
                    Clone c2 = f3.getClone();
                    ClonePair cp2 = f3.getClonePair();
                    c2.fixClonePairs(cp2.getIdxOnClone());
                    c2.setNumberOfClonePairs(c2.getClonePairs().size());
                    cp2.setClone(c1);
                    cp2.setIdxOnClone(c1.getClonePairs().size());
                    cp2.setIdx(c1.getClonePairs().size());
                    c1.getClonePairs().add(cp2);
                    c1.setNumberOfClonePairs(c1.getClonePairs().size());
                    c1.setMaxWeight(Math.max(c1.getMaxWeight(), cp2.getWeight()));
                    c1.getMethods().addAll(c2.getMethods());
                    c2.getMethods().clear();
                    // mark fragment A moved from c2 to c1 as merged
                    f3.setMerged(true);
                    // fragment B from c1 and cp1
                    Fragment f2 = f1.getClonePair().getFragments().get(1 - f1.getIdxOnClonePair());
                    // fragment C from c2 and cp2
                    Fragment f4 = f3.getClonePair().getFragments().get(1 - f3.getIdxOnClonePair());
                    // locate third mergeable fragment
                    ClonePair cp3 = findMergeableClonePair(fragmentsMap, f2, f4, overlapPercentage);
                    if (cp3 != null) {
                        // fragments B and C from c3 and cp3
                        Fragment f5 = cp3.getFragments().get(0);
                        Fragment f6 = cp3.getFragments().get(1);
                        // move third clone pair to first clone and fix references
                        Clone c3 = f5.getClone();
                        c3.fixClonePairs(cp3.getIdxOnClone());
                        c3.setNumberOfClonePairs(c3.getClonePairs().size());
                        cp3.setClone(c1);
                        cp3.setIdxOnClone(c1.getClonePairs().size());
                        cp3.setIdx(c1.getClonePairs().size());
                        c1.getClonePairs().add(cp3);
                        c1.setNumberOfClonePairs(c1.getClonePairs().size());
                        c1.setMaxWeight(Math.max(c1.getMaxWeight(), cp3.getWeight()));
                        c1.getMethods().addAll(c3.getMethods());
                        c3.getMethods().clear();
                    }
                }
            }
        }
        // remove clones with no clone pairs
        for (int c = clones.size() - 1; c >= 0; c--) {
            if (clones.get(c).getClonePairs().size() == 0) {
                clones.remove(c);
            }
        }
        // fix indexes
        for (int c = 0; c < clones.size(); c++) {
            clones.get(c).setIdx(c);
        }
    }

    private ClonePair findMergeableClonePair(Hashtable<Integer, Integer> fragmentsMap,
                                             Fragment f2, Fragment f4, int overlapPercentage) {
        int numFrags = fragments.size();
        for (int f = f2.getCMethod().getIdx(); f < numFrags && f2.fromSameMethod(fragments.get(f)); f++) {
            ClonePair cp3 = fragments.get(f).getClonePair();
            int f5Side = f2.fromSameMethod(cp3.getFragments().get(0)) ? 0 : 1;
            int f6Side = 1 - f5Side;
            Fragment f5 = cp3.getFragments().get(f5Side);
            Fragment f6 = cp3.getFragments().get(f6Side);
            if (f2.canBeMerged(f5, overlapPercentage) || f4.canBeMerged(f6, overlapPercentage))
                return cp3;
        }
        return null;
    }

    private void resetDictionaries() {
        CClassDict.reset();
        CMethodDict.reset();
        CPackageDict.reset();
        FragmentDict.reset();
        NestedSentenceDict.reset();
        StatementDict.reset();
        TokenDict.reset();
    }
}
