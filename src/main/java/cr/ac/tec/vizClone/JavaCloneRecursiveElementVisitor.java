package cr.ac.tec.vizClone;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.text.LineColumn;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import cr.ac.tec.vizClone.model.CClass;
import cr.ac.tec.vizClone.model.CMethod;
import cr.ac.tec.vizClone.model.CPackage;
import cr.ac.tec.vizClone.model.CStatement;
import cr.ac.tec.vizClone.utils.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JavaCloneRecursiveElementVisitor extends JavaRecursiveElementVisitor {

    private CloneCollector cloneCollector;
    private boolean visitingClass = false;
    private boolean visitingMethod = false;
    private boolean visitingStatement = false;
    private boolean visitingCodeBlock = false;
    private boolean visitingAnonymousClass = false;
    /*
    private List<List<IElementType>> methodElements;
    private List<Integer> methodLines;
    private List<IElementType> methodStatements;
    */
    private List<LineColumn> lineColumns;
    private CClass cClass = null;
    private CMethod cMethod = null;
    private CStatement cStatement = null;

    public JavaCloneRecursiveElementVisitor(CloneCollector cloneCollector) {
        this.cloneCollector = cloneCollector;
    }

    @Override
    public void visitFile(@NotNull PsiFile file) {
        lineColumns = mapOffsetToLineColumn(file.getText());
        super.visitFile(file);
    }

    @Override
    public void visitAnonymousClass(PsiAnonymousClass aClass) {
        boolean prevVisitingAnonymousClass = visitingAnonymousClass; // handle nested anonymous classes
        visitingAnonymousClass = true;
        super.visitAnonymousClass(aClass);
        visitingAnonymousClass = prevVisitingAnonymousClass;
    }

    @Override
    public void visitClass(PsiClass aClass) {
        if (!visitingAnonymousClass) {
            CClass previousCClass = cClass;
            cClass = CClassDict.getClass(aClass, lineColumns);
            boolean previousVisitingClass = visitingClass;
            visitingClass = true;
            super.visitClass(aClass);
            visitingClass = previousVisitingClass;
            cClass = previousCClass;
        }
        else super.visitClass(aClass);
    }

    @Override
    public void visitMethod(PsiMethod method) {
        if (visitingClass && !visitingAnonymousClass) {
            CMethod previousCMethod = cMethod;
            //System.out.println(method.getName());
            cMethod = CMethodDict.getMethod(method, lineColumns);
            CClassDict.addMethod(cClass.getIdx(), cMethod);
            boolean previousVisitingMethod = visitingMethod;
            visitingMethod = true;
            super.visitMethod(method);
            visitingMethod = previousVisitingMethod;
            cMethod = previousCMethod;
        }
        else
            super.visitMethod(method);

        if (!visitingAnonymousClass) {
            /*
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
             */
        }
    }

    @Override
    public void visitCodeBlock(PsiCodeBlock block) {
        boolean previousVisitingCodeBlock = visitingCodeBlock;
        visitingCodeBlock = true;
        super.visitCodeBlock(block);
        //PsiStatement[] statements = block.getStatements();
        visitingCodeBlock = previousVisitingCodeBlock;
    }

    private void addStatement(PsiStatement psiStatement) {
        String statementName = psiStatement.getNode().getElementType().getDebugName();
        Integer statementId = StatementDict.getStatementId(statementName);
        cStatement = new CStatement();
        cStatement.setStatementId(statementId);
        cStatement.setPsiStatement(psiStatement);
        cStatement.setCMethod(cMethod);
        cStatement.setFromOffset(psiStatement.getTextRange().getStartOffset());
        cStatement.setToOffset(psiStatement.getTextRange().getEndOffset());
        cStatement.setFromLineColumn(lineColumns.get(cStatement.getFromOffset()));
        cStatement.setToLineColumn(lineColumns.get(cStatement.getToOffset()));
        CMethodDict.addStatement(cMethod.getIdx(), cStatement);
    }

    private void addStatement(PsiStatement psiStatement, PsiKeyword psiKeyword) {
        String statementName = psiKeyword.getNode().getElementType().getDebugName();
        Integer statementId = StatementDict.getStatementId(statementName);
        cStatement = new CStatement();
        cStatement.setStatementId(statementId);
        cStatement.setPsiStatement(psiStatement);
        cStatement.setCMethod(cMethod);
        cStatement.setFromOffset(psiKeyword.getTextRange().getStartOffset());
        cStatement.setToOffset(psiKeyword.getTextRange().getEndOffset());
        cStatement.setFromLineColumn(lineColumns.get(cStatement.getFromOffset()));
        cStatement.setToLineColumn(lineColumns.get(cStatement.getToOffset()));
        CMethodDict.addStatement(cMethod.getIdx(), cStatement);
    }

    @Override
    public void visitStatement(PsiStatement psiStatement) {

        /*
        if (visitingClass && !visitingAnonymousClass && visitingMethod && visitingStatement) {
            NestedSentenceDict.setNestedSentence(
                    methodStatements.get(methodStatements.size() - 1).getDebugName(),
                    statement.getNode().getElementType().getDebugName());

//            PsiMethod containingMethod = PsiTreeUtil.getParentOfType(statement, PsiMethod.class);
//            if (containingMethod != null) {
//                PsiClass containingClass = containingMethod.getContainingClass();
//                if (containingClass != null) {
//                    System.out.println("Class '" + containingClass.getQualifiedName() + "', " +
//                            "\n\tMethod '" + containingMethod.getName() + containingMethod.getParameterList().getText() + "', " +
//                            "\n\tSentence '" + statement.getNode().getElementType().getDebugName() + "' " +
//                            "\n\tis inside of Sentence '" + methodStatements.get(methodStatements.size() - 1).getDebugName() + "'");
//                }
//                else {
//                    System.out.println("Method '" + containingMethod.getName() + containingMethod.getParameterList().getText() + "', " +
//                            "\n\tSentence '" + statement.getNode().getElementType().getDebugName() + "' " +
//                            "\n\tis inside of Sentence '" + methodStatements.get(methodStatements.size() - 1).getDebugName() + "'");
//                }
//            }
//            else {
//                System.out.println("Sentence '" + statement.getNode().getElementType().getDebugName() + "' " +
//                        "\n\tis inside of Sentence '" + methodStatements.get(methodStatements.size() - 1).getDebugName() + "'");
//            }

        }
        */

        CStatement previousCStatement = cStatement;

        if (visitingClass && !visitingAnonymousClass && visitingMethod) {
            addStatement(psiStatement);
            if ("BLOCK_STATEMENT".equals(StatementDict.getStatement(cStatement.getStatementId()))) {
                cStatement.getTokens().add(TokenDict.getTokenId("LBRACE"));
                cMethod.setNumTokens(cMethod.getNumTokens() + 1);
            }
        }

        boolean previousVisitingStatement = visitingStatement;
        visitingStatement = true;
        super.visitStatement(psiStatement);
        visitingStatement = previousVisitingStatement;

        if (visitingClass && !visitingAnonymousClass && visitingMethod) {
            if ("BLOCK_STATEMENT".equals(StatementDict.getStatement(cStatement.getStatementId()))) {
                addStatement(psiStatement);
                cStatement.getTokens().add(TokenDict.getTokenId("RBRACE"));
                cMethod.setNumTokens(cMethod.getNumTokens() + 1);
            }
            else CMethodDict.sumStatementTokens(cMethod.getIdx(), cStatement);
        }

        cStatement = previousCStatement;
    }

    @Override
    public void visitKeyword(@NotNull PsiKeyword keyword) {
        if (cStatement != null &&
            "IF_STATEMENT".equals(StatementDict.getStatement(cStatement.getStatementId())) &&
            "ELSE_KEYWORD".equals(keyword.getTokenType().getDebugName())) {
            addStatement(cStatement.getPsiStatement(), keyword);
            cMethod.setNumTokens(cMethod.getNumTokens() + 1);
        }
        super.visitKeyword(keyword);
    }

    @Override
    public void visitElement(@NotNull PsiElement element) {
        ASTNode astNode = element.getNode();
        String elementType = astNode.getElementType().toString();
        String braceElements[] = { "LBRACE", "RBRACE" };
        if (visitingClass && visitingMethod && visitingCodeBlock && visitingStatement
                && element.getChildren().length == 0
                && astNode.getTextLength() > 0
                && !elementType.equals("WHITE_SPACE")
                && !elementType.equals("END_OF_LINE_COMMENT")
                && (!Arrays.asList(braceElements).contains(elementType) ||
                    !"BLOCK_STATEMENT".equals(StatementDict.getStatement(cStatement.getStatementId()))))
        {
            String tokenName = element.getNode().getElementType().getDebugName();
            Integer tokenId = TokenDict.getTokenId(tokenName);
            cStatement.getTokens().add(tokenId);
        }
        super.visitElement(element);
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
