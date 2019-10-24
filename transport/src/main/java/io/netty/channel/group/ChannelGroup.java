/*
 * Copyright 2013 The Netty Project
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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;
import io.netty.channel.ServerChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Set;

/**
 * A thread-safe {@link Set} that contains open {@link Channel}s and provides
 * various bulk operations on them.  Using {@link ChannelGroup}, you can
 * categorize {@link Channel}s into a meaningful group (e.g. on a per-service
 * or per-state basis.)  A closed {@link Channel} is automatically removed from
 * the collection, so that you don't need to worry about the life cycle of the
 * added {@link Channel}.  A {@link Channel} can belong to more than one
 * {@link ChannelGroup}.
 *
 * <h3>Broadcast a message to multiple {@link Channel}s</h3>
 * <p>
 * If you need to broadcast a message to more than one {@link Channel}, you can
 * add the {@link Channel}s associated with the recipients and call {@link ChannelGroup#write(Object)}:
 * <pre>
 * <strong>{@link ChannelGroup} recipients =
 *         new {@link DefaultChannelGroup}({@link GlobalEventExecutor}.INSTANCE);</strong>
 * recipients.add(channelA);
 * recipients.add(channelB);
 * ..
 * <strong>recipients.write({@link Unpooled}.copiedBuffer(
 *         "Service will shut down for maintenance in 5 minutes.",
 *         {@link CharsetUtil}.UTF_8));</strong>
 * </pre>
 *
 * <h3>Simplify shutdown process with {@link ChannelGroup}</h3>
 * <p>
 * If both {@link ServerChannel}s and non-{@link ServerChannel}s exist in the
 * same {@link ChannelGroup}, any requested I/O operations on the group are
 * performed for the {@link ServerChannel}s first and then for the others.
 * <p>
 * This rule is very useful when you shut down a server in one shot:
 *
 * <pre>
 * <strong>{@link ChannelGroup} allChannels =
 *         new {@link DefaultChannelGroup}({@link GlobalEventExecutor}.INSTANCE);</strong>
 *
 * public static void main(String[] args) throws Exception {
 *     {@link ServerBootstrap} b = new {@link ServerBootstrap}(..);
 *     ...
 *     b.childHandler(new MyHandler());
 *
 *     // Start the server
 *     b.getPipeline().addLast("handler", new MyHandler());
 *     {@link Channel} serverChannel = b.bind(..).sync();
 *     <strong>allChannels.add(serverChannel);</strong>
 *
 *     ... Wait until the shutdown signal reception ...
 *
 *     // Close the serverChannel and then all accepted connections.
 *     <strong>allChannels.close().awaitUninterruptibly();</strong>
 * }
 *
 * public class MyHandler extends {@link ChannelInboundHandlerAdapter} {
 *     {@code @Override}
 *     public void channelActive({@link ChannelHandlerContext} ctx) {
 *         // closed on shutdown.
 *         <strong>allChannels.add(ctx.channel());</strong>
 *         super.channelActive(ctx);
 *     }
 * }
 * </pre>
 * 一个线程安全的集合，它包含开放的通道，并为它们提供各种批量操作。使用ChannelGroup，您可以将通道划分为一个有意义的组(例如，基于每个服务或基于每个状态)。关闭通道会自动从集合中删除，这样您就不需要担心添加通道的生命周期。一个通道可以属于多个通道组。
 *将消息广播到多个频道
 如果您需要向多个通道广播消息，您可以添加与收件人关联的通道，并调用write(对象):
 使用ChannelGroup简化关闭过程。
 如果在同一个ChannelGroup中存在serverchannel和非serverchannel，那么对该组的任何请求的I/O操作都是先为serverchannel执行的，然后是为其他服务器执行的。
 这条规则在一次关闭服务器时非常有用:

 */
public interface ChannelGroup extends Set<Channel>, Comparable<ChannelGroup> {

    /**
     * Returns the name of this group.  A group name is purely for helping
     * you to distinguish one group from others.返回该组的名称。组名纯粹是为了帮助您区分一个组和另一个组。
     */
    String name();

    /**
     * Returns the {@link Channel} which has the specified {@link ChannelId}.返回具有指定信道的信道。
     *
     * @return the matching {@link Channel} if found. {@code null} otherwise.
     */
    Channel find(ChannelId id);

    /**
     * Writes the specified {@code message} to all {@link Channel}s in this
     * group. If the specified {@code message} is an instance of
     * {@link ByteBuf}, it is automatically
     * {@linkplain ByteBuf#duplicate() duplicated} to avoid a race
     * condition. The same is true for {@link ByteBufHolder}. Please note that this operation is asynchronous as
     * {@link Channel#write(Object)} is.将指定的消息写入此组中的所有通道。如果指定的消息是ByteBuf的实例，则会自动复制它以避免竞争条件。ByteBufHolder也是如此。请注意，该操作与Channel.write(对象)是异步的。
     *
     * @return itself
     */
    ChannelGroupFuture write(Object message);

    /**
     * Writes the specified {@code message} to all {@link Channel}s in this
     * group that are matched by the given {@link ChannelMatcher}. If the specified {@code message} is an instance of
     * {@link ByteBuf}, it is automatically
     * {@linkplain ByteBuf#duplicate() duplicated} to avoid a race
     * condition. The same is true for {@link ByteBufHolder}. Please note that this operation is asynchronous as
     * {@link Channel#write(Object)} is.
     * 将指定的消息写入此组中由给定的ChannelMatcher匹配的所有通道。如果指定的消息是ByteBuf的实例，则会自动复制它以避免竞争条件。ByteBufHolder也是如此。请注意，该操作与Channel.write(对象)是异步的。
     *
     * @return the {@link ChannelGroupFuture} instance that notifies when
     *         the operation is done for all channels
     */
    ChannelGroupFuture write(Object message, ChannelMatcher matcher);

    /**
     * Writes the specified {@code message} to all {@link Channel}s in this
     * group that are matched by the given {@link ChannelMatcher}. If the specified {@code message} is an instance of
     * {@link ByteBuf}, it is automatically
     * {@linkplain ByteBuf#duplicate() duplicated} to avoid a race
     * condition. The same is true for {@link ByteBufHolder}. Please note that this operation is asynchronous as
     * {@link Channel#write(Object)} is.
     *
     * If {@code voidPromise} is {@code true} {@link Channel#voidPromise()} is used for the writes and so the same
     * restrictions to the returned {@link ChannelGroupFuture} apply as to a void promise.
     *
     * @return the {@link ChannelGroupFuture} instance that notifies when
     *         the operation is done for all channels
     *         将指定的消息写入此组中由给定的ChannelMatcher匹配的所有通道。如果指定的消息是ByteBuf的实例，则会自动复制它以避免竞争条件。ByteBufHolder也是如此。请注意，该操作与Channel.write(对象)是异步的。如果voidPromise是true Channel.voidPromise()，则用于写操作，因此对于返回的ChannelGroupFuture的相同限制也适用于无效承诺。
     */
    ChannelGroupFuture write(Object message, ChannelMatcher matcher, boolean voidPromise);

    /**
     * Flush all {@link Channel}s in this
     * group. If the specified {@code messages} are an instance of
     * {@link ByteBuf}, it is automatically
     * {@linkplain ByteBuf#duplicate() duplicated} to avoid a race
     * condition. Please note that this operation is asynchronous as
     * {@link Channel#write(Object)} is.
     *
     * @return the {@link ChannelGroupFuture} instance that notifies when
     *         the operation is done for all channels
     *         刷新该组中的所有通道。如果指定的消息是ByteBuf的实例，则会自动复制它以避免竞争条件。请注意，该操作与Channel.write(对象)是异步的。
     */
    ChannelGroup flush();

    /**
     * Flush all {@link Channel}s in this group that are matched by the given {@link ChannelMatcher}.
     * If the specified {@code messages} are an instance of
     * {@link ByteBuf}, it is automatically
     * {@linkplain ByteBuf#duplicate() duplicated} to avoid a race
     * condition. Please note that this operation is asynchronous as
     * {@link Channel#write(Object)} is.
     *
     * @return the {@link ChannelGroupFuture} instance that notifies when
     *         the operation is done for all channels
     *         刷新该组中由给定的通道匹配器匹配的所有通道。如果指定的消息是ByteBuf的实例，则会自动复制它以避免竞争条件。请注意，该操作与Channel.write(对象)是异步的。
     */
    ChannelGroup flush(ChannelMatcher matcher);

    /**
     * Shortcut for calling {@link #write(Object)} and {@link #flush()}.调用write(对象)和flush()的快捷方式。
     */
    ChannelGroupFuture writeAndFlush(Object message);

    /**
     * @deprecated Use {@link #writeAndFlush(Object)} instead.
     */
    @Deprecated
    ChannelGroupFuture flushAndWrite(Object message);

    /**
     * Shortcut for calling {@link #write(Object)} and {@link #flush()} and only act on
     * {@link Channel}s that are matched by the {@link ChannelMatcher}.调用write(Object)和flush()的快捷方式，并且只对与ChannelMatcher匹配的通道执行操作。
     */
    ChannelGroupFuture writeAndFlush(Object message, ChannelMatcher matcher);

    /**
     * Shortcut for calling {@link #write(Object, ChannelMatcher, boolean)} and {@link #flush()} and only act on
     * {@link Channel}s that are matched by the {@link ChannelMatcher}.调用write(Object, ChannelMatcher, boolean)和flush()的快捷方式，并且只对与ChannelMatcher匹配的通道起作用。
     */
    ChannelGroupFuture writeAndFlush(Object message, ChannelMatcher matcher, boolean voidPromise);

    /**
     * @deprecated Use {@link #writeAndFlush(Object, ChannelMatcher)} instead.
     */
    @Deprecated
    ChannelGroupFuture flushAndWrite(Object message, ChannelMatcher matcher);

    /**
     * Disconnects all {@link Channel}s in this group from their remote peers.将该组中的所有通道与其远程对等点断开连接。
     *
     * @return the {@link ChannelGroupFuture} instance that notifies when
     *         the operation is done for all channels
     */
    ChannelGroupFuture disconnect();

    /**
     * Disconnects all {@link Channel}s in this group from their remote peers,
     * that are matched by the given {@link ChannelMatcher}.将该组中的所有通道与其远程对等点断开连接，这些通道对等点由给定的通道匹配器进行匹配。
     *
     * @return the {@link ChannelGroupFuture} instance that notifies when
     *         the operation is done for all channels
     */
    ChannelGroupFuture disconnect(ChannelMatcher matcher);

    /**
     * Closes all {@link Channel}s in this group.  If the {@link Channel} is
     * connected to a remote peer or bound to a local address, it is
     * automatically disconnected and unbound.关闭此组中的所有通道。如果通道连接到远程对等点或绑定到本地地址，则会自动断开连接并取消绑定。
     *
     * @return the {@link ChannelGroupFuture} instance that notifies when
     *         the operation is done for all channels
     */
    ChannelGroupFuture close();

    /**
     * Closes all {@link Channel}s in this group that are matched by the given {@link ChannelMatcher}.
     * If the {@link Channel} is  connected to a remote peer or bound to a local address, it is
     * automatically disconnected and unbound.关闭此组中由给定的通道匹配器匹配的所有通道。如果通道连接到远程对等点或绑定到本地地址，则会自动断开连接并取消绑定。
     *
     * @return the {@link ChannelGroupFuture} instance that notifies when
     *         the operation is done for all channels
     */
    ChannelGroupFuture close(ChannelMatcher matcher);

    /**
     * @deprecated This method will be removed in the next major feature release.
     *
     * Deregister all {@link Channel}s in this group from their {@link EventLoop}.
     * Please note that this operation is asynchronous as {@link Channel#deregister()} is.
     *
     * @return the {@link ChannelGroupFuture} instance that notifies when
     *         the operation is done for all channels
     *         弃用此方法将在下一个主要特性发布中被删除。从这个组的EventLoop中删除所有通道。请注意，该操作作为Channel.deregister()是异步的。
    返回:
     */
    @Deprecated
    ChannelGroupFuture deregister();

    /**
     * @deprecated This method will be removed in the next major feature release.
     *
     * Deregister all {@link Channel}s in this group from their {@link EventLoop} that are matched by the given
     * {@link ChannelMatcher}. Please note that this operation is asynchronous as {@link Channel#deregister()} is.
     *
     * @return the {@link ChannelGroupFuture} instance that notifies when
     *         the operation is done for all channels
     *         弃用此方法将在下一个主要特性发布中被删除。Deregister在这个组中的所有通道都与给定的ChannelMatcher匹配。请注意，该操作作为Channel.deregister()是异步的。
     */
    @Deprecated
    ChannelGroupFuture deregister(ChannelMatcher matcher);

    /**
     * Returns the {@link ChannelGroupFuture} which will be notified when all {@link Channel}s that are part of this
     * {@link ChannelGroup}, at the time of calling, are closed.返回ChannelGroupFuture，当该通道组的所有通道在调用时关闭时，将通知该通道。
     */
    ChannelGroupFuture newCloseFuture();

    /**
     * Returns the {@link ChannelGroupFuture} which will be notified when all {@link Channel}s that are part of this
     * {@link ChannelGroup}, at the time of calling, are closed.返回ChannelGroupFuture，当该通道组的所有通道在调用时关闭时，将通知该通道。
     */
    ChannelGroupFuture newCloseFuture(ChannelMatcher matcher);
}
