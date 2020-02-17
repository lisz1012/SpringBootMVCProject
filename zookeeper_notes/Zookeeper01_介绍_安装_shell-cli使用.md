# Zookeeper

## 介绍

完成分布式的协调是一件很难的事情。在Redis击穿和穿透的时候都需要分布式锁。Redis数据没有了的时候，很多并发这时候到达了，尽量的不把并发的请求透穿到数据库里去。这样就需要在数据库前面卡一下：第一个过来的人抢一把锁，后面的没抢到锁
所以就在那里阻塞着（有点像自旋锁），第一个人抢到锁之后就去后面的数据库拿到数据，并放入缓存，其他人等时间到了之后再去Redis把数据取走。所以即便是数据有穿透，这么大的并发也不是一下子全压过去，而只是压过去了一笔。但是这个事做起来
太麻烦了，这一点在Redis里面讲过。zookeeper现在要做的就是这个分布式协调, 而且zookeeper的数据也是保存在内存中的，也能类比Redis.Zookeeper也一般是一个集群，一谈到集群就是主从复制集群或者无主集群，要么就是镜像拷贝的，要么
就是sharding分片的集群。zk属于主从复制集群，zk好多进程分布在不同的节点，每个节点的数据都一样的，server中个leader，类比主从复制的Redis集群，zk也是主节点可以增删改查，从节点只能读。当我们一看到主从集群的时候，条件反射地就
应该反应出 ----- 单点问题. 客户端想连谁就连谁，写操作在leader这里，但是读操作在任何节点。所以连到从节点却要执行写操作的话会转到leader。leader一旦挂掉是可以快速自我修复的，zk集群极其高可用。如果有一种方式可以快速恢复出一
个leader，集群就有恢复到了可用的状态。zk leader挂掉的时候，进入无主状态，此时所有的请求操作都没有了，集群不可用，关门了，不对外提供服务。从不可用状态恢复到可用状态速度越快越好。zk选择新领导者的时间不到200ms。zk做读写分离
读写之比为10:1做有的时候读的速度是非常快的，跟Redis类似，都发生在内存，单进程在一个物理机上每秒能hold住100000请求。这就是内存中运行的好处。  

zk在内存中是树状文件结构，与一般的文件系统不同的是每个节点都可以存数据，最大1M。强调：不要把zk当做数据库用，速度快的言外之意是传输的数据体量变小。Redis可以当做数据库用，但是zk不行。zk要配合10:1读写比例应用才好。1M的限制就
是为了对外该提供协调服务的时候会很快。zk的节点分为1.持久节点 2.临时节点 Session的概念：每一个客户端连接到zk的时候都会产生一个session来代表这个客户端，与Tomcat有点类似。Session能描述这个用户是谁。临时节点是依托于
session的。引入临时节点的目的是可以用session实现锁，在Redis中实现锁会有过期的问题，这里zk的client一旦结束，session也就没了，解锁，别人就可以用了，即便client挂了，session也随之消失，所以连过期时间都不用设置了，无需任何
代码来像Redis那样再抛一个线程监控锁是不是快要过期了，完全靠session。这两种节点都可以带序号（支持序列节点），先记着，后面讲。zk和Redis一样有顺序一致性，这是如何实现的呢？可以看到zk是主从复制，一个单机负责写操作，由单机做序
列的维护是很容易的。这里利用了“单点”的好处。写请求过来之后就在这一台机器这里排队，编号顺序处理，这里zk有点像Redis。zk的原子性：先写一个节点A给leader，leader再复制，给各个从节点。原子性是指：创建A这件事要么全体集群都成功，
要么大家都失败，只就是强一致性了，所以在集群条件下，就一定会破坏可用性。而zk自称高可用性，所以他一定是过半成功就表示成功 --- 最终一致性。zk的可靠性是靠持久化来保证的，写日志、拍镜像... zk还有及时性：其实是最终一致性，
client在另一半没同步到的机器上找A，这台机器先去同步在返回A，最终会返回一个整个集群一致的A的状态。  

## 安装与配置

### 安装

注意！！一定是下载XXX-bin.tar.gz,而不是tar.gz（否则找不到类）下载文件后解压，然后放到`/opt/mashibing`目录下即可  
https://www.jianshu.com/p/c5dd1b4b0697  

### 配置
更改主机名：
/etc/hosts:  
```
127.0.0.1   localhost node03
192.168.1.131   node01
192.168.1.132   node02
192.168.1.133   node03
192.168.1.134   node04
```
/etc/sysconfig/network:
```
NETWORKING=yes
HOSTNAME=node03
```
重启机器  


进入`/opt/mashibing/apache-zookeeper-3.5.7/conf`目录下`cp zoo_sample.cfg zoo.cfg`然后进入后者查看都配置了什么。tickTime：leader和follower之间是有心跳的，来维护对方还活着这件事情，这是心跳的时间，根据生产环境
来。initLimit=10，说明follower和leader建立连接的时候，leader最多可以忍受follower 2000*10 = 20s的初始延迟，超过了就不要这个follower了。syncLimit=5 也就是说5 * 2000 = 10s，leader下达同步的时候10秒之后还没有
得到follower的回馈的化，也认为这个follower有问题。dataDir改为`/var/mashibing/zk`(别忘了创建)未来放zk的日志、快照和myid等数据文件. clientPort=2181是开放的服务端口。maxClientCnxns=60允许最多连接60个.在配置的最
后，加上：
```
server.1=0.0.0.0:2888:3888
server.2=node02:2888:3888
server.3=node03:2888:3888
server.4=node04:2888:3888
```
当前这台机器上写0.0.0.0 其余的写机器名或者IP。  

这里跟Redis不同，没有一个pubsub发布订阅来发现都有哪些机器，所以必须手动配置的时候就写出来。而且写下这些节点之后，他们的行数除以2 + 1就是“过半数”，把这个“过半数”。这几行写的都是什么？node01是节点名，3888是在leader挂掉之
后或者第一次启动还没有leader，大家都慌着的时候的时候，通过3888这个端口建立连接，通过3888这个端口的socket通信，投票选出一个leader，选出的leader会启动一个2888的端口，监听。其他的节点去连接leader的2888端口，后续在有
leader情况下的的通信在2888端口。server.x这个x最大的自动成为leader，由于过半机制，要么server.3=node03 要么 server.4=node04 就成为了leader.  

`cd /var/mashibing/zk`在这个目录下创建一个叫myid的文件里面就只写一个数字，比如在node01上的这个myid文件写入：1. myid里面的值一定要跟`zoo.cfg`配置文件server.x中的x相一致. 分发文件到各台机器。然后从node01开始挨个
启动: `zkServer.sh start-foreground` 前台启动,前台阻塞，实时打印日志（`start`参数是后台启动）。其中，leader机器会在日志里打印这样一句话：`LEADING - LEADER ELECTION TOOK`.leader下线会选出新的leader，也会打印
这句话。集群第一次启动的时候比myid选出leader，再启动的时候看谁的数据完整，都差不多的话再比较myid数字.

客户端登录：`zkCli.sh`默认登陆本机。`ls /`命令查看目录结构，更多命令可以输入help回车查看,create命令可以查看：
```
[zk: localhost:2181(CONNECTED) 2] create /abc
Created /abc
[zk: localhost:2181(CONNECTED) 3] ls /
[abc, zookeeper]
```
前一个版本必须写`create /abc ""`才可以创建成功，创建失败也不会报错. 还可以创建多级目录：
```
[zk: localhost:2181(CONNECTED) 4] create /abc/xyz
Created /abc/xyz
[zk: localhost:2181(CONNECTED) 5] ls /
[abc, zookeeper]
[zk: localhost:2181(CONNECTED) 6] ls /abc
[xyz]
[zk: localhost:2181(CONNECTED) 7]
```
还可以用get命令：
```
[zk: localhost:2181(CONNECTED) 7] get /abc
null
[zk: localhost:2181(CONNECTED) 8] get /abc/xyz
null
[zk: localhost:2181(CONNECTED) 9] get /zookeeper

[zk: localhost:2181(CONNECTED) 10]
```
set数据, 最多放1M，而且他也是二进制安全的（外界推送过来自己出租，项目重要约定好序列化和反序列化器）：
```
[zk: localhost:2181(CONNECTED) 10] set /abc "hello"
[zk: localhost:2181(CONNECTED) 11] get /abc
hello
```
```
[zk: localhost:2181(CONNECTED) 15] get -s /abc
hello
cZxid = 0x50000000b
ctime = Sun Feb 16 22:05:11 PST 2020
mZxid = 0x50000000d
mtime = Sun Feb 16 22:19:45 PST 2020
pZxid = 0x50000000c
cversion = 1
dataVersion = 1
aclVersion = 0
ephemeralOwner = 0x0
dataLength = 5
numChildren = 1
```
cZxid: zk是顺序执行，体现在这个ID值上，所有的写操作，给到任何节点的时候都会被递交给leader，leader因为是单机，所以维护一个单调递增的计数器很容易。0000000b一共32位，低32位是事务的递增ID；前32位00000005（0省略）
代表leader的纪元，表示leader几世，每换一个新的leader就会递增一个。field中，第一个字母c表示创建，m表示修改。pZxid：当前的节点下，创建的最后的节点的那个事务的ID号, 这里是cZxid = 0x50000000c:
```
[zk: localhost:2181(CONNECTED) 16] get -s /abc/xyz
null
cZxid = 0x50000000c
ctime = Sun Feb 16 22:12:29 PST 2020
mZxid = 0x50000000c
mtime = Sun Feb 16 22:12:29 PST 2020
pZxid = 0x50000000c
cversion = 0
dataVersion = 0
aclVersion = 0
ephemeralOwner = 0x0
dataLength = 0
numChildren = 0

```
ephemeralOwner：临时的持有者，当前取值是0x0，`/abc/xyz`没有归属者，create的时候没带任何选项，他是持久节点。下面用create -e选项创建临时节点：
```
[zk: localhost:2181(CONNECTED) 2] create -e /aaa
Created /aaa
[zk: localhost:2181(CONNECTED) 3] set /aaa "afwerf"
[zk: localhost:2181(CONNECTED) 4] get -s /aaa
afwerf
cZxid = 0x500000010
ctime = Sun Feb 16 22:51:42 PST 2020
mZxid = 0x500000011
mtime = Sun Feb 16 22:51:51 PST 2020
pZxid = 0x500000010
cversion = 0
dataVersion = 1
aclVersion = 0
ephemeralOwner = 0x100016627d10002
dataLength = 6
numChildren = 0
```
ephemeralOwner就不是0x0了.另起一个zkCli查看，当前session的zkCli退出之后就没有aaa了
```
[zk: localhost:2181(CONNECTED) 0] ls /aaa
[]
[zk: localhost:2181(CONNECTED) 1] ls /
[aaa, abc, zookeeper]
[zk: localhost:2181(CONNECTED) 2] ls /
[abc, zookeeper]
```
一个临时节点被创建了之后在他的client退出之前，再有其他的client创建同名的临时节点的话，就会报错：
```
[zk: localhost:2181(CONNECTED) 2] create -e /abc/fff
Node already exists: /abc/fff
```
直到当前节点退出。
如果连接的server挂掉了，session是高可用的，不光是数据，连session也会统一视图，不会消失。一台机器上的server ID其他所有机器也都知道，所以支持客户端的failover.
```
[zk: localhost:2181(CONNECTED) 4] create /ccc
Created /ccc
[zk: localhost:2181(CONNECTED) 5] get -s /ccc
null
cZxid = 0x500000019
ctime = Sun Feb 16 23:28:43 PST 2020
mZxid = 0x500000019
mtime = Sun Feb 16 23:28:43 PST 2020
pZxid = 0x500000019
cversion = 0
dataVersion = 0
aclVersion = 0
ephemeralOwner = 0x0
dataLength = 0
numChildren = 0
[zk: localhost:2181(CONNECTED) 6] create /ddd
Created /ddd
[zk: localhost:2181(CONNECTED) 7] get -s /ddd
null
cZxid = 0x50000001b
ctime = Sun Feb 16 23:30:21 PST 2020
mZxid = 0x50000001b
mtime = Sun Feb 16 23:30:21 PST 2020
pZxid = 0x50000001b
cversion = 0
dataVersion = 0
aclVersion = 0
ephemeralOwner = 0x0
dataLength = 0
numChildren = 0
[zk: localhost:2181(CONNECTED) 8]
```
中间有一个其他的客户端连接到了集群，所以ID号跳过了一个，这从侧面证明了session也是统一视图的，要走leader，把这个事写给所有人的内存。API连接集群的话，一旦挂了就去连别的机器，会亮出session ID，设置临时节点超时时间，比如
3秒，内回来了的话，节点是不会消失的。客户端断开的时候，删除session有会走一个删除的事务.  

多个客户端创建同名节点的时候覆盖的问题如何解决？用create -s参数：
```
[zk: localhost:2181(CONNECTED) 0] create -s /abc/xxx
Created /abc/xxx0000000002
```
后面拼上了一个数字，这样就可以规避覆盖的问题。另一个角度讲这就是分布式命名或者分布式ID，因为每一个人拿到的是绝对不会重复的节点名称。客户端要自己记住拼上的数字。做了隔离可以统一命名。每个人都想创建自己的节点，又都怕覆盖了别人
的节点，zookeeper于是就要求客户端记住拼上的数字，未来读写的时候带上这个数字就可以了. 删掉节点，ID计数依然会递增，不会重复出现被删除的ID计数：
```
[zk: localhost:2181(CONNECTED) 3] rmr /abc/xxx0000000002
The command 'rmr' has been deprecated. Please use 'deleteall' instead.
[zk: localhost:2181(CONNECTED) 4] deleteall /abc/xxx0000000002
Node does not exist: /abc/xxx0000000002
[zk: localhost:2181(CONNECTED) 5] ls /abc
[xxx0000000001, xyz]
[zk: localhost:2181(CONNECTED) 6] create -s /abc/xxx
Created /abc/xxx0000000003
```
以上功能可以完成：1.统一配置管理 --- 1M数据、统一视图。2.分组管理 --- path结构 3. 统一命名 --- sequential 4.分布式锁 --- 临时节点。可以创建一个父节点，下面有很多带序列的临时子节点，然后就会有好几把锁。如果后一个锁
盯住前一个锁，只有前一把锁消失之后后一把锁才继续操作的话，这样就可以做队列式事务。这些高级一些的功能需要客户端代码去实现。zk还可以做HA选主节点