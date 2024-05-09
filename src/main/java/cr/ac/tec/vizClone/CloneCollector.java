package cr.ac.tec.vizClone;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiUtilCore;
import cr.ac.tec.vizClone.model.CMethod;
import cr.ac.tec.vizClone.model.Clone;
import cr.ac.tec.vizClone.model.Fragment;
import cr.ac.tec.vizClone.model.Method;
import cr.ac.tec.vizClone.utils.*;
import groovy.lang.Tuple2;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Data
public class CloneCollector {

    public static double MIN_SIM = 0.95; // 0.7;
    public static double MIN_SENT_SIM = 0.95;
    public static int MIN_TOKENS = 200; // 50;
    public static int MIN_SENT = 20; // 7;
    public static int NUM_WEIGHT_LEVELS = 4;
    public static int OVERLAP_PERCENTAGE = 60;

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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        System.out.print(String.format("%s: %s", formatter.format(currentDateTime), message));
        previousDateTime = currentDateTime;
    }

    private void collectMethodsMinhashSignatures(@NotNull Project project) {
        this.resetDictionaries();
        String projectName = project.getName();
        VirtualFile[] vSourceRoots = ProjectRootManager.getInstance(project)
            .getContentSourceRoots();
        printLocalDateTime("Collecting java virtual files...");
        List<VirtualFile> javaVFilesList = Arrays.stream(vSourceRoots)
            .map(vFile -> CloneCollector.collectJavaClassesInFolder(vFile.getPath(), vFile))
            .collect(Collectors.toList())
            .stream()
            .flatMap(l -> l.stream())
            .collect(Collectors.toList());
        printLocalDateTime("Collecting java psi files...");
        List<PsiFile> javaPsiFilesList = PsiUtilCore.toPsiFiles(PsiManager.getInstance(project), javaVFilesList);

        printLocalDateTime("Collecting shingles...");

        CloneConfig cloneConfig = new CloneConfig(true, true);

        javaPsiFilesList.stream()
            .forEach(javaPsiFile -> javaPsiFile.accept(new ShinglingRecursiveElementVisitor(cloneConfig)));

        printLocalDateTime(String.format("Collecting methods from %d Java Psi files...", javaPsiFilesList.size()));
        javaPsiFilesList.stream()
                .forEach(javaPsiFile -> javaPsiFile.accept(
                    new JavaCloneRecursiveElementVisitor(this, MIN_SENT, MIN_TOKENS, cloneConfig))
                );

        printLocalDateTime(String.format("Collecting minhash signatures for %d methods...", CMethodDict.list().size()));
        ShingleDict.setMinhashSignatures(CMethodDict.list(), ShingleDict.NUM_MIN_HASHES);
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

    static private ArrayList<String> cloneNames = new ArrayList<>(Arrays.asList(
        "org.jetbrains.plugins.groovy.editor.GroovyReferenceCopyPasteProcessor.findReferencesToRestore",
        "org.jetbrains.plugins.groovy.editor.GroovyReferenceCopyPasteProcessor.findReferencesToRestoreClone1",
        "org.jetbrains.plugins.groovy.editor.GroovyReferenceCopyPasteProcessor.findReferencesToRestoreClone2"));

    public void collectJavaClones(@NotNull Project project, double minSim, double minSentSim, int minTokens, int minSent,
                                  int numWeightLevels) {
        MIN_SIM = minSim;
        MIN_SENT_SIM = minSentSim;
        MIN_TOKENS = minTokens;
        MIN_SENT = minSent;
        NUM_WEIGHT_LEVELS = numWeightLevels;
        //clones = new ArrayList<>();
        fragments = new ArrayList<>();
        collectMethodsMinhashSignatures(project);

        printLocalDateTime("Collecting LSH for minhash signatures and generating similar pairs...");
        List<CMethod> methods = CMethodDict.list();
        //List<Tuple2<Integer, Integer>> similarPairs = ShingleDict.lshMinhashSignatures(methods, ShingleDict.b, ShingleDict.r);
        List<Clone> similarPairs = ShingleDict.lshMinhashSignatures(methods, ShingleDict.b, ShingleDict.r);




/*
        System.out.printf("%s Shingle Set -> %s%n", methods.get(5121).getName(), methods.get(5121).getShingleSet().toString());
        System.out.printf("%s Shingle Set -> %s%n", methods.get(5122).getName(), methods.get(5122).getShingleSet().toString());
        System.out.printf("%s Shingle Set -> %s%n", methods.get(5123).getName(), methods.get(5123).getShingleSet().toString());

        System.out.printf("%s Shingle Signature -> %s%n", methods.get(5121).getName(), methods.get(5121).getShingleSignature().toString());
        System.out.printf("%s Shingle Signature -> %s%n", methods.get(5122).getName(), methods.get(5122).getShingleSignature().toString());
        System.out.printf("%s Shingle Signature -> %s%n", methods.get(5123).getName(), methods.get(5123).getShingleSignature().toString());
*/



        printLocalDateTime(String.format("Collecting clones from %d similar pairs...", similarPairs.size()));
        this.clones = (ArrayList<Clone>)similarPairs
            .parallelStream()
            .map(this::verifyAndGroupClones)
            .flatMap(cloneList -> cloneList.stream())
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        /*
        System.out.println("------------------------------------------------------------------------");
        this.clones.stream()
            .map(clone -> clone.getClonePairs())
            .flatMap(clonePairList -> clonePairList.stream())
            .map(clonePair -> clonePair.getFragments())
            .flatMap(fragmentList -> fragmentList.stream())
            .forEach(fragment -> System.out.println(String.format("Clone: %d Fragment: %d- %d-%d",
                fragment.getClone().getIdx(),
                fragment.getCMethod().getIdx(),
                fragment.getFromLineColumn().line,
                fragment.getToLineColumn().line)));
        System.out.println("------------------------------------------------------------------------");
         */
        printLocalDateTime(String.format("Fixing %d collected clones...", this.clones.size()));
        for (int c = 0; c < this.clones.size(); c++) this.clones.get(c).fixClone(c);
        printLocalDateTime(String.format("Collecting fragments for %d clones...", this.clones.size()));
        FragmentDict.collectFragments(this.clones, this.fragments, this.methods);
        printLocalDateTime("Finished processing.\n");
        /*
        System.out.println("------------------------------------------------------------------------");
        CMethodDict.list()
            .stream()
            .forEach(method -> System.out.println(String.format("%d - %s", method.getIdx(), method.getSignature())));
        System.out.println("------------------------------------------------------------------------");
        this.clones.stream()
            .map(clone -> clone.getClonePairs())
            .flatMap(clonePairList -> clonePairList.stream())
            .map(clonePair -> clonePair.getFragments())
            .flatMap(fragmentList -> fragmentList.stream())
            .forEach(fragment -> System.out.println(String.format("Clone: %d Fragment: %d- %d-%d",
                fragment.getClone().getIdx(),
                fragment.getCMethod().getIdx(),
                fragment.getFromLineColumn().line,
                fragment.getToLineColumn().line)));
        System.out.println("------------------------------------------------------------------------");
         */
    }

    private List<Clone> verifyAndGroupClones(Clone clone) {
        return new SmithWatermanGotoh()
            .verifyAndGroupClone(clone, MIN_SIM, MIN_SENT_SIM, MIN_TOKENS, MIN_SENT, NUM_WEIGHT_LEVELS, OVERLAP_PERCENTAGE);
        /*
        return new SmithWatermanGotoh()
            .verifyAndGroupClone(clone, 0.7, 0.7, MIN_TOKENS, MIN_SENT, NUM_WEIGHT_LEVELS, OVERLAP_PERCENTAGE);
         */
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
