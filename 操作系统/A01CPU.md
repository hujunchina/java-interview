2020年5月23日 19:39:32

[TOC]

# 1. 多线程

## 1.1 综述

### 1.1.1并发挑战

- 上下午切换：并发比单线程慢，上下文切换频繁
- 死锁：四大条件
- 资源限制：并行变串行执行（同步）

### 1.1.2 线程状态

程序中执行的控制流
线程优先级：setPriority() 设置，默认是5，范围1到10
线程中断

## 1.2 CPU细节

https://www.infoq.com/presentations/Lock-Free-Algorithms

### 1.2.1 CPU结构：L1-L2-L3   MQ  DRAM

### 1.2.2 缓存器Cache

Cache分类：

- 寄存器：存储一次一个CPU周期，时间极短
- L1 Cache：3~4周期，0.5~1ns
- L2 Cache：10~20周期，3~7ns
- L3 Cache：40~45周期，15ns
- 跨槽传输：无，20ns
- 内存：120~240周期，60~120ns

Cache line：为了高效的存取缓存，不是简单地随意地将单条数据写入缓存，缓存是由Cache line构成存储的最小单位，intel平台一般是64字节。读写最小单位，64字节

### 1.2.3 Cache  entry：

- cache  line，最小单位
- tag，标记cache line对应的主存地址
- flag，标记是否有效

### 1.2.4 Cache 策略:

   				1. CPU通过cache间接访问主存，不直接访问
   				2. 遍历一遍全部cache line，主存地址是否在其中
   				3. 如果未找到，新建一个entry把line写入，再读取到CPU中

### False Sharing:

在多处理器，多线程的情况下，如果两个线程分别运行在不同CPU上，而其中某个线程修改了cache line中的元素，由于cache一致性原因，另一个线程的cache的line被宣告无效，在下一次访问时会出现一次cache line miss。

 1. 多线程下，A修改了entry中的line
 2. 由于缓存一致性，导致B访问无效
 3. core之间的cache就会一次复制，影响效率
 4. 解决方法：构建一个line长度的对象，64字节=对象头2字节+long8字节*7+6字节空余

## 1.3 实验

1. 查找占CPU最高的线程
  1. top -Hp  进程端口 找到最高的线程nid
  2. jstack 进程端口  找到对应的nid即可
2. 查看一个线程运行了多久
  1. ps -p  pid -o  etime
3. 统计进程中线程数
  1. ps hH p pid |  wc -l
  2. /proc
  2. cat  /proc/pid/status
4. 查看某个进程中的线程
  1. ps -T -p  pid
  2. top -H  -p  pid