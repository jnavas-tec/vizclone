package cr.ac.tec.vizClone.utils;

import com.intellij.psi.JavaTokenType;
import com.intellij.psi.tree.IElementType;
import lombok.Data;

import java.util.*;

public final class TokenDict {
    private static List<Token> tokensArray = new ArrayList<>();
    private static Map<IElementType, Integer> tokensDict = new Hashtable<>();
    private static Map<String, Integer> idOrLiteralDict = new Hashtable<>();

    static public void reset() {
        tokensArray.clear();
        tokensDict.clear();
        idOrLiteralDict.clear();
    }

    @Data
    public static class Token {
        Integer tokenId;
        boolean idOrLiteral;
        IElementType token;
        String text;
    }

static private void setNextToken(Token newToken) {
        newToken.tokenId = tokensArray.size();
        tokensArray.add(newToken);
    }

    static public Integer getTokenId(IElementType token) {
        if (tokensDict.get(token) == null) {
            Token newToken = new Token();
            newToken.token = token;
            newToken.idOrLiteral = false;
            setNextToken(newToken);
            tokensDict.put(token, newToken.tokenId);
            return newToken.tokenId;
        }
        return tokensDict.get(token);
    }

    static public Integer getTokenId(String token) {
        if (idOrLiteralDict.get(token) == null) {
            Token newToken = new Token();
            newToken.text = token;
            newToken.idOrLiteral = true;
            setNextToken(newToken);
            idOrLiteralDict.put(token, newToken.tokenId);
            return newToken.tokenId;
        }
        return idOrLiteralDict.get(token);
    }

    static public IElementType getToken(Integer tokenId) {
        if (tokenId >= tokensArray.size())
            return JavaTokenType.NULL_KEYWORD;
        else
            return tokensArray.get(tokenId).token;
    }

    static public Map<IElementType, Integer> dict() {
        return tokensDict;
    }

    static public List<Token> list() { return tokensArray; }
}
