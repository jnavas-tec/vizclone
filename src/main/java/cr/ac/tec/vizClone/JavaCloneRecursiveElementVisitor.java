package cr.ac.tec.vizClone;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.text.LineColumn;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.ChildRole;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.impl.source.tree.java.PsiBreakStatementImpl;
import com.intellij.psi.impl.source.tree.java.PsiContinueStatementImpl;
import com.intellij.psi.impl.source.tree.java.PsiIfStatementImpl;
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
    private Integer minSentences = 0;
    private Integer minTokens = 0;
    private CloneConfig cloneConfig = new CloneConfig(false, false);
    private StringBuilder methodSourceCode = null;
    private StringBuilder sentenceSourceCode = null;

    // Cognitive Complexity
    private int ccNestingLevel = 0;
    private int ccScore = 0;

    private boolean folderAsPackage = false;

    public JavaCloneRecursiveElementVisitor(CloneCollector cloneCollector, Integer minSentences, Integer minTokens, CloneConfig cloneConfig, boolean folderAsPackage) {
        this.minSentences = minSentences;
        this.minTokens = minTokens;
        this.cloneCollector = cloneCollector;
        if (cloneConfig != null) this.cloneConfig = cloneConfig;
        this.folderAsPackage = folderAsPackage;
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
        if (/*!visitingClass &&*/ !visitingAnonymousClass && !visitingMethod) {
            CClass previousCClass = cClass;
            cClass = CClassDict.getClass(aClass, lineColumns, folderAsPackage);
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
        if (this.visitingClass && !this.visitingAnonymousClass && !this.visitingMethod) {
            // TODO: Add method even if they have the same signature
            this.cMethod = CMethodDict.getMethod(method, this.lineColumns, this.cClass, folderAsPackage);
            if (this.cMethod.getCStatements().size() == 0) {
                this.visitingMethod = true;
                this.methodSourceCode = new StringBuilder();
                ccNestingLevel = 0;
                ccScore = 0;
                super.visitMethod(method);
                // add method if it has minimun number of sentences
                if (this.cMethod.getCStatements().size() < this.minSentences || this.cMethod.getNumTokens() < this.minTokens) {
                    CMethodDict.removeMethod(this.cMethod);
                }
                else {
                    cMethod.setCcScore(ccScore);
                    CClassDict.addMethod(cClass.getIdx(), cMethod);
                    this.collectMethodShingles();
                }
                this.visitingMethod = false;
                this.cMethod = null;
            }
            else
                super.visitMethod(method);
        }
        else
            super.visitMethod(method);
    }

    private void collectMethodShingles() {
        for (int i = 0; i < this.methodSourceCode.length() - ShinglingRecursiveElementVisitor.SHINGLE_SIZE; i++) {
            this.cMethod.getShingleSet().add(ShingleDict.getShingleId(this.methodSourceCode.substring(i, i + ShinglingRecursiveElementVisitor.SHINGLE_SIZE)));
        }
        this.methodSourceCode = null;
    }

    @Override
    public void visitCodeBlock(PsiCodeBlock block) {
        boolean previousVisitingCodeBlock = visitingCodeBlock;
        visitingCodeBlock = true;
        super.visitCodeBlock(block);
        visitingCodeBlock = previousVisitingCodeBlock;
    }

    private void addStatement(PsiStatement psiStatement) {
        Integer statementId = StatementDict.getStatementId(psiStatement.getNode().getElementType());
        cStatement = new CStatement();
        cStatement.setStatementId(statementId);
        cStatement.setPsiStatement(psiStatement);
        cStatement.setCMethod(cMethod);
        cStatement.setFromOffset(psiStatement.getTextRange().getStartOffset());
        cStatement.setToOffset(psiStatement.getTextRange().getEndOffset() - 1);
        cStatement.setFromLineColumn(lineColumns.get(cStatement.getFromOffset()));
        cStatement.setToLineColumn(lineColumns.get(cStatement.getToOffset()));

        // fix ToOffset of these statements
        if (psiStatement.getNode().getElementType().equals(JavaElementType.IF_STATEMENT)) {
            if (((PsiIfStatement)psiStatement).getRParenth() != null) {
                cStatement.setToOffset(((PsiIfStatement) psiStatement).getRParenth().getTextRange().getEndOffset() - 1);
                cStatement.setToLineColumn(lineColumns.get(cStatement.getToOffset()));
            }
        }
        else if (psiStatement.getNode().getElementType().equals(JavaElementType.SWITCH_STATEMENT)) {
            if (((PsiSwitchStatement)psiStatement).getRParenth() != null) {
                cStatement.setToOffset(((PsiSwitchStatement) psiStatement).getRParenth().getTextRange().getEndOffset() - 1);
                cStatement.setToLineColumn(lineColumns.get(cStatement.getToOffset()));
            }
        }
        else if (psiStatement.getNode().getElementType().equals(JavaElementType.FOR_STATEMENT)) {
            if (((PsiForStatement)psiStatement).getRParenth() != null) {
                cStatement.setToOffset(((PsiForStatement) psiStatement).getRParenth().getTextRange().getEndOffset() - 1);
                cStatement.setToLineColumn(lineColumns.get(cStatement.getToOffset()));
            }
        }
        else if (psiStatement.getNode().getElementType().equals(JavaElementType.FOREACH_STATEMENT)) {
            if (((PsiForeachStatement)psiStatement).getRParenth() != null) {
                cStatement.setToOffset(((PsiForeachStatement) psiStatement).getRParenth().getTextRange().getEndOffset() - 1);
                cStatement.setToLineColumn(lineColumns.get(cStatement.getToOffset()));
            }
        }
        else if (psiStatement.getNode().getElementType().equals(JavaElementType.FOREACH_PATTERN_STATEMENT)) {
            if (((PsiForeachPatternStatement)psiStatement).getRParenth() != null) {
                cStatement.setToOffset(((PsiForeachPatternStatement) psiStatement).getRParenth().getTextRange().getEndOffset() - 1);
                cStatement.setToLineColumn(lineColumns.get(cStatement.getToOffset()));
            }
        }
        else if (psiStatement.getNode().getElementType().equals(JavaElementType.WHILE_STATEMENT)) {
            if (((PsiWhileStatement)psiStatement).getRParenth() != null) {
                cStatement.setToOffset(((PsiWhileStatement) psiStatement).getRParenth().getTextRange().getEndOffset() - 1);
                cStatement.setToLineColumn(lineColumns.get(cStatement.getToOffset()));
            }
        }
        else if (psiStatement.getNode().getElementType().equals(JavaElementType.TRY_STATEMENT)) {
            cStatement.setToOffset(cStatement.getFromOffset() + 2);
            cStatement.setToLineColumn(lineColumns.get(cStatement.getToOffset()));
        }
        CMethodDict.addStatement(cMethod.getIdx(), cStatement);
    }

    private void addStatement(PsiStatement psiStatement, PsiKeyword psiKeyword) {
        Integer statementId = StatementDict.getStatementId(psiKeyword.getNode().getElementType());
        cStatement = new CStatement();
        cStatement.setStatementId(statementId);
        cStatement.setPsiStatement(psiStatement);
        cStatement.setCMethod(cMethod);
        cStatement.setFromOffset(psiKeyword.getTextRange().getStartOffset());
        cStatement.setToOffset(psiKeyword.getTextRange().getEndOffset() - 1);
        cStatement.setFromLineColumn(lineColumns.get(cStatement.getFromOffset()));
        cStatement.setToLineColumn(lineColumns.get(cStatement.getToOffset()));
        CMethodDict.addStatement(cMethod.getIdx(), cStatement);
    }

    // score += 1
    private static ArrayList<IElementType> incrementElements = new ArrayList<>(Arrays.asList(
        JavaElementType.IF_STATEMENT, JavaElementType.SWITCH_STATEMENT, JavaElementType.FOR_STATEMENT,
        JavaElementType.FOREACH_STATEMENT, JavaElementType.FOREACH_PATTERN_STATEMENT, JavaElementType.WHILE_STATEMENT,
        JavaElementType.DO_WHILE_STATEMENT, JavaElementType.CATCH_SECTION, JavaElementType.BREAK_STATEMENT,
        JavaElementType.CONTINUE_STATEMENT, JavaElementType.CONDITIONAL_EXPRESSION, JavaTokenType.ELSE_KEYWORD
    ));

    // nesting_level++
    private static ArrayList<IElementType> nestingLevelElements = new ArrayList<>(Arrays.asList(
        JavaElementType.IF_STATEMENT, JavaElementType.SWITCH_STATEMENT, JavaElementType.FOR_STATEMENT,
        JavaElementType.FOREACH_STATEMENT, JavaElementType.FOREACH_PATTERN_STATEMENT, JavaElementType.WHILE_STATEMENT,
        JavaElementType.DO_WHILE_STATEMENT, JavaElementType.CATCH_SECTION, JavaElementType.CONDITIONAL_EXPRESSION,
        JavaElementType.LAMBDA_EXPRESSION
    ));

    // score += nesting_level
    private static ArrayList<IElementType> nestingElements = new ArrayList<>(Arrays.asList(
        JavaElementType.IF_STATEMENT, JavaElementType.FOR_STATEMENT, JavaElementType.FOREACH_STATEMENT,
        JavaElementType.FOREACH_PATTERN_STATEMENT, JavaElementType.WHILE_STATEMENT, JavaElementType.DO_WHILE_STATEMENT,
        JavaElementType.CATCH_SECTION, JavaElementType.CONDITIONAL_EXPRESSION, JavaElementType.SWITCH_STATEMENT
    ));

    private void preCC(IElementType elementType) {
        if (incrementElements.contains(elementType)) {
            this.ccScore++;
            this.cStatement.setCcScore(1);
        }
        if (nestingElements.contains(elementType)) {
            this.ccScore += this.ccNestingLevel;
            this.cStatement.setCcScore(this.ccNestingLevel + this.cStatement.getCcScore());
        }
        if (nestingLevelElements.contains(elementType)) this.ccNestingLevel++;
    }

    private void postCC(IElementType elementType) {
        if (nestingLevelElements.contains(elementType)) this.ccNestingLevel--;
    }

    private IElementType ccElementToCheck(PsiStatement psiStatement) {
        IElementType elementType = psiStatement.getNode().getElementType();
        if (JavaElementType.BLOCK_STATEMENT.equals(elementType) ||
            JavaElementType.IF_STATEMENT.equals(elementType)) {
            if (psiStatement.getParent().getNode().getPsi() instanceof PsiIfStatement) {
                PsiIfStatementImpl psiIfStatement = (PsiIfStatementImpl)psiStatement.getParent().getNode().getPsi();
                if (psiStatement.equals(psiIfStatement.getElseBranch())) {
                    return JavaTokenType.ELSE_KEYWORD;
                }
            }
        }
        else if (JavaElementType.BREAK_STATEMENT.equals(elementType)) {
            PsiBreakStatementImpl breakStatement = (PsiBreakStatementImpl)psiStatement;
            if (breakStatement.findChildByRole(ChildRole.LABEL) != null)
                return JavaElementType.BREAK_STATEMENT;
            else
                return JavaElementType.EMPTY_STATEMENT;
        }
        else if (JavaElementType.CONTINUE_STATEMENT.equals(elementType)) {
            PsiContinueStatementImpl continueStatement = (PsiContinueStatementImpl)psiStatement;
            if (continueStatement.findChildByRole(ChildRole.LABEL) != null)
                return JavaElementType.CONTINUE_STATEMENT;
            else
                return JavaElementType.EMPTY_STATEMENT;
        }
        return elementType;
    }


    @Override
    public void visitStatement(PsiStatement psiStatement) {
        CStatement previousCStatement = cStatement;
        CStatement thisBlockStatement = null;

        StringBuilder previousStatementSourceCode= this.sentenceSourceCode;
        this.sentenceSourceCode = new StringBuilder();

        if (visitingClass  && visitingMethod) {
            if (!visitingAnonymousClass) {
                addStatement(psiStatement);
                if (JavaElementType.BLOCK_STATEMENT.equals(StatementDict.getStatement(cStatement.getStatementId()))) {
                    thisBlockStatement = cStatement;
                    cStatement.getTokens().add(TokenDict.getTokenId(JavaTokenType.LBRACE));
                    cMethod.setNumTokens(cMethod.getNumTokens() + 1);
                    cStatement.setText("{");
                    // fix end of block start
                    cStatement.setToLineColumn(cStatement.getFromLineColumn());
                    cStatement.setToOffset(cStatement.getFromOffset());
                }
            }
            this.preCC(this.ccElementToCheck(psiStatement));
        }

        boolean previousVisitingStatement = visitingStatement;
        visitingStatement = true;
        super.visitStatement(psiStatement);
        visitingStatement = previousVisitingStatement;

        if (visitingClass && visitingMethod) {
            this.postCC(this.ccElementToCheck(psiStatement));
            if (!visitingAnonymousClass) {
                if (thisBlockStatement != null) {
                    addStatement(psiStatement);
                    cStatement.getTokens().add(TokenDict.getTokenId(JavaTokenType.RBRACE));
                    cMethod.setNumTokens(cMethod.getNumTokens() + 1);
                    cStatement.setText("}");
                    // fix start of block end
                    cStatement.setFromLineColumn(cStatement.getToLineColumn());
                    cStatement.setFromOffset(cStatement.getToOffset());
                } else {
                    CMethodDict.sumStatementTokens(cMethod.getIdx(), cStatement);
                    cStatement.setText(this.sentenceSourceCode.toString());
                }
            }
        }

        cStatement = previousCStatement;
        this.sentenceSourceCode = previousStatementSourceCode;
    }

    @Override
    public void visitConditionalExpression(@NotNull PsiConditionalExpression expression) {
        if (visitingClass && visitingMethod) {
            this.preCC(expression.getNode().getElementType());
            super.visitConditionalExpression(expression);
            this.postCC(expression.getNode().getElementType());
        }
        else
            super.visitConditionalExpression(expression);
    }

    @Override
    public void visitLambdaExpression(@NotNull PsiLambdaExpression expression) {
        if (visitingClass && visitingMethod) {
            this.preCC(expression.getNode().getElementType());
            super.visitLambdaExpression(expression);
            this.postCC(expression.getNode().getElementType());
        }
        else
            super.visitLambdaExpression(expression);
    }

    @Override
    public void visitKeyword(@NotNull PsiKeyword keyword) {
        if (cStatement != null) {
            CStatement prevStatement = cStatement;
            if (JavaElementType.IF_STATEMENT.equals(StatementDict.getStatement(cStatement.getStatementId())) &&
                JavaTokenType.ELSE_KEYWORD.equals(keyword.getTokenType().getDebugName())) {
                addStatement(cStatement.getPsiStatement(), keyword);
                cStatement.setText("else");
                cMethod.setNumTokens(cMethod.getNumTokens() + 1);
            }
            else if (JavaElementType.TRY_STATEMENT.equals(StatementDict.getStatement(cStatement.getStatementId())) &&
                JavaTokenType.FINALLY_KEYWORD.equals(keyword.getTokenType().getDebugName())) {
                addStatement(cStatement.getPsiStatement(), keyword);
                cStatement.setText("finally");
                cMethod.setNumTokens(cMethod.getNumTokens() + 1);
            }
            cStatement = prevStatement;
        }
        super.visitKeyword(keyword);
    }

    private static ArrayList<IElementType> braceElements = new ArrayList<>(Arrays.asList(
        JavaTokenType.LBRACE, JavaTokenType.RBRACE
    ));

    @Override
    public void visitElement(@NotNull PsiElement element) {
        ASTNode astNode = element.getNode();
        IElementType elementType = astNode.getElementType();
        if (visitingClass && visitingMethod && visitingCodeBlock && visitingStatement
            && cStatement != null
            && element.getChildren().length == 0
            && astNode.getTextLength() > 0
            && !elementType.equals(JavaTokenType.WHITE_SPACE)
            && !elementType.equals(JavaTokenType.END_OF_LINE_COMMENT)
            && (!braceElements.contains(elementType) ||
                !JavaElementType.BLOCK_STATEMENT.equals(StatementDict.getStatement(cStatement.getStatementId()))))
        {
            String tokenText = element.getNode().getText();
            Integer tokenId = doGenericComparison(elementType) ? TokenDict.getTokenId(elementType) : TokenDict.getTokenId(tokenText);
            cStatement.getTokens().add(tokenId);
            this.methodSourceCode.append(doGenericComparison(elementType) ? elementType.getDebugName() : element.getNode().getText());
            this.methodSourceCode.append(" ");
            // this is needed to identify Type 1 clones
            this.sentenceSourceCode.append(element.getNode().getText());
            this.sentenceSourceCode.append(" ");
        }
        super.visitElement(element);
    }

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
