2020年5月22日 11:38:43

### 1. 概念

Executor - 运行任务的简单接口。ExecutorService - 扩展了 Executor 接口。扩展能力：支持有返回值的线程，支持管理线程的生命周期。ScheduledExecutorService - 扩展了ExecutorService接口。扩展能力：支持定期执行任务。AbstractExecutorService - ExecutorService 接口的默认实现。ThreadPoolExecutor - Executor 框架最核心的类，它继承了 AbstractExecutorService 类。ScheduledThreadPoolExecutor - ScheduledExecutorService 接口的实现，一个可定时调度任务的线程池。Executors - 可以通过调用 Executors 的静态工厂方法来创建线程池并返回一个 ExecutorService 对象。支持有返回值的线程 - sumbit、invokeAll、invokeAny 方法中都支持传入Callable 对象。支持管理线程生命周期 - shutdown、shutdownNow、isShutdown 等方法。4大构造方法，参数意义，线程池工作原理，4大拒绝策略。execute方法提交任务并执行，submit方法针对有返回值的线程。4大默认线程池single，fixed，cached，scheduled。

### 2. 线程池原理

```java
public void test(){
    Executor poolA = new ThreadPoolExecutor(10, 100, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));
    ExecutorService poolB = Executors.newCachedThreadPool();
    ExecutorService poolC = Executors.newSingleThreadExecutor();
    ExecutorService poolD = Executors.newFixedThreadPool(12);
    ExecutorService poolE = Executors.newScheduledThreadPool(10);
    ScheduledExecutorService poolF = new ScheduledThreadPoolExecutor(3);
}
```

#### 2.1 继承了Executor接口的线程池ThreadPoolExecutor

```java
//五个参数的构造函数
public ThreadPoolExecutor(int corePoolSize,int maximumPoolSize,long keepAliveTime,TimeUnit unit,BlockingQueue<Runnable> workQueue)

//六个参数的构造函数-1
public ThreadPoolExecutor(int corePoolSize,int maximumPoolSize,long keepAliveTime,TimeUnit unit,BlockingQueue<Runnable> workQueue,ThreadFactory threadFactory)

//六个参数的构造函数-2
public ThreadPoolExecutor(int corePoolSize,int maximumPoolSize,long keepAliveTime,TimeUnit unit,BlockingQueue<Runnable> workQueue,RejectedExecutionHandler handler)

//七个参数的构造函数
public ThreadPoolExecutor(int corePoolSize,int maximumPoolSize,long keepAliveTime,TimeUnit unit,BlockingQueue<Runnable> workQueue,ThreadFactory threadFactory,RejectedExecutionHandler handler)
```

- orePoolSize 核心线程数最大值，用于创建线程时检测。如果当前线程总数小于corePoolSize，则新建核心线程，如果超过corePoolSize，则新建非核心线程。
- 核心线程默认情况下会一直存活在线程池中，处于闲置状态。可以通过设置ThreadPoolExecutor的allowCoreThreadTimeOut为真，这样当核心线程处于闲置状态一段时间后就会销毁。

- maximumPoolSize线程总数最大值（核心线程数 加 非核心线程数）
- keepAliveTime非核心线程闲置超时，就会被销毁掉。可以通过设置allowCoreThreadTimeOut为真，来用于核心线程
- BlockingQueue任务队列，用于维护等待执行的Runnable对象。当所有核心线程在忙，新任务会添加到这个队列中等待处理，如果队列满了，则新建非核心线程执行任务。

#### 2.2 常用的workQueue类型

- SynchronousQueue会直接提交给线程处理，而不保留它，如果所有线程都在忙，会新建一个线程来处理这个任务。

- LinkedBlockingQueue如果当前线程数小于核心线程数，则新建线程(核心线程)处理任务；如果当前线程数等于核心线程数，则进入队列等待。

- ArrayBlockingQueue：可以限定队列的长度，接收到任务的时候，如果没有达到corePoolSize的值，则新建线程(核心线程)执行任务，如果达到了则入队等候，如果队列已满则新建线程(非核心线程)执行任务，如果总线程数到了maximumPoolSize，并且队列也满了则发生错误

- DelayQueue队列内元素必须实现Delayed接口，必须先实现Delayed接口。队列接收任务时先入队，只有达到了指定的延时时间，才会执行任务

#### 2.3 工厂方法

ThreadFactory threadFactory创建线程的方式（接口），需要实现他的Thread newThread(Runnable r)方法。AsyncTask是对线程池的封装，AsyncTask新建线程池的threadFactory参数源码：

```java
new ThreadFactory() {
    private final AtomicInteger mCount = new AtomicInteger(1);
    public Thread new Thread(Runnable r) {
        return new Thread(r,"AsyncTask #" + mCount.getAndIncrement());
    }
}
```



### 3 ThreadPoolExecutor的策略

1. 线程数量未达到corePoolSize，则新建一个线程(核心线程)执行任务

2. 线程数量达到了corePools，则将任务移入队列等待

3. 队列已满，新建线程(非核心线程)执行任务

4. 队列已满，总线程数又达到了maximumPoolSize，就会由上面那位星期天(RejectedExecutionHandler)抛出异常
