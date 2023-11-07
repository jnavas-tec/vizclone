package cr.ac.tec.vizClone;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.text.LineColumn;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JavaCloneRecursiveElementVisitor extends JavaRecursiveElementVisitor {

    private CloneCollector cloneCollector;
    private boolean visitingMethod = false;
    private boolean visitingCodeBlock = false;
    private boolean visitingAnonymousClass = false;
    private List<List<IElementType>> methodElements;
    private List<Integer> methodLines;
    private List<IElementType> methodStatements;
    private List<LineColumn> lineColumns;

    public JavaCloneRecursiveElementVisitor(CloneCollector cloneCollector) {
        this.cloneCollector = cloneCollector;
    }

    @Override
    public void visitFile(@NotNull PsiFile file) {
        lineColumns = mapOffsetToLineColumn(file.getText());
        super.visitFile(file);
    }

    @Override
    public void visitMethod(PsiMethod method) {
        if (!visitingAnonymousClass) {
            this.visitingMethod = true;
            methodElements = new ArrayList<>();
            methodLines = new ArrayList<>();
            methodStatements = new ArrayList<>();
        }
        super.visitMethod(method);
        if (!visitingAnonymousClass) {
            this.visitingMethod = false;
            String iElementTypes = methodElements.stream()
                    .map(elem -> elem.stream().map(el -> el.toString()).collect(Collectors.joining(",")))
                    .collect(Collectors.joining(String.format("%n     ")));
            String statementTypes = methodStatements.stream()
                    .map(statement -> statement.toString())
                    .collect(Collectors.joining(String.format("%n     ")));
            System.out.println(String.format("%n"));
            System.out.println(method.getContainingClass().getQualifiedName() + "." + method.getName() + method.getParameterList().getText());
            System.out.println(iElementTypes);
            System.out.println(statementTypes);
        }
    }

    @Override
    public void visitCodeBlock(PsiCodeBlock block) {
        if (visitingMethod && !visitingCodeBlock) {
            visitingCodeBlock = true;
            super.visitCodeBlock(block);
            visitingCodeBlock = false;
        }
        else {
            super.visitCodeBlock(block);
        }
    }

    // TODO: Instead of lines use statements
    // TODO: Register all tokens and assign consecutive id (two way dictionary)
    // TODO: Port Needleman-Wunsch algorithm from C#
    // TODO: If match is not good, implement

    @Override
    public void visitElement(@NotNull PsiElement element) {
        ASTNode astNode = element.getNode();
        String elementType = astNode.getElementType().toString();
        if (visitingMethod && visitingCodeBlock
                && element.getChildren().length == 0
                && astNode.getTextLength() > 0
                && !elementType.equals("WHITE_SPACE")
                && !elementType.equals("END_OF_LINE_COMMENT")) {
            Integer numLines = methodElements.size();
            Integer currLine = methodLines.size() == 0 ? 0 : methodLines.get(methodLines.size() - 1);
            List<IElementType> currentLineElements = numLines == 0 ? null : methodElements.get(numLines - 1);
            if (currLine < lineColumns.get(element.getTextOffset()).line) {
                currLine = lineColumns.get(element.getTextOffset()).line;
                numLines++;
                methodElements.add(new ArrayList<>());
                currentLineElements = methodElements.get(numLines - 1);
                methodLines.add(currLine);
            }
            currentLineElements.add(element.getNode().getElementType());
        }
        super.visitElement(element);
    }

    @Override
    public void visitAnonymousClass(PsiAnonymousClass aClass) {
        visitingAnonymousClass = true;
        super.visitAnonymousClass(aClass);
        visitingAnonymousClass = false;
    }

    @Override
    public void visitStatement(PsiStatement statement) {
        if (visitingMethod && !visitingAnonymousClass) {
            methodStatements.add(statement.getNode().getElementType());
        }
        super.visitStatement(statement);
    }

    private static List<LineColumn> mapOffsetToLineColumn(@NotNull CharSequence text) {
        List<LineColumn> list = new ArrayList<>();
        int curLine = 0;
        int curLineStart = 0;
        int curOffset = 0;
        while (curOffset < text.length()) {
            //if (curOffset == text.length()) return list;
            char c = text.charAt(curOffset);
            if (c == '\n') {
                list.add(LineColumn.of(curLine + 1, curOffset - curLineStart + 1));
                curLine++;
                curLineStart = curOffset + 1;
            }
            else if (c == '\r') {
                list.add(LineColumn.of(curLine + 1, curOffset - curLineStart + 1));
                curLine++;
                if (curOffset < text.length() - 1 && text.charAt(curOffset + 1) == '\n') {
                    curOffset++;
                    list.add(LineColumn.of(curLine, curOffset - curLineStart + 1));
                }
                curLineStart = curOffset + 1;
            }
            else {
                list.add(LineColumn.of(curLine + 1, curOffset - curLineStart + 1));
            }
            curOffset++;
        }

        return list;
    }
}
