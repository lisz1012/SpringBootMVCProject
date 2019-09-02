部署项目的时候一般都打成jar包而不是war包：
项目上右键，Run As， mvn install之后就会在target目录下面出现jar包
java -jar命令或者双击都可以运行他.

这里会有个问题：jar包可以被用户下载
上传文件（图片）有两种存储方法：1.在DB表种存储完整的URL路径。2.存储相对路径（文件名）
bootstrap有默认头像的图片