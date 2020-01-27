# Redis单节点容量问题

作业：Redis spring的API自学：https://docs.spring.io/spring-data/redis/docs/2.2.4.RELEASE/reference/html/#reference 先快速多看几遍中文的，再看英文的。各个官网和Github都要会自己查

###  一、单节点容量问题
主从复制既可以让从完全作为备份，一般没有任何读和写流量，只是备用；也可以给做主从的读写分离
我们在实际场景中，往往遇上一个单节点容量问题。要控制一个Redis实例分得多少G的内存，这样不至于数据落地时间太长，动作轻盈。有8、9种方案可以解决容量有限的问题，讲Redis不要只局限于Redis，而是要扩展到
整个分布式的理论。  

数据量很大应该怎么放？
1. 客户端下手解决？从Y轴上来说，可以后台有多个Redis，按照首字母分配存哪些key，也可以按照业务划分各个Redis的职能。这里之所以说是客户端下手解决，是因为这种情况下的一变多，Redis自己是不知道的，在客户端要融入
   一部分代码指定那些数据访问哪些Redis，倾向于业务拆分。

2.到了**数据**不能按业务拆分的时候，可以进行数据分片，一整套同质化的数据sharding分片

- 进行哈希取模（影响分布式下的扩展性%3,%4，如果多加一台机器，就会收到影响）弊端是取模的值是固定额%3跟%4要放的机器是不一样的，影响分布式下的扩展性
- 进行逻辑随机（可以放进去，但是拿不出来）
  - 解决方案：多个Redis用同一个key同时存储一个list，一个客户端来存数据的时候就是作lpush，然后这两个Redis就都有数据了，然后另外一个client直接连2个redis，做lpop，进行两台一起消费，当消息队列来用。
            key就是topic，每个Redis就是partition。生成消息的速度笔消费的快，中间这个队列就缓冲一下，一台Redis内存受限，平行多台Redis。
- 一致性哈希算法
  -  哈希算法是一种映射算法，有：crc16 crc32 fnv md5 sha1 sha256 不管给我什么，返回的都是个等宽的字符串或者一个数字
  -  没有进行取模，要求key（data）和node都要参与哈希计算，往哈希环（虚拟的）上映射。等宽16位，将16位抽象出一个哈希环，计算一致性哈希算法
  -  虚拟节点：一个物理节点拼上10个数字，然后全都hash，于是就映射到了哈希环上的10个随机的点，这样可以解决数据倾斜的问题

```
一致性哈希算法（哈希环）：
1.求出memcached服务器（节点）的哈希值，并将其配置到0～232的圆（continuum）上。
2.采用同样的方法求出存储数据的键的哈希值，并映射到相同的圆上。
3.从数据映射到的位置开始顺时针查找，将数据保存到找到的第一个服务器上。如果超过232仍然找不到服务器，就会保存到第一台Redis服务器上。
来源：（https://www.cnblogs.com/williamjie/p/9477852.html）
```

3.优缺点

优点：加节点的确可以分担其他节点压力，而且也不会造成全局洗牌

缺点：新增节点会造成一小部分数据不能命中（他不影响他后面的data的映射，还能找到）这样会造成击穿Redis，流量压到MySQL，然后再加一遍数据到新节点。解决方案之一是从离着data点最近的两个Redis节点去寻找，有点复杂
     而且新增的node要是挨着的话，找俩也找不到。而且新增节点后面的节点上的一部分数据的访问被新增节点分担，但是数据在后面的节点上还是占据着空间。基于这些缺点，这种方法的使用场景更多的是Redis作为缓存，而不是
     数据库。所以Redis可以开启LRU和LFU，淘汰多余的用不到的数据，而不用把每个数据都读出来一遍，算算该不该存储然后删掉多余的  

以上各个方案都发生在客户端，他们的共同的缺点是只能用来做缓存。生产环境中可能Redis只有2个实例，client有50个。假设2个Redis实例Redis01、Redis02分别跑在两台物理机上，客户端有4个，这样就有8个可能的连接，
Redis的连接成本很高，这个成本是对Server端造成的。反向代理服务器nginx（自己不干存东西、不参与计算、不产生页面，只是把连接hold住，我们叫它接入层）可以把连接（握手）hold住，接入之后，把请求给后端，后端是
真正干活的。基于反向代理还可以做一个负载均衡。所以一般不让client直接连接各个Redis Server，而是在中间又一个proxy，4个client连接proxy，proxy转发请求到2个Redis Server，这样Server端的Socket连接只有
2个，压力减小了，每个Redis server上只有一个socket连接。这时候只需要关注代理层的性能就可以了。而如果这时候client太多，一台nginx hold不住怎么办？这时可以做nginx的集群，前边再放一个lvs，提供VIP。LVS靠
keepalived可以做一个主备。keepalived不仅可以监控lvs，也可以监控各个nginx的健康状态，如果发现有的nginx挂了，就会触发脚本，剔除那一台nginx。Redis连多线程都没有去触碰，可见作者就是不想引入太多技术，单纯
使用Redis就行，别太费劲了。无论企业后面技术多么复杂，对于客户端是透明的，lvs就有只一个VIP，给client调用的时候client只需找这个VIP就行了。这时候，哈希取模（modula）、逻辑随机（random）、一致性哈希
（kemeta）等原本在客户端的代码写在nginx上就可以了。这个东西叫做“无状态”，意思就是nginx这里并不需要有数据库，不存数据，只有达到无状态的代理，才会很容易的实现“一变多”，引入多个nginx和lvs。三个模式最好的是
一致性哈希，只能作为缓存用，这个怎么解决？可以用“预分区”解决：  

现在有一台Redis，将来可能新增到好几台Redis节点。新增的节点会对代理或者客户端有一个挑战。取模的时候之所以要rehash，是因为加一台机器就会该变模数，现在有2台机器，但是直接%10，余数是0-9，中间再加一道mapping
表示第一个节点负责0-4，第二个节点负责5-9，然后新加进来的第三个节点可以让第一个节点匀过来3、4，第二个节点匀过来8、9，并且把对应的3、4的数据和8、9的数据直接传输给第三个节点，只要迁移完数据，任何新的数据的读
写都可以到正确的位置上读写了。数据迁移的时候先把时点数据传到目标机器上，并且做一个花虫，相当于RDB和增量日志，先给过去全量的数据，再把缓冲的记录发过去，数据可以追平。这中间的mapping好像需要个代理，但是Redis
做的让用户用起来更简单：用户可以访问任何一个Redis节点，每个Redis节点有计算hash取模的算法和所有取模的结果和Redis机器的映射关系，也就是他们都知道所有人都有哪部分的数据。所以每个实例都可以当家作主，因为大家
都有相同的算法。如果数据经过取模计算之后得知不在这台机器上，则会让客户端redirect到那台有数据的机器上，客户端跳转，这样就直接可以拿到数据了。这个模型就是无主(主主)模型，缺点是一旦有机器挂掉了它上面的数据没有
备份。而且客户端有时候会发两次请求。做分片数据分治的时候，聚合操作和事务很难实现，比如Redis做交集，这种情况下无法实现，因为不在当前访问节点上的数据会有一个数据移动的过程。Redis的作者很细腻，一般会让计算向数
据移动，而不会移动数据，而且Redis最大的特点就是快，所以作者一直在做一个取舍：把影响性能的功能都抹杀掉了。这个锅由Redis甩给了人：hash tag。什么叫hash tag？数据一旦被分开就很难被整合使用了，换句话说：数据
不被分开就能够发生事务，就是说，数据如果都在一个节点上，对一个节点做事务就不会报错。hash tag是：有两个key：(oo)k1和(oo)k2，又用户决定，仅仅拿着oo取模而不是整个key取模，这样他们就会落在同一个节点上。
Redis是让要执行事务的用户自己去把那些数据放到一个节点上去，这样事务还给用户保证能执行成功。详见： http://redis.cn/topics/partitioning.html

### 二、twemproxy

https://github.com/twitter/twemproxy 有的开源软件甚至可以在releases tab下面下载编译完的结果。要养成看github跟着操作的习惯，而且看什么东西要先看完，别还没整体看完就开始跟着做了

twemproxy是一种代理分片机制，由twitter开源，twemproxy作为代理，可以接受多个程序访问，按照路由规则，转发为后台各个Redis服务器，再进行原路返回，该方案很好的解决了Redis实例承载能力问题。

**安装**

```
git clone https://github.com/twitter/twemproxy.git
如果报错，执行：yum update nss
如果没有git就: yum install git 
yum install automake libtool
autoreconf -fvi
如果报错，执行
yum search autoconf 来查看，发现版本是213，太低。这里有个常识：yum装的软件来自于仓库，可能系统自带的那个仓库就是版本偏低,
                    那里有更高级一点的仓库呢？http://mirrors.aliyun.com 找到epel，点进去找到"下载新repo到/etc/yum.repos.d/"然后复制以下命令到命令行执行
wget -O /etc/yum.repos.d/epel.repo http://mirrors.aliyun.com/repo/epel-6.repo   这样/etc/yum.repos.d/下面就添加了一个源多了一个仓库
yum clean all
再次执行yum search autoconf，发现多了一条：autoconf268.noarc。 回到/root/soft/twemproxy中，然后执行
yum install autoconf268.noarch -y
autoreconf268 -fvi  # 这是生成configure
cd twemproxy/
./configure --enable-debug=full # 会生成MakeFile
make  # 会有可执行程序出现, 进入src目录后会看到nutcracker可执行文件
查看服务文件
cd scripts
nutcracker.init
拷贝这个文件进/etc/init.d目录: cp nutcracker.init /etc/init.d/twemproxy
nutcracker.init里面指定了OPTIONS是"-d -c /etc/nutcracker/nutcracker.yml, 所以要创建/etc/nutcracker/目录 mkdir /etc/nutcracker/ 配置文件nutcracker.yml在/root/soft/twemproxy/conf
下面，所以执行cp /root/soft/twemproxy/conf/* /etc/nutcracker/ 把文件copy过去，以符合OPTIONS中的设置
拷贝编译运行文件进/usr/bin目录：cp /root/soft/twemproxy/src/nutcracker /usr/bin  然后再操作系统的任何位置就都可以使用这个命令了
拷贝conf文件夹进/etc/nutcracker目录
进入/etc/nutcracker，修改nutcracker.yml进行配置

alpha:
  listen: 127.0.0.1:22121
  hash: fnv1a_64
  distribution: ketama
  auto_eject_hosts: true
  redis: true
  server_retry_timeout: 2000
  server_failure_limit: 1
  servers:
   - 127.0.0.1:6379:1
   - 127.0.0.1:6380:1              # 最后面这个1是他的权重

然后创建并分别进入~/data/6379和~/data/6380目录，然后再redis-server --port 6379 和 redis-server --port 6380 Redis手工起的话就拿当前目录作为他的持久化目录
然后再启动服务：service twemproxy start  监控的端口是22121
此时redis-cli -p 22121 就能连上22121端口了，也就是twitter的代理那台机器，连的不是具体某一台的Redis机器.在 127.0.0.1:22121>之下可以添加修改查看keys，都有效
也可以绕过代理直接登录真实的Redis服务器查看那些数据被放到了哪些Redis机器上
之后开启nutcracker服务，开启service服务，之后连接redis-cli进行连接22121端口.作为编程人员，只能去连接22121，而不能直接去连接6379和6380，甚至连代理后面有几台机器都不知道
我们通过nutcracker进行get和set，我们在nutcracker不支持的命令：
keys *
watch k1
multi
这些命令都不支持, 因为他们是多条指令一起先缓存，然后EXEC发送出去。要是支持的话就相当于三次握手握到了不同的机器上耍流氓一样是不可以的，数据分治了就不好做这件事情了

```

### Predixy
predixy软件，也可作为替代品

```
wget https://github.com/joyieldInc/predixy/releases/download/1.0.5/predixy-1.0.5-bin-amd64-linux.tar.gz -O predixy-1.0.5-bin-amd64-linux.tar.gz
```
或者在https://github.com/joyieldInc/predixy/releases 下载
修改predixy.conf 参考：https://github.com/joyieldInc/predixy/blob/master/doc/config_CN.md 其中“Group xxx”是指定那个Master，一个哨兵可以监控多个主从复制，上面的
“Distribution modula|random”可以把数据打散到多套主从复制里

```
打开Bind 127.0.0.1:7617
打开include sentinel.conf
注释掉：#Include try.conf
```

然后vim sentinel.conf修改哨兵的配置。 按冒号后输入`.,$y`回车是选中并复制当前光标行到最后的文字，p是粘贴 `.,$s/#//`是把选中的部分的#全部去掉. `2dd`是删除包括当前光标行在内的往下的2行

修改26379的哨兵

```
port 26379
sentinel monitor ooxx 127.0.0.1 36379 2
sentinel monitor xxoo 127.0.0.1 46379 2
```
上面的“ooxx”和“xxoo”就是要填入Group XXX那里，取代XXX，然后上面的Distribution modula就会把数据哈希取模，指不定放进哪一个group

修改26380的哨兵

```
port 26380
sentinel monitor ooxx 127.0.0.1 36379 2
sentinel monitor xxoo 127.0.0.1 46379 2
```
同理，修改26381哨兵
```
port 26381
sentinel monitor ooxx 127.0.0.1 36379 2
sentinel monitor xxoo 127.0.0.1 46379 2
```
哨兵的配置只是最上面的端口不一样，如果三台物理机，端口也可以一样了  
现在配置了一个三哨兵集群：26379、26380、26381，一起监视着两个Redis主从复制集群，他们的Master分别是36379和46379


下面分别启动。先跑哨兵：
```
redis-server ~/test/26379.conf --sentinel
redis-server ~/test/26380.conf --sentinel
redis-server ~/test/26381.conf --sentinel
```
再启动两套主从复制集群：
```
[root@chaoren0 test]# mkdir 36379
[root@chaoren0 test]# mkdir 36380
[root@chaoren0 test]# mkdir 46379
[root@chaoren0 test]# mkdir 46380
[root@chaoren0 test]# cd 36379/
[root@chaoren0 36379]# redis-server --port 36379
[root@chaoren0 36380]# redis-server --port 36380 --replicaof 127.0.0.1 36379
```
再启动代理：
`/root/soft/predixy/predixy-1.0.5/bin/predixy /root/soft/predixy/predixy-1.0.5/conf/predixy.conf`

predixy只支持单Group的事务。这里由于predixy是64位的而CentOS是32位的，实验做不了

之后可以直接测试

相类似的，还有codis和Redis自带的Cluster