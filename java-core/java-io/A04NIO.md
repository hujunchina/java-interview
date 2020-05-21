2020年5月21日 12:11:31

### 目录

1. NIO概念
2. NIO原理
3. NIO实践
4. NIO总结

### 1. NIO概念

回答NIO，从四点出发：非阻塞、Buffer缓冲区、Channel通道、Selector选择器。

NIO需要解决的最根本的问题就是存在于BIO中的两个阻塞，分别是等待连接时的阻塞和等待数据时的阻塞。

在真实NIO中，并不会在Java层上来进行一个轮询，而是将轮询的这个步骤交给我们的操作系统来进行，他将轮询的那部分代码改为操作系统级别的系统调用（select函数，在linux环境中为epoll），在操作系统级别上调用select函数，主动地去感知有数据的socket。

NIO 的 IO 行为还是同步的,在 IO 操作准备好时，业务线程得到通知，接着就由这个线程自行进行 IO 操作，IO操作本身是同步的。

测试了一下，读写确实还是单线程的，很不好，而且空轮询bug会使CPU飙升，从1%到40%左右。

NIO以上两点缺点+代码复杂容易出bug，很难用，改用NIO 2.0 即 AIO。

### 2. NIO原理

NIO需要解决的**最根本的问题**就是存在于BIO中的两个阻塞，分别是**等待连接时的阻塞**和**等待数据时的阻塞**。

我们需要再老调重谈的一点是，如果单线程服务器**在等待数据时阻塞**，那么第二个连接请求到来时，服务器是**无法响应**的。如果是多线程服务器，那么又会有**为大量空闲请求产生新线程**从而造成线程占用系统资源，**线程浪费**的情况。

那么我们的问题就转移到，**如何让单线程服务器在等待客户端数据到来时，依旧可以接收新的客户端连接请求**。

我们在之前实现了一个使用Java做多个客户端连接轮询的逻辑，但是在真正的NIO源码中其实并不是这么实现的，NIO使用了操作系统底层的轮询系统调用 select/epoll(windows:select,linux:epoll)，那么为什么不直接实现而要去调用系统来做轮询呢？

![select](C:\code\github\java-interview\img\nio-1.png)

假设有A、B、C、D、E五个连接同时连接服务器，那么根据我们上文中的设计，程序将会遍历这五个连接，轮询每个连接，获取各自数据准备情况，那么**和我们自己写的程序有什么区别呢**？

首先，我们写的Java程序其本质在轮询每个Socket的时候也需要去调用系统函数，那么轮询一次调用一次，会造成不必要的上下文切换开销。

而Select会将五个请求从用户态空间**全量复制**一份到内核态空间，在内核态空间来判断每个请求是否准备好数据，完全避免频繁的上下文切换。所以效率是比我们直接在应用层写轮询要高的。

如果select没有查询到到有数据的请求，那么将会一直阻塞（是的，select是一个阻塞函数）。如果有一个或者多个请求已经准备好数据了，那么select将会先将有数据的文件描述符**置位**，然后select返回。返回后通过**遍历**查看哪个请求有数据。

**select的缺点**：

1. 底层存储依赖bitmap，处理的请求是有上限的，为1024。
2. 文件描述符是会置位的，所以如果当被置位的文件描述符需要重新使用时，是需要重新赋空值的。
3. fd（文件描述符）从用户态拷贝到内核态仍然有一笔开销。
4. select返回后还要再次遍历，来获知是哪一个请求有数据。

#### 2.2 poll函数底层逻辑

poll的工作原理和select很像，先来看一段poll内部使用的一个结构体。

```c++
struct pollfd{
    int fd;
    short events;
    short revents;
}
```

poll同样会将所有的请求拷贝到内核态，和select一样，poll同样是一个阻塞函数，当一个或多个请求有数据的时候，也同样会进行置位，但是它置位的是结构体pollfd中的events或者revents置位，而不是对fd本身进行置位。

所以在下一次使用的时候不需要再进行重新赋空值的操作。poll内部存储**不依赖bitmap**，而是使用pollfd**数组**的这样一个数据结构，数组的大小肯定是大于1024的。解决了select 1、2两点的缺点。

#### 2.3 epoll

epoll是最新的一种多路IO复用的函数。这里只说说它的特点。

epoll和上述两个函数最大的不同是，它的fd是**共享**在用户态和内核态之间的，所以可以不必进行从用户态到内核态的一个拷贝，这样可以节约系统资源；另外，在select和poll中，如果某个请求的数据已经准备好，它们会将所有的请求都返回，供程序去遍历查看哪个请求存在数据，但是epoll只会返回存在数据的请求，这是因为epoll在发现某个请求存在数据时，首先会进行一个**重排**操作，将所有有数据的fd放到最前面的位置，然后返回（返回值为存在数据请求的个数N），那么我们的上层程序就可以不必将所有请求都轮询，而是直接遍历epoll返回的前N个请求，这些请求都是有数据的请求。

### 3. NIO 实践

```java
final static int PORT = 60000;

static class NIOServer implements Runnable{
    //   1. serverSelector负责轮询是否有新的连接，服务端监测到新的连接之后，不再创建一个新的线程，
    //    // 而是直接将新连接绑定到clientSelector上，这样就不用 IO 模型中 1w 个 while 循环在死等
    //        轮询服务端accept客户端
    Selector selectorServer;
    //        轮询客户端IO读写
    Selector selectorClient;
    //       服务端socket类似ServerSocket
    ServerSocketChannel serverSocketChannel = null;
    //        获得一个socket来操作
    Socket socket = null;

    public NIOServer() throws IOException {
        selectorServer = Selector.open();
        selectorClient = Selector.open();
        //            初始化服务端通道
        serverSocketChannel = ServerSocketChannel.open();
        //            绑定通道内socket端口
        serverSocketChannel.socket().bind(new InetSocketAddress(PORT));
        //            设置为非阻塞
        serverSocketChannel.configureBlocking(false);
        //            注册到选择器上
        serverSocketChannel.register(selectorServer, SelectionKey.OP_ACCEPT);
    }
    public Selector getSelectorClient(){
        System.out.println("服务端启动");
        return selectorClient;
    }
    @Override
    public void run(){
        while(true) {
            // 监测是否有新的连接，这里的1指的是阻塞的时间为 1ms
            try {
                if(selectorServer.select(1) > 0){
                    //                        选择器获得有读写信号的channel
                    Set<SelectionKey> selectionKeys = selectorServer.selectedKeys();
                    for(SelectionKey key : selectionKeys){
                        if( key.isAcceptable()){
                            // (1) 每来一个新连接，不需要创建一个线程，而是直接注册到clientSelector
                            SocketChannel socketChannel = ((ServerSocketChannel)key.channel()).accept();
                            socketChannel.configureBlocking(false);
                            socketChannel.register(selectorClient, SelectionKey.OP_READ);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

static class NIOOperator implements Runnable{
    Selector selectorClient;
    public NIOOperator(Selector selectorClient){
        this.selectorClient = selectorClient;
    }
    @Override
    public void run() {
        //            select返回后还要再次遍历，来获知是哪一个请求有数据。
        while(true){
            try {
                // (2) 批量轮询是否有哪些连接有数据可读，这里的1指的是阻塞的时间为 1ms
                if( selectorClient.select(1) > 0){
                    Set<SelectionKey> selectedKeys = selectorClient.selectedKeys();
                    for(SelectionKey key : selectedKeys){
                        if(key.isReadable()){
                            SocketChannel socketChannel = (SocketChannel) key.channel();
                            //                                缓冲区
                            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
                            // (3) 面向 Buffer
                            socketChannel.read(buffer);
                            //                                转为写
                            buffer.flip();
                            System.out.println(Charset.defaultCharset().newDecoder().decode(buffer).toString());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
public static String getDate(){
    return new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]").format(new Date());
}
public static void Msg(String msg){
    System.out.println(msg);
}
//   客户端
static class SocketClient implements Runnable{
    Socket socket = null;
    String name = null;
    PrintWriter out = null;
    public SocketClient(String name) throws IOException {
        this.name = name;
        socket = new Socket("127.0.0.1", PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        Msg("客户端"+name+"启动");
    }
    @Override
    public void run() {
        while(true){
            out.println(getDate()+" MSG from "+name);
            //                Msg("客户端已写入数据");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Msg("client sleep interrupted");
            }
        }
    }
}

public static void main(String[] args) throws IOException {
    NIOServer nioServer = new NIOServer();
    new Thread(nioServer).start();
    new Thread(new NIOOperator(nioServer.getSelectorClient())).start();
    for (int i = 0; i < 5; i++) {
        new Thread(new SocketClient("client"+i)).start();
    }
}
```

### 4. NIO总结

#### 4.1 **Java流概述** 

流stream是数据流动的方式，In表示从源流入到内存中（Java进程中），out表示从内存流出到目标中。In输入有read方法，读取源到内存中，out输出有write方法，从内存写出到目标中。

![img](C:\code\github\java-interview\img\nio-2.png)

根据数据的流向可分为输入流和输出流，InputStream，OutputStream，Reader，Writer有4个最基本的流

根据读取的位大小分为字节流和字符流，字节是Stream结尾，字符是XXer结尾，可以使用InputStreamReader，OutputStreamWriter两个类做字节流和字符流的转换。

#### 4.2 **Java流之BIO** 

BIO即Blocked InputOutput，同步阻塞流，阻塞的输入输出流。特点是其读写会阻塞线程。Linux中应用进程通过调用recvfrom获取磁盘或内存中数据时，内核如果没有准备好数据就不会立即返回，这样该进程就要一直等待，直到内核返回读写结果。这里的阻塞是因为，Java进程线程读写数据时，CPU会停下来等待磁盘读写而造成CPU等待，造成资源浪费。BIO专指Java中基本的流，因为他们都是阻塞的非异步的。

BIO缺点很明显，不适合高并发场景。典型的CPU与外设速度不匹配。

#### 4.3 Java流之NIO

NIO即Non-blocked InputOutput，同步非阻塞流。

同步非阻塞IO ：同步即Java线程发出读写请求一直等待返回结果才执行下一步，非阻塞即不在让CPU等待了，CPU可以去帮其他线程。虽然CPU资源利用起来了，但需要频繁的切换线程，直到这个线程获得读写资源。也是很低效浪费资源。

NIO支持面向缓冲的|基于通道的 I/O 操作方法。

NIO 提供了与传统 BIO 模型中的socket 和 serverSocket 相对应的 socket-Channel 和 serverSocketChannel两种不同的套接字通道实现。可开发高负载、高并发的应用。

![img](C:\code\github\java-interview\img\nio-3.png)

NIO 底层是通过Selecter线程管理多路复用IO的多个连接，当其中一个channel把数据准备好的时候会发送一个可读事件给selecter线程，然后线程调用socket进行读操作。

![img](C:\code\github\java-interview\img\nio-4.png)

#### 4.4 NIO与BIO区别 

1.  NIO非阻塞，BIO阻塞
2. NIO面向缓冲区buffer，BIO面向流，上图所示NIO中的channel准备好数据后都是在buffer中，NIO读写都是基于一个buffer对象。
3. NIO通过channel双向读写，BIO通过流单向，通道只有和buffer结合才能实现异步非阻塞。
4. NIO有select选择器线程，BIO没有

- 从通道进行数据读取 ：创建一个缓冲区，然后请求通道读取数据
- 从通道进行数据写入 ：创建一个缓冲区，填充数据并要求通道写入数据