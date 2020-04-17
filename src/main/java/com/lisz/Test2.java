package com.lisz;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Test2 {
    private static final int TIMES = 10000000;

    public static void main(String[] args) {
        Map<Object, Object> map1 = Collections.synchronizedMap(new HashMap<>());
        Map<Object, Object> map2 = new ConcurrentHashMap<>();
        Map<Object, Object> map3 = new HashMap<>();
        Map<Object, Object> map4 = new HashMap<>();

        Object objects[] = new Object[TIMES * 2];

        for (int i = 0; i < objects.length; i++) {
            objects[i] = new Object();
        }

        long start = System.currentTimeMillis();
        for (int i = 0; i < TIMES; i++) {
            map1.put(objects[i], objects[i + TIMES]);
        }
        System.out.println("Synchronized Map: " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < TIMES; i++) {
            map2.put(objects[i], objects[i + TIMES]);
        }
        System.out.println("ConcurrentHashMap: " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < TIMES; i++) {
            synchronized (Test2.class) {
                map3.put(objects[i], objects[i + TIMES]);
            }
        }
        System.out.println("synchronized block: " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < TIMES; i++) {
            map4.put(objects[i], objects[i + TIMES]);
        }
        System.out.println("HashMap: " + (System.currentTimeMillis() - start));
    }
}
