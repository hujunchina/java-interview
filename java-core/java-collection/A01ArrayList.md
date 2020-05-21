2020年5月20日 19:15:18

### 目录

1. ArrayList是什么
2. ArrayList继承关系
3. ArrayList成员属性
4. ArrayList构造方法
5. ArrayList add方法
6. ArrayList grow方法
7. ArrayList 应用场景
8. ArrayList 小结

### 1. ArrayList是什么

java.util.ArrayList是一个动态可变长数组，本质上是一个初始长度为10的定长 Object 数组。当超过初始长度时，使用重新分配堆大小方式对数组扩容1.5倍，以实现变长的特性。

使用泛型<E>表示可以接受不同类型元素。

### 2. ArrayList继承关系

```java
public class ArrayList<E> extends AbstractList<E> 
implements List<E>, RandomAccess, Cloneable, Serializable
---------------
public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E>
---------------
public abstract class AbstractCollection<E> implements Collection<E>
---------------
public interface List<E> extends Collection<E>
```

ArrayList继承AbstractList抽象类，该抽象类又实现List接口，List接口又继承Collection接口（Idea中按Ctrl+B快速定位到类声明文件）。

Java类的设计方式，一般是一个统一的接口，然后让一个抽象类去实现接口，最后再让具体的类继承抽象类（接口不能实现接口，因为接口没有实现的能力，里面是抽象方法）。

![img](C:\code\github\java-interview\img\arraylist-1.png)

### 3. ArrayList成员属性

```java
private static final long serialVersionUID = 8683452581122892189L;
private static final int DEFAULT_CAPACITY = 10;
private static final Object[] EMPTY_ELEMENTDATA = new Object[0];
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = new Object[0];
transient Object[] elementData;
private int size;
private static final int MAX_ARRAY_SIZE = 2147483639;
```

因为ArrayList实现了 Serializable 接口，所以设置了 serialVersionUID，避免反序列化时因不同序列号而找不到类报错。

关于序列化，会给序列化类关联一个ID，称为序列号，在反序列化过程中，用于验证序列化类的发送者和接收者是否已经加载了该序列化类。如果不指明序列号，会自动生成一个序列号。这个生成过程根据 JVM 版本、类的定义等不同导致序列号不一样，那么多次加载的时候就会报错 InvalidClassException。

如A引用了B，同时对A和B序列化，A是接受者会保留B的序列号，B是发送者。如果对B修改再次序列化，那么反序列化时，A就会报错找不到B。因为对B的两次序列化，得到的序列号不一样。

数组容量为0，只有真正对数据进行添加add时，才分配DEFAULT_CAPACITY 大小为10，并把初始数组类型定为 Object，这也解释了为什么不能用基本类型，而要用int 的引用类型 Integer。因为放入 ArrayList 中的元素必须要继承 Object，其默认数组类型就是 Object，不然无法存储。

定义了一个实时大小 size，和最大的容量 2147483639，21亿多。

实验不停插入，结果报java.lang.OutOfMemoryError: Java heap space 错误。

### 4. ArrayList构造方法

```java
public ArrayList(int initialCapacity) {
    if (initialCapacity > 0) {
        this.elementData = new Object[initialCapacity];
    } else {
        if (initialCapacity != 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
        this.elementData = EMPTY_ELEMENTDATA;
    }
}
public ArrayList() {
    this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
}
public ArrayList(Collection<? extends E> c) {
    this.elementData = c.toArray();
    if ((this.size = this.elementData.length) != 0) {
        if (this.elementData.getClass() != Object[].class) {
            this.elementData = 
            Arrays.copyOf(this.elementData, this.size, Object[].class);
        }
    } else {
        this.elementData = EMPTY_ELEMENTDATA;
    }
}
```

ArrayList 构造方法有三个。前两个是指定数组大小的，说明在初始化时可以控制数组大小。但并没有给size=initialCapacity，所以我们获得大小还是存多少是多少，并不是初始大小。

由于没有用到泛型，还说明可以在 arraylist 中混合放入不同类型。

第三个构造方法，可以传递一个 Collection 子类集合，来把元素直接复制到里面。这里使用了泛型的 extends 表示上界，表示集合中的元素范围要比 ArrayList 声明的元素小，？extends  extends E 。原因是保证加入的元素可以放入 ArrayList 中，不会报类型错误。

这里还使用 getClass 获得方法区的类结构信息与object数组类结构比较。

如果 toArray 方法返回的不是 Object[] 类型，比如我们自己定义一个实现 Collection 的类并覆盖重写了 toArray，但返回的是 String[] 类型，这个时候就会调用 Arrays.copyOf 方法，并传入参数 Object[].class 强制转为 Object[] 类型。

```java
public static <T, U> T[] copyOf(
    U[] original, int newLength, Class<? extends T[]> newType) {
    T[] copy = 
        newType == Object[].class ? 
        new Object[newLength] : 
        (Object[])Array.newInstance(newType.getComponentType(), newLength);
    System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
    return copy;
}
```

### 5. ArrayList add方法

当我们实例化一个未指定大小的 ArrayList 的对象时，其默认大小不是10而是0，只有在第一次放入元素时才更改大小为10。这样设计很巧妙，节省堆内存并提高运行效率，因为实例化后不知道用不用呢，object  数组大小为0即节省堆内存又快速。那么 add 方法是如何做的呢？

```java
private void add(E e, Object[] elementData, int s) {
    if (s == elementData.length) {
        elementData = this.grow();
    }
    elementData[s] = e;
    this.size = s + 1;
}
public boolean add(E e) {
    ++this.modCount;
    this.add(e, this.elementData, this.size);
    return true;
}
public void add(int index, E element) {
    this.rangeCheckForAdd(index);
    ++this.modCount;
    int s;
    Object[] elementData;
    if ((s = this.size) == (elementData = this.elementData).length) {
        elementData = this.grow();
    }
    // 空出一个位置
    System.arraycopy(elementData, index, 
                     elementData, index + 1, 
                     s - index);
    // 插入元素
    elementData[index] = element;
    this.size = s + 1;
}
```

ArrayList提供了三个add方法，一个private，两个public，其实我们只能用后两个add方法。

- add(E e) 直接把元素插入到ArrayList中
- add(int index, E element) 指定插入下标，并插入元素

代码写的很严谨，上来就把modCount加1，这个变量是在父类AbstractList声明的。

`protected transient int modCount = 0;`

源代码使用 protected 修饰表示最大访问权限到子类，然后使用 transient 修饰表示不序列化这个属性。modCount 表示操作次数，用于并发。

随后调用私有 add 方法。在其方法中调用了 grow() 方法来增加长度，并把要插入元素放入最后并更新size。

所以准确讲，是容量**等于**长度时，才扩容。

对第二个add方法，下标必定在新扩容长度的范围内。这就引出了一个问题，我们如何高效的插入这个元素呢？肯定不是一个一个往后移然后插入。

![img](C:\code\github\java-interview\img\arraylist-2.png)

这里使用arraycopy方法后半段整体复制的方式插入。

```java
arraycopy(Object src, int srcPos, 
          Object dest, int destPos, 
          int length)
Copies an array from the specified source array, 
beginning at the specified position, 
to the specified position of the destination array.
```

Java文档写的从起始下标位置复制一定长度的元素到目的下标位置。

即直接把[index, size]后半段数组整体复制到[index+1, size+1]位置，这样就留出了index位置，插入元素。

### 6. ArrayList grow方法

在add方法中使用grow方法来扩容ArrayList。

```java
private Object[] grow(int minCapacity) {
    return this.elementData = 
    Arrays.copyOf(this.elementData, this.newCapacity(minCapacity));
}
private Object[] grow() {
    return this.grow(this.size + 1);
}
private int newCapacity(int minCapacity) {
    int oldCapacity = this.elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    if (newCapacity - minCapacity <= 0) {
        if (this.elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
            return Math.max(10, minCapacity);
        } else if (minCapacity < 0) {
            throw new OutOfMemoryError();
        } else {
            return minCapacity;
        }
    } else {
        return newCapacity - 2147483639 <= 0 ? newCapacity : hugeCapacity(minCapacity);
    }
}
```

grow 方法调用了 copyof 方法来复制数组，参数中又调用 newCapacity 方法。

在 newCapacity 方法中，我们可以看到 oldCapacity+(oldCapacity >> 1)，表示数组容量变为原来的1.5倍。如果没有超过10，则设置为10。

```java
public static <T, U> T[] copyOf(
    U[] original, int newLength, Class<? extends T[]> newType) {
    T[] copy = 
        newType == Object[].class ? 
        new Object[newLength] : 
        (Object[])Array.newInstance(newType.getComponentType(), newLength);
    System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
    return copy;
}
```

这里copyof方法是先实例化一个新容量大小的object数组，然后把旧的复制过去。这样说明了ArrayList扩容的方式不是直接分配堆内存，而是实例化新的对象并复制元素。

copyof 底层也是用 System.arraycopy 来操作复制的。

### 7. ArrayList 应用场景

谈应用场景，并不需要特别指明某一个业务，只要要结合ArrayList自身特点指明应用一个大的方面即可。

- ArrayList可变长，可以用于存储无法确定长度的场景
- ArrayList可快速获得数据，可以用于频繁查找和获取数据上
- ArrayList支持泛型，可以用于对象存储，方便管理对象。

### 8. ArrayList 小结

需要知道如下知识点：

- ArrayList是什么？
- ArrayList扩容怎么实现的？
- ArrayList容量满的时候，如何高效插入？
- ArrayList应用场景？

### 9. 补充

虽然初始化了大小，但是对 object 数组的大小，而不是 size，故不能 set(5, 1) 。

![img](C:\code\github\java-interview\img\arraylist-3.png)

论遍历ArrayList要比LinkedList快得多，ArrayList遍历最大的优势在于内存的连续性，CPU的内部缓存结构会缓存连续的内存片段，可以大幅降低读取内存的性能开销。

ArrayList 不适合用作队列，适合用作堆栈；数组适合用作队列。

可以放入空值 null 。

```java
public static void main(String[] args) {
//  构造方法
//        transient Object[] elementData;
//        private int size;
    List<Integer> list = new ArrayList<>();
    List<Integer> list2 = new ArrayList<>(10);

    for (int i = 0; i < 10; i++) {
        list.add(i);
    }
    list.add(6, 10);
    //System.arraycopy(elementData, 3, elementData, 2, numMoved);
    //        把【3，end】直接复制到【2，end】位置
    list.remove(2);

    //        测试能不能放入空值
    List<Object> list3 = new ArrayList<>(5);
    list3.add("hujun");
    list3.add(null);
    //        可以放入null，占位置，可以输出
    System.out.println(list3.size());
    System.out.println(list3.get(1));

}

public static void array2(){
    int[] a = {1,2,3};
    int[] b;
    b = new int[]{1,2,3};
    int[] c = new int[5];
    System.out.println(c[0]);
    //        int[] 有长度属性
    int len = c.length;

    ArrayList<Integer> test = new ArrayList<>();
    for(int i=0; i<2247; i++){
        for(int j=0; j<83; j++){
            test.add(i+j);
        }
    }
    //        2 dims array
    int[][] arr2d = {{1,2,3},{4,5,6}};
    //       局部变量未初始化不会赋初值，编译报错
    //        int x;
    //        System.out.println(x);

    String[] as = {"a","C","b","D"};
    Arrays.sort(as,String.CASE_INSENSITIVE_ORDER);
    for(int i = 0; i < as.length; i ++) {
        System.out.print(as[i] + "\t");
    }
    Arrays.sort(as, Collections.reverseOrder());//Collections 集合类中的工具类
    for(int i = 0; i < as.length; i ++) {
        System.out.print(as[i] + "\t");
    }
}
```

