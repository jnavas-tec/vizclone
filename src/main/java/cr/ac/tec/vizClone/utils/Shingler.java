package cr.ac.tec.vizClone.utils;

import java.util.ArrayList;

public class Shingler {
    public final Integer DEFAULT_SHINGLE_SIZE = 27;
    private ArrayList<Integer> tokens = null;
    private Integer shingleSize = DEFAULT_SHINGLE_SIZE;
    private Integer nextShingle = 0;
    public Shingler() {
        tokens = new ArrayList<>();
        nextShingle = 0;
    }
    public Shingler(ArrayList<Integer> tokens) {
        this.tokens = tokens;
        nextShingle = 0;
    }
    public Shingler(Integer shingleSize) {
        this.shingleSize = shingleSize;
        tokens = new ArrayList<>();
        nextShingle = 0;
    }
    public Shingler(Integer shingleSize, ArrayList<Integer> tokens) {
        this.shingleSize = shingleSize;
        this.tokens = tokens;
        nextShingle = 0;
    }
    public void addToken(Integer token) {
        tokens.add(token);
    }
    public void resetCursor() {
        nextShingle = 0;
    }
    public boolean hasShingles() {
        return tokens.size() >= nextShingle + shingleSize;
    }
    public Integer getNextShingle() {
        Integer hash = hashCodeRange(nextShingle, nextShingle + shingleSize);
        nextShingle++;
        return hash;
    }

    private Integer hashCodeRange(int from, int to) {
        if (to > tokens.size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int hashCode = 1;
        for (int i = from; i < to; i++) {
            Integer e = tokens.get(i);
            hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
        }
        return hashCode;
    }
}
