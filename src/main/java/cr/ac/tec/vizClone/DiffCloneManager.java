package cr.ac.tec.vizClone;

import com.intellij.dupLocator.DupInfo;
import com.intellij.dupLocator.DupLocatorBundle;
import com.intellij.dupLocator.DuplicatesProfile;
import com.intellij.dupLocator.resultUI.ContentPanel;
import com.intellij.dupLocator.resultUI.DuplicatesForm;
import com.intellij.dupLocator.treeHash.GroupNodeDescription;
import com.intellij.dupLocator.treeHash.TreePsiFragment;
import com.intellij.dupLocator.util.PsiFragment;
import com.intellij.icons.AllIcons;
import com.intellij.ide.impl.ContentManagerWatcher;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiStatement;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.usageView.UsageInfo;
import cr.ac.tec.vizClone.model.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

@Service(Service.Level.PROJECT)
public final class DiffCloneManager {
    private final Project myProject;
    private ContentManager myContentManager;

    public DiffCloneManager(@NotNull Project p) {
        super();
        this.myProject = p;
        StartupManager.getInstance(p).runWhenProjectIsInitialized(() -> {
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(this.myProject);
            ToolWindow toolWindow = toolWindowManager.registerToolWindow("DiffClones", true, ToolWindowAnchor.BOTTOM);
            toolWindow.setIcon(AllIcons.Diff.ApplyNotConflicts);
            this.myContentManager = toolWindow.getContentManager();
            ContentManagerWatcher.watchContentManager(toolWindow, this.myContentManager);
        });
    }

    public static DiffCloneManager getInstance(@NotNull Project project) {
        return (DiffCloneManager)project.getService(DiffCloneManager.class);
    }

    public void showDiffClones(List<Clone> clones) {
        PsiDocumentManager.getInstance(this.myProject).commitAllDocuments();
        final Runnable showDiffClonesRunnable = () -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (clones != null && clones.size() > 0) {
                    DupInfo dupInfo = this.getDupInfo(clones);
                    this.showClones(dupInfo);
                } else
                    Messages.showInfoMessage(this.myProject, "No code duplicates found.", "DiffClones");
            });
        };
        showDiffClonesRunnable.run();
    }

    private DupInfo getDupInfo(List<Clone> clones) {
        final PsiFragment[][] duplicates = new PsiFragment[getNumDuplicates(clones)][];
        for (int c = 0, d = 0; c < clones.size(); c++) {
            Clone clone = clones.get(c);
            List<ClonePair> clonePairs = clone.getClonePairs();
            int numClonePairs = clone.getClonePairs().size();
            for (int cp = 0; cp < numClonePairs; d++, cp++) {
                ClonePair clonePair = clonePairs.get(cp);
                Fragment fragmentA = clonePair.getFragments().get(0);
                Fragment fragmentB = clonePair.getFragments().get(1);
                List<PsiStatement> statementsFromA = fragmentA.getCMethod().getCStatements()
                    .stream().map(CStatement::getPsiStatement).collect(Collectors.toList());
                List<PsiStatement> statementsFromB = fragmentB.getCMethod().getCStatements()
                    .stream().map(CStatement::getPsiStatement).collect(Collectors.toList());
                int fromA = fragmentA.getFromStatement();
                int toA = fragmentA.getToStatement();
                int fromB = fragmentB.getFromStatement();
                int toB = fragmentB.getToStatement();
                duplicates[d] = new PsiFragment[2];
                duplicates[d][0] = new TreePsiFragment(null, statementsFromA, fromA, toA);
                duplicates[d][1] = new TreePsiFragment(null, statementsFromB, fromB, toB);
                duplicates[d][0].setCost(clonePair.getSim());
            }
            /*
            Fragment fragmentA = clones.get(c).getClonePairs().get(0).getFragments().get(0);
            Fragment fragmentB = clones.get(c).getClonePairs().get(0).getFragments().get(1);
            List<PsiStatement> statementsFromA = clones.get(c).getMethods().get(0).getCStatements()
                    .stream().map(CStatement::getPsiStatement).collect(Collectors.toList());
            List<PsiStatement> statementsFromB = clones.get(c).getMethods().get(1).getCStatements()
                    .stream().map(CStatement::getPsiStatement).collect(Collectors.toList());
            int fromA = fragmentA.getFromStatement();
            int toA = fragmentA.getToStatement();
            int fromB = fragmentB.getFromStatement();
            int toB = fragmentB.getToStatement();
            duplicates[c] = new PsiFragment[2];
            duplicates[c][0] = new TreePsiFragment(null, statementsFromA, fromA, toA);
            duplicates[c][1] = new TreePsiFragment(null, statementsFromB, fromB, toB);
             */
        }
        Arrays.sort(duplicates, new Comparator<PsiFragment[]>() {
            public int compare(PsiFragment[] f1, PsiFragment[] f2) {
                return f2[0].getCost() - f1[0].getCost();
            }
        });
        return new DupInfo() {
            private final Int2ObjectMap<GroupNodeDescription> myPattern2Description = new Int2ObjectOpenHashMap<>();

            @Override
            public int getPatterns() {
                return duplicates.length;
            }

            @Override
            public int getPatternCost(int number) {
                return ((PsiFragment[]) duplicates[number])[0].getCost();
            }

            @Override
            public int getPatternDensity(int number) {
                return duplicates[number].length;
            }

            @Override
            public PsiFragment[] getFragmentOccurences(int pattern) {
                return duplicates[pattern];
            }

            @Override
            public UsageInfo[] getUsageOccurences(int pattern) {
                PsiFragment[] occurrences = getFragmentOccurences(pattern);
                UsageInfo[] infos = new UsageInfo[occurrences.length];
                for (int i = 0; i < infos.length; i++) {
                    infos[i] = occurrences[i].getUsageInfo();
                }
                return infos;
            }

            @Override
            public int getFileCount(final int pattern) {
                if (myPattern2Description.containsKey(pattern)) {
                    return myPattern2Description.get(pattern).getFilesCount();
                }
                return cacheGroupNodeDescription(pattern).getFilesCount();
            }

            private GroupNodeDescription cacheGroupNodeDescription(final int pattern) {
                final Set<PsiFile> files = new HashSet<>();
                final PsiFragment[] occurrences = getFragmentOccurences(pattern);
                for (PsiFragment occurrence : occurrences) {
                    final PsiFile file = occurrence.getFile();
                    if (file != null) {
                        files.add(file);
                    }
                }
                final int fileCount = files.size();
                final PsiFile psiFile = occurrences[0].getFile();
                DuplicatesProfile profile = DuplicatesProfile.findProfileForDuplicate(this, pattern);
                String comment = profile != null ? profile.getComment(this, pattern) : "";
                String filename = psiFile != null ? psiFile.getName() : DupLocatorBundle.message("duplicates.unknown.file.node.title");
                final GroupNodeDescription description = new GroupNodeDescription(fileCount, filename, comment);
                myPattern2Description.put(pattern, description);
                return description;
            }

            @Override
            public @Nullable @Nls String getTitle(int pattern) {
                if (getFileCount(pattern) == 1) {
                    if (myPattern2Description.containsKey(pattern)) {
                        return myPattern2Description.get(pattern).getTitle();
                    }
                    return cacheGroupNodeDescription(pattern).getTitle();
                }
                return null;
            }

            @Override
            public @Nullable @Nls String getComment(int pattern) {
                if (getFileCount(pattern) == 1) {
                    if (myPattern2Description.containsKey(pattern)) {
                        return myPattern2Description.get(pattern).getComment();
                    }
                    return cacheGroupNodeDescription(pattern).getComment();
                }
                return null;
            }

            @Override
            public int getHash(final int i) {
                return -1; // duplicateList.getInt(duplicates[i]);
            }
        };
    }

    private int getNumDuplicates(List<Clone> clones) {
        return clones.stream().map(clone -> clone.getClonePairs().size()).reduce(0, Integer::sum);
    }

    private void showClones(DupInfo duplicates) {
        DuplicatesForm form = DuplicatesForm.create(this.myProject, duplicates);

        ContentPanel contentPanel = new ContentPanel(form.getComponent());

        JPanel twPanel = contentPanel.getComponent();
        Content content = this.myContentManager.getFactory().createContent(twPanel, "Duplicate Code", true);
        content.setHelpId("find.locateDuplicates.result");
        content.setDisposer(form);
        this.myContentManager.addContent(content);
        this.myContentManager.setSelectedContent(content);
        contentPanel.addCloseAction(this.myContentManager, content);
        form.addToolbarActionsTo(contentPanel);
        ToolWindowManager.getInstance(this.myProject).getToolWindow("DiffClones").activate((Runnable)null);
    }
}
