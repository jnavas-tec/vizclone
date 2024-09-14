package cr.ac.tec.vizClone;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiUtilCore;
import cr.ac.tec.vizClone.model.*;
import cr.ac.tec.vizClone.utils.*;
import groovy.lang.Tuple2;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
public class CloneCollector {

    public static double MIN_SIM = 0.8; // 0.7;
    public static double MIN_SENT_SIM = 0.8;
    public static int MIN_TOKENS = 100; // 200; //300; // 50;
    public static int MIN_SENT = 15; // 20; //25;  // 7;
    public static int NUM_WEIGHT_LEVELS = 4;
    public static int OVERLAP_PERCENTAGE = 60;
    public static int MIN_WEIGHT = 0;
    public static int MAX_WEIGHT = 100;

    private ArrayList<Clone> clones = new ArrayList<>();
    private ArrayList<Fragment> fragments = new ArrayList<>();
    private ArrayList<Method> methods = new ArrayList<>();

    private LocalDateTime previousDateTime = null;

    private void printLocalDateTime(String message) {
        long elapsedMinutes = 0;
        long elapsedSeconds = 0;
        LocalDateTime currentDateTime = LocalDateTime.now();
        if (previousDateTime != null) {
            elapsedMinutes = previousDateTime.until(currentDateTime, ChronoUnit.MINUTES);
            elapsedSeconds = previousDateTime.until(currentDateTime, ChronoUnit.SECONDS) - elapsedMinutes * 60;
            System.out.println(String.format(" (time ellapsed = %d:%d)", elapsedMinutes, elapsedSeconds));
        }
        if (message.length() > 0) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            System.out.print(String.format("%s: %s", formatter.format(currentDateTime), message));
            previousDateTime = currentDateTime;
        }
        else previousDateTime = null;
    }

    private void printLocalDateTimeLabel(String label) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        System.out.println(String.format("%s: %s", formatter.format(currentDateTime), label));
    }

    private boolean collectMethodsMinhashSignatures(@NotNull Project project) {
        this.resetDictionaries();
        String projectName = project.getName();
        VirtualFile[] vSourceRoots = ProjectRootManager.getInstance(project)
            .getContentSourceRoots();
        printLocalDateTime("Collecting java virtual files...");
        List<VirtualFile> javaVFilesList = Arrays.stream(vSourceRoots)
            .map(vFile -> CloneCollector.collectJavaClassesInFolder(vFile))
            .collect(Collectors.toList())
            .stream()
            .flatMap(l -> l.stream())
            .collect(Collectors.toList());
        printLocalDateTime("Collecting java psi files...");
        List<PsiFile> javaPsiFilesList = PsiUtilCore.toPsiFiles(PsiManager.getInstance(project), javaVFilesList);

        printLocalDateTime("Collecting shingles...");

        CloneConfig cloneConfig = new CloneConfig(true, true);

        //Collections.shuffle(javaPsiFilesList);

        javaPsiFilesList.stream()//.limit(30000)
            .forEach(javaPsiFile -> javaPsiFile.accept(new ShinglingRecursiveElementVisitor(cloneConfig)));

        printLocalDateTime(String.format("Collecting methods from %d Java Psi files...", javaPsiFilesList.size()));
        javaPsiFilesList.stream()//.limit(30000)
            .forEach(javaPsiFile -> javaPsiFile.accept(
                new JavaCloneRecursiveElementVisitor(this, MIN_SENT, MIN_TOKENS, cloneConfig))
            );

        printLocalDateTime(String.format("Collecting minhash signatures for %d methods...", CMethodDict.list().size()));
        //ShingleDict.setMinhashSignatures(CMethodDict.list(), ShingleDict.NUM_MIN_HASHES);
        try {
            ShingleDict.setMinhashSignaturesInFiles(CMethodDict.list(), ShingleDict.b, ShingleDict.r);
            return true;
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static List<VirtualFile> collectJavaClassesInFolder(@NotNull VirtualFile vFolder) {
        VirtualFile[] childrenVFiles = vFolder.getChildren();
        List<VirtualFile> javaVFilesList = Arrays.stream(childrenVFiles)
            .filter(vf -> !vf.isDirectory() && vf.getExtension() != null && vf.getExtension().equals("java"))
            .collect(Collectors.toList());
        javaVFilesList.addAll(
            Arrays.stream(childrenVFiles)
                .filter(vf -> vf.isDirectory())
                .map(vFile -> CloneCollector.collectJavaClassesInFolder(vFile))
                .collect(Collectors.toList())
                .stream()
                .flatMap(l -> l.stream())
                .collect(Collectors.toList()));
        return javaVFilesList;
    }

    private static List<List<VirtualFile>> collectJavaClassesInSubfolders(@NotNull VirtualFile vFolder) {
        VirtualFile[] childrenVFiles = vFolder.getChildren();
        List<List<VirtualFile>> javaVFilesLists = new ArrayList<>();
        /*
        List<VirtualFile> javaVFilesList = Arrays.stream(childrenVFiles)
            .filter(vf -> !vf.isDirectory() && vf.getExtension() != null && vf.getExtension().equals("java"))
            .collect(Collectors.toList());
         */
        javaVFilesLists.addAll(
            Arrays.stream(childrenVFiles)
                .filter(vf -> vf.isDirectory())
                .map(vFile -> CloneCollector.collectJavaClassesInFolder(vFile))
                .collect(Collectors.toList()));
//                .stream()
//                .flatMap(l -> l.stream())
//                .collect(Collectors.toList()));
        return javaVFilesLists;
    }

    static private ArrayList<String> cloneNames = new ArrayList<>(Arrays.asList(
        "org.jetbrains.plugins.groovy.editor.GroovyReferenceCopyPasteProcessor.findReferencesToRestore",
        "org.jetbrains.plugins.groovy.editor.GroovyReferenceCopyPasteProcessor.findReferencesToRestoreClone1",
        "org.jetbrains.plugins.groovy.editor.GroovyReferenceCopyPasteProcessor.findReferencesToRestoreClone2"));

    public void collectBCBClones(@NotNull Project project, double minSim, double minSentSim, int minTokens, int minSent,
                                 int numWeightLevels) {
        // /Users/jnavas/Documents/Academia/Doctorado/Java/Workspace/BigCloneEval/cloneviz
        // ./src/ijadataset/bcb_reduced/<folders>
        MIN_SIM = minSim;
        MIN_SENT_SIM = minSentSim;
        MIN_TOKENS = minTokens;
        MIN_SENT = minSent;
        NUM_WEIGHT_LEVELS = numWeightLevels;

        String projectName = project.getName();
        // src folder
        VirtualFile[] vSourceRoots = ProjectRootManager.getInstance(project)
            .getContentSourceRoots();
        String outputFilePath = vSourceRoots[0].getPath() + "/clones.csv";
        try {
            FileWriter outputWriter = new FileWriter(outputFilePath);
            printLocalDateTime("Collecting java virtual files from reduced BCB dataset...");
            // src/ijadataset/bcb_reduced/<folders>
            List<List<VirtualFile>> javaVFilesLists = Arrays.stream(vSourceRoots)
                .map(vFile -> CloneCollector.collectJavaClassesInSubfolders(
                    vFile.findChild("bcb").findChild("ijadataset").findChild("bcb_reduced")))
                .collect(Collectors.toList())
                .stream()
                .flatMap(l -> l.stream())
                .collect(Collectors.toList());
            printLocalDateTime("Collecting java psi files...");
            List<List<PsiFile>> javaPsiFilesLists = javaVFilesLists.stream()
                .map(vFilesList -> PsiUtilCore.toPsiFiles(PsiManager.getInstance(project), vFilesList))
                .collect(Collectors.toList());
            printLocalDateTime("");

            int i = 1;
            int n = javaPsiFilesLists.size();
            javaPsiFilesLists.sort(Comparator.comparing(List::size, Comparator.reverseOrder()));
            for (List<PsiFile> javaPsiFilesList : javaPsiFilesLists) {
                fragments = new ArrayList<>();
                this.resetDictionaries();

                printLocalDateTimeLabel("Iteration " + String.valueOf(i++) + " of " + String.valueOf(n) +
                    "\n------------------------------------------------------");

                printLocalDateTime("Collecting shingles...");
                CloneConfig cloneConfig = new CloneConfig(true, true);
                javaPsiFilesList.stream()
                    .forEach(javaPsiFile -> javaPsiFile.accept(new ShinglingRecursiveElementVisitor(cloneConfig)));

                printLocalDateTime(String.format("Collecting methods from %d Java Psi files...", javaPsiFilesList.size()));
                javaPsiFilesList.stream()//.limit(30000)
                    .forEach(javaPsiFile -> javaPsiFile.accept(
                        new JavaCloneRecursiveElementVisitor(this, MIN_SENT, MIN_TOKENS, cloneConfig))
                    );

                printLocalDateTime(String.format("Collecting minhash signatures for %d methods...", CMethodDict.list().size()));
                ShingleDict.setMinhashSignaturesInFiles(CMethodDict.list(), ShingleDict.b, ShingleDict.r);

                printLocalDateTime("Collecting LSH for minhash signatures and generating similar pairs...");
                List<CMethod> methods = CMethodDict.list();
                List<Clone> similarPairs = ShingleDict.lshMinhashSignaturesFromFiles(methods, ShingleDict.b, ShingleDict.r);

                printLocalDateTime(String.format("Collecting clones from %d similar pairs...", similarPairs.size()));
                this.clones = (ArrayList<Clone>) similarPairs
                    .parallelStream()
                    .map(this::verifyAndGroupClones)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

                printLocalDateTime(String.format("Fixing fragment indices from %d clones...", this.clones.size()));
                FragmentDict.indexFragments();
                printLocalDateTime(String.format("Merging clone pairs from %d clones...", this.clones.size()));
                for (int c = 0; c < this.clones.size(); c++) this.clones.get(c).fixClone(c);
                this.mergeClonePairs(OVERLAP_PERCENTAGE);
                printLocalDateTime(String.format("Fixing %d collected clones...", this.clones.size()));
                for (int c = 0; c < this.clones.size(); c++) this.clones.get(c).fixClone(c);
                this.calculateCCWeight();
                printLocalDateTime(String.format("Collecting fragments for %d clones...", this.clones.size()));
                FragmentDict.collectFragments(this.clones, this.fragments, this.methods);
                printLocalDateTime("Writing clones found to output file...");
                for (Clone clone: this.clones) {
                    for (ClonePair pair: clone.getClonePairs()) {
                        Fragment f1 = pair.getFragments().get(0);
                        String left = f1.getCClass().getPsiClass().getContainingFile().getContainingDirectory().toString();
                        left = left.substring(left.lastIndexOf('/') + 1);
                        Fragment f2 = pair.getFragments().get(1);
                        String right = f2.getCClass().getPsiClass().getContainingFile().getContainingDirectory().toString();
                        right = right.substring(right.lastIndexOf('/') + 1);
                        outputWriter.write(String.format("%s,%s,%d,%d,%s,%s,%d,%d\n",
                            left,
                            f1.getCMethod().getCClass().getPsiClass().getContainingFile().getName(),
                            f1.getFromLineColumn().line,
                            f1.getToLineColumn().line,
                            right,
                            f2.getCMethod().getCClass().getPsiClass().getContainingFile().getName(),
                            f2.getFromLineColumn().line,
                            f2.getToLineColumn().line));
                    }
                }
                printLocalDateTime("");
            }
            outputWriter.close();
            printLocalDateTimeLabel("Finished processing.\n");
        }
        catch (IOException e) {
            System.out.println("Could not write to file");
            e.printStackTrace();
        }
    }

    public void collectJavaClones(@NotNull Project project, double minSim, double minSentSim, int minTokens, int minSent,
                                  int numWeightLevels) {
        MIN_SIM = minSim;
        MIN_SENT_SIM = minSentSim;
        MIN_TOKENS = minTokens;
        MIN_SENT = minSent;
        NUM_WEIGHT_LEVELS = numWeightLevels;
        fragments = new ArrayList<>();
        collectMethodsMinhashSignatures(project);

        printLocalDateTime("Collecting LSH for minhash signatures and generating similar pairs...");
        List<CMethod> methods = CMethodDict.list();
        //List<Clone> similarPairs = ShingleDict.lshMinhashSignatures(methods, ShingleDict.b, ShingleDict.r);
        List<Clone> similarPairs = ShingleDict.lshMinhashSignaturesFromFiles(methods, ShingleDict.b, ShingleDict.r);

        printLocalDateTime(String.format("Collecting clones from %d similar pairs...", similarPairs.size()));
        this.clones = (ArrayList<Clone>)similarPairs
            .parallelStream()
            .map(this::verifyAndGroupClones)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        printLocalDateTime(String.format("Fixing fragment indices from %d clones...", this.clones.size()));
        FragmentDict.indexFragments();
        printLocalDateTime(String.format("Merging clone pairs from %d clones...", this.clones.size()));
        for (int c = 0; c < this.clones.size(); c++) this.clones.get(c).fixClone(c);
        this.mergeClonePairs(OVERLAP_PERCENTAGE);
        printLocalDateTime(String.format("Fixing %d collected clones...", this.clones.size()));
        for (int c = 0; c < this.clones.size(); c++) this.clones.get(c).fixClone(c);
        this.calculateCCWeight();
        printLocalDateTime(String.format("Collecting fragments for %d clones...", this.clones.size()));
        FragmentDict.collectFragments(this.clones, this.fragments, this.methods);
        printLocalDateTime("Finished processing.\n");
    }

    private void calculateCCWeight() {
        MAX_WEIGHT = this.clones.stream().map(Clone::getMaxCognitiveComplexity).reduce(0, Math::max);
        this.clones.stream().forEach(clone -> clone.setMaxWeight(clone.getMaxCognitiveComplexity()));
        this.clones.stream()
            .map(Clone::getClonePairs)
            .flatMap(array -> array.stream())
            .forEach(clonePair -> {
                clonePair.setWeight(clonePair.getMaxCognitiveComplexity());
            });
        this.clones.stream()
            .map(Clone::getMethods)
            .flatMap(array -> array.stream())
            .forEach(method -> {
                method.setMaxWeight(method.getCcScore());
            });
    }

    private Clone verifyAndGroupClones(Clone clone) {
        return new SmithWatermanGotoh()
            .verifyAndGroupClone(clone, MIN_SIM, MIN_SENT_SIM, MIN_TOKENS, MIN_SENT, NUM_WEIGHT_LEVELS, OVERLAP_PERCENTAGE);
        /*
        return new SmithWatermanGotoh()
            .verifyAndGroupClone(clone, 0.7, 0.7, MIN_TOKENS, MIN_SENT, NUM_WEIGHT_LEVELS, OVERLAP_PERCENTAGE);
         */
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
        // fix fragments indices
        for (int f = 0; f < fragments.size(); f++) {
            fragments.get(f).setIdx(f);
            fragments.get(f).getKey().setIdx(f);
        }

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

    private void moveClonePair(ClonePair cp, Clone fromClone, Clone toClone) {
        int idxOnClone = cp.getIdxOnClone();
        List<ClonePair> pairs = fromClone.getClonePairs();
        pairs.remove(idxOnClone);
        fromClone.setNumberOfClonePairs(fromClone.getClonePairs().size());
        for (int c = idxOnClone; c < pairs.size(); c++) pairs.get(c).setIdxOnClone(c);
        pairs = toClone.getClonePairs();
        cp.setIdxOnClone(pairs.size());
        pairs.add(cp);
        toClone.setNumberOfClonePairs(toClone.getClonePairs().size());
        cp.setClone(toClone);
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
            // f1 -> fragment A from c1 and cp1
            Fragment f1 = fragments.get(i);
            for (int j = i + 1; j < fragments.size(); j++) {
                // f3 -> fragment A from c2 and cp2
                Fragment f3 = fragments.get(j);
                if (!f1.fromSameMethod(f3)) break;
                // if the two fragments can be merged
                if (f1.canBeMerged(f3, overlapPercentage)) {
                    // move second clone pair and methods to first clone and fix references
                    Clone c1 = f1.getClone();
                    Clone c2 = f3.getClone();
                    ClonePair cp2 = f3.getClonePair();
                    this.moveClonePair(cp2, c2, c1);
                    // fragment A was moved from c2 to c1

                    // f2 -> fragment B from c1 and cp1
                    Fragment f2 = f1.getClonePair().getFragments().get(1 - f1.getIdxOnClonePair());
                    // f4 -> fragment C from c2 and cp2
                    Fragment f4 = f3.getClonePair().getFragments().get(1 - f3.getIdxOnClonePair());
                    // locate third mergeable fragment
                    ClonePair cp3 = findMergeableClonePair(fragmentsMap, f2, f4, overlapPercentage);
                    if (cp3 != null) {
                        // fragments B and C from c3 and cp3
                        // move third clone pair to first clone and fix references
                        this.moveClonePair(cp3, cp3.getClone(), c1);
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
        for (int f = fragmentsMap.get(f2.getCMethod().getIdx()); f < numFrags && f2.fromSameMethod(fragments.get(f)); f++) {
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
        ShingleDict.reset();
        StatementDict.reset();
        TokenDict.reset();
    }
}
