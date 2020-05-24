2020年5月23日 16:19:19

[TOC]

## 1. FullGC情况

### 1.1 一个案例

```java
package cn.edu.zju.a06jvm;

import com.alibaba.algorithm.zuochengyun.Question;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Question(
        question = "模拟FullGC问题，并调优",
        condition = "从数据库中读取信用数据，套用模型，并把结果进行记录和传输",
        solution = "java -Xms:30MB -Xmx:30MB -XX:+PrintGC file"
)
public class A05FullGCProblem {
    private static class CardInfo{
        BigDecimal price = new BigDecimal(0.0);
        String name = "张三";
        int age = 30;
        Date birthDate = new Date();

        public void m(){}
    }

    private static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
            50,
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );

    private static void modelFit(){
        List<CardInfo> takeCard = getAllCardInfo();
        takeCard.forEach( cardInfo -> {
            executor.scheduleWithFixedDelay(
                    ()->{ cardInfo.m();},  // Lambda runnable
                    2, 3, TimeUnit.SECONDS
                    );
        });
    }

    private static List<CardInfo> getAllCardInfo() {
        List<CardInfo> cardInfos = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            CardInfo cardInfo = new CardInfo();
            cardInfos.add(cardInfo);
        }
        return cardInfos;
    }

    public static void main(String[] args) throws InterruptedException {
        executor.setMaximumPoolSize(50);

        for(;;){
            modelFit();
            Thread.sleep(100);
        }
    }
}
```

#### 1.1.1 目的与现象

线程池操作，从数据库读取大量信息，并放入模型（一个方法）中处理，返回结果。

分配30MB，内存不够用，提示Allocation Failure 不断地 Full GC，无法工作。

### 1.2 调试方法

#### 1.2.1 看GC分配情况

`-XX:+PrintGC`  或 `-XLog:gc`   先不断的监视GC情况才能发现问题 。

`-XX:PrintCommandLineFlags` 打印项目问题。

#### 1.2.2 查看线程

- `jps` 直接返回所有运行的 Java 的线程
- `top -Hp pid` 动态监控该线程的CPU和内存占用
- `ps a` 查看线程情况

ps 就是 process status 的缩写，即进程的状态。

#### 1.2.3 看JVM状态

拿到pid后 可以使用 `jstat -gc pid 1000` 动态查看JVM状态了，表示每隔 1 秒就打印出 gc 的状态信息。

也可以使用 `jinfo` 查看JVM相关参数时候有异常。

#### 1.2.4 分析线程问题

`jstack pid`  打印出线程栈信息和所有线程信息，可以看到线程运行状态和是否发生死锁。

`jviusalvm` 一个提供图形界面的信息窗口，但在生产环境下没法使用

`arthas`  一款功能强大的开源调试工具，因为是基于java编写的运行还是要用jvm，所以当jvm快要崩溃的时候是无法进入无法调试的。

其提供了一些指令：

- `dashboard`  动态显示CPU和内存的变化
- `thread pid`   可以通过指定线程pid 直接看到该线程栈的情况
- `sc`  search class 找到该类加载的所有方法和类信息
- `trace`  跟踪方法调用的过程 和调用的时间
- `heapdump` 打印堆信息，实际生产中不能使用

#### 1.2.5 分析内存情况

` hmap -histo  pid | head -20`  生成内存分布图，把当前时刻内存中所有的实例打印出来。多次间隔打印，查看实例最多的方法或对象，就是要排查的大头。生产环境不能随便用，影响大至卡顿，电商不适用，但很多服务器备用（高可用），这台卡顿没多大影响，可以用于测试。

`arthas heapdump` 记录好日志，怀疑那块就跟踪那块，解决，jad 动态反编译，排查版本问题，redefine 热替换class文件。

### 1.3 解决办法

#### 1.3.1 一个是CPU飙升

CPU问题肯定是线程引起的，查找是否线程开过多，或线程阻塞，或CAS操作。

通过 `jstack` 查看。

#### 1.3.2 一个是OOM

OOM 即out of memory 超过了内存区域，内存溢出。先推断内存设置的是否过小。

还有就是内存是否被容器分配完而没有及时释放，GC问题（频繁GC）。

通过`jmap` 打印出堆内存。

### 1.4 设置日志文件

日志文件是保护案发现场最好的工具，也是案后分析问题的最有力的依据。在设置日志是有技巧的，不能一概设置一个文件而是要多设置几个文件轮流存储。不然日志会太大打不开。

```shell
-Xloggc:/var/log/javagc/my-log-%t.log
-XX:+UseGCFileRotaion
-XX:NumberOfGCLogFiles=5
-XX:GCLogFileSize=20M
-XX:PrintGCDetails
-XX:PrintGCDetailStamps
-XX:PrintGCCause
-XX:HeapDumpOnOutOfMemoryError
```

