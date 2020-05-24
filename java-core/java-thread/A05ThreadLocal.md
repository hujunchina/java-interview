2020年5月21日 14:55:02

### 目录

1. ThreadLocal 是什么
2. ThreadLocal 原理
3. ThreadLocal 总结

[TOC]

### 1. ThreadLocal 是什么

#### 1.1 note

一个存储线程本地副本的工具类，解决线程安全另一个方法，不一定要同步，同步只是不让多个线程修改共享资源，如果不涉及共享资源也就没必要同步了，怎么做: 1.可重入代码，2.线程本地副本。

1.可重入代码：一个方法，它的返回结果是可以预测的，即只要输入了相同的数据，就能返回相同的结果，那它就满足可重入性，共享区的数据对方法本身没有影响的。

2.线程本地副本：使用 ThreadLocal 为共享变量在每个线程中都创建了一个本地副本，这个副本只能被当前线程访问，其他线程无法访问。就是利用了线程栈的私有性，把共享数据变为私有数据。应用场景：管理数据库连接、Session。

#### 1.2 介绍

在多线程编程中通常解决线程安全的问题我们会利用 synchronzed 或者 lock 控制线程对临界区资源的同步顺序从而解决线程安全的问题，但是这种加锁的方式会让未获取到锁的线程进行阻塞等待，很显然这种方式的时间效率并不是很好。**线程安全问题的核心在于多个线程会对同一个临界区共享资源进行操作**，那么，如果每个线程都使用自己的“共享资源”，各自使用各自的，又互相不影响到彼此即让多个线程间达到隔离的状态，这样就不会出现线程安全的问题。事实上，这就是一种“**空间换时间**”的方案，每个线程都会都拥有自己的“共享资源”无疑内存会大很多，但是由于不需要同步也就减少了线程可能存在的阻塞等待的情况从而提高的时间效率。

虽然 ThreadLocal 并不在 java.util.concurrent 包中而在 java.lang 包中，但我更倾向于把它当作是一种并发容器（虽然真正存放数据的是 ThreadLoclMap）进行归类。从**ThreadLocal 这个类名可以顾名思义的进行理解，表示线程的“本地变量”，即每个线程都拥有该变量副本，达到人手一份的效果，各用各的这样就可以避免共享资源的竞争**。



### 2. ThreadLocal 原理

#### 2.1 note

原理：Thread 类中维护着一个 ThreadLocal.ThreadLocalMap 类型的成员 threadLocals。这个成员就是用来存储线程独占的变量副本。

ThreadLocalMap 是 ThreadLocal 的内部类，它维护着一个 Entry 数组， Entry 用于保存键值对，其 key 是 ThreadLocal 对象，value 是传递进来的对象（变量副本）。

2.ThreadLocalMap 采用线性探测的方式来解决 Hash 冲突。所谓线性探测，就是根据初始 key 的 hashcode 值确定元素在 table 数组中的位置，如果发现这个位置上已经被其他的 key 值占用，则利用固定的算法寻找一定步长的下个位置，依次判断，直至找到能够存放的位置。

3.ThreadLocalMap 的 Entry 继承了 WeakReference，所以它的 key（ThreadLocal 对象）是弱引用，而 value（变量副本）是强引用。如果 ThreadLocal 对象没有外部强引用来引用它，那么 ThreadLocal 对象会在下次 GC 时被回收。此时，Entry 中的 key 已经被回收，但是 value 由于是强引用不会被垃圾收集器回收。如果创建 ThreadLocal 的线程一直持续运行，那么 value 就会一直得不到回收，产生内存泄露。那么如何避免内存泄漏呢？方法就是：使用 ThreadLocal 的 set 方法后，显示的调用 remove 方法 。

```java
public class ThreadLocal<T> {
    public T get() {}
    public void set(T value) {}
    public void remove() {}
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {}
}
get - 用于获取 ThreadLocal 在当前线程中保存的变量副本。
set - 用于设置当前线程中变量的副本。
remove - 用于删除当前线程中变量的副本。如果此线程局部变量随后被当前线程读取，
则其值将通过调用其 initialValue 方法重新初始化，除非其值由中间线程中的当前线程设置。
这可能会导致当前线程中多次调用 initialValue 方法。
initialValue - 为 ThreadLocal 设置默认的 get 初始值，需要重写 initialValue 方法 。
    
/// 那么如何避免内存泄漏呢？方法就是：使用 ThreadLocal 的 set 方法后，显示的调用 remove 方法 。
ThreadLocal<String> threadLocal = new ThreadLocal();
try {
    threadLocal.set("xxx");
    // ...
} finally {
    threadLocal.remove();
}
```

#### 2.2 set 方法

**set 方法设置在当前线程中 threadLocal 变量的值**，该方法的源码为：

```java
public void set(T value) {
	//1. 获取当前线程实例对象
    Thread t = Thread.currentThread();
	//2. 通过当前线程实例获取到ThreadLocalMap对象
    ThreadLocalMap map = getMap(t);
    if (map != null)
		//3. 如果Map不为null,则以当前threadLocl实例为key,值为value进行存入
        map.set(this, value);
    else
		//4.map为null,则新建ThreadLocalMap并存入value
        createMap(t, value);
}
```

方法的逻辑很清晰，具体请看上面的注释。通过源码我们知道 value 是存放在了 ThreadLocalMap 里了，当前先把它理解为一个普普通通的 map 即可，也就是说，**数据 value 是真正的存放在了 ThreadLocalMap 这个容器中了，并且是以当前 threadLocal 实例为 key**。先简单的看下 ThreadLocalMap 是什么，有个简单的认识就好，下面会具体说的。

#### 2.3 ThreadLocalMap

```java
ThreadLocalMap getMap(Thread t) {
    return t.threadLocals;
}
ThreadLocal.ThreadLocalMap threadLocals = null;
```

该方法直接返回的就是当前线程对象 t 的一个成员变量 threadLocals：也就是说**ThreadLocalMap 的引用是作为 Thread 的一个成员变量，被 Thread 进行维护的**。回过头再来看看 set 方法，当 map 为 Null 的时候会通过`createMap(t，value)`方法：

```java
void createMap(Thread t, T firstValue) {
    t.threadLocals = new ThreadLocalMap(this, firstValue);
}
```

该方法就是**new 一个 ThreadLocalMap 实例对象，然后同样以当前 threadLocal 实例作为 key,值为 value 存放到 threadLocalMap 中，然后将当前线程对象的 threadLocals 赋值为 threadLocalMap**。

现在来对 set 方法进行总结一下： **通过当前线程对象 thread 获取该 thread 所维护的 threadLocalMap,**

**若 threadLocalMap 不为 null，则以 threadLocal 实例为 key，值为 value 的键值对存入 threadLocalMap；若 threadLocalMap 为 null 的话，就新建 threadLocalMap 然后在以 threadLocal 为键，值为 value 的键值对存入即可。**

#### 2.4 get 方法

**get 方法是获取当前线程中 threadLocal 变量的值**，同样的还是来看看源码：

```java
public T get() {
	//1. 获取当前线程的实例对象
    Thread t = Thread.currentThread();
	//2. 获取当前线程的threadLocalMap
    ThreadLocalMap map = getMap(t);
    if (map != null) {
		//3. 获取map中当前threadLocal实例为key的值的entry
        ThreadLocalMap.Entry e = map.getEntry(this);
        if (e != null) {
            @SuppressWarnings("unchecked")
			//4. 当前entitiy不为null的话，就返回相应的值value
            T result = (T)e.value;
            return result;
        }
    }
	//5. 若map为null或者entry为null的话通过该方法初始化，并返回该方法返回的value
    return setInitialValue();
}
```

弄懂了 set 方法的逻辑，看 get 方法只需要带着逆向思维去看就好，如果是那样存的，反过来去拿就好。代码逻辑请看注释，另外，看下 setInitialValue 主要做了些什么事情？

```java
private T setInitialValue() {
    T value = initialValue();
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null)
        map.set(this, value);
    else
        createMap(t, value);
    return value;
}
protected T initialValue() {
    return null;
}
```

这个**方法是 protected 修饰的也就是说继承 ThreadLocal 的子类可重写该方法，实现赋值为其他的初始值**。关于 get 方法来总结一下：

**通过当前线程 thread 实例获取到它所维护的 threadLocalMap，然后以当前 threadLocal 实例为 key 获取该 map 中的键值对（Entry），若 Entry 不为 null 则返回 Entry 的 value。如果获取 threadLocalMap 为 null 或者 Entry 为 null 的话，就以当前 threadLocal 为 Key，value 为 null 存入 map 后，并返回 null。**

#### 2.5 remove 方法

```java
public void remove() {
	//1. 获取当前线程的threadLocalMap
	ThreadLocalMap m = getMap(Thread.currentThread());
 	if (m != null)
		//2. 从map中删除以当前threadLocal实例为key的键值对
		m.remove(this);
}
```

#### 2.6 Entry 内部类

```java
static class Entry extends WeakReference<ThreadLocal<?>> {
    /** The value associated with this ThreadLocal. */
    Object value;
    Entry(ThreadLocal<?> k, Object v) {
        super(k);
        value = v;
    }
}
```

Entry 是一个以 ThreadLocal 为 key,Object 为 value 的键值对，另外需要注意的是这里的**threadLocal 是弱引用，因为 Entry 继承了 WeakReference，在 Entry 的构造方法中，调用了 super(k)方法就会将 threadLocal 实例包装成一个 WeakReferenece。

![title](C:\code\github\java-interview\img\thread-14.png)

`ThreadLocal`并不维护`ThreadLocalMap`，并不是一个存储数据的容器，它只是相当于一个工具包，提供了操作该容器的方法，如get、set、remove等。而`ThreadLocal`内部类`ThreadLocalMap`才是存储数据的容器，并且该容器由`Thread`维护。

每一个`Thread`对象均含有一个`ThreadLocalMap`类型的成员变量`threadLocals`，它存储本线程中所有ThreadLocal对象及其对应的值。

ThreadLocal 相当于一个仓库，里面用ThreadLocalMap存了所有线程的副本数据。key是每个线程自定义的一个引用threadLocals，value就是值了。每个线程通过threadLocals到仓库中的Map架子中找到自己的值。



### 3. ThreadLocal 总结

```java
static class ThreadLocalTest{
    public String DB_URL = "";
    private static ThreadLocal<Connection> connectionHolder = 
        new ThreadLocal<Connection>() {
        @Override
        public Connection initialValue() {
            try {
                return DriverManager.getConnection("");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
    };
    public static Connection DBConnection(){
        return connectionHolder.get();
    }

    private static final ThreadLocal<Session> sessionHolder = new ThreadLocal<>();
    public static Session getSession() {
        Session session = (Session) sessionHolder.get();
        try {
            if (session == null) {
                session = createSession();
                sessionHolder.set(session);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sessionHolder.remove();
        }
        return session;
    }

    private static Session createSession() {
        return sessionHolder.get();
    }
}
```

