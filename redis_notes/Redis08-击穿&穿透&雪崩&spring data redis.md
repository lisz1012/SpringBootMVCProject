# Redis08-击穿&穿透&雪崩&分布式锁&spring data redis

Jedis底层是线程不安全的，虽然有poll但是不如luttce性能好。顶多大数据里会用到Jedis

### 一、常见概念

- 击穿：

  - 概念：redis作为缓存，设置了key的过期时间，key在过期的时候刚好出现并发访问，直接击穿redis，访问数据库

  - 解决方案：使用setnx() ->相当于一把锁，设置的时候，发现设置过期，加锁，只有获得锁的人才可以访问DB，这样就能防止击穿。

  - 逻辑：

    ```
    1. get key
    2. setnx
    3. if ok addDB
       else sleep 
            go to 1
    ```

  - question1：如果第一个加锁的人挂了？ 可以设置过期时间

- question2：如果第一个加锁的人没挂，但是锁超时了？ 可以使用多线程，一个线程取库，一个线程监控前一个线程是否存活，更新锁时间。

- 穿透：

  - 概念：从业务接收查询的是你系统根本不存在的数据，这时候刚好从redis穿透到数据

  - 解决方案：

    使用布隆过滤器，不存在的数据使用bitmap进行拦截

    - 1.使用布隆过滤器。从客户端包含布隆过滤器的算法。
    - 2.直接redis集成布隆模块。

  - question1:布隆过滤器只能查看，不能删除？解决方案：换cuckoo过滤器。

- 雪崩：

  - 概念：大量的key同时失效，造成雪崩。

  - 解决方案：在失效的基础上，再加入一个时间（1-5min）

    

### 二、SpringDataRedis

客户端连接，我们可以使用Jedis、lettuce、redisson...但是，我们在技术选型时，鉴于多方面考虑，选用SpringDataRedis

##### 1.创建一个SpringBoot项目，勾选Spring Data Redis，也可以直接引入

```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

##### 2.使用序列化的方式，进行set和get值（乱码）

```
ValueOperations vo = redisTemplate.opsForValue();
vo.set("Hello","china");
System.out.println(vo.get("Hello"));
```

##### 3.使用StringRedisTemplate来调整乱码情况

```
ValueOperations<String, String> svo = stringRedisTemplate.opsForValue();
svo.set("a","b");
System.out.println(svo.get("a"));
```

##### 4.Hash操作

```
HashOperations<String,Object,Object> hash=stringRedisTemplate.opsForHash();
hash.put("sean","name","steve yu");
hash.put("sean","age","20");
hash.put("sean","sex","M");
System.out.println(hash.get("sean","name"));;
System.out.println(hash.get("sean","age"));;
System.out.println(hash.get("sean","sex"));;
```

##### 5.对象操作(这边需要引入Spring Json)

```
HashOperations<String,Object,Object> hash=stringRedisTemplate.opsForHash();
hash.put("sean","name","steve yu");
hash.put("sean","age","20");
hash.put("sean","sex","M");
System.out.println(hash.get("sean","name"));;
System.out.println(hash.get("sean","age"));;
System.out.println(hash.get("sean","sex"));;
//5.对象转哈希存储操作
Person p=new Person();p.setAge(15);p.setName("steve yu");
Jackson2HashMapper jm = new Jackson2HashMapper(objectMapper, false);
stringRedisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<Object>(Object.class));
stringRedisTemplate.opsForHash().putAll("sean01",jm.toHash(p));
Map<Object, Object> map = stringRedisTemplate.opsForHash().entries("sean01");
System.out.println(map);
Person person = objectMapper.convertValue(map, Person.class);
```

