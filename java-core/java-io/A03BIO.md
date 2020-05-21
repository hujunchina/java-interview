2020年5月21日 11:23:52

### 目录

1. IO模型构建
2. BIO原理
3. BIO操作
4. BIO源码
5. BIO优缺点
6. BIO改进

### 1. IO模型构建

常规IO是对本地本机内存到硬盘的读写操作。如果是远程异构机器如何实现IO读写操作？

需要一个能够网络传输的读写协议，如Socket套接字。Socket套接字就是在进程上监听一个端口来完成数据的发送和接收。socket建立了进程和网卡等硬件的关系。一般的进程无法监听远程数据的，而数据可以通过网卡适配器达到内存或CPU缓存等区域，这时候socket可以绑定端口来把相应的数据传输到该进程对应的内存区域中。

在网络中，socket是指两个连接端。

#### 1.1 Unix中的Socket

操作系统中也有使用到Socket这个概念用来进行进程间通信，它和通常说的基于TCP/IP的Socket概念十分相似，代表了在操作系统中传输数据的两方，只是它不再基于网络协议，而是操作系统本身的文件系统。

一个socket对应一个文件，比如f5d。

#### 1.2 网络中的Socket

通常所说的Socket API，是指操作系统中（也可能不是操作系统）提供的对于传输层（TCP/UDP）抽象的接口。现行的Socket API大致都是遵循了BSD Socket规范（包括Windows）。这里称规范其实不太准确，规范其实是POSIX，但BSD Unix中对于Socket的实现被广为使用，所以成为了实际的规范。

如果你要使用HTTP来构建服务，那么就不需要关心Socket，如果你想基于TCP/IP来构建服务，那么Socket可能就是你会接触到的API。

![img](C:\code\github\java-interview\img\bio-1.png)

从上图中可以看到，HTTP是基于传输层的TCP协议的，而Socket API也是，所以只是从使用上说，可以认为Socket和HTTP类似（但一个是成文的互联网协议，一个是一直沿用的一种编程概念），是对于传输层协议的另一种*直接*使用，因为按照设计，网络对用户的接口都应该在应用层。

#### 1.3 Socket名称的由来

和很多其他Internet上的事物一样，Socket这个名称来自于大名鼎鼎的ARPANET（Advanced Research Projects Agency），早期ARPANET中的Socket指的是一个源或者目的地址——大致就是今天我们所说的IP地址和端口号。最早的时候一个Socket指的是一个40位的数字（RFC33中说明了此用法，但在RFC36中并没有*明确*地说使用40位数字来标识一个地址），其中前32为指向的地址（socket number，大致相当于IP），后8位为发送数据的源（link，大致相当于端口号）。

随着ARPANET的发展，后来（RFC433，Socket Number List）socket number被明确地定义为一个40位的数字，其中后8位被用来制定某个特定的应用使用（比如1是Telnet）。这8位数有很多名字：link、socket name、AEN（another eight number，看到这个名字我也是醉了）。

后来在Internet的规范制定中，才真正的用起了port number这个词。至于为什么端口号是16位的，我想可能有两个原因，一是对于当时的工程师来说，如果每个端口号来标识一个程序，65535个端口号也差不多够用了。二可能是为了对齐吧。

### 2. BIO原理

BIO 是指传统阻塞的 Socket 通信，该阻塞体现在两个方面：1. 服务端监听accept时，2. IO读写时

BIO 可以分为客户端和服务端。

服务端：ServerSocket初始化，构造InetSocketAddress地址，bind绑定地址，accept监听。

客户端： Socket初始化（可以直接绑定server地址不用connect）。

注意accept会返回一个socket对象，而每个socket绑定一个流，所以一个accept只能接受一个客户端请求并生成一个socket来操作io。其实socket在linux中就是一个文件，是对文件的读写，来一个建立一个对于的文件。

两端初始化后，要getInputStream，getOutputStream来获得输入输出流才能完成读写。

初始化就是建立了一个socket文件，需要建立通道即IO流才能对文件读写。

#### 2.2 以下描述使用的是 UNIX/Linux 系统的 API

首先，我们创建 `ServerSocket` 后，内核会创建一个 socket。这个 socket 既可以拿来监听客户连接，也可以连接远端的服务。由于 `ServerSocket` 是用来监听客户连接的，紧接着它就会对内核创建的这个 socket 调用 `listen` 函数。这样一来，这个 socket 就成了所谓的 listening socket，它开始监听客户的连接。

接下来，我们的客户端创建一个 `Socket`，同样的，内核也创建一个 socket 实例。内核创建的这个 socket 跟 `ServerSocket` 一开始创建的那个没有什么区别。不同的是，接下来 `Socket` 会对它执行 `connect`，发起对服务端的连接。前面我们说过，socket API 其实是 TCP 层的封装，所以 `connect` 后，内核会发送一个 `SYN` 给服务端。

现在，我们切换角色到服务端。**服务端的主机在收到这个 `SYN` 后，会创建一个新的 socket**，这个新创建的 socket 跟客户端继续执行三次握手过程。

三次握手完成后，我们执行的 `serverSocket.accept()` 会返回一个 `Socket` 实例，这个 socket 就是上一步内核自动帮我们创建的。

所以说，在一个客户端连接的情况下，其实有 3 个 socket。

关于内核自动创建的这个 socket，还有一个很有意思的地方。它的端口号跟 `ServerSocket` 是一毛一样的。咦！！不是说，一个端口只能绑定一个 socket 吗？其实这个说法并不够准确。

前面我说的TCP 通过端口号来区分数据属于哪个进程的说法，在 socket 的实现里需要改一改。Socket 并不仅仅使用端口号来区别不同的 socket 实例，而是使用 <peer addr:peer port, local addr:local port>这个四元组。

在上面的例子中，我们的 `ServerSocket` 长这样：`<*:*, *:9877>`。意思是，可以接受任何的客户端，和本地任何 IP。

`accept` 返回的 `Socket` 则是这样： `<127.0.0.1:xxxx, 127.0.0.1:9877>`，其中`xxxx` 是客户端的端口号。

如果数据是发送给一个已连接的 socket，内核会找到一个完全匹配的实例，所以数据准确发送给了对端。

如果是客户端要发起连接，这时候只有 `<*:*, *:9877>` 会匹配成功，所以 `SYN` 也准确发送给了监听套接字。

`Socket/ServerSocket` 的区别我们就讲到这里。如果读者觉得不过瘾，可以参考《TCP/IP 详解》卷1、卷2。




### 3. BIO操作

```java
static class Client implements Runnable{
    Socket socket = null;
    String name;
    public Client(Socket socket, String name) {
        this.socket = socket;
        this.name = name;
    }
    @Override
    public void run() {
        while(true){
            try {
                String msg = String.format("%s [%s]", new Date().toString(), name);
                socket.getOutputStream().write(msg.getBytes());
                Thread.sleep(10000);
            } catch (IOException | InterruptedException e) {
                System.out.println("客户端写失败");
            }
        }
    }
}

static class Server implements Runnable{
    ServerSocket serverSocket = null;
    public Server(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }
    @Override
    public void run() {
        System.out.format("服务端启动成功 %n");
        while(true){
            Socket acceptSocket = null;
            try {
                acceptSocket = serverSocket.accept();
                System.out.format("客户端 %s:%d 连接成功%n", acceptSocket.getLocalAddress().getHostAddress(), acceptSocket.getLocalPort());

                InputStream inputStream = acceptSocket.getInputStream();
                byte[] msg = new byte[1024];
                int len = 0;
                while((len = inputStream.read(msg))!=-1){
                    System.out.println(new Date()+" [server] "+new String(msg));
                }
            } catch (IOException e) {
                System.out.println("服务端错误");
            }
        }
    }
}

public static void main(String[] args) throws IOException {
    //         实例化服务端
    ServerSocket serverSocket = new ServerSocket();
    //        构建地址
    SocketAddress address = new InetSocketAddress("127.0.0.1", 60000);
    //        绑定地址
    serverSocket.bind(address);

    new Thread(new Server(serverSocket)).start();

    Socket socket = new Socket("127.0.0.1", 60000);
    new Thread(new Client(socket, "client01")).start();

    Socket socket2 = new Socket("127.0.0.1", 60000);
    new Thread(new Client(socket2, "client02")).start();
}
```

### 4. BIO源码

多个IO读写操作，处理accept问题

从上文的运行结果中我们可以看到，服务器端在启动后，首先需要等待客户端的连接请求（第一次阻塞），

如果没有客户端连接，服务端将一直阻塞等待，然后当客户端连接后，服务器会等待客户端发送数据（第二次阻塞），

如果客户端没有发送数据，那么服务端将会一直阻塞等待客户端发送数据。服务端从启动到收到客户端数据的这个过程，将会有两次阻塞的过程。

这就是BIO的非常重要的一个特点，BIO会产生两次阻塞，第一次在等待连接时阻塞，第二次在等待数据时阻塞。

```java
static class Client implements Runnable{
    Socket socket = null;
    String name;
    public Client(String name) throws IOException {
        this.socket = new Socket("127.0.0.1", 60000);;
        this.name = name;
    }
    @Override
    public void run() {
        while(true){
            try {
                String msg = String.format("%s [%s]", new Date().toString(), name);
                socket.getOutputStream().write(msg.getBytes());
                Thread.sleep(10000);
            } catch (IOException | InterruptedException e) {
                System.out.println("客户端写失败");
            }
        }
    }
}

static class Server implements Runnable{
    ServerSocket serverSocket = null;
    public Server() throws IOException {
        //         实例化服务端
        serverSocket = new ServerSocket();
        //        构建地址
        SocketAddress address = new InetSocketAddress("127.0.0.1", 60000);
        //        绑定地址
        serverSocket.bind(address);
    }
    @Override
    public void run() {
        System.out.format("服务端启动成功 %n");
        Socket[] acceptSocket = new Socket[10];
        int num = 0;
        while(true){
            try {
                acceptSocket[num] = serverSocket.accept();      // 这个也是阻塞状态
                System.out.format("客户端 %s:%d 连接成功 %d %n", acceptSocket[num].getInetAddress().getHostAddress(), acceptSocket[num].getPort(), num);

                InputStream inputStream = acceptSocket[num].getInputStream();

                if(num<10){
                    num++;
                    System.out.println(num);
                }

                byte[] msg = new byte[1024];
                int len = 0;
                //  这个loop已经把线程io阻塞了！，不停的等待读！！
                //  while((len = inputStream.read(msg))!=-1){
                //   System.out.println(new Date()+" [server] "+new String(msg) + num);
                //    }
                new Thread(new AsyncRead(inputStream)).start();

            } catch (IOException e) {
                System.out.println("服务端错误");
            }
        }
    }
}

//    把读写改为多线程的，这样就可以完成读写了
static class AsyncRead implements Runnable{

    InputStream is = null;
    public AsyncRead(InputStream is){
        this.is = is;
    }
    @Override
    public void run() {
        byte[] msg = new byte[1024];
        int len = 0;
        try{
            while((len = is.read(msg))!=-1){
                System.out.println(new Date()+" [server] "+new String(msg));
            }
        } catch (IOException io){
            System.out.println("Thread read error");
        }
    }
}
```



### 5. BIO优缺点

简单高效，适合传统CS模式，对于高并发形式无法满足，阻塞且只能连接固定多个客户端。

### 6. BIO改进

前面两个案例，一个简单的使用BIO（一个客户端一个服务端通信），还有一个把读写阻塞放在了线程中（一个服务端多个客户端）。

这次使用线程池来管理线程,在jdk1.5之前，nio还没出现的时候，都是使用线程池+队列模拟出伪异步的模式缓解服务器端压力。

正常写服务端和客户端，然后把服务端的socket丢给线程池处理。

```java
final static int PORT = 60000;

public static void Msg(String msg){
    System.out.println(msg);
}
//serversocket服务端
static class SocketServer implements Runnable{
    ServerSocket serverSocket = null;
    HandlerThreadPool threadPool = null;
    Socket socket = null;
    public SocketServer() throws IOException {
        // 服务端启动默认是本地主机，不需要额外指定IP，只需要绑定端口号
        serverSocket = new ServerSocket(PORT);
        threadPool = new HandlerThreadPool(5000, 500);
        Msg("服务端正在监听端口： "+PORT);
    }
    @Override
    public void run() {
        while(true){
            try {
                socket = serverSocket.accept();
                //  需要把socket封装为runnable对象在线程池中执行
                threadPool.execute(new SocketHandler(socket));
                Msg("一个客户端已连接");
            } catch (IOException e) {
                Msg("服务端无法响应请求accept失败");
            }
        }
    }
}

//   线程池，处理accept的客户端
static class HandlerThreadPool{
    ThreadPoolExecutor threadPoolExecutor = null;
    ExecutorService executorService = null;
    public HandlerThreadPool(int maxSize, int queueSize){
        executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),   //核心线程数=CPU线程数
            maxSize,        // 最大线程数 = 核心+非核心的
            120L,   //响应时间长短，120秒后非核心线程如果未运行就结束
            TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(queueSize));  //使用link类型阻塞队列
    }
    //        需要把socket封装为runnable对象在线程池中执行
    public void execute(Runnable task){
        executorService.execute(task);
    }
}

//     需要把socket封装为runnable对象在线程池中执行
static class SocketHandler implements Runnable{
    Socket socket = null;
    BufferedReader in = null;
    PrintWriter out = null;
    public SocketHandler(Socket socket){
        this.socket = socket;
    }
    @Override
    public void run() {
        //            int len = 0;  不用字节流，改用字符流
        //            byte[] msg = new byte[1024];
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());
            String msgs = null;
            while(true){
                // Msg("sockethandler");
                //   服务端是readnline按行读取的，所以客户端写入要println！
                msgs = in.readLine();
                Msg(getDate()+msgs);
                if (msgs==null){
                    break;
                }
                //      out.write("Service response");
            }
        } catch (IOException e) {
            Msg("server handler broken");
        }
    }
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

public static void main(String[] args) throws IOException, InterruptedException {
    new Thread(new SocketServer()).start();
    for (int i = 0; i < 5000; i++) {
        Thread.sleep(100);
        new Thread(new SocketClient("client"+i)).start();
    }
}

public static String getDate(){
    return new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]").format(new Date());
}
```

