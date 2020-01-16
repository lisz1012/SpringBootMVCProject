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
的数据实时聊天的东西可以用pubsub来实现；三天之内的数据可以用Redis的zset数据类型，可以使用zset命令`ZREMRANGEBYSCORE key min max`留下大的天数，剔除小的天数所以max应该是三天前，正向的就可以了，Redis由小到大排列的，把时间long作为
记录的score，把消息作为元素就可以了放进zset的时候就已经排好序了 


##### 三、事务（transactions）

```
multi 开启事务
...
exec 执行事务
```

```
watch 如果数据被更改，那就不执行事务
unwatch 取消监视
```

```
discard 放弃事务
```

##### 四、布隆过滤器（redisbloom）

在redis.io/modules选择redisbloom的github，克隆下来

解压，make编译，将redisbloom.so这个链接库复制到/opt/redis

执行

```
redis-server --loadmodule /opt/redis/redisbloom.so 
```



```
科普：bloom filter,counting bloom,cukcoo是什么？
1.bloom filter
它实际上是一个很长的二进制向量和一系列随机映射函数。布隆过滤器可以用于检索一个元素是否在一个集合中。它的优点是空间效率和查询时间都远远超过一般的算法，缺点是有一定的误识别率和删除困难。
运用:网页黑名单、垃圾邮件、爬虫网址判重
Java想要使用BloomFilter可以考虑使用google的guava

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













