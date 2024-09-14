package org.intellij.sdk.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import cr.ac.tec.vizClone.CloneCollector;
import cr.ac.tec.vizClone.VizCloneToolWindowFactory;
import org.jetbrains.annotations.NotNull;

public class BigCloneBenchAction extends AnAction {

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
        collector.collectBCBClones(event.getProject(), CloneCollector.MIN_SIM, CloneCollector.MIN_SENT_SIM,
            CloneCollector.MIN_TOKENS, CloneCollector.MIN_SENT, CloneCollector.NUM_WEIGHT_LEVELS);
    }
}
