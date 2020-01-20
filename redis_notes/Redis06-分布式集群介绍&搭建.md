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

2.基于三维进行扩展AKF。

- X轴进行**全量镜像**的节点复制（从单库到多库）

- Y轴进行**业务功能**分离（根据业务逻辑再次进行分库）
- Z轴进行**优先级逻辑**再拆分（单业务进行分片，如MR的DataNode，单业务进行分节点运算）

##### 3.进行集群化面临的几个问题：

![1568362595235](C:\Users\74302\AppData\Roaming\Typora\typora-user-images\1568362595235.png)

1.数据一致性

分量数据不能保证一致，如果保证一致，那么就会丢失可用性。如果我们保证数据的强一致性，那么，我们将会破坏可用性（数据在同步，不能保持可用）。

2.数据完整性

我们保证弱一致，可能会取到错误数据。

3.数据最终一致性

我们如图3的价架构，在中间放一个类似kafka之类的中间件，然后我们能保证最终一致性。（kafka通过一定技术，变得可靠）

##### 4.分布式常见的几个架构：

主备架构：主机死了，备机可以顶

主从复制：主机后面有几个从节点。（redis用的是主从复制）



**主从复制架构，又面临一个问题，单点故障怎么解决？**

我们对主进行HA架构，用一个程序进行监控，该监控程序，为了避免单点故障，那么也必须是一个集群架构。

我们的监控设备一定是奇数台，进行过半选举，如果过半都选举故障，那么，将会跳到另一台节点。

### 配置

##### 1.解压

准备一个redis的tar包，进行解压。

##### 2.启动节点并跟随

启动3个实例，从节点使用replicaof ip port这个命令进行跟随主节点。

（注意，在redis 5之前，我们可以通过slaveof进行跟随主节点，但是，从redis5之后，改为了replicaof进行跟随）

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



