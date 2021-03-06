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
  "text": "我的心里"
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

##########################################################################
##########################################################################
##########################################################################


POST /my_index/_bulk
{"index": {"_id": 1}}
{"text": "my english"}
{"index": {"_id": 2}}
{"text": "my english is good"}
{"index": {"_id": 3}}
{"text": "my chinese is good"}
{"index": {"_id": 4}}
{"text": "my japanese is bad"}
{"index": {"_id": 5}}
{"text": "my disk is full"}

GET /my_index/_search
{
  "query": {
    "prefix": {
      "text": {
        "value": "ch"
      }
    }
  }
}
# prefix wildcard reg匹配的都是分词后倒排索引的结果，对于日期ik会比standard好用
# text能搜到结果
GET /my_index/_search
{
  "query": {
    "wildcard": {
      "text": {
        "value": "engl?sh"
      }
    }
  }
}
# keyword不能搜到结果，与上面不同
GET /my_index/_search
{
  "query": {
    "wildcard": {
      "keyword": {
        "value": "engl?sh"
      }
    }
  }
}
GET /my_index/_search
{
  "query": {
    "wildcard": {
      "text.keyword": {
        "value": "engl?sh"
      }
    }
  }
}
#原因是 .keyword是全量匹配
GET /my_index/_search
{
  "query": {
    "wildcard": {
      "text.keyword": {
        "value": "*engl?sh*"
      }
    }
  }
}

GET /product/_search
{
  "query": {
    "wildcard": {
      "tags": {
        "value": "f?shao"
      }
    }
  }
}

GET /product/_search
{
  "query": {
    "regexp": {
      "name": "[\\s\\S]*nfc[\\s\\S]*"
    }
  }
}
GET /product/_search
{
  "query": {
    "regexp": {
      "name": {
        "value": "[\\s\\S]*nfc[\\s\\S]*",
        "flags": "ALL",
        "max_determinized_states": 10000,
        "rewrite": "constant_score"
      }
    }
  }
}
GET /product/_search
{
  "query": {
    "regexp": {
      "desc": ".*zhandouji.*"
    }
  }
}
GET _analyze
{
  "text": "shouji zhong 2020-05-20 de zhandouji",
  "analyzer": "ik_max_word"
}

GET _analyze
{
  "text": "shouji zhong 2020-05-20 de zhandouji",
  "analyzer": "ik_smart"
}

PUT /my_index
{
  "mappings": {
    "properties": {
      "text": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_max_word"
      }
    }
  }
}

PUT /my_index/_doc/1
{
  "testid": "123456",
  "text": "shouji zhong 2020-05-20 de zhandouji"
}
GET /my_index/_doc/1
GET /my_index/_search
{
  "query": {
    "regexp": {
      "text": {
        "value": ".*2020-05-20.*",
        "flags": "ALL"
      }
    }
  }
}



PUT /my_index
{
  "settings": {
    "analysis": {
      "analyzer": "ik_max_word"
    }
  }
}

POST /my_index/_bulk
{"index": {"_id": 1}}
{"text": "此情可待成追忆，只是当时已惘然。"}
{"index": {"_id": 2}}
{"text": "念天地之悠悠，独怆然而涕下。"}
{"index": {"_id": 3}}
{"text": "事了拂衣去，深藏功与名。"}
{"index": {"_id": 4}}
{"text": "天生我材必有用，千金散尽还复来。"}
{"index": {"_id": 5}}
{"text": "春心莫共花争发，一寸相思一寸灰。"}

GET /my_index/_search

GET /my_index/_search
{
  "query": {
    "prefix": {
      "text": {
        "value": "天生"
      }
    }
  }
}

GET _analyze
{
  "text": "天生我材必有用",
  "analyzer": "standard"
}

# "quangengneng"被容错，"quangongneng"还是被搜索了出来
# "fuzziness": 2 表示编辑距离为2以内的都可以算作匹配上了，比如ES中：axe => aex
# 距离就是1，表示调换了一下相邻的字母
GET /product/_search
{
  "query": {
    "fuzzy": {
      "desc": {
        "value": "quangengneng",
        "fuzziness": 2
      }
    }
  }
}

GET /product/_search
{
  "query": {
    "match": {
      "desc": {
        "query": "quangengneng nfc",
        "fuzziness": "AUTO"
      }
    }
  }
}
# match_phrase_prefix
# 先回顾一下短语搜索, 匹配match_phrase指定的字符串的整体
# "desc": "shouji zhong d"就不会搜索到结果
# 但是用match搜就会搜索到
# 但是
GET /product/_search
{
  "query": {
    "match_phrase": {
      "desc": "shouji zhong de"
    }
  }
}
GET /product/_search
{
  "query": {
    "match": {
      "desc": "shouji zhong d"
    }
  }
}

GET /product/_search
{
  "query": {
    "match_phrase_prefix": {
      "desc": "shouji zhong d"
    }
  }
}
#再倒排索引中扫描，匹配到max_expansions个还没有匹配到结果就不匹配了，为了性能考虑的
#不同的shard可能都有一个doc，doc之间还互不相同。所以最终结果可能不止一个
GET /product/_search?routing=0
{
  "query": {
    "match_phrase_prefix": {
      "desc": {
        "query": "zhong d",
        "max_expansions": 1
      }
    }
  }
}
########################
########################
#按理说"zhong zhandouji" 搜不出来，但是"slop": 2(>=1)之后就可以了.slop指示了中间可以隔几个词都可以认为匹配，slop默认值是0
GET /product/_search
{
  "query": {
    "match_phrase_prefix": {
      "desc": {
        "query": "zhong zhandouji",
        "max_expansions": 1,
        "slop": 2
      }
    }
  }
}
#仍然匹配
GET /product/_search
{
  "query": {
    "match_phrase_prefix": {
      "desc": {
        "query": "shouji de zhong zhandouji",
        "max_expansions": 1,
        "slop": 2
      }
    }
  }
}
#仍然匹配
GET /product/_search
{
  "query": {
    "match_phrase_prefix": {
      "desc": {
        "query": "zhong shouji de zhandouji",
        "max_expansions": 1,
        "slop": 2
      }
    }
  }
}

#不匹配了，shouji要往左移动两次，之后de再往右移动两次，一共4次大于slop的值2
#移动每一个单词的时候，分别跟被匹配的字符串做比较。空格会在分词的时候被替换掉
GET /product/_search
{
  "query": {
    "match_phrase_prefix": {
      "desc": {
        "query": "de zhong shouji zhandouji",
        "max_expansions": 1,
        "slop": 2
      }
    }
  }
}

#slop改成4，就又匹配了
GET /product/_search
{
  "query": {
    "match_phrase_prefix": {
      "desc": {
        "query": "de zhong shouji zhandouji",
        "max_expansions": 1,
        "slop": 4
      }
    }
  }
}

#匹配
GET /product/_search
{
  "query": {
    "match_phrase_prefix": {
      "desc": {
        "query": "de shouji zhandouji",
        "max_expansions": 1,
        "slop": 4
      }
    }
  }
}
#匹配
GET /product/_search
{
  "query": {
    "match_phrase_prefix": {
      "desc": {
        "query": "shouji zhandouji",
        "max_expansions": 1,
        "slop": 2
      }
    }
  }
}
#不匹配删除也是一次slop
GET /product/_search
{
  "query": {
    "match_phrase_prefix": {
      "desc": {
        "query": "shouji zhandouji",
        "max_expansions": 1,
        "slop": 1
      }
    }
  }
}
#ngram默认min_gram=1 max_gram=2, 可以等于1或者2，没有必要一定max_gram大于min_gram
#不能大于2，否则必须在index level上进行修改
GET _analyze
{
  "tokenizer": "ik_max_word",
  "filter": [
    {
      "type": "ngram",
      "max_gram": 2,
      "min_gram": 1
    }
  ],
  "text": "She always loves me"
}

PUT my_index
{
  "settings": {
    "analysis": {
      "filter": {
        "2_3_gram": {
          "type": "ngram",
          "min_gram": 2,
          "max_gram": 3
        }
      },
      "analyzer": {
        "my_ngram": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["2_3_gram"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "text": {
        "type": "text",
        "analyzer": "my_ngram",
        "search_analyzer": "standard"
      }
    }
  }
}

GET _analyze
{
  "tokenizer": "ik_max_word",
  "filter": [
    {
      "type": "ngram",
      "max_gram": 3,
      "min_gram": 2
    }
  ],
  "text": "She always loves me"
}

POST /my_index/_bulk
{"index": {"_id": 1}}
{"text": "my english"}
{"index": {"_id": 2}}
{"text": "my english is good"}
{"index": {"_id": 3}}
{"text": "my chinese is good"}
{"index": {"_id": 4}}
{"text": "my japanese is bad"}
{"index": {"_id": 5}}
{"text": "my disk is full"}

#搜得到，最大粒度是3:ngl，goo匹配。slop默认是0
#词的顺序还不能不一样，有一个词匹配不了也不行
GET /my_index/_search
{
  "query": {
    "match_phrase": {
      "text": "my ngl is goo"
    }
  }
}
#分出来的词就没有engl，所以匹配不到，"my english is good"都匹配不到. 
GET /my_index/_search
{
  "query": {
    "match_phrase": {
      "text": "my engl is good"
    }
  }
}
#也搜不到，最小粒度是2
GET /my_index/_search
{
  "query": {
    "match_phrase": {
      "text": "my e is goo"
    }
  }
}
# 词序不对也不行，搜不到
GET /my_index/_search
{
  "query": {
    "match_phrase": {
      "text": "my is e goo"
    }
  }
}

# 有一个词不匹配也不行，搜不到
GET /my_index/_search
{
  "query": {
    "match_phrase": {
      "text": "my en ss goo"
    }
  }
}

# match全文检索，任意一个能匹配就可以搜到，my匹配所有，所以全都搜出来了
GET /my_index/_search
{
  "query": {
    "match": {
      "text": "my en ss goo"
    }
  }
}

GET _analyze
{
  "tokenizer": "ik_max_word",
  "filter": ["edge_ngram"],
  "text": "reba always loves me"
}

PUT my_index
{
  "settings": {
    "analysis": {
      "filter": {
        "2_3_grams": {
          "type": "edge_ngram",
          "min_gram": 2,
          "max_gram": 3
        }
      },
      "analyzer": {
        "my_edge_ngram": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["2_3_grams"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "text": {
        "type": "text",
        "analyzer": "my_edge_ngram",
        "search_analyzer": "standard"
      }
    }
  }
}

POST /my_index/_bulk
{"index": {"_id": 1}}
{"text": "my english"}
{"index": {"_id": 2}}
{"text": "my english is good"}
{"index": {"_id": 3}}
{"text": "my chinese is good"}
{"index": {"_id": 4}}
{"text": "my japanese is bad"}
{"index": {"_id": 5}}
{"text": "my disk is full"}

GET /my_index/_search
GET /my_index/_mapping

# 匹配前若干个，能匹配
GET /my_index/_search
{
  "query": {
    "match_phrase": {
      "text": "my en is goo"
    }
  }
}
# 匹配前若干个，能匹配
GET /my_index/_search
{
  "query": {
    "match_phrase": {
      "text": "my en is"
    }
  }
}
```

我们ES是要照相关度最高的，而不是找到的结果越多越好（召回率高）