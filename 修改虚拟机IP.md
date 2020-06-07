# 修改虚拟机IP

VMware修改mac

- 修改 ip地址:   
```vim /etc/sysconfig/network-scripts/ifcfg-ens33```  
- rm -f /etc/udev/rules.d/70-persistent-net.rules

- reboot  

或者在图形界面里修改IP 网关 掩码和DNS，然后再：
```service network restart```