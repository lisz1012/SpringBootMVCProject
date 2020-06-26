```
GET /product/_search
{
  "aggs": {
    "tag_agg_group": {
      "terms": {
        "field": "tags"
      }
    }
  },
  "size": 0
}

#### 批量查询
GET /_mget
{
  "docs": [
    {
      "_index": "product",
      "_id": 1
    },
    {
      "_index": "product2",
      "_id": 2
    }
  ]
}

GET /product/_mget
{
  "docs": [
    {
      "_id": 1
    },
    {
      "_id": 2
    }
  ]
}

GET /product/_mget
{
  "ids": [1,2]
}

GET /product/_mget
{
  "docs": [
    {
      "_id": 1,
      "_source": false
    },
    {
      "_id": 2,
      "_source": [
        "name", "price"  
      ]
    }
  ]
}

GET /product/_mget
{
  "docs": [
    {
      "_id": 2,
      "_source": {
        "include": ["name"]
      }
    }  
  ]
}

GET /product/_mget
{
  "docs": [
    {
      "_id": 2,
      "_source": {
        "exclude": ["name"]
      }
    }  
  ]
}

GET /product2/_search
#### 从关系型数据库中同步过来批量插入ES的情况下需要指定ID
PUT /product2/_create/12
{
    "name" : "xiaomi phone",
    "desc" :  "shouji zhong de zhandouji",
    "price" :  3999,
    "tags": [ "xingjiabi", "fashao", "buka" ]
}

#### 自动随机生成一个id。
POST /product2/_doc
{
    "name" : "xiaomi phone",
    "desc" :  "shouji zhong de zhandouji",
    "price" :  3999,
    "tags": [ "xingjiabi", "fashao", "buka" ]
}

#### versoin更迭不会立即删除老的版本，而是积累到一定version值之后之前的版本才会删除，和并发控制相关

#### 展开的话性能低，会在内存中生成对象，省内存.create的ID已存在则会报错
POST /_bulk
{"create": {"_index": "product2","_id": "1"}}
{"name":"_bulk create1"}
{"create": {"_index": "product2","_id": "11"}}
{"name":"_bulk create11"}
{"delete": {"_index": "product2","_id": "11"}}

PUT /product4/_doc/11
{
    "name" : "xiaomi phone",
    "desc" :  "shouji zhong de zhandouji",
    "price" :  3999,
    "tags": [ "xingjiabi", "fashao", "buka" ]
}

GET /product2/_doc/15
#### update是部分更改
POST /_bulk
{"update": {"_index": "product4","_id": "11","retry_on_conflict":3}}
{"doc":{"name":"_bulk name"}}
#### index是创建或者全量更改
POST /_bulk
{"index": {"_index": "product2","_id": "1"}}
{"doc":{"name":"_bulk name1"}}
{"index": {"_index": "product2","_id": "15"}}
{"doc":{"name":"_bulk name15"}}
#### 只返回失败的结果
POST /_bulk?filter_path=items.*.error
{"create": {"_index": "product2","_id": "1"}}
{"name":"_bulk create1"}
{"create": {"_index": "product2","_id": "111"}}
{"name":"_bulk create111"}

#### ES通过乐观锁的机制控制并发中的冲突
#### 用老式的versoin来验证乐观锁版本号冲突的问题
GET /product4/_search
PUT /product4/_doc/11?version=9&version_type=external
{
    "name" : "xiaomi phone",
    "desc" :  "shouji zhong de zhandouji",
    "price" :  6999,
    "tags": [ "xingjiabi", "fashao", "buka" ]
}
```