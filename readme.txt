netty nio网络通讯

适合场景是tcp
http太重了，httpClient比较适合
文件存储也太重了，hdfs更适合

netty实现好的地方是server端和client端交互代码和相应的业务代码解耦，也就是handler代码是我们要实现的

AIO是异步IO的缩写，虽然NIO在网络操作中，提供了非阻塞的方法，但是NIO的IO行为还是同步的。对于NIO来说，我们的业务线程是在IO操作准备好时，得到通知，接着就由这个线程自行进行IO操作，IO操作本身是同步的。
但是对AIO来说，则更加进了一步，它不是在IO准备好时再通知线程，而是在IO操作已经完成后，再给线程发出通知。因此AIO是不会阻塞的，此时我们的业务逻辑将变成一个回调函数，等待IO操作完成后，由系统自动触发。

OIO中，每个线程只能处理一个channel（同步的，该线程和该channel绑定，）。
NIO中，每个线程可以处理多个channel（异步）。
那么OIO如何处理海量连接请求呢？是对每个请求封装成一个request，然后从线程池中挑一个worker线程专门为此请求服务，如果线程池中的线程用完了，就对请求进行排队。请求中如果有读写数据，是会阻塞线程的

protobuf 比json效率高
跨平台、跨语言
解析速度快，比对应的xml快20-100倍
序列化数据非常简洁、紧凑，与XML相比，其序列化之后的数据量约为1/3到1/10

kyro序列化是java序列化的十几倍，dubbo底层用的是这种


epoll io多路复用，为什么linux环境下epoll比poll效率高
》节省了内核大部分工作
epoll很巧妙，分为三个函数，第一个函数创建一个session类似的东西，第二函数告诉内核维持这个session，并把属于session内的fd传给内核，第三个函数epoll_wait是真正的监控多个文件描述符函数，
只需要告诉内核，我在等待哪个session，而session内的fd，内核早就分析过了，不再在每次epoll调用的时候分析，这就节省了内核大部分工作。这样每次调用epoll，内核不再重新扫描fd数组，
因为我们维持了session

我们先看select和poll的通知方式，也就是level-triggered notification，内核在被DMA中断，捕获到IO设备来数据后，本来只需要查找这个数据属于哪个文件描述符，进而通知线程里等待的函数即可，
但是，select和poll要求内核在通知阶段还要继续再扫描一次刚才所建立的内核fd和io对应的那个数组，因为应用程序可能没有真正去读上次通知有数据后的那些fd，应用程序上次没读，
内核在这次select和poll调用的时候就得继续通知，这个os和应用程序的沟通方式效率是低下的。只是方便编程而已（可以不去读那个网络io，方正下次会继续通知）。
于是epoll设计了另外一种通知方式：edge-triggered notification，在这个模式下，io设备来了数据，就只通知这些io设备对应的fd，上次通知过的fd不再通知，内核不再扫描一大堆fd了。

epoll是专门针对大网络并发连接下的os和应用沟通协作上的一个设计，在linux下编网络服务器，必然要采用这个，只有Netty的epoll+edge-triggered notification最牛，能在linux让应用和OS取得最高效率的沟通


Netty是一个高性能、异步事件驱动的NIO框架，它提供了对TCP、UDP和文件传输的支持，作为一个异步NIO框架，Netty的所有IO操作都是异步非阻塞的，通过Future-Listener机制，用户可以方便的主动获取或者通过通知机制获得IO操作结果。

TCP粘包/拆包:

TCP是个“流”协议，所谓流，就是没有界限的一串数据。TCP底层并不了解上层业务数据的具体含义，它会根据TCP缓存区的实际情况进行包的划分，所以在业务上的一个完整的包，可能被TCP拆分成多个包进行发送，也可能把多个小包封装成一个大的数据包发送，这就是TCP粘包和拆包问题。

Netty模块组件
Netty主要有下面一些组件：

Selector
NioEventLoop
NioEventLoopGroup
ChannelHandler
ChannelHandlerContext
ChannelPipeline

Selector

Netty 基于 Selector 对象实现 I/O 多路复用，通过 Selector 一个线程可以监听多个连接的 Channel 事件。

NioEventLoop

其中维护了一个线程和任务队列，支持异步提交执行任务，线程启动时会调用 NioEventLoop 的 run 方法，执行 I/O 任务和非 I/O 任务。

NioEventLoopGroup

主要管理 eventLoop 的生命周期，可以理解为一个线程池，内部维护了一组线程，每个线程(NioEventLoop)负责处理多个 Channel 上的事件，而一个 Channel 只对应于一个线程。

ChannelHandler

是一个接口，处理 I/O 事件或拦截 I/O 操作，并将其转发到其 ChannelPipeline(业务处理链)中的下一个处理程序。

ChannelHandlerContext

保存 Channel 相关的所有上下文信息，同时关联一个 ChannelHandler 对象。

 

ChannelPipeline 是保存 ChannelHandler 的 List，用于处理或拦截 Channel 的入站事件和出站操作。实现了一种高级形式的拦截过滤器模式，使用户可以完全控制事件的处理方式，以及 Channel 中各个的 ChannelHandler 如何相互交互。

第一层 NioEventLoop、NioEventLoopGroup、NioSocketChannel、NioServerSocketChannel
第二层 职责链 PipeLine
第三层 业务handler