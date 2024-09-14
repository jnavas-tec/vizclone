package cr.ac.tec.vizClone.utils;

import cr.ac.tec.vizClone.model.CMethod;
import cr.ac.tec.vizClone.model.Clone;
import cr.ac.tec.vizClone.model.ClonePair;
import groovy.lang.Tuple2;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

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
import java.util.stream.StreamSupport;

public class ShingleDict {
    private static List<String> shingleArray = new ArrayList<>();
    private static Map<String, Integer> shingleDict = new Hashtable<>();
    private static ArrayList<Integer> hash;

    // n = 250, b = 25, r = 10, t = (1/b)^(1/r) = 0.73 of similitude
    // n = 200, b = 10, r = 20, t = (1/b)^(1/r) = 0.89 of similitude
    // n = 200, b =  5, r = 40, t = (1/b)^(1/r) = 0.96 of similitude
    /*
        n = 300
            b = 2.0,   r = 150.0, t = 0.995
            b = 3.0,   r = 100.0, t = 0.989
            b = 4.0,   r = 75.0,  t = 0.981
            b = 5.0,   r = 60.0,  t = 0.973
            b = 6.0,   r = 50.0,  t = 0.964
            b = 10.0,  r = 30.0,  t = 0.926
            b = 12.0,  r = 25.0,  t = 0.905
            b = 15.0,  r = 20.0,  t = 0.873
            b = 20.0,  r = 15.0,  t = 0.818
            b = 25.0,  r = 12.0,  t = 0.764
            b = 30.0,  r = 10.0,  t = 0.711
            b = 50.0,  r = 6.0,   t = 0.521
            b = 60.0,  r = 5.0,   t = 0.440
            b = 75.0,  r = 4.0,   t = 0.339
            b = 100.0, r = 3.0,   t = 0.215
            b = 150.0, r = 2.0,   t = 0.081

        n = 250
            b = 2.0,   r  = 125.0, t = 0.994
            b = 5.0,   r  = 50.0,  t = 0.968
            b = 10.0,  r = 25.0,   t = 0.912
            b = 25.0,  r = 10.0,   t = 0.724
            b = 50.0,  r = 5.0,    t = 0.457
            b = 125.0, r = 2.0,    t = 0.089

        n = 200
            b = 2.0,   r = 100.0, t = 0.993
            b = 4.0,   r = 50.0,  t = 0.972
            b = 5.0,   r = 40.0,  t = 0.960
            b = 8.0,   r = 25.0,  t = 0.920
            b = 10.0,  r = 20.0,  t = 0.891
            b = 20.0,  r = 10.0,  t = 0.741
            b = 25.0,  r = 8.0,   t = 0.668
            b = 40.0,  r = 5.0,   t = 0.478
            b = 50.0,  r = 4.0,   t = 0.376
            b = 100.0, r = 2.0,   t = 0.1

        n = 1000
            b = 2.0,   r = 500.0, t = 0.9986
            b = 4.0,   r = 250.0, t = 0.9944
            b = 5.0,   r = 200.0, t = 0.9919
            b = 8.0,   r = 125.0, t = 0.9835
            b = 10.0,  r = 100.0, t = 0.9772
            b = 20.0,  r = 50.0,  t = 0.9418
            b = 25.0,  r = 40.0,  t = 0.9226
            b = 40.0,  r = 25.0,  t = 0.8628
            b = 50.0,  r = 20.0,  t = 0.8223
            b = 100.0, r = 10.0,  t = 0.6309
            b = 125.0, r = 8.0,   t = 0.5468
            b = 200.0, r = 5.0,   t = 0.3465
            b = 250.0, r = 4.0,   t = 0.2514
            b = 500.0, r = 2.0,   t = 0.0447
     */

    /* n = 1000, b = 100, r = 10 => 63% */
    /* n = 1000, b = 40,  r = 25 => 86% */
    public static final Integer NUM_MIN_HASHES = 1000; // 300; //200;//300;//250;//200;
    public static final Integer b = 50; // 25; //5;//25;//25;//5;
    public static final Integer r = 20; // 12; //40;//12;//10;//40;

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
                                method.getShingleSignature().set(h_i, Math.min(method.getShingleSignature().get(h_i), hash.get(shingle)))
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
                // collect candidate methods in buckets per band
                ConcurrentMap<Integer, List<Integer>> buckets =
                    methodList.stream()
                        .map(CMethod::getIdx)
                        .toList()
                        .parallelStream()
                        .collect(Collectors.groupingByConcurrent(method -> methodsMinhashes.get(method).hashCode()));
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
                    .collect(Collectors.toList());
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
