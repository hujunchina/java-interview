2020年5月20日 23:36:56

### 目录

1. HashSet
2. LinkedHashSet
3. TreeSet



### 1. HashSet

hashset底层是hashmap，是无序散列的，允许null，非线程安全
HashSet 的核心，通过维护一个 HashMap 实体来实现 HashSet 方法
private transient HashMap<E,Object> map;
PRESENT 是用于关联 map 中当前操作元素的一个虚拟值
private static final Object PRESENT = new Object();



### 2. LinkedHashSet

hashset是无序，散列的，而linkedhashset是按照输入顺序保存的

非线程安全，调用hashmap的构造方法实例化，底层是双链表存储插入的节点的次序



### 3. TreeSet

treeset底层基于treemap实现，是元素有序的，把set中的元素当做key存在map中

因为对key排序，所以不能加入null值

