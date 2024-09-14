package cr.ac.tec.vizClone.utils;

import java.util.*;

public class BucketList {
    private Map<Integer, Integer> bucketMap = new HashMap<>();
    private List<List<Integer>> bucketLists = new ArrayList<>();

    public BucketList() {}
    public BucketList(List<List<Integer>> lists) {
        for (List<Integer> list: lists) {
            int nextIdx = bucketLists.size();
            for (Integer elem: list) {
                bucketMap.put(elem, nextIdx);
            }
            bucketLists.add(list);
        }
    }

    public void merge(List<List<Integer>> lists) {
        for (List<Integer> list: lists) {
            Integer found = -1;
            // search for existing element
            for (int e = 0; e < list.size(); e++) {
                if (bucketMap.containsKey(list.get(e))) {
                    found = e;
                    list.remove(e);
                    break;
                }
            }
            // if not found add a new bucket
            if (found == -1) {
                int nextIdx = bucketLists.size();
                for (Integer elem: list) {
                    bucketMap.put(elem, nextIdx);
                }
                bucketLists.add(list);
            }
            // if found merge buckets with intersection bucket
            else {
                for (int e = 0; e < list.size(); e++) {
                    Integer elem = list.get(e);
                    // if element's bucket exists merge
                    if (bucketMap.containsKey(elem)) {
                        if (!Objects.equals(bucketMap.get(elem), found)) {
                            Integer elemBucket = bucketMap.get(elem);
                            List<Integer> elemBucketList = bucketLists.get(elemBucket);
                            List<Integer> foundBucketList = bucketLists.get(found);
                            for (Integer element: elemBucketList) {
                                bucketMap.put(element, found);
                            }
                            foundBucketList.addAll(elemBucketList);
                            elemBucketList.clear();
                        }
                    }
                    // if element's bucket does not exist add element
                    else {
                        bucketMap.put(elem, found);
                        bucketLists.get(found).add(elem);
                    }
                }
            }
        }
    }

    public List<List<Integer>> getBucketLists() {
        return bucketLists
            .stream()
            .filter(lst -> lst.size() > 1)
            .toList();
    }
}
