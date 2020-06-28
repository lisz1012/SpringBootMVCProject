GET /product2/_search
```
#### ES内部执行，没有了网络请求和并发，计算向数据移动
POST /product2/_update/4
{
  "script": {
    "source": "ctx._source.price-=1"
  }
}
GET /product2/_doc/4

#### 只有一条操作，简写
POST /product2/_update/4
{
  "script": "ctx._source.price-=1"
}

#### tag无线充电
POST /product2/_update/3
{
  "script": {
    "lang": "painless",
    "source": "ctx._source.tags.add('无线充电')"
  }
}

#### 传参
POST /product2/_update/3
{
  "script": {
    "lang": "painless",
    "source": "ctx._source.tags.add(params.tag_name)",
    "params": {
      "tag_name": "无线充电"
    }
  }
}

GET /product2/_doc/3

GET /product2/_search
{
  "query": {
    "match_all": {}
  }
}

POST product2/_update/111
{
  "script": {
    "lang": "painless",
    "source": "ctx.op='delete'"
  }
}

#### ID=6此时并不存在
POST /product2/_update/4
{
  "script": {
    "lang": "painless",
    "source": "ctx._source.tags.add(params.tag_name)",
    "params": {
      "tag_name": "超长待机"
    }
  },
  "upsert": {
    "name": "小米10",
    "price": 19999
  }
}

GET /product2/_doc/4

# Painless之外还有expression，支持的范围小，但某些场景下性能好。还有mustache和java
GET /product2/_search
{
  "script_fields": {
    "test_field": {
      "script": {
        "lang": "expression",
        "source": "doc['price']*0.8"
      }
    }
  }
}

GET /product2/_search
{
  "query": {
    "match_all": {}
  }
}


GET product2/_search
{
  "script_fields": {
    "test_field": {
      "script": {
        "lang": "expression",
        "source": "doc['price']*0.8"
      }
    }
  }
}

#编译每分钟只有15次。 脚本部分新版本好像有bug，下面3个查询都不能够成功
GET /product2/_search
{
  "script_fields": {
    "test_field": {
      "script": {
        "lang": "painless",
        "source": "doc['price'].value*params.discount",
        "params": {
          "discount": 0.8
        }
      }
    }
  }
}

GET /product2/_search
{
  "script_fields": {
    "price": {
      "script": {
        "lang": "expression",
        "source": "doc['price'].value"
      }
    },
    "test_field": {
      "script": {
        "source": "doc.price*discount_9",
        "params": {
          "discount_9": 0.9
        }
      }
    }
  }
}

GET /product2/_search
{
  "script_fields": {
    "price": {
      "script": {
        "lang": "expression",
        "source": "doc['price'].value"
      }
    },
    "test_field": {
      "script": {
        "lang": "painless",
        "source": "[doc.price*discount_9,doc.price*discount_8]",
        "params": {
          "discount_9": 0.9,
          "discount_8": 0.8
        }
      }
    }
  }
}

GET /product/_search
{
  "query": {
    "match_all": {}
  }
}

#Dates
GET /product/_search
{
  "script_fields": {
    "test_year": {
      "script": {
        "source": "doc.date.value.year"
      }
    }
  }
}

GET /product/_search
{
  "script_fields": {
    "test_year": {
      "script": {
        "source": "doc.date.value.month"
      }
    }
  }
}

GET /product/_search
{
  "script_fields": {
    "test_year": {
      "script": {
        "source": "doc.date.value.dayOfWeek"
      }
    }
  }
}

GET /product/_search
{
  "script_fields": {
    "test_year": {
      "script": {
        "source": "doc.date.value.getDayOfWeekEnum().getValue()"
      }
    }
  }
}

GET /product/_search
{
  "script_fields": {
    "test_day_of_month": {
      "script": {
        "source": "doc.date.value.dayOfMonth"
      }
    }
  }
}

GET /product/_search
{
  "script_fields": {
    "test_hour": {
      "script": {
        "source": "doc.date.value.hour"
      }
    }
  }
}

GET /product/_search
{
  "script_fields": {
    "test_millis": {
      "script": {
        "source": "doc.date.value.toInstant().toEpochMilli()"
      }
    }
  }
}

POST /product2/_update/2
{
  "script": {
    "lang": "painless",
    "source": """
      ctx._source.name += params.name;
      ctx._source.price += params.discount;
    """,
    "params": {
      "name": "test",
      "discount": 1
    }
  }
}

GET /product2/_doc/2

##注意：所有节点的elasticsearch.yml文件中必须加上：script.painless.regex.enabled: true https://discuss.elastic.co/t/script-painless-regex-enabled-true-but-es-log-says-it-is-not/145566
POST /product2/_update/2
{
  "script": {
    "lang": "painless",
    "source": """
      if (ctx._source.name =~ /[\s\S]*phone[\s\S]*/) {
        ctx._source.name = "- match";
      } else {
        ctx.op="noop";
      }
    """
  }
}

POST /_bulk
{"update": {"_index": "product2","_id": "2","retry_on_conflict":3}}
{"doc":{"date":"2020-06-27"}}

POST /product2/_update/2
{
  "script": {
    "lang": "painless",
    "source": """
      if (ctx._source.date =~ /.*06.*/) {
        ctx._source.name = "xioami nfc phone";
      } else {
        ctx.op="noop";
      }
    """
  }
}

POST /product2/_update/2
{
  "script": {
    "lang": "painless",
    "source": """
      if (ctx._source.date ==~ /2020-\d\d-\d\d/) {
        ctx._source.name += "- match"
      }
    """
  }
}

GET /product2/_search

GET /product2/_search
{
  "query": {
    "bool": {
      "filter": [
        {"range": {
          "price": {
            "lt": 1000
          }
        }}
      ]
    }
  },
  "aggs": {
    "test_sum": {
      "sum": {
        "script": {
          "lang": "painless",
          "source": """
            int total = 0;
            for (int i=0; i<doc['tags'].length; i++) {
              total ++;
            }
            return total;
          """
        }
      }
    }
  }
}

# 用params['_source']['price']，"lang": "painless"可以不写
GET /product2/_search
{
  "aggs": {
    "test": {
      "sum": {
        "script": {
          "lang": "painless",
          "source": """
            int total = 0;
            if (params['_source']['price'] != null && params['_source']['price'] < 1000) {
              total ++;
            }
            return total;
          """
        }
      }
    }
  }
}

#### 写doc就不行。doc会把[]中的数据加载到内存里，查询效率更高，而且只支持简单类型
GET /product2/_search
{
  "aggs": {
    "test": {
      "sum": {
        "script": {
          "lang": "painless",
          "source": """
            int total = 0;
            if(doc['price'] != null && doc['price'] < 1000) {
              total ++;
            }
            return total;
          """
        }
      }
    }
  }
}




GET /product2/_search
{
  "query": {
    "bool": {
      "filter": [
        {
          "range": {
            "price": {
              "lte": 1000
            }
          }
        }
      ]
    }
  }
}

POST /tvs/_bulk
{ "index": {}}
{ "price" : 1000, "color" : "红色", "brand" : "长虹", "sold_date" : "2016-10-28" }
{ "index": {}}
{ "price" : 2000, "color" : "红色", "brand" : "长虹", "sold_date" : "2016-11-05" }
{ "index": {}}
{ "price" : 3000, "color" : "绿色", "brand" : "小米", "sold_date" : "2017-05-18" }
{ "index": {}}
{ "price" : 1500, "color" : "蓝色", "brand" : "TCL", "sold_date" : "2017-07-02" }
{ "index": {}}
{ "price" : 1200, "color" : "绿色", "brand" : "TCL", "sold_date" : "2018-08-19" }
{ "index": {}}
{ "price" : 2000, "color" : "红色", "brand" : "长虹", "sold_date" : "2017-11-05" }
{ "index": {}}
{ "price" : 8000, "color" : "红色", "brand" : "三星", "sold_date" : "2017-01-01" }
{ "index": {}}
{ "price" : 2500, "color" : "蓝色", "brand" : "小米", "sold_date" : "2018-02-12" }


GET /tvs/_search
{
  "size": 0,
  "aggs": {
    "group_color": {
      "terms": {
        "field": "color.keyword"
      }
    }
  }
}

GET /product2/_search
{
  "aggs": {
    "test": {
      "terms": {
        "field": "price"
      }
    }
  }
}
```