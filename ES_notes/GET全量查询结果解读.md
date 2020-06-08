# GET /product/_search 全量查询结果解读

在Kibana dev tool里面执行`GET /product/_search`或者在命令行执行：`curl http://192.168.1.3:9200/product/_search`  
会输出执行结果：
```
{
  "took" : 350,
  "timed_out" : false,
  "_shards" : {
    "total" : 1,
    "successful" : 1,
    "skipped" : 0,
    "failed" : 0
  },
  "hits" : {
    "total" : {
      "value" : 5,
      "relation" : "eq"
    },
    "max_score" : 1.0,
    "hits" : [
      {
        "_index" : "product",
        "_type" : "_doc",
        "_id" : "2",
        "_score" : 1.0,
        "_source" : {
          "name" : "xiaomi nfc phone",
          "desc" : "zhichi quangongneng nfc,shouji zhong de jianjiji",
          "price" : 4999,
          "tags" : [
            "xingjiabi",
            "fashao",
            "gongjiaoka"
          ]
        }
      },
      {
        "_index" : "product",
        "_type" : "_doc",
        "_id" : "3",
        "_score" : 1.0,
        "_source" : {
          "name" : "nfc phone",
          "desc" : "shouji zhong de hongzhaji",
          "price" : 2999,
          "tags" : [
            "xingjiabi",
            "fashao",
            "menjinka"
          ]
        }
      },
      {
        "_index" : "product",
        "_type" : "_doc",
        "_id" : "4",
        "_score" : 1.0,
        "_source" : {
          "name" : "xiaomi erji",
          "desc" : "erji zhong de huangmenji",
          "price" : 999,
          "tags" : [
            "low",
            "bufangshui",
            "yinzhicha"
          ]
        }
      },
      {
        "_index" : "product",
        "_type" : "_doc",
        "_id" : "5",
        "_score" : 1.0,
        "_source" : {
          "name" : "hongmi erji",
          "desc" : "erji zhong de kendeji",
          "price" : 399,
          "tags" : [
            "lowbee",
            "xuhangduan",
            "zhiliangx"
          ]
        }
      },
      {
        "_index" : "product",
        "_type" : "_doc",
        "_id" : "1",
        "_score" : 1.0,
        "_source" : {
          "name" : "xiaomi phone",
          "desc" : "小米手机",
          "price" : 3999,
          "tags" : [
            "xingjiabi",
            "fashao",
            "buka"
          ]
        }
      }
    ]
  }
}
```
其中，took是指这次查询花费了多少ms，timeout表示有没有超时，hits表示有多少条记录被选出来