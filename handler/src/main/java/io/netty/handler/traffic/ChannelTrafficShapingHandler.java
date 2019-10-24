/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.traffic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;

/**
 * <p>This implementation of the {@link AbstractTrafficShapingHandler} is for channel
 * traffic shaping, that is to say a per channel limitation of the bandwidth.</p>
 * <p>Note the index used in {@code OutboundBuffer.setUserDefinedWritability(index, boolean)} is <b>1</b>.</p>
 *
 * <p>The general use should be as follow:</p>
 * <ul>
 * <li><p>Add in your pipeline a new ChannelTrafficShapingHandler.</p>
 * <p><tt>ChannelTrafficShapingHandler myHandler = new ChannelTrafficShapingHandler();</tt></p>
 * <p><tt>pipeline.addLast(myHandler);</tt></p>
 *
 * <p><b>Note that this handler has a Pipeline Coverage of "one" which means a new handler must be created
 * for each new channel as the counter cannot be shared among all channels.</b>.</p>
 *
 * <p>Other arguments can be passed like write or read limitation (in bytes/s where 0 means no limitation)
 * or the check interval (in millisecond) that represents the delay between two computations of the
 * bandwidth and so the call back of the doAccounting method (0 means no accounting at all).</p>
 *
 * <p>A value of 0 means no accounting for checkInterval. If you need traffic shaping but no such accounting,
 * it is recommended to set a positive value, even if it is high since the precision of the
 * Traffic Shaping depends on the period where the traffic is computed. The highest the interval,
 * the less precise the traffic shaping will be. It is suggested as higher value something close
 * to 5 or 10 minutes.</p>
 *
 * <p>maxTimeToWait, by default set to 15s, allows to specify an upper bound of time shaping.</p>
 * </li>
 * <li>In your handler, you should consider to use the {@code channel.isWritable()} and
 * {@code channelWritabilityChanged(ctx)} to handle writability, or through
 * {@code future.addListener(new GenericFutureListener())} on the future returned by
 * {@code ctx.write()}.</li>
 * <li><p>You shall also consider to have object size in read or write operations relatively adapted to
 * the bandwidth you required: for instance having 10 MB objects for 10KB/s will lead to burst effect,
 * while having 100 KB objects for 1 MB/s should be smoothly handle by this TrafficShaping handler.</p></li>
 * <li><p>Some configuration methods will be taken as best effort, meaning
 * that all already scheduled traffics will not be
 * changed, but only applied to new traffics.</p>
 * <p>So the expected usage of those methods are to be used not too often,
 * accordingly to the traffic shaping configuration.</p></li>
 * </ul>
 * 这个AbstractTrafficShapingHandler的实现是用于通道流量的形成，也就是说每个通道限制带宽。
 注意OutboundBuffer中使用的索引。setUserDefinedWritability(指数(布尔)是1。
 一般用途应如下:
 在您的管道中添加一个新的通道traffic shapinghandler。
 channeltraffic shapandler myHandler = new ChannelTrafficShapingHandler();
 pipeline.addLast(myHandler);
 注意，这个处理程序有一个“one”的管道覆盖，这意味着必须为每个新通道创建一个新的处理程序，因为计数器不能在所有通道之间共享。
 写或阅读等其他参数可以传递限制(在字节/秒0意味着没有限制)或检查的时间间隔(毫秒)表示两个计算之间的延迟的带宽和doAccounting的回调方法(0意味着没有会计)。
 的值为0意味着没有占checkInterval。如果你需要交通形成但没有这样的会计,建议设置一个积极的价值,即使是自精度高的流量整形取决于流量计算的时间。最高的区间,精确的流量整形会越少。建议为更高的价值接近5或10分钟。
 maxTimeToWait默认设置为15秒，允许指定时间形成的上界。
 在您的处理程序中，您应该考虑使用channel.isWritable()和channelWritabilityChanged(ctx)来处理可写性，或者在以后。addListener(ctx.write()返回的未来的新GenericFutureListener())。
 您还应该考虑在读或写操作中具有与所需带宽相适应的对象大小:例如，10KB/s的10mb对象将导致突发效果，而1mb /s的100kb对象应该由这个traffic shaphandler平稳地处理。
 一些配置方法将被认为是最好的努力，这意味着所有已经计划的交通将不会被改变，而仅仅应用于新的交通。
 因此，对这些方法的期望使用不会太频繁，因此对流量形成配置也会有影响。
 */
public class ChannelTrafficShapingHandler extends AbstractTrafficShapingHandler {
    private final ArrayDeque<ToSend> messagesQueue = new ArrayDeque<ToSend>();
    private long queueSize;

    /**
     * Create a new instance.
     *
     * @param writeLimit
     *            0 or a limit in bytes/s
     * @param readLimit
     *            0 or a limit in bytes/s
     * @param checkInterval
     *            The delay between two computations of performances for
     *            channels or 0 if no stats are to be computed.
     * @param maxTime
     *            The maximum delay to wait in case of traffic excess.
     */
    public ChannelTrafficShapingHandler(long writeLimit, long readLimit,
            long checkInterval, long maxTime) {
        super(writeLimit, readLimit, checkInterval, maxTime);
    }

    /**
     * Create a new instance using default
     * max time as delay allowed value of 15000 ms.使用默认的最大时间创建一个新实例，延迟允许值为15000毫秒。
     *
     * @param writeLimit
     *          0 or a limit in bytes/s
     * @param readLimit
     *          0 or a limit in bytes/s
     * @param checkInterval
     *          The delay between two computations of performances for
     *            channels or 0 if no stats are to be computed.
     */
    public ChannelTrafficShapingHandler(long writeLimit,
            long readLimit, long checkInterval) {
        super(writeLimit, readLimit, checkInterval);
    }

    /**
     * Create a new instance using default Check Interval value of 1000 ms and
     * max time as delay allowed value of 15000 ms.使用默认的检查间隔值1000 ms和最大时间作为允许的延迟值15000 ms创建一个新实例。
     *
     * @param writeLimit
     *          0 or a limit in bytes/s
     * @param readLimit
     *          0 or a limit in bytes/s
     */
    public ChannelTrafficShapingHandler(long writeLimit,
            long readLimit) {
        super(writeLimit, readLimit);
    }

    /**
     * Create a new instance using
     * default max time as delay allowed value of 15000 ms and no limit.使用默认的最大时间创建一个新实例，延迟允许值为15000毫秒，没有限制。
     *
     * @param checkInterval
     *          The delay between two computations of performances for
     *            channels or 0 if no stats are to be computed.
     */
    public ChannelTrafficShapingHandler(long checkInterval) {
        super(checkInterval);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        TrafficCounter trafficCounter = new TrafficCounter(this, ctx.executor(), "ChannelTC" +
                ctx.channel().hashCode(), checkInterval);
        setTrafficCounter(trafficCounter);
        trafficCounter.start();
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        trafficCounter.stop();
        // write order control
        synchronized (this) {
            if (ctx.channel().isActive()) {
                for (ToSend toSend : messagesQueue) {
                    long size = calculateSize(toSend.toSend);
                    trafficCounter.bytesRealWriteFlowControl(size);
                    queueSize -= size;
                    ctx.write(toSend.toSend, toSend.promise);
                }
            } else {
                for (ToSend toSend : messagesQueue) {
                    if (toSend.toSend instanceof ByteBuf) {
                        ((ByteBuf) toSend.toSend).release();
                    }
                }
            }
            messagesQueue.clear();
        }
        releaseWriteSuspended(ctx);
        releaseReadSuspended(ctx);
        super.handlerRemoved(ctx);
    }

    private static final class ToSend {
        final long relativeTimeAction;
        final Object toSend;
        final ChannelPromise promise;

        private ToSend(final long delay, final Object toSend, final ChannelPromise promise) {
            relativeTimeAction = delay;
            this.toSend = toSend;
            this.promise = promise;
        }
    }

    @Override
    void submitWrite(final ChannelHandlerContext ctx, final Object msg,
            final long size, final long delay, final long now,
            final ChannelPromise promise) {
        final ToSend newToSend;
        // write order control
        synchronized (this) {
            if (delay == 0 && messagesQueue.isEmpty()) {
                trafficCounter.bytesRealWriteFlowControl(size);
                ctx.write(msg, promise);
                return;
            }
            newToSend = new ToSend(delay + now, msg, promise);
            messagesQueue.addLast(newToSend);
            queueSize += size;
            checkWriteSuspend(ctx, delay, queueSize);
        }
        final long futureNow = newToSend.relativeTimeAction;
        ctx.executor().schedule(new Runnable() {
            @Override
            public void run() {
                sendAllValid(ctx, futureNow);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void sendAllValid(final ChannelHandlerContext ctx, final long now) {
        // write order control
        synchronized (this) {
            ToSend newToSend = messagesQueue.pollFirst();
            for (; newToSend != null; newToSend = messagesQueue.pollFirst()) {
                if (newToSend.relativeTimeAction <= now) {
                    long size = calculateSize(newToSend.toSend);
                    trafficCounter.bytesRealWriteFlowControl(size);
                    queueSize -= size;
                    ctx.write(newToSend.toSend, newToSend.promise);
                } else {
                    messagesQueue.addFirst(newToSend);
                    break;
                }
            }
            if (messagesQueue.isEmpty()) {
                releaseWriteSuspended(ctx);
            }
        }
        ctx.flush();
    }

    /**
    * @return current size in bytes of the write buffer.
    */
   public long queueSize() {
       return queueSize;
   }
}
