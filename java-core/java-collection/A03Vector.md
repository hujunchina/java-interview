2020年5月20日 21:50:50

### 目录

1. 介绍
2. 原理
3. 源码



### 1. 介绍

底层实现与 ArrayList 类似，不过Vector 是线程安全的，而ArrayList 不是。

```java
public class Vector<E>  extends AbstractList<E>
    implements List<E>, RandomAccess, Cloneable, java.io.Serializable
```

**Vector** 是一个矢量队列，它的依赖关系跟 **ArrayList**  是一致的，因此它具有一下功能：

- 1、**Serializable**：支持对象实现序列化，虽然成员变量没有使用 transient 关键字修饰，Vector 还是实现了 writeObject() 方法进行序列化。
- 2、**Cloneable**：重写了 clone（）方法，通过 Arrays.copyOf（） 拷贝数组。
- 3、**RandomAccess**：提供了随机访问功能，我们可以通过元素的序号快速获取元素对象。
- 4、**AbstractList**：继承了AbstractList ，说明它是一个列表，拥有相应的增，删，查，改等功能。
- 5、**List**：留一个疑问，为什么继承了 AbstractList 还需要 实现List 接口？

#### 1.1 成员变量

```java
/**
        与 ArrayList 中一致，elementData 是用于存储数据的。
     */
protected Object[] elementData;

/**
     * The number of valid components in this {@code Vector} object.
      与ArrayList 中的size 一样，保存数据的个数
     */
protected int elementCount;

/**
     * 设置Vector 的增长系数，如果为空，默认每次扩容2倍。
     *
     * @serial
     */
protected int capacityIncrement;

// 数组最大值
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
```

#### 1.2 构造函数

```java
//默认构造函数
public Vector() {
    this(10);
}

//带初始容量构造函数
public Vector(int initialCapacity) {
    this(initialCapacity, 0);
}

//带初始容量和增长系数的构造函数
public Vector(int initialCapacity, int capacityIncrement) {
    super();
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal Capacity: "+
                                           initialCapacity);
    this.elementData = new Object[initialCapacity];
    this.capacityIncrement = capacityIncrement;
}
```

### 2. 原理

#### 2.1 Add 方法

```java
public synchronized void addElement(E obj) {
    modCount++;
    ensureCapacityHelper(elementCount + 1);
    elementData[elementCount++] = obj;
}
```

```java
public void add(int index, E element) {
    insertElementAt(element, index);
}

public synchronized void insertElementAt(E obj, int index) {
    modCount++;
    if (index > elementCount) {
        throw new ArrayIndexOutOfBoundsException(index
                                                 + " > " + elementCount);
    }
    ensureCapacityHelper(elementCount + 1);
    System.arraycopy(elementData, index, elementData, index + 1, elementCount - index);
    elementData[index] = obj;
    elementCount++;
}

```



#### 2.2 Grow 方法

```java
private void grow(int minCapacity) {
    int oldCapacity = elementData.length;
    //区别与ArrayList 中的位运算，这里支持自定义增长系数
    int newCapacity = oldCapacity + ((capacityIncrement > 0) ?
                                     capacityIncrement : oldCapacity);
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    elementData = Arrays.copyOf(elementData, newCapacity);
}
```



#### 2.3 删除方法

```java
public synchronized void removeElementAt(int index) {
    modCount++;
    if (index >= elementCount) {
        throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);
    }
    else if (index < 0) {
        throw new ArrayIndexOutOfBoundsException(index);
    }
    int j = elementCount - index - 1;
    if (j > 0) {
        System.arraycopy(elementData, index + 1, elementData, index, j);
    }
    elementCount--;
    elementData[elementCount] = null; /* to let gc do its work */
}


public synchronized E remove(int index) {
    modCount++;
    if (index >= elementCount)
        throw new ArrayIndexOutOfBoundsException(index);
    E oldValue = elementData(index);

    int numMoved = elementCount - index - 1;
    if (numMoved > 0)
        System.arraycopy(elementData, index+1, elementData, index,
                         numMoved);
    elementData[--elementCount] = null; // Let gc do its work

    return oldValue;
}
```



### 3. 原理

Vector 中的每一个独立方法都是线程安全的，因为它有着 synchronized 进行修饰。但是如果遇到一些比较复杂的操作，并且多个线程需要依靠 vector 进行相关的判断，那么这种时候就不是线程安全的了。

```java
if (vector.size() > 0) {
    System.out.println(vector.get(0));
}
```

如上述代码所示，Vector 判断完 size（）>0 之后，另一线程如果同时清空vector 对象，那么这时候就会出现异常。因此，在复合操作的情况下，Vector 并不是线程安全的。

vector 只有在插入删除是线程安全的，而对于长度和容量的处理不是线程安全的。

```java
Vector<String> v = new Vector<>();
v.add("hujun");
System.out.println(v.capacity());
System.out.println(v.get(0));
```

底层存储以 arraylist 一样，所以可以存储 null。