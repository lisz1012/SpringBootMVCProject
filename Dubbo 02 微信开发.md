# Dubbo 02 微信开发

## Dubbo Admin

https://github.com/apache/dubbo-admin

下载下来之后import existing maven project引入，然后右键点击dubbo-admin这个父项目，maven build install，会在dubbo-admin-distribution子项目中target目录下生成一个dubbo-admin-0.1.jar文件，运行它：

```java -jar ./dubbo-admin-0.1.jar --admin.registry.address=zookeeper://192.168.1.120:2181 --admin.config-center=zookeeper://192.168.1.120:2181 
--admin.metadata-report.address=zookeeper://192.168.1.120:2181```

这是因为dubbo-admin项目下的dubbo-admin-server/src/main/resources/application.properties里面的

```

# centers in dubbo2.7
admin.registry.address=zookeeper://192.168.1.120:2181
admin.config-center=zookeeper://192.168.1.120:2181
admin.metadata-report.address=zookeeper://192.168.1.120:2181

```

并没有改成我们正确的zk集群的地址，所以要用参数覆盖（这里我已经手动改过来了）   
项目小的话用dubbo就够了。RPC框架就是调用服务的，而Spring Cloud有了一堆对服务治理增强的工具或者框架（dubbo也在往这方面走，不太成熟，还需时日才能追上Spring Cloud）  
拆分服务的时候Consumer和Provider都拷贝了entity。Consumer只拷贝service接口，他还拷贝controllers；Provider拷贝mapper，utils，service接口，service实现类。拷贝的时候包名最好也一样  
发现一bug，拆分项目的时候，前端的那个Consumer，明明在pom.xml文件中已经删除了mybatis相关的dependency，结果启动springboot的时候还是找dataSource的url，然后找不到（当然找不到了，前端的部分根本不连数据库，没这业务）报错
这时候就要在整个springboot的App启动类的脑袋上面上的注解里加属性设置：@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})，以强制springboot不要去找数据库相关的配置。Dao的那一层跟Provider
那个为服务在一起就可以了，也没必要把粒度分得太细了，那就太乱了。

微服务带来了，扩展性，稳定性，高可用性，降低了耦合度.  
微信的登陆仅仅就能拿到头像，昵称，地点，性别没有更多的信息了

## 原系统微服务改造

### mvc层排除数据源检查

Application 入口程序添加

```
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
```

自己手机扫码进入https://mp.weixin.qq.com/debug/cgi-bin/sandboxinfo?action=showinfo&t=sandbox/index 可以设置Token。appID和appsecret也要跟application.properties中的一致
在Ticket/TokenManagerListener (implements ServletContextListener)脑袋上面写注解：@WebListener 而且在启动类脑袋上面写@ServletComponentScan(basePackages = "com.lisz.controller.listener")
以保证listner会被加载。启动之后会出现：https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=28_YUm4GceMOT7zX6Gw_NjOyRtvi5YRKDvR12uGPClJiE7aToVCCtWkThGQQZxtw95MBUjA8f5atqJ6hvr2-yhr1t9SU6kl0lMkKvKRd_MlAxkvJZ6OoeLNy7DgBAEi46h9WOrtdkDQnFomV3vtCLYhAGAPYL&type=jsapi
我们就可以用这个token调用微信给我们提供的API接口了  

发图的时候图片是传到了微信的服务器上


## 新增微信接口微服务

功能：微信登录

前置条件：微信开放平台

https://open.weixin.qq.com/

可以获取snsapi_login

开发测试环境：公众号

公众号（公众平台）获取的scope只包括两种：snsapi_base 和snsapi_userinfo

公众号和小程序只有展示不一样，该调用的API一个都不少

### 环境搭建

#### 获取测试账号

https://mp.weixin.qq.com

注册登录后使用测试账号开发

#### 反向代理服务器

主要用于开发中内网穿透

http://www.ngrok.cc/

http://www.natapp.cc/

### API

#### 微信公众平台开发者文档

https://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1445241432

#### 微信开放平台（公众号第三方平台开发）

https://open.weixin.qq.com/cgi-bin/showdocument?action=dir_list&t=resource/res_list&verify=1&lang=zh_CN

#### 微信小程序开发文档

https://developers.weixin.qq.com/miniprogram/dev/framework/

#### 微信商户服务中心

https://mp.weixin.qq.com/cgi-bin/readtemplate?t=business/faq_tmpl&lang=zh_CN

#### 微信支付商户平台开发者文档

https://pay.weixin.qq.com/wiki/doc/api/index.html

#### 微信支付H5

https://pay.weixin.qq.com/wiki/doc/api/H5.php?chapter=15_1

#### 微信支付代扣费

https://pay.weixin.qq.com/wiki/doc/api/pap.php?chapter=17_1

#### 微信支付单品优惠

https://pay.weixin.qq.com/wiki/doc/api/danpin.php?chapter=9_201&index=3

### 开发框架

https://github.com/liyiorg/weixin-popular

- TokenAPI access_token 获取
- MediaAPI 多媒体上传下载(临时素材)
- MaterialAPI 永久素材
- MenuAPI 菜单、个性化菜单
- MessageAPI 信息发送（客服消息、群发消息、模板消息）
- PayMchAPI 支付订单、红包、企业付款、委托代扣、代扣费(商户平台版)、分账
- QrcodeAPI 二维码
- SnsAPI 网签授权
- UserAPI 用户管理、分组、标签、黑名单
- ShorturlAPI 长链接转短链接
- TicketAPI JSAPI ticket
- ComponentAPI 第三方平台开发
- CallbackipAPI 获取微信服务器IP地址
- ClearQuotaAPI 接口调用频次清零
- PoiAPI 微信门店 @Moyq5 (贡献)
- CardAPI 微信卡券 @Moyq5 (贡献)Shak
- earoundAPI 微信摇一摇周边 @Moyq5 (贡献)
- DatacubeAPI 数据统计 @Moyq5 (贡献)
- CustomserviceAPI 客服功能 @ConciseA (贡献)
- WxaAPI 微信小程序
- WxopenAPI 微信小程序
- CommentAPI 文章评论留言
- OpenAPI 微信开放平台帐号管理
- BizwifiAPI 微信连WiFi
- ScanAPI 微信扫一扫
- SemanticAPI 微信智能

```
<dependency>
  <groupId>com.github.liyiorg</groupId>
  <artifactId>weixin-popular</artifactId>
  <version>2.8.28</version>
</dependency>
```

## 入口层 -> 域名与高并发

在入口层 加入CDN技术可以提高用户响应时间 让系统能够承受更高并发，分发请求

尤其对 全网加速（海外用户）效果明显

![img](C:\Users\Administrator\Desktop\tmp\域名\01jpg)



### 域名

### DNS

domain name system

DNS是应用层协议，事实上他是为其他应用层协议工作的，包括不限于HTTP和SMTP以及FTP，用于将用户提供的主机名解析为ip地址。

dns集群

![img](https://pic2.zhimg.com/80/607e9d15fd6d5f9d02f6f4b0adb261b9_hd.jpg)

### CDN

![img](https://pic2.zhimg.com/80/607e9d15fd6d5f9d02f6f4b0adb261b9_hd.jpg)

## 微信开发

### 私服验证

```

```



### 菜单管理

创建菜单

```json
{
    "button": [
        {
            "type": "click", 
            "name": "今日歌曲", 
            "key": "V1001_TODAY_MUSIC"
        }, 
        {
            "name": "菜单", 
            "sub_button": [
                {
                    "type": "view", 
                    "name": "搜索", 
                    "url": "http://www.soso.com/"
                }, 
                {
                    "type": "miniprogram", 
                    "name": "wxa", 
                    "url": "http://mp.weixin.qq.com", 
                    "appid": "wx286b93c14bbf93aa", 
                    "pagepath": "pages/lunar/index"
                }, 
                {
                    "type": "click", 
                    "name": "赞一下我们", 
                    "key": "V1001_GOOD"
                }
            ]
        }
    ]
}
```
view就是个网页url，click会把它里面的key传到后台
自己做实验的时候没有实验认证条件，可以删掉上面JSON中小程序的部分：
```
{
     "type":"miniprogram",
     "name":"wxa",
     "url":"http://mp.weixin.qq.com",
     "appid":"wx286b93c14bbf93aa",
     "pagepath":"pages/lunar/index"
},
```
高版本的JDK9/10之后要pom文件里添加：
```<!-- API, java.xml.bind module -->
		<dependency>
		    <groupId>jakarta.xml.bind</groupId>
		    <artifactId>jakarta.xml.bind-api</artifactId>
		    <version>2.3.2</version>
		</dependency>
		
		<!-- Runtime, com.sun.xml.bind module -->
		<dependency>
		    <groupId>org.glassfish.jaxb</groupId>
		    <artifactId>jaxb-runtime</artifactId>
		    <version>2.3.2</version>
		</dependency>
```

### 消息回复

文本

```java
XMLTextMessage xmlTextMessage = new XMLTextMessage(eventMessage.getFromUserName(), eventMessage.getToUserName(), "hi");

xmlTextMessage.outputStreamWrite(outputStream);
```

图

```
String mediaId= "YiHQtRD_fDKEG3-yTOwiGWlqv56-SUW5vfEDeEuAKx9a78337LKlSUmI4T-Cj8ij";
XMLImageMessage xmlImageMessage = new XMLImageMessage(eventMessage.getFromUserName(),eventMessage.getToUserName(),mediaId);
xmlImageMessage.outputStreamWrite(outputStream);
```

连接

```

XMLTextMessage xmlTextMessage2 = new XMLTextMessage(eventMessage.getFromUserName(), eventMessage.getToUserName(), "请先<a href='"+wxConf.getAppDomain()+"/h5/account/register'>完善一下信息</a>");
	
```



```
	TemplateMessage msg = new TemplateMessage();

		msg.setTouser("oStlBwHto08mKRIVUod5IHyevJyE");
		msg.setUrl("http://baidu.com");
		msg.setTemplate_id("gj4jA7HoS-1bmGyBK8VedBBQAXAboRJfWxUpbA8HlvM");

		LinkedHashMap<String, TemplateMessageItem> items = new LinkedHashMap<>();

		// 填充模板内容
		items.put("content", new TemplateMessageItem(" 宝宝，你好。", "#000000"));
		msg.setData(items);

		// 发送提醒
		MessageAPI.messageTemplateSend(TokenManager.getToken(wxConf.getAppID()), msg);

```

redirect_uri域名与后台配置不一致，错误码：10003 解决办法：在https://mp.weixin.qq.com/debug/cgi-bin/sandboxinfo?action=showinfo&t=sandbox/index 下找到“网页授权获取用户基本信息”点击后面的“修改”，然后填入
要限制访问的uri  
用户名字里面有emoji的时候mysql无法用varchar存，得用utf8mb4