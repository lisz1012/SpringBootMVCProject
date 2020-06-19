- _score在有sort的时候就不见了，因为默认使用相关度score排序，手动指定的话就无效了score为null
- ES查询快，但是写入更新比MySQL满
- 尽量避免不同字段的doc放在同一个index里面，虽然他支持不同的doc里面有不同的字段（schema-less）
- filter的字段不会参与score计算，所以filter不是SQL语义下的过滤语句，而是不让参与score计算的意思：
  https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-bool-query.html