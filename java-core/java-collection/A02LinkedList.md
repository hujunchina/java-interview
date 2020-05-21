2020年5月20日 20:53:13

### 目标

1. **LinkedList是什么?**
2. **LinkedList继承关系?**
3. **LinkedList成员变量**
4. **LinkedList构造方法**
5. **LinkedList add方法**
6. **LinkedList get方法**
7. **LinkedList set方法**
8. **LinkedList remove方法**
9. **LinkedList 使用场景**

### 1. LinkedList是什么?

java.util.LinkedList 是 Java 实现的一个双链表数据结构。支持泛型，可以存储 Object 及其子类作为元素。双向链表即可以访问前后元素的一种线性存储结构，每个节点存储了上一个元素引用和下一个元素的引用，保证可以前后访问到。双链表优点是插入和删除快速，无需移动其他元素即可完成，但查找效率低，需要遍历整个链表。

可以用 LinkedList 实现堆栈、队列等。

### 2. LinkedList继承关系?

```java
public class LinkedList<E> extends AbstractSequentialList<E> 
implements List<E>, Deque<E>, Cloneable, Serializable
------------------
public abstract class AbstractSequentialList<E> extends AbstractList<E>
------------------
public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E>
------------------
public abstract class AbstractCollection<E> implements Collection<E>
------------------
```

LinkedList 继承了 AbstractSequentialList 抽象类，实现了List接口，AbstractSequentialList 又继承了AbstractList 抽象类，AbstractList 继承了 AbstractCollection 抽象类，AbstractCollection 实现 Collection接口。

![img](C:\code\github\java-interview\img\arraylist-4.png)

### 3. LinkedList成员变量

```java
transient int size;
transient LinkedList.Node<E> first;
transient LinkedList.Node<E> last;
private static final long serialVersionUID = 876323262645176354L;
```

四个变量分别表示长度大小，前节点引用，后节点引用，序列化号。

为了实现链表功能，LinkedList定义了一个内部静态类Node。

```java
private static class Node<E> {
    E item;
    LinkedList.Node<E> next;
    LinkedList.Node<E> prev;
    Node(LinkedList.Node<E> prev, E element, LinkedList.Node<E> next) {
        this.item = element;
        this.next = next;
        this.prev = prev;
    }
}
```

在Node类里面定义与C语言声明节点结构体一样。

### 4. LinkedList构造方法

```java
public LinkedList() {
    this.size = 0;
}
public LinkedList(Collection<? extends E> c) {
    this();
    this.addAll(c);
}
```

与 ArrayList 不同点是少了一个直接设置长度大小的构造函数，因为链表没有必要初始化大小。第二个方法参数是一个集合对象引用，然后调用 addAll 方法把集合中元素插入到链表中。

```java
public boolean addAll(Collection<? extends E> c) {
    return this.addAll(this.size, c);
}
public boolean addAll(int index, Collection<? extends E> c) {
    this.checkPositionIndex(index);
    Object[] a = c.toArray();
    int numNew = a.length;
    if (numNew == 0) {
        return false;
    } else {
        LinkedList.Node pred;
        LinkedList.Node succ;
        if (index == this.size) {
            succ = null;
            pred = this.last;
        } else {
            succ = this.node(index);
            pred = succ.prev;
        }
        Object[] var7 = a;
        int var8 = a.length;
        for(int var9 = 0; var9 < var8; ++var9) {
            Object o = var7[var9];
            LinkedList.Node<E> newNode = 
                new LinkedList.Node(pred, o,(LinkedList.Node)null);
            if (pred == null) {
                this.first = newNode;
            } else {
                pred.next = newNode;
            }
            pred = newNode;
        }
        if (succ == null) {
            this.last = pred;
        } else {
            pred.next = succ;
            succ.prev = pred;
        }
        this.size += numNew;
        ++this.modCount;
        return true;
    }
}
```

在 addAll 方法中，首先把参数集合转为一个 object 类型数组，然后遍历这个数组取出元素，并加入pre和succ前驱和后驱构造 Node 节点。

### 5. LinkedList add方法

```java
public boolean add(E e) {
    this.linkLast(e);
    return true;
}
void linkLast(E e) {
    LinkedList.Node<E> l = this.last;  // last是全局位置标记
    LinkedList.Node<E> newNode = 
          new LinkedList.Node(l, e, (LinkedList.Node)null);
    this.last = newNode;  // 先更新last标记
    if (l == null) {     //调制链接
        this.first = newNode;
    } else {
        l.next = newNode;
    }
    ++this.size;
    ++this.modCount;
}
```

链表插入方法是 linkLast 插入到链表后面，因为有 last 记录了最后指针位置，所以可以直接插入。

### 6. LinkedList get方法

```java
public E get(int index) {
    this.checkElementIndex(index);
    return this.node(index).item;
}
LinkedList.Node<E> node(int index) {
    LinkedList.Node x;
    int i;
    if (index < this.size >> 1) {
        x = this.first;
        for(i = 0; i < index; ++i) {
            x = x.next;
        }
        return x;
    } else {
        x = this.last;
        for(i = this.size - 1; i > index; --i) {
            x = x.prev;
        }
        return x;
    }
}
```

我们可以指定下标顺序来获得元素，这里先比较下标与长度的一半的大小，来决定是前序遍历，还是后序遍历找到元素。

```java
public E getFirst() {
    LinkedList.Node<E> f = this.first;
    if (f == null) {
        throw new NoSuchElementException();
    } else {
        return f.item;
    }
}
public E getLast() {
    LinkedList.Node<E> l = this.last;
    if (l == null) {
        throw new NoSuchElementException();
    } else {
        return l.item;
    }
}
```

还提供了得到第一个，和得到最后一个元素方法。

```java
public E peek() {
    LinkedList.Node<E> f = this.first;
    return f == null ? null : f.item;
}
public E poll() {
    LinkedList.Node<E> f = this.first;
    return f == null ? null : this.unlinkFirst(f);
}
```

另外，peek 方法直接得到链表第一个节点元素，poll方法不仅得到第一个节点元素，还会把节点删除。（peek表示偷看，即偷偷看一眼链表，poll表示获得民意选票）。

### 7. LinkedList set方法

```java
public E set(int index, E element) {
  this.checkElementIndex(index);
  LinkedList.Node<E> x = this.node(index);
  E oldVal = x.item;
  x.item = element;
  return oldVal;
}
```

先通过 get 方法的到当前下标的 node 节点，然后直接赋值即可。

### 8. LinkedList remove方法

```java
public E remove() {
    return this.removeFirst();
}
public E remove(int index) {
    this.checkElementIndex(index);
    return this.unlink(this.node(index));
}
public boolean remove(Object o) {
    LinkedList.Node x;
    if (o == null) {
        for(x = this.first; x != null; x = x.next) {
            if (x.item == null) {
                this.unlink(x);
                return true;
            }
        }
    } else {
        for(x = this.first; x != null; x = x.next) {
            if (o.equals(x.item)) {
                this.unlink(x);
                return true;
            }
        }
    }
    return false;
}
```

如果指定下标很简单，直接unlink即可；如果不指定下标而指定元素，就需要遍历一遍链表后找到元素node节点再unlink。

```java
public E removeFirst() {
    LinkedList.Node<E> f = this.first;
    if (f == null) {
        throw new NoSuchElementException();
    } else {
        return this.unlinkFirst(f);
    }
}
public E removeLast() {
    LinkedList.Node<E> l = this.last;
    if (l == null) {
        throw new NoSuchElementException();
    } else {
        return this.unlinkLast(l);
    }
}
```

这两个方法均调用unlink方法实现删除。

```java
E unlink(LinkedList.Node<E> x) {
    E element = x.item;
    LinkedList.Node<E> next = x.next;
    LinkedList.Node<E> prev = x.prev;
    if (prev == null) {
        this.first = next;
    } else {
        prev.next = next;
        x.prev = null;
    }
    if (next == null) {
        this.last = prev;
    } else {
        next.prev = prev;
        x.next = null;
    }
    x.item = null;
    --this.size;
    ++this.modCount;
    return element;
}
```

### 9. LinkedList 使用场景

根据LinkedList特点：

- 长度没有限制，适合不确定长度存储
- 插入删除快速，适合频繁修改场景
- 双向均可遍历，使遍历速度减少一半

### 10. 补充

`LinkedList` 集合底层实现的数据结构为双向链表

`LinkedList` 集合中元素允许为 null

`LinkedList` 允许存入重复的数据

`LinkedList` 中元素存放顺序为存入顺序。

`LinkedList` 是非线程安全的，如果想保证线程安全的前提下操作 `LinkedList`，可以使用 `List list = Collections.synchronizedList(new LinkedList(...));` 来生成一个线程安全的 `LinkedList`

```java
public static void main(String[] args) {
    //        简单列表
    List<String> list = new LinkedList<>();
    list.add("hujun");
    //        链表可以放入null，空值，但是站空间，长度会增长，还可以输出
    list.add(null);
    System.out.println(list.get(0));
    System.out.println(list.size());
    System.out.println(list.get(1));
    //      简单列表
    LinkedList<Object> list2 = new LinkedList<>();
    list2.add("12");
    list2.add(0,"hu");
    list2.add(null);
    System.out.println(list2.get(0));
    System.out.println(list2.size());

    //
    // 链表长度
    //transient int size = 0;
    //// 链表头节点
    //transient Node<E> first;
    //// 链表尾节点
    //transient Node<E> last;
    //        基于链表的队列
    Queue<String> queue = new LinkedList<>();
    queue.offer("hujun");
    System.out.println(queue.peek());
    queue.poll();

    LinkedList<Integer> doubleLink = new LinkedList<>();
    doubleLink.addFirst(1);
    doubleLink.addLast(2);
    doubleLink.removeFirst();
    doubleLink.removeLast();

    DequeTest();
}


public static void DequeTest(){
    Queue<String> queue = new LinkedList<>();
    queue.offer("a"); // 入队
    queue.offer("b"); // 入队
    queue.offer("c"); // 入队

    for (String q : queue) {
        System.out.println(q);
    }
    //      可以加入null
    queue.offer(null);
    queue.poll();
}
```

 