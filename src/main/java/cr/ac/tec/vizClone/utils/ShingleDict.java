package cr.ac.tec.vizClone.utils;

import cr.ac.tec.vizClone.model.CMethod;
import cr.ac.tec.vizClone.model.Clone;
import cr.ac.tec.vizClone.model.ClonePair;
import groovy.lang.Tuple2;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ShingleDict {
    private static List<String> shingleArray = new ArrayList<>();
    private static Map<String, Integer> shingleDict = new Hashtable<>();
    private static ArrayList<Integer> hash;

    // n = 250, b = 25, r = 10, t = (1/b)^(1/r) = 0.73 of similitude
    // n = 200, b = 10, r = 20, t = (1/b)^(1/r) = 0.89 of similitude
    // n = 200, b =  5, r = 40, t = (1/b)^(1/r) = 0.96 of similitude
    public static final Integer NUM_MIN_HASHES = 200;
    public static final Integer b = 5;
    public static final Integer r = 40;

    static public void reset() {
        shingleDict.clear();
        shingleArray.clear();
    }

    static public Integer getShingleId(String shingle) {
        if (!shingleDict.containsKey(shingle)) {
            shingleArray.add(shingle);
            shingleDict.put(shingle, shingleDict.size());
        }
        return shingleDict.get(shingle);
    }

    static private List<Integer> getIntegerStreamRange(Integer start, Integer end) {
        return IntStream.range(start, end)
            .boxed()
            .collect(Collectors.toList());
    }

    static private List<Integer> getInitializedIntegerStreamRange(Integer length, Integer value) {
        return Stream.iterate(value, val -> val)
            .limit(length)
            .collect(Collectors.toList());
    }

    static private ArrayList<Integer> getFirstMinhash() {
        hash = new ArrayList<>(getIntegerStreamRange(0, shingleDict.size()));
        Collections.shuffle(hash);
        return hash;
    }

    static private ArrayList<Integer> getNextMinhash() {
        Collections.shuffle(hash);
        return hash;
    }

    static public void setMinhashSignatures(List<CMethod> methodList, Integer numMinHashes) {
        hash = getFirstMinhash();
        for (int idx = 0; idx < numMinHashes; idx++) {
            final int h_i = idx;
            methodList.parallelStream()
                .forEach(method -> {
                    method.getShingleSignature().add(Integer.MAX_VALUE);
                    method.getShingleSet()
                        .forEach(r ->
                            method.getShingleSignature().set(h_i, Math.min(method.getShingleSignature().get(h_i), hash.get(r)))
                        );
                });
            hash = getNextMinhash();
        }
    }

    static public List<Clone> lshMinhashSignatures(List<CMethod> methodList, Integer bands, Integer rows) {
        List<Clone> methodPairs = new ArrayList<>();
        List<HashSet<Integer>> lshBuckets = new ArrayList<>();
        // LSH Minhash signatures to b bands with r rows
        for (int band = 0; band < bands; band++) {
            final int bandStart = band * rows;
            final int bandEnd = bandStart + rows;
            // collect candidate methods in buckets per band
            ConcurrentMap<Integer, List<Integer>> buckets = methodList
                .stream()
                .map(CMethod::getIdx)
                .toList()
                .parallelStream()
                .collect(Collectors.groupingByConcurrent(method ->
                    CMethodDict.list().get(method).getShingleSignature().subList(bandStart, bandEnd).hashCode()));
            // add new buckets and merge buckets with intersection
            for (List<Integer> bucket : buckets.values()) {
                if (bucket.size() > 1) {
                    boolean isNewSet = true;
                    HashSet<Integer> newLshBucket = new HashSet<>(bucket);
                    HashSet<Integer> firstLshBucket = null;
                    for (int b = 0; b < lshBuckets.size();) {
                        HashSet<Integer> intersection = (HashSet<Integer>) newLshBucket.clone();
                        intersection.retainAll(lshBuckets.get(b));
                        // merge buckets with common methods
                        if (!intersection.isEmpty()) {
                            // merge into first matching bucket
                            if (isNewSet) {
                                firstLshBucket = lshBuckets.get(b);
                                firstLshBucket.addAll(newLshBucket);
                                isNewSet = false;
                            }
                            // merge this matching bucket into first matching bucket
                            else {
                                firstLshBucket.addAll(lshBuckets.get(b));
                                lshBuckets.remove((int)b);
                                continue;
                            }
                        }
                        b++;
                    }
                    if (isNewSet) {
                        lshBuckets.add(newLshBucket);
                    }
                }
            }
        }
        for (HashSet<Integer> bucketSet : lshBuckets) {
            ArrayList<Integer> bucket = new ArrayList<>(bucketSet);
            Clone clone = new Clone();
            for (int left = 0; left < bucket.size(); left++)
                for (int right = left + 1; right < bucket.size(); right++) {
                    ClonePair clonePair = new ClonePair();
                    clone.addClonePair(clonePair);
                    clonePair.setClone(clone);
                    clonePair.addFragments(
                        CMethodDict.list().get(bucket.get(left)),
                        CMethodDict.list().get(bucket.get(right)));
                }
            methodPairs.add(clone);
        }
        return methodPairs;
    }
}
