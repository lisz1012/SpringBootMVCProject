# Redis语法

## 客户端command line登录使用Redis
`redis-cli` 命令启动客户端，前提是已经运行redis了 （/opt/redis5/bin/redis-server）直接回车，如果有多个Redis进程的话，默认会连到6379那个端口上启动的 Redis 上.`redis-cli -h`查看帮助
参数中可以用-n参数指定db，数据库。类似MySQL会建立数据库，数据隔离。Redis准备了默认16个库：0-15


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
- incr k1 将integer的数据类型加一
- incrby k1 v 将integer数据类型加v
- decr k1 将integer的数据类型减一
- decrby k1 v 将integer数据类型减v
- incrbyfloat k1  v 将integer数据类型加一个浮点型. 这时执行OBJECT encoding k1则返回"embstr"
- 数据不够长的时候编码是embstr，之后会变为raw格式
- strlen k1 查看v的长度
- redis-cli --raw 进行进入，会识别编码（比如自动识别GBK）
- getset k1 v 更新新值，返回旧值
- bitpos key bit [start] [end] 查看从start到end的字节，第一次bit出现的位置
- bitcount key [start] [end] 查看start到end的时候，1出现的次数
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
疑问：`set k2 中` 然后 `strlen k2`为什么输出`(integer) 3`?这是因为在ssh shell的编码是UTF-8，如果换成GBK，则输出2，因为GBK不顾虑其他的字符集，压缩空间


### 2.list

- lpush、lpop、rpush、rpop 和栈一样
- lrange 0 -1 所有元素查看
- lindex key index 查看索引位置的值
- lrem key count value 移除count数量的value

- linsert key after afval value 在键后面插入值
- linsert key before befval value 在key前面插入值
- blpop 阻塞式取值（等待有值再取出）
- ltrim key [start] [end] 修剪，进行修剪队列

### 3.hash

- hset key filed value 设置一个key field的值
- hget key field 获得一个key field的值
- hmset key field value field value 设置多个field的值
- hmget key field fied 获取多个field的值
- hkeys key 查看所有的key
- hvals key 查看所有的field
- hincrby key field num 增加num值

### 4.set

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

  