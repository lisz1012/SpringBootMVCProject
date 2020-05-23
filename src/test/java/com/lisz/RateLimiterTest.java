package com.lisz;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

// 在单进程内调节
public class RateLimiterTest {
    @Test
    public void test1() {
        RateLimiter limiter = RateLimiter.create(2); // 每秒产生两个令牌，0.5秒一个，一个个的生产，生产者速率
        // limiter.acquire()是阻塞式方法
        System.out.println(limiter.acquire());
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(limiter.acquire(4)); // 我后面的limiter.acquire()那个需要等的时间比较长，需要拿到4个令牌才可以不阻塞了
        System.out.println(limiter.acquire());
        System.out.println(limiter.acquire());
        System.out.println(limiter.acquire());

        System.out.println(limiter.acquire());
        System.out.println(limiter.acquire());
    }

    @Test
    public void test2() {
        RateLimiter limiter = RateLimiter.create(2); // 每秒产生两个令牌，0.5秒一个，生产者速率
        System.out.println(limiter.acquire(20)); // 这一行后面再有limiter.acquire()的话，需要拿到20个令牌才可以
        System.out.println(limiter.acquire());
        System.out.println(limiter.acquire());
        System.out.println(limiter.acquire());
        //System.out.println(limiter.tryAcquire(2000, Duration.ofSeconds(5)));
    }

    @Test
    public void test3() {
        RateLimiter limiter = RateLimiter.create(2); // 每秒产生两个令牌，0.5秒一个，一个个的生产，生产者速率
        System.out.println(limiter.tryAcquire(10, Duration.ofSeconds(10))); // 返回本次尝试在规定时间内取没取到，同时设定下一次要求取得多少个令牌才可以
        System.out.println(limiter.acquire(10));
        System.out.println(limiter.tryAcquire(5, Duration.ofSeconds(4))); // 本次尝试取只给4s，取不到就返回false，前面设置要求10个，4s是凑不齐的，则不尝试等待，立刻返回false；如果能凑齐则等待，且最终返回true
    }
}
