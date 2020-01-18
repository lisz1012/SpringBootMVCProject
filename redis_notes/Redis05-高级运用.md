# Redis高级运用

##### 一、管道连接redis（一次发送多个命令，节省往返时间）

1.安装nc

```
yum install nc -y
```

2.通过nc连接redis

```
nc localhost 6379
```

3.通过echo向nc发送指令

```
echo -e "set k2 99\nincr k2\n get k2" |nc localhost 6379
```

Redis冷启动：刚启动的Redis里面是空的，而我们希望他预加载一些数据，可以先起来然后写个程序，抽取库里的数据往里放，或者别人有请求到的数据，顺便放到Redis里，或者直接跑一个程序把数据写成文件一股脑写道Redis里面去

##### 二、发布订阅(pub/Sub)

```
publish channel message
```

```
subscribe channel
```
只有消费端监听以后，别人再推送消息才能看到。直播间里的聊天可以用这个做。在使用微信、QQ、腾讯课堂的时候有这么个现象：除了进入聊天室能够看到新的消息，往上滑动的时候还能拿到之前的消息。所有的数据放到哪里存呢？关系型数据库还是Redis呢？放到
数据库的话，数据全亮可以保证，但是多人查的时候成本高。作为微信客户端来说，有实时性的需求，就是实时知道大家在聊什么；还有一个需求就是看一些历史的聊天。而历史的聊天又分为X天之内的和更老的数据。全量数据是在数据库里。可以拿Redis做缓存，
目的是解决数据的读请求（也有写请求，对于数据一致性要求不高的，比如浏览数购买数收藏数等，少个十个八个的也无所谓了，不要非得因为要达到一致性而灭了Redis的特征，Redis要求的就是快）。做带过期时间的缓存，过期的存进数据库，Redis只存最近一个月
的数据,实时聊天的东西可以用pubsub来实现；三天之内的数据可以用Redis的zset数据类型，可以使用zset命令`ZREMRANGEBYSCORE key min max`留下大的天数，剔除小的天数所以max应该是三天前，正向的就可以了，Redis由小到大排列的，把时间long作为
记录的score，把消息作为元素就可以了放进zset的时候就已经排好序了. 具体来讲，一种设计方案是client先往pubsub发送数据，再往Redis的zset放数据，然后再往Kafka放数据，Kafka怼在数据库那边，慢慢的累积写入数据库。微信所有人聊天，对于内存
是可以承受的，但是如果流量直接给数据库，肯定就压趴下了。另一种设计是：再起一个Redis进程，也订阅这个聊天频道，一旦收到消息，就把它写入zset；再来一个Redis进程，也订阅这个频道，一旦收到消息再转给Kafka，在写入数据库。Redis的计算并不多，
只是个内存存储层，消息到了广播出去。这样就避免了client在两次调用Redis之间宕机的问题。第三种方案：使用Redis事务避免client在两次调用Redis之间宕机的问题。Redis事务没有回滚


##### 三、事务（transactions）

```
multi 开启事务
...
exec 执行事务
```
先收集命令，然后一次性发给Redis，Redis按顺序一次性执行所有的命令。如果出现错误的话会撤销。要么成功要么失败，但不是百分之百的，先有这么个概念。这里回顾上一节课的知识：Redis是单进程的，多个客户端连上来，一个客户端的事务不会阻碍别的客户端
即便两个客户端想操作同一个数据，首先他们得开启事物，而在Redis那边，所有的指令都是排着队串行过来的。假设client1先发送指令，他就先发送一个开启事务的标记：MULTI，当client1还没发过来指令的时候，client2也发了一个开启事务的标记MULTI，则
client2发的MULTI会排在client1的MULTI的后面，以后即便是client2比client1手速快，一气儿发了好几条过来，在Redis看来各条命令如下：
```
client1: MULTI
client2: MULTI
client2: del k1
client1: get k1
client2: EXEC
client1: EXEC
```
其实就是看最后两个EXEC哪个先执行。每个客户端过来的命令单独放在一个缓冲区。现在是EXEC先到达了，那执行的顺序就是：
```
client2: MULTI
client2: del k1
client2: EXEC
client1: MULTI
client1: get k1
client1: EXEC
```
client1拿不到了。而如果最后client1的EXEC先到达，像下面这样：
```
client1: MULTI
client2: MULTI
client2: del k1
client1: get k1
client1: EXEC
client2: EXEC
```
则最终执行顺序成了：
```
client1: MULTI
client1: get k1
client1: EXEC
client2: MULTI
client2: del k1
client2: EXEC
```
client1先读到，client2再删除。谁的EXEC先到达Redis就先执行谁的事务
```
watch 如果数据被更改，那就不执行事务，一般在发MULTI之前就发送，如果EXEC的时候发现当初wantch的key已经更改了，各条指令是不执行的，直接就把事务给撤销了，这没办法，Redis服务端就只能帮你到这个程度，客户端需要自己捕获这件事情，自行修复处理
unwatch 取消监视
```
client1:
```
127.0.0.1:6379> set k1 a
OK
127.0.0.1:6379> WATCH k1
OK
127.0.0.1:6379> MULTI
OK
127.0.0.1:6379> set k1 aa
QUEUED
127.0.0.1:6379> EXEC

127.0.0.1:6379> get k1
aaa
```
原因是client2在client1之前EXEC: 
```
127.0.0.1:6379> MULTI
OK
127.0.0.1:6379> set k1 aaa
QUEUED
127.0.0.1:6379> EXEC
OK
127.0.0.1:6379> get k1
aaa
```


```
discard 放弃事务
```

##### 四、布隆过滤器（redisbloom）

在redis.io/modules选择redisbloom的github：https://github.com/RedisBloom/RedisBloom，克隆下来: 右键Download ZIP，copy Link，然后再在linux中：wget https://github.com/RedisBloom/RedisBloom/archive/master.zip
如果需要的话`yum install unzip`, 然后unzip 刚下好的文件

解压，make编译，将编译后生成的redisbloom.so这个链接库复制到/opt/redis （其实放在哪里都行，习惯放在这里）

执行

```
redis-server /etc/redis/6379.conf --loadmodule /opt/redis/redisbloom.so 
```
/etc/redis/6379.conf 可以不加, --loadmodule 后面要写绝对路径.`redis-cli -p 6379`之后再敲"BF"就会出现一些新命令了。  
布隆过滤器解决缓存穿透的问题，比如：有些数据是缓存和数据库都没有的，这时用户查找这些数据的垃圾请求过来，相当于让数据库和服务器做很多没有意义的事情。布隆过滤器首先搞清楚数据库里都有啥，放在一个集合里，用户要查的东西发送过来之后，在Redis
里面一查找，如果没找到，那就不用去数据库了，找到的话返回，或者再去数据库。但是数据那么多，可怎么存得下呢？别忘了我们Redis这里只存数据存不存在这个信息，布隆过滤器就是解决：如何用小的空间解决大量数据匹配的过程。做法其实很简单：先找到数据
库里有的元素，将他们经过k个哈希函数计算，结果得到k个数字，再以这些数字为下标，做类似的setbit，把bitmap中相应的下标为的数字置1；再来一个新元素的访问请求的时候，将这个新元素也算k次哈希值，然后看看是不是所有的结果所对应的下标位上的置都是
1，如果是这样的，那它是个数据库已有的元素，否则就是个新元素，将不是1的位置置1. 有可能几个哈希函数会有碰撞，但是不太可能都碰撞上。一旦发现不全是1，则说明是数据库里没有的的元素。但也有可能新元素匹配上的1来自不同的老元素, 其实数据库里没有，
但是布隆过滤器认为他有，就会放过来到数据库。所以布隆过滤器是个概率的，不可能100%阻挡恶意请求可以降低到<1%。bitmap里面剩下的0越多越好使  
1. 你有啥？（自己要往里添加：`BF.ADD key value`）2. 标记bitmap3.请求的可能被误标记4.一定概率会大量减少放行：穿透5.而且成本低，因为是二进制位。  
在架构师的角度考虑，可以把bloom算法和bitmap放在客户端，服务端只有原生的Redis；也可以把bloom算法放在client端，bitmap放在服务端；还可以像这样集成布隆过滤器。这就取决于我们需要的性能和成本了。如果所有东西都压在Redis且它是一个memory
内存级的，Redis对CPU的损耗并不大，这样可以让客户端更轻量一些，也更符合“微服务”的概念：所有的东西都迁出去，也更符合未来Service Mesh的设计理念.如果穿透了，而数据库里却没有，则加一个key-value对，value是error或者null以便下次直接返回  
下次再查的时候，先命中这个key，就不走布隆过滤器，直接返回了。  

数据库一旦增加了元素，必须完成元素对bloom的添加，这样别人通过bloom才能请求到这个数据（这里面会有很多坑，双写了）

简单来说：  

布隆过滤器说某个元素在，可能会被误判。  
布隆过滤器说某个元素不在，那么一定不在。  

小作业，爱做不做：高布隆过滤器和cukcoo，counting bloom 


```
科普：bloom filter,counting bloom,cukcoo是什么？
1.bloom filter
它实际上是一个很长的二进制向量和一系列随机映射函数。布隆过滤器可以用于检索一个元素是否在一个集合中。它的优点是空间效率和查询时间都远远超过一般的算法，缺点是有一定的误识别率和删除困难。
运用:网页黑名单、垃圾邮件、爬虫网址判重
Java想要使用BloomFilter可以考虑使用google的guava（这个在resolve dependency conflict方面又是一个大坑）

2.counting bloom
这个计数器，使用4位bit来表示一个计数（这个数字可以自己指定长度的），所以我们可以进行计数。

（详细参考：https://wenku.baidu.com/view/9e5832df7f1922791688e84f.html）

3.cuckcoo
cuckoo filter的产生源于一个故事，盒子故事（参考：https://www.cnblogs.com/chuxiuhong/p/8215719.html）
我们使用2个表存，可以存放在任意一个，那么数学期望由O(logN/loglogN)变成O(loglogN)。
因此，而设计出来布谷过滤器。
布谷过滤器使用2个哈希表，元素计算哈希，如果没有值，则放入，有值，则踢出元素，重新计算新的哈希，放入，如此反复。参考附件：cuckoo filter

redis的布隆过滤器模块也是用到了cuckoo的哈希，在cuckoo.c文件中

```



Redis Bloom的使用：

```
BF.ADD k1 V   添加数据值
BF.EXISTS k1 V    判断是否存在
```

##### Redis作为数据库和缓存的区别

缓存数据其实“不重要”，先要有数据库，加个缓存并不是全量数据，而且缓存应该随着访问变化，要缓存是减轻后端的访问压力，缓存里应该放的是前面请求的、热数据。Redis作为缓存，要求是里面的数据怎么能随着业务而变化：只保留热数据，因为内存大小
是有限的，也是个瓶颈。 这就引出了key的有效期，有效期可以由：1。业务逻辑推动（带有效期的） 2. 业务运转，随着访问的变化应该淘汰掉冷数据。 内存有多大呢？看/etc/redis/6379.conf, maxclients表示最大有多少个socket连接可以往Redis
传命令。而`maxmemory <bytes>`可以设置内存的大小，一般给1-10G，因为太大的话做半持久化存储成本高、做数据迁移成本高.`maxmemory-policy noeviction`控制maxmemory满了应该踢掉谁选项有如下几种：
```
# volatile-lru -> Evict using approximated LRU among the keys with an expire set.
# allkeys-lru -> Evict any key using approximated LRU.
# volatile-lfu -> Evict using approximated LFU among the keys with an expire set.
# allkeys-lfu -> Evict any key using approximated LFU.
# volatile-random -> Remove a random key among the ones with an expire set.
# allkeys-random -> Remove a random key, any key.
# volatile-ttl -> Remove the key with the nearest expire time (minor TTL)
# noeviction -> Don't evict anything, just return an error on write operations.
```
allkeys但是作用于所有的key，volatile是马上要过期的。lru指的是多久没碰的，越久没碰的越优先删除；lfu是指使用频率最低的，使用次数最少的优先删除；ttl是指剔除离过期时间（如果有的话）最近的。作为缓存的话`noeviction`是不能使用的；
但是作为数据库的话，一定要使用他的。所以作为缓存的话，最好是lru或者lfu，因为random的太随意，而ttl成本高。大量的设置过期的话，就用volatile的，否则选择allkeys，因为设置了过期时间的并不多，所以释放的量不是很大。有效期是系统
业务逻辑给定的。ttl=10s，被访问之后不会被延长，而且除了在最初`set k1 ex 50`的时候设置过期时间之外，还可以用`EXPIRE k1 50`设置某个k1在50秒之后过期. 一旦ttl倒计时之中，重新set了一下这个key：`set k2 bbb`，这样则会使得
这个key的过期时间为-1，永久存在。发生了不带ex参数的写操作，会剔除过期时间；发生了带ex参数的写，可以重置过期时间。`EXPIREAT key timestamp`命令, 可以设置在什么时候过期，定死时间。刚刚说的这些keys就是volatile集合里的那些
key  

过期判定原理：已经过期的key被访问的时候，比对过期时间，一看过期了，则返回nil然后清除key，这是被动的方式，但过期的key永远不被访问的话就永远在内存中；主动清除：每秒十次 1.测试随机的20个keys进行相关过期检测。2. 删除所有已经过期的keys。
3.如果有多于25%的keys过期，重复步奏1. 所以最终内存的浪费会低于25%，稍微牺牲一下内存，但是保住了Redis的性能。对于Redis来讲：性能为王！

##### 缓存常见问题
- 击穿
- 雪崩
- 穿透
- 一致性（双写）  

哲学的总结：技术是要易于人的使用，但是理论是极其复杂的












