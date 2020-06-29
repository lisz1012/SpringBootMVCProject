```
PUT /my_index
{
  "settings": {
    "analysis": {
      "char_filter": {
        "my_char_filter": {
          "type": "html_strip"
        }
      },
      "analyzer": {
        "my_analizer": {
          "tokenizer": "keyword",
          "char_filter": "my_char_filter"
        }
      }
    }
  }
}

GET /my_index/_analyze
{
  "analyzer": "my_analizer",
  "text": "lisz <a><b>hahaha</b></a."
}

PUT /my_index2
{
  "settings": {
    "analysis": {
      "char_filter": {
        "my_char_filter": {
          "type": "html_strip",
          "escaped_tags": ["a"]
        }
      },
      "analyzer": {
        "my_analizer": {
          "tokenizer": "keyword",
          "char_filter": "my_char_filter"
        }
      }
    }
  }
}

GET /my_index2/_analyze
{
  "analyzer": "my_analizer",
  "text": "lisz <a><b>hahaha</b></a>."
}

PUT /my_index3
{
  "settings": {
    "analysis": {
      "char_filter": {
        "my_char_filter": {
          "type": "mapping",
          "mappings": [
            "零 => 0",
            "一 => 1",
            "二 => 2",
            "三 => 3",
            "四 => 4",
            "五 => 5",
            "六 => 6",
            "七 => 7",
            "八 => 8",
            "九 => 9"
          ]
        }
      },
      "analyzer": {
        "my_analizer": {
          "tokenizer": "keyword",
          "char_filter": "my_char_filter"
        }
      }
    }
  }
}

GET /my_index3/_analyze
{
  "analyzer": "my_analizer",
  "text": "一二 dsfd  三第 wfwef 五 八 九 --十"
}
# 写replace不太对
PUT /my_index4
{
  "settings": {
    "analysis": {
      "char_filter": {
        "my_char_filter": {
          "type": "pattern_replace",
          "pattern": "(\\d)-(?=\\d)",
          "replace": "$1_"
        }
      },
      "analyzer": {
        "my_analizer": {
          "tokenizer": "keyword",
          "char_filter": "my_char_filter"
        }
      }
    }
  }
}

GET /my_index4/_analyze
{
  "analyzer": "my_analizer",
  "text": "123-456-789"
}
# 应该写replacement
PUT /my_index5
{
  "settings": {
    "analysis": {
      "char_filter": {
        "my_char_filter": {
          "type": "pattern_replace",
          "pattern": "(\\d)-(?=\\d)",
          "replacement": "$1_"
        }
      },
      "analyzer": {
        "my_analizer": {
          "tokenizer": "keyword",
          "char_filter": "my_char_filter"
        }
      }
    }
  }
}

GET /my_index5/_analyze
{
  "analyzer": "my_analizer",
  "text": "123-456-789"
}

GET /_analyze
{
  "tokenizer": "standard",
  "filter": ["lowercase"],
  "text": "LI SHUZHENG"
}
#默认分词器对中文支持得不好，一个字一个字的分开
GET /_analyze
{
  "tokenizer": "standard",
  "text": "江山如此多娇"
}

PUT /my_index6
{
  "settings": {
    "analysis": {
      "char_filter": {
        "my_char_filter": {
          "type": "mapping",
          "mappings": "& => and"
        }
      },
      "filter": {
        "test_filter": {
          "type": "stop",
          "stopword": ["is","a","at","the"]
        }
      },
      "analyzer": {
        "my_analizer": {
          "type": "custom",
          "char_filter": [
            "my_char_filter",
            "html_strip"
          ],
          "filter": ["lowercase","test_filter"],
          "tokenizer": "standard"
        }
      }
    }
  }
}
# stop停用词不会出现在下面执行的结果中，分词的时候不对他们进行操作
GET /my_index6/_analyze
{
  "analyzer": "my_analizer",
  "text": "Teacher ma & is a also at <a>think</a> is <mother's friends> is good"
}

#安装中文分词器：在/usr/share/elasticsearch/bin下面执行：./elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.7.1/elasticsearch-analysis-ik-7.7.1.zip
GET _analyze
{
  "analyzer": "ik_max_word",
  "text": ["明月几时有","当时明月在"]
}

GET _analyze
{
  "tokenizer": "ik_max_word",
  "text": ["明月几时有","当时明月在"]
}

# ik_max_word相对准一些，analyzer和tokenizer好像可以通用
GET _analyze
{
  "tokenizer": "ik_max_word",
  "text": "我爱北京天安门"
}
GET _analyze
{
  "analyzer": "ik_max_word",
  "text": "我爱北京天安门"
}

GET _analyze
{
  "tokenizer": "ik_smart",
  "text": "我爱北京天安门"
}


GET _analyze
{
  "tokenizer": "ik_smart",
  "text": "我爱中华人民共和国"
}

GET /product/_mapping

GET _analyze
{
  "analyzer": "ik_max_word",
  "text": "关关雎鸠"
}

# 把想跳过的词汇加入/etc/elasticsearch/analysis-ik/extra_stopword.dic，然后设置好
# /etc/elasticsearch/analysis-ik/IKAnalyzer.cfg.xml 中相应的标签中的内容就可以自定义词库了，在这里添加了“个”，所以这个词不会被分出来了。同理可以设置extra_main.dic。热加载新词需要修改源代码并连接一个MySQL数据库，作为数据交换历史存储的地方
GET _analyze
{
  "analyzer": "ik_max_word",
  "text": "我是一个好人，我的家住在东北"
}

GET _analyze
{
  "analyzer": "ik_max_word",
  "text": "我的心里深深地爱着小鹿"
}

PUT my_index7
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_analyzer": {
          "type": "pattern",
          "pattern": "[&,<>《》]"
        }
      }
    }
  }
}

GET _analyze
{
  "analyzer": "my_analyzer",
  "text": "《Teacher》 ma & is a also at <a>think</a> is <mother's friends> is good"
}
```