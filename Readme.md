部署项目的时候一般都打成jar包而不是war包：
项目上右键，Run As， mvn install之后就会在target目录下面出现jar包
java -jar命令或者双击都可以运行他.

这里会有个问题：jar包可以被用户下载
上传文件（图片）有两种存储方法：1.在DB表种存储完整的URL路径。2.存储相对路径（文件名）
bootstrap有默认头像的图片。
数据库里可以只有Username password，这些的查询频率最高，其他的数据可以放到es里（冷数据备份）
account_role, role_permission这种连接表都是手动处理，不用mybatis-generator-gui生成

#### MyBatis的坑：关联的各个表里面不能有相同的列名，否则就要起别名避免冲突, 所以SQL中要起别名，而且在resultMap的column那里也要改，
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
sendfile on也会出问题，有时候要有意关掉，比如网上图片加载一半出不来了，就是因为file传输太快，没来得及去解析，接受的时候出问题了，文件来的特快，接收端解析的程序没跟上，这时候就关掉sendfile。以低性能对低性能
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

~表示要用正则表达式了。auth_basic这一项的值可以随便写，auth_basic_user_file的值是一个相对路径，相对于本配置文件，user和密码存在哪里，生成的时候要先安装apache，
再通过密码生成命令`htpasswd -c -d /usr/local/users lisz1012` 生成密码
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

更加详细的服务器状态
location /status {
	check_status;
}
然后在upstream中加上：
interval=3000 rise=2 fall=5 timeout=1000 type=http;
check_http_send "HEAD / HTTP/1.0\r\n\r\n";
check_http_expect_alive http_2xx http_3xx;
但是2.3.x之后要加编译参数编译才可以用此功能

负载均衡：用 upstream关键字配置 （https://blog.csdn.net/xyang81/article/details/51702900）
upstream tomcats {
    server 192.168.1.101:8080;
    server 192.168.1.102:8080;
}

location / {
	proxy_pass http://tomcats;
}
其实是通过服务器端跳转做负载均
当一个node掉线之后，如果他再上线，此时并不会通知nginx：这台机器恢复在线了
ip_hash;一般不用，会造成网段的访问量倾斜:每个访问的客户端只被分配给特定的服务器服务
upstream tomcats {
	ip_hash;
    server 192.168.1.101:8080;
    server 192.168.1.102:8080;
}
http_proxy 本地磁盘缓存。把后段服务器的结果存到nginx磁盘作为缓存，缺点是不是在内存中，内存中更快
但是可以用linux内核的指令将文件映射到内存

Session共享
PS:jsp里面打印session的ID要这么写：<%=session.getId()%> 不能打分号，略坑，jsp有点忘了

### 结合OA项目做nginx前后端分离
1. 改application-prod.properties中的数据库url链接，使它指向本机的IP（而不再是localhost， application-dev.properties的数据库的IP还指向本机，这就体现出多配置文件的好出来了）
2. 用maven install命令把jar包build好
3. 把build好的jar包传到一台server 上去，例如某台机器的/var/data/jar目录 

#### MySQL 8+ 设置远程登录：
mysql> CREATE USER 'root'@'%' IDENTIFIED BY 'root';
mysql> GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
https://blog.csdn.net/sgrrmswtvt/article/details/82344183

#### Controller 方法的返回值会出问题
Controller 方法的返回值中最前面的“/” 在本地加不加都可以，但在部署到别的机器上的时候一定不能加最前面的“/”
否则会报错

#### 在nginx的location 中配置：
```
location ~ .*\.(css|js|png|gif|jpg|jpeg|bmp|swf|html|htm|ico)$ {
	root /Users/shuzheng/Documents/OA/static;
}
```
用来让nginx拦截静态文件的请求，配合前面的upstream的配置，就可以实现动静分离。但是还要做一步就是把所有的static目录下的静态文件都集中放到nginx服务器上（controller返回的页面此时会有问题，因为他们在template目录）.
动静分离可以极大地减小tomcat服务器的压力（不仅仅是省空间），一旦撑不住，先想这个法子减小压力。商业网站（如腾讯课堂，网易）一个请求过来之后可能要请求几十上百个静态文件，也就是说几十上百个并发就过来了，动态请求其实没多少，
但是算上静态的话，效率会大打折扣

### SSL 保证网络连接安全
当前我们访问网站的时候，网址处会出现“Not Secure”字样。这是因为在网络各个节点和路由器传输的时候会发生“Man in the middle”拦截到所传输的信息。提醒，不要用公共wifi登录一些东西或者做跟银行账户相关的事情，公共wifi其实
就是一个拦截器，会从中拦截packets
SSL解决传输中的数据加密。浏览器只添加一个功能：解密server端传过来的已加密的数据。用一些加密解密算法处理数据。顺便聊一聊加密算法，sha1，sha256，sha512，md5等加密算法是不可逆的，则不能用这些算法，解密不了，所以还要
把算法也传过去，所以还是避免不了中间被拦截，这个不安全，这叫对称式加密。非对称式加密：加密解密算法不一样，加密算法（私钥）不传输，只传输数据和解密算法（公钥）。中间即使被拦截，也无法篡改。这是最开始的https SSL。这种
方式也有毛病，拦截者可以拿到解密算法，揭开数据，修改，然好用自己的加密算法加密，再把数据和自己的解密算法发到目的地，这样就完成了修改。公钥可以下发给浏览器的。公钥依赖于私钥，所以他才能解开私钥加密的信息。只要私钥不丢，
谁都不能模仿服务器加密数据这个动作。私钥就是一段任何人都猜不着的明文随机数，从中取一个值出来生成公钥。为了避免非对称加密仍然被拦截，就产生了一个中间机构叫CA。DNS劫持：浏览器发请求的时候先找DNS，然后拿到IP地址，然后
向着IP发送数据。host文件篡改是常见的劫持手段（遭遇过。。。），网关直接把假的IP返回给浏览器，则伪造的网站就被访问了。CA就是帮助认证是不是要访问的网站，还是个钓鱼网站。CA做什么呢？1.收集企业信息，其中包含主机名域名公钥
2. 对以上企业信息，使用CA的私钥再次加密成证书，同时让浏览器持有CA的公钥（浏览器厂商一般都会让浏览器内置CA的公钥）。3.浏览器拿CA的公钥解密，之后就拿到了企业信息域名和企业的公钥，再拿着企业的公钥解密数据就可以了。中间的拦截者
即使用CA的公钥解密证书，且用企业公钥解密了数据，也无法加密回去继续发送，因为他没有CA的私钥，无法伪造证书。CA可以防劫持，防篡改。安全浏览器最重要的就是CA证书是可被信任的，对于浏览器的信任很重要，证书是由CA机构下发给他的。
CA的私钥完全不能丢，可以投保险，一旦丢了可以索取赔偿。网站要在CA机构认证是要花钱的，CA机构要浏览器信任他也是要付给浏览器钱的。公钥这样就保证了不会被篡改，传输数据的时候，开两个端口：80和443，前者普通传输，后者传输被CA加密的
证书。浏览器要求要有CA公钥。CA会帮我们网站加密证书（签名，只打包公钥，域名和公司信息，不加密数据。一个证书可以被公司旗下多个域名使用），数据有服务器的公钥保证安全，服务器的公钥想不被篡改，是由CA的私钥加密保证安全的，只要CA的
私钥不丢就没问题。数据也不能被篡改，因为中间的这个拦截者没有server的私钥。CA的私钥生成的签名保证的是公钥在传输的过程中不会被篡改

#### 虚拟主机
```
server {
    listen 80;
    server_name taobao.com;
    location / {
            root /Users/shuzheng/Documents/html/taobao.com;
            autoindex on;
    }
}

server {
    listen 80;
    server_name qq.com;
    location / {
            root /Users/shuzheng/Documents/html/qq.com;
            autoindex on;
    }
}
```
然后再在/etc/hosts中配置:
```
192.168.1.102   taobao.com
192.168.1.102   qq.com
```
这样的话访问taobao.com或者qq.com就会访问到192.168.1.102，然后找到/Users/shuzheng/Documents/html/taobao.com或者/Users/shuzheng/Documents/html/qq.com下的index.html 相当于本地的DNS返回一个192.168.1.102
所以请求打到192.168.1.102，然后再看到taobao.com，找到主机名taobao.com，然后找到/对应的目录/Users/shuzheng/Documents/html/taobao.com下的index.html返回

#### OpenSSL自签名
在控制台生成屏显示一个私钥：
``` openssl genrsa``` 或者加参数 ```openssl genrsa -des3 -out ~/Documents/html/server.key 1024```
key是私钥，明文，自己生成的，不能丢；csr是公钥，由私钥生成的；crt是证书 = 公钥 + 签名
由私钥生成公钥：
```openssl req -new -key ~/Documents/html/server.key -out ~/Documents/html/server.csr```
中间要输入密码的话要记住密码，签名或者nginx启动的时候会用到 111111
显示csr里面的内容：
```openssl req -text -in ~/Documents/html/server.csr```
进行签名生成证书：
```openssl x509 -req -days 365 -in ~/Documents/html/server.csr -signkey ~/Documents/html/server.key -out ~/Documents/html/server.crt```
使用证书：
打开nginx.conf文件关于443端口的的注释并修改server_name, ssl_certificate, ssl_certificate_key：
```
server {
    listen       443 ssl;
    server_name  192.168.1.102;

    ssl_certificate      /usr/local/tengine/server.crt;
    ssl_certificate_key  /usr/local/tengine/server.key;

#    ssl_session_cache    shared:SSL:1m;
#    ssl_session_timeout  5m;

#    ssl_ciphers  HIGH:!aNULL:!MD5;
#    ssl_prefer_server_ciphers  on;

#    location / {
#        root   html;
#        index  index.html index.htm;
#    }
}
```
总结干了哪些事儿：先生成私钥，用来标识自己，对公钥进行签名生成证书，证书里包含公钥和签名。有了用私钥签的签名之后，nginx开始工作，它需要私钥和证书，证书需要下发，这就要开一个端口443，还需要单独开一个server出来，他用的是ssl协议。
ssl协议需要4次握手。多个虚拟主机的话，还需要配置多个443端口的server，配置的时候一个证书crt，一个私钥key。保存退出之后关闭，重新重启，不要reload。浏览器通过443端口访问服务器，把把证书拿过来  
此时浏览器还认为是不安全的连接，因为是自己给自己签名的，只需要一个私钥，现在需要伪造一个CA签名，这就用到了xca工具  
证书里面有两部分：1.明文的公钥 2. 用私钥（对应的公钥叫“根证书”）加密后的（公钥hash + 公司的各种信息）。浏览器拿到证书之后，用根证书解密第二部分，验证公钥的hash。如果浏览器上没有根证书，则会出现“不安全站点”提示.  
#### 自己搞一个CA
下载xca-1.4.1.dmg安装并打开，然后file，新建一个database，如certificateDB，然后设置密码，如123456 然后就可以玩儿了  
1.点击private key，新建，输入key的名字，加密方法，长度  
2.点击Certificate，新建，Subject里面填写公司信息，然后选择刚才生成的private key  
3.在生成一个CA的私钥，叫CA_Private_Key
4.点击Certificate，新建，点击上面的“Extensions”, 选择CA
5.再回到subject，填写CA的信息，最后选择CA_Private_Key作为私钥
6.点击Certificate signing requests，新建，新建将要被CA签名的证书。选择Source，在“Template for the new certificate”中选择“HTTPS_server”  
(CA签名先跳过)

### FastDFS
FastDFS 是C语言写的，性能极高