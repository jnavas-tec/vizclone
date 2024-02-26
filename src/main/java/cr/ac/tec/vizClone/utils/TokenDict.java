package cr.ac.tec.vizClone.utils;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public final class TokenDict {
    private static List<Token> tokensArray = new ArrayList<>();
    private static Hashtable<String, Integer> tokensDict = new Hashtable<>();

    private static class Token {
        Integer tokenId;
        String  token;
    }

    static public Integer getTokenId(String token) {
        if (tokensDict.get(token) == null) {
            Token newToken = new Token();
            newToken.token = token;
            newToken.tokenId = tokensArray.size();
            tokensArray.add(newToken);
            tokensDict.put(token, newToken.tokenId);
            return newToken.tokenId;
        }
        return tokensDict.get(token);
    }

    static public String getToken(Integer tokenId) {
        if (tokenId >= tokensArray.size())
            return "UNKNOWN";
        else
            return tokensArray.get(tokenId).token;
    }

    static public Hashtable<String, Integer> dict() {
        return tokensDict;
    }
}
