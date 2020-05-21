2020年5月20日 23:19:21

### 目录

1. TreeMap是什么
2. TreeMap继承关系
3. TreeMap成员属性
4. TreeMap构造方法
5. TreeMap add方法
6. TreeMap grow方法
7. TreeMap应用场景



### 1. TreeMap是什么

直接基于红黑树实现，完成对key的排序存储，输出也是有序的，非线程安全
因为对key排序，所以不需要hash解决冲突，直接存储即可。

### 2. TreeMap继承关系

```java
public class TreeMap<K,V> extends AbstractMap<K,V>
    implements NavigableMap<K,V>, Cloneable, java.io.Serializable
```

从类的定义来看，HashMap和TreeMap都继承自AbstractMap，不同的是HashMap实现的是Map接口，而TreeMap实现的是NavigableMap接口。NavigableMap是SortedMap的一种，实现了对Map中key的排序。

这样两者的第一个区别就出来了，TreeMap是排序的而HashMap不是。

TreeMap是一个泛型类

TreeMap继承自AbstractMap

TreeMap实现NavigableMap接口，表示TreeMap具有方向性，支持导航

TreeMap实现Cloneable接口，表示TreeMap支持克隆

TreeMap实现java.io.Serializable接口，表示TreeMap支持序列化

### 3. TreeMap成员属性

```java
//比较器，TreeMap的顺序由比较器决定，如果比较器为空，顺序由key自带的比较器决定
private final Comparator<? super K> comparator;
//根节点，Entry是一个红黑树结构
private transient Entry<K,V> root;
//节点的数量
private transient int size = 0;
//修改统计
private transient int modCount = 0;
//缓存键值对集合
private transient EntrySet entrySet;
//缓存key的Set集合
private transient KeySet<K> navigableKeySet;
private transient NavigableMap<K,V> descendingMap;
```

从字段属性中可以看出

- TreeMap是一个红黑树结构
- TreeMap保存了根节点root

TreeMap和HashMap不同的是，TreeMap的底层是一个Entry root。

### 4. TreeMap构造方法

```java
//默认的构造方法
public TreeMap() {
    	//比较器默认为null
        comparator = null;
    }
//传入一个比价器对象
public TreeMap(Comparator<? super K> comparator) {
    	//设置比较器
        this.comparator = comparator;
    }
//传入一个Map对象
public TreeMap(Map<? extends K, ? extends V> m) {
    	//比较器设为null
        comparator = null;
    	//添加Map对象的数据
        putAll(m);
    }
//传入一个SortedMap对象
public TreeMap(SortedMap<K, ? extends V> m) {
    	//把SortedMap的比较器赋值给当前的比较器
        comparator = m.comparator();
        try {
            //添加SortedMap对象的数据
            buildFromSorted(m.size(), m.entrySet().iterator(), null, null);
        } catch (java.io.IOException cannotHappen) {
        } catch (ClassNotFoundException cannotHappen) {
        }
    }

```

从构造方法中可以看出

- TreeMap默认比较器为null，实质上使用的是Key自带的比较器，如果默认比较器为空，Key的对象必须实现Comparable接口
- TreeMap可以指定比较器进行初始化
- TreeMap可以接收Map对象来初始化
- TreeMap可以接收SortedMap对象来初始化

### 5. TreeMap put方法

```java
//添加键值对元素
public V put(K key, V value) {
    //获取根节点副本
    Entry<K,V> t = root;
    if (t == null) {
        //如果根节点为null
        //类型检查和空检查，如果当前比较器为null，key的对象必须实现Comparable接口
        compare(key, key); // type (and possibly null) check
        //根据key和value创建新的节点，并把创建的新节点作为根节点
        root = new Entry<>(key, value, null);
        //把元素数量置为1
        size = 1;
        //修改统计加1
        modCount++;
        //返回null
        return null;
    }
    //根节点不为null的情况
    int cmp;
    //parent 插入节点的父节点
    Entry<K,V> parent;
    // split comparator and comparable paths
    //获取当前的比较器
    Comparator<? super K> cpr = comparator;
    if (cpr != null) {
        //如果存在比较器
        //while循环遍历查找
        do {
            //把parent指向当前节点
            parent = t;
            //表当前节点的key和传入的key
            cmp = cpr.compare(key, t.key);
            if (cmp < 0)
                //如果传入的key小于当前节点的key，往左查找
                //把当前节点指向左子节点
                t = t.left;
            else if (cmp > 0)
                //如果传入的key大于当前节点的key，往右查找
                //把当前节点指向右子节点
                t = t.right;
            else
                //如果查找到了，把当前节点的值替换为传入的值
                return t.setValue(value);
        } while (t != null);
    }
    //不存在比较器，使用key的比较器
    else {
        if (key == null)
            //如果传入key为空，抛出异常
            throw new NullPointerException();
        //强转key
        @SuppressWarnings("unchecked")
        Comparable<? super K> k = (Comparable<? super K>) key;
        //while循环遍历查找
        do {
            //把parent指向当前节点
            parent = t;
            //使用key自带的比较器进行比较
            cmp = k.compareTo(t.key);
            if (cmp < 0)
                //如果传入的key小于当前节点的key，往左查找
                //把当前节点指向左子节点
                t = t.left;
            else if (cmp > 0)
                //如果传入的key大于当前节点的key，往右查找
                //把当前节点指向右子节点
                t = t.right;
            else
                //如果查找到了，把当前节点的值替换为传入的值
                return t.setValue(value);
        } while (t != null);
    }
    //把最后遍历的节点作为父节点，创建新的节点
    Entry<K,V> e = new Entry<>(key, value, parent);
    if (cmp < 0)
        //如果插入的key比parent的key小，新的节点作为左子节点
        parent.left = e;
    else
        //如果插入的key比parent的key大，新的节点作为右子节点
        parent.right = e;
    //插入过后进行平衡操作
    fixAfterInsertion(e);
    //元素数量加1
    size++;
    //修改统计加1
    modCount++;
    //返回null
    return null;
}
```

### 6. TreeMap get方法

```java
//根据传入的key查找值
public V get(Object key) {
    //通过getEntry来查找对应key的节点
    Entry<K,V> p = getEntry(key);
    //如果节点不存在返回null，存在返回节点的value
    return (p==null ? null : p.value);
}
//根据传入的key获取对应的节点
final Entry<K,V> getEntry(Object key) {
    if (comparator != null)
        //如果存在比较器，调用getEntryUsingComparator方法来查找节点
        return getEntryUsingComparator(key);
    //检查key的合法性
    if (key == null)
        throw new NullPointerException();
    //key的对象必须实现Comparable接口
    @SuppressWarnings("unchecked")
    Comparable<? super K> k = (Comparable<? super K>) key;
    //获取根节点的副本
    Entry<K,V> p = root;
    //通过while循环查找
    while (p != null) {
        //key进行比较
        int cmp = k.compareTo(p.key);
        if (cmp < 0)
            //如果传入的key比当前节点的key小，往左边查找
            //把当前节点指向左子节点
            p = p.left;
        else if (cmp > 0)
            //如果传入的key比当前节点的key大，往右边查找
            //把当前节点指向右子节点
            p = p.right;
        else
            //查找成功，返回当前节点
            return p;
    }
    //没找到，返回null
    return null;
}
```

源码 https://juejin.im/post/5e621592f265da573d61b1fb

### 7. TreeMap 区别

#### 7.1 Null值的区别

HashMap可以允许一个null key和多个null value。而TreeMap不允许null key，但是可以允许多个null value。

```java
@Test
public void withNull() {
    Map<String, String> hashmap = new HashMap<>();
    hashmap.put(null, null);
    log.info("{}",hashmap);
}
@Test
public void withNull() {
    Map<String, String> hashmap = new TreeMap<>();
    hashmap.put(null, null);
    log.info("{}",hashmap);
}
```

#### 7.2 性能区别

HashMap的底层是Array，所以HashMap在添加，查找，删除等方法上面速度会非常快。而TreeMap的底层是一个Tree结构，所以速度会比较慢。

另外HashMap因为要保存一个Array，所以会造成空间的浪费，而TreeMap只保存要保持的节点，所以占用的空间比较小。

HashMap如果出现hash冲突的话，效率会变差，不过在java 8进行TreeNode转换之后，效率有很大的提升。

TreeMap在添加和删除节点的时候会进行重排序，会对性能有所影响。

```java
public class A06TreeMapTest {
    private static final String[] chars = "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z".split(" ");

    public static void main(String[] args) {
        TreeMap<Integer, String> treeMap = new TreeMap<>();
        for (int i = 0; i < chars.length; i++) {
            treeMap.put(i, chars[i]);
        }

//       key 不能为 null，因为红黑树要比较key大小，为null怎么比较？
//        treeMap.put(null, "2");
//       但 value 可以为null
        treeMap.put(2, null);

        System.out.println(treeMap);
        Integer low = treeMap.firstKey();
        Integer high = treeMap.lastKey();
        System.out.println(low);
        System.out.println(high);
        Iterator<Integer> it = treeMap.keySet().iterator();
        for (int i = 0; i <= 6; i++) {
            if (i == 3) { low = it.next(); }
            if (i == 6) { high = it.next(); } else { it.next(); }
        }
        System.out.println(low);
        System.out.println(high);
        System.out.println(treeMap.subMap(low, high));
        System.out.println(treeMap.headMap(high));
        System.out.println(treeMap.tailMap(low));
    }
}
```

