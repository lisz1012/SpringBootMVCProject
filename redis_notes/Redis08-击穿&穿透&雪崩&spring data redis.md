# Redis08-击穿&穿透&雪崩&分布式锁&spring data redis

题目中的这些概念是Redis做缓存的时候才会出现的，所以一般的场景下Redis的后面还会有一个DB。Redis的Client一般是指某个service，他来取DB的数据，然后他的前面可能还有多个Service，组成一个service的群体
再往前是nginx和lvs组成的接入层，最前面的真实client才是活人，造成所有的行为和流量，比方说100000的并发，由于前面几层的技术，包括nginx和Redis，压到数据库上的可能只有几百或者几千。这才是架构师横看项目
的时候该看的东西

Jedis底层是线程不安全的，虽然有poll但是不如luttce性能好。顶多大数据里会用到Jedis

### 一、常见概念

- 击穿：

  - 概念：redis作为缓存，设置了一个key的过期时间，人为设置了过期时间，或者Redis开启了LRU或者LFU自动清理相对冷的数据。这个key在过期的时候刚好出现并发访问，直接击穿redis，访问数据库。这里讨论的击穿，
  		 前面一定是出现了高并发，而且key还过期了，这时候几千几万的流量压到数据库了

  - 解决方案：使用setnx() ->相当于一把锁，设置的时候，发现设置过期，加锁，只有获得锁的人才可以访问DB，这样就能防止击穿。10000个请求打到Redis上，第一个请求发现没有key，他就会回到客户端，然后他再次
             排到去Redis的请求队列的最后一个位置，执行setnx，而所有的请求都发送setnx的话就相当于都去抢一把锁，第一个请求会抢到这把锁，只有他这个设置成功的请求才可以去访问数据库。而没有获得锁的请求
             要等待几秒钟，然后在尝试去Redis取key。这里用用到了Redis单线程的特性。setnx相当于上了一把锁，宁可睡一会儿（睡多长时间看压测，不能能睡几秒，否则前面各个service早超时了），也要保护数据库

  - 逻辑：

    ```
    1. get key
    2. setnx
    3. if ok addDB
       else sleep 
            go to 1 重新开始下一循环
    ```

  - question1：如果第一个加锁的人挂了（访问数据库的时候出现阻塞、拥塞甚至是断网）？ 可以设置锁的过期时间

- question2：如果第一个加锁的人没挂，但是锁超时了？ 可以使用多线程，一个线程取库，一个线程监控前一个线程是否存活，如果还没取回来就到时间了则更新延长锁时间。但这样会让客户端的代码逻辑复杂度提高，技术讨论到
			 这儿就可以了，分布式条件下协调实现锁，成本还是很高的，麻烦，为了后边引入zookeeper做个铺垫

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

