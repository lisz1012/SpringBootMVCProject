# ES安装

【踩坑】ES集群启动失败、用除了localhost之外的IP无法访问网页UI

这是因为要修改一下/etc/elasticsearch/elasticsearch.yml中的network.host 等属性
https://juejin.im/post/5cb81bf4e51d4578c35e727d

为了不在本机上也能访问9200端口，需要在elasticsearch.yml中加入
```
http.cors.enabled: true
http.cors.allow-origin: "*"
```

下载链接：  
https://www.elastic.co/guide/en/elasticsearch/reference/7.7/rpm.html#rpm-repo

ES部署建议：不要部署在Docker或者K8S中。因为，他们的意义是简化复杂的部署，但是ES的部署不复杂，而且ES的服务器功能很单一，
理想情况下，ES应该占用服务器的所有资源，不应部署其他任何东西。也不需要隔离，所以在容器中没什么好处

ES服务安装：
https://www.cnblogs.com/zhi-leaf/p/8487404.html