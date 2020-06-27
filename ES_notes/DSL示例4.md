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
```