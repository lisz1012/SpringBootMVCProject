# Redis集群

### 介绍

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
  然后执行，在子进程sleep期间，在父进程中更改num的值，则不会影响子进程num的值：
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
2.日志：增删改的时候记录如日志文件，一旦数据没有了，可以通过日志中已经执行过的增删改命令恢复。在Redis里是AOF   


##### 1.单机、单实例的持久化方式

  在我们之前的课程中，我搭建了一个单机，单进程，缓存redis。我们使用rdb,aof持久化，用来确保数据的安全。

```
rdb（relation-ship database）持久化：
默认redis会以一个rdb快照的形式，将一段时间内的数据持久化到硬盘，保存成一个dumpr.rdb二进制文件。
工作原理：当redis需要持久化时，redis会fork一个子进程，子进程将数据写到磁盘上临时一个RDB文件中。当子进程完成写临时文件后，将原来的RDB替换掉，这样的好处就是可以copy-on-write。

配置redis.conf:
save 900 1
save 300 10
save 60 10000

在900秒内，如果有一个key被修改，则会发起快照保存。300秒之内，如果超过10个key被修改，则会发起快照保存。在60秒内，如果1w个key被修改，则会发起快照保存。

aof（append-only file）持久化：
默认会以一个appendonly.aof追加进硬盘。

redis.conf默认配置:
appendfsync yes
appendfdync always
#每次有数据修改都会写入AOF
appendfsync everysec
#每秒同步一次，策略为AOF的缺省策略
```

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



