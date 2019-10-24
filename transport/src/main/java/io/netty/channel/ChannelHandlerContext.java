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
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;
import io.netty.util.concurrent.EventExecutor;

import java.nio.channels.Channels;

/**
 * Enables a {@link ChannelHandler} to interact with its {@link ChannelPipeline}
 * and other handlers. Among other things a handler can notify the next {@link ChannelHandler} in the
 * {@link ChannelPipeline} as well as modify the {@link ChannelPipeline} it belongs to dynamically.
 *
 * <h3>Notify</h3>
 *
 * You can notify the closest handler in the same {@link ChannelPipeline} by calling one of the various methods
 * provided here.
 *
 * Please refer to {@link ChannelPipeline} to understand how an event flows.
 *
 * <h3>Modifying a pipeline</h3>
 *
 * You can get the {@link ChannelPipeline} your handler belongs to by calling
 * {@link #pipeline()}.  A non-trivial application could insert, remove, or
 * replace handlers in the pipeline dynamically at runtime.
 *
 * <h3>Retrieving for later use</h3>
 *
 * You can keep the {@link ChannelHandlerContext} for later use, such as
 * triggering an event outside the handler methods, even from a different thread.
 * <pre>
 * public class MyHandler extends {@link ChannelDuplexHandler} {
 *
 *     <b>private {@link ChannelHandlerContext} ctx;</b>
 *
 *     public void beforeAdd({@link ChannelHandlerContext} ctx) {
 *         <b>this.ctx = ctx;</b>
 *     }
 *
 *     public void login(String username, password) {
 *         ctx.write(new LoginMessage(username, password));
 *     }
 *     ...
 * }
 * </pre>
 *
 * <h3>Storing stateful information</h3>
 *
 * {@link #attr(AttributeKey)} allow you to
 * store and access stateful information that is related with a handler and its
 * context.  Please refer to {@link ChannelHandler} to learn various recommended
 * ways to manage stateful information.
 *
 * <h3>A handler can have more than one context</h3>
 *
 * Please note that a {@link ChannelHandler} instance can be added to more than
 * one {@link ChannelPipeline}.  It means a single {@link ChannelHandler}
 * instance can have more than one {@link ChannelHandlerContext} and therefore
 * the single instance can be invoked with different
 * {@link ChannelHandlerContext}s if it is added to one or more
 * {@link ChannelPipeline}s more than once.
 * <p>
 * For example, the following handler will have as many independent {@link AttributeKey}s
 * as how many times it is added to pipelines, regardless if it is added to the
 * same pipeline multiple times or added to different pipelines multiple times:
 * <pre>
 * public class FactorialHandler extends {@link ChannelInboundHandlerAdapter} {
 *
 *   private final {@link AttributeKey}&lt;{@link Integer}&gt; counter = {@link AttributeKey}.valueOf("counter");
 *
 *   // This handler will receive a sequence of increasing integers starting
 *   // from 1.
 *   {@code @Override}
 *   public void channelRead({@link ChannelHandlerContext} ctx, Object msg) {
 *     Integer a = ctx.attr(counter).get();
 *
 *     if (a == null) {
 *       a = 1;
 *     }
 *
 *     attr.set(a * (Integer) msg);
 *   }
 * }
 *
 * // Different context objects are given to "f1", "f2", "f3", and "f4" even if
 * // they refer to the same handler instance.  Because the FactorialHandler
 * // stores its state in a context object (using an {@link AttributeKey}), the factorial is
 * // calculated correctly 4 times once the two pipelines (p1 and p2) are active.
 * FactorialHandler fh = new FactorialHandler();
 *
 * {@link ChannelPipeline} p1 = {@link Channels}.pipeline();
 * p1.addLast("f1", fh);
 * p1.addLast("f2", fh);
 *
 * {@link ChannelPipeline} p2 = {@link Channels}.pipeline();
 * p2.addLast("f3", fh);
 * p2.addLast("f4", fh);
 * </pre>
 *
 * <h3>Additional resources worth reading</h3>
 * <p>
 * Please refer to the {@link ChannelHandler}, and
 * {@link ChannelPipeline} to find out more about inbound and outbound operations,
 * what fundamental differences they have, how they flow in a  pipeline,  and how to handle
 * the operation in your application.
 * 使ChannelHandler能够与它的ChannelPipeline和其他处理程序交互。处理程序还可以通知ChannelPipeline中的下一个ChannelHandler，并动态修改它所属的ChannelPipeline。
 * 通知
 您可以通过调用这里提供的各种方法之一来通知同一管道中最近的处理程序。请参考ChannelPipeline以理解事件如何流动。
 修改管道
 您可以通过调用管道()来获取处理程序所属的通道管道。一个重要的应用程序可以在运行时动态地插入、删除或替换管道中的处理程序。
 检索以备后用
 您可以保留ChannelHandlerContext以供以后使用，例如在处理程序方法之外触发事件，即使是在不同的线程中。
 存储状态信息
 attr(AttributeKey)允许您存储和访问与处理程序及其上下文相关的有状态信息。请参考ChannelHandler来了解管理有状态信息的各种推荐方法。
 一个处理程序可以有多个上下文
 请注意，一个ChannelHandler实例可以添加到多个ChannelPipeline。这意味着单个ChannelHandler实例可以有多个ChannelHandlerContext，因此如果将单个实例多次添加到一个或多个ChannelHandlerContext中，则可以使用不同的ChannelHandlerContext调用该实例。
 例如，以下处理程序将具有与向管道添加多少次相同的独立属性键，而不管它是多次添加到同一管道中，还是多次添加到不同的管道中:
 额外的资源值得一读
 请参考ChannelHandler和ChannelPipeline，以了解关于入站和出站操作的更多信息，它们有哪些基本区别，它们如何在管道中流动，以及如何在应用程序中处理操作。
 */
public interface ChannelHandlerContext extends AttributeMap, ChannelInboundInvoker, ChannelOutboundInvoker {

    /**
     * Return the {@link Channel} which is bound to the {@link ChannelHandlerContext}.返回绑定到ChannelHandlerContext的通道。
     */
    Channel channel();

    /**
     * Returns the {@link EventExecutor} which is used to execute an arbitrary task.返回用于执行任意任务的EventExecutor。
     */
    EventExecutor executor();

    /**
     * The unique name of the {@link ChannelHandlerContext}.The name was used when then {@link ChannelHandler}
     * was added to the {@link ChannelPipeline}. This name can also be used to access the registered
     * {@link ChannelHandler} from the {@link ChannelPipeline}.
     * ChannelHandlerContext的唯一名称。当ChannelHandler被添加到ChannelPipeline时，将使用该名称。这个名称还可以用于从ChannelPipeline访问已注册的ChannelHandler。
     *
     */
    String name();

    /**
     * The {@link ChannelHandler} that is bound this {@link ChannelHandlerContext}.
     * 处理I/O事件或拦截I/O操作，并将其转发到其ChannelPipeline中的下一个处理程序。
     * ChannelHandler本身并没有提供很多方法，但是您通常需要实现它的一个子类型:
     * ChannelInboundHandler来处理入站I/O事件
     ChannelOutboundHandler来处理出站I/O操作
     * 另外，为方便起见，还提供了以下适配器类:
     * ChannelInboundHandlerAdapter处理入站I/O事件，
     ChannelOutboundHandlerAdapter用于处理出站I/O操作和
     ChannelDuplexHandler处理入站和出站事件
     上下文对象
     ChannelHandler提供了一个ChannelHandlerContext对象。ChannelHandler应该通过上下文对象与它所属的ChannelPipeline进行交互。通过使用上下文对象，ChannelHandler可以在上游或下游传递事件，动态修改管道，或者存储特定于处理程序的信息(使用AttributeKeys)。
     因为处理程序实例有一个状态变量，该状态变量专门用于一个连接，所以您必须为每个新通道创建一个新的处理程序实例，以避免出现一个竞争条件，即未经身份验证的客户端可以获得机密信息:
     尽管建议使用成员变量来存储处理程序的状态，但是出于某些原因，您可能不希望创建许多处理程序实例。在这种情况下，您可以使用ChannelHandlerContext提供的AttributeKeys:
     现在，处理程序的状态被附加到ChannelHandlerContext中，您可以在不同的管道中添加相同的处理程序实例:
     @Sharable注释
     在上面使用AttributeKey的示例中，您可能注意到了@Sharable注释。
     如果一个ChannelHandler被@Sharable注释，这意味着您可以只创建一个处理程序的实例，并在没有竞争条件的情况下多次将其添加到一个或多个channelpipeline。
     如果没有指定这个注释，那么每次将它添加到管道中时，都必须创建一个新的处理程序实例，因为它具有非共享状态，比如成员变量。
     这个注释是为文档目的而提供的，就像JCIP注释一样。
     额外的资源值得一读
     请参考ChannelHandler和ChannelPipeline，以了解关于入站和出站操作的更多信息，它们有哪些基本区别，它们如何在管道中流动，以及如何在应用程序中处理操作。
     */
    ChannelHandler handler();

    /**
     * Return {@code true} if the {@link ChannelHandler} which belongs to this context was removed
     * from the {@link ChannelPipeline}. Note that this method is only meant to be called from with in the
     * {@link EventLoop}.如果属于此上下文的ChannelHandler从ChannelPipeline中删除，则返回true。注意，该方法仅用于在EventLoop中调用。
     */
    boolean isRemoved();

    @Override
    ChannelHandlerContext fireChannelRegistered();

    @Override
    ChannelHandlerContext fireChannelUnregistered();

    @Override
    ChannelHandlerContext fireChannelActive();

    @Override
    ChannelHandlerContext fireChannelInactive();

    @Override
    ChannelHandlerContext fireExceptionCaught(Throwable cause);

    @Override
    ChannelHandlerContext fireUserEventTriggered(Object evt);

    @Override
    ChannelHandlerContext fireChannelRead(Object msg);

    @Override
    ChannelHandlerContext fireChannelReadComplete();

    @Override
    ChannelHandlerContext fireChannelWritabilityChanged();

    @Override
    ChannelHandlerContext read();

    @Override
    ChannelHandlerContext flush();

    /**
     * Return the assigned {@link ChannelPipeline} 返回指定ChannelPipeline
     */
    ChannelPipeline pipeline();

    /**
     * Return the assigned {@link ByteBufAllocator} which will be used to allocate {@link ByteBuf}s.
     * 返回分配的ByteBufAllocator，它将用于分配ByteBufs。
     */
    ByteBufAllocator alloc();

    /**
     * @deprecated Use {@link Channel#attr(AttributeKey)}
     */
    @Deprecated
    @Override
    <T> Attribute<T> attr(AttributeKey<T> key);

    /**
     * @deprecated Use {@link Channel#hasAttr(AttributeKey)}
     */
    @Deprecated
    @Override
    <T> boolean hasAttr(AttributeKey<T> key);
}
