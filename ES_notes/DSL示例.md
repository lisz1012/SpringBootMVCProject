```
GET /product/_search
GET /product/_search?timeout=0ms
GET /product/_search?q=name:xiaomi
GET /product/_search?from=0&size=2&sort=price:asc
```
```
GET /product/_search
{
  "query": {
    "match_all": {}
  }
}

GET /product/_search
{
  "query": {
    "match": {
      "name": "nfc"
    }
  }
}
```
# name中包含的，且按照价格的降序排列
```
GET /product/_search
{
  "query": {
    "match": {
      "name": "nfc"
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
```
# name或desc中包含nfc的(多个字段中包含同一个关键词)，且按照价格的降序排列
```
GET /product/_search
{
  "query": {
    "multi_match": {
      "query": "nfc",
      "fields": ["name", "desc"]
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
      "price": "4999"
    }
  }
}
```
# select name, price from product; _source这里规定了要哪些字段
```
GET /product/_search
{
  "query": {
    "match_all": {}
  },
  "_source": ["name","price"]
}
```
# select name, price from product where name like %nfc% order by price;
```
GET /product/_search
{
  "query": {
    "match": {
      "name": "nfc"
    }
  },
  "_source": ["name", "price"],
  "sort": [
    {
      "price": {
        "order": "asc"
      }
    }
  ]
}
```
# 分页
```
GET /product/_search
{
  "query": {
    "match_all": {}
  },
  "sort": [
    {
      "price": {
        "order": "asc"
      }
    }
  ], 
  "from": 0,
  "size": 2
}
```
# 全文检索 去倒排索引的Map<String, List<Intger>>中找key "nfc phone"，找不到.搜索前不会再对搜索词进行分词拆解。
```
GET /product/_search
{
  "query": {
    "term": {
      "name": "nfc phone"
    }
  }
}
```
# bool: 组合查询，must：必须符合的条件:name必须包含nfc，name必须包含phone
```
GET /product/_search
{
  "query": {
    "bool": {
      "must": [
        {"term": {
          "name": {
            "value": "nfc"
          }
        }},
        {"term": {
          "name": {
            "value": "phone"
          }
        }}
      ]
    }
  }
}
```
# name中包含nfc的、包含phone的全都要
# select * from product where name like "%nfc%" or name like "%phone%"
```
GET /product/_search
{
  "query": {
    "terms": {
      "name": [
        "nfc",
        "phone"
      ]
    }
  }
}
```
# 现在match被分词了
```
GET /product/_search
{
  "query": {
    "match": {
      "name": "nfc phone" 
    }
  }
}

GET /product/_search
{
  "query": {
    "match": {
      "name": "xiaomi nfc zhineng phone"
    }
  }
}
```
# 查看分词器
```
GET /_analyze
{
  "analyzer": "standard",
  "text": "xiaomi nfc zhineng phone"
}
```
# 短语搜索，不分词，精确匹配：https://www.jianshu.com/p/d5583dff4157 term必须短语搜索搜得到，且关键字长度相等才可以。term：关键字是实际数据的充要条件，match_phrase：关键字是实际数据的必要不充分条件
```
GET /product/_search
{
  "query": {
    "match_phrase": {
      "name": "nfc phone"
    }
  }
}
```

#query and filter
```
GET /product/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "name": "xiaomi"
          }
        },
        {"match": {
          "desc": "shouji"
        }}
      ],
      "filter": [
        {"match_phrase": {"name": "xiaomi phone"}},
        {"range": {
          "price": {
            "gt": 10000,
            "lt": 20000
          }
        }}
      ]
    }
  }
}
```
# filter默认不计算相关度，所以性能要比must、match要高，而且filter支持缓存。先进行filter，然后再去进行must和match这样就更省时间
```
GET /product/_search
{
  "query": {
    "bool": {
      "filter": [
        {"match_phrase": {"name": "xiaomi phone"}},
        {"range": {
          "price": {
            "gt": 10000,
            "lte": 20000
          }
        }}
      ]
    }
  }
}

GET /product/_search
{
  "query": {
    "bool": {
      "must_not": [
        {"match": {
          "name": "hongmi"
        }},
        {"match": {
          "desc": "zhandouji"
        }}
      ]
    }
  }
}
```

# should 类似或者 or 
```
GET /product/_search
{
  "query": {
    "bool": {
      "must": [
        {"match": {
          "name": "xiaomi"
        }}
      ], 
      "must_not": [
        {"match": {
          "name": "erji"
        }}
      ], 
      "should": [
        {"match": {
          "name": "phone"
        }},
        {"match": {
          "desc": "nfc"
        }}
      ]
    }
  }
}
```

# should 类似或者 or, minimum_should_match设置了should中最少满足几个才会被选出来，如果这个指定的数字等于should里面的条目数，则这个should相当于一个must。
```
GET /product/_search
{
  "query": {
    "bool": {
      "must": [
        {"match": {
          "name": "xiaomi"
        }}
      ], 
      "must_not": [
        {"match": {
          "name": "erji"
        }}
      ], 
      "should": [
        {"match": {
          "name": "phone"
        }},
        {"match": {
          "desc": "nfc"
        }}
      ],
      "minimum_should_match": 2
    }
  }
}
```
# 只有should自己，默认minimum_should_match就是1
```
GET /product/_search
{
  "query": {
    "bool": {
      "should": [
        {"range": {
          "price": {
            "gt": 10000
          }
        }},
        {"range": {
          "price": {
            "gt": 20000
          }
        }}
      ]
    }
  }
}
```

# 有must有should， minimum_should_match的默认值是0，也就是说should实在匹配不上，must匹配上的也可以选出来
```
GET /product/_search
{
  "query": {
    "bool": {
      "must": [
        {"match": {
          "name": "xiaomi"
        }}
      ], 
      "must_not": [
        {"match": {
          "name": "erji"
        }}
      ], 
      "should": [
        {"match": {
          "name": "a"
        }},
        {"match": {
          "desc": "a"
        }}
      ]
    }
  }
}

GET /product/_search
{
  "query": {
    "bool": {
      "should": [
        {"range": {
          "price": {
            "gt": 10000
          }
        }},
        {"range": {
          "price": {
            "gt": 20000
          }
        }}
      ]
    }
  }
}
```
# filter
# 没有任何排序
```
GET /product/_search
{
  "query": {
    "constant_score": {
      "filter": {"range": {
        "price": {
          "gte": 99,
          "lte": 113999
        }
      }}
    }
  }
}

GET /product/_search
{
  "query": {
    "bool": {
      "should": [
        {"match": {
          "name": "nfc"
        }},
        {"match": {
          "name": "xiaomi"
        }}
      ],
      "must_not": [
        {"match": {
          "name": "erji"
        }}
      ]
    }
  }
}
```
# 跟上一条查询有何区别吗？score不一样
```
GET /product/_search
{
  "query": {
    "constant_score": {
      "filter": {
        "bool": {
          "should": [
              {"term": {"name": "xiaomi"}},
              {"term": {"name": "nfc"}}
            ],
            "must_not": [
              {"term": {"name": "erji"}}
              ]
        }
      },
      "boost": 1.2
    }
  }
}

# name中有xiaomi nfc phone，或者价钱在2999以下的手机
GET /product/_search
{
  "query": {
    "constant_score": {
      "filter": {
        "bool": {
          "should": [
              {"match_phrase": {"name": "xiaomi nfc phone"}},
              {
               "bool": {
                 "must":[
                    {"term": {"name": "phone"}},
                    {"range": {
                        "price": {
                          "lte": 2999
                        }
                    }}
                   ]
               } 
              }
            ]
        }
      },
      "boost": 1.2
    }
  }
}

GET /product/_search
{
  "query": {
    "bool": {
      "filter": [
        {"bool": {
          "should": [
              {
                "range": 
                {
                  "price": {
                    "gte": 11999
                }}
              },
              {
                "range":
                {
                  "price": {
                    "gte": 13999
                  }
                }
              }
            ],
            "must": [{
              "match": {"name": "nfc"}
            }],
            "minimum_should_match": 1
        }}
      ]
    }
  }
}
```
# 高亮查询
```
GET /product/_search
{
  "query": {
    "match_phrase": {
      "name": "nfc phone"
    }
  },
  "highlight": {
    "fields": {"name": {}}
  }
}
```
# 解决Deep paging问题
```
GET /product/_search?scroll=1m
{
  "query": {
    "match_all": {}
  },
  "sort": [
    {
      "price": {
        "order": "desc"
      }
    }
  ],
  "size": 1
}
```
# 接着上一次返回这个ID的查询的页数继续查，size也是上一次的，最后也会返回这个ID这个"scroll": "1m" 必须要写，顺延scroll的过期时间
```
GET /_search/scroll
{
  "scroll": "1m",
  "scroll_id" : "FGluY2x1ZGVfY29udGV4dF91dWlkDXF1ZXJ5QW5kRmV0Y2gBFDJMakExWElCbGxROFhKZEFnUU1XAAAAAAAAAS8WcW5oZjFpLTRTLU9xWkhMNDNqSE9IQQ=="
}
```
# 类似于desc TABLE
```
GET /product/_mappings
```

```
POST /product/_update/1
{
  "doc": {
    "desc": "shouji zhong de zhandouji"
  }
}

GET /product/_doc/1
```