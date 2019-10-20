### 简介 （高手请跳过）
- 1. Nginx 做负载均衡（必要时配合lvs），可以定制简单的负载均衡策略，平衡各节点流量。其反向代理功能可以用来开放442端口实现SSL非对称加密，实现https协议  
	 Nginx 做动静分离：Nginx对静态文件的处理效率比tomcat高的不止一倍两倍，解决最前端的并发量的问题，前端让nginx多负载。将静态的html，css，js等文件部署到Nginx, 减轻后端service的负载，提升性能
- 2. FastDFS 作文件存储，其存储功能类似hadoop的HDFS，但是不承担分布式计算.FastDFS在各种DFS中也算是用的比较多比较稳定的了（关键是他是国人写的，余庆）

### 准备工作
- 1. 先安装Nginx（参见：https://www.cnblogs.com/eaglezb/p/6073661.html
- 2. 再按照FastDFS.md（https://github.com/lisz1012/SpringBootMVCProject/blob/master/FastDFS.md）中的方法安装好FastDFS  
     这里列出本次安装了各个文件和路径的配置：
     tracker IP: 192.168.1.120 Port:22122 (默认)  
     tracker.conf:  
     ```base_path=/var/data/fastdfs-tracker```  
     storage.conf:
     ```base_path=/var/data/fastdfs-storage/base```（<---PS：启动报错的时候log也存在次目录下的logs自目录中）
     ```store_path0=/var/data/fastdfs-storage/data```  
	client.conf:  
	```base_path=/usr/local/fastdfs/client```  
	nginx安装路径：
	```/usr/local/nginx```  
	nginx源码解压路径：  
	```/usr/local/nginx-1.16.1``` 
	FastDFS安装路径：  
	```/usr/local/fastdfs```  
	
	
### Nginx集成FastDFS
这里先将Nginx，FastDFS tracker 和 FastDFS storage安装在同一台主机上。  
- 1. 下载 fastdfs-nginx-module：https://github.com/happyfish100/fastdfs-nginx-module  
- 2. 将下载好的fastdfs-nginx-module目录放在/usr/local/fastdfs下
- 3. 修改/usr/local/fastdfs/fastdfs-nginx-module/src/config文件，修改如下：
	 ```ngx_module_incs="/usr/include/fastdfs /usr/include/fastcommon/"
	 ... ... 
		CORE_INCS="$CORE_INCS /usr/include/fastdfs /usr/include/fastcommon/"```
- 4. 复制 fastdfs-nginx-module 源码中的配置文件到/etc/fdfs 目录， 并修改，命令：
     进入fastdfs-nginx-module/src目录下，复制mod_fastdfs.conf文件到 /etc/fdfs目录，进入/etc/fdfs目录，修改mod_fastdfs.conf配置文件
	```
	cd fastdfs-nginx-module/src
	cp mod_fastdfs.conf /etc/fdfs
	cd /etc/fdfs
	vim mod_fastdfs.conf
	```
修改如下三处：
```tracker_server=192.168.1.120:22122``` # tracker服务IP和端口
```url_have_group_name=true``` # 访问链接前缀加上组名
```store_path0=/var/data/fastdfs-storage/data``` # 文件存储路径	

https://zhuanlan.zhihu.com/p/29157952