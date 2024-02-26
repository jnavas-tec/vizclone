package cr.ac.tec.vizClone.utils;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class NestedSentenceDict
{
    private static Hashtable<String, Set<String>> nestedSentencesDict = new Hashtable<>();

    static public Hashtable<String, Set<String>> getNestedSentencesDict() {
        return nestedSentencesDict;
    }

    static public Set<String> getNestedSentencesSet(String sentence) {
        if (nestedSentencesDict.get(sentence) == null) {
            Set<String> nestedSentences = new HashSet<>();
            nestedSentencesDict.put(sentence, nestedSentences);
            return nestedSentences;
        }
        return nestedSentencesDict.get(sentence);
    }

    static public void setNestedSentence(String sentence, String nestedSentence) {
        getNestedSentencesSet(sentence).add(nestedSentence);
    }
}
