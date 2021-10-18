package com.lisz.test;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;

public class Test {
    public static void main(String[] args) {
        Integer i1 = new Integer(1);
        Integer i2 = new Integer(1);
        System.out.println(i1 == i2);
        Integer i3 = 127;
        Integer i4 = 127;
        System.out.println(i3 == i4);
        Integer i5 = 10000;
        Integer i6 = 10000;
        System.out.println(i5 == i6);
        DelayQueue<Delayed> queue = new DelayQueue<>();
    }
}
