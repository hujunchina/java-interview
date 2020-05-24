2020年5月23日 21:00:08

[TOC]

# 1. CURD

包含数据库基本操作语句，对管理权限、用户等管理。

## 1.1 用户管理

用户表在数据库msql的user表中。MysQL用户根据授予的权限担任不同的角色。比如某用户只能对一个表进行操作等。

使用数据库和表可以直接用命令 `use +table|database`

### 1.1.1 创建用户

`create user hujun identified by 123456`

创建用户hujun，密码为123456

### 1.1.2 设置密码

可以使用更新语句：`update mysql.user set password=password('123456') where user=hujun`

user表中保存密码默认加密方式，MD5或SHA1等，使用password函数设置。

### 1.1.3 修改用户

`update mysql.user set user='hujun' where user='hujun'`

使用更新语句直接设置即可。

## 1.2 权限管理

MySQL的权限可分为select、insert、delete、update等。在命令中设置的用户仅对某张表起作用。

### 1.2.1 授予权限

`grant select, insert, delete, update on books to hujun@localhost`

使用命令 grant 授予，通过介词 on 表示在那个数据库中，介词 to 表示给哪个用户。

### 1.2.2 收回权限

`revoke all privileges on books from hujun@localhsot`

## 1.3 七种Join

### 1.3.1 交集 inner join A on B

`select * from emp t1 inner join dept t2 on t1.deptId = t2.id`



# 2. 范式