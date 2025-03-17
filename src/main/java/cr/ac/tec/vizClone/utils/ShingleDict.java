package cr.ac.tec.vizClone.utils;

import cr.ac.tec.vizClone.model.CMethod;
import cr.ac.tec.vizClone.model.Clone;
import cr.ac.tec.vizClone.model.ClonePair;
import groovy.lang.Tuple2;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ShingleDict {
    private static List<Integer> intShingleArray = new ArrayList<>();
    private static Map<Integer, Integer> intShingleDict = new Hashtable<>();
    private static ArrayList<Integer> hash;
    private static ArrayList<ArrayList<Integer>> hashes;


    public static final Integer NUM_MIN_HASHES = 32;
    public static final Integer b = 28;
    public static final Integer r = 4;

    static public void reset() {
        intShingleDict.clear();
        intShingleArray.clear();
        intShingleDict = new Hashtable<>();
        intShingleArray = new ArrayList<>();
    }

    static public Integer getNumShingles() {
        return (intShingleArray != null) ? intShingleArray.size() : 0;
    }

    static public Integer getHashesSize() {
        return hashes.stream().mapToInt(ArrayList::size).sum();
    }

    static public Integer getShingleId(Integer shingle) {
        if (!intShingleDict.containsKey(shingle)) {
            intShingleArray.add(shingle);
            intShingleDict.put(shingle, intShingleDict.size());
        }
        return intShingleDict.get(shingle);
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
        hash = new ArrayList<>(getIntegerStreamRange(0, intShingleDict.size()));
        Collections.shuffle(hash);
        return hash;
    }

    static private ArrayList<Integer> getNextMinhash() {
        Collections.shuffle(hash);
        return hash;
    }

    static public void initHashes(Integer numHashes) {
        ArrayList<Integer> hash = new ArrayList<>(getIntegerStreamRange(0, intShingleDict.size()));
        hashes = new ArrayList<>();
        for (int idx = 0; idx < numHashes; idx++) {
            Collections.shuffle(hash);
            ArrayList<Integer> shuffledHash = new ArrayList<>(getIntegerStreamRange(0, intShingleDict.size()));
            Collections.copy(hash, shuffledHash);
            hashes.add(shuffledHash);
        }
    }

    static public void clearHashes() {
        hashes.forEach(hash -> { hash.clear(); });
        hashes.clear();
        hashes = null;
        reset();
    }

    static public void setMethodMinhashBandSignature(CMethod method, Shingler shingler) {
        final int bands = b;
        final int rows = r;
        TreeSet<Integer> shingleSet = new TreeSet<>();
        while (shingler.hasShingles()) {
            shingleSet.add(ShingleDict.getShingleId(shingler.getNextShingle()));
        }

        int numMinHashes = bands * rows;
        ArrayList<Integer> signature = new ArrayList<>();
        for (int idx = 0; idx < numMinHashes; idx++) {
            final int h_i = idx;
            //final ArrayList<Integer> hash = hashes.get(idx);
            signature.add(Integer.MAX_VALUE);
            shingleSet.forEach(r -> signature.set(h_i, Math.min(signature.get(h_i), Shingler.hashShingle(r, h_i))));
        }
        shingleSet.clear();
        for (int band = 0; band < bands; band++) {
            final int bandStart = band * rows;
            final int bandEnd = bandStart + rows;
            method.getLshBandHashes().add(Shingler.hashCodeRange(signature.subList(bandStart, bandEnd)));
        }
        signature.clear();
    }

    static public void setMinhashBandSignatures(List<CMethod> methodList, Integer bands, Integer rows) {
        int numMinHashes = bands * rows;
        initHashes(numMinHashes);
        methodList.parallelStream()
            .forEach(method -> {
                for (int idx = 0; idx < numMinHashes; idx++) {
                    final int h_i = idx;
                    final ArrayList<Integer> hash = hashes.get(idx);
                    method.getShingleSignature().add(Integer.MAX_VALUE);
                    method.getShingleSet()
                        .forEach(r ->
                            method.getShingleSignature().set(h_i, Math.min(method.getShingleSignature().get(h_i), hash.get(r)))
                        );
                }
                method.getShingleSet().clear();
                method.setShingleSet(null);
                for (int band = 0; band < bands; band++) {
                    final int bandStart = band * rows;
                    final int bandEnd = bandStart + rows;
                    method.getLshBandHashes().add(method.getShingleSignature().subList(bandStart, bandEnd).hashCode());
                }
                method.getShingleSignature().clear();
                method.setShingleSignature(null);
            });
        clearHashes();
    }

    static public List<Clone> lshMinhashBandSignatures(List<CMethod> methodList) {
        final int bands = b;
        List<Clone> methodPairs = new ArrayList<>();
        List<HashSet<Integer>> lshBuckets = new ArrayList<>();
        Set<Tuple2<Integer, Integer>> pairList = new TreeSet<>();
        // LSH Minhash signatures to b bands with r rows
        for (int band = 0; band < bands; band++) {
            final int b = band;
            // collect candidate methods in buckets per band
            ConcurrentMap<Integer, List<Integer>> buckets = methodList
                .stream()
                .map(CMethod::getIdx)
                .toList()
                .parallelStream()
                .collect(Collectors.groupingByConcurrent(method -> {
                    //List<CMethod> list = CMethodDict.list();
                    //if (method >= list.size() || b >= list.get(method).getLshBandHashes().size()) {
                    //    boolean OMG = true;
                    //}
                    return CMethodDict.list().get(method).getLshBandHashes().get(b);
                }));
            // add candidate pairs to tuples pairList
            buckets.values()
                .stream()
                .map(list -> list.stream().distinct().collect(Collectors.toList()))
                .filter(lst -> lst.size() > 1)
                .forEach(lst -> {
                    for (int left = 0; left < lst.size() - 1; left++) {
                        for (int right = left + 1; right < lst.size(); right++) {
                            Integer leftMethod = Math.min(lst.get(left), lst.get(right));
                            Integer rightMethod = Math.max(lst.get(left), lst.get(right));
                            if (!leftMethod.equals(rightMethod)) {
                                Tuple2<Integer, Integer> pair = new Tuple2<>(leftMethod, rightMethod);
                                pairList.add(pair);
                            }
                        }
                    }
                });
        }
        methodList.parallelStream()
            .forEach(method -> {
                method.getLshBandHashes().clear();
                method.setLshBandHashes(null);
            });
        for (Tuple2<Integer, Integer> pair : pairList) {
            Clone clone = new Clone();
            ClonePair clonePair = new ClonePair();
            clone.addClonePair(clonePair);
            clonePair.setClone(clone);
            clonePair.addFragments(CMethodDict.list().get(pair.getV1()), CMethodDict.list().get(pair.getV2()));
            methodPairs.add(clone);
        }
        return methodPairs;
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
        methodList.parallelStream()
            .forEach(method -> {
                method.getShingleSet().clear();
                method.setShingleSet(null);
            });
    }

    static public List<Clone> lshMinhashSignatures(List<CMethod> methodList, Integer bands, Integer rows) {
        List<Clone> methodPairs = new ArrayList<>();
        List<HashSet<Integer>> lshBuckets = new ArrayList<>();
        Set<Tuple2<Integer, Integer>> pairList = new TreeSet<>();
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
            // add candidate pairs to tuples pairList
            buckets.values()
                .stream()
                .map(list -> list.stream().distinct().collect(Collectors.toList()))
                .filter(lst -> lst.size() > 1)
                .forEach(lst -> {
                    for (int left = 0; left < lst.size() - 1; left++) {
                        for (int right = left + 1; right < lst.size(); right++) {
                            Integer leftMethod = Math.min(lst.get(left), lst.get(right));
                            Integer rightMethod = Math.max(lst.get(left), lst.get(right));
                            if (!leftMethod.equals(rightMethod)) {
                                Tuple2<Integer, Integer> pair = new Tuple2<>(leftMethod, rightMethod);
                                pairList.add(pair);
                            }
                        }
                    }
                });
        }
        methodList.parallelStream()
            .forEach(method -> {
                method.setShingleSignature(null);
            });
        for (Tuple2<Integer, Integer> pair : pairList) {
            Clone clone = new Clone();
            ClonePair clonePair = new ClonePair();
            clone.addClonePair(clonePair);
            clonePair.setClone(clone);
            clonePair.addFragments(CMethodDict.list().get(pair.getV1()), CMethodDict.list().get(pair.getV2()));
            methodPairs.add(clone);
        }
        return methodPairs;
    }

    static public void setMinhashSignaturesInFiles(List<CMethod> methodList, Integer bands, Integer rows) throws IOException {
        hash = getFirstMinhash();
        for (int band = 0; band < bands; band++) {
            for (int row = 0; row < rows; row++) {
                final int h_i = row;
                methodList.parallelStream()
                    .forEach(method -> {
                        method.getShingleSignature().add(Integer.MAX_VALUE);
                        method.getShingleSet()
                            .forEach(shingle ->
                                {
                                    /*
                                    if (method.getShingleSignature().size() <= h_i) {
                                        boolean stopHere = true;
                                    }
                                    if (!hash.contains(shingle)) {
                                        boolean stopThenHere = true;
                                    }

                                     */
                                    method.getShingleSignature().set(h_i, Math.min(method.getShingleSignature().get(h_i), hash.get(shingle)));
                                }
                            );
                    });
                hash = getNextMinhash();
            }
            CSVPrinter printer = null;
            try {
                FileWriter fileWriter = new FileWriter("/Users/jnavas/temp/minhash" + String.valueOf(band) + ".csv");
                printer = new CSVPrinter(fileWriter, CSVFormat.DEFAULT);
                final CSVPrinter csvPrinter = printer;
                methodList.stream()
                    .forEach(method -> {
                        try {
                            csvPrinter.printRecord(method.getShingleSignature().stream());
                            method.getShingleSignature().clear();
                        } catch (IOException ex) {
                            System.out.println("From inner catch.");
                            ex.printStackTrace();
                        }
                    });
            } catch (IOException ex) {
                System.out.println("From outer catch.");
                ex.printStackTrace();
            }
            finally {
                if (printer != null) {
                    printer.flush();
                    printer.close();
                }
            }
        }
        methodList.parallelStream()
            .forEach(method -> {
                method.getShingleSet().clear();
                method.setShingleSet(null);
            });
    }

    static public List<Clone> lshMinhashSignaturesFromFiles(List<CMethod> methodList, Integer bands, Integer rows) {
        Set<Tuple2<Integer, Integer>> pairList = new TreeSet<>();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().build();
        // LSH Minhash signatures from b bands with r rows
        for (int band = 0; band < bands; band++) {
            Reader minhashReader = null;
            try {
                minhashReader = new FileReader("/Users/jnavas/temp/minhash" + String.valueOf(band) + ".csv");
                List<List<Integer>> methodsMinhashes =
                    csvFormat.parse(minhashReader)
                        .stream()
                        .map(csvRecord -> csvRecord.stream().map(Integer::valueOf).toList())
                        .toList();
                minhashReader.close();
                Files.delete(Path.of("/Users/jnavas/temp/minhash" + String.valueOf(band) + ".csv"));
                //System.out.print("\\");
                List<CMethod> badMethods = methodList.stream().filter(method -> method.getIdx() == methodList.size()).toList();
                // DEBUG: DELETE
                //if (badMethods.size() > 0) {
                //    boolean wtf = true;
                //}
                // collect candidate methods in buckets per band
                ConcurrentMap<Integer, List<Integer>> buckets =
                    methodList.stream()
                        .map(CMethod::getIdx)
                        .toList()
                        .parallelStream()
                        .collect(Collectors.groupingByConcurrent(method -> {
                            try {
                                return methodsMinhashes.get(method).hashCode();
                            }
                            catch (Exception ex) {
                                return -1024;
                            }
                        }));
                if (buckets.containsKey((int)-1024)) {
                    buckets.remove((int)-1024);
                }
                //System.out.print("_");
                // add candidate pairs to tuples pairList
                List<List<Tuple2<Integer, Integer>>> pairLists = buckets.values()
                    .stream()
                    .filter(lst -> lst.size() > 1)
                    .toList()
                    .parallelStream()
                    .map(lst -> {
                        List<Tuple2<Integer, Integer>> tuple2List = new ArrayList<>();
                        for (int left = 0; left < lst.size() - 1; left++) {
                            for (int right = left + 1; right < lst.size(); right++) {
                                Integer leftMethod = Math.min(lst.get(left), lst.get(right));
                                Integer rightMethod = Math.max(lst.get(left), lst.get(right));
                                if (!leftMethod.equals(rightMethod)) {
                                    Tuple2<Integer, Integer> pair = new Tuple2<>(leftMethod, rightMethod);
                                    tuple2List.add(pair);
                                }
                            }
                        }
                        return tuple2List;
                    })
                    .toList();
                pairLists.stream().forEach(pairList::addAll);
                //System.out.print("/");
            }
            catch (IOException io) {
                io.printStackTrace();
            }
        }
        List<Clone> methodPairs = pairList.parallelStream()
            .map(pair -> {
                Clone clone = new Clone();
                ClonePair clonePair = new ClonePair();
                clone.addClonePair(clonePair);
                clonePair.setClone(clone);
                clonePair.addFragments(CMethodDict.list().get(pair.getV1()), CMethodDict.list().get(pair.getV2()));
                return clone;
            })
            .collect(Collectors.toList());
        return methodPairs;
    }
}
