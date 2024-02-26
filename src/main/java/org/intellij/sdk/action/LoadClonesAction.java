package org.intellij.sdk.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import cr.ac.tec.vizClone.CloneCollector;
import cr.ac.tec.vizClone.SmithWatermanGotoh;
import cr.ac.tec.vizClone.model.CMethod;
import cr.ac.tec.vizClone.model.Clone;
import cr.ac.tec.vizClone.utils.CMethodDict;
import cr.ac.tec.vizClone.utils.CPackageDict;
import cr.ac.tec.vizClone.utils.StatementDict;
import cr.ac.tec.vizClone.utils.TokenDict;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LoadClonesAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        // Set the availability based on whether a project is open
        Project currentProject = event.getProject();
        event.getPresentation().setEnabledAndVisible(currentProject != null);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        CloneCollector collector = new CloneCollector();
        collector.collectJavaClasses(project);

        // show collected information
        CPackageDict
            .list()
            .forEach(cPackage -> {
                //System.out.println(String.format("Package: %s", cPackage.getSignature()));
                cPackage.getCClasses()
                    .forEach(cClass -> {
                        //System.out.println(String.format("\tClass: %s", cClass.getSignature()));
                        cClass.getCMethods()
                            .forEach(cMethod -> {
                                //System.out.println(String.format("\t\tMethod: %s", cMethod.getSignature()));
                                cMethod.getCStatements()
                                    .forEach(cStatement -> {
                                       //System.out.print((String.format("\t\t\t%s: ", StatementDict.getStatement(cStatement.getStatementId()))));
                                       String tokens = cStatement.getTokens().stream()
                                           .map(token -> TokenDict.getToken(token))
                                           .collect(Collectors.joining(" "));
                                       //System.out.println(tokens);
                                    });
                            });
                    });
            });
        List<CMethod> methods = CMethodDict.list();
        int numMethods = methods.size() - 1;
        SmithWatermanGotoh swg = new SmithWatermanGotoh();
        ArrayList<Clone> clones = new ArrayList<>();
        Clone clone;
        int cloneCnt = 0;
        for (int i = 0; i < numMethods; i++) {
            CMethod methodA = methods.get(i);
            for (int j = i + 1; j <= numMethods; j++) {
                swg.init();
                CMethod methodB = methods.get(j);
                if (swg.config(methodA, methodB)) {
                    clone = swg.getClone();
                    if (clone != null) {
                        clones.add(clone);
                        System.out.printf("Clone #%d (Sim: %d)%n", ++cloneCnt, clone.getMaxWeight());
                        System.out.printf("\tMethod A: %s (%s, %s), Sentences: %d%n",
                                methodA.getSignature(),
                                clone.getClonePairs().get(0).getFragments().get(0).getFromLineColumn().toString(),
                                clone.getClonePairs().get(0).getFragments().get(0).getToLineColumn().toString(),
                                clone.getClonePairs().get(0).getFragments().get(0).getNumberOfStatements());
                        System.out.printf("\tMethod B: %s (%s, %s), Sentences: %d%n",
                                methodB.getSignature(),
                                clone.getClonePairs().get(0).getFragments().get(1).getFromLineColumn().toString(),
                                clone.getClonePairs().get(0).getFragments().get(1).getToLineColumn().toString(),
                                clone.getClonePairs().get(0).getFragments().get(1).getNumberOfStatements());
                    }
                }
                swg.release();
            }
        }
        boolean stopHere = true;
    }
}
