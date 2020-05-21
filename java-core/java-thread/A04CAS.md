2020年5月21日 14:41:09

### 目录

1. CAS 是什么
2. CAS 原理
3. CAS 总结

### 1. CAS 是什么

互斥同步最主要的问题是线程阻塞和唤醒所带来的性能问题,互斥同步属于一种悲观的并发策略。用户态核心态转换、维护锁计数器和检查是否有被阻塞的线程需要唤醒等操作。

CAS: 基于冲突检测的乐观并发策略。先进行操作，如果没有其它线程争用共享数据，那操作就成功了，否则采取补偿措施（不断地重试，直到成功为止）。这种乐观的并发策略的许多实现都不需要将线程阻塞，因此这种同步操作称为非阻塞同步。

CAS（Co Swmpare andap），字面意思为比较并交换。CAS 有 3 个操作数，分别是：内存值 V，旧的预期值 A，要修改的新值 B。

当且仅当预期值 A 和内存值 V 相同时，将内存值 V 修改为 B，否则什么都不做。

### 2. CAS 原理

#### 2.1 Sample

原理：利用 Unsafe 这个类提供的 CAS 操作，实现的 Atomic::cmpxchg 指令。

Atomic::cmpxchg 的实现使用了汇编的 CAS 操作，并使用 CPU 提供的 lock 信号保证其原子性。

应用： 原子类、自旋锁（线程反复检查锁变量是否可用，直到成功为止）

CAS 比锁性能更高。因为 CAS 是一种非阻塞算法，所以其避免了线程阻塞和唤醒的等待时间。

问题：

- ABA问题，解决方法加入标记，对比两个A是否一样，AtomicStampedReference，

- 循环开销大，（定时或自旋一定次数）

- 只能保证一个共享变量的原子性，无法对多个变量保证，需要对变量重定义

#### 2.2 源码分析

```java
// 1、获取UnSafe实例对象，用于对内存进行相关操作
private static final Unsafe unsafe = Unsafe.getUnsafe();
// 2、内存偏移量
private static final long valueOffset;

static {
    try {
        // 3、初始化地址偏移量
        valueOffset = unsafe.objectFieldOffset
            (AtomicInteger.class.getDeclaredField("value"));
    } catch (Exception ex) { throw new Error(ex); }
}

// 4、具体值，使用volatile保证可见性
private volatile int value;
```

从上面代码中我们可以看出，`AtomicInteger`中依赖于一个叫`Unsafe`的实例对象，我们都知道，java语言屏蔽了像C++那样直接操作内存的操作，程序员不需手动管理内存，但话说回来，java还是开放了一个叫`Unsafe`的类直接对内存进行操作，由其名字可以看出，使用`Unsafe`中的操作是不安全的，要小心谨慎。

`valueOffset`是对象的内存偏移地址，通过`Unsafe`对象进行初始化，有一点需要注意的是，对于给定的某个字段都会有相同的偏移量，同一类中的两个不同字段永远不会有相同的偏移量。也就是说，只要对象不死，这个偏移量就永远不会变，可以想象，CAS所依赖的第一个参数（内存地址值）正是通过这个地址偏移量进行获取的。

`value`属于共享资源，借助`volatile`保证内存可见性，关于`volatile`的简单分析

```java
// 1、获取并增加delta
public final int getAndAdd(int delta) {
    return unsafe.getAndAddInt(this, valueOffset, delta);
}
// 2、加一
public final int incrementAndGet() {
    return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
}
```

上面两个方法依赖下面`Unsafe`类中的`getAndAddInt`操作，借助`openjdk`提供的`Unsafe`源码，我们看下其实现:

```java
public final int getAndAddInt(Object o, long offset, int delta) {
    int v;
    // 1、不断的循环比较，直到CAS操作成功返回
    do {
        v = getIntVolatile(o, offset);
    } while (!compareAndSwapInt(o, offset, v, v + delta));
    return v;
}
```

从上面可以看出，本质上CAS使用了自旋锁进行自旋，直到CAS操作成功，如果很长一段时间都没有操作成功，那么将一直自旋下去。

### 3. CAS 总结

```java
static class CASTest{
    public static void test() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        final AtomicInteger count = new AtomicInteger(0);
        for (int i = 0; i < 10; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    count.incrementAndGet();
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(3, TimeUnit.SECONDS);
        System.out.println("Final Count is : " + count.get());
    }
}
```

