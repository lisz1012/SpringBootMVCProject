package com.lisz;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Test {
    private static final int TIMES = 10000000;

    public static void main(String[] args) {
        Map<Integer, Integer> map1 = Collections.synchronizedMap(new HashMap<>());
        Map<Integer, Integer> map2 = new ConcurrentHashMap<>();
        Map<Integer, Integer> map3 = new HashMap<>();
        Map<Integer, Integer> map4 = new HashMap<>();

        long start = System.currentTimeMillis();
        for (int i = 0; i < TIMES; i++) {
            map1.put(i, i);
        }
        System.out.println("Synchronized Map: " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < TIMES; i++) {
            map2.put(i, i);
        }
        System.out.println("ConcurrentHashMap: " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < TIMES; i++) {
            synchronized (Test.class) {
                map3.put(i, i);
            }
        }
        System.out.println("synchronized block: " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < TIMES; i++) {
            map4.put(i, i);
        }
        System.out.println("HashMap: " + (System.currentTimeMillis() - start));
    }
}
