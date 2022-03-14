# payment-demo-back

这是一个微信支付后端项目

## 构建项目

### 创建数据库
```mysql
create database payment_demo;
```
### 执行payment_demo.sql脚本
```mysql
source payment_demo.sql;
```
    
### 配置wxpay.properites文件
该文件已有默认的配置，包括apiclient_key.pem文件，
只需修改通知地址（wxpay.notify-domain）即可，也可以自行申请微信支付平台的身份信息。
通知地址需要是公网地址，也可以进行内网穿透。

## 对应前端项目地址
> https://github.com/qixuan123456/payment-demo-front.git
