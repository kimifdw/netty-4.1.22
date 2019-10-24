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
package io.netty.channel.group;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.Iterator;

/**
 * The result of an asynchronous {@link ChannelGroup} operation.
 * {@link ChannelGroupFuture} is composed of {@link ChannelFuture}s which
 * represent the outcome of the individual I/O operations that affect the
 * {@link Channel}s in the {@link ChannelGroup}.
 *
 * <p>
 * All I/O operations in {@link ChannelGroup} are asynchronous.  It means any
 * I/O calls will return immediately with no guarantee that the requested I/O
 * operations have been completed at the end of the call.  Instead, you will be
 * returned with a {@link ChannelGroupFuture} instance which tells you when the
 * requested I/O operations have succeeded, failed, or cancelled.
 * <p>
 * Various methods are provided to let you check if the I/O operations has been
 * completed, wait for the completion, and retrieve the result of the I/O
 * operation. It also allows you to add more than one
 * {@link ChannelGroupFutureListener} so you can get notified when the I/O
 * operation have been completed.
 *
 * <h3>Prefer {@link #addListener(GenericFutureListener)} to {@link #await()}</h3>
 *
 * It is recommended to prefer {@link #addListener(GenericFutureListener)} to
 * {@link #await()} wherever possible to get notified when I/O operations are
 * done and to do any follow-up tasks.
 * <p>
 * {@link #addListener(GenericFutureListener)} is non-blocking.  It simply
 * adds the specified {@link ChannelGroupFutureListener} to the
 * {@link ChannelGroupFuture}, and I/O thread will notify the listeners when
 * the I/O operations associated with the future is done.
 * {@link ChannelGroupFutureListener} yields the best performance and resource
 * utilization because it does not block at all, but it could be tricky to
 * implement a sequential logic if you are not used to event-driven programming.
 * <p>
 * By contrast, {@link #await()} is a blocking operation.  Once called, the
 * caller thread blocks until all I/O operations are done.  It is easier to
 * implement a sequential logic with {@link #await()}, but the caller thread
 * blocks unnecessarily until all I/O operations are done and there's relatively
 * expensive cost of inter-thread notification.  Moreover, there's a chance of
 * dead lock in a particular circumstance, which is described below.
 *
 * <h3>Do not call {@link #await()} inside {@link ChannelHandler}</h3>
 * <p>
 * The event handler methods in {@link ChannelHandler} is often called by
 * an I/O thread.  If {@link #await()} is called by an event handler
 * method, which is called by the I/O thread, the I/O operation it is waiting
 * for might never be complete because {@link #await()} can block the I/O
 * operation it is waiting for, which is a dead lock.
 * <pre>
 * // BAD - NEVER DO THIS
 * {@code @Override}
 * public void messageReceived({@link ChannelHandlerContext} ctx, ShutdownMessage msg) {
 *     {@link ChannelGroup} allChannels = MyServer.getAllChannels();
 *     {@link ChannelGroupFuture} future = allChannels.close();
 *     future.awaitUninterruptibly();
 *     // Perform post-shutdown operation
 *     // ...
 *
 * }
 *
 * // GOOD
 * {@code @Override}
 * public void messageReceived(ChannelHandlerContext ctx, ShutdownMessage msg) {
 *     {@link ChannelGroup} allChannels = MyServer.getAllChannels();
 *     {@link ChannelGroupFuture} future = allChannels.close();
 *     future.addListener(new {@link ChannelGroupFutureListener}() {
 *         public void operationComplete({@link ChannelGroupFuture} future) {
 *             // Perform post-closure operation
 *             // ...
 *         }
 *     });
 * }
 * </pre>
 * <p>
 * In spite of the disadvantages mentioned above, there are certainly the cases
 * where it is more convenient to call {@link #await()}. In such a case, please
 * make sure you do not call {@link #await()} in an I/O thread.  Otherwise,
 * {@link IllegalStateException} will be raised to prevent a dead lock.
 * 异步通道组操作的结果。ChannelGroupFuture由ChannelFutures组成，它代表影响ChannelGroup中的通道的单个I/O操作的结果。
 ChannelGroup中的所有I/O操作都是异步的。这意味着任何I/O调用将立即返回，但不能保证在调用结束时所请求的I/O操作已经完成。相反，您将返回一个ChannelGroupFuture实例，该实例告诉您被请求的I/O操作何时成功、失败或取消。
 提供了各种方法来检查I/O操作是否已经完成，等待完成，并检索I/O操作的结果。它还允许您添加多个ChannelGroupFutureListener，以便在I/O操作完成时得到通知。
 喜欢addListener(GenericFutureListener)等待()
 建议选择addListener(GenericFutureListener)在I/O操作完成时尽可能等待()得到通知，并执行任何后续任务。
 addListener(GenericFutureListener)阻塞。它只是将指定的ChannelGroupFutureListener添加到ChannelGroupFuture，当与future关联的I/O操作完成时，I/O线程将通知侦听器。ChannelGroupFutureListener能够获得最好的性能和资源利用率，因为它根本不阻塞，但是如果您不习惯事件驱动的编程，那么实现一个序列逻辑可能会很困难。
 相反，等待()是一个阻塞操作。调用后，调用者线程阻塞，直到完成所有I/O操作。使用wait()实现顺序逻辑更容易，但调用者线程会不必要地阻塞，直到完成所有I/O操作，并且线程间通知的成本相对较高。此外，在特定的情况下有死锁的机会，如下所述。
 */
public interface ChannelGroupFuture extends Future<Void>, Iterable<ChannelFuture> {

    /**
     * Returns the {@link ChannelGroup} which is associated with this future.返回与这个未来关联的ChannelGroup。
     */
    ChannelGroup group();

    /**
     * Returns the {@link ChannelFuture} of the individual I/O operation which
     * is associated with the specified {@link Channel}.返回与指定通道关联的单个I/O操作的ChannelFuture。
     *
     * @return the matching {@link ChannelFuture} if found.
     *         {@code null} otherwise.
     */
    ChannelFuture find(Channel channel);

    /**
     * Returns {@code true} if and only if all I/O operations associated with
     * this future were successful without any failure.如果且仅当与此未来相关的所有I/O操作成功且没有任何失败时，返回true。
     */
    @Override
    boolean isSuccess();

    @Override
    ChannelGroupException cause();

    /**
     * Returns {@code true} if and only if the I/O operations associated with
     * this future were partially successful with some failure.如果且仅当与此未来相关的I/O操作在某些失败时部分成功时，返回true。
     */
    boolean isPartialSuccess();

    /**
     * Returns {@code true} if and only if the I/O operations associated with
     * this future have failed partially with some success.如果且仅当与此未来相关的I/O操作部分失败并获得一些成功时，返回true。
     */
    boolean isPartialFailure();

    @Override
    ChannelGroupFuture addListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelGroupFuture addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelGroupFuture removeListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelGroupFuture removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelGroupFuture await() throws InterruptedException;

    @Override
    ChannelGroupFuture awaitUninterruptibly();

    @Override
    ChannelGroupFuture syncUninterruptibly();

    @Override
    ChannelGroupFuture sync() throws InterruptedException;

    /**
     * Returns the {@link Iterator} that enumerates all {@link ChannelFuture}s
     * which are associated with this future.  Please note that the returned
     * {@link Iterator} is is unmodifiable, which means a {@link ChannelFuture}
     * cannot be removed from this future.返回迭代器，该迭代器枚举与此未来相关的所有通道期货。请注意，返回的迭代器是不可修改的，这意味着不能从这个将来删除通道的未来。
     */
    @Override
    Iterator<ChannelFuture> iterator();
}
