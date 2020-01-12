# Redis安装

### 一、Redis的数据类型

- string
- hash
- list
- set
- zset

 ### 二、安装

##### 2.1.下载

```
wget http://download.redis.io/releases/redis-5.0.5.tar.gz
```

##### 2.2.解压

```
tar -xf redis-5.0.5.tar.gz
```
不带参数v是为了不让verbose显示的内容回写到客户端来，规避网络IO

##### 2.3.安装
Linux下的程序基本都是C语言开发的。源码安装的套路就是上来先看解压后文件夹里的README.md文件

```
make
make install PREDIX=/opt/redis
```
如果缺失gcc的话要执行
```yum install gcc``` 和
```make distclean```
之后再make，在src生成可执行程序  
make是Linux操作系统自带的编译命令，但他并不知道不同的源码包应该怎么编译，所以他必须找到一个文件叫Makefile。之前的nginx安装的时候，并没有Makefile，所以这里要先执行configure生成这个文件。直接执行make命令不加参数的话，是会去读
Makefile这个文件的。Makefile是程序的厂商提供的，里面有编译和安装的过程，编译就是把源码做成可执行程序；安装就是copy. PREDIX=/opt/redis指定安装到哪个目录，安装生成的文件会copy到那里，不跟源码混在一起


##### 2.4.修改环境变量

```
vim /etc/profile
export REDIS_HOME:/opt/redis
export PATH:.$PATH:REDIS_HOME/bin
```

##### 2.5.安装服务

```
cd utils
./install_server.sh 按脚本填写配置，自动生成脚本文件在/etc/redis/6379
```

会出现：```Please select the redis port for this instance: [6379]``` 也就是说一台物理机器上可以跑多个Redis进程，靠端口号区分，默认6379。直接回车之后，又会问：
```Please select the redis config file name [/etc/redis/6379.conf]``` 也就是说启动多个Redis的话多个配置根据端口不同，放在不同的文件里，不会互相影响，同样log也一样
```Please select the redis log file name [/var/log/redis_6379.log]```，同样data也是```Please select the data directory for this instance [/var/lib/redis/6379]```,install_server.sh也会自动识别.
环境变量里的安装路径```Please select the redis executable path [/opt/redis5/bin/redis-server] ```  
回车之后
会输出：```Copied /tmp/6379.conf => /etc/init.d/redis_6379
Installing service...
Successfully added to chkconfig!
Successfully added to runlevels 345!
Starting Redis server...
Installation successful!```
第二步是把启动脚本装到了/etc/init.d下，并且设置开机启动及其启动级别，并且还把Redis启动了。既然开机可以启动，则在/etc/init.d下必然有个redis_6379脚本.此时在任意一个目录里写
```service redis_6379 start```就可以启动Redis了.还可以指定另一个端口，再跑一个Redis实例，验证：
```[root@master utils]# ps -ef | grep redis  

root      7448     1  0 23:24 ?        00:00:00 /opt/redis5/bin/redis-server 127.0.0.1:6379      
root      7688     1  0 23:40 ?        00:00:00 /opt/redis5/bin/redis-server 127.0.0.1:6380      
root      7705  2556  0 23:40 pts/0    00:00:00 grep redis```
或者
```[root@master utils]# netstat -nlp
Active Internet connections (only servers)
Proto Recv-Q Send-Q Local Address               Foreign Address             State       PID/Program name   
tcp        0      0 127.0.0.1:6379              0.0.0.0:*                   LISTEN      7448/redis-server 1 
tcp        0      0 127.0.0.1:6380              0.0.0.0:*                   LISTEN      7688/redis-server 1 
tcp        0      0 0.0.0.0:22                  0.0.0.0:*                   LISTEN      1961/sshd           
tcp        0      0 :::22                       :::*                        LISTEN      1961/sshd           
udp        0      0 0.0.0.0:68                  0.0.0.0:*                               1857/dhclient    
```
可执行程序只有一份，但是会有多个进程，个个进程有自己的配置文件和持久化目录等资源。  

Redis是单进程、单线程、单实例的，但他一秒钟可以hold住很多的请求，那并发（一定是很多请求）到来的时候，它是如何变得很快的？  

所有的连接、TCP握手等先到达内核，会有很多socket，此时Redis进程和内核之间使用的是epoll，这种非阻塞的多路复用的概念，epoll是内核提供的一种系统调用。  

啥是epoll？  

最开始的内核，每来一个连接就启动一个线程阻塞在那里专门读(read)那个socket来的数据，多线程，但是cpu在一个时间便只执行一个线程，数据来了可能还无法立即读。
看read命令可以安装
```yum install man man-pages```
然后```man 2 read``` 其中2是指2类的（共8类），2类的是指系统调用。系统调用是指内核给程序暴露的供调用的方法。执行后会知道read的用法：```ssize_t read(int fd, void *buf, size_t count);```
fd是文件描述符，每个进程都是个文件，找一个进程，比如7688，执行```cd /proc/7688/fd```然后```ll```就可以看到程序里面的那些东西例如：
```
[root@master fd]# ll
total 0
lrwx------. 1 root root 64 Jan  7 23:40 0 -> /dev/null
lrwx------. 1 root root 64 Jan  7 23:40 1 -> /dev/null
lrwx------. 1 root root 64 Jan  7 23:40 2 -> /dev/null
lr-x------. 1 root root 64 Jan  7 23:40 3 -> pipe:[33901]
l-wx------. 1 root root 64 Jan  7 23:40 4 -> pipe:[33901]
lrwx------. 1 root root 64 Jan  7 23:40 5 -> anon_inode:[eventpoll]
lrwx------. 1 root root 64 Jan  7 23:40 6 -> /dev/pts/0
lrwx------. 1 root root 64 Jan  7 23:40 7 -> socket:[33904]
```
执行```man 2 socket```可以发现```int socket(int domain, int type, int protocol);``` type有一项是SOCK_NONBLOCK 非阻塞的。 

其中的0、1、2、3...就是文件描述符，0代表标准输入，1代表标准输出，2代表err流  

### 系统内核的变迁
#### 1. BIO
每来一个连接启动一个线程，每个线程都阻塞在那里等着对方传过来的数据。JVM一个线程开辟出来的成本：对是共享的，但是线程栈是独立的，各有各的线程栈，默认大小是1M，但是这个大小可以调节，把它调小一点，代表单位内存中可以穿件更多的线程。但是
线程多了 1.调度成本高，浪费CPU 2. 内存成本：1000个线程就消耗出去一个G，32位机器总共就4G，还不能new 对象，堆怎么还要给个1、2G，这样才有了下面的NIO
#### 2. NIO
只用一个线程来监视所有连接的文件描述符（fd），这个线程里有个while死循环，遍历各个fd，轮询。一旦发现有数据，就处理数据，然后再去处理下一个文件描述符。轮询发生在用户空间，同步（遍历、取出、处理都由轮询的那个线程自己来处理）非阻塞，NIO。
但现在的问题是：如果有1000个文件描述符，代表用户进程轮询要调用1000次kernel，成本很大：查询一次fd就得有一次系统调用，用户态和内核态切换一次，保护现场恢复现场等一大堆事情...连接数少还好，多了就麻烦了。
#### 3. 多路复用的NIO
要是不用触发1000次系统调用，只调一两次能不能行？这一点用户这边无能为力，所以只能是内核发展。把用户态轮询的事情扔到内核里完成：增加一个名叫select的系统调用，然后用户控件实际上是调用select:
```
int select(int nfds, fd_set *readfds, fd_set *writefds,
                  fd_set *exceptfds, struct timeval *timeout);
```
1000个fd都要传给内核。select的效果：
```
select()  and pselect() allow a program to monitor multiple file descriptors, waiting until one or more of the file descriptors become "ready" for some class of I/O operation
       (e.g., input possible).  A file descriptor is considered ready if it is possible to perform the corresponding I/O operation (e.g., read(2)) without blocking.
```
内核去监控，直到有一个或多个fd有数据来了，就返回有数据的fd给用户态，然后再调用read，也就是说，read不会调没有数据的fd。对fd的选择处理更精确了，用户空间复杂度变低了。还是同步非阻塞的，只是减少了用户态和内核态的切换次数。我们的程序跑在
JVM（C语言写的）上。多路复用就是把轮询的工作拿到了内核里。这里依靠了内核的进步，Linux内核现在还不能实现AIO，Windows可以。这里还有一个问题：在调用select的时候要传进来很多文件描述符，然后还得挑出谁能用再去调read，而且这些有数据的fd
也是每次都要调用内核的read，用户态和内核态来回切换，有点复杂，下面再把这个复杂度降低一下，做成伪AIO。
#### 4. 伪AIO
首先说一下用户态和内核态，在用户态下，有1000个fd（内存里是一些0101），以他们为参数调用select传到内核态，做数据的拷贝，fd成了累赘了，而在传输的时候我们期望是“0拷贝”。这时可以用mmap内核调用来解决这个问题：
```
void *mmap(void *addr, size_t length, int prot, int flags,
                  int fd, off_t offset);
NAME
     mmap, mmap64, munmap - map or unmap files or devices into memory                 
```
内存映射。曾经，内核有内核的内存地址空间；用户进程有用户进程的内存地址空间。他们都是虚拟的地址空间，在内存中无非就是两个区域，但是内核的内存区域用户进程是不能访问的，所以需要通过传参拷贝fd过去。现在两边搞一个共享的内存空间，通过mmap
来实现，比如：用户的8号位置对应内核空间中的18号位置，内核空间中的19号位置对应用户进程空间中的9号位置等。内核和程序怎么用这个空间呢？用户空间有1000个文件描述符，就都写到共享空间里去，实际上是往共享空间的红黑树里面放，内核就可以看见这棵
红黑树一共有多少个fd，然后用内核再拿着这些fd，通过IO中断看谁的数据到达了，然后就把该fd放在一个链表里（中断处理的钩子函数），上层的用户进程因为也能访问共享空间，所以就能从链表里取出来了数据的fd。epoll有三个调用：epoll_create、
epoll_ctl、epoll_wait. epoll_create被调用之后，返回一个epoll的文件描述符epfd，将来用户空间有一个连接建立了，就把连接写给epfd，后者会准备一个共享空间：mmap。伪AIO与多路复用的NIO的重要变化就是有这个共享空间，里面维护一个红黑树，
用户空间有1000个连接的话，现在是1000个先注册进红黑树，这个共享空间里的增删改的操作是由内核来完成的，查询是内核和用户进程都可以查，主要是用户空间来查。注册一个连接写到红黑树之后，由用户空间调用epoll_ctl(add/delete) socket的文件描述
符，往里面加或者删fd；还有一个调用是epoll_wait，等待事件，这个其实是叫“事件驱动”就是这么来的。在内核里看哪个fd的数据到了，把节点放到链表里去，并维护这个数据是可写还是可读。只要链表中有节点了，wait就可以返回了。wait从阻塞变成不阻塞，
把有数据的那几个fd取出来，因为空间是共享的，直接取出来到用户空间这边。epoll它poll的只是fd，用户还要单独去对fd调用read和write方法读取数据，所以他并不是AIO，仍然是NIO。AIO是有一个buffer等数据，read方法注册到内核，数据来了之后，
假设在内核中完成数据读写了，最终给了用户一个消息或者回调了一个方法：把被回调的方法压到他的线程栈里面去，此时读这个动作根本不是用户在数据到了之后主动去做的，这个才叫AIO，但是Linux很难做到这一点。这个epoll并不是“0拷贝”，虽然有点类似，
下面是“0拷贝”的说明：
```
ssize_t sendfile(int out_fd, int in_fd, off_t *offset, size_t count);

DESCRIPTION
       sendfile()  copies data between one file descriptor and another.  Because this copying is done within the kernel, sendfile() is more efficient than the combination of read(2)
       and write(2), which would require transferring data to and from user space.
```
sendfile发生在两个fd之间，直接在内核态中就通过内核的缓冲区把数据从一个fd给了另一个fd，不走用户态，省了来回切换的过程，更快。一个网卡连着内核，同时一个文件fd也连着内核，这就是两个IO，之前是通过read和write依次调用：文件数据先到内核的
buffer缓冲区，再由read调用拷贝到用户进程的内存区，然后再write拷回来，再由网卡发出去，中间有拷贝的过程。  

sendfile + mmap可以组建Kafka：网卡的数据进来，走内核然后进入JVM（Kafka是Scala写的），使用mmap，而mmap可以对files和devices进行挂载，挂载到文件，又因为mmap有内核共享空间，kafka通过mmap看到内存空间，往里写数据，其实内核也能看到
数据，直接触发内核，这样就减少了用户态内核态的切换，减少数据拷贝，也约等于一个0拷贝，但不是两个fd之间的，而是内核空间和用户进程空间之间的。这就是为什么往里进数据的时候可以很快，而且可以存在文件上。而消费者还要拿着偏移量来把它读出来，这时
走的是“0拷贝” sendfile，sendfile的输入来自文件的fd，输出给网卡上给消费者提供的端口的fd。面试的时候这么答，真的很难被拒绝^_^.  

说回来Redis，Redis是单进程，他用了**epoll**，但是高并发他也不怕，因为内存是相当快的，IO相对来说很慢，由于有epoll，所以把数据会放到共享区，而用户进程可以直接通过mmap用，读出来处理。由于是单线程所以了一个效果：所有的数据到来是有顺序的
其实数据处理是串行的，只有一个线程，挨个的处理一笔一笔的数据，不用额外再加锁。client想保证事务的话，对于同一个key的CRUD，要让他们由同一个连接发出来。跟Kafka一样创建删除同一个资源的话，要打到同一个topic的同一个分区里，而不能做多个分区
的并行。这一块跟nginx也类似，nginx要满足有多少颗CPU就启动多少个worker进程，一个worker进程就可以把数据压到的1、2、3级缓存了，有多少个CPU就有多少个nginx worker进程，每个worker进程使用的也是多路复用epoll，**同步非阻塞多路复用**。
而tomcat 8以后也是NIO了。

### epoll的应用场景

Redis有很多的客户端socket连接进来，很多tomcat通过线程池连到Redis内核，这时Redis相当于用户进程，可以调用所谓的epoll，来寻找哪一个客户端来数据了，这里Redis处理数据的是一个单线程。注意，只是处理数据是单线程的，Redis还有其他的线程在做
别的事情。单进程单线程的好处就是“顺序性”：每连接内的命令顺序，顺序到达串行处理。只要client端的操作能控制好先在Redis里面创建key=a再删除之，则在Redis里面的顺序是能保证相同的。要尽量地把相同的东西打到同一个节点上去（跟Kafka一样），一个
topic的一个分区里放对于一个key的所有的操作.JDK的NIO虽然用了Selector类但是底层查看OS，有epoll就用epoll，否则用select nio