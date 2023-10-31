package cr.ac.tec.vizClone;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CloneCollector {

    //private static ProjectFileIndex projectFileIndex = null;
    //projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();

    public void collectJavaClasses(@NotNull Project project) {
        String projectName = project.getName();
        VirtualFile[] vFiles = ProjectRootManager.getInstance(project)
                .getContentSourceRoots();
        List<String> javaClassesList = Arrays.stream(vFiles)
                .map(CloneCollector::collectJavaClassesInFolder)
                .collect(Collectors.toList())
                .stream()
                .flatMap(l -> l.stream())
                .collect(Collectors.toList());
        String filesList = javaClassesList.stream()
                .collect(Collectors.joining("\n"));
        Messages.showInfoMessage("Files for the " + projectName +
                " plugin:\n" + filesList, "Project Properties");
    }

    private static List<String> collectJavaClassesInFolder(@NotNull VirtualFile vFile) {
        VirtualFile[] childrenVFiles = vFile.getChildren();
        List<String> javaClassesList = Arrays.stream(childrenVFiles)
                .filter(vf -> !vf.isDirectory() && vf.getExtension().equals("java"))
                .map(VirtualFile::getPath)
                .collect(Collectors.toList());
        javaClassesList.addAll(
                Arrays.stream(childrenVFiles)
                        .filter(vf -> vf.isDirectory())
                        .map(CloneCollector::collectJavaClassesInFolder)
                        .collect(Collectors.toList())
                        .stream()
                        .flatMap(l -> l.stream())
                        .collect(Collectors.toList()));
        return javaClassesList;
    }
}
