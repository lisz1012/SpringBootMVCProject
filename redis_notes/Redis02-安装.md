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
Linux下的程序基本都是
语言开发的。源码安装的套路就是上来先看解压后文件夹里的README.md文件

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
然后```man 2 man-pages``` 其中2是指2类的（共8类），2类的是指系统调用。系统调用是指内核给程序暴露的供调用的方法。执行后会知道read的用法：```ssize_t read(int fd, void *buf, size_t count);```
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
其中的0、1、2、3...就是文件描述符，0代表标准输入，1代表标准输出，2代表err流
后来，系统内核发生了跃迁：

