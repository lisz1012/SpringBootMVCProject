# Dubbo 04

# 服务化最佳实践

## 分包

建议将服务接口、服务模型、服务异常等均放在 API 包中，因为服务模型和异常也是 API 的一部分，这样做也符合分包原则：重用发布等价原则(REP)，共同重用原则(CRP)。

如果需要，也可以考虑在 API 包中放置一份 Spring 的引用配置，这样使用方只需在 Spring 加载过程中引用此配置即可。配置建议放在模块的包目录下，以免冲突，如：`com/alibaba/china/xxx/dubbo-reference.xml`。

## Maven 聚合项目改造

公用的接口、工具类、实体类等抽取出API项目独立维护更新.maven本质上是一个工程构建和管理的工具，构建就是把项目从源码的状态编译成可执行的状态。

新建一个Maven项目，next，然后选择quick start那个. GroupId的设置：尽量跟已有的项目一致，因为这样好拷贝东西。由于我们不需要给新项目打jar包，所以在pom文件里<package>标签里写"pom"而不是"jar".然后在项目上右键->
Maven->New Maven Module Project, create simple，起好Module名字一路next然后finish。然后可以看到：在Project Explorer下面有一个新项目生成了，然后在我们刚刚右键点击的项目下面也有一个新的同名的项目（目录），
它的pom文件很干净，这样骨架就搭好了，然后如法炮制所有的业务子模块。而我们现在想把公用的接口和Entity抽取出来，然大家都用，所以还要创建一个工程（API）装着它们,也是一个子Module。然后对于每个子项目（不是父项目里的目录）
进行Run As -> Build Install, 这样会把该module了做成jar包放到本地的中央仓库里，形成一个依赖，在打印出来的日志里可以找到。项目间的互相引用可以通过<dependencies>标签来引入相应的groupId和artifactId，或者项目上
右键->Maven->Add Dependency  
然后就是把各个原来项目的代码拷贝到子module中来。然后就是管理依赖，因为里面有重复的，也有某些项目里独有的. 共有的依赖放在父项目的pom文件里面，可以被继承，子项目的pom文件可以放个性化的依赖。在父项目的pom中有两个部分
管理依赖：1. 对依赖版本的管理（<dependencyManagement>，只定义版本号，非要自定义版本号的时候会覆盖父项目的版本 不是所有的子项目都需要的依赖） 2. 对真正公用的依赖的管理(<dependencies>， 所有项目都要用的依赖)  
对于springboot的项目，所有的项目和module都有一个spring-boot-starter，所以把它（爷爷）拷贝到父项目里的<project>标签里面  
<properties>中定义了Dubbo和java的version，拷贝过来  
子项目里的所有<dependencies>(包括本标签)先全部粘到父项目的pom的<denpendencyManagement>中。由于我们做的是Dubbo项目，所以Dubbo的依赖也是所有项目都需要的，所以把<dependencyManagement>中的dubbo相关依赖都
移到<dependencies>中去. <denpendencyManagement>中剩下的<dependency>拷贝到需要他的项目的pom文件中，但是要去掉版本号，因为在父项目里面已经指定了. 依赖的管理的最佳实践是：在父项目里面定义一个，子项目里都继承。  
然后就是copy代码，先拷贝主启动类。注意，pom文件在父子项目继承的时候，如果还有爷爷项目，而父项目没写版本号，则子项目也没有版本号，报错。解决方法是把父项目中的那个没有版本号的dependency删掉。拷贝代码的时候公用得类跳过，
其他有报错的就是缺少依赖，那就从父项目的<denpendencyManagement>中拷贝过来然后去掉版本号. 公用代码copy到了API项目中之后build install 这个API项目一下，然后就可以在其他的用到他的项目里的pom文件的dependencies
标签下引入他了：
```
<dependency>
	<groupId>com.lisz</groupId>
	<artifactId>API</artifactId>
	<version>0.0.1-SNAPSHOT</version>
</dependency>
```  
在Maven Dependencies里可以查看有没有引入，成功的话会多一个名叫API的dependency


聚合项目就是把好多子项目聚合成一个大项目

Dubbo可以做微服务治理，包括LB，容错，监控等。Dubbo有LB策略，在@Service或者@Reference的属性里配置不定调用到了那个service实例。默认是随机的
所以有weight和loadbalance = "roundrobin"的配置.后者就是有多少个当前service的实例就把它们挨个轮询，策略还有"leastactive"和一致性哈希
"consistenthash"(用不好的话还不如不用这个)。@Service里的配置在消费端的@Reference中也能配置，我们一般在服务端配置。两端可以都配置，但是消费端的配置会
覆盖掉服务端的配置，所以本质上应该先有一份配置就在服务端。服务提供方自己最了解自己的架构性能。这也避免了没配置，结果来了个随机的loadbalance的情形。
timeout(ms)属性是定义多长时间没有响应就算超时了，如果是幂等（不管多少次，只要输入一样，输出都相同）的话，可以配合retries属性一起配置来重复请求，重试的次数不
包含最初的那次调用.重试的话就会按照负载均衡策略，不一定还会访问到当前这个机器或者实例
每次请求过来都新开一个线程：
   Port: 9999, Latency: 3395, Thread: DubboServerHandler-192.168.1.102:9999-thread-2
   Port: 9999, Latency: 3193, Thread: DubboServerHandler-192.168.1.102:9999-thread-3
   Port: 9999, Latency: 3039, Thread: DubboServerHandler-192.168.1.102:9999-thread-4
   超时的话会在客户端抛出异常，但是服务端可能还在执行，但是客户端认为这次已经无效了。超时要小于浏览器的超时时间
   execute属性可以限流：Provider处理请求时，统计方法维度的调用情况，如果并发数超过设置的最大值，则直接抛出异常。
   org.apache.dubbo.rpc.RpcException: Failed to invoke method getPort in provider 
   dubbo://192.168.1.102:9999/com.lisz.service.TestService?anyhost=false&application=Dubbo-OA-Provider&bean.name
   =ServiceBean:com.lisz.service.TestService:1.0.0&bind.ip=192.168.1.102&bind.port=9999&deprecated=false&dubbo
   =2.0.2&dynamic=true&executes=1&generic=false&interface=com.lisz.service.TestService&loadbalance=roundrobin&methods
   =getPort&pid=10856&qos.enable=false&register=true&release=2.7.3&retries=3&revision=1.0.0&side=provider&timeout=3000&timestamp
   =1577682606066&version=1.0.0, cause: The service using threads greater than <dubbo:service executes="1" /> limited.
	at org.apache.dubbo.rpc.filter.ExecuteLimitFilter.invoke(ExecuteLimitFilter.java:56)
	at org.apache.dubbo.rpc.protocol.ProtocolFilterWrapper$1.invoke(ProtocolFilterWrapper.java:82)
	最好不要在服务端设置execute限制，这样客户端直接报错，浏览器用户体验不好，应该在API网关（Kong kong，Zuul）或者hetries设置，整体并发量已经接近阈值了，就不
	应该让请求达到service上。但是另一方面讲，如果前端网关，比如说限流为1000，则后面的两台服务器各500，如果后端在前端网关挂掉的时候没有限流，则可能挂掉；反之则可以
	拒绝掉一些请求
	
	负载策略轮询用得最多，随机的话会有可能有流量倾斜，最小连接数性能稍微受影响。Dubbo中没有降级熔断，需要用springcloud的组件
每次更新后需要install

## 粒度

服务接口尽可能大粒度，每个服务方法应代表一个功能，而不是某功能的一个步骤，否则将面临分布式事务问题，Dubbo 暂未提供分布式事务支持。

服务接口建议以业务场景为单位划分，并对相近业务做抽象，防止接口数量爆炸。

不建议使用过于抽象的通用接口，如：`Map query(Map)`，这样的接口没有明确语义，会给后期维护带来不便。

## 版本

每个接口都应定义版本号，为后续不兼容升级提供可能，如： `<dubbo:service interface="com.xxx.XxxService" version="1.0" />`。

建议使用两位版本号，因为第三位版本号通常表示兼容升级，只有不兼容时才需要变更服务版本。

当不兼容时，先升级一半提供者为新版本，再将消费者全部升为新版本，然后将剩下的一半提供者升为新版本。

## 兼容性

服务接口增加方法，或服务模型增加字段，可向后兼容，删除方法或删除字段，将不兼容，枚举类型新增字段也不兼容，需通过变更版本号升级。

## 枚举值

如果是完备集，可以用 `Enum`，比如：`ENABLE`, `DISABLE`。

如果是业务种类，以后明显会有类型增加，不建议用 `Enum`，可以用 `String` 代替。

如果是在返回值中用了 `Enum`，并新增了 `Enum` 值，建议先升级服务消费方，这样服务提供方不会返回新值。

如果是在传入参数中用了 `Enum`，并新增了 `Enum` 值，建议先升级服务提供方，这样服务消费方不会传入新值。

## 序列化

服务参数及返回值建议使用 POJO 对象，即通过 `setter`, `getter` 方法表示属性的对象。

服务参数及返回值不建议使用接口，因为数据模型抽象的意义不大，并且序列化需要接口实现类的元信息，并不能起到隐藏实现的意图。

服务参数及返回值都必须是[传值调用](https://en.wikipedia.org/wiki/Evaluation_strategy#Call_by_value)，而不能是[传引用调用](https://en.wikipedia.org/wiki/Evaluation_strategy#Call_by_reference)，消费方和提供方的参数或返回值引用并不是同一个，只是值相同，Dubbo 不支持引用远程对象。

## 异常

建议使用异常汇报错误，而不是返回错误码，异常信息能携带更多信息，并且语义更友好。

如果担心性能问题，在必要时，可以通过 override 掉异常类的 `fillInStackTrace()` 方法为空方法，使其不拷贝栈信息。

查询方法不建议抛出 checked 异常，否则调用方在查询时将过多的 `try...catch`，并且不能进行有效处理。

服务提供方不应将 DAO 或 SQL 等异常抛给消费方，应在服务实现中对消费方不关心的异常进行包装，否则可能出现消费方无法反序列化相应异常。

## 调用

不要只是因为是 Dubbo 调用，而把调用 `try...catch` 起来。`try...catch` 应该加上合适的回滚边界上。

Provider 端需要对输入参数进行校验。如有性能上的考虑，服务实现者可以考虑在 API 包上加上服务 Stub 类来完成检验。

## 在 Provider 端尽量多配置 Consumer 端属性

原因如下：

- 作服务的提供方，比服务消费方更清楚服务的性能参数，如调用的超时时间、合理的重试次数等
- 在 Provider 端配置后，Consumer 端不配置则会使用 Provider 端的配置，即 Provider 端的配置可以作为 Consumer 的缺省值 [[1\]](http://dubbo.apache.org/zh-cn/docs/user/recommend.html#fn1)。否则，Consumer 会使用 Consumer 端的全局设置，这对于 Provider 是不可控的，并且往往是不合理的

Provider 端尽量多配置 Consumer 端的属性，让 Provider 的实现者一开始就思考 Provider 端的服务特点和服务质量等问题。

## 建议在 Provider 端配置的 Consumer 端属性

1. `timeout`：方法调用的超时时间

1. `retries`：失败重试次数，缺省是 2 

1. `loadbalance`：负载均衡算法，缺省是随机 `random` + 权重。还可以配置轮询 `roundrobin`、最不活跃优先 `leastactive` 和一致性哈希 `consistenthash` 等

1. `actives`：消费者端的最大并发调用限制，即当 Consumer 对一个服务的并发调用到上限后，新调用会阻塞直到超时，在方法上配置 `dubbo:method` 则针对该方法进行并发限制，在接口上配置 `dubbo:service`，则针对该服务进行并发限制
2. `executes`服务提供方可以使用的最大线程数

## 在 Provider 端配置合理的 Provider 端属性

建议在 Provider 端配置的 Provider 端属性有：

1. `threads`：服务线程池大小
2. `executes`：一个服务提供者并行执行请求上限，即当 Provider 对一个服务的并发调用达到上限后，新调用会阻塞，此时 Consumer 可能会超时。在方法上配置 `dubbo:method` 则针对该方法进行并发限制，在接口上配置 `dubbo:service`，则针对该服务进行并发限制

### 

项目中多个模块间公共依赖的版本号、scope的控制

## 配置 Dubbo 缓存文件

提供者列表缓存文件：

```xml
<dubbo:registry file=”${user.home}/output/dubbo.cache” />
```

```properties
dubbo.registry.file=c:/output/dubbo.cache
```



注意：

1. 可以根据需要调整缓存文件的路径，保证这个文件不会在发布过程中被清除；
2. 如果有多个应用进程，请注意不要使用同一个文件，避免内容被覆盖；

该文件会缓存注册中心列表和服务提供者列表。配置缓存文件后，应用重启过程中，若注册中心不可用，应用会从该缓存文件读取服务提供者列表，进一步保证应用可靠性。

## 启动检查

Dubbo 缺省会在启动时检查依赖的服务是否可用，不可用时会抛出异常，阻止 Spring 初始化完成，以便上线时，能及早发现问题，默认 `check="true"`。

可以通过 `check="false"` 关闭检查，比如，测试时，有些服务不关心，或者出现了循环依赖，必须有一方先启动。

另外，如果你的 Spring 容器是懒加载的，或者通过 API 编程延迟引用服务，请关闭 check，否则服务临时不可用时，会抛出异常，拿到 null 引用，如果 `check="false"`，总是会返回引用，当服务恢复时，能自动连上。

### 示例

#### 通过 spring 配置文件

关闭某个服务的启动时检查 (没有提供者时报错)：

```xml
<dubbo:reference interface="com.foo.BarService" check="false" />
```

关闭所有服务的启动时检查 (没有提供者时报错)：

```xml
<dubbo:consumer check="false" />
```

关闭注册中心启动时检查 (注册订阅失败时报错)：

```xml
<dubbo:registry check="false" />
```

#### 通过 dubbo.properties

```properties
dubbo.reference.com.foo.BarService.check=false
dubbo.reference.check=false
dubbo.consumer.check=false
dubbo.registry.check=false
```

#### 通过 -D 参数

```sh
java -Ddubbo.reference.com.foo.BarService.check=false
java -Ddubbo.reference.check=false
java -Ddubbo.consumer.check=false 
java -Ddubbo.registry.check=false
```

#### 配置的含义

`dubbo.reference.check=false`，强制改变所有 reference 的 check 值，就算配置中有声明，也会被覆盖。

`dubbo.consumer.check=false`，是设置 check 的缺省值，如果配置中有显式的声明，如：`<dubbo:reference check="true"/>`，不会受影响。

`dubbo.registry.check=false`，前面两个都是指订阅成功，但提供者列表是否为空是否报错，如果注册订阅失败时，也允许启动，需使用此选项，将在后台定时重试。









## metrics

当我们需要为某个系统某个服务做监控、做统计，就需要用到Metrics。

## 延迟暴露

@Service(version = "1.0.0" ,timeout = 10000, interfaceClass = IAccountService.class,delay = 1000000)



## Telnet治理服务

###  显示服务

 `ls`

1. `ls`: 显示服务列表
2. `ls -l`: 显示服务详细信息列表
3. `ls XxxService`: 显示服务的方法列表
4. `ls -l XxxService`: 显示服务的方法详细信息列表



`ps`

1. `ps`: 显示服务端口列表
2. `ps -l`: 显示服务地址列表
3. `ps 20880`: 显示端口上的连接信息
4. `ps -l 20880`: 显示端口上的连接详细信息

### 服务调用

引入fastjson依赖

```xml
<dependency>
	<groupId>com.alibaba</groupId>
	<artifactId>fastjson</artifactId>
	<version>1.2.51</version>
</dependency>
```



```java
invoke com.mashibing.springboot.service.IRoleService:1.0.0.findById(8)
```

### Telnet命令扩展

http://dubbo.apache.org/zh-cn/docs/dev/impls/telnet-handler.html

### QOS 模块手动上下线

#### 相关参数说明

QoS提供了一些启动参数，来对启动进行配置，他们主要包括：

| 参数               | 说明              | 默认值 |
| ------------------ | ----------------- | ------ |
| qosEnable          | 是否启动QoS       | true   |
| qosPort            | 启动QoS绑定的端口 | 22222  |
| qosAcceptForeignIp | 是否允许远程访问  | false  |

> 注意，从2.6.4/2.7.0开始，qosAcceptForeignIp默认配置改为false，如果qosAcceptForeignIp设置为true，有可能带来安全风险，请仔细评估后再打开。

新版本的 telnet 端口 与 dubbo 协议的端口是不同的端口，默认为 `22222`，可通过配置文件`dubbo.properties`修改:

```
dubbo.application.qos.port=33333
```

或者通过设置 JVM 参数:

```
-Ddubbo.application.qos.port=33333
```

默认情况下，dubbo 接收任何主机发起的命令，可通过配置文件`dubbo.properties` 修改:

```
dubbo.application.qos.accept.foreign.ip=false
```

或者通过设置 JVM 参数:

```
-Ddubbo.application.qos.accept.foreign.ip=false
```

## 