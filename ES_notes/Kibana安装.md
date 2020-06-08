# Kibana 安装

1. 到这里下载： https://www.elastic.co/kibana， 
    或者直接在/usr/local下执行： `wget https://artifacts.elastic.co/downloads/kibana/kibana-7.7.1-linux-x86_64.tar.gz`
    注意：Kibana版本要跟ES版本一致  
2. 在/etc/profile中加入kibana的路径到环境变量PATH  
3. 配置kibana.yml，修改配置文件中的server.host、elasticsearch.hosts为真实IP，如: "192.168.1.3" 和 "http://192.168.1.3:9200"  
4. 启动kibana：`kibana` 或者 `kibana --allow-root`  
5. 到web UI上去验证，例如：`192.168.1.3:5601`  


**参考文档**  
https://www.elastic.co/guide/cn/kibana/current/targz.html  
https://soyuone.github.io/2020/01/06/elasticsearch-setup-linux/  