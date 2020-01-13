# Redis语法

## 客户端command line登录使用Redis
`redis-cli` 命令启动客户端，前提是已经运行redis了 （/opt/redis5/bin/redis-server）直接回车，如果有多个Redis进程的话，默认会连到6379那个端口上启动的 Redis 上.`redis-cli -h`查看帮助
参数中可以用-n参数指定db，数据库。类似MySQL会建立数据库，数据隔离。Redis准备了默认16个库：0-15.以下要讲的类型都是value的类型


### 1.string (字符串或者byte数组的操作、数值的、位图的)

- select db 选择数据库（0-20）
- set k v 设置一个数据
- set k1 v nx nx仅仅可以新建的时候进行插入数据, 不存在才设置，分布式锁的时候用这个用的多，一堆人拿着一个redis做参考，都想删一个文件，谁去删呢？很多连接对一个单线程的Redis发这条命令，谁用这个命令设置成功了谁去删除
- set k2 v xx xx仅仅可以更新的时候进行更新数据，存在才设置
- mset k1 v1 k2 v2 可以进行设置多个值
- get k 返回一个v，没有返回nil
- mget k1 k2 k3 获取多个v
- getrange k start end 获取一个索引从start到end，双闭合的区间
- setrange k start value 更新区间范围，我们可以从start的索引开始，更新value数据
- del key 删除一条kv数据
- keys pattern 用正则查询key
- flushdb 清空db
- help @string 查询string相关帮助信息
- append k v 给k的数据进行追加v这个数据
- type k 查看value是什么类型. 不用执行类型对应的方法，上来先检查类型，类型不对直接报错，Redis的基本优化。key里面除了value还存了个type，表示 value是什么类型.set k1 99之后type k1得到string，因为SET的分组是string
- object encoding k 查看v的数据类型 查看value的编码，面向string类型，除了字符串操作还有计算的操作，如果发现value是int类型则可以做incr k操作
- incr k1 将integer的数据类型加一。应用场景：抢购、秒杀、详情页、点赞数、评论数、好友数，可以规避并发下对数据库的事务操作，完全由Redis内存操作代替，而且符合“计算向数据移动”的原则。但是涉及到银行的钱，就不能用Redis了，必须持久化
- incrby k1 v 将integer数据类型加v
- decr k1 将integer的数据类型减一
- decrby k1 v 将integer数据类型减v
- incrbyfloat k1  v 将integer数据类型加一个浮点型. 这时执行OBJECT encoding k1则返回"embstr"
- 数据不够长的时候编码是embstr，之后会变为raw格式
- strlen k1 查看v的长度。长度存在某个地方，改了才更新，查的话不用每次都数长度
- redis-cli --raw 进行进入，会识别编码（比如自动识别GBK）
- getset k1 v 更新新值，返回旧值.先get后set等于在通信的时候发了两个包，就有两次IO请求，合成一个的话，在通信上只发了一个包，这就是作者比较细腻的一个地方。
- mset k1 v1 k2 v2 k3 v3 ... key和value相间排列，可以设置多个值，也只是发送一个数据包。
- msetnx 只有所有的key都不存在才创建。原子性操作，有一个失败了就全部失败
- bitpos key bit [start] [end] 查看从start到end的字节，第一次bit出现的位置。start和end都是指第几个字节，而不是bit.对01000000 01000001执行bitpos k1 1 1 1会返回9，是指start和end两字节之间第一次出现的1在所有二进制位中的
位置而不是在当前字节中的第几个。
- bitcount key [start] [end] 查看start到end字节的时候，1出现的次数
- bitop and andkey k1 k2 执行k1 k2 按位与操作
- bitop or orkey k1 k2 按位或操作

Redis在client访问的时候只拿字节流，而不是字符流，因为只取字节而没有按照某个编码转换的话，未来的双方客户端只要有统一的编解码，数据就不会被破坏，所以Redis十二进制安全的。不同语言对于整型的宽度理解是不一样的，有可能发生截断溢出
这样的错误。就想再多语言开发的时候，我们更倾向于使用json、xml这种文本表示数据的方式来交互，而不是序列化。Redis作为核心的中间者，他为了二进制安全，只取字节流. 99999存进Redis之后就是一个字符占一个字节，strlen返回5，而不是4
或者2。客户端的编码也是，一个字符一个字节，直接向字节流去写，虽然他知道OBJECT encoding是int类型，但不会按照4个或2个字节来存。incr和decr的计算的时候是要把字节转换成数值，然后更新encoding, 只要计算成功就更新编码为int,这个
value往后就都是int类型了，下次就直接拿出来往上加，如果发现不是int类型就可以规避报错，更新编码就是个加速。编码并没有影响数据存储。例如：
```
127.0.0.1:6379> set k3 99999
OK
127.0.0.1:6379> strlen k3
(integer) 5
127.0.0.1:6379> OBJECT encoding k3
"int"
127.0.0.1:6379> APPEND k3 99
(integer) 7
127.0.0.1:6379> OBJECT encoding k3
"raw"
127.0.0.1:6379> incr k3
(integer) 10000000
127.0.0.1:6379> OBJECT encoding k3
"int"
```
疑问：`set k2 中` 然后 `strlen k2`为什么输出`(integer) 3`?这是因为在ssh shell的编码是UTF-8，如果换成GBK，则输出2，因为GBK不顾虑其他的字符集，压缩空间.在utf-8和GBK下分别存入“中”字，然后再拿出来：
```
127.0.0.1:6379> mget k2 k3
1) "\xe4\xb8\xad"
2) "xd6\xd0"
```
1）2）分别代表在utf-8和GBK下“中”字的编码表示,\x表示是16进制. `redis-cli --raw`这时再get就可以看到原文了：
```
[root@master ~]# redis-cli --raw
127.0.0.1:6379> get k2
中
```
不带上 --raw的话，Redis只会识别ascii码，超过了就直接按照16进制显示，如果加了这个--raw选项，就会触发编码集的格式化，也就是说Redis发现了这几个十六进制符合当前编码集的编码规则，就从编码集中找到那个字符显示出来。总之Redis就是二进制
安全的，跟hbase一样，向hbase写数据时先做序列化，转化成字节数组，hbase不会破坏编码的。在set的时候如果set k1 5此时直接判断，就转成了int，之后再incr就不用类型判断了，所以这个encoding可以提速，如果incr作用在了embstr上，就直接报错。
二进制安全：各个用户之间沟通好编码和解码，Redis哪不是没有数据类型的。

setbit的help说明如下：  
```127.0.0.1:6379> help setbit

  SETBIT key offset value
  summary: Sets or clears the bit at offset in the string value stored at key
  since: 2.2.0
  group: string
```
这里的offset是二进制位的offset而非字节的offset. `setbit k1 1 1`之后strlen k1返回1，第一位是其他都是0，只开辟一个字节就可以了；如果`setbit k1 9 1`之后再strlen k1则返回2，9位在第二个字节里了get k1会显示“@@”。字符集标准的
叫做ascii，其他的是扩展字符集，也就是说，其他的字符集不再修改ascii字符集。ascii就是0xxxxxxx，第一位必须是0.自己写个程序读字节流，只要第一个位是0，则直接差ascii表显示这个字符；否则比如111开头，则出了这个字节还要读出两个字节，这表示
总共用3个字节表示该字符，拿111后面的各位拼成一个数值，去客户端字符集里面再找（--raw的作用），然后显示出来，这就是“中”为什么占3个字节，strlen k1是3

setbit、bitpos、bitcount、bitop这几个命令很有用：  
应用场景1: 当公司有用户表的时候，当统计用户的登录天数且窗口随机的时候，怎么维护并用这个随机窗口查询。如果用MySQL记录一个用户的登录情况，一条记录代表他登没登录，这样这条记录里至少要有id：4字节；
Date：4字节，也就是一天的登录情况要用8个字节了，但是如果用一个二进制位来表示的话就好多了，这天登录了，就setbit key day 1， 这样8天才用一个字节，而且查起来、计算起来还最快, 例子如下：
```
127.0.0.1:6379> setbit shuzheng 1 1
0
127.0.0.1:6379> setbit shuzheng 7 1
0
127.0.0.1:6379> setbit shuzheng 364 1
0
127.0.0.1:6379> strlen shuzheng
46
127.0.0.1:6379> bitcount shuzheng 0 -1
3
```
统计全年登录次数：shuzheng在第2、8、365天登录，一共三次. 很酷，就是粒度大了点 --- 8天。  
bitmap矩阵：
```shuzheng 01000010...1
   zhangsan 10011111...0
   ...
``` 
每用户46字节，假设10000000个用户，则内存消耗：460000000 约= 400M, 存储量既小于MySQL又快于MySQL


应用场景2: 比如京东就是我的，6.18做活动：凡是登录用户就送给他/她一件礼物。需求是：计算大库备货多少礼物？假设京东有2亿用户。  
两亿用户，是有分类的：僵尸用户和冷热用户。可能只有1亿的活跃用户，但是1亿和2亿的差别很大，一件礼物10块钱，这就差出1亿，一个简单的计算就省了1亿，这个一般的销售可比不了。如何做活跃用户统计？618，918，1018，也是个随机窗口，数据必须能够支撑
未来随意给出一个窗口就能算出活跃用户是多少。活跃用户统计的本质是：登录在同一天不管几次都算出现了，这里有个去重的问题。做法是：每个用户映射到一个二进制位上，表示他/她是否在某一天登录过，一旦登录就
`setbit 20200101 <用户1映射的二进制位> 1`， 同理有`setbit 20200102 <用户1映射的二进制位> 1`，`setbit 20200102 <用户2映射的二进制位> 1`现在统计这两天的活跃用户数：
```
127.0.0.1:6379> setbit 20200101 1 1
0
127.0.0.1:6379> setbit 20200102 1 1
0
127.0.0.1:6379> setbit 20200102 7 1
0
127.0.0.1:6379> bitop or stat 20200101 20200102
1
127.0.0.1:6379> bitcount stat 0 -1
2
```
统计结果就是有多少个不同的用户在给定的时间窗口里登录过（至少一次）----- 考Redis基本会考这道题
然后`bitcount key 0 -1`。这样2亿用户可以用200000000/8=25000000 byte = 2个G，内存装得下，算起来很快
bitmap矩阵：
```20200101 01101010...1 （2亿位）
   20200102 10111111...0
   ...
   （365行）
``` 
可见这个矩阵差不多是上一个矩阵旋转90度，要么就是用户ID为key天为位；要么就是天为key，用户ID映射为位。登没登录就是0/1

### 2.list
链表。Redis中的key身上有两个东西：head 和 tail，头尾指针，快速访问到链表中的第一个和最后一个元素. 同向命令就把list作为栈使用；反向命令就把list作为队列使用;lindex就把list作为数组操作了;blpush/blpop ----- 阻塞的单播队列FIFO

- lpush、lpop、rpush、rpop 和栈一样
- lrange 0 -1 所有元素查看 0是最左边节点的索引
- lindex key index 查看索引位置的值
- lrem key count value 移除count数量的value. count > 0从左面开始移除，count < 0从右面开始移除

- linsert key after afval value 在键后面插入值,afval指的是值, 如果list里面有有两个相同value的话，只在第一个后面追加
- linsert key before befval value 在key前面插入值，befval指的是值, 如果list里面有有两个相同value的话，只在第一个前面追加
- blpop key ... timeout阻塞式取值（等待有值再取出）timeout是秒数，设为0则一直等,直到有数据了才拿出来并退出阻塞
- ltrim key [start] [end] 修剪，进行修剪队列
- 更多详见`help @list`

### 3.hash
value的类型是一个hash，类似java的HashMap，而Redis自身又是个键值对，等于是两层嵌套的map.hash的应用场景有详情页及其被收藏、浏览、加入购物车的次数、微博关注、点赞，这些数据既要查询又要计算，而且还可能取回同一个对象的一批数据
hash是一种简单的Document，所以可以用Mongo，也可以用键值对来存储，Redis只是做不了更复杂的，不能再嵌套结构了

- hset key field value 设置一个key field的值
- hget key field 获得一个key field的值
- hmset key field value field value 设置多个field的值
- hmget key field fied 获取多个field的值
- hkeys key 查看所有的key
- hvals key 查看所有的field
- hincrby key field num 增加num值

### 4.set
对比list，list有序（插入的时间顺序）可重复，set无序不可重复

- sadd key v1 v2 v3... 插入v1，v2，v3...
- smember key 列出所有的value
- srem v1 v2 删除v1，v2...
- sinter k1 k2 求交集并返回
- sinterstore dest k1 k2 交集结果存储dest
- sunion k1 k2 求并集返回
- sunionstore dest k1 k2 并集存储dest
- sdiff k1 k2 求差集并返回
- sdiffstore dest k1 k2 求差集存储dest
- srandmember k1 随机返回一个成员
- srandmember k1 num 随机返回num个元素，num为正数，取出一个去重结果集，如果为负数，那么取出不去重结果集
- smembers key 拿出key下的所有元素，但不要轻易用，这样会消耗网卡的吞吐量，应该单拿出来放到一台机器上  
注：带store的直接在Redis服务器就存储了，不用来回传输数据，节省IO，这是作者细心的地方

### 5.zset

- zadd k score mem score mem 插入数据后增加权重

- zrange k 0 -1 取出所有的值

- zrangebyscore k low high 取出从low到high区间的数据

- zrange k start end 从start到end之间的数据取出

- zscore k v 返回一个数据的分值

- zscore k  v 返回一个数据的排行

- zrange k 0 -1 withscores 携带分数取出

- zincrby k incrscore v 增加一个值的分值

- zunionstore k keynum k1 k2..[aggregate max] 多个key的并集[最大值]

  
内存寻址时间单位是ns级的（而且计算向数据移动），而socket是ms级的，差了10万倍，除非有10万个连接同时到达，可能会造成秒级的响应。  
Redis单线程单进程是不是为了规避用户态和内核态之间来回切换的损耗才这么设计的？不完全是。切换的确有性能的损耗，但是这个损耗站在数据一致性面前算不了啥，Redis主要是为了数据一致性  
MySQL更倾向于使用BIO，需要更多的线程，因为后面的磁盘IO会成为瓶颈，epoll好似让很多连接进来了，但是后面读写磁盘的时候还是会阻塞住，反倒是索性BIO更省事儿，epoll没必要了。MySQL其实也有自己的缓存，看到之前完全一样的select查询过来之后，
把缓存中的值返回，SQL的语法树就不做了，省了一大块东西。但是MySQL数据毕竟大过内存的，很大的表开启缓存反而会降低性能，因为会多一次判断缓存的过程。MySQL还是不能以开启缓存的方式模拟Redis。  
每颗核心启动一个进程，还有个“亲密度”的概念，起一个Redis可以亲密到4可核心的其中一颗，这可核心就专心处理Redis请求了，更多的Redis时间片由这个核心处理，这样1、2、3级缓存也不会倒来倒去，这时Redis才能达到最快的秒级10万的速度。后面讲到了
Docker的时候肯定也会将类似的内容  
数据库模型：Document、Graph、Time series、Relational、Key-Value、Wide-Column 详见：https://db-engines.com/en/articles