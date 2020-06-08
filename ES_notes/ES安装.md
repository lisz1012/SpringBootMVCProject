# ES安装

【踩坑】ES集群启动失败、用除了localhost之外的IP无法访问网页UI

这是因为要修改一下/etc/elasticsearch/elasticsearch.yml中的network.host 等属性
https://juejin.im/post/5cb81bf4e51d4578c35e727d

为了不在本机上也能访问9200端口，需要在elasticsearch.yml中加入
```
http.cors.enabled: true
http.cors.allow-origin: "*"
```

https://www.elastic.co/guide/en/elasticsearch/reference/7.7/rpm.html#rpm-repo