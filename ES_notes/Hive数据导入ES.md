# Hive数据导入ES

先建立一个基础表，真实数据在这里：
```
create table person1
(
    id int,
    name string
)
row format delimited
fields terminated by ',';
```
并往里面load数据：`load data local inpath '/root/data/data' into table person1;`  
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
往联系表中写入数据就可以同步到ES：  
`insert overwrite table person select * from person1;`  
注意，这里虽然是"overwrite", 但是其实是每次都追加