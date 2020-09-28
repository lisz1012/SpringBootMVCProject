# Jenkins内网端口映射
目的是让外网能够访问Jenkins，方便Github/Lab的webhook在有`git push`操作的时候触发Jenkins的流水线。由于我的Jenkins服务器是
安装在192.168.1.253上，通过无线路由器192.168.1.1上网，而后者又通过Modem 192.168.0.1上网（无线路由器在Modem的局域网中的IP：
192.168.0.2），所以要设置两次端口转发。

## 1. 把Jenkins Server看作内网，无线路由器之外的Modem看作外网，此时要在无线路由器这里设置
端口转发：
![image](https://github.com/lisz1012/SpringBootMVCProject/blob/master/images/Jenkins_Port_Forwarding_1.png)
  
## 2. 要把无线路由其看作内网，把modem之外的互联网世界看作外网，所以要在Modem上设置端口转发：
![image](https://github.com/lisz1012/SpringBootMVCProject/blob/master/images/Jenkins_Port_Forwarding_2.png)
  
此时就把8083端口对外开放了，通过它的访问就会被转发到Jenkins Server（192.168.1.253）的8080端口（Jenkins默认），然后可以通过
`curl ifconfig.me` 命令得知Modem的外网IP，然后在浏览器中就可以用 `IP:8083` 访问Jenkins服务了😊