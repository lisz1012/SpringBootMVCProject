package com.lisz.controller;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cache")
public class CacheController {

    @RequestMapping("/")
    public ResponseEntity<String> last(@RequestHeader(value="IF-Modified-Since",required = false) Date ifModifiedSince) {

        // 想象成是一个ConcurrentHashMap，是线程安全的，而且自带超时功能。这个LoadingCache跟RateLimiter用法的不同之处在于流量超出限制之后，这里是报错，而RateLimiter是阻塞等待
        LoadingCache<Long, AtomicLong> counter = CacheBuilder
                                                .newBuilder()
                                                .expireAfterWrite(2, TimeUnit.SECONDS) //自带超时功能，反正就只统计过去一秒的访问量，2秒过期
                                                .build(
                                                    new CacheLoader<Long, AtomicLong>() {
                                                        @Override
                                                        public AtomicLong load(Long aLong) throws Exception {
                                                            return new AtomicLong(0);
                                                        }
                                                    }
                                                );
        try {
            if (counter.get(System.currentTimeMillis()/1000).incrementAndGet() > 100) { // 每秒钟最多承受100个request
                return new ResponseEntity<>("", null, HttpStatus.SERVICE_UNAVAILABLE);
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);

        long now = System.currentTimeMillis() / 1000 *1000;

        HttpHeaders headers = new HttpHeaders();

        String body = "<a href =''>hi点我</a>";

        String ETag = getMd5(body);

        headers.add("Date", simpleDateFormat.format(new Date(now)));
        headers.add("ETag", ETag);

        return new ResponseEntity<>(body,headers,HttpStatus.OK);
    }

    /**
     * 字符串转md5
     * @param msg
     * @return
     */
    private String getMd5(String msg) {
        MessageDigest md5 = null;

        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        md5.update(msg.getBytes());
        byte[] digest = md5.digest();

        StringBuffer buf = null;
        buf = new StringBuffer(digest.length * 2);
        //遍历
        for (int i = 0; i < digest.length; i++) {
            if (((int) digest[i] & 0xff) < 0x10) { //(int) b[i] & 0xff 转换成无符号整型
                buf.append("0");
            }
            //Long.toHexString( 无符号长整数的十六进制字符串表示
            buf.append(Long.toHexString((int) digest[i] & 0xff));
        }
        return buf.toString();
    }
    @GetMapping("hello")
    public void hello(){
        System.out.println("hello");
    }
}