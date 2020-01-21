# Redis集群

## 介绍

### RDB

存储层的技术（Redis，MySQL、Oracle等）都有两个东西：
1.快照（副本，每天的cron拷出数据来存好，今天的数据有致命错误，可以拿昨天的数据做一个回滚。对Redis说就是隔一定时间就把内存dump到硬盘或别的主机，对于Redis来说是RDB，Redis镜像）Redis的RDB有时点性：比如8点的时候生成一个文件，但不可能
  8点的时候瞬间就写到磁盘，会有几分钟的延迟，这时磁盘中的数据是8点时的状态。两种实现的方法：1. 先阻塞，Redis整个进程不对外提供服务，写完文件再继续。但这样肯定不行，服务不可用了 2. 非阻塞，同时数据落地。但这样会造成已经落地的数据再没
  全部copy完之前又在内存中被修改，这样数据就不严格的是某个时刻的快照了。怎么办呢？这里要补一个Linux OS知识：Linux管道`ls -al | grep more`, 管道就是衔接前边命令的输出作为后边命令的输入, 管道会触发创建子进程：当前bash是一个进程，
  “|”的左边和右边各是一个进程，一共就有了3个进程:
  ```
  [root@master ~]# echo $$
  5872
  [root@master ~]# echo $$ | more
  5872
  [root@master ~]# echo $BASHPID
  5872
  [root@master ~]# echo $BASHPID | more
  5962
  [root@master ~]# echo $BASHPID | more
  5967
  [root@master ~]# echo $BASHPID | more
  5969
  [root@master ~]# echo $BASHPID | more
  5972
  ```
  这里$$取当前进程ID，优先级高于管道，所以`echo $$`的时候先把自己换成5872，然后再开辟两个进程，传递给more；而$BASHPID的优先级是低于管道的，先开辟了两个子进程，然后左边的echo $BASHPID替换的就是子进程的ID号，所以输出会不一样。
  讲这个是为了抛出Linux父子进程的概念。这就带来一个问题：父进程的数据子进程可不可以看得到？不能：
  ```
  [root@master ~]# echo $$
  5872
  [root@master ~]# num=1
  [root@master ~]# /bin/bash (这里开启了一个新的bash进程)
  [root@master ~]# echo $$
  6153
  [root@master ~]# echo $num

  [root@master ~]#
  ```
  常规思想进程数据隔离,打破数据隔离可以用export命令：
  ```
  [root@master ~]# echo $num
  1
  [root@master ~]# export num
  [root@master ~]# /bin/bash
  [root@master ~]# echo $num
  1
  ```
  进阶思想：父进程可以让子进程看到数据,但是自己成的修改不会影响到父进程：
  写个bash：
  ```
  #/bin/bash
  echo $$
  echo $num
  num=999
  echo num:$num

  sleep 20

  echo num:$num
  ```
  然后执行，子进程修改num的值。在子进程sleep期间，在父进程中查看num的值，发现子进程的修改不会影响父进程num的值：
  ```
  [root@master ~]# vim test
  [root@master ~]# chmod 0755 test
  [root@master ~]# ./test &
  [1] 6682
  [root@master ~]# 6682
  1
  num:999

  [root@master ~]# echo $num
  1
  [root@master ~]# num:999

  [1]+  Done                    ./test
  [root@master ~]#
  ```
  同样，父进程也改不了子进程中num的值：
  ```
  [root@master ~]# ./test &
  [1] 6753
  [root@master ~]# 6753
  1
  num:999

  [root@master ~]# echo $num
  1
  [root@master ~]# num=88
  [root@master ~]# num:999

  [1]+  Done                    ./test
  ```
  创建子进程的速度是什么程度？如果父进程是Redis那么他的内存数据，比如10G，可以在8:00的时候创建一个子进程，这个时候数据拷贝过去一个副本，不受主进程数据变化的影响，有父进程接受外界请求，而子进程把副本存成文件就可以了。所以问题就成了
  1. 数据在内存拷贝的速度多快？ 2. 内存够不够？速度来讲肯定还是不够快，这里Linux为了克服这个问题，内核有了fork()函数，fork达到的效果其实就是计算机玩了个指针的引用，创建速度特别快而且空间要求不是特别多，子进程占用很小的空间。怎么
  实现的呢？补一个知识：物理内存是一个线性的地址空间，Redis程序默认整个内存都是自己用的，他有一个虚拟地址空间，指不定跟物理地址空间怎么映射的，这个由MMU管理。子进程创建出来之后，对应的内存和变量映射到跟父进程相同的物理内存地址，在
  子进程里没有把数据真正拷贝过来，只是拷贝了指针。这样子进程就也能看到父进程的变量的值了。那为什么一个进程修改变量但是另一个进程却看不到呢？这里有个知识点叫copy on write：写时复制，创建子进程并不发生复制。这样带来的好处是：创建
  进程变快了，同时根据经验不可能父/子进程把所有数据都改一遍，这时候先出现一个新的物理内存地址，有一个新的值，再把地址传给那个有修改的进程中的变量。其实玩儿的是指针.  
  
  Redis的做法是：当程序有了数据之后，做快照的时候，创建一个子进程，且子进程的创建形式是通过fork()的方式，速度很快，且数据没有真的copy，父进程的数据会发生修改，但会触发一个写时复制，父进程会更新成一个新的值，并把他的地址返回到父进程
  内存，子进程还是指向老的地址，继续拷贝。所以即便子进程用了10个小时才复制完，也不会因为父进程的数据变化而变化。这时的数据才是真正的时点数据。指针拷贝很快，只有4个字节，但他指向的内容可能500字节。这里父进程有异常退出的话，子进程也退出。
  怎么去触发RDB方式时点数据落地？1.Redis命令的方式：`save`阻塞 和 `bgsave`这个会调用fork()创建子进程. 2. 配置文件中给出bgsave的规则，文件里面用的是save这个标识(这里语义和命名有不严谨的地方)。什么时候用save呢？关机维护的时候。
  这是内存数据不能丢，拷完数据，然后关机，少见
  ```
  save 900 1
  save 300 10
  save 60 10000 #到了60秒，且写请求达到10000个
  ```
  `save ""`是关闭RDB类似的配置还有`rdbcompression yes`开启压缩，`rdbchecksum yes`RDB文件最后写一个校验位，`dbfilename dump.rdb`数据落地后的文件名，`dir /var/lib/redis/6379`数据落地后的文件存放目录
  
  
2.日志：增删改的时候记录如日志文件，一旦数据没有了，可以通过日志中已经执行过的增删改命令恢复。在Redis里是AOF   


##### 1.单机、单实例的持久化方式

  在我们之前的课程中，我搭建了一个单机，单进程，缓存redis。我们使用rdb,aof持久化，用来确保数据的安全。


rdb（relation-ship database）持久化：
默认redis会以一个rdb快照的形式，将一段时间内的数据持久化到硬盘，保存成一个dumpr.rdb二进制文件。
工作原理：当redis需要持久化时，redis会fork一个子进程，子进程将数据写到磁盘上临时一个RDB文件中。当子进程完成写临时文件后，将原来的RDB替换掉，这样的好处就是可以copy-on-write。

配置redis.conf:
save 900 1
save 300 10
save 60 10000

时间满900秒，如果有一个key被修改，则会发起快照保存。300秒之内，如果超过10个key被修改，则会发起快照保存。在60秒内，如果1w个key被修改，则会发起快照保存。Redis默认开启落地。子进程落地完了就结束。每个时点落地是全量的。    

以上的RDB有个缺点：丢失数据相对多一些，不是实时在记录数据的变化，窗口数据容易丢失。比如：8点得到一个DB，假设9点刚要落地一个RDB，挂机了，这样就丢了一个小时的数据。全量数据备份都有这样的缺点。不支持拉链(就是1号数据库长什么样、
2好数据库长什么样...生成多个文件)，永远只有一个dump.rdb，省存盘空间，但是需要人为地写其他脚本去做copy或者重命名到其他的地方。  

优点：类似Java中的序列化，把字节数组搬到磁盘中去，恢复的时候也直接恢复，恢复的速度相对快

只有一个子线程写完了，后面的才能写。固态硬盘PC-E或者服务器硬盘快速接口的话几秒钟就可以写完了，如果只有3-4G maxmemory，写很长时间的话RDB就没有意义了

### aof（append-only file）持久化

Redis的写操作记录到文件中。这样的第一个好处是丢失数据相对少，第二个是Redis中RDB和AOF同时开启，如果开启了AOF，只会用AOF做恢复（落数据的时候两边都会做）。4.0之后AOF包含RDB全量，增加记录新的写操作。这样恢复的时候，先把RDB数据load到
内存，然后将它的时间点之后新增的数据再重新执行一遍，这样恢复速度快一些。AOF是bin log，重新执行命令所以慢，所以要克服这一点，得结合RDB时点数据。  

假设Redis运行了10年且开启了AOF，请问10年头的时候Redis挂了，1. AOF多大？2. 恢复要多久？
1. 回答：10T也是有可能的，恢复的时候不会溢出，有限的内存可以做无限的操作，操作虽然多，但是挂机的时候maxmemory总是一定的那些内存，溢出的东西不可能写到文件里去。
2. 回答：恢复时间要看写操作的数量，有可能Redis日志就没有多大，比如几k，这样就很快。如果是 10T的日志，恢复也得5年左右  

弊端：体量无限变大、恢复慢。日志如果能保住，还是可以用的  
解决：设计一个方案让日志再不丢失数据的前提下足够小，所以在hdfs中用fsimage + edits.log，比如现在是9:30，fsimage是9:00的，edits.log就是9:00 - 9:30的增量的日志。Redis在4.0以前：重写。把能抵消掉的操作删除，合并重复的命令（对于
     同一个key），最终也是一个纯指令的日志文件；4.0以后：重写的时候，先将老的数据RDB到AOF文件中，再将增量的以指令的方式append到AOF，所以AOF是一个混合体，利用了RDB的块，也利用了日志的全量两个优点  
     
原点：Redis是内存数据库，再增删改发生的时候，写操作会触发IO，会拖慢Redis内存处理数据快的特征。这是有三个写IO的级别可以调：
1. NO `appendfsync no` 所有对于硬件和磁盘的IO操作都是要调用内核的，java这个进程想调用文件描述符fd8，内核会在内核中会给fd8开辟一个buffer，java要先写内容到这个buffer，等buffer满了之后，内核会从这个缓冲区中向磁盘中刷写，但是内核
   向磁盘刷写这个事儿我们一般不知道。NO的意思是：Redis取代jvm，来了若干笔写操作，内核的文件描述符的buffer什么时候满了等他自己刷写到磁盘。有一点隐患就是可能会丢失一个buffer大小的数据，一个buffer的数据还没刷进磁盘去，就down了
2. AWAYS `appendfsync always` 机理类似上面的，但是buffer 里一旦有写操作的内容就立刻flush刷进磁盘，数据最可靠的选择，顶多刷数据的时候失败，比如服务器断电，顶多丢一条写数据的操作记录。但速度最慢。
3. 每秒 `appendfsync everysec` 每秒（或者buffer满了）调一次。最多丢失差一点点到一个buffer的数据，但是这种概率太小，很有可能一秒钟之内buffer满了3-4次，所以可能丢的操作记录很少，折中了前两者

配置文件中开启AOF：`appendonly yes`打开。配置文件里还有`appendfilename "appendonly.aof"` -- 文件名，这个文件会被放在上边的RDB设置的那个dir下. `no-appendfsync-on-rewrite no`抛出子进程进行bgsave/rewrite的时候，不会调
buffer刷新这个事，无论什么级别。因为他会认为自己的子进程会对着磁盘发生一个疯狂的写操作，这时候就不来争抢这个IO了，当然这个时候就容易丢数据，所以要不要开启是要看数据的敏感性的。丢失的这部分数据其实可以再下一次的RDB时点数据更改中被捕获，
`aof-load-truncated yes`检查...（稍后讲） `aof-use-rdb-preamble yes`开启后即上面所说的：4.0之后的Redis可以利用RDB和AOF日志各自的优势。这种设置下，AOF文件以"REDIS"开头

实操:  
1. 修改/etc/redis/6379.conf: `daemonize yes` --> `daemonize no`,不要让她在后台运行
2. 注掉`#logfile /var/log/redis_6379.log`不要把各种信息记到log里面，而是打印在屏幕上。
3. `aof-use-rdb-preamble no`4.0之后的是否打开混合体模式，可以设置为关闭或者打开，根据测试的内容而定。
4. 删除 `/var/lib/redis/6379/dump.rdb`,让Redis启动的时候是个空的，没得加载。
5. 用命令启动（不是以service的方式）：`redis-server /etc/redis/6379.conf` 此时在`/var/lib/redis/6379/`下面出现了文件：
   `-rw-r--r--. 1 root root 0 Jan 18 22:35 appendonly.aof`, 而且是空的，因为什么都没有发生。
6. 另起一个客户端登录6379这个Redis：`redis-cli --raw`
7. 执行`set k1 hello`
8. 查看`/var/lib/redis/6379/appendonly.aof`,发现如下内容：
   ```
   *2             //*后面的数字代表的是后面有几个元素组成，2代表了 select和0这两个元素，表示选择了0号库，2 * 2 = 4 所以读取下面4行
   $6			  //$表示后面的命令或参数有几个字节组成	
   SELECT		  
   $1
   0
   *3
   $3
   set
   $2
   k1
   $5
   hello
   ```
   这是老式的AOF文件，最前面没有REDIS标识，因为虽然用的是5.0，但是并没有开启`aof-use-rdb-preamble yes`
9. 执行`bgsave`做dump. Redis前台会出现：
```
11664:M 18 Jan 2020 23:04:36.298 * Background saving started by pid 12105
12105:C 18 Jan 2020 23:04:36.303 * DB saved on disk
12105:C 18 Jan 2020 23:04:36.304 * RDB: 0 MB of memory used by copy-on-write
11664:M 18 Jan 2020 23:04:36.374 * Background saving terminated with success
```
而`/var/lib/redis/6379`下会多一个dump.rdb文件, 强行打开之后发现它是以"REDIS"开头的。执行`redis-check-rdb ./dump.rdb`命令可以看这个文件。

10. 下面反复设置k1验证重写功能
```
127.0.0.1:6379> set k1 a
OK
127.0.0.1:6379> set k1 b
OK
127.0.0.1:6379> set k1 c
OK
```
再去看`/var/lib/redis/6379/appendonly.aof`,发现：
```
*3
$3
set
$2
k1
$1
a
*3
$3
set
$2
k1
$1
b
*3
$3
set
$2
k1
$1
c
```
其实是有一些不必要的记录，因为k1最终的状态只是c，不必要保留中间状态的记录。此时可以在Redis交互界面命令行下执行`BGREWRITEAOF`,则这三条操作的记录便精简成了：
```
*3
$3
SET
$2
k1
$1
c
```
同时Redis前台打印：
```
11664:M 18 Jan 2020 23:40:12.145 * Background append only file rewriting started by pid 12389
11664:M 18 Jan 2020 23:40:12.316 * AOF rewrite child asks to stop sending diffs.
12389:C 18 Jan 2020 23:40:12.316 * Parent agreed to stop sending diffs. Finalizing AOF...
12389:C 18 Jan 2020 23:40:12.316 * Concatenating 0.00 MB of AOF diff received from parent.
12389:C 18 Jan 2020 23:40:12.316 * SYNC append only file rewrite performed
12389:C 18 Jan 2020 23:40:12.316 * AOF rewrite: 0 MB of memory used by copy-on-write
11664:M 18 Jan 2020 23:40:12.344 * Background AOF rewrite terminated with success
11664:M 18 Jan 2020 23:40:12.344 * Residual parent diff successfully flushed to the rewritten AOF (0.00 MB)
11664:M 18 Jan 2020 23:40:12.344 * Background AOF rewrite finished successfully
```
11. Control + C结束Redis server的进程，并删除`/var/lib/redis/6379`下的所有文件
12. `vim /etc/redis/6379.conf`改配置为：`aof-use-rdb-preamble yes` 开启混合体
13. 执行`redis-server /etc/redis/6379.conf`再次启动redis-server
14. 查看`/var/lib/redis/6379/appendonly.aof`,发现他是空的
15. 再开启redis交互界面：`redis-cli --raw`
16. 执行：
	```
	127.0.0.1:6379> set k1 a
	OK
	127.0.0.1:6379> set k1 b
	OK
	```
17. 查看`/var/lib/redis/6379/appendonly.aof`得到：
	```
	*2
	$6
	SELECT
	$1
	0
	*3
	$3
	set
	$2
	k1
	$1
	a
	*3
	$3
	set
	$2
	k1
	$1
	b
	```
不是混合体，因为还没有到时间触发重写，下面手动触发  

18. Redis交互界面执行`127.0.0.1:6379> BGREWRITEAOF`，再回去查看`/var/lib/redis/6379/appendonly.aof`，发现aof文件变脸了，得到了当前对RDB的一个存储，以"REDIS"开头，不计算命令是否抵消了,直接存成rdb快照。
19. 继续
```
	127.0.0.1:6379> set k1 w
	OK
	127.0.0.1:6379> set k1 m
	OK 
```
20. 查看`/var/lib/redis/6379/appendonly.aof`得到：
```
redis-bitsÀ ú^EctimeÂ^R^P$^ú^Hused-memÂèJ
^@ú^Laof-preambleÀ^Aþ^@û^A^@^@^Bk1^Amÿþ^N`÷ÇHNL*2^M
$6^M
SELECT^M
$1^M
0^M
*3^M
$3^M
set^M
$2^M
k1^M
$1^M
w^M
*3^M
$3^M
set^M
$2^M
k1^M
$1^M
m^M
```
后面的两条记录已明文的方式追加到下面：全量时点数据 + 增量日志，这样恢复速度快

21. 执行`BGSAVE` 更新或创建 dump.rdb，执行 `BGREWRITEAOF` 更新 appendonly.aof。然后发现两文件记录的是一样的东西，都是时点数据的dump。  

注：这里如果出现了`flushall`误操作，想能够恢复数据的话，千万不能执行`BGREWRITEAOF`否则会把误操作之后的结果数据保存，并清除增量操作的历史，而误操作就在增量操作里面, 不执行`BGREWRITEAOF`的话把flushall相关的记录删掉，将来重启的时候
还能恢复:
```
127.0.0.1:6379> keys *
k1
127.0.0.1:6379> FLUSHALL
OK
127.0.0.1:6379> keys *     //这之前删除了appendonly.aof 中 flushall相关的记录
k1
```
结论：无论是混合模式还是独立模式，只要执行了BGREWRITEAOF，误操作的影响就永远造成了，数据救不回来了.

自动触发rewrite的配置，以下两个条件全满足才触发rewrite：
```
auto-aof-rewrite-percentage 100          //超出上一次rewrite之后的AOF文件的百分比达到多少才触发rewrite，0表示永远不触发
auto-aof-rewrite-min-size 64mb           //AOF文件最小是多大才能被重写，重写之后可能就变成32M了，历史数据就被抵消掉了（4.0以前的旧版本），或者做成了rdb
```
  
Redis做缓存的话rdb就够了，做数据库就要开启AOF，混合体：RDB+增量日志


默认会以一个appendonly.aof追加进硬盘。

redis.conf默认配置:
appendfsync yes
appendfdync always
#每次有数据修改都会写入AOF
appendfsync everysec
#每秒同步一次，策略为AOF的缺省策略


##### 2.单节点、单实例面临的问题：

- 单点故障
- 容量有限
- 压力

面对这么多问题，我们解决的方式是，将单节点变为多节点进行架构：

1.进行读写分离。

2.基于三维进行扩展AKF。（微服务划分原则中的第一项，它不仅限定于微服务）

- X轴进行**全量镜像**的节点复制（从单库到多库）一主多备，然后发展到读写分离，往主这一台机器上读写，从多台（其他备机）机器上只读。因为除了主备之外还有主主，多主多活，这样成本更高，因为数据一致性的问题更严重。只可以解决读取方向的压力

- Y轴进行**业务功能**分离（根据业务逻辑再次进行分库）有的Redis从公司订单的、有的Redis是存用户信息的...
- Z轴进行**优先级逻辑**再拆分（单业务进行分片，如MR的DataNode，单业务进行分节点运算，有点像FastDFS的group，ID=1-1000的在一个Redis主备集群，ID=1001-2000的在另一个Redis主备集群）  

一台单机的承载的数据量足够小的时候，就更能发挥单机的性能，也再也没有容量的限制，而访问这一小片数据的压力应该也不是很大。但是，AKF也会引来一些新的问题，见下面

##### 3.进行集群化面临的几个问题：

![1568362595235](C:\Users\74302\AppData\Roaming\Typora\typora-user-images\1568362595235.png)

1.数据一致性

一变多，主备之间，分量数据不能保证一致，如果保证一致，所有节点阻塞，直到数据全部一致，那么就会丢失可用性。如果我们保证数据的强一致性，那么，我们将会破坏可用性（数据在同步，不能保持可用，一台机器因为某种原因失败，则整个写操作都失败。
而一变多本来就是增强可用性的）。要么容忍数据丢失一部分，把同步阻塞写，改成异步，主节点写完了就给client返回（Redis就是这种方案，要得就是快！Redis武功，唯快不破！）。解决丢数据，可以在主备之间加一个Kafka，且他不可能是单机的，不丢
数据而且响应速度足够快，而且数据可以持久化。这样保证了最终一致性。最终一致性有可能client来了之后取到不一致的数据（Redis和Zookeeper都是最终一致性的）强调：强一致性

2.数据完整性

我们保证弱一致，可能会取到错误数据。

3.数据最终一致性

我们如图3的价架构，在中间放一个类似kafka之类的中间件，然后我们能保证最终一致性。（kafka通过一定技术，变得可靠）

##### 4.分布式常见的几个架构：

主备架构：主机死了，备机可以顶。client只能直接访问主机

主从复制：主机后面有几个从节点。（redis多数在企业中用的是主从复制，client可以访问任何机器）  

主身上一般CRUD各种操作都可以，要么备机不发生操作，要么备机只发生读的操作。而主机又是一个单点，所以要对主做HA，高可用的目的是最终把一个备（从）机变成一个主机，切换。或者专门找台机器在旁边盯着，主要是挂了就顶上。主挂了之后，人工可以指定
下一任的主，比如，一旦主挂了，就通知工程师，赶紧来另立新主。但是人是最不靠谱的，所以更倾向于用程序和技术来自动的故障转移，代替人。但是监控程序也有单点问题，所以他自身也应该是一个一变多的集群。解决这个事，多台监控机器盯着同一个主，主挂了
的话，有好多种可能：其中一个、两个、多个、全部都知道主挂了。所有的监控机器都说挂了才算挂，这就是强一致性，但是有可能有一个机器延迟了，此时不能给出主挂了这个判断。所以不能实现强一致性，强一致性要是实现的话可用性就没了，整体决策被卡住了。
强一致性在分布式条件下是很难实现的一件事情。现在退让一步，一部分机器给出判断主挂了，另一部分不算数就算主挂了，关键是一部分到底是几个？比如有3个监控机器监控一个主，如果有一个人说主挂了就把主踢掉换成另外一个，会统计不准确或着说势力范围不够，
造成网络分区（脑裂，一个请求却给出两个不同的response）因为这个说挂了的机器自己和主的连接有问题了。所以必须一半以上的机器认为主挂了才算是挂了，过半是为了防止脑裂。注：不是说出现脑裂就一定不好，有个东西叫“分区容忍性”，比如服务发现的时候，
eureka中有的机器可能看到一个服务里面有50台机器可用，有的看到30台可用... 但不要紧client只需要一台能通信就可以了。换句话说，要么负载50台，要么负载30台，都没什么差异，反正能跑通。  

现在考虑两个机器说主挂了就算挂了。两台说主挂了，势力范围是2，另外一台势力范围是1，这个单出来的再说什么都没用了，而且他自己知道自己是少数，说的没用了，所以由另外两台机器的决策说了算，没有模棱两可的状态。

如果有4个机器，判断不一样：2 + 2的情况，这样又可能各自给出不同的决策，都说了算，那又会脑裂，所以2（一半）也不行，应该是`n/2+1`才能成功解决脑裂问题,n为监控主的机器的总数。  

一半集群使用n=奇数台，因为：打个比方，n=3过半的是2台，n=4过半的是3台，也就是说都只能容忍挂掉1台，但是4台比3台更容易挂掉一台；同样，5台和6台都允许挂两台，但是6台中，更容易挂掉两台，而且购置偶数台成本还高。




**主从复制架构，又面临一个问题，单点故障怎么解决？**

我们对主进行HA架构，用一个程序进行监控，该监控程序，为了避免单点故障，那么也必须是一个集群架构。

我们的监控设备一定是奇数台，进行过半选举，如果过半都选举故障，那么，将会跳到另一台节点。

做实验：建立`~/test`目录， redis5.0.7/util目录下启动三个实例，端口分别是6379、6380、6381. 然后把配置文件全部copy到test目录下：`cp /etc/redis/* ./`
在test目录下的conf文件里面修改：
```
daemonize no
#logfile /var/log/redis_6380.log
appendonly no  #如果开启改成no
```
就是让Redis实例前台阻塞运行且没有aof
删除三个实例的Data目录：
```
rm -rf /var/lib/redis/6379/*
rm -rf /var/lib/redis/6380/*
rm -rf /var/lib/redis/6381/*
```
等于之前所有持久化的数据都没有了.然后启动各个Redis实例：
```
redis-server ~/test/6379.conf
redis-server ~/test/6380.conf
redis-server ~/test/6381.conf
```



### 配置

##### 1.解压

准备一个redis的tar包，进行解压。

##### 2.启动节点并跟随

启动3个实例，从节点使用replicaof ip port这个命令进行跟随主节点。

（注意，在redis 5之前，我们可以通过slaveof进行跟随主节点，但是，从redis5之后，改为了replicaof进行跟随）
6380/6381上面执行
```
REPLICAOF localhost 6379 
```
在6379那里会打印：
```
10006:M 20 Jan 2020 21:32:47.257 * Replica 127.0.0.1:6380 asks for synchronization
10006:M 20 Jan 2020 21:32:47.257 * Partial resynchronization not accepted: Replication ID mismatch (Replica asked for '366ea54afd924a232cfa745d1dbea460ac5b0c58', my replication IDs are '4900448cbbf36e29e5386a528a76d96c099dc9bd' and '0000000000000000000000000000000000000000')
10006:M 20 Jan 2020 21:32:47.259 * Starting BGSAVE for SYNC with target: disk
10006:M 20 Jan 2020 21:32:47.261 * Background saving started by pid 10182
10182:C 20 Jan 2020 21:32:47.287 * DB saved on disk
10182:C 20 Jan 2020 21:32:47.288 * RDB: 0 MB of memory used by copy-on-write
10006:M 20 Jan 2020 21:32:47.358 * Background saving terminated with success
10006:M 20 Jan 2020 21:32:47.358 * Synchronization with replica 127.0.0.1:6380 succeeded
```
先数据落地。在6380/6381那里会打印：
```
10012:S 20 Jan 2020 21:32:46.620 * REPLICAOF 127.0.0.1:6379 enabled (user request from 'id=4 addr=127.0.0.1:51080 fd=7 name= age=461 idle=0 flags=N db=0 sub=0 psub=0 multi=-1 qbuf=44 qbuf-free=32724 obl=0 oll=0 omem=0 events=r cmd=replicaof')
10012:S 20 Jan 2020 21:32:47.229 * Connecting to MASTER 127.0.0.1:6379
10012:S 20 Jan 2020 21:32:47.230 * MASTER <-> REPLICA sync started
10012:S 20 Jan 2020 21:32:47.230 * Non blocking connect for SYNC fired the event.
10012:S 20 Jan 2020 21:32:47.231 * Master replied to PING, replication can continue...
10012:S 20 Jan 2020 21:32:47.257 * Trying a partial resynchronization (request 366ea54afd924a232cfa745d1dbea460ac5b0c58:1).
10012:S 20 Jan 2020 21:32:47.262 * Full resync from master: 6b0978a20110746724c64e271b27959449742d95:0
10012:S 20 Jan 2020 21:32:47.262 * Discarding previously cached master state.
10012:S 20 Jan 2020 21:32:47.358 * MASTER <-> REPLICA sync: receiving 175 bytes from master
10012:S 20 Jan 2020 21:32:47.358 * MASTER <-> REPLICA sync: Flushing old data
10012:S 20 Jan 2020 21:32:47.359 * MASTER <-> REPLICA sync: Loading DB in memory
10012:S 20 Jan 2020 21:32:47.359 * MASTER <-> REPLICA sync: Finished with success
```
Flushing old data这里是为了先把自己的老数据删除，再跟主Redis同步,把主上面的数据copy过来  
6379下面：
```
set k1 aaa
```
6380/6381下面：
```
127.0.0.1:6380> get k1
aaa
127.0.0.1:6380> keys *
k1
```
但是默认Redis从节点禁止写入：
```
127.0.0.1:6380> set k2 aaa
READONLY You can't write against a read only replica.
```
但是配置文件可以调整这个。然后查看`/var/lib/redis/6379`等目录，发现dump.rdb文件出现了。  

现在强制断掉6381:Control + C，则主Redis实例前端收到一条提示：
```
10006:M 20 Jan 2020 21:52:12.729 # Connection with replica 127.0.0.1:6381 lost.
```
再次启动6381:
```
redis-server ~/test/6381.conf --replicaof 127.0.0.1 6379
```
启动和追随一步到位，然后6381打印：
```
10425:S 20 Jan 2020 21:59:24.918 * Ready to accept connections
10425:S 20 Jan 2020 21:59:24.918 * Connecting to MASTER 127.0.0.1:6379
10425:S 20 Jan 2020 21:59:24.918 * MASTER <-> REPLICA sync started
10425:S 20 Jan 2020 21:59:24.919 * Non blocking connect for SYNC fired the event.
10425:S 20 Jan 2020 21:59:24.919 * Master replied to PING, replication can continue...
10425:S 20 Jan 2020 21:59:24.919 * Trying a partial resynchronization (request 6b0978a20110746724c64e271b27959449742d95:2053).
10425:S 20 Jan 2020 21:59:24.919 * Successful partial resynchronization with master.
10425:S 20 Jan 2020 21:59:24.919 * MASTER <-> REPLICA sync: Master accepted a Partial Resynchronization.
```
发现只是同步了掉钱期间的增量数据：`Trying a partial resynchronization (request 6b0978a20110746724c64e271b27959449742d95:2053).`

6381再次退出然后以以下命令重启：
```
redis-server ~/test/6381.conf --replicaof 127.0.0.1 6379 --appendonly yes
```
而在主Redis：6379这里发现：
```
10006:M 20 Jan 2020 22:05:27.278 * Replica 127.0.0.1:6381 asks for synchronization
10006:M 20 Jan 2020 22:05:27.278 * Full resync requested by replica 127.0.0.1:6381
10006:M 20 Jan 2020 22:05:27.278 * Starting BGSAVE for SYNC with target: disk
10006:M 20 Jan 2020 22:05:27.279 * Background saving started by pid 10494
10494:C 20 Jan 2020 22:05:27.287 * DB saved on disk
10494:C 20 Jan 2020 22:05:27.287 * RDB: 0 MB of memory used by copy-on-write
10006:M 20 Jan 2020 22:05:27.367 * Background saving terminated with success
10006:M 20 Jan 2020 22:05:27.367 * Synchronization with replica 127.0.0.1:6381 succeeded
```
这次执行数据落地了，而且每次以这种方式启动，都会走这个过程，不管主机那边是否有数据更新。带上--appendonly yes之后，先load rdb然后再AOF重写，做了一个全量同步。  

##### 下面测试主挂了的情况  

讲主挂之前要知道一件事：凡是一个从，手工方式连到主，主总是知道都是谁连上他了。现在让主下线，然后两个从就都发现了. 这时候可以把其中的一个从的追随对象改成`on one`，手工让它变成主：
```
replicaof no one
```
然后发现其前端打印：
```
MASTER MODE enabled
```
自己变成了主。在6381上面执行
```
REPLICAOF 127.0.0.1 6380
```
则打印：
```
10538:S 20 Jan 2020 22:35:33.386 * REPLICAOF 127.0.0.1:6380 enabled (user request from 'id=4 addr=127.0.0.1:53740 fd=7 name= age=277 idle=0 flags=N db=0 sub=0 psub=0 multi=-1 qbuf=44 qbuf-free=
	  32724 obl=0 oll=0 omem=0 events=r cmd=replicaof')
10538:S 20 Jan 2020 22:35:33.893 * Connecting to MASTER 127.0.0.1:6380
10538:S 20 Jan 2020 22:35:33.893 * MASTER <-> REPLICA sync started
10538:S 20 Jan 2020 22:35:33.893 * Non blocking connect for SYNC fired the event.
10538:S 20 Jan 2020 22:35:33.893 * Master replied to PING, replication can continue...
10538:S 20 Jan 2020 22:35:33.893 * Trying a partial resynchronization (request 6b0978a20110746724c64e271b27959449742d95:5023).
10538:S 20 Jan 2020 22:35:33.894 * Successful partial resynchronization with master.
10538:S 20 Jan 2020 22:35:33.894 # Master replication ID changed to 3eda71bd4a1315791d1681992f25cd4cf79f3f7e
10538:S 20 Jan 2020 22:35:33.894 * MASTER <-> REPLICA sync: Master accepted a Partial Resynchronization.
```
在6380那里也发现了它：
```
10012:M 20 Jan 2020 22:35:33.894 * Replica 127.0.0.1:6381 asks for synchronization
10012:M 20 Jan 2020 22:35:33.894 * Partial resynchronization request from 127.0.0.1:6381 accepted. Sending 0 bytes of backlog starting from offset 5023.
```

查看配置文件：~/test/6379.conf, 配置replicaof <masterip> <masterport>可以指定追随谁，masterauth <master-password>可以提前设置好密码。 replica-serve-stale-data yes 设置的是，从机再次上线的时候，同步数据会等一段时间，
在这期间从机的老数据是否对外暴露？yes就是老的数据可以查，no的意思就是必须同步完了才能对外提供数据。`replica-read-only` yes 指的是从机只能读还是支持读写操作。`repl-diskless-sync no`这个设置是指是否直接从主Redis传到从Redis
还是现在主的机器上数据落地，然后再通过网络IO传给从机。如果硬盘IO明显比网络IO慢，则开启. `repl-backlog-size 1mb`设置得越大越有可能只做一个partial resynchronization（增量复制）。在Rdis内部还有个小的消息队列，这个队列的大小
就由这个repl-backlog-size给出，里面记录了从机下线这会儿来的数据，他要是没满，则只触发增量copy，否则触发全量copy，要落地AOF或者RDB

```
min-replicas-to-write 3  #最小有几个副本写成功才算写成功了，有点偏向强一致性了。把取舍的权力交给用户
min-replicas-max-lag 10  #10秒内至少有3个写成功
```
我们到现在一直是需要人工维护主机故障的问题，所以我们还需要HA，见下面的哨兵。

#### 哨兵

这里的哨兵就是上面所说的进行监控的机器，可以是单点，也可以是多点。哨兵也有自己的配置文件:
```
port 26379  #哨兵进程端口号
sentinel monitor mymaster 127.0.0.1 6379 2   #被监控的逻辑名称叫mymaster，一套哨兵可以监控多套主从复制集群，最后是通过需要的票数:将这个主服务器判断为失效至少需要 2 个 Sentinel 同意.
```
以上是最简单的写。其实还有个sentinel.conf文件在源码解压目录：`/usr/local/redis-5.0.7`之下。 参考http://redis.cn/topics/sentinel.html  

由于`/opt/redis/bin`下面已经有`redis-sentinel`了，但是默认是指向redis-server的链接。作为哨兵启动的时候要：
```
redis-server ./26379.conf --sentinel
```
告诉Redis，作为哨兵，而不存储数据, 或者用`redis-sentinel`. 启动之后会打印：
```
11740:X 21 Jan 2020 01:43:10.432 # Sentinel ID is 80778eb29eb217741deafa7f2fd68c706b495fcb
11740:X 21 Jan 2020 01:43:10.432 # +monitor master mymaster 127.0.0.1 6379 quorum 2
11740:X 21 Jan 2020 01:43:10.451 * +slave slave 127.0.0.1:6380 127.0.0.1 6380 @ mymaster 127.0.0.1 6379
11740:X 21 Jan 2020 01:43:10.454 * +slave slave 127.0.0.1:6381 127.0.0.1 6381 @ mymaster 127.0.0.1 6379
```
哨兵立马就通过主知道了有哪些从.现在再启动一个哨兵：
```
redis-server ./26380.conf --sentinel
```
发现两个哨兵都打印：
```
11820:X 21 Jan 2020 01:46:57.526 # +monitor master mymaster 127.0.0.1 6379 quorum 2
11820:X 21 Jan 2020 01:46:57.527 * +slave slave 127.0.0.1:6380 127.0.0.1 6380 @ mymaster 127.0.0.1 6379
11820:X 21 Jan 2020 01:46:57.532 * +slave slave 127.0.0.1:6381 127.0.0.1 6381 @ mymaster 127.0.0.1 6379
11820:X 21 Jan 2020 01:46:58.970 * +sentinel sentinel 80778eb29eb217741deafa7f2fd68c706b495fcb 127.0.0.1 26379 @ mymaster 127.0.0.1 6379
```
即原来的那个哨兵发现了这个新哨兵，而且配置文件里只写了监控谁，没告诉他从和其他的哨兵，他是怎么知道的？这是因为上面的监控理论，监控必须两两组建成势力才可以，否则就成了单个机器，自己说了算，组建成势力之后才能决定自己的行为.再启动一台
哨兵：
```
redis-server ./26381.conf --sentinel
```
则三台哨兵都有类似下面的打印输出：
```
11931:X 21 Jan 2020 01:54:16.543 # Sentinel ID is 895f7b3a185b493197818cb1d421e02e03310f82
11931:X 21 Jan 2020 01:54:16.543 # +monitor master mymaster 127.0.0.1 6379 quorum 2
11931:X 21 Jan 2020 01:54:16.544 * +slave slave 127.0.0.1:6380 127.0.0.1 6380 @ mymaster 127.0.0.1 6379
11931:X 21 Jan 2020 01:54:16.546 * +slave slave 127.0.0.1:6381 127.0.0.1 6381 @ mymaster 127.0.0.1 6379
11931:X 21 Jan 2020 01:54:16.874 * +sentinel sentinel dd9e9b9441d9e812694ed24ae8b85820e2cff13a 127.0.0.1 26380 @ mymaster 127.0.0.1 6379
11931:X 21 Jan 2020 01:54:17.362 * +sentinel sentinel 80778eb29eb217741deafa7f2fd68c706b495fcb 127.0.0.1 26379 @ mymaster 127.0.0.1 6379
```
现在强制主下线，然后发现两个从肯定发现它挂掉了：
```
59:12.087 * MASTER <-> REPLICA sync started
11627:S 21 Jan 2020 01:59:12.087 # Error condition on socket for SYNC: Connection refused
```
一小段时间之后，三个哨兵会投票选出一个leader，leader会打印：
```
11820:X 21 Jan 2020 01:59:31.696 # +sdown master mymaster 127.0.0.1 6379
11820:X 21 Jan 2020 01:59:31.768 # +odown master mymaster 127.0.0.1 6379 #quorum 2/2
11820:X 21 Jan 2020 01:59:31.768 # +new-epoch 1
11820:X 21 Jan 2020 01:59:31.768 # +try-failover master mymaster 127.0.0.1 6379
11820:X 21 Jan 2020 01:59:31.770 # +vote-for-leader dd9e9b9441d9e812694ed24ae8b85820e2cff13a 1
11820:X 21 Jan 2020 01:59:31.780 # 895f7b3a185b493197818cb1d421e02e03310f82 voted for dd9e9b9441d9e812694ed24ae8b85820e2cff13a 1
11820:X 21 Jan 2020 01:59:31.781 # 80778eb29eb217741deafa7f2fd68c706b495fcb voted for dd9e9b9441d9e812694ed24ae8b85820e2cff13a 1
11820:X 21 Jan 2020 01:59:31.846 # +elected-leader master mymaster 127.0.0.1 6379
11820:X 21 Jan 2020 01:59:31.846 # +failover-state-select-slave master mymaster 127.0.0.1 6379
11820:X 21 Jan 2020 01:59:31.909 # +selected-slave slave 127.0.0.1:6380 127.0.0.1 6380 @ mymaster 127.0.0.1 6379
11820:X 21 Jan 2020 01:59:31.909 * +failover-state-send-slaveof-noone slave 127.0.0.1:6380 127.0.0.1 6380 @ mymaster 127.0.0.1 6379
11820:X 21 Jan 2020 01:59:31.976 * +failover-state-wait-promotion slave 127.0.0.1:6380 127.0.0.1 6380 @ mymaster 127.0.0.1 6379
11820:X 21 Jan 2020 01:59:32.409 # +promoted-slave slave 127.0.0.1:6380 127.0.0.1 6380 @ mymaster 127.0.0.1 6379
11820:X 21 Jan 2020 01:59:32.409 # +failover-state-reconf-slaves master mymaster 127.0.0.1 6379
11820:X 21 Jan 2020 01:59:32.475 * +slave-reconf-sent slave 127.0.0.1:6381 127.0.0.1 6381 @ mymaster 127.0.0.1 6379
11820:X 21 Jan 2020 01:59:32.910 # -odown master mymaster 127.0.0.1 6379
11820:X 21 Jan 2020 01:59:33.439 * +slave-reconf-inprog slave 127.0.0.1:6381 127.0.0.1 6381 @ mymaster 127.0.0.1 6379
11820:X 21 Jan 2020 01:59:33.439 * +slave-reconf-done slave 127.0.0.1:6381 127.0.0.1 6381 @ mymaster 127.0.0.1 6379
11820:X 21 Jan 2020 01:59:33.522 # +failover-end master mymaster 127.0.0.1 6379
11820:X 21 Jan 2020 01:59:33.522 # +switch-master mymaster 127.0.0.1 6379 127.0.0.1 6380
11820:X 21 Jan 2020 01:59:33.522 * +slave slave 127.0.0.1:6381 127.0.0.1 6381 @ mymaster 127.0.0.1 6380
11820:X 21 Jan 2020 01:59:33.522 * +slave slave 127.0.0.1:6379 127.0.0.1 6379 @ mymaster 127.0.0.1 6380
11820:X 21 Jan 2020 02:00:03.551 # +sdown slave 127.0.0.1:6379 127.0.0.1 6379 @ mymaster 127.0.0.1 6380
```
非leader哨兵打印：
```
11931:X 21 Jan 2020 01:59:31.669 # +sdown master mymaster 127.0.0.1 6379
11931:X 21 Jan 2020 01:59:31.774 # +new-epoch 1
11931:X 21 Jan 2020 01:59:31.780 # +vote-for-leader dd9e9b9441d9e812694ed24ae8b85820e2cff13a 1
11931:X 21 Jan 2020 01:59:32.481 # +config-update-from sentinel dd9e9b9441d9e812694ed24ae8b85820e2cff13a 127.0.0.1 26380 @ mymaster 127.0.0.1 6379
11931:X 21 Jan 2020 01:59:32.481 # +switch-master mymaster 127.0.0.1 6379 127.0.0.1 6380
11931:X 21 Jan 2020 01:59:32.481 * +slave slave 127.0.0.1:6381 127.0.0.1 6381 @ mymaster 127.0.0.1 6380
11931:X 21 Jan 2020 01:59:32.481 * +slave slave 127.0.0.1:6379 127.0.0.1 6379 @ mymaster 127.0.0.1 6380
11931:X 21 Jan 2020 02:00:02.521 # +sdown slave 127.0.0.1:6379 127.0.0.1 6379 @ mymaster 127.0.0.1 6380
```
以及
```
11740:X 21 Jan 2020 01:59:31.730 # +sdown master mymaster 127.0.0.1 6379
11740:X 21 Jan 2020 01:59:31.773 # +new-epoch 1
11740:X 21 Jan 2020 01:59:31.781 # +vote-for-leader dd9e9b9441d9e812694ed24ae8b85820e2cff13a 1
11740:X 21 Jan 2020 01:59:31.797 # +odown master mymaster 127.0.0.1 6379 #quorum 3/2
11740:X 21 Jan 2020 01:59:31.797 # Next failover delay: I will not start a failover before Tue Jan 21 02:05:32 2020
11740:X 21 Jan 2020 01:59:32.483 # +config-update-from sentinel dd9e9b9441d9e812694ed24ae8b85820e2cff13a 127.0.0.1 26380 @ mymaster 127.0.0.1 6379
11740:X 21 Jan 2020 01:59:32.483 # +switch-master mymaster 127.0.0.1 6379 127.0.0.1 6380
11740:X 21 Jan 2020 01:59:32.484 * +slave slave 127.0.0.1:6381 127.0.0.1 6381 @ mymaster 127.0.0.1 6380
11740:X 21 Jan 2020 01:59:32.484 * +slave slave 127.0.0.1:6379 127.0.0.1 6379 @ mymaster 127.0.0.1 6380
11740:X 21 Jan 2020 02:00:02.495 # +sdown slave 127.0.0.1:6379 127.0.0.1 6379 @ mymaster 127.0.0.1 6380
```
leader哨兵操控着做的故障转移：推举出了新的主,而且哨兵们转而去监控`mymaster 127.0.0.1 6380`了, 这里跟各个哨兵的最初的配置文件就不一样了. 在6380那里打印了：
```
11603:M 21 Jan 2020 01:59:31.976 # Setting secondary replication ID to b52c8ae0ffa3c65fbdb15ef57989baf314d7a4e3, valid up to offset: 130560. New replication ID is e6327c42b3b9414f433ffd1c5035a93ac247e139
11603:M 21 Jan 2020 01:59:31.976 * Discarding previously cached master state.
11603:M 21 Jan 2020 01:59:31.976 * MASTER MODE enabled (user request from 'id=7 addr=127.0.0.1:51227 fd=10 name=sentinel-dd9e9b94-cmd age=754 idle=0 flags=x db=0 sub=0 psub=0 multi=3 qbuf=140 qbuf-free=32628 obl=36 oll=0 omem=0 events=r cmd=exec')
11603:M 21 Jan 2020 01:59:31.978 # CONFIG REWRITE executed with success.
11603:M 21 Jan 2020 01:59:33.250 * Replica 127.0.0.1:6381 asks for synchronization
11603:M 21 Jan 2020 01:59:33.250 * Partial resynchronization request from 127.0.0.1:6381 accepted. Sending 688 bytes of backlog starting from offset 130560
```
MASTER MODE enabled --- 新主登基。然后在6381那里打印：
```
11627:S 21 Jan 2020 01:59:32.475 * REPLICAOF 127.0.0.1:6380 enabled (user request from 'id=7 addr=127.0.0.1:53751 fd=10 name=sentinel-dd9e9b94-cmd age=755 idle=0 flags=x db=0 sub=0 psub=0 multi=3 qbuf=281 qbuf-free=32487 obl=36 oll=0 omem=0 events=r cmd=exec')
11627:S 21 Jan 2020 01:59:32.480 # CONFIG REWRITE executed with success.
11627:S 21 Jan 2020 01:59:33.250 * Connecting to MASTER 127.0.0.1:6380
11627:S 21 Jan 2020 01:59:33.250 * MASTER <-> REPLICA sync started
11627:S 21 Jan 2020 01:59:33.250 * Non blocking connect for SYNC fired the event.
11627:S 21 Jan 2020 01:59:33.250 * Master replied to PING, replication can continue...
11627:S 21 Jan 2020 01:59:33.250 * Trying a partial resynchronization (request b52c8ae0ffa3c65fbdb15ef57989baf314d7a4e3:130560).
11627:S 21 Jan 2020 01:59:33.250 * Successful partial resynchronization with master.
11627:S 21 Jan 2020 01:59:33.250 # Master replication ID changed to e6327c42b3b9414f433ffd1c5035a93ac247e139
11627:S 21 Jan 2020 01:59:33.250 * MASTER <-> REPLICA sync: Master accepted a Partial Resynchronization.
```
从主那里同步数据，以示臣服。哨兵是会动配置文件的，6379哨兵的配置文件变成了：
```
port 26379
sentinel myid 80778eb29eb217741deafa7f2fd68c706b495fcb
# Generated by CONFIG REWRITE
dir "/opt/redis/bin"
maxmemory 3gb
protected-mode no
sentinel deny-scripts-reconfig yes
sentinel monitor mymaster 127.0.0.1 6380 2
sentinel config-epoch mymaster 1
sentinel leader-epoch mymaster 1
sentinel known-replica mymaster 127.0.0.1 6379
sentinel known-replica mymaster 127.0.0.1 6381
sentinel known-sentinel mymaster 127.0.0.1 26380 dd9e9b9441d9e812694ed24ae8b85820e2cff13a
sentinel known-sentinel mymaster 127.0.0.1 26381 895f7b3a185b493197818cb1d421e02e03310f82
sentinel current-epoch 1
```
看见没？mymaster被修改成新的主6380了，而且注明了两个从是6379和6381.  

一个哨兵是怎么知道其他哨兵的？它使用了Redis自带的一个发布订阅的功能。新的主是6380，哨兵们要去监控它，要先去6380拿到两个从，同时在存活的新主上开启发布订阅。我们可以作为订阅方看看哨兵们在聊什么事。在新的主的交互界面下执行
```
PSUBSCRIBE *
```
则会看到：
```
127.0.0.1,26379,80778eb29eb217741deafa7f2fd68c706b495fcb,1,mymaster,127.0.0.1,6380,1
pmessage
*
__sentinel__:hello
127.0.0.1,26381,895f7b3a185b493197818cb1d421e02e03310f82,1,mymaster,127.0.0.1,6380,1
pmessage
*
__sentinel__:hello
127.0.0.1,26380,dd9e9b9441d9e812694ed24ae8b85820e2cff13a,1,mymaster,127.0.0.1,6380,1
pmessage
*
__sentinel__:hello
127.0.0.1,26379,80778eb29eb217741deafa7f2fd68c706b495fcb,1,mymaster,127.0.0.1,6380,1
pmessage
*
__sentinel__:hello
127.0.0.1,26381,895f7b3a185b493197818cb1d421e02e03310f82,1,mymaster,127.0.0.1,6380,1
pmessage
...
...
```
可以看到，哨兵们是通过发布订阅，也就是说所有的哨兵都怼到了master身上，如果有从接到了master上，则各个哨兵会取到从的信息，且通过这个发布订阅来发现其他哨兵  

哨兵配置文件`sentinel.conf`中，重要的配置有：
```
port 26379 
sentinel monitor mymaster 127.0.0.1 6379 2
```
这种方式只解决了单点故障和一些压力山大的问题，但是容量的问题还是存在，并没有有效增长容量。容量问题怎么解决？可以在客户端决定：不同的业务直接就去访问不同的Redis实例；对于每个业务也可以通过Redis的集群（Clluster）模式做分片。
16384个槽位落在N个节点上，达到公司数据变成N份，可以解决容量有限的问题。这一点下一节讲

##### 3.使用追加方式

开启时候，使用appendonly yes这个配置，进行追加的方式进行写入集群。

是有用dump.rdb全量备份的时候，可以进行追随主节点

使用appendonly进行增量备份，是无法进行追随主节点的

##### 4.主节点挂机

手动将主节点挂机

从节点可以变为主节点，直接使用replicaof no one命令



### 哨兵

##### 1.启动3个哨兵，进行监控

命令是：

```
1.redis-server ./26379.conf --sentinel
2.也可以直接启动redis-sentinel
```

```
port 26379
sentinel monitor mymaster 127.0.0.1 6379 2
```

26379.conf文件只需要2行

第二行语义：哨兵 监控 监控名 ip 端口 几票通过

需要过30秒哨兵才能生效



