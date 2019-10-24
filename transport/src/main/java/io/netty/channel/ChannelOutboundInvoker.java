/*
 * Copyright 2016 The Netty Project
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

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.FutureListener;

import java.net.ConnectException;
import java.net.SocketAddress;

public interface ChannelOutboundInvoker {

    /**
     * Request to bind to the given {@link SocketAddress} and notify the {@link ChannelFuture} once the operation
     * completes, either because the operation was successful or because of an error.
     * <p>
     * This will result in having the
     * {@link ChannelOutboundHandler#bind(ChannelHandlerContext, SocketAddress, ChannelPromise)} method
     * called of the next {@link ChannelOutboundHandler} contained in the {@link ChannelPipeline} of the
     * {@link Channel}.
     *
     请求绑定到给定的SocketAddress，并在操作完成后通知ChannelFuture，这可能是因为操作成功，也可能是因为错误。
     这将导致拥有ChannelOutboundHandler。bind(ChannelHandlerContext、SocketAddress、ChannelPromise)方法调用了包含在通道通道管道中的下一个ChannelOutboundHandler。
     */
    ChannelFuture bind(SocketAddress localAddress);

    /**
     * Request to connect to the given {@link SocketAddress} and notify the {@link ChannelFuture} once the operation
     * completes, either because the operation was successful or because of an error.
     * <p>
     * If the connection fails because of a connection timeout, the {@link ChannelFuture} will get failed with
     * a {@link ConnectTimeoutException}. If it fails because of connection refused a {@link ConnectException}
     * will be used.
     * <p>
     * This will result in having the
     * {@link ChannelOutboundHandler#connect(ChannelHandlerContext, SocketAddress, SocketAddress, ChannelPromise)}
     * method called of the next {@link ChannelOutboundHandler} contained in the {@link ChannelPipeline} of the
     * {@link Channel}.
     * 请求连接到给定的SocketAddress，并在操作完成后通知ChannelFuture，这可能是因为操作成功，也可能是因为错误。
     如果连接由于连接超时而失败，ChannelFuture将会在ConnectTimeoutException中失败。如果由于连接被拒绝而失败，则将使用ConnectException。
     这将导致拥有ChannelOutboundHandler。connect(ChannelHandlerContext, SocketAddress, SocketAddress, ChannelPromise)方法，该方法调用了通道管道中的下一个ChannelOutboundHandler。
     */
    ChannelFuture connect(SocketAddress remoteAddress);

    /**
     * Request to connect to the given {@link SocketAddress} while bind to the localAddress and notify the
     * {@link ChannelFuture} once the operation completes, either because the operation was successful or because of
     * an error.
     * <p>
     * This will result in having the
     * {@link ChannelOutboundHandler#connect(ChannelHandlerContext, SocketAddress, SocketAddress, ChannelPromise)}
     * method called of the next {@link ChannelOutboundHandler} contained in the {@link ChannelPipeline} of the
     * {@link Channel}.
     * 请求在绑定到localAddress时连接到给定的SocketAddress，并在操作完成后通知ChannelFuture，这可能是因为操作成功，也可能是因为错误。
     这将导致拥有ChannelOutboundHandler。connect(ChannelHandlerContext, SocketAddress, SocketAddress, ChannelPromise)方法，该方法调用了通道管道中的下一个ChannelOutboundHandler。
     */
    ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress);

    /**
     * Request to disconnect from the remote peer and notify the {@link ChannelFuture} once the operation completes,
     * either because the operation was successful or because of an error.
     * <p>
     * This will result in having the
     * {@link ChannelOutboundHandler#disconnect(ChannelHandlerContext, ChannelPromise)}
     * method called of the next {@link ChannelOutboundHandler} contained in the {@link ChannelPipeline} of the
     * {@link Channel}.
     * 请求断开与远程对等点的连接，并在操作完成后通知ChannelFuture，要么是因为操作成功，要么是因为错误。
     这将导致拥有ChannelOutboundHandler。断开(ChannelHandlerContext, ChannelPromise)方法，调用包含在通道通道管道中的下一个ChannelOutboundHandler。
     */
    ChannelFuture disconnect();

    /**
     * Request to close the {@link Channel} and notify the {@link ChannelFuture} once the operation completes,
     * either because the operation was successful or because of
     * an error.
     *
     * After it is closed it is not possible to reuse it again.
     * <p>
     * This will result in having the
     * {@link ChannelOutboundHandler#close(ChannelHandlerContext, ChannelPromise)}
     * method called of the next {@link ChannelOutboundHandler} contained in the {@link ChannelPipeline} of the
     * {@link Channel}.
     * 请求在操作完成后关闭通道并通知ChannelFuture，这可能是因为操作成功，也可能是因为错误。关闭之后，就不可能再重用它了。
     这将导致调用ChannelOutboundHandler。close(ChannelHandlerContext, ChannelPromise)方法调用通道管道中包含的下一个ChannelOutboundHandler。
     */
    ChannelFuture close();

    /**
     * Request to deregister from the previous assigned {@link EventExecutor} and notify the
     * {@link ChannelFuture} once the operation completes, either because the operation was successful or because of
     * an error.
     * <p>
     * This will result in having the
     * {@link ChannelOutboundHandler#deregister(ChannelHandlerContext, ChannelPromise)}
     * method called of the next {@link ChannelOutboundHandler} contained in the {@link ChannelPipeline} of the
     * {@link Channel}.
     * 请求从先前分配的EventExecutor中删除并在操作完成后通知ChannelFuture，这可能是因为操作成功，也可能是因为错误。
     这将导致ChannelOutboundHandler.deregister(ChannelHandlerContext, ChannelPromise)方法调用通道管道中包含的下一个ChannelOutboundHandler。
     *
     */
    ChannelFuture deregister();

    /**
     * Request to bind to the given {@link SocketAddress} and notify the {@link ChannelFuture} once the operation
     * completes, either because the operation was successful or because of an error.
     *
     * The given {@link ChannelPromise} will be notified.
     * <p>
     * This will result in having the
     * {@link ChannelOutboundHandler#bind(ChannelHandlerContext, SocketAddress, ChannelPromise)} method
     * called of the next {@link ChannelOutboundHandler} contained in the {@link ChannelPipeline} of the
     * {@link Channel}.
     * 请求绑定到给定的SocketAddress，并在操作完成后通知ChannelFuture，这可能是因为操作成功，也可能是因为错误。给定的频道将被通知。
     这将导致拥有ChannelOutboundHandler。bind(ChannelHandlerContext、SocketAddress、ChannelPromise)方法调用了包含在通道通道管道中的下一个ChannelOutboundHandler。
     */
    ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise);

    /**
     * Request to connect to the given {@link SocketAddress} and notify the {@link ChannelFuture} once the operation
     * completes, either because the operation was successful or because of an error.
     *
     * The given {@link ChannelFuture} will be notified.
     *
     * <p>
     * If the connection fails because of a connection timeout, the {@link ChannelFuture} will get failed with
     * a {@link ConnectTimeoutException}. If it fails because of connection refused a {@link ConnectException}
     * will be used.
     * <p>
     * This will result in having the
     * {@link ChannelOutboundHandler#connect(ChannelHandlerContext, SocketAddress, SocketAddress, ChannelPromise)}
     * method called of the next {@link ChannelOutboundHandler} contained in the {@link ChannelPipeline} of the
     * {@link Channel}.
     * 等于连接到给定的SocketAddress，并在操作完成后通知ChannelFuture，要么是因为操作成功，要么是因为错误。将通知给定的通道未来。
     如果连接由于连接超时而失败，ChannelFuture将会在ConnectTimeoutException中失败。如果由于连接被拒绝而失败，则将使用ConnectException。
     这将导致拥有ChannelOutboundHandler。connect(ChannelHandlerContext, SocketAddress, SocketAddress, ChannelPromise)方法，
     该方法调用了通道管道中的下一个ChannelOutboundHandler。
     */
    ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise);

    /**
     * Request to connect to the given {@link SocketAddress} while bind to the localAddress and notify the
     * {@link ChannelFuture} once the operation completes, either because the operation was successful or because of
     * an error.
     *
     * The given {@link ChannelPromise} will be notified and also returned.
     * <p>
     * This will result in having the
     * {@link ChannelOutboundHandler#connect(ChannelHandlerContext, SocketAddress, SocketAddress, ChannelPromise)}
     * method called of the next {@link ChannelOutboundHandler} contained in the {@link ChannelPipeline} of the
     * {@link Channel}.
     * 请求在绑定到localAddress时连接到给定的SocketAddress，并在操作完成后通知ChannelFuture，这可能是因为操作成功，也可能是因为错误。给定的频道承诺将被通知并被返回。
     这将导致调用ChannelOutboundHandler。连接(ChannelHandlerContext、SocketAddress、SocketAddress、ChannelPromise)方法，调用通道的ChannelOutboundHandler中包含的下一个ChannelOutboundHandler。
     */
    ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise);

    /**
     * Request to disconnect from the remote peer and notify the {@link ChannelFuture} once the operation completes,
     * either because the operation was successful or because of an error.
     *
     * The given {@link ChannelPromise} will be notified.
     * <p>
     * This will result in having the
     * {@link ChannelOutboundHandler#disconnect(ChannelHandlerContext, ChannelPromise)}
     * method called of the next {@link ChannelOutboundHandler} contained in the {@link ChannelPipeline} of the
     * {@link Channel}.
     * 请求断开与远程对等点的连接，并在操作完成后通知ChannelFuture，要么是因为操作成功，要么是因为错误。给定的频道将被通知。
     这将导致调用ChannelOutboundHandler。断开(ChannelHandlerContext, ChannelPromise)方法调用通道管道中的下一个ChannelOutboundHandler。
     */
    ChannelFuture disconnect(ChannelPromise promise);

    /**
     * Request to close the {@link Channel} and notify the {@link ChannelFuture} once the operation completes,
     * either because the operation was successful or because of
     * an error.
     *
     * After it is closed it is not possible to reuse it again.
     * The given {@link ChannelPromise} will be notified.
     * <p>
     * This will result in having the
     * {@link ChannelOutboundHandler#close(ChannelHandlerContext, ChannelPromise)}
     * method called of the next {@link ChannelOutboundHandler} contained in the {@link ChannelPipeline} of the
     * {@link Channel}.
     * 请求在操作完成后关闭通道并通知ChannelFuture，这可能是因为操作成功，也可能是因为错误。关闭之后，就不可能再重用它了。给定的频道将被通知。
     这将导致调用ChannelOutboundHandler。close(ChannelHandlerContext, ChannelPromise)方法调用通道管道中包含的下一个ChannelOutboundHandler。
     */
    ChannelFuture close(ChannelPromise promise);

    /**
     * Request to deregister from the previous assigned {@link EventExecutor} and notify the
     * {@link ChannelFuture} once the operation completes, either because the operation was successful or because of
     * an error.
     *
     * The given {@link ChannelPromise} will be notified.
     * <p>
     * This will result in having the
     * {@link ChannelOutboundHandler#deregister(ChannelHandlerContext, ChannelPromise)}
     * method called of the next {@link ChannelOutboundHandler} contained in the {@link ChannelPipeline} of the
     * {@link Channel}.
     * 请求从先前指定的EventExecutor中注销，并在操作完成后通知ChannelFuture，要么是因为操作成功，要么是因为错误。给定的频道将被通知。
     这将导致调用包含在通道的ChannelOutboundHandler.deregister(ChannelHandlerContext, ChannelPromise)中的下一个ChannelOutboundHandler。
     */
    ChannelFuture deregister(ChannelPromise promise);

    /**
     * Request to Read data from the {@link Channel} into the first inbound buffer, triggers an
     * {@link ChannelInboundHandler#channelRead(ChannelHandlerContext, Object)} event if data was
     * read, and triggers a
     * {@link ChannelInboundHandler#channelReadComplete(ChannelHandlerContext) channelReadComplete} event so the
     * handler can decide to continue reading.  If there's a pending read operation already, this method does nothing.
     * <p>
     * This will result in having the
     * {@link ChannelOutboundHandler#read(ChannelHandlerContext)}
     * method called of the next {@link ChannelOutboundHandler} contained in the {@link ChannelPipeline} of the
     * {@link Channel}.
     * 请求从通道读取数据到第一个入站缓冲区，触发一个ChannelInboundHandler。channelRead(ChannelHandlerContext, Object)事件如果读取数据，并触发一个channelReadComplete事件，以便处理程序可以决定继续读取。如果已经有一个挂起的读操作，这个方法什么也不做。
     这将导致在通道的ChannelOutboundHandler.read(ChannelHandlerContext)方法中调用下一个ChannelOutboundHandler，该方法包含在通道的ChannelPipeline中。
     */
    ChannelOutboundInvoker read();

    /**
     * Request to write a message via this {@link ChannelHandlerContext} through the {@link ChannelPipeline}.
     * This method will not request to actual flush, so be sure to call {@link #flush()}
     * once you want to request to flush all pending data to the actual transport.
     * 请求通过ChannelHandlerContext通过ChannelPipeline编写消息。此方法不会请求实际的刷新，因此，当您想请求将所有待处理的数据刷新到实际的传输时，请确保调用flush()。
     */
    ChannelFuture write(Object msg);

    /**
     * Request to write a message via this {@link ChannelHandlerContext} through the {@link ChannelPipeline}.
     * This method will not request to actual flush, so be sure to call {@link #flush()}
     * once you want to request to flush all pending data to the actual transport.
     * 请求通过ChannelHandlerContext通过ChannelPipeline编写消息。此方法不会请求实际的刷新，所以在请求将所有挂起的数据刷新到实际传输时，请确保调用flush()。
     */
    ChannelFuture write(Object msg, ChannelPromise promise);

    /**
     * Request to flush all pending messages via this ChannelOutboundInvoker.请求通过这个ChannelOutboundInvoker刷新所有挂起的消息。
     */
    ChannelOutboundInvoker flush();

    /**
     * Shortcut for call {@link #write(Object, ChannelPromise)} and {@link #flush()}.调用写(对象、ChannelPromise)和flush()的快捷方式。
     */
    ChannelFuture writeAndFlush(Object msg, ChannelPromise promise);

    /**
     * Shortcut for call {@link #write(Object)} and {@link #flush()}.调用写入(对象)和刷新()的快捷方式。
     */
    ChannelFuture writeAndFlush(Object msg);

    /**
     * Return a new {@link ChannelPromise}.
     */
    ChannelPromise newPromise();

    /**
     * Return an new {@link ChannelProgressivePromise}
     */
    ChannelProgressivePromise newProgressivePromise();

    /**
     * Create a new {@link ChannelFuture} which is marked as succeeded already. So {@link ChannelFuture#isSuccess()}
     * will return {@code true}. All {@link FutureListener} added to it will be notified directly. Also
     * every call of blocking methods will just return without blocking.
     * 创建一个已经标记为成功的新通道。所以ChannelFuture.isSuccess()将返回true。所有添加到它的FutureListener将被直接通知。同样，每个阻塞方法的调用都会返回而不会阻塞。
     */
    ChannelFuture newSucceededFuture();

    /**
     * Create a new {@link ChannelFuture} which is marked as failed already. So {@link ChannelFuture#isSuccess()}
     * will return {@code false}. All {@link FutureListener} added to it will be notified directly. Also
     * every call of blocking methods will just return without blocking.
     * 创建一个已经标记为失败的新通道future。因此，ChannelFuture.isSuccess()将返回false。所有添加到它的FutureListener将被直接通知。同样，每个阻塞方法的调用都会返回而不会阻塞。
     */
    ChannelFuture newFailedFuture(Throwable cause);

    /**
     * Return a special ChannelPromise which can be reused for different operations.
     * <p>
     * It's only supported to use
     * it for {@link ChannelOutboundInvoker#write(Object, ChannelPromise)}.
     * </p>
     * <p>
     * Be aware that the returned {@link ChannelPromise} will not support most operations and should only be used
     * if you want to save an object allocation for every write operation. You will not be able to detect if the
     * operation  was complete, only if it failed as the implementation will call
     * {@link ChannelPipeline#fireExceptionCaught(Throwable)} in this case.
     * </p>
     * <strong>Be aware this is an expert feature and should be used with care!</strong>
     * 返回一个可以在不同操作中重用的特殊通道承诺。
     它只支持用于写(对象，ChannelPromise)。
     请注意，返回的ChannelPromise将不支持大多数操作，仅当您希望为每个写操作保存对象分配时才应该使用。您将无法检测操作是否已完成，除非操作失败，因为在这种情况下，实现将调用channel流水线. fireexceptioncaught(可抛出)。
     请注意，这是一个专家特性，应该小心使用!
     */
    ChannelPromise voidPromise();
}
