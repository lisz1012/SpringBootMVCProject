# ES Head 插件的安装

1. 下载head：https://github.com/mobz/elasticsearch-head  
2. 将解压好的目录`elasticsearch-head-master`传输到目标机器的/usr/local目录下  
3. 目标机器上, 进入/usr/local目录，然后： `wget https://nodejs.org/dist/v12.18.0/node-v12.18.0-linux-x64.tar.xz`  
4. `tar xf node-v12.18.0-linux-x64.tar.xz`  
5. `mv node-v12.18.0-linux-x64 nodejs`  
6. 修改/etc/profile，把/usr/local/nodejs/bin配置为环境变量, `source /etc/profile`  
7. `node -v`  
8. `npm install -g grunt-cli`
9. `grunt -version`
10. `vim /usr/local/elasticsearch-head-master/Gruntfile.js`, 在9100端口配置上面加上：`hostname: '*'`  
11. `npm install --global phantomjs-prebuilt  --unsafe-perm`  
12. `npm install --unsafe-perm`  
13. `npm run start`  
14. 访问该ES master节点的9100端口：`http://192.168.1.3:9100/`
15. 在URL输入框中用IP地址替换掉"localhost"，如`http://192.168.1.3:9200/`
16. 点击"Connect"  