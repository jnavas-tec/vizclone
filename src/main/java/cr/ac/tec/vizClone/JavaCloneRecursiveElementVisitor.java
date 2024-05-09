package cr.ac.tec.vizClone;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.text.LineColumn;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.JavaElementType;
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

    public JavaCloneRecursiveElementVisitor(CloneCollector cloneCollector, Integer minSentences, Integer minTokens, CloneConfig cloneConfig) {
        this.minSentences = minSentences;
        this.minTokens = minTokens;
        this.cloneCollector = cloneCollector;
        if (cloneConfig != null) this.cloneConfig = cloneConfig;
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
        if (!visitingClass && !visitingAnonymousClass && !visitingMethod) {
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
        if (this.visitingClass && !this.visitingAnonymousClass && !this.visitingMethod) {
            this.cMethod = CMethodDict.getMethod(method, this.lineColumns);
            this.visitingMethod = true;
            this.methodSourceCode = new StringBuilder();
            super.visitMethod(method);
            // add method if it has minimun number of sentences
            if (this.cMethod.getCStatements().size() < this.minSentences || this.cMethod.getNumTokens() < this.minTokens) {
                CMethodDict.removeMethod(this.cMethod);
            }
            else {
                CClassDict.addMethod(cClass.getIdx(), cMethod);
                this.collectMethodShingles();
            }
            this.visitingMethod = false;
            this.cMethod = null;
        }
        else
            super.visitMethod(method);
    }

    private void collectMethodShingles() {
        for (int i = 0; i < this.methodSourceCode.length() - ShinglingRecursiveElementVisitor.SHINGLE_SIZE; i++) {
            this.cMethod.getShingleSet().add(ShingleDict.getShingleId(this.methodSourceCode.substring(i, i + ShinglingRecursiveElementVisitor.SHINGLE_SIZE)));
        }
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

    @Override
    public void visitStatement(PsiStatement psiStatement) {
        CStatement previousCStatement = cStatement;

        StringBuilder previousStatementSourceCode= this.sentenceSourceCode;
        this.sentenceSourceCode = new StringBuilder();

        if (visitingClass && !visitingAnonymousClass && visitingMethod) {
            addStatement(psiStatement);
            if (JavaElementType.BLOCK_STATEMENT.equals(StatementDict.getStatement(cStatement.getStatementId()))) {
                cStatement.getTokens().add(TokenDict.getTokenId(JavaTokenType.LBRACE));
                cMethod.setNumTokens(cMethod.getNumTokens() + 1);
                cStatement.setText("{");
            }
        }

        boolean previousVisitingStatement = visitingStatement;
        visitingStatement = true;
        super.visitStatement(psiStatement);
        visitingStatement = previousVisitingStatement;

        if (visitingClass && !visitingAnonymousClass && visitingMethod) {
            if (JavaElementType.BLOCK_STATEMENT.equals(StatementDict.getStatement(cStatement.getStatementId()))) {
                addStatement(psiStatement);
                cStatement.getTokens().add(TokenDict.getTokenId(JavaTokenType.RBRACE));
                cMethod.setNumTokens(cMethod.getNumTokens() + 1);
                cStatement.setText("}");
            }
            else {
                CMethodDict.sumStatementTokens(cMethod.getIdx(), cStatement);
                cStatement.setText(this.sentenceSourceCode.toString());
            }
        }

        cStatement = previousCStatement;
        this.sentenceSourceCode = previousStatementSourceCode;
    }

    @Override
    public void visitKeyword(@NotNull PsiKeyword keyword) {
        if (cStatement != null &&
            JavaElementType.IF_STATEMENT.equals(StatementDict.getStatement(cStatement.getStatementId())) &&
            JavaTokenType.ELSE_KEYWORD.equals(keyword.getTokenType().getDebugName())) {
            addStatement(cStatement.getPsiStatement(), keyword);
            cStatement.setText("else");
            cMethod.setNumTokens(cMethod.getNumTokens() + 1);
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
