# Kafka

## 简介
市面上常见的消息队列产品：XXMQ Redis Kafka
             
Kafka同步写入内存，但是异步写入磁盘。解决Kafka高性能的核心思想是不是减少内核态和用户态的转换？
MMF，Memory Mapped File。磁盘空间映射到内核级别的内存空间 —— pageCache，用户空间的程序只需要把数据写入到内核空间，
就等价地写入到了磁盘之中。pageCache什么时候会把数据刷新到磁盘呢？由OS决定，这样就极大地降低了写入IO的损失，应用程序
也不会应为等待写入磁盘而阻塞挂起。这里有个问题，应用程序过来的数据太多太快，pageCache不够怎么办？
             
内核级别的内存空间，不同的进程是可以共享的，这一点不同于用户空间。即使应用挂了，也不会影响内核空间的内存，还是会有操作
系统刷入到磁盘。但是操作系统内核不稳定或者断电，就会造成pageCache数据丢失，会造成问题。最最保险的话是直接拿application
以阻塞的方式写到磁盘，但会损失性能。
             
数据被消费者读出的时候，直接通过内核空间将数据传输出去，并不抵达用户空间，0拷贝。DMA加进来会大大减少磁盘读写时CPU的中断次数

## 单机安装

1. 首先安装JDK：
    i    先下载`jdk-13.0.1_linux-x64_bin.rpm`
    ii   如果有原来的JDK则可以卸载：rpm -e `rpm -qa | grep jdk`
    iii  `rpm -ivh ./jdk-13.0.1_linux-x64_bin.rpm`
    iv   `vim ~/.bashrc` 配置环境变量，在文件的最后加入：
          ```
          export JAVA_HOME
          export PATH
          export CLASSPATH
          ```
          保存退出之后执行`source ~/.bashrc`  
          
2. 安装Zookeeper，`tar xf ...` 在`/usr/local/zk/conf`下复制zoo_sample.cfg到zoo.cfg,并修改中的条目：
`dataDir=/root/zkdata`, 然后`mkdir /root/zkdata`创建该目录  

3. 启动zk：`/usr/local/zk/bin/zkServer.sh start zoo.cfg`, 可以jps验证是否出现QuorumPeerMain进程.进一步验证：
```
[root@Kafka_1 bin]# ./zkServer.sh status
JMX enabled by default
Using config: /usr/local/zk/bin/../conf/zoo.cfg
Mode: standalone
```
standalone出现的话就算成功了  

4. 在`/user/local`目录下解压`kafka_2.13-2.4.1.tar`: `tar xf kafka_2.13-2.4.1.tar`