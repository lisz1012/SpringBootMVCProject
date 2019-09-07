部署项目的时候一般都打成jar包而不是war包：
项目上右键，Run As， mvn install之后就会在target目录下面出现jar包
java -jar命令或者双击都可以运行他.

这里会有个问题：jar包可以被用户下载
上传文件（图片）有两种存储方法：1.在DB表种存储完整的URL路径。2.存储相对路径（文件名）
bootstrap有默认头像的图片。
数据库里可以只有Username password，这些的查询频率最高，其他的数据可以放到es里（冷数据备份）
account_role, role_permission这种连接表都是手动处理，不用mybatis-generator-gui生成

MyBatis的坑：关联的各个表里面不能有相同的列名，否则就要起别名避免冲突, 所以SQL中要起别名，而且在resultMap的column那里也要改，
比如id改aid，rid，pid，name改成role_name, permission_name

前后端分离之后一律只返回JSON，没有什么model.addAttribute什么的了

中间件：有功能但是没有业务，如：redis，kafka，tomcat

YAML文件的优先级低于application.properties这种文件，后者会覆盖YAML的属性

有用的一点是：启动springboot jar项目的时候可以用：
java -jar SpringBootMVCProjectApplication-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
命令来启动，可以动态选择生产或者测试或者开发环境.但是具体覆盖笼统，县官不如现管：
java -jar SpringBootMVCProjectApplication-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --server.port=80
会在80端口监听
优先级： jar命令参数 > application-dev/test/prod.properties参数 > application.properties > x.yaml
不用背，用两次就记住了。
启动起来就不会变化的数据不太适合放在数据库里

权限在内存中的存储：Map<uri, crud>