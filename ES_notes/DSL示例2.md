# DSL示例2

GET /product/_search
{
  "query": {
    "match_all": {}
  }
}
GET /product/_mapping
# date字段中搜索2020
GET /product/_search?q=date:2020
# date字段中搜索2020
GET /product/_search?q=2020
GET /product/_search
{
  "query": {
    "match": {
      "date": "2020-06-22"
    }
  }
}

GET /product/_search
{
  "query": {
    "match": {
      "date": "2020-01-01"
    }
  }
}

GET /product/_search
{
  "query": {
    "match": {
      "date": "2020"
    }
  }
}

GET /product/_search
{
  "query": {
    "match": {
      "date.keyword": "2020"
    }
  }
}

GET /product/_search
{
  "query": {
    "match": {
      "name": "xiaomi"
    }
  }
}
#### FIELD.keyword 必须要全部匹配上
GET /product/_search
{
  "query": {
    "match": {
      "name.keyword": "xiaomi"
    }
  }
}

#### FIELD.keyword 必须要全部匹配上
GET /product/_search
{
  "query": {
    "match": {
      "name.keyword": "xiaomi nfc phone"
    }
  }
}

PUT /asin
{
  "mappings": {
    "properties": {
      "registration_date": {
        "type": "date"
      }
    }
  }
}

PUT /product3
{
  "mappings": {
    "properties": {
      "date": {
        "type": "text"
      },
      "desc": {
        "type": "text",
        "analyzer": "english"
      },
      "name": {
        "type": "text",
        "index": false
      },
      "price": {
        "type": "long"
      },
      "tags": {
        "type": "text",
        "index": true
      },
      "parts": {
        "type": "object"
      },
      "partlist": {
        "type": "nested"
      }
    }
  }
}

GET /product3/_mapping

PUT /product3/_doc/1
{
  "name": "xiaomi phone",
  "desc": "shoji zhong de zhandouji",
  "count": 123456,
  "price": 3999,
  "date": "2020-06-24",
  "isdel": false,
  "tags": [
      "xingjiabi",
      "fashao",
      "buka"
   ],
   "parts": {
     "name": "adapter",
     "desc": "5V 2A"
   },
   "partlist": [
     {
       "name": "adapter",
       "desc": "5V 2A"
     },
     {
       "name": "USB-C",
       "desc": "5V 2A 1.5m"
     },
     {
       "name": "erji",
       "desc": "boom"
     }
    ]
}

GET /product3/_search
{
  "query": {
    "match": {
      "name": "xiaomi phone"
    }
  }
}

PUT /my_index
{
  "mappings": {
    "properties": {
      "number_one": {
        "type": "integer"
      },
      "number_two": {
        "type": "integer",
        "coerce": false
      }
    }
  }
}

PUT /my_index/_doc/1
{
  "number_one": "10" 
}

#### 10不能是"10",不支持隐式类型转换
PUT /my_index/_doc/2
{
  "number_two": 10
}

GET /my_index/_search
{
  "query": {
    "match_all": {}
  }
}

PUT /copy_to
{
  "mappings": {
    "properties": {
      "field1": {
        "type": "text",
        "copy_to": "field_all"
      },
      "field2": {
        "type": "text",
        "copy_to": "field_all"
      },
      "field_all": {
        "type": "text"
      }
    }
  }
}

PUT /copy_to/_doc/1
{
  "field1": "xiaomi ",
  "field2": "nfc ",
  "field_all": ""
}

GET /copy_to/_doc/1

#### "field_all" : "" 但是还是会查询得到
GET /copy_to/_search
{
  "query": {
    "match": {
      "field_all": "xiaomi"
    }
  }
}

#### text类型的字段不支持聚合查询 doc_value
#### doc_value设置支不支持正排索引（默认true），index设置支不支持倒排索引. 
#### enable不会创建倒排索引，将来没有需求通过当前字段去检索就可以把这个字段设置为false
#### type字段用来做全文检索，keyword字段用来生成正排索引做聚合分析，因为keyword是一个exact value类型
"desc" : {
  "type" : "text",
  "fields" : {
    "keyword" : {
      "type" : "keyword",
      "ignore_above" : 256
    }
  }
},
####  "size": 0 就只给出聚合结果了，不展示原始数据了
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

#### 用 fielddata 性能低，这就是为什么有keyword这个字段,FIELD.keyword,可以代替fielddata做聚合。
#### 创建索引的时候doc_value为false，但是突然又需要正排索引的时候，可以用 fielddata，它是查询时内存结构，动态的创建正排索引
#### doc_value缓存在os cache中（一般在disk中，内存充足的时候来到系统内存），但是fielddata缓存在jvm内存中，jvm内存很宝贵，
#### 所以fielddata默认是false。类型为text的字段doc_value默认是false，long和dat就是true。不支持doc_value或者支持倒是doc_value
#### 已经被设置为false的做聚合查询，要把fielddata设置为true
#### Fielddata 是延迟加载的。如果你从来没有聚合一个分析字符串，就不会加载 fielddata 到内存中，是在查询时候构建的。
#### fielddata 是基于字段加载的， 只有很活跃地使用字段才会增加fielddata 的负担。
#### fielddata 会加载索引中（针对该特定字段的） 所有的文档，而不管查询是否命中。逻辑是这样：如果查询会访问文档 X、Y 和 Z，那很有可能会在下一个查询中访问其他文档。
#### 如果空间不足，使用最久未使用（LRU）算法移除fielddata。与 doc values 不同，fielddata 构建和管理 100% 在内存中，常驻于 JVM 内存堆。
#### 对于 aggs 来说，如果也使用倒排索引的话就会出现性能低下的问题， 以 1000 条数据来说，如果聚合字段是分词的，那么你至少要遍历整个倒排索引数据才能拿到整个 field 
#### 的全部值， 如果是以正派索引来获取，那么最多遍历 1000 次就能拿到整个 doc 的信息，而往往可能在前面几条就拿到了
#### https://zq99299.github.io/note-book/elasticsearch-senior/aggs/52-doc-value.html
     
PUT /product/_mapping
{
  "properties": {
    "tags": {
      "type": "text",
      "fielddata": false
    }
  }
}

GET /product/_mapping
#### "ignore_above" : 256 字段字符个数多于265就截断

### 在搜索的结果之上进行聚合, 筛选的时候优先用filter，性能原因
GET /product/_search
{
  "query": {
    "bool": {
      "must": [
        {"range": {
          "price": {
            "gt": 1999
          }
        }}
      ]
    }
  }, 
  "aggs": {
    "tag_agg_group": {
      "terms": {
        "field": "tags.keyword"
      }
    }
  },
  "size": 0
}

GET /product/_search
{
  "query": {
    "match_all": {}
  }
}

#### 价格大于1999的手机的评估价格
GET /product/_search
{
  "query": {
    "bool": {
      "must": [
        {"range": {
          "price": {
            "gt": 1999
          }
        }}
      ]
    }
  },
  "aggs": {
    "avg_price": {
      "avg": {
        "field": "price"
      }
    }
  },
  "size": 0
}

#### 每个tag的产品的平均价格
GET /product/_search
{
  "aggs": {
    "avg_gt_1999_per_tag": {
      "terms": {
        "field": "tags.keyword",
        "order": {
          "avg_price": "desc"
        }
      },
      "aggs": {
        "avg_price": {
          "avg": {
            "field": "price"
          }
        }
      }
    }
  },
  "size": 0
}

#### 按照价格区间分别统计总个数
GET /product/_search
{
  "aggs": {
    "tag_agg_group": {
      "range": {
        "field": "price",
        "ranges": [
          {
            "from": 0,
            "to": 1000
          },
          {
            "from": 1001,
            "to": 2000
          },
          {
            "from": 2001,
            "to": 3000
          },
          {
            "from": 3001
          }
        ]
      },
      "aggs": {
        "ranged_avg": {
          "avg": {
            "field": "price"
          }
        }
      }
    }
  },
  "size": 0
}

#### 按照价格区间分别统计总个数
GET /product/_search
{
  "aggs": {
    "tag_agg_group": {
      "range": {
        "field": "price",
        "ranges": [
          {
            "from": 0,
            "to": 1000
          },
          {
            "from": 1001,
            "to": 2000
          },
          {
            "from": 2001,
            "to": 3000
          },
          {
            "from": 3001
          }
        ]
      },
      "aggs": {
        "ranged_avg": {
          "value_count": {
            "field": "price"
          }
        }
      }
    }
  },
  "size": 0
}

#### 按照价格区间分别计算个区间的平均价格
GET /product/_search
{
  "aggs": {
    "tag_agg_group": {
      "range": {
        "field": "price",
        "ranges": [
          {
            "from": 0,
            "to": 1000
          },
          {
            "from": 1001,
            "to": 2000
          },
          {
            "from": 2001,
            "to": 3000
          },
          {
            "from": 3001
          }
        ]
      },
      "aggs": {
        "ranged_avg": {
          "avg": {
            "field": "price"
          }
        }
      }
    }
  },
  "size": 0
}

GET /product/_search
{
  "query": {
    "match": {
      "name": "xiaomi"
    }
  },
  "aggs": {
    "tag_agg_group": {
      "terms": {
        "field": "tags"
      }
    }
  }
}

GET /product/_mapping








