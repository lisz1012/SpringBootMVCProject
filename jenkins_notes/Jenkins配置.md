# Jenkins配置

### 权限
安装 Role-based Authorization Strategy插件，然后在"全局安全设置"的"授权策略"里就会出现Role-Based Strategy选项，选中他并保存.
然后在Manage Jenkins下面会增加一个"Manage and Assign Roles"的选项，然后里面可以添加编辑和分配角色了  

可以新建角色， 让他对overall 只有一个读权限，Item Roles里面, 可以设置哪些用户可以看见哪些项目

Extended Choice Parameter 和Git Parameter可以安装一下,Git Parameter的"默认"分支些master就行. */${branch} 有时要去掉
*/否则构建选择分支的时候会多一个每个分支origin/，就不对了