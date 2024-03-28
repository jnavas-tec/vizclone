package org.intellij.sdk.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import cr.ac.tec.vizClone.CloneCollector;
import cr.ac.tec.vizClone.DiffCloneManager;
import cr.ac.tec.vizClone.VizCloneToolWindowFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LoadClonesAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        // Set the availability based on whether a project is open
        Project currentProject = event.getProject();
        event.getPresentation().setEnabledAndVisible(currentProject != null);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        PsiDocumentManager.getInstance(event.getProject()).commitAllDocuments();
        CloneCollector collector = new CloneCollector();
        collector.collectJavaClones(event.getProject());
        DiffCloneManager.getInstance(event.getProject()).showDiffClones(collector.getClones());
        VizCloneToolWindowFactory.getInstance(event.getProject(), collector.getClones(), collector.getFragments()).showVizClones();
    }
}
