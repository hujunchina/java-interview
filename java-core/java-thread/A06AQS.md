2020年5月21日 15:34:27

### 目录

1. AQS 是什么
2. AQS 原理
3. AQS 源码
4. AQS 总结

### 1. AQS 是什么

#### 1.1 note

aqs 队列同步器 abstractqueuedsynchronizer, 处理同步，是并发锁的基础原理，锁都没有直接继承 AQS，而是定义了一个 Sync 类去继承 AQS。因为锁面向的是使用用户，而同步器面向的则是线程控制。API: 三个获取，一个得到。

#### 1.2 profile

在同步组件的实现中，AQS是核心部分，同步组件的实现者通过使用AQS提供的模板方法实现同步组件语义，AQS则实现了对**同步状态的管理，以及对阻塞线程进行排队，等待通知**等等一些底层的实现处理。AQS的核心也包括了这些方面:**同步队列，独占式锁的获取和释放，共享锁的获取和释放以及可中断锁，超时等待锁获取这些特性的实现**，而这些实际上则是AQS提供出来的模板方法。



### 2. AQS 原理

#### 2.1 note

获取独占锁：先尝试获取同步状态state，如果获取同步状态成功(加锁了)，则结束方法，直接返回。

将当前节点设为头节点。如果获取同步状态不成功(被其他线程占用)，AQS不断尝试利用CAS将当前线程插入等待同步队列的队尾，直到成功为止。

接着，不断尝试为等待队列中的线程节点获取独占锁。检测是否为头节点。

#### 2.2 同步队列

当共享资源被某个线程占有，其他请求该资源的线程将会阻塞，从而进入同步队列。就数据结构而言，队列的实现方式无外乎两者一是通过数组的形式，另外一种则是链表的形式。AQS中的同步队列则是**通过链式方式**进行实现。接下来，很显然我们至少会抱有这样的疑问：**1. 节点的数据结构是什么样的？2. 是单向还是双向？3. 是带头结点的还是不带头节点的？**我们依旧先是通过看源码的方式。

```java
volatile int waitStatus //节点状态
volatile Node prev //当前节点/线程的前驱节点
volatile Node next; //当前节点/线程的后继节点
volatile Thread thread;//加入同步队列的线程引用
Node nextWaiter;//等待队列中的下一个节点
int CANCELLED =  1//节点从同步队列中取消
int SIGNAL    = -1//后继节点的线程处于等待状态，如果当前节点释放同步状态会通知后继节点，使得后继节点的线程能够运行；
int CONDITION = -2//当前节点进入等待队列中
int PROPAGATE = -3//表示下一次共享式同步状态获取将会无条件传播下去

private transient volatile Node head;
private transient volatile Node tail;
```

AQS实际上通过头尾指针来管理同步队列，同时实现包括获取锁失败的线程进行入队，释放锁时对同步队列中的线程进行通知等核心方法。其示意图如下：

![image-20200521154616197](C:\code\github\java-interview\img\thread-16.png)

通过对源码的理解以及做实验的方式，现在我们可以清楚的知道这样几点：

1. **节点的数据结构，即AQS的静态内部类Node,节点的等待状态等信息**；
2. **同步队列是一个双向队列，AQS通过持有头尾指针管理同步队列**；

### 3. AQS 源码

```java
独享锁API
public final void acquire(int arg)获取独占锁
public final void acquireInterruptibly(int arg)获取可中断的独占锁
区别仅在于它会通过 Thread.interrupted 检测当前线程是否被中断，如果是，则立即抛出中断异常
public final boolean tryAcquireNanos(int arg, long nanosTimeout)
public final boolean release(int arg)
tryAcquireNanos - 尝试在指定时间内获取可中断的独占锁。在以下三种情况下回返回：
在超时时间内，当前线程成功获取了锁；
当前线程在超时时间内被中断；
超时时间结束，仍未获得锁返回 false。
区别在于它会根据超时时间和当前时间计算出截止时间。
在获取锁的流程中，会不断判断是否超时，如果超时，直接返回 false；
如果没超时，则用 LockSupport.parkNanos 来阻塞当前线程
release - 释放独占锁。
先尝试获取解锁线程的同步状态，如果获取同步状态不成功，则结束方法，直接返回。
如果获取同步状态成功，AQS 会尝试唤醒当前线程节点的后继节点。

#共享锁 API
public final void acquireShared(int arg)
public final void acquireSharedInterruptibly(int arg)
public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
public final boolean releaseShared(int arg)
acquireShared - 获取共享锁。
acquireShared 方法和 acquire 方法的逻辑很相似，区别仅在于自旋的条件以及节点出队的操作有所不同。
成功获得共享锁的条件如下：
tryAcquireShared(arg) 返回值大于等于 0 （这意味着共享锁的 permit 还没有用完）。
当前节点的前驱节点是头结点。
acquireSharedInterruptibly - 获取可中断的共享锁。
tryAcquireSharedNanos - 尝试在指定时间内获取可中断的共享锁。
release - 释放共享锁。
releaseShared 首先会尝试释放同步状态，如果成功，则解锁一个或多个后继线程节点。
释放共享锁和释放独享锁流程大体相似，区别在于：
对于独享模式，如果需要 SIGNAL，释放仅相当于调用头节点的 unparkSuccessor
```

#### 3.2 独占锁获取

```java
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            // 1. 获得当前节点的先驱节点
            final Node p = node.predecessor();
            // 2. 当前节点能否获取独占式锁					
            // 2.1 如果当前节点的先驱节点是头结点并且成功获取同步状态，即可以获得独占式锁
            if (p == head && tryAcquire(arg)) {
                //队列头指针用指向当前节点
                setHead(node);
                //释放前驱节点
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            // 2.2 获取锁失败，线程进入等待状态等待获取独占式锁
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```



### 4. AQS 总结

![独占式锁获取](C:\code\github\java-interview\img\thread-17.png)

![超时等待式获取锁](C:\code\github\java-interview\img\thread-18.png)

```java
static class ExclusiveLock {
}

static class Sync extends AbstractQueuedSynchronizer {
    protected boolean isHeldExclusive() {
        return getState() == 1;
    }

    public boolean tryAcquire(int acquires) {
        assert acquires == 1;
        if (compareAndSetState(0, 1)) {
            setExclusiveOwnerThread(Thread.currentThread());
            return true;
        }
        return false;
    }

    protected boolean tryRelease(int releases) {
        assert releases == 1;
        if (getState() == 0) {      // 该线程没有获得同步状态即未加锁，无法释放锁
            throw new IllegalMonitorStateException();
        }
        setExclusiveOwnerThread(null);      //把当前排他线程设空
        setState(0);                        //把状态设置为0
        return true;
    }

    Condition newCondition() {       // 得到一个条件，以支持await和asignal，线程等待和唤醒
        return new ConditionObject();
    }
    //       实现序列化的方法还有writeObject等
    //        private void readObject(ObjectInputStream ois){
    //            ois.defaultReadObject();
    //            setState(0);
    //        }
}

//    本质就是mutex的01信号量
private final Sync sync = new Sync();

public void lock() {
    sync.acquire(1);
}

public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
    return sync.tryAcquireNanos(1, unit.toNanos(timeout));
}

public void unlock() {
    sync.release(1);
}

public boolean tryUnlock() {
    return sync.tryRelease(0);
}

public Condition newCondition() {
    return sync.newCondition();
}

public boolean isLocked() {
    return sync.isHeldExclusive();
}

public boolean hasQueuedThreads() {
    return sync.hasQueuedThreads();
}

public void lockInterruptibly() throws InterruptedException {
    sync.acquireInterruptibly(1);
}

public static void main(String[] args) {
    A11AQS aqs = new A11AQS();
    for (int i = 0; i < 10; i++) {
        new Thread(()->{
            try{
                aqs.lock();
                Thread.sleep(1000);
            } catch (InterruptedException e){

            } finally {
                aqs.unlock();
            }

        }).start();

    }
}
```

