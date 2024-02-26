package cr.ac.tec.vizClone.utils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public final class StatementDict {
    private static List<Statement> statementArray = new ArrayList<>();
    private static Hashtable<String, Integer> statementDict = new Hashtable<>();

    private static class Statement {
        Integer statementId;
        String statement;
    }

    static public Integer getStatementId(String statement) {
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

    static public String getStatement(Integer statementId) {
        if (statementId >= statementArray.size())
            return "UNKNOWN";
        else
            return statementArray.get(statementId).statement;
    }

    static public Hashtable<String, Integer> dict() {
        return statementDict;
    }
}
