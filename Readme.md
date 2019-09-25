部署项目的时候一般都打成jar包而不是war包：
项目上右键，Run As， mvn install之后就会在target目录下面出现jar包
java -jar命令或者双击都可以运行他.

这里会有个问题：jar包可以被用户下载
上传文件（图片）有两种存储方法：1.在DB表种存储完整的URL路径。2.存储相对路径（文件名）
bootstrap有默认头像的图片。
数据库里可以只有Username password，这些的查询频率最高，其他的数据可以放到es里（冷数据备份）
account_role, role_permission这种连接表都是手动处理，不用mybatis-generator-gui生成

MyBatis的坑：关联的各个表里面不能有相同的列名，否则就要起别名避免冲突, 所以SQL中要起别名，而且在resultMap的column那里也要改，
比如id改aid，rid，pid，name改成role_name, permission_name

前后端分离之后一律只返回JSON，没有什么model.addAttribute什么的了

中间件：有功能但是没有业务，如：redis，kafka，tomcat

YAML文件的优先级低于application.properties这种文件，后者会覆盖YAML的属性

有用的一点是：启动springboot jar项目的时候可以用：
java -jar SpringBootMVCProjectApplication-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
命令来启动，可以动态选择生产或者测试或者开发环境.但是具体覆盖笼统，县官不如现管：
java -jar SpringBootMVCProjectApplication-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --server.port=80
会在80端口监听
优先级： jar命令参数 > application-dev/test/prod.properties参数 > application.properties > x.yaml
不用背，用两次就记住了。
启动起来就不会变化的数据不太适合放在数据库里

权限在内存中的存储：Map<uri, crud>
menu表以后要全部读出来做成缓存，村道系统内存，所以就不参与多表关联了；要做的话其实也可以再跟permission表关联

能不能一个账户有多个角色？能：各个权限维护起来相对独立，但是新功能加进来的话要往多个角色里面都添加对它的权限，会有冗余的
权限信息在数据库表里，尤其是多对多的中间关系表里；不能的话，生数据库空间，但是后期维护的时候比较麻烦


# Nginx
nginx.conf里面worker_processes表示有多少进程；events下面的worker_connections表示每个进程最多支持的连接数，不大于服务器内核每个进程能够打开的最多的文件句柄数：less /proc/sys/fs/file-max.除此之外还要考虑物理内存
修改单一进程的句柄个数：ulimit -SHn 65535 
less /proc/sys/fs/file-nr 是当前句柄数，句柄相当于内存里的指针

mime.type文件里有很多类型头和类型，比如:video/mp4   mp4浏览器遇到这个类型头且支持这个类型的显示或者播放的话，就会显示或播放在浏览器里；否则弹出下载框让用户下载。从服务器端发送类型到浏览器，浏览器决定自己的行为。
default_type是默认的类型或行为：application/octet-stream弹出下载框。在服务器端，@ResponseBody的API会在返回对象的时候加一个头信息：application/json

Nginx会作为数据收集器，所以会有日志相关的配置，跟大数据相关些。
各种配置参考：http://tengine.taobao.org/nginx_docs/cn/docs/
sendfile off 相当于control + c。app以字节码的方式加载文件，然后以字节码的方式复制给内核，内核再发给网卡（NIC）
sendfile on 相当于control + x  由app发送一个指令给内核，内核去读文件，由内核直接推给网卡，只有一次复制操作 --- 异步网络IO
sendfile on业户出问题，有时候要有意关掉，比如网上图片加载一半出不来了，就是因为file传输太快，没来得及去解析，接受的时候出问题了，文件来的特快，接收端解析的程序没跟上，这时候就关掉sendfile。以低性能对低性能
后面拿Nginx开发插件儿，这里面能玩儿的东西太多了
tcp_nopush linux内核网络相关的，优化tcp网络连接的一些属性，打开会对优化网络传输，跟TCP缓存相关，tcp不是一个字节一个字节往外发数据。微批处理也叫流处理，也叫实时处理
keepalive_timeout尝试连接多少秒后超时，返回一个错误页面
gzip是网络压缩，多年前网络不好的时候产生的。server给client发html的时候，html里面空格特别多，压缩可以大幅节省贷款，但是两端需要压缩和解压操作，都需要内存和CPU和浏览器的支持。对于固定页面可以事先准备一些压缩好的包。
	不过5G要来了，所以没那么大的必要了。不太希望客户端做太多计算
Server 一个Nginx对应一个server，开n个Nginx就是有N个虚拟主机，但他们的域名和端口号不能相同。一台主机的性能跑一个网站有时候有点浪费。虚拟主机有基于ip的，一个主机可以绑定多个ip。有基于servername的，
	用不同的主机名虚拟出server来，有基于端口的，前面都重复port不同也能行
	location 虚拟目录：表示域名后面什么都不加就是在nginx家目录下的html目录里面找页面文件，如果/后面没写文件名，则默认返回html目录下的index.html或者index.htm，找到第一个就不找第二个了location，root，index都是关键字
	location / {        <------ /就是网站的根目录，两侧少一个空格都不行. 找到Server对应的机器之后就找location对应的URI，location 就是帮我们定为资源的，本地磁盘目录和URI的对应关系，跟路径下找静态文件。location不要冲突
		root html;
		index index.html index.htm
	}
	listen

反向代理
服务器是在自己公司内部互相调用的，不能全部被暴露在公网上，当用户访问服务器的时候，通过代理来指向具体的出口服务器。当然这里的功能就不只是过滤上网了，还可以做负载均衡 --- URL哈希，此时可以把特定的URL请求打到定向的服务器上。
还可以节省IP地址，其实也是保护有些IP，过滤一下用户的请求了。“反向”和“正向”只是观察角度不同而已。用户把请求发到代理服务器的时候，代理服务器会将其中转到后台服务器们，后者将response在返回给代理服务器，代理服务器再把结果给用户
这样有个小问题：1. 代理的负载会很高 2. IO瓶颈，proxy的IO负载大，IO密集的时候，代理服务器这一块儿也要做负载均衡，就是response的方向上，server和proxy之间也需要load balance。在nginx之上加一层lvs，使得response直接给
用户。这样的话只有用户发的请求才打到反向代理服务器上，降低了IO。用户上传文件的情况除外，但一般我们会把用户上传的模块独立出来，所以也不会对反向代理造成压力。反向的代理更倾向于在服务端做一些业务逻辑或者性能提升。反向代理还可以做
软防火墙，分析URL是否合法，频率是否过高（自己写的爬虫程序会遇到反向代理制造的麻烦^_^）物理的路由器提供这些功能就没有那么灵活了，不好扩展
location{}的里面些proxy_pass相当于服务器端跳转。匹配的时候， 先匹配/baidu ，再匹配/，看他下面有没有baidu这么个目录。就是说先匹配最精确的.注意里面那个网址最后的/还真不能少（坑），然后分号;结束
被代理的网址或者服务器返回302重定向的时候，nginx会转给客户端，客户端此时再发一个新的request，直接到被代理的服务器，不走nginx
IP访问控制：我的网站并不是对所有人开放的。注册完会员之后才能访问的。
location ~(.*)\.avi$ {
	auth_basic "closed site";
	auth_basic_user_file users;
}

auth_basic这一项的值可以随便写，auth_basic_user_file的值是一个相对路径，相对于本配置文件，user和密码存在哪里，生成的时候要先安装apache，再通过密码生成命令`htpasswd -c -d /usr/local/users lisz1012` 生成密码
然后把文件/usr/local/users拷贝到auth_basic_user_file users所指定的路径：users那里
location {
	deny IP1;
	allow IP2;
	...
}
同在老爷爷教的那些apache配置

查看当前服务器状态：
location /basic_status {
	stub_status on;
}
在浏览器里输入：http://192.168.1.101/basic_status
就会返回：
Active connections: 1 
server accepts handled requests request_time
 2 2 2 538
Reading: 0 Writing: 1 Waiting: 0 