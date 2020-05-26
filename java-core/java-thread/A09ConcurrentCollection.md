2020年5月22日 11:38:46

### 1. 概述

并发容器：ConcurrentHashMap，CopyOnWriteArrayList，CopyOnWriteArraySet。和同步容器不同，同步容器实现是在读写修改方法上加Synchronized锁，并发容器使用AQS队列同步器。

ConcurrentHashMap:维护了一个 Segment 数组，一般称为分段桶。final Segment<K,V>[] segments;把锁的对象分成多段，每段独立控制，使得锁粒度更细，减少阻塞开销，从而提高并发性。Java 1.8 之前采用分段锁机制细化锁粒度，降低阻塞，从而提高并发性。

Java 1.8 之后基于 CAS 实现。取消 segments 字段，直接采用 transient volatile HashEntry<K,V>[] table保存数据，采用 table 数组元素作为锁，从而实现了对每一行数据进行加锁，进一步减少并发冲突的概率。将原先 table 数组＋单向链表的数据结构，变更为 table 数组＋单向链表＋红黑树的结构。对于 hash 表来说，最核心的能力在于将 key hash 之后能均匀的分布在数组中。如果 hash 之后散列的很均匀，那么 table 数组中的每个队列长度主要为 0 或者 1。但实际情况并非总是如此理想，虽然 ConcurrentHashMap 类默认的加载因子为 0.75，但是在数据量过大或者运气不佳的情况下，还是会存在一些队列长度过长的情况，如果还是采用单向列表方式，那么查询某个节点的时间复杂度为 O(n)；	因此，对于个数超过 8(默认值)的列表，jdk1.8 中采用了红黑树的结构，那么查询的时间复杂度可以降低到 O(logN)，可以改进性能。CopyOnWrite 字面意思为写入时复制。CopyOnWriteArrayList 是线程安全的 ArrayList。读操作不同步不加锁不会阻塞，所有写操作都会加锁同步，使用可重入锁 ReentrantLock。

### 2. 分类

```java
ConcurrentHashMap   HashMap    Java 1.8 之前采用分段锁机制细化锁粒度，降低阻塞，从而提高并发性；Java 1.8 之后基于 CAS 实现。
ConcurrentSkipListMap  SortedMap  基于跳表实现的
CopyOnWriteArrayList   ArrayList
CopyOnWriteArraySet    Set    基于 CopyOnWriteArrayList 实现。
ConcurrentSkipListSet  SortedSet  基于 ConcurrentSkipListMap 实现。
ConcurrentLinkedQueue  Queue  线程安全的无界队列。底层采用单链表。支持 FIFO。
ConcurrentLinkedDeque  Deque  线程安全的无界双端队列。底层采用双向链表。支持 FIFO 和 FILO。
ArrayBlockingQueue Queue  数组实现的阻塞队列。
LinkedBlockingQueue    Queue  链表实现的阻塞队列。
LinkedBlockingDeque    Deque  双向链表实现的双端阻塞队列
```