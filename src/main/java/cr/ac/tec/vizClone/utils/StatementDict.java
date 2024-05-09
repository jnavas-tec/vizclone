package cr.ac.tec.vizClone.utils;

import com.intellij.psi.JavaTokenType;
import com.intellij.psi.tree.IElementType;

import java.util.*;

public final class StatementDict {
    private static List<Statement> statementArray = new ArrayList<>();
    private static Map<IElementType, Integer> statementDict = new Hashtable<>();

    public static void reset() {
        statementArray.clear();
        statementDict.clear();
    }

    private static class Statement {
        Integer statementId;
        IElementType statement;
    }

    static public Integer getStatementId(IElementType statement) {
        if (statementDict.get(statement) == null) {
            Statement newStatement = new Statement();
            newStatement.statement = statement;
            newStatement.statementId = statementArray.size();
            statementArray.add(newStatement);
            statementDict.put(statement, newStatement.statementId);
            return newStatement.statementId;
        }
        return statementDict.get(statement);
    }

    static public IElementType getStatement(Integer statementId) {
        if (statementId >= statementArray.size())
            return JavaTokenType.NULL_KEYWORD;
        else
            return statementArray.get(statementId).statement;
    }

    static public Map<IElementType, Integer> dict() {
        return statementDict;
    }
}
