package cr.ac.tec.vizClone;

import com.intellij.lang.ASTNode;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.tree.IElementType;
import cr.ac.tec.vizClone.utils.ShingleDict;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShinglingRecursiveElementVisitor extends JavaRecursiveElementVisitor {
    private boolean visitingMethod = false;
    private StringBuilder methodSourceCode;
    public static final Integer SHINGLE_SIZE = 27;
    private CloneConfig cloneConfig = new CloneConfig(false, false);

    public ShinglingRecursiveElementVisitor(CloneConfig cloneConfig) {
        if (cloneConfig != null) this.cloneConfig = cloneConfig;
    }

    @Override
    public void visitMethod(@NotNull PsiMethod method) {
        if (!visitingMethod) {
            visitingMethod = true;
            this.methodSourceCode = new StringBuilder();
            super.visitMethod(method);
            this.updateShingleDict();
            visitingMethod = false;
        }
        else
            super.visitMethod(method);
    }

    private void updateShingleDict() {
        for (int i = 0; i < this.methodSourceCode.length() - SHINGLE_SIZE; i++) {
            ShingleDict.getShingleId(this.methodSourceCode.substring(i, i + SHINGLE_SIZE));
        }
    }

    private static List<IElementType> skipTokens = Arrays.asList(JavaTokenType.WHITE_SPACE, JavaTokenType.END_OF_LINE_COMMENT, JavaTokenType.C_STYLE_COMMENT);

    @Override
    public void visitElement(@NotNull PsiElement element) {
        ASTNode astNode = element.getNode();
        IElementType elementType = astNode.getElementType();
        if (visitingMethod
            && element.getChildren().length == 0
            && astNode.getTextLength() > 0
            && !skipTokens.contains(elementType))
        {
            this.methodSourceCode.append(doGenericComparison(elementType) ? elementType.getDebugName() : element.getNode().getText());
            this.methodSourceCode.append(" ");
        }
        super.visitElement(element);
    }

    private static ArrayList<IElementType> braceElements = new ArrayList<>(Arrays.asList(
        JavaTokenType.LBRACE, JavaTokenType.RBRACE
    ));

    private static ArrayList<IElementType> literalTokens = new ArrayList<>(Arrays.asList(
        JavaTokenType.CHARACTER_LITERAL, JavaTokenType.LONG_LITERAL, JavaTokenType.DOUBLE_LITERAL,
        JavaTokenType.STRING_LITERAL, JavaTokenType.FLOAT_LITERAL, JavaTokenType.INTEGER_LITERAL,
        JavaTokenType.TEXT_BLOCK_LITERAL
    ));

    private boolean doGenericComparison(IElementType elementType) {
        if (literalTokens.contains(elementType) && !cloneConfig.isAnyLiteral()) return false;
        if (JavaTokenType.IDENTIFIER.equals(elementType) && !cloneConfig.isAnyIdentifier()) return false;
        return true;
    }
}
