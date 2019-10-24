/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeMap;

import java.net.InetSocketAddress;
import java.net.SocketAddress;


/**
 * A nexus to a network socket or a component which is capable of I/O
 * operations such as read, write, connect, and bind.
 * <p>
 * A channel provides a user:
 * <ul>
 * <li>the current state of the channel (e.g. is it open? is it connected?),</li>
 * <li>the {@linkplain ChannelConfig configuration parameters} of the channel (e.g. receive buffer size),</li>
 * <li>the I/O operations that the channel supports (e.g. read, write, connect, and bind), and</li>
 * <li>the {@link ChannelPipeline} which handles all I/O events and requests
 *     associated with the channel.</li>
 * </ul>
 *
 * <h3>All I/O operations are asynchronous.</h3>
 * <p>
 * All I/O operations in Netty are asynchronous.  It means any I/O calls will
 * return immediately with no guarantee that the requested I/O operation has
 * been completed at the end of the call.  Instead, you will be returned with
 * a {@link ChannelFuture} instance which will notify you when the requested I/O
 * operation has succeeded, failed, or canceled.
 *
 * <h3>Channels are hierarchical</h3>
 * <p>
 * A {@link Channel} can have a {@linkplain #parent() parent} depending on
 * how it was created.  For instance, a {@link SocketChannel}, that was accepted
 * by {@link ServerSocketChannel}, will return the {@link ServerSocketChannel}
 * as its parent on {@link #parent()}.
 * <p>
 * The semantics of the hierarchical structure depends on the transport
 * implementation where the {@link Channel} belongs to.  For example, you could
 * write a new {@link Channel} implementation that creates the sub-channels that
 * share one socket connection, as <a href="http://beepcore.org/">BEEP</a> and
 * <a href="http://en.wikipedia.org/wiki/Secure_Shell">SSH</a> do.
 *
 * <h3>Downcast to access transport-specific operations</h3>
 * <p>
 * Some transports exposes additional operations that is specific to the
 * transport.  Down-cast the {@link Channel} to sub-type to invoke such
 * operations.  For example, with the old I/O datagram transport, multicast
 * join / leave operations are provided by {@link DatagramChannel}.
 *
 * <h3>Release resources</h3>
 * <p>
 * It is important to call {@link #close()} or {@link #close(ChannelPromise)} to release all
 * resources once you are done with the {@link Channel}. This ensures all resources are
 * released in a proper way, i.e. filehandles.
 * 连接到网络套接字或可执行I/O操作(如读、写、连接和绑定)的组件。
 通道提供用户:
 通道的当前状态(例如，是否打开?)它是连接吗?)
 通道的配置参数(例如接收缓冲区大小)，
 通道支持的I/O操作(例如读、写、连接和绑定)和
 通道管道，用于处理与通道相关的所有I/O事件和请求。

 所有I/O操作都是异步的。
 Netty中的所有I/O操作都是异步的。这意味着任何I/O调用将立即返回，并不能保证请求的I/O操作在调用结束时已完成。相反，您将返回一个ChannelFuture实例，该实例将在请求的I/O操作成功、失败或取消时通知您。

 通道是分层的
 一个通道可以有一个父通道，这取决于它是如何创建的。例如，被ServerSocketChannel接受的SocketChannel将返回ServerSocketChannel作为其父()上的父通道。
 层次结构的语义取决于通道所属的传输实现。例如，您可以编写一个新的通道实现，创建共享一个套接字连接的子通道，就像BEEP和SSH所做的那样。

 向下转换以访问特定于传输的操作
 有些传输公开特定于传输的附加操作。将通道转换为子类型以调用此类操作。例如，使用旧的I/O数据报传输，多播连接/离开操作由DatagramChannel提供。

 释放资源
 调用close()或close(ChannelPromise)来释放在通道完成后的所有资源非常重要。这确保所有资源都以适当的方式发布，即文件句柄。
 */
public interface Channel extends AttributeMap, ChannelOutboundInvoker, Comparable<Channel> {

    /**
     * Returns the globally unique identifier of this {@link Channel}.返回该通道的全局惟一标识符。
     */
    ChannelId id();

    /**
     * Return the {@link EventLoop} this {@link Channel} was registered to.返回该通道已注册到的EventLoop。
     */
    EventLoop eventLoop();

    /**
     * Returns the parent of this channel.
     *
     * @return the parent channel.
     *         {@code null} if this channel does not have a parent channel.
     */
    Channel parent();

    /**
     * Returns the configuration of this channel.返回该通道的配置。
     */
    ChannelConfig config();

    /**
     * Returns {@code true} if the {@link Channel} is open and may get active later
     * 如果通道是打开的，则返回true，稍后可能会激活
     */
    boolean isOpen();

    /**
     * Returns {@code true} if the {@link Channel} is registered with an {@link EventLoop}.
     * 如果通道已注册到EventLoop，则返回true。
     */
    boolean isRegistered();

    /**
     * Return {@code true} if the {@link Channel} is active and so connected.如果通道是活动的，并且是连接的，返回true。
     */
    boolean isActive();

    /**
     * Return the {@link ChannelMetadata} of the {@link Channel} which describe the nature of the {@link Channel}.返回描述通道性质的通道元数据。
     */
    ChannelMetadata metadata();

    /**
     * Returns the local address where this channel is bound to.  The returned
     * {@link SocketAddress} is supposed to be down-cast into more concrete
     * type such as {@link InetSocketAddress} to retrieve the detailed
     * information.返回该通道绑定到的本地地址。返回的SocketAddress应该向下转换为更具体的类型，例如InetSocketAddress，以检索详细的信息。
     *
     * @return the local address of this channel.
     *         {@code null} if this channel is not bound.
     */
    SocketAddress localAddress();

    /**
     * Returns the remote address where this channel is connected to.  The
     * returned {@link SocketAddress} is supposed to be down-cast into more
     * concrete type such as {@link InetSocketAddress} to retrieve the detailed
     * information.
     *
     * @return the remote address of this channel.
     *         {@code null} if this channel is not connected.
     *         If this channel is not connected but it can receive messages
     *         from arbitrary remote addresses (e.g. {@link DatagramChannel},
     *         use {@link DatagramPacket#recipient()} to determine
     *         the origination of the received message as this method will
     *         return {@code null}.
     *         返回该通道所连接的远程地址。返回的SocketAddress应该被向下转换为更具体的类型，如InetSocketAddress检索详细信息。
     */
    SocketAddress remoteAddress();

    /**
     * Returns the {@link ChannelFuture} which will be notified when this
     * channel is closed.  This method always returns the same future instance.返回当该通道关闭时将被通知的ChannelFuture。此方法总是返回相同的未来实例。
     */
    ChannelFuture closeFuture();

    /**
     * Returns {@code true} if and only if the I/O thread will perform the
     * requested write operation immediately.  Any write requests made when
     * this method returns {@code false} are queued until the I/O thread is
     * ready to process the queued write requests.
     * 如果且仅当I/O线程将立即执行请求的写操作时返回true。当这个方法返回false时，任何写请求都将排队等待，直到I/O线程准备处理排队的写请求。
     */
    boolean isWritable();

    /**
     * Get how many bytes can be written until {@link #isWritable()} returns {@code false}.
     * This quantity will always be non-negative. If {@link #isWritable()} is {@code false} then 0.
     * 获取可以写入的字节数，直到isWritable()返回false。这个量永远是非负的。如果isWritable()为false，则为0。
     */
    long bytesBeforeUnwritable();

    /**
     * Get how many bytes must be drained from underlying buffers until {@link #isWritable()} returns {@code true}.
     * This quantity will always be non-negative. If {@link #isWritable()} is {@code true} then 0.
     * 获取在isWritable()返回true之前必须从底层缓冲区中抽取多少字节。这个量永远是非负的。如果isWritable()为真，则为0。
     */
    long bytesBeforeWritable();

    /**
     * Returns an <em>internal-use-only</em> object that provides unsafe operations.
     * 返回一个只提供不安全操作的内部使用对象。
     */
    Unsafe unsafe();

    /**
     * Return the assigned {@link ChannelPipeline}.
     */
    ChannelPipeline pipeline();

    /**
     * Return the assigned {@link ByteBufAllocator} which will be used to allocate {@link ByteBuf}s.
     * 返回分配的ByteBufAllocator，它将用于分配ByteBufs。
     */
    ByteBufAllocator alloc();

    @Override
    Channel read();

    @Override
    Channel flush();

    /**
     * <em>Unsafe</em> operations that should <em>never</em> be called from user-code. These methods
     * are only provided to implement the actual transport, and must be invoked from an I/O thread except for the
     * following methods:
     * <ul>
     *   <li>{@link #localAddress()}</li>
     *   <li>{@link #remoteAddress()}</li>
     *   <li>{@link #closeForcibly()}</li>
     *   <li>{@link #register(EventLoop, ChannelPromise)}</li>
     *   <li>{@link #deregister(ChannelPromise)}</li>
     *   <li>{@link #voidPromise()}</li>
     * </ul>
     * 不安全的操作，不应该从用户代码中调用。这些方法只用于实现实际的传输，除了以下方法外，必须从I/O线程调用:
     */
    interface Unsafe {

        /**
         * Return the assigned {@link RecvByteBufAllocator.Handle} which will be used to allocate {@link ByteBuf}'s when
         * receiving data.返回指定RecvByteBufAllocator。句柄，用于在接收数据时分配ByteBuf的句柄。
         */
        RecvByteBufAllocator.Handle recvBufAllocHandle();

        /**
         * Return the {@link SocketAddress} to which is bound local or
         * {@code null} if none.返回绑定到本地的SocketAddress，如果没有，则返回null。
         */
        SocketAddress localAddress();

        /**
         * Return the {@link SocketAddress} to which is bound remote or
         * {@code null} if none is bound yet.返回被绑定到远程的SocketAddress，如果还没有被绑定，则返回null。
         */
        SocketAddress remoteAddress();

        /**
         * Register the {@link Channel} of the {@link ChannelPromise} and notify
         * the {@link ChannelFuture} once the registration was complete.
         * 注册ChannelPromise的通道，并在注册完成后通知ChannelFuture。
         */
        void register(EventLoop eventLoop, ChannelPromise promise);

        /**
         * Bind the {@link SocketAddress} to the {@link Channel} of the {@link ChannelPromise} and notify
         * it once its done.将SocketAddress绑定到ChannelPromise的通道，并在完成之后通知它。
         */
        void bind(SocketAddress localAddress, ChannelPromise promise);

        /**
         * Connect the {@link Channel} of the given {@link ChannelFuture} with the given remote {@link SocketAddress}.
         * If a specific local {@link SocketAddress} should be used it need to be given as argument. Otherwise just
         * pass {@code null} to it.
         *
         * The {@link ChannelPromise} will get notified once the connect operation was complete.
         * 将给定通道未来的通道与给定的远程足球地址连接起来。如果应该使用特定的本地SocketAddress，则需要将其作为参数。否则就把null传递给它。连接操作完成后，会通知ChannelPromise。
         */
        void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise);

        /**
         * Disconnect the {@link Channel} of the {@link ChannelFuture} and notify the {@link ChannelPromise} once the
         * operation was complete.断开ChannelFuture的通道，并在操作完成后通知ChannelPromise。
         */
        void disconnect(ChannelPromise promise);

        /**覆盖
         * Close the {@link Channel} of the {@link ChannelPromise} and notify the {@link ChannelPromise} once the
         * operation was complete.
         * 关闭通道承诺的通道，并在操作完成后通知通道承诺。
         */
        void close(ChannelPromise promise);

        /**
         * Closes the {@link Channel} immediately without firing any events.  Probably only useful
         * when registration attempt failed.立即关闭通道，不触发任何事件。可能只有在注册尝试失败时才有用。
         */
        void closeForcibly();


        /**
         * Deregister the {@link Channel} of the {@link ChannelPromise} from {@link EventLoop} and notify the
         * {@link ChannelPromise} once the operation was complete.取消事件循环通道的通道，并在操作完成后通知ChannelPromise。
         */
        void deregister(ChannelPromise promise);

        /**
         * Schedules a read operation that fills the inbound buffer of the first {@link ChannelInboundHandler} in the
         * {@link ChannelPipeline}.  If there's already a pending read operation, this method does nothing.
         * 安排一个读取操作，该操作将填充ChannelInboundHandler在ChannelPipeline中的第一个入站缓冲区。如果已经有一个挂起的读操作，这个方法什么也不做。
         */
        void beginRead();

        /**
         * Schedules a write operation.
         */
        void write(Object msg, ChannelPromise promise);

        /**
         * Flush out all write operations scheduled via {@link #write(Object, ChannelPromise)}.
         * 通过write(对象，ChannelPromise)清除所有预定的写操作。
         */
        void flush();

        /**
         * Return a special ChannelPromise which can be reused and passed to the operations in {@link Unsafe}.
         * It will never be notified of a success or error and so is only a placeholder for operations
         * that take a {@link ChannelPromise} as argument but for which you not want to get notified.
         * 返回一个特殊的通道承诺，它可以被重用并传递给通道中的操作。不安全。它永远不会被通知成功或错误，因此它只是一个占位符，用于将ChannelPromise作为参数，但您不希望得到通知的操作。
         */
        ChannelPromise voidPromise();

        /**
         * Returns the {@link ChannelOutboundBuffer} of the {@link Channel} where the pending write requests are stored.
         * 返回存储挂起写请求的通道的ChannelOutboundBuffer。
         */
        ChannelOutboundBuffer outboundBuffer();
    }
}
