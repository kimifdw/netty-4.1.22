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
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.PlatformDependent;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>This implementation of the {@link AbstractTrafficShapingHandler} is for global
 * traffic shaping, that is to say a global limitation of the bandwidth, whatever
 * the number of opened channels.</p>
 * <p>Note the index used in {@code OutboundBuffer.setUserDefinedWritability(index, boolean)} is <b>2</b>.</p>
 *
 * <p>The general use should be as follow:</p>
 * <ul>
 * <li><p>Create your unique GlobalTrafficShapingHandler like:</p>
 * <p><tt>GlobalTrafficShapingHandler myHandler = new GlobalTrafficShapingHandler(executor);</tt></p>
 * <p>The executor could be the underlying IO worker pool</p>
 * <p><tt>pipeline.addLast(myHandler);</tt></p>
 *
 * <p><b>Note that this handler has a Pipeline Coverage of "all" which means only one such handler must be created
 * and shared among all channels as the counter must be shared among all channels.</b></p>
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
 * So the expected usage of those methods are to be used not too often,
 * accordingly to the traffic shaping configuration.</li>
 * </ul>
 *
 * Be sure to call {@link #release()} once this handler is not needed anymore to release all internal resources.
 * This will not shutdown the {@link EventExecutor} as it may be shared, so you need to do this by your own.
 * 这个AbstractTrafficShapingHandler的实现是用于全局流量整形，也就是说，无论开放通道的数量多少，带宽的全局限制。
 注意OutboundBuffer中使用的索引。setUserDefinedWritability(指数(布尔)是2。
 一般用途应如下:
 打造独一无二的全球交通工具:
 GlobalTrafficShapingHandler myHandler =新的GlobalTrafficShapingHandler(执行人);
 执行程序可以是底层IO worker池
 pipeline.addLast(myHandler);
 注意，该处理程序具有“all”的管道覆盖，这意味着必须在所有通道之间创建和共享此类处理程序，因为计数器必须在所有通道之间共享。
 其他参数可以传递，如写或读限制(以字节/秒为单位，0表示没有限制)或检查间隔(以毫秒为单位)，它表示两个带宽计算之间的延迟，因此doAccounting方法的回调(0表示根本没有记帐)。
 0的值表示不考虑checkInterval。如果您需要进行流量整形，但不需要这样的计算，那么建议设置一个正值，即使这个值很高，因为流量整形的精度取决于计算流量的周期。区间越高，对交通的影响就越小。建议在接近5或10分钟的时候使用更高的值。
 maxTimeToWait默认设置为15秒，允许指定时间形成的上界。
 在您的处理程序中，您应该考虑使用channel.isWritable()和channelWritabilityChanged(ctx)来处理可写性，或者在以后。addListener(ctx.write()返回的未来的新GenericFutureListener())。
 您还应该考虑在读或写操作中具有与所需带宽相适应的对象大小:例如，10KB/s的10mb对象将导致突发效果，而1mb /s的100kb对象应该由这个traffic shaphandler平稳地处理。
 一些配置方法将被认为是最好的努力，这意味着所有已经计划的交通将不会被改变，而仅仅应用于新的交通。
 因此，对这些方法的期望使用不会太频繁，因此对流量形成配置也会有影响。
 一旦不再需要这个处理程序来释放所有内部资源时，请确保调用release()。这不会关闭EventExecutor，因为它可能是共享的，所以您需要自己执行。
 */
@Sharable
public class GlobalTrafficShapingHandler extends AbstractTrafficShapingHandler {
    /**
     * All queues per channel
     */
    private final ConcurrentMap<Integer, PerChannel> channelQueues = PlatformDependent.newConcurrentHashMap();

    /**
     * Global queues size
     */
    private final AtomicLong queuesSize = new AtomicLong();

    /**
     * Max size in the list before proposing to stop writing new objects from next handlers
     * for all channel (global) 在建议停止为所有通道(全局)从下一个处理程序编写新对象之前，列表中的最大大小
     */
    long maxGlobalWriteSize = DEFAULT_MAX_SIZE * 100; // default 400MB

    private static final class PerChannel {
        ArrayDeque<ToSend> messagesQueue;
        long queueSize;
        long lastWriteTimestamp;
        long lastReadTimestamp;
    }

    /**
     * Create the global TrafficCounter.
     */
    void createGlobalTrafficCounter(ScheduledExecutorService executor) {
        if (executor == null) {
            throw new NullPointerException("executor");
        }
        TrafficCounter tc = new TrafficCounter(this, executor, "GlobalTC", checkInterval);
        setTrafficCounter(tc);
        tc.start();
    }

    @Override
    protected int userDefinedWritabilityIndex() {
        return AbstractTrafficShapingHandler.GLOBAL_DEFAULT_USER_DEFINED_WRITABILITY_INDEX;
    }

    /**
     * Create a new instance.
     *
     * @param executor
     *            the {@link ScheduledExecutorService} to use for the {@link TrafficCounter}.
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
    public GlobalTrafficShapingHandler(ScheduledExecutorService executor, long writeLimit, long readLimit,
            long checkInterval, long maxTime) {
        super(writeLimit, readLimit, checkInterval, maxTime);
        createGlobalTrafficCounter(executor);
    }

    /**
     * Create a new instance using
     * default max time as delay allowed value of 15000 ms.使用默认的最大时间创建一个新实例，延迟允许值为15000毫秒。
     *
     * @param executor
     *          the {@link ScheduledExecutorService} to use for the {@link TrafficCounter}.
     * @param writeLimit
     *          0 or a limit in bytes/s
     * @param readLimit
     *          0 or a limit in bytes/s
     * @param checkInterval
     *          The delay between two computations of performances for
     *            channels or 0 if no stats are to be computed.
     */
    public GlobalTrafficShapingHandler(ScheduledExecutorService executor, long writeLimit,
            long readLimit, long checkInterval) {
        super(writeLimit, readLimit, checkInterval);
        createGlobalTrafficCounter(executor);
    }

    /**
     * Create a new instance using default Check Interval value of 1000 ms and
     * default max time as delay allowed value of 15000 ms.使用默认检查间隔值1000 ms创建一个新实例，默认最大时间为15000毫秒。
     *
     * @param executor
     *          the {@link ScheduledExecutorService} to use for the {@link TrafficCounter}.
     * @param writeLimit
     *          0 or a limit in bytes/s
     * @param readLimit
     *          0 or a limit in bytes/s
     */
    public GlobalTrafficShapingHandler(ScheduledExecutorService executor, long writeLimit,
            long readLimit) {
        super(writeLimit, readLimit);
        createGlobalTrafficCounter(executor);
    }

    /**
     * Create a new instance using
     * default max time as delay allowed value of 15000 ms and no limit.使用默认的最大时间创建一个新实例，延迟允许值为15000毫秒，没有限制。
     *
     * @param executor
     *          the {@link ScheduledExecutorService} to use for the {@link TrafficCounter}.
     * @param checkInterval
     *          The delay between two computations of performances for
     *            channels or 0 if no stats are to be computed.
     */
    public GlobalTrafficShapingHandler(ScheduledExecutorService executor, long checkInterval) {
        super(checkInterval);
        createGlobalTrafficCounter(executor);
    }

    /**
     * Create a new instance using default Check Interval value of 1000 ms and
     * default max time as delay allowed value of 15000 ms and no limit.使用默认检查间隔值为1000 ms和默认最大时间值(允许延迟值为15000 ms且没有限制)创建一个新实例。
     *
     * @param executor
     *          the {@link ScheduledExecutorService} to use for the {@link TrafficCounter}.
     */
    public GlobalTrafficShapingHandler(EventExecutor executor) {
        createGlobalTrafficCounter(executor);
    }

    /**
     * @return the maxGlobalWriteSize default value being 400 MB.
     */
    public long getMaxGlobalWriteSize() {
        return maxGlobalWriteSize;
    }

    /**
     * Note the change will be taken as best effort, meaning
     * that all already scheduled traffics will not be
     * changed, but only applied to new traffics.<br>
     * So the expected usage of this method is to be used not too often,
     * accordingly to the traffic shaping configuration.
     *
     * @param maxGlobalWriteSize the maximum Global Write Size allowed in the buffer
     *            globally for all channels before write suspended is set,
     *            default value being 400 MB.
     *                           请注意，这一变化将是最好的努力，这意味着所有已经预定的交通将不会改变，但只适用于新的交通。因此，这种方法的预期用途是不太经常地用于流量整形配置。
     */
    public void setMaxGlobalWriteSize(long maxGlobalWriteSize) {
        this.maxGlobalWriteSize = maxGlobalWriteSize;
    }

    /**
     * @return the global size of the buffers for all queues.
     */
    public long queuesSize() {
        return queuesSize.get();
    }

    /**
     * Release all internal resources of this instance.释放此实例的所有内部资源。
     */
    public final void release() {
        trafficCounter.stop();
    }

    private PerChannel getOrSetPerChannel(ChannelHandlerContext ctx) {
        // ensure creation is limited to one thread per channel
        Channel channel = ctx.channel();
        Integer key = channel.hashCode();
        PerChannel perChannel = channelQueues.get(key);
        if (perChannel == null) {
            perChannel = new PerChannel();
            perChannel.messagesQueue = new ArrayDeque<ToSend>();
            perChannel.queueSize = 0L;
            perChannel.lastReadTimestamp = TrafficCounter.milliSecondFromNano();
            perChannel.lastWriteTimestamp = perChannel.lastReadTimestamp;
            channelQueues.put(key, perChannel);
        }
        return perChannel;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        getOrSetPerChannel(ctx);
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        Integer key = channel.hashCode();
        PerChannel perChannel = channelQueues.remove(key);
        if (perChannel != null) {
            // write operations need synchronization
            synchronized (perChannel) {
                if (channel.isActive()) {
                    for (ToSend toSend : perChannel.messagesQueue) {
                        long size = calculateSize(toSend.toSend);
                        trafficCounter.bytesRealWriteFlowControl(size);
                        perChannel.queueSize -= size;
                        queuesSize.addAndGet(-size);
                        ctx.write(toSend.toSend, toSend.promise);
                    }
                } else {
                    queuesSize.addAndGet(-perChannel.queueSize);
                    for (ToSend toSend : perChannel.messagesQueue) {
                        if (toSend.toSend instanceof ByteBuf) {
                            ((ByteBuf) toSend.toSend).release();
                        }
                    }
                }
                perChannel.messagesQueue.clear();
            }
        }
        releaseWriteSuspended(ctx);
        releaseReadSuspended(ctx);
        super.handlerRemoved(ctx);
    }

    @Override
    long checkWaitReadTime(final ChannelHandlerContext ctx, long wait, final long now) {
        Integer key = ctx.channel().hashCode();
        PerChannel perChannel = channelQueues.get(key);
        if (perChannel != null) {
            if (wait > maxTime && now + wait - perChannel.lastReadTimestamp > maxTime) {
                wait = maxTime;
            }
        }
        return wait;
    }

    @Override
    void informReadOperation(final ChannelHandlerContext ctx, final long now) {
        Integer key = ctx.channel().hashCode();
        PerChannel perChannel = channelQueues.get(key);
        if (perChannel != null) {
            perChannel.lastReadTimestamp = now;
        }
    }

    private static final class ToSend {
        final long relativeTimeAction;
        final Object toSend;
        final long size;
        final ChannelPromise promise;

        private ToSend(final long delay, final Object toSend, final long size, final ChannelPromise promise) {
            relativeTimeAction = delay;
            this.toSend = toSend;
            this.size = size;
            this.promise = promise;
        }
    }

    @Override
    void submitWrite(final ChannelHandlerContext ctx, final Object msg,
            final long size, final long writedelay, final long now,
            final ChannelPromise promise) {
        Channel channel = ctx.channel();
        Integer key = channel.hashCode();
        PerChannel perChannel = channelQueues.get(key);
        if (perChannel == null) {
            // in case write occurs before handlerAdded is raised for this handler
            // imply a synchronized only if needed
            perChannel = getOrSetPerChannel(ctx);
        }
        final ToSend newToSend;
        long delay = writedelay;
        boolean globalSizeExceeded = false;
        // write operations need synchronization
        synchronized (perChannel) {
            if (writedelay == 0 && perChannel.messagesQueue.isEmpty()) {
                trafficCounter.bytesRealWriteFlowControl(size);
                ctx.write(msg, promise);
                perChannel.lastWriteTimestamp = now;
                return;
            }
            if (delay > maxTime && now + delay - perChannel.lastWriteTimestamp > maxTime) {
                delay = maxTime;
            }
            newToSend = new ToSend(delay + now, msg, size, promise);
            perChannel.messagesQueue.addLast(newToSend);
            perChannel.queueSize += size;
            queuesSize.addAndGet(size);
            checkWriteSuspend(ctx, delay, perChannel.queueSize);
            if (queuesSize.get() > maxGlobalWriteSize) {
                globalSizeExceeded = true;
            }
        }
        if (globalSizeExceeded) {
            setUserDefinedWritability(ctx, false);
        }
        final long futureNow = newToSend.relativeTimeAction;
        final PerChannel forSchedule = perChannel;
        ctx.executor().schedule(new Runnable() {
            @Override
            public void run() {
                sendAllValid(ctx, forSchedule, futureNow);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void sendAllValid(final ChannelHandlerContext ctx, final PerChannel perChannel, final long now) {
        // write operations need synchronization
        synchronized (perChannel) {
            ToSend newToSend = perChannel.messagesQueue.pollFirst();
            for (; newToSend != null; newToSend = perChannel.messagesQueue.pollFirst()) {
                if (newToSend.relativeTimeAction <= now) {
                    long size = newToSend.size;
                    trafficCounter.bytesRealWriteFlowControl(size);
                    perChannel.queueSize -= size;
                    queuesSize.addAndGet(-size);
                    ctx.write(newToSend.toSend, newToSend.promise);
                    perChannel.lastWriteTimestamp = now;
                } else {
                    perChannel.messagesQueue.addFirst(newToSend);
                    break;
                }
            }
            if (perChannel.messagesQueue.isEmpty()) {
                releaseWriteSuspended(ctx);
            }
        }
        ctx.flush();
    }
}
