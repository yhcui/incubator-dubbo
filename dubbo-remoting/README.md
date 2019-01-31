## 远程通信模块
* 相当于 Dubbo 协议的实现，如果 RPC 用 RMI协议则不需要使用此包

## 模块说明
* dubbo-remoting-api：定义了客户端和服务端的接口。
* dubbo-remoting-grizzly：基于Grizzly实现的Client和Server。
* dubbo-remoting-http：基于Jetty或Tomcat实现的Client和Server。
* dubbo-remoting-mina：基于Mina实现的Client和Server。
* dubbo-remoting-netty：基于Netty3实现的Client和Server。
* dubbo-remoting-netty4：基于Netty4实现的Client和Server。
* dubbo-remoting-p2p：P2P服务器，注册中心multicast中会用到这个服务器使用。
* dubbo-remoting-zookeeper：封装了Zookeeper Client ，和 Zookeeper Server 通信。
