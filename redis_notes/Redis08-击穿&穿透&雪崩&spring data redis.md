# Redis08-击穿&穿透&雪崩&分布式锁&spring data redis

题目中的这些概念是Redis做缓存的时候才会出现的，所以一般的场景下Redis的后面还会有一个DB。Redis的Client一般是指某个service，他来取DB的数据，然后他的前面可能还有多个Service，组成一个service的群体
再往前是nginx和lvs组成的接入层，最前面的真实client才是活人，造成所有的行为和流量，比方说100000的并发，由于前面几层的技术，包括nginx和Redis，压到数据库上的可能只有几百或者几千。这才是架构师横看项目
的时候该看的东西

Jedis底层是线程不安全的，虽然有poll但是不如luttce性能好。顶多大数据里会用到Jedis。这里主角将是spring

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

- question2：如果第一个加锁的人没挂，但是锁超时了？ 可以使用多线程，一个线程取库，一个线程监控前一个线程是否存活，如果还没取回来就快到时间了则更新延长锁时间。但这样会让客户端的代码逻辑复杂度提高，技术讨论到
			 这儿就可以了，分布式条件下协调实现锁，成本还是很高的，麻烦，为了后边引入zookeeper做个铺垫

- 穿透：

  - 概念：从业务接收查询的是你系统缓存和数据库都根本不存在的数据，这时候刚好从redis穿透到数据库，这样也消耗了数据库的性能

  - 解决方案：

    使用布隆过滤器，不存在的数据使用bitmap进行拦截

    - 1.使用布隆过滤器。从客户端包含布隆过滤器的算法，这样连Redis也到达不了
    - 2.客户端只包含算法，布隆过滤器的bitmap是在Redis上，数据从客户点迁移到了Redis缓存，所有服务是无状态的，服务里只有算法，跟上节课讲的代理那个模式是一样的。
    - 3.Redis直接集成Bloom模块，变成一个指令，这样客户端上算法和数据都没有，客户端相对简单一些

  - question1:布隆过滤器只能查看、增加，不能删除。解决方案：换cuckoo这种支持删除的过滤器，或者使用空key。

- 雪崩：

  - 概念：大量的key同时失效，造成雪崩。比如确定某个时间点的到期时间，间接造成大量的访问到达数据库几百个key过期，每个key上有几十个请求压到数据库上

  - 解决方案：随机化过期时间，在失效的基础上，再加入一个时间（1-5min）。定时失效key是根源。到点必须过期，比如每天的利率，零点必须发生变化，则不能使用随机过期时间的办法。解决办法就跟击穿有点相似，到零点了，
  			必须所有key都下来，大家再来请求这个key的时候，为了整个项目架构的数据一致性，就让大家像击穿的时候一样：拿不到key的值就回头排到队尾然后去尝试setnx拿锁，第一个请求会拿到锁，然后去DB中取值，
  			并更新Redis，其他的线程拿不到锁，在那里转圈圈等待，一旦到时间就再次尝试去拿那个key的值。在前面的业务client中可以加一个判断，零点延时：只要到了夜里12点，发request的线程随机睡几秒。这样只有
  			第一个请求去访问数据库了，其余的仍然正常进行
  			相反，如果不是类似这种零点必须过期的，那是咱自己设计的有问题，可以用LRU或者LFU分批淘汰不常用的keys

雪崩比击穿更容易发生，击穿发生的比较少。架构师要横着看项目，越往右边，流量越小，层层限流


### 用Redis做分布式锁

`setnx`、`setnx + 过期时间`、 `setnx + 过期时间` + 守护线程（延长过期时间） 可用redisson框架，但是用Redis可以做分布式锁不常见，否则也不会再开发一个zookeeper，zookeeper做分布式锁是最容易的，如果
涉及到分布式锁，尽量使用zookeeper。因为既然已经涉及到分布式锁了，本身对效率的要求并不是特别高，而更加看重准确度和一致性，虽然zookeeper肯定没有Redis快（后面讲到Zookeeper就理解了），但是它一定会尽量做到
数据的可靠性，而且API开发还很简单。一般跟面试官把上面Redis的三种手段描述清楚，他就知道你有很强的大局观了，接下来可以谈Zookeeper（套路）

    

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

注：tb双十一会有预加载，例子是提前几天更新手机客户端，这样会把缓存数据提前加载到客户端，打散流量，把访问量前置了，前一周或者一个月让大量的人分散的把双十一时的缓存下载好。这样到了双十一晚上12点的时候不会有
   太大的流量去整体做更新。这个时候你必须已经准备好了未来那个时点的数据，才可以使用预加载这个方案