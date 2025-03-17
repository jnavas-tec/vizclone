package cr.ac.tec.vizClone.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Shingler {
    public final Integer DEFAULT_SHINGLE_SIZE = 27;
    private ArrayList<Integer> tokens = null;
    private Integer shingleSize = DEFAULT_SHINGLE_SIZE;
    public final static Integer M = 99999989;
    private static ArrayList<Integer> r;
    private final static Integer MAX_RANDOM_HASHES = 10240;
    private final static Integer seed = 20250315;
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

    public static Integer hashShingle(Integer shingle, Integer hashIdx) {
        return modM(r(hashIdx) * shingle);
    }

    public static Integer hashCodeRange(List<Integer> shingles) {
        int hashCode = 0;
        for (int i = 0; i < shingles.size(); i++) {
            Integer e = shingles.get(i);
            if (e != null) hashCode = hashCode + r(i) * e;
        }
        return modM(hashCode);
    }

    private Integer hashCodeRange(int from, int to) {
        if (to > tokens.size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int hashCode = 0;
        for (int i = from; i < to; i++) {
            Integer e = tokens.get(i);
            if (e != null) hashCode = hashCode + r(i - from) * e;
        }
        return modM(hashCode);
    }

    private static Integer r(Integer i) {
        if (r == null) {
            r = new ArrayList<>(MAX_RANDOM_HASHES);
            Random random = new Random(seed);
            for (int k = 0; k < MAX_RANDOM_HASHES; k++)
                r.add(random.nextInt(M));
        }
        return r.get(i);
    }

    private static Integer mod(Integer x, Integer y) {
        return (Math.floorMod(x, y) + Math.abs(y)) % Math.abs(y);
    }

    private static Integer modM(Integer x) {
        return (Math.floorMod(x, M) + Math.abs(M)) % Math.abs(M);
    }
}
