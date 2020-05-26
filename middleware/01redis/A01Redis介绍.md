2020年5月24日 16:19:17

[TOC]

# 1. Redis 是什么

Redis 是非关系数据库（NoSQL）的一种，与传统的数据库的区别是数据存储在内存中而不是硬盘中。

Remote dictionary server （Redis）

## 1.1 Redis 特性

经常面试被问为什么是单线程而不是多线程等。

### 1.1.1 基本特性

存储在内存中，以K-V键值对形式存储，支持 master-slaver 主从模式。

支持数据的持久化：快照RDB和日志AOF。

### 1.1.2 单线程模型

Redis 的 Work 线程即读写修改的线程是单线程模式，而不是多线程模式。设计为单线程是因为 Redis 本身是内存存储的，速度很快了，性能瓶颈不单单在线程上了，如果再使用多线程并不能提高多少性能，而且有可能因为多线程间上下文的切换导致性能下降。

### 1.1.3 IO Threads 多线程模型

Redis 使用 NIO 模型进行 IO 操作，当多个 Client 请求连接时，Redis 使用多线程进行数据的传入与发出。

# 2. Redis 基本类型

Redis 提供了丰富的类型，可以快速简单实现一些复杂的操作。

## 2.1 String 类型

Redis 的 String 类型与 Java 的不同，这里包含 整型和 位图等结构。

### 2.1.1 字符串

​	set key value
​	get key
​	append key value
​	getrange key start end
​	setrange  key  offerset  value
​	getset key value
​	setnx key value（set  if  not  exist，如果不存在就设置，存在就不改动）
​	setex  key  secends value（set key expire secends）
​		设置key并设置其过期时间，单位秒
​		psetex，以毫秒为单位
​	mset，mget，msetnx 对多个key操作

使用场景：基本存储、缓存数据

存储形式：以字符形式存储，strlen 得到的是字符长度，object encoding key，type key

### 2.1.2 数值

以字符形式存储，二进制安全，与语言无关的存储形式。

应用场景：

- 统计（incr key， decr key）
- 点赞（incr key，decr key）
- 限流（使流量成倒三角，进入流量很大，到数据库流量很小，redis做大量记录，最后写入数据库，短时间频繁更改的数据）

### 2.1.3 BitMap 位图

操作：

- setbit  key  1 1 （把key的第一个bit 置为1）
- getbit  key  1 （得到key 的第一个bit的值）
- bitcount  key  （返回多少个1 ）
- bitop  or  res k1  k2（ 按位或）
- bitfield （把Redis字符串当作位数组，	然后当做位操作）

存储形式： string 0000 0001

应用场景：

- 大数据统计
- 任意时间窗口，用户登录多少天
  	用户A在一年内登录了多少次？
    	用uid做key，天数做位
    	setbit   uidA   2   1  表示2号登陆一次
    	setbit  uidA  365 1  表示365天登陆一次
    	bitcount uidA 0 -1   表示登录的次数
- 任意时间窗口，活跃用户数
  	上个星期，用户活跃度是多少？
    	用时间做key， 用uid做位
    	setbit 20200414 uidA 1  表示用户A在这天登录
    	setbit 20200414 uidB 1 表示用户B在这天登录
    	bitop or res keys... 把一个星期天数按位或叠加
    	bitcount res  得到用户活跃度（去重后的）

## 2.2 List 类型

### 2.2.1 左操作

- lpush：元素插入左边
- lpushx：元素插入列表头部，如果list为空，报错
- lpop：从list左边弹出一个元素，如果list为空，返回nil

### 2.2.2 右操作

- rpush：右边插入元素，list的存储顺序和插入顺序一样
- rpushx：与lpushx类似
- rpop：与lpop类似
- rpoppush  k1  k2：原子操作，将k1最右边元素弹出，并将该元素插入到k2头部，返回该元素
  		k1=a，b，c  ； k2=x，y，z
    		k1=a，b  ；k2=c，x，y，z

### 2.2.3 List操作

- lrange  klist  start end：输出list元素，0 -1 表示所有元素，实现了逆序
- llen klist：list长度
- lindex  key  index：返回index+1位置的元素，index可以用负数，从0开始
- linsert key before/after value  value
  		把 value 插入存于 key 的列表中在基准值 pivot 的前面或后面
    		当 key 不存在时，这个list会被看作是空list，任何操作都不会发生
    		当 key 不存在时，这个list会被看作是空list，任何操作都不会发生
- lrange  key  start end：返回范围内的元素，包括两端点
- lrem key count  value
  		移除count次出现的value（count个）
    		count>0 从前数，count<0 从后数，count=0 全部
    		lrem k -2 "h"  移除最后两个h
- ltrim key start end：截取从start到end范围内的元素，其他的删除
- lset  key index value：index超出范围报错

### 2.2.4 阻塞操作

可以应用到事件提醒中，当有元素时不阻塞直接返回，但无元素时阻塞直到有元素插入

- BLPOP key1  keyn   timeout
  		lpop阻塞版，对多个list弹出，有时间限制，事件提醒
- BRPOP key1 keyn  timeout ：rpop阻塞版
- BRPOPPUSH  src   dest  timeout：可靠的队列，循环列表

### 2.2.5 底层实现

底层是quicklist，一个双向链表，以list存储。

应用场景：

- list可作为一个列表存储多个数据
- 评论列表，商品聚合列表
- 事件提醒
- 循环列表，可实现栈和队列等数据结构

## 2.3 Hash 类型

### 2.3.1 基本操作

- hset key  field  value
  		hashmap的key变成了这里的field
    		对redis来说就是多一个字段实现hash
- hget  key  field
- hdel  key   field
- hexits  key  field
- hgetall  key：返回所有值
- hlen  key：返回个数
- hkeys  key：返回所有field
- hvals key：返回所有value

### 2.3.2 其他操作

- HINCRBY key field increment：自增多少
- HINCRBYFLOAT key field increment：自增浮点型
- hmget：多值获取
- hmset：多值添加
- hsetnx：不存在创建，存在无效果
- hstrlen：返回value长度

### 2.3.3 底层实现

底层使用ziplist实现，ziplist，hash

应用场景：

- 商品详情页-集合数据（评论，商品，用户）
- 统计值：点赞，粉丝，评论

## 2.4 Set 类型

### 2.4.1 基本操作

- sadd key  member：添加成员
- scard key：返回key的基数即个数
- sismemeber key member：判断是否有这个成员，存在返回1，不存在返回0
- smembers key：返回所有成员
- srem  key member：移除某个成员

### 2.4.2 其他操作

- smove  src  dest  member：从A到B移动成员
- spop key count：从集合中随机弹出元素，可用于抽奖
- srandmember  key  count
  		返回count个成员，但不删除
    		count为正，超出长度，返回长度个成员
    		count为负，超出长度，重复出现成员

### 2.4.3 集合操作

- sdiff key1  key2：返回集合间的差集，可用于推荐系统
- sdiffscore res  key1  key2：将差集存储在res中
- sinter  key1  key2：返回集合间的交集，用于公共部分检索，共同买的商品
- sinterscore res  key1  key2：将交集存储在res中
- sunion  key1  key2：返回集合间并集，统计所有买的商品
-  sunioncore res  key1  key2：将并集存储在res中

### 2.4.4 底层实现

使用hashtable存储，成员就是其key，value为固定值。set。

应用场景：

- 抽奖
- 推荐
- 找关系

## 2.5 ZSet 类型

### 2.5.1 基本操作

- zadd key score member
- zcard  key：统计个数
- zcount key min  max：score在范围内的个数
- zrange key  start  end

### 2.5.2 其他操作

- ZINCRBY key increment member：自加
- ZINTERSTORE dest numkeys key，把更新后的值保存在dest中，zrank。

### 2.5.3 底层实现

底层实现：
	skiplist原理：多层双向链表，通过不同节点比较，跳跃找到插入的位置。

存储形式：ziplist压缩表，当value大于64个或长度大于128时，转为skiplist跳跃表。

应用场景：

- 数据分页，固定大小10个
- 排行榜

# 3. Redis 内存模型

## 3.1 Redis 内存统计

使用命令：`info memory`

- used_memory：redis分配的内存包括变量的存储内存和虚拟内存（swap）（硬盘）
- used_menory_human,  转为人方便看的格式
- used_memory_rss，Redis 进程占操作系统的内存，与top看到的值一样，包括内存碎片，进程运行时的内存
- memory_fragmentation_radio：内存碎片比率，一般大于1的，1.03比较健康，used_memory_rss / used_memory 比值，小于1，说明使用了虚拟内存（硬盘）速度就很慢。排查原因，内存不足，增加 Redis节点，增加内存，优化变量等。

## 3.2 内存划分

1. 数据，五大数据类型，占用统计在 usedmemenory 中，并不是直接存储到内存中，是以特定的数据结构存储，redisObject SDS 等存储。
2. 运行时内存，如代码、常量池等。
3. 缓存内存，由 jemalloc 分配，所以统计在 usedmemory中，
  1. 客户端缓冲区，用户输入输出命令
  2. 复制积压缓冲区，用于部分复制功能
  3. Redis AOF 缓冲区，用于AOF复制时的命令
4. 内存碎片，Redis 释放的内存空间在物理内存中没有释放，可以通过安全重启方法解决，重新加载数据并为数据分配适合的内存区域。

## 3.3 数据存储细节

### 3.3.1 实体类型概述

涉及到jemalloc内存分配器、简单动态字符串SDS，redisObject，类型内部编码等

dicEntry类型包括三部分：

- void* key， sds类型，存储了"hello“，并不是直接存储的，而是包装成sds
- void* val，redisObject类型：
  - unsigned type（string）指明了key的类型，也是 type 关键字返回的结果
  - void* ptr，指向内存存储地址，这里还是指向一个 sds类型，“world”
    也不是直接存储的，而是包装成 redisObject 类型
- struct dicEntry* next，下一个键值对引用

### 3.3.2 Jemalloc 概述

把内存分为 small，large，huge 三种大小。

small 8bit 到512bit
large 是 4KB
huge 是4MB
其中单元大小是按照5倍增加的。

### 3.3.3 redisObject类型

对象类型、内存编码、内存回收、共享内存功能。

- unsigned  type:4，基本5大类型标识
- unsigned encoding:4，对类型进行编码方式标识
- unsigned lru:REDIS_LRU_LEN，对象最后一次被访问的时间
- int refcount，对象被引用的次数，为0被回收
- void*  ptr，指向具体数据

一共占用16byte内存，4bit+4bit+24bit+4byte+8byte=16byte字节。

### 3.3.4 SDS 类型

Redis 未直接用C语言的字符串存储，而是定义了一个数据结构

- int len：已经使用长度
- int free：未使用长度
- char buf[]：具体数据，长度，数组长度=len+free+1，SDS占内存大小len+free+1+4+4

优势：一步获取字符串长度，防止缓冲区溢出，空间预分配，存取二进制，可以是空值情况。

## 3.4 对象类型内部编码

内部编码在写入数据时完成，不可逆，只能从小内存编码转到大内存编码。

### 3.4.1 字符串

长度不超过 512MB。
内部编码：

- int：存储整型的数
- embstr：≤39字节的字符串，64-redisObject16-sds9=39，一次分配内存，redisObject和SDS连续，只读
- raw：＞39字节，sds=9+free+len，二次内存分配，redisObject和SDS不连续
- 编码转换：int不在是整数或超过long范围转为raw，修改embstr转为raw，才修改。

### 3.4.2 列表

存储 2^32-1 个元素，充当栈|队列|数组
内部编码：

- 压缩列表 ziplist，连续内存块，有点像arraylist数组
- 双端链表 linkedlist，真链表
- 编码转换：元素小于512个 && 元素长度不超过64字节，不满足转为双端链表

### 3.4.3 哈希

内部编码：

- 压缩列表 ziplist，元素个数少，长度小
- 哈希表 hashtable，dict，dictht，dictEntry数组（桶）

编码转换：元素小于512个 && 元素长度不超过64字节，不满足转为哈希表。

### 3.4.4 集合

无序+不能重复
内部编码：
		整数集合 intset
		哈希表 hashtable
编码转换：
		元素小于512个 && 所有元素都是整数，不满足转为哈希表

### 3.4.5 有序集合

有序+不重复，使用score 来排序
内部编码：
	压缩列表 ziplist
	跳跃表 skiplist，通过在每个节点中维护多个指向其他节点的指针达到快速访问节点的目的
编码转换：元素小于128个 && 长度不足64字节，不满足转为跳跃表。

# 4. Redis 持久化

## 4.1 Redis 高可用概述

一般高可用：一段时间内正常服务的时间占比；
Redis 高可用：除了正常服务，还包括数据安全不丢失；
高可用实现：
  		1. 持久化：将数据备份到磁盘，永久保存
    		2. 复制：3和4的基础，数据的多机备份，读操作可以负载均衡，可以手动还原数据，故障恢复
      		3. 哨兵：在复制基础上，实现了自动化故障恢复，读负载均衡，但写不负载均衡
        		4. 集群：在哨兵的基础上，解决了写负载均衡，升级为较完整的高可用方案

## 4.2 Redis 持久化概述

把内存中数据保存到硬盘上，分为 AOF 和 RDB 两种。

## 4.3 AOF 日志持久化

AOF 日志持久化：类似binlog，每次执行的写命令保存到硬盘，保存的是命令，通过再次执行命令恢复。
特点：只保存命令，快速，实时性好，，append only files，不需要触发；支持秒级持久化，启动时加载，效验，伪客户端。

执行流程：
		命令追加：写命令写到 aof_buf 缓冲区，后面写入才到硬盘文件
		文件写入和同步：不同同步策略写入到硬盘
				always
					立刻写入文件中，不放入缓冲区，fsync
				no
					由操作系统负责同步缓冲区到文件
				everysec
					每秒执行一次fsync同步数据到文件
		文件重写：定期重写 AOF 文件，达到压缩目的
				手动触发：bgrewriteaof 命令，fork子进程操作
				自动触发：配置参数，auto-aof-rewrite-min-size，auto-aof-rewrite-percentage

## 4.4 RDB 快照持久化 

RDB 快照持久化：将当前时刻的所有数据保存到硬盘，使用LRZ算法压缩，体积小。
手动触发
		sava
			阻塞服务进程，直到保存结束
				线上环境禁止使用
		bgsave
			非阻塞，创建子进程来保存数据
			执行流程

   				1. 父进程判断是否有其他bgsave，要确保一次只执行一个bgsave，只能fork一个子进程
   				2. 父进程fork创建子进程，短暂阻塞
  				3. 子进程创建rdb文件，并把内存数据写入文件
 				4. 子进程完成操作，发送消息给父进程

自动触发：通过在redis.conf配置生效，save m n，在m秒内发生了n次变化，触发bgsave
原理：

- serverCron函数，每100ms执行一次，检查是否达到save条件
- dirty计数器，记录上次gbsave后，多少次修改
- lastsave时间戳，记录上一次bgsave成功执行的时间
- 当前时间-lastsave>m && dirty>=n 才触发

其他：在主从复制场景下，如果从节点执行全量复制操作，则主节点会执行bgsave命令，并将rdb文件发送给从节点，执行shutdown命令时，自动执行rdb持久化。

# 5. Redis 主从复制

## 5.1 概述

主从复制概述：单向，只能主到从，一个从节点只能有一个主节点
作用：数据冗余，故障恢复，负载均衡，高可用基石，配合读写分离，主写从读。

## 5.2 实现原理

实现原理

1. 建立连接：保存主节点信息，建立socket连接，发送ping命令，身份验证，发送从节点端口信息

2. 数据同步：全量复制，将主节点所有数据发送过来保存，主节点bgsave好rdb文件发送给从节点，

   部分复制：复制偏移量，复制积压缓冲区，服务器运行ID

3. 命令传播：心跳机制；主到从ping；从到主 replconf back；发送写命令。

# 6. Redis 哨兵

## 6.2作用和架构

核心功能：主节点自动化故障转移

- 监控：检测主从节点正常运行
- 自动故障转移：将从节点升级为新主节点
- 配置提供者：客户端通过哨兵获取主节点地址
- 通知：转移结果发给客户端

## 6.3 原理

选举主节点至少需要一半以上哨兵同意，所以哨兵至少要3个，哨兵只需要配置一个主节点ip，就能自动发现所有从节点和其他哨兵