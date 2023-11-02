package cr.ac.tec.vizClone;

import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiMethod;

public class JavaCloneRecursiveElementVisitor extends JavaRecursiveElementVisitor {
    @Override
    public void visitMethod(PsiMethod method) {
        super.visitMethod(method);
        System.out.println(method.getContainingClass().getQualifiedName()+"."+method.getName()+method.getParameterList().getText());
    }
}
