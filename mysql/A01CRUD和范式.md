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

```mysql
SELECT <ROW-LIST>
	FROM <LEFT-TABLE>
	<INNER | LETT | RIGHT> JOIN <RIGHT-TABLE>
		ON <JOIN-CONDITION>
			WHERE <WHERE-CONDITION>
```

执行顺序（**SQL语句里第一个被执行的总是FROM子句**）：

- **FROM**:对左右两张表执行笛卡尔积，产生第一张虚拟表vt1。行数为n*m（n左表的行数，m右表的行数）
- **ON**:根据ON的条件逐行筛选vt1，将结果插入vt2中
- **JOIN**:添加外部行，如果指定了**LEFT JOIN**(**LEFT OUTER JOIN**)，则先遍历一遍**左表**的每一行，其中不在vt2的行会被插入到vt2，该行的剩余字段将被填充为**NULL**，形成vt3；如果指定了**RIGHT JOIN**也是同理。但如果指定的是**INNER JOIN**，则不会添加外部行，上述插入过程被忽略，vt2=vt3（所以**INNER JOIN**的过滤条件放在**ON**或**WHERE**里 执行结果是没有区别的，下文会细说）
- **WHERE**:对vt3进行条件过滤，满足条件的行被输出到vt4
- **SELECT**:取出vt4的指定字段到vt5

### 1.3.1 执行顺序案例

创建用户信息表和用户余额表：

```mysql
CREATE TABLE `user_info`(
    `userId` int(11) not null,
    `name` varchar(255) not null,
    UNIQUE `userId`(`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `user_account`(
    `userId` int(11) not null,
    `money` bigint(20) not null,
    UNIQUE `userId`(`userId`)
) ENGING=InnoDB DEFAULT CHARSET=utf8mb4
```

导入数据：

| userId | 1    | 2    | 3    | 4    | 5    |
| ------ | ---- | ---- | ---- | ---- | ---- |
| name   | a    | b    | c    | d    | e    |

| userId | 1    | 2    | 3    | 9    |
| ------ | ---- | ---- | ---- | ---- |
| money  | 100  | 200  | 300  | 400  |

取出userid为1003的用户姓名和余额，SQL如下:

```mysql
SELECT U.NAME, A.MONEY
	FROM `user_info` AS U
		LEFT JOIN `user_account` AS A
			ON U.userId = A.userId
				WHERE A.userID=3
```

#### 1.3.1.1 第一步 from

笛卡尔积操作后会返回两张表中所有行的组合，左表userinfo有5行，右表useraccount有4行，生成的虚拟表vt1就是5*4=20行：

| userId | 1    | 2    | 3    | 4    | 5    | 1    | 2    | 3    | 4    | 5    | 1    | 2    | 3    | 4    | 5    | 1    | 2    | 3    | 4    |
| ------ | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| name   | a    | b    | c    | d    | e    | a    | b    | c    | d    | e    | a    | b    | c    | d    | e    | a    | b    | c    | d    |
| userId | 1    | 1    | 1    | 1    | 1    | 2    | 2    | 2    | 2    | 2    | 3    | 3    | 3    | 3    | 3    | 9    | 9    | 9    | 9    |
| money  | 1    | 1    | 1    | 1    | 1    | 2    | 2    | 2    | 2    | 2    | 3    | 3    | 3    | 3    | 3    | 4    | 4    | 4    | 4    |

from 会直接把两个表中的数据做一一映射，即笛卡尔积。

#### 1.3.1.2 第二步 on

执行ON子句过滤掉不满足条件的行，`ON U.userId = A.userId`，得到第二个虚拟表vt2：

| userId | 1    | 2    | 3    |
| ------ | ---- | ---- | ---- |
| name   | a    | b    | c    |
| userId | 1    | 2    | 3    |
| money  | 100  | 200  | 300  |

from 做了两个表的笛卡尔积，就确保了无论是什么类型join，所有可能情况都在表中，所以可以直接先判断on的条件，不用join。

#### 1.3.1.3 第三步 join

**LEFT JOIN**会将左表未出现在vt2的行插入进vt2，每一行的剩余字段将被填充为NULL，**RIGHT JOIN**同理。

本例中用的是**LEFT JOIN**，所以会将左表**user_info**剩下的行都添上 生成表vt3：

| userId | 1    | 2    | 3    | 4    | 5    |
| ------ | ---- | ---- | ---- | ---- | ---- |
| name   | a    | b    | c    | e    | f    |
| userId | 1    | 2    | 3    | NULL | NULL |
| money  | 100  | 100  | 100  | NULL | NULL |

left join 就是把左表出现的键值全部连接进虚拟表中；right join 就是把右表所有主键连接到虚拟表中；inner join 需要把左右表共有部分（交集）连接到虚拟表中。

如果直接看实绩表，left join 表现效果就是以左表主键为主建立连接，右表有主键而左表没有的直接舍去；右表没有的而左表有的直接置NULL。 

#### 1.3.1.4 第四步 where

WHERE a.userid = 3 生成表vt4：

| userId | name | userId | money |
| ------ | ---- | ------ | ----- |
| 3      | c    | 3      | 300   |

#### 1.3.1.5 第五步 select

最后 select 会选择需要的列字段返回为结果，虚拟表vt5：

| name | money |
| ---- | ----- |
| c    | 300   |

### 1.3.2 三种 Join 区别

- **INNER JOIN...ON...**: 返回 左右表互相匹配的所有行（因为只执行上文的第二步ON过滤，不执行第三步 添加外部行）
- **LEFT JOIN...ON...**: 返回左表的所有行，若某些行在右表里没有相对应的匹配行，则将右表的列在新表中置为NULL
- **RIGHT JOIN...ON...**: 返回右表的所有行，若某些行在左表里没有相对应的匹配行，则将左表的列在新表中置为NULL

注意，MySQL 没有 FULL JOIN 所以需要通过left+right+union来实现。 

### 1.3.3 交集 inner join A on B

```mysql
SELECT U.NAME, A.MONEY
	FROM `user_info` as U
		INNER JOIN `user_account` as A
			ON A.userId = U.userId
				WHERE A.userId = 3;
```

交集只会在 on 这一步停下来，不会去 join 了，所以有时候缩写 `inner on` 。即不会把左表或右表的所有行都映射到虚拟表中。

### 1.3.4 左连 left join A on B

```mysql
SELECT U.NAME, A.MONEY
	FROM `user_info` as U
		LEFT JOIN `user_account` as A
			ON A.userId = U.userId
				WHERE A.userId = 3;
```



### 1.3.5 右连 right join A on B

```mysql
SELECT U.NAME, A.MONEY
	FROM `user_info` as U
		RIGHT JOIN `user_account` as A
			ON A.userId = U.userId
				WHERE A.userId = 3;
```



### 1.3.6 左表独有

```mysql
SELECT U.NAME, A.MONEY
	FROM `user_info` as U
		LEFT JOIN `user_account` as A
			ON A.userId = U.userId
				WHERE A.userId IS NULL;
```



### 1.3.7 右表独有

```mysql
SELECT U.NAME, A.MONEY
	FROM `user_info` as U
		RIGHT JOIN `user_account` as A
			ON A.userId = U.userId
				WHERE U.userId IS NULL;
```



### 1.3.8 全连 full join

```mysql
SELECT U.NAME, A.MONEY
	FROM `user_info` as U
		LEFT JOIN `user_account` as A
			ON A.userId = U.userId
				WHERE A.userId = 3
UNION
SELECT U.NAME, A.MONEY
	FROM `user_info` as U
		RIGHT JOIN `user_account` as A
			ON A.userId = U.userId
				WHERE A.userId = 3;
```



### 1.3.9 独立部分并

```mysql
SELECT U.NAME, A.MONEY
	FROM `user_info` as U
		LEFT JOIN `user_account` as A
			ON A.userId = U.userId
				WHERE A.userId IS NULL
UNION
SELECT U.NAME, A.MONEY
	FROM `user_info` as U
		RIGHT JOIN `user_account` as A
			ON A.userId = U.userId
				WHERE U.userId IS NULL;
```



# 2. 范式

## 2.1 基础知识

**主键为候选键的子集，候选键为超键的子集，而外键的确定是相对于主键的**

### 2.1.1 视图是什么

视图是虚拟的表，与包含数据的表不一样，视图只包含使用时动态检索数据的查询；不包含任何列或数据。使用视图可以简化复杂的sql操作，隐藏具体的细节，保护数据；视图创建后，可以使用与表相同的方式利用它们。

视图不能被索引，也不能有关联的触发器或默认值，如果视图本身内有 order by ，则对视图再次 order by 将被覆盖。

## 2.2 第一范式

在任何一个关系数据库中，第一范式（1NF）是对关系模式的基本要求，不满足第一范式（1NF）的数据库就不是关系数据库。

 所谓第一范式（1NF）是指数据库表的每一列都是不可分割的基本数据项，同一列中不能有多个值，即实体中的某个属性不能有多个值或者不能有重复的属性。

如果出现重复的属性，就可能需要定义一个新的实体，新的实体由重复的属性构成，新实体与原实体之间为一对多关系。在第一范式（1NF）中表的每一行只包含一个实例的信息。

简而言之，**「第一范式就是无重复的列」**。

## 2.3 第二范式

第二范式（2NF）是在第一范式（1NF）的基础上建立起来的，即满足第二范式（2NF）必须先满足第一范式（1NF）。第二范式（2NF）要求数据库表中的每个实例或行必须可以被惟一地区分。为实现区分通常需要为表加上一个列，以存储各个实例的惟一标识。这个惟一属性列被称为主关键字或主键、主码。 

第二范式（2NF）要求实体的属性完全依赖于主关键字。所谓完全依赖是指不能存在仅依赖主关键字一部分的属性，如果存在，那么这个属性和主关键字的这一部分应该分离出来形成一个新的实体，新实体与原实体之间是一对多的关系。为实现区分通常需要为表加上一个列，以存储各个实例的惟一标识。

简而言之，**「第二范式就是非主属性非部分依赖于主关键字」**。

## 2.4 第三范式

满足第三范式（3NF）必须先满足第二范式（2NF）。简而言之，第三范式（3NF）要求一个数据库表中不包含已在其它表中已包含的非主关键字信息。

例如，存在一个部门信息表，其中每个部门有部门编号（dept_id）、部门名称、部门简介等信息。那么在员工信息表中列出部门编号后就不能再将部门名称、部门简介等与部门有关的信息再加入员工信息表中。如果不存在部门信息表，则根据第三范式（3NF）也应该构建它，否则就会有大量的数据冗余。

简而言之，**第三范式就是属性不依赖于其它非主属性**。

