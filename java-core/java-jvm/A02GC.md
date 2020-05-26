2020年5月22日 12:25:32

[TOC]

### 1. JVM 内存划分

一、JVM运行数据区域（内存划分）

1.  程序计数器：一个寄存器空间，存储线程执行当前指令的地址。
2.  JVM栈：一块内存区域，方法一定是对方法而言，是方法执行的内存模型，方法从执行到结束表示创建栈帧入栈到出栈的过程。
3.  栈帧：包括局部变量，操作栈，动态链接，方法出口等。
4.  本地方法栈：是native方法的内存模型。
5.  JVM堆：一大块内存区域且线程共享，存储对象实例和数组，GC活动主要区域。由于GC采用分代收集算法，从内存回收角度细分，可以再分为新生代和老年代，新生代再分为Eden区Survivor#1区Survivor#2。
6.  方法区：一块内存区域且线程共享，存储类加载信息|常量|静态变量|编译后的代码，GC在此区域做常量池回收和类型卸载。
7.  运行时常量池：存放编译期生成的字面值String和符号引用，使用String的intern方法可以检测有没有存储这个字面值，有就直接返回，没有就在常量池中创建。

#### 1.1 JVM对象实例化过程

```java
Object obj = new Object();
```

1.  在JVM堆中创建Object实例，并把结构化内存的首地址返回给obj存储到JVM栈帧中。
2.  实例只是一些数值，还需要Object类的类型定义才知道每个成员变量的内存范围。
3.  所以需要在实例中再创建一个引用指向，方法区对象类型数据（类定义）。

![img](C:\code\github\java-interview\img\jvm-7.png)

### 2. 垃圾回收器与内存分配策略

两种判断Rechable和Unrechable对象的方法：

- 引用计数算法：引用此对象实例计数器就加一，引用失效就减一；但问题是很难解决对象之间的循环引用问题。相互依赖，关联关系，各自把对象引用作为成员变量，此时双方的计数器均为1，当两个对象置null时，计数器无法减为0，因为对象的引用未失效。

- ```java
  obj1.val = obj2;
  obj2.val = obj1;
  obj1 = null;
  obj2 = null;
  //但是val的引用未失效，计数器还是1
  //obj1.val = null;
  //obj2.val = null;
  ```

- 根搜索算法：以图论思想来检测不可达的节点，此时节点即对象实例，边或路径即引用链接。从GCroots很多个根节点出发，这些根节点可为栈中的引用对象，方法区静态变量引用对象，方法区常量引用对象，Native方法引用对象。

四种引用类型（一种优化，如果内存很大，可以保存，如果小清除）

- 强引用：普通引用
- 软引用：还有用但非必需对象，内存不足，会清除对象实例回收内存
- 弱引用：非必需对象，强度比软引用弱；无论内存如何都会回收内存
- 虚引用：最弱引用，唯一目的是在对象被清除时收到GC通知

为什么Eden和两个survivor区内存分配大小为8：1：1？

经验总结，一般Eden区回收垃圾最大可达70%~95%之多，所以剩下的可达对象实例完全可以放在survivor中。

类卸载满足条件：

- 类所有实例已被回收，没有实例了
- 加载该类的ClassLoader已经被回收
- Class对象没有被引用，无法反射访问Class任何方法

### 3. GC算法

标记清除法（mark-swap）：标记所有清除对象然后清除。两个问题：

- 效率 - 标记和清除都不高
- 空间 - 产生大量不连续碎片，空间碎片多无法再次分配大内存给对象实例，会再次触发垃圾回收

标记复制算法：就是把Rechable对象复制到servivor，然后Eden区全清除，一开始设计的是5：5，后来根据经验改进为8：1：1。

标记整理法（mark-Compact）：

用于老年代GC清理，先标记存活对象，然后把这些对象整理到一端，集中起来，最后把边界以外的unrechable对象清理。

所以总体选择分代回收方式，新生代用标记复制算法，老年代用标记整理算法。

![img](C:\code\github\java-interview\img\jvm-8.png)

Serial收集器：单线程版的收集器，会让所有工作线程停止运行。

![img](C:\code\github\java-interview\img\jvm-9.png)

Parallel New收集器：多线程并行的收集器，可以和CMS合作

![img](C:\code\github\java-interview\img\jvm-10.png)

Parallel Scavenge觅食收集器：复制算法并行多线程新生代收集器，特别处是可以精准的控制吞吐量。

![img](C:\code\github\java-interview\img\jvm-11.png)

Concurrent Mark Sweep CMS收集器：多线程并发老年代收集器，特别处是获取最短回收停顿时间，使用标记清除法。

![img](C:\code\github\java-interview\img\jvm-12.png)

Garbage first G1收集器：使用标记整理老年代垃圾，没有空间碎片；精确控制停顿，不牺牲吞吐量的前提降低回收停顿。

G1对新生代minor gc采用复制算法，从Eden复制到Survivor，每次复制增加一次年龄，当到达预设值后移到老年代

G1对老年代mixed gc采用标记整理算法，当mixed gc无法使用就使用fullgc，注意fullgc效率很低，再注意mixed gc会同时清理新生代和老年代，这和minor gc不冲突，因为我们设置了一个垃圾占比值，先用minor gc处理，当达到占比值时使用mixed gc处理。

mixed gc前需要并发标记：

1.   Initial marking phase: 标记GCRoots(会STW)；
2.   Root region scanning phase：标记存活Region；
3.   Concurrent marking phase：标记存活的对象；
4.   Remark phase：重新标记(会STW)；
5.   Cleanup phase：回收内存。

### 4. 补充

为什么垃圾回收要采用分代模式？ 减少Full GC的次数（FullGC就是Major GC）

#### 4.1 垃圾回收10种模式

1. 早期 Serial + Serial Old：单线程清除垃圾，停顿时间长，淘汰了（建个虚拟机分配单核CPU，默认Serial！）

2. 改进 Parallel Scavenge + Parallel Old：JDK 1.8 默认的组合PS+PO，简称UseParallel；多线程并发回收垃圾，无法清除浮动垃圾；复制算法+标记整理，一次回收几个GB。
3. 调优方向ParNew + CMS：把默认组合调参成这种组合，如果调不了最优就用G1，并发垃圾回收不搞STW，但有浮动垃圾；并发，停顿时间短为目标；标记时还是STW， 复制算法+标记压缩，一次回收几十个GB
   - 四个阶段
     		初始标记：STW很短，只找到ROOT对象，不相信索图
       		并发标记：不STW，与工作线程并发执行，索图标记对象
       		重新标记：STW短，修正标错或漏标的，必须重头在重新扫码一遍
       		并发清理：清除垃圾，会产生浮动垃圾，碎片化严重，最后调用serial old来清理，触发FULLGC
     PN和PS差不多，为了配合CMS而产生了PN
4. 新思维G1：基于分代的GC无论如何调都无法最优，内存空间越来越大，所以G1产生以局部清理为主，而不是全部的空间区域，不用分代
   思想：G1把内存分为很多小的区域称为Region，这样按区域回收就不会STW
   并发标记使用三色标记算法
5. 颜色指针 ZGC：G1缺点，结构占内存多；前18位不用，中间分为四段，后面42为做内存
   四个颜色指针
   		1.marked 0  已标记
   		2.marked 1  已标记
     		3. remapped  正在移动的对象，产生一个读屏障，当对象移动完成再标记
                   	4. fianalizable 销毁对象
                  支持内存大小：2的42次方是4TB内存，最大可支持16TB，44次方，因为现代计算机最大总线有48条，不是64条，考虑经济因素，所以48-4（个指针）=44次方，CPU根据指令总线还是数据总线区分内存传过来的是数据还是指令。

#### 4.2 如何标记的并防止错标漏标？

##### 三色标记法

​	白色：未被标记对象
​	灰色：自身被标记，成员变量未被标记
​	黑色：自身和成员变量均已标记完成

![image-20200522182716413](C:\code\github\java-interview\img\jvm-14.png)	漏标，A黑色标记完了，但又重新指向D，GC以为D是白色的没有标记，就清除了，漏标 
CMS处理方式： Incremental  Update 增量更新；把A再次变成灰色（写屏障）这样重新标记时会再扫码A。

![image-20200522182740734](C:\code\github\java-interview\img\jvm-16.png)并发标记，产生漏标：ABA问题，线程1把A标称黑色，线程2发现A有连接又把A标位灰色，但线程1是在2后执行的，最后A是黑色的。所以重新标记必须重新扫描一遍。
G1处理方式：SATB，snapshot at the beginning  初始快照；对断开的引用对象快照，保存到堆栈区，当再次扫码时，去检测堆栈是否为空；后续GC重点优化的地方，时间长待优化。

### 5. JVM命令

#### 5.1 命令类型

-命令：直接用，正规命令
-X命令：非正规命令，不太稳定
-XX命令：下个版本可能淘汰
JVM调整命令

-命令：直接用，正规命令
-X命令：非正规命令，不太稳定
-XX命令：下个版本可能淘汰
JVM调整命令：1. -XX:+UseTuning

#### 5.2 常用查看命令

-XX:+PrintCommandLineFlags：打印命令行参数，可以查看堆大小，使用了什么GC

```
-XX:InitialHeapSize=131714624 
-XX:MaxHeapSize=2107433984 
-XX:+PrintCommandLineFlags 
-XX:+UseCompressedClassPointers 
-XX:+UseCompressedOops 
-XX:-UseLargePagesIndividualAllocation 
-XX:+UseParallelGC 
```

-XX:+PrintFlagsInitial，默认值，初始化值
-XX:+PrintFlagsFinal，设置值（最终生效的值）
-XX:+PrintGCDetails，打印GC详细信息
-XX:+PrintGC，查看使用了什么GC；更新：-Xlog:gc instead

#### 5.3Parallel常用参数

-XX:ServiorRatio：存活区占比
-XXPreTenureSizeThreshold：设置大对象大小
-XX:MaxTenuringThreshold：调整年龄
-XX:+ParallelGCThreads：并行收集器线程数
