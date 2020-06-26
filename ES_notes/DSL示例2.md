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

#### 用fielddata性能低，这就是为什么有keyword这个字段,FIELD.keyword,可以代替fielddata做聚合
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








