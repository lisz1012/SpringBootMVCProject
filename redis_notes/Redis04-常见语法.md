# Redis语法

### 1.string

- select db 选择数据库（0-20）
- set k v 设置一个数据
- set k1 v nx nx仅仅可以新建的时候进行插入数据
- set k2 v xx xx仅仅可以更新的时候进行更新数据
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
- type k 查看value是什么类型
- object encoding k 查看v的数据类型
- incr k1 将integer的数据类型加一
- incrby k1 v 将integer数据类型加v
- decr k1 将integer的数据类型减一
- decrby k1 v 将integer数据类型减v
- incrbyfloat k1  v 将integer数据类型加一个浮点型
- 数据不够长的时候编码是embstr，之后会变为raw格式
- strlen k1 查看v的长度
- redis-cli --raw 进行进入，会识别编码（比如自动识别GBK）
- getset k1 v 更新新值，返回旧值
- bitpos key bit [start] [end] 查看从start到end的字节，第一次bit出现的位置
- bitcount key [start] [end] 查看start到end的时候，1出现的次数
- bitop and andkey k1 k2 执行k1 k2 按位与操作
- bitop or orkey k1 k2 按位或操作

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

  