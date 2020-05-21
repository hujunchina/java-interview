2020年5月20日 22:17:52

### 目录

1. HashMap是什么
2. HashMap继承关系
3. HashMap成员属性
4. HashMap构造方法
5. HashMap set方法
6. HashMap get方法
7. HashMap应用场景

### 1. HashMap 是什么

HashMap定义，以散列方式存储键值对，允许使用空值和空键。有两个影响其性能的参数：初始容量和负载因子，底层以桶存储。

- 容量是哈希表中桶的数量，初始容量就是哈希表创建时的容量。
- 加载因子是散列表在容量自动扩容之前被允许的最大饱和量。当哈希表中的 entry 数量超过负载因子和容量的乘积时，散列表就会被重新映射（即重建内部数据结构），一般散列表大约是存储桶数量的两倍。

桶大小默认16，负载因子0.75，即桶内元素饱和度，最多可用存12个，多了就要再散列，扩大为原来的2倍。

为什么默认加载因子是0.75？经验，0.75是时间和空间良好的平衡结果。如果大了，会减少空间开销，但查找成本高（重复的键多）。

### 2. HashMap 继承关系

```java
public class HashMap<K,V> extends AbstractMap<K,V> 
    implements Map<K,V>, Cloneable, Serializable
```

![img](C:\code\github\java-interview\img\hasmap01.png)

支持泛型，继承抽象类，实现必要的方法get、set

### 3. HashMap 成员属性

```java
private static final long serialVersionUID = 362498820763181265L;
static final int DEFAULT_INITIAL_CAPACITY = 16;
static final int MAXIMUM_CAPACITY = 1073741824;
static final float DEFAULT_LOAD_FACTOR = 0.75F;
static final int TREEIFY_THRESHOLD = 8;
static final int UNTREEIFY_THRESHOLD = 6;
static final int MIN_TREEIFY_CAPACITY = 64;
transient HashMap.Node<K, V>[] table;
transient Set<Entry<K, V>> entrySet;
transient int size;
transient int modCount;
int threshold;
final float loadFactor;
```

固定的声明为 static final，不想序列化的声明为 transient。

```java
static class Node<K, V> implements Entry<K, V> {
    final int hash;
    final K key;
    V value;
    HashMap.Node<K, V> next;
}
```

在Java7叫Entry，在Java8中叫Node。

数据存储的实体，设计思路是包裹一个静态内部类来实现具体数据结构。

### 4. HashMap 构造方法

**java8之前是头插法**，就是说新来的值会取代原有的值，原有的值就顺推到链表中去，就像上面的例子一样，因为写这个代码的作者认为后来的值被查找的可能性更大一点，提升查找的效率。

但是，在java8之后，都是所用尾部插入了。因为扩容容易产生环形链表。

![img](C:\code\github\java-interview\img\hasmap06.png)

因为resize的赋值方式，也就是使用了**单链表的头插入方式，同一位置上新元素总会被放在链表的头部位置**，在旧数组中同一条Entry链上的元素，通过重新计算索引位置后，有可能被放到了新数组的不同位置上。

![img](C:\code\github\java-interview\img\hasmap07.png)

一旦几个线程都调整完成，就可能出现环形链表：

![img](C:\code\github\java-interview\img\hasmap08.png)

### 5. HashMap put方法

```java
if (((HashMap.Node)p).hash == hash && ((k = ((HashMap.Node)p).key) == key 
|| key != null && key.equals(k))) {
// 注意是&操作
if ((p = tab[i = n - 1 & hash]) == null) {
    tab[i] = this.newNode(hash, key, value, (HashMap.Node)null);
 }
 static final int hash(Object key) {
    int h;
    return key == null ? 0 : (h = key.hashCode()) ^ h >>> 16;
 }
```

- 对key做hash运算，得到key的hash值；key的hashCode先无符号左移16位，然后与hashCode按位异或。（hash值与hashCode不一样）

- 计算桶的index（数组的下标）；(n-1)&hash值

- 如果没有下标冲突，直接放入数组中，如果有，以链表形式放入桶后面（当前键值对的后面）next指针。

- 如果链表过长超过默认的阈值8，就会把链表转为红黑树。为什么？

  考虑到查找效率问题 log8=3。

- 如果数组中节点key已经存在了，就替换value值；

- 如果x>16*0.75=12 就调用resize自动扩容一倍。

#### 5.1 Hash 为什么是 & ？

![img](C:\code\github\java-interview\img\hasmap02.png)

高16位不变，低16位和高16位做一个异或。如果不异或，因为与0x1111进行与操作有效位只有4位，很容发生冲突。

高位与低位异或一下可以让高位参与计算数组下标。hashCode分布已经很均匀了，高位也参与减少冲突。

一是比取余效率高，二是直接取hashcode的最后几位，借用hashcode的散列性来增加 put 到桶的散列性。

#### 5.2 HashMap resize方法

影响因子饱和度问题，如果饱和了，就把数组扩大2倍。从16到32。

![img](C:\code\github\java-interview\img\hasmap03.png)

![img](C:\code\github\java-interview\img\hasmap04.png)

在扩充 HashMap 的时候，不需要重新计算 hash。

只需要看看原来的 hash 值新增的那个 bit 是 1 还是 0 就好了。

是 0 的话索引没变，是 1 的话索引变成 “原索引 + oldCap”。可以看看下图为 16 扩充为 32 的 resize 示意图：

![img](C:\code\github\java-interview\img\hasmap05.png)

### 6. HashMap get方法

```java
if ((tab = this.table) != null && (n = tab.length) > 0 
    && (first = tab[n - 1 & hash]) != null) {
   Object k;
   if (first.hash == hash && ((k = first.key) == key || key != null && key.equals(k))) {
        return first;
   }
   if ((e = first.next) != null) {
        if (first instanceof HashMap.TreeNode) {
            return ((HashMap.TreeNode)first).getTreeNode(hash, key);
        }

        do {
            if (e.hash == hash && ((k = e.key) == key || key != null && key.equals(k))) {
                return e;
            }
        } while((e = e.next) != null);
    }
}
```

- 对key做hash计算，并找到数组下标；
- 比较如果hash值一样并且key一样，直接返回键值对；
- 对比发现不一样，dowhile继续比较hash和key两个条件知道相同。

### 7. HashMap 扩容方式

#### 7.1 那为啥用16不用别的呢？

因为在使用是2的幂的数字的时候，Length-1的值是所有二进制位全为1，这种情况下，index的结果等同于HashCode后几位的值。

只要输入的HashCode本身分布均匀，Hash算法的结果就是均匀的。

这是为了**实现均匀分布**。

#### 7.2 为啥我们重写equals方法的时候需要重写hashCode方法呢？

因为在java中，所有的对象都是继承于Object类。Ojbect类中有两个方法equals、hashCode，这两个方法都是用来比较两个对象是否相等的。

在未重写equals方法我们是继承了object的equals方法，**那里的 equals是比较两个对象的内存地址**，显然我们new了2个对象内存地址肯定不一样。

如果我们对 equals 方法进行了重写，建议一定要对 hashCode 方法重写，以保证相同的对象返回相同的hash值，不同的对象返回不同的hash值。

不然一个链表的对象，到时候发现hashCode都一样，无法确定那个元素！

还有一种情况，写了 equals 但没有写 hashcode，那么存入的元素取不出来。

#### 7.3 冲突处理 红黑树

冲突后，使用链表形式存储，如果链表长度大于8，会转为红黑树，如果长度小于8，还是红黑树，不可逆。

### 8. HashMap 应用场景

```java
public static void main(String[] args) {
    System.out.println("do");
    Map<String, String> map = new HashMap<>();
    map.put("hujun", "liujihong");
    map.put("hujun", "covered");

    map.put(null, null);
    map.put(null, "covered null key");
    map.put("null", null);
    //        hashmap都可以放入null，占长度，均可输出
    System.out.println(map.get(null));
    System.out.println(map.get("null"));

    int h=5;
    int hash1 = (h^h)>>>16;
    int hash2 = h^h>>>16;
    System.out.format("%d, %d, %d", hash1, hash2, 5>>>16);
    System.out.println(4-1&3);
}

public static void iteratorTest(){
    Map<Integer, String> map = new HashMap<>();
    map.put(1, "12");
    map.put(2, "23");
    map.put(3, "34");

    Set<Map.Entry<Integer, String>> set = map.entrySet();
    Iterator<Map.Entry<Integer, String>> it = set.iterator();
    while (it.hasNext()){
        Map.Entry<Integer, String> entry = it.next();
        Integer key = entry.getKey();
        String val = entry.getValue();
        System.out.println(key+"=>"+val);
    }

    for(Map.Entry<Integer, String> entry:set){
        Integer key = entry.getKey();
        String val = entry.getValue();
        System.out.println(key+"=>"+val);
    }

    sortMap(map);
}

public static void sortMap(Map m){
    Set<Integer> set = m.keySet();
    for(Integer i : set){
        System.out.println(i+"=>"+m.get(i));
    }
}
```

