package cr.ac.tec.vizClone;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiUtilCore;
import cr.ac.tec.vizClone.utils.NestedSentenceDict;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CloneCollector {

    //private static ProjectFileIndex projectFileIndex = null;
    //projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();

    public void collectJavaClasses(@NotNull Project project) {
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


        boolean dummy_stopper = true;


        /*
        NestedSentenceDict.getNestedSentencesDict().keys()
                .asIterator().forEachRemaining(
                        sentence -> {
                            String sentences = NestedSentenceDict.getNestedSentencesSet(sentence).stream()
                                    .collect(Collectors.joining("\n\t"));
                            System.out.println(String.format("%s\n\t%s", sentence, sentences));
                        }
                );
         */

        /*
        String sourceRoots = Arrays.stream(vSourceRoots)
                .map(VirtualFile::getPath)
                .collect(Collectors.joining("\n"));
        String javaClassesPathList = javaVFilesList.stream()
                .map(VirtualFile::getPath)
                .map(path -> path.substring(Arrays.stream(vSourceRoots).map(VirtualFile::getPath).filter(sourcePath -> sourcePath.equals(path.substring(0, sourcePath.length()))).findFirst().orElse("").length()))
                //.map(path -> Arrays.stream(vSourceRoots).map(VirtualFile::getPath).filter(sourcePath -> sourcePath.equals(path.substring(0, sourcePath.length()))).findFirst().orElse("").toString())
                .collect(Collectors.joining("\n"));
        Messages.showInfoMessage("Files for the " + projectName +
                " plugin:\n" + javaClassesPathList + "\nSource paths:\n" +
                sourceRoots, "Project Properties");
        */

        /*
        System.out.println("Project Name: " + projectName);

        System.out.println("\nSENTENCES\n");
        SentenceDict.dict().entrySet()
                .forEach(
                        entry -> {
                            System.out.println(entry.getKey() + "->" + entry.getValue());
                        }
                );

        System.out.println("\nTOKENS\n");
        TokenDict.dict().entrySet()
            .forEach(
                entry -> {
                    System.out.println(entry.getKey() + "->" + entry.getValue());
                }
            );
         */
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
}
