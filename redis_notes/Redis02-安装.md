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

##### 2.3.安装

```
make
make install PREDIX=/opt/redis
```

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



