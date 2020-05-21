2020年5月21日 10:53:33

### 目录

1. RandomAccessFile类
2. 输入输出字节流
3. 输入输出字符流
4. 文件通道
5. 数据流
6. 标准Stream写法



### 1. RandomAccessFile类

文件读写, RandomAccessFile会打开文件从头开始写，然后覆盖文件内容，严格按照字符长度来读写。

用此类可以存储固定长度，并读取固定长度的数据，对空间极大的利用，压缩存储空间。

```java
public static void main(String[] args) throws IOException {
    String filename = "randomaccessfile.txt";
    File file = new File(filename);
    RandomAccessFile rdf = new RandomAccessFile(file, "rw");

    String name = null;
    int age = 0;

    name = "zhangsan"; // 字符串长度为8
    age = 30; // 数字的长度为4
    rdf.writeBytes(name); // 将姓名写入文件之中
    rdf.writeInt(age); // 将年龄写入文件之中

    name = "lisi    "; // 字符串长度为8
    age = 31; // 数字的长度为4
    rdf.writeBytes(name); // 将姓名写入文件之中
    rdf.writeInt(age); // 将年龄写入文件之中

    name = "wangwu  "; // 字符串长度为8
    age = 32; // 数字的长度为4
    rdf.writeBytes(name); // 将姓名写入文件之中
    rdf.writeInt(age); // 将年龄写入文件之中

    rdf.close(); // 关闭

    readFile(filename);
}

public static void readFile(String filename) throws IOException {
    File f = new File(filename);    // 指定要操作的文件
    RandomAccessFile rdf = null;        // 声明RandomAccessFile类的对象
    rdf = new RandomAccessFile(f, "r");// 以只读的方式打开文件
    String name = null;
    int age = 0;
    byte b[] = new byte[8];    // 开辟byte数组
    // 读取第二个人的信息，意味着要空出第一个人的信息
    rdf.skipBytes(12);        // 跳过第一个人的信息
    for (int i = 0; i < b.length; i++) {
        b[i] = rdf.readByte();    // 读取一个字节
    }
    name = new String(b);    // 将读取出来的byte数组变为字符串
    age = rdf.readInt();    // 读取数字
    System.out.println("第二个人的信息 --> 姓名：" + name + "；年龄：" + age);
    // 读取第一个人的信息
    rdf.seek(0);    // 指针回到文件的开头
    for (int i = 0; i < b.length; i++) {
        b[i] = rdf.readByte();    // 读取一个字节
    }
    name = new String(b);    // 将读取出来的byte数组变为字符串
    age = rdf.readInt();    // 读取数字
    System.out.println("第一个人的信息 --> 姓名：" + name + "；年龄：" + age);
    rdf.skipBytes(12);    // 空出第二个人的信息
    for (int i = 0; i < b.length; i++) {
        b[i] = rdf.readByte();    // 读取一个字节
    }
    name = new String(b);    // 将读取出来的byte数组变为字符串
    age = rdf.readInt();    // 读取数字
    System.out.println("第三个人的信息 --> 姓名：" + name + "；年龄：" + age);
    rdf.close();                // 关闭
}
```



### 2. 输入输出字节流

流分为字节流和字符流两种，有流向就有两个端，这两个端就是内存和硬盘，从内存到硬盘叫输出流，反过来叫输入流。

字节流都是继承inputstream和Outputstream抽象类。

根据流的用途，分为二进制、文件、数据、管道、序列化、过滤。

```java
//    内存字节流，把数据存储到内存中，在内存中存储一些临时信息，所以参数是数组
//    从内存A到内存B
public static void BinaryStream() throws IOException {
    byte[] memData = {1,1,1,1,1,1,1,1,1,1,1,1,1,-1};
    // 不用写入，memData本身就在内存中，只要指定流的入口
    InputStream is = new ByteArrayInputStream(memData);     

    int tag = 0;
    //流循环读取，最后一个是-1标识文件结束，这是流的出口，到tag上了
    while((tag = is.read())!=-1){       
        System.out.println((byte)tag);  // 输出
    }
    is.close();
}
```

```java
//    文件流，对文件读写
public static void FileStream() throws IOException {
    String filename = "randomaccessfile.txt";

    FileInputStream fis = new FileInputStream(filename);
    FileOutputStream fos = new FileOutputStream(filename);

    fos.write("hujun".getBytes());
    fos.close();

    //        int tag = 0;
    //        while((tag=fis.read())!=-1){
    //            System.out.println((char)tag);
    //        }
    //        上面代码，已经把文件指针读到尾了

    File f = new File(filename);
    byte[] ret = new byte[(int) f.length()];
    fis.read(ret);
    System.out.println(new String(ret));
    fis.close();
}
```

```java
//    管道流，管道流的主要作用是可以进行两个线程间的通信
//    如果要进行管道通信，则必须把 PipedOutputStream 连接在 PipedInputStream 上。
//    为此，PipedOutputStream 中提供了 connect() 方法
public static void PipedStream() throws IOException {

    Send sender = new Send();
    Receive receiver = new Receive();

    sender.getConnect(receiver.putPipe());
    new Thread(sender).start();
    new Thread(receiver).start();

    //   每个汉字转为字节是不一样的，有的是3个有的是4个
    System.out.println("息".length()+ " "+ "息".getBytes().length);  // 1 3
    System.out.println("一".length()+ " "+ "一".getBytes().length);  // 1 3
    System.out.println("a".length()+ " "+ "a".getBytes().length);  // 1 1
    System.out.println("这是来自另一个线程的信".getBytes().length);  // 33
    System.out.println("这是来自另一个线程的信息".getBytes().length);  // 36
}
static class Send implements Runnable{
    PipedOutputStream pos = new PipedOutputStream();;

    public void getConnect(PipedInputStream pis) throws IOException {
        pos.connect(pis);
    }

    @Override
    public void run() {
        try {
            pos.write("这是来自另一个线程的信息".getBytes());
            pos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

static class Receive implements Runnable{
    PipedInputStream pis = new PipedInputStream();
    public PipedInputStream putPipe(){
        return pis;
    }
    @Override
    public void run() {
        try {
            // 12个汉字，转为byte是多长？ 一个汉字3个字节，12个一共36个字节,  存储在数组中占了36个字节
            byte[] ret = new byte[36];
            pis.read(ret);
            System.out.println(ret.length+" "+Arrays.toString(ret));
            System.out.println(new String(ret));
            pis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### 3. 输入输出字符流

#### 3.1 字符流

字符流有两个顶层抽象类Reader和Writer，派生出字符串、缓冲区、管道、文件、过滤、转换等流。

一个字符等于两个字节,  但是转为byte时是占3个长度。

计算机一次读的字节称为编码单位(英文名叫Code Unit)，也叫码元， byte中汉字占3个码元。

UTF-8中，如果首字节以1110开头，肯定是三字节编码(3个码元)。

字节流在操作时本身不会用到缓冲区（内存），是文件本身直接操作的。

字符流在操作时是使用了缓冲区，通过缓冲区再操作文件。

除了纯文本数据文件使用字符流以外，其他文件类型都应该使用字节流方式。

#### 3.2 实战

```java
//    文件字符流
public static void FileRW() throws IOException {
    File file = new File("randomaccessfile.txt");
    FileReader fr = new FileReader(file);
    FileWriter fw = new FileWriter(file);

    fw.write("一个字符等于两个字节");
    fw.close();

    char[] ret = new char[10];
    fr.read(ret);
    System.out.println(new String(ret));
    fr.close();
}
//    字节转字符
public static void StreamToChar() throws FileNotFoundException {
    File file = new File("randomaccessfile.txt");
    InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
    OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file));
}
```

#### 3.3 文件操作

```java
static class FileOperator{
    private File f = null;
    public FileOperator(File f){
        this.f = f;
    }
    public void creteFile(){
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void deleteFile(){
        f.delete();
    }
    public void createFold(){
        if(f.isDirectory()){
            f.mkdir();
        }
    }
    public void listFiles(){
        String str[] = f.list();
        for(String s:str){
            System.out.println(str);
        }
    }
    public void show(){
        System.out.println(File.pathSeparator);
        System.out.println(File.separator);
    }

    public void writeLine(String line) throws IOException {
        OutputStream out = new FileOutputStream(f);
        out.write(line.getBytes());
        out.close();
    }
    public void writeAppendLine(String line) throws IOException {
        OutputStream out = new FileOutputStream(f, true);
        out.write(line.getBytes());
        out.close();
    }
    public String readLine() throws IOException {
        String line = null;
        InputStream input = new FileInputStream(f);
        byte[] b = new byte[1024];
        input.read(b);
        input.close();
        line = new String(b);
        input.close();
        return line;
    }
    public byte[] readBytes() throws IOException {
        byte[] line = new byte[(int)f.length()];
        InputStream input = new FileInputStream(f);
        int i=0;
        while(input.read()!=-1){
            line[i++] = (byte)input.read();
        }
        input.close();
        return line;
    }
    public void writeWord(String line) throws IOException {
        Writer writer = new FileWriter(f);
        writer.write(line);
        writer.append(line);
        writer.close();
    }
    public String readWord() throws IOException {
        String line = null;
        Reader reader = new FileReader(f);
        char c[] = new char[1024];
        int len = reader.read(c);
        reader.close();
        line = new String(c, 0, len);
        return line;
    }
    public void streamWriter(String line) throws IOException {
        Writer out = new OutputStreamWriter(new FileOutputStream(f));
        out.write(line);
    }
}
```

### 4. 文件通道

文件通道，NIO不仅有网络传输的socket，也有文件传输的通道。

NIO是非阻塞IO，面向Buffer缓冲区，以块为单位传输数据，支持双向读写。

虽然是面向缓冲区的，但是还是面向字节而不是字符。FileChannel是阻塞的。

```java
public static void fastCopy(String filename, String dist) throws IOException {
    FileInputStream fis = new FileInputStream(filename);
    FileChannel fcin = fis.getChannel();

    FileOutputStream fos = new FileOutputStream(dist);
    FileChannel fcout= fis.getChannel();

    // 通道读写需要先设置缓冲区
    ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
    while (true) {
        /* 从输入通道中读取数据到缓冲区中 */
        int r = fcin.read(buffer);
        /* read() 返回 -1 表示 EOF */
        if (r == -1) {
            break;
        }
        /* 切换读写 */
        buffer.flip();
        /* 把缓冲区的内容写入输出文件中 */
        fcout.write(buffer);
        /* 清空缓冲区 */
        buffer.clear();
    }
}
```

### 5. 数据流

```java
public static void readData(String file) throws IOException {
    FileInputStream fis = new FileInputStream(file);
    DataInputStream dis = new DataInputStream(fis);
    int i = dis.readInt();
    boolean b = dis.readBoolean();
    char c = dis.readChar();
    //        can not read string
    String str = dis.readUTF();
    //        String str = "can not read string";
    double pi = dis.readDouble();
    System.out.format("%d,%b,%c,%s,%f%n", i, b, c, str, pi);

}
public static void main(String[] args) throws IOException {
    String file ="Base01/src/cn/edu/zju/how2jcn/middle/datastream.txt";
    FileOutputStream fos = new FileOutputStream(file);
    DataOutputStream dos = new DataOutputStream(fos);
    int i = 10;
    boolean b = true;
    char c = '胡';
    String str = "hujun";
    double pi = 3.14;
    dos.writeInt(i);
    dos.writeBoolean(b);
    dos.writeChar(c);
    dos.writeUTF(str);
    dos.writeDouble(pi);
    dos.close();

    readData(file);
}
```

### 6. 标准Stream写法

```java
public static void GBK2UTF8(String filename) throws IOException {
    BufferedReader br = 
        new BufferedReader(new InputStreamReader(new FileInputStream(filename), Charset.forName("GBK")));
    BufferedWriter bw = 
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8));
    String line = null;
    while((line = br.readLine())!=null) {
        bw.write(line);
    }
    br.close();
    bw.close();
}
```

选取流的标准，如果是对纯文本文件如日志等读写，使用字符流。而且要使用buffer缓存。

其他的文件如二进制等要使用字节流，但是也要buffer缓存，就要多做一个转换（字节流转字符流）。

```java
FileInputStream fis = new FileInputStream("readme.txt");
FileOutputStream fos = new FileOutputStream("reademe.txt");
FileReader fr = new FileReader("readme.txt");
FileWriter fw = new FileWriter("readme.txt");
InputStreamReader isr = new InputStreamReader(is);
OutputStreamWriter osw = new OutputStreamWriter(os);
DataInputStream dis = new DataInputStream(fis);
DataOutputStream dos = new DataOutputStream(fos);
ObjectInputStream ois = new ObjectInputStream(is);
ObjectOutputStream oos = new ObjectOutputStream(os);
BufferedReader br = new BufferedReader(isr);
BufferedWriter bw = new BufferedWriter(osw);
PrintStream ps = new PrintStream("readme.txt");
PrintWriter pw = new PrintWriter("readme.txt");
```

