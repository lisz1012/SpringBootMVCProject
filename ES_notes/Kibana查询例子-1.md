```
GET /_cat/indices?v
PUT /test?pretty
PUT /product/_doc/1
{
    "name" : "xiaomi phone",
    "desc" :  "小米手机",
    "price" :  3999,
    "tags": [ "xingjiabi", "fashao", "buka" ]
}
PUT /product/_doc/2
{
    "name" : "xiaomi nfc phone",
    "desc" :  "zhichi quangongneng nfc,shouji zhong de jianjiji",
    "price" :  4999,
    "tags": [ "xingjiabi", "fashao", "gongjiaoka" ]
}


PUT /product/_doc/3
{
    "name" : "nfc phone",
    "desc" :  "shouji zhong de hongzhaji",
    "price" :  2999,
    "tags": [ "xingjiabi", "fashao", "menjinka" ]
}

PUT /product/_doc/4
{
    "name" : "xiaomi erji",
    "desc" :  "erji zhong de huangmenji",
    "price" :  999,
    "tags": [ "low", "bufangshui", "yinzhicha" ]
}

PUT /product/_doc/5
{
    "name" : "hongmi erji",
    "desc" :  "erji zhong de kendeji",
    "price" :  399,
    "tags": [ "lowbee", "xuhangduan", "zhiliangx" ]
}
GET /product/_doc/1
POST /product/_doc/1/_update
{
  "doc" : {
    "desc" :  "shouji zhong de zhandouji"
  }
}
GET /product
GET /product/_search
GET /product/_search?sort=price
GET /product/_search?sort=price:desc
GET /product/_search?q=price:3999
GET /product/_search
{
  "query": {
    "match": {
      "name": "xiaomi"
    }
  },
  "sort": [
    {
      "price": {
        "order": "desc"
      }
    }
  ]
}
GET /product/_search
{
  "query": {
    "match": {
      "name": "phone xiaomi"
    }
  },
  "sort": [
    {
      "price": {
        "order": "desc"
      }
    }
  ]
}
DELETE /product/_doc/1
```