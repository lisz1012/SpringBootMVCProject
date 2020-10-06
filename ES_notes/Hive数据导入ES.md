# Hive数据导入ES

1. 在要执行命令的hive机器上下载3个Jar包：
```
elasticsearch-hadoop-7.7.1.jar
httpclient-4.5.12.jar
org.apache.commons.httpclient.jar
```

2. 把三个Jar包加入进来：
```
add jar /LOCAL/PATH/elasticsearch-hadoop-7.7.1.jar
add jar /LOCAL/PATH/httpclient-4.5.12.jar
add jar /LOCAL/PATH/org.apache.commons.httpclient.jar
```
也可以用jar包在HDFS或者S3的路径:
```
add jar hdfs://nameservice1/lib/commons-httpclient-3.1.jar;
```

3. 先建立一个基础表，真实数据在这里：
```
create table person1
(
    id int,
    name string
)
row format delimited
fields terminated by ',';
```
4. 并往里面load数据：  
`load data local inpath '/root/data/data' into table person1;`  
建立与ES的联系表
```
create external table person (
    id int,
    name string
    )
    stored by 'org.elasticsearch.hadoop.hive.EsStorageHandler'
    TBLPROPERTIES('es.resource' = 'person',
    'es.index.auto.create' = 'false',
    'es.nodes' = '192.168.1.3',
    'es.port'='9200'
);
```

5. 往联系表中写入数据就可以同步到ES：  
`insert overwrite table person select * from person1;`  
注意，这里虽然是"overwrite", 但是其实是每次都追加,overwrite似乎不起作用.想要每次都是新数据，都要把联系表删掉重新建立，
在hive命令行可以删，在ES删除也有相同的效果, 也就是说，联系表的同步是双向的

6. Kibana上查看数据  
http://192.168.1.3:5601/app/kibana#/dev_tools/console
```
GET /person/_search
{
  "query": {
    "match_all": {}
  },
  "size": 1000
}
```