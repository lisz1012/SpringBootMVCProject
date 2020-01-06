# Redis架构

##### 1.1.问题

  redis是单线程，单实例，为什么并发那么多，依旧很快呢？

  回答：因为调用了系统内核的epoll

##### 1.2.Linux的早期版本

  Linux有Linux kernal，我们的客户端，进行连接，首先到达的是Linux kernal，在Linux的早期版本，只有read和write进行文件读写。我们使用一个线程/进程 进行调用read和write函数，那么将会返回一个文件描述符fd(file description)。我们开启线程/进程去调用read进行读取。因为socket在这个时期是blocking（阻塞的），遇到高并发，就会阻塞，也就是bio时期。

##### 1.3.内核的跃迁

  Linux kernal在之后的发展，有了很大的变化，Linux到达率NIO时期。我们可以使用客户端进行轮询访问。但是，我们如果打进1000个线程访问，那么成本就会很大。我们出现了select函数，select函数和pselect函数，我们可以直接传1000个文件描述符，一旦有返回，那么再去调read函数。这个叫做多路复用的NIO。

  紧接着，内核再次跃迁，我们出现了一个共享空间，通过mmap进行空间映射。（本质是红黑树+链表//红黑树是一种自平衡的二叉查找树）。我们将1000个文件描述符写进共享空间，如果我们的数据有返回，那么加入链表，我们从链表取出调用read进行读取。我们出现了一个epoll函数，它能够处理大量并发连接少量活跃的情况。epoll是同步，非阻塞的多路复用。

```
科普：
（参考：https://www.jianshu.com/p/444646e02ef7）
I/O:I/O，input output，即磁盘的输入输出。

蔟（sector）和块（block）:磁盘驱动的最小单位叫做sector，也叫扇区。对于Linux，虚拟文件系统（VFS）抽象了磁盘设备，统一称为块设备（block device）。

页面缓存（page cache）:我们在调用write函数式，会首先写入页面缓存。此时，这个页面叫做脏页面（dirty page），但是，会最终会写回（write back）到内核。如果没有进行write back，那么断电之后，我们就会丢失数据。

mmap:我们的page cache仍然是看不见的，所以，我们通过mmap的映射，可以在应用层直接对page cache进行读写操作。

sendfile:我们可以直接将page cache的fd的一部分数据传给另一个fd，不需要拷贝到应用层的2次copy，所以也被称为零拷贝（zero copy）。

direct I/O:不通过page cache，直接对VFS进行读写，但是在linux2.6之后被废弃

AIO（Asynchronous IO）:异步IO，Linux有两套“AIO”接口，仅仅支持磁盘IO，不支持网络IO
	POSIX AIO:POSIX AIO用信号通知IO完成了，所以，要先注册一个句柄（handle）。
	Linux AIO:第二套IO叫做Linux AIO
	这两套接口都有问题，所以周老师说Linux没有AIO，Windows才有（设计问题）

BIO（Blocking IO）:阻塞型的，早期Linux，没数据会卡住
NIO（NonBlocking IO）:非阻塞型的，没数据直接返回没有数据

IO多路复用（IO Multiplexing）:注册一组socket文件描述符给操作系统，如果IO事件发生，交给程序处理。

select:接受指定的文件描述符数组，来读写和异常
poll（译文：轮询）:优化select，把读写异常这三个变量变为结构体
epoll:创建一个表，然后用文件描述符指向表，监听表内事件

```



