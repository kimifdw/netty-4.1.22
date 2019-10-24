/*
 * Copyright 2011 The Netty Project
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
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * <p>AbstractTrafficShapingHandler allows to limit the global bandwidth
 * (see {@link GlobalTrafficShapingHandler}) or per session
 * bandwidth (see {@link ChannelTrafficShapingHandler}), as traffic shaping.
 * It allows you to implement an almost real time monitoring of the bandwidth using
 * the monitors from {@link TrafficCounter} that will call back every checkInterval
 * the method doAccounting of this handler.</p>
 *
 * <p>If you want for any particular reasons to stop the monitoring (accounting) or to change
 * the read/write limit or the check interval, several methods allow that for you:</p>
 * <ul>
 * <li><tt>configure</tt> allows you to change read or write limits, or the checkInterval</li>
 * <li><tt>getTrafficCounter</tt> allows you to have access to the TrafficCounter and so to stop
 * or start the monitoring, to change the checkInterval directly, or to have access to its values.</li>
 * </ul>
 * AbstractTrafficShapingHandler允许将全球带宽(参见GlobalTrafficShapingHandler)或每个会话带宽(参见ChannelTrafficShapingHandler)限制为流量整形。它允许您使用来自于TrafficCounter的监视器实现几乎实时的带宽监视，该监视器将调用该处理程序的方法doAccounting的每个校验间隔。
 如果您出于任何特殊原因想要停止监视(会计)或更改读/写限制或检查间隔，有几种方法可以实现:
 configure允许您更改读或写限制，或者检查间隔
 getTrafficCounter允许您访问该交通计数器，因此可以停止或启动监视，直接更改检查间隔，或者访问其值。
 */
public abstract class AbstractTrafficShapingHandler extends ChannelDuplexHandler {
    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(AbstractTrafficShapingHandler.class);
    /**
     * Default delay between two checks: 1s 两个检查之间的默认延迟:1
     */
    public static final long DEFAULT_CHECK_INTERVAL = 1000;

   /**
    * Default max delay in case of traffic shaping
    * (during which no communication will occur).
    * Shall be less than TIMEOUT. Here half of "standard" 30s
    * 当发生流量整形时，默认的最大延迟(在此期间不会发生任何通信)。应小于超时。这是标准30年代的一半
    */
    public static final long DEFAULT_MAX_TIME = 15000;

    /**
     * Default max size to not exceed in buffer (write only).
     * 默认的最大大小不超过缓冲区(仅写)。
     */
    static final long DEFAULT_MAX_SIZE = 4 * 1024 * 1024L;

    /**
     * Default minimal time to wait
     * 默认的最小等待时间
     */
    static final long MINIMAL_WAIT = 10;

    /**
     * Traffic Counter
     */
    protected TrafficCounter trafficCounter;

    /**
     * Limit in B/s to apply to write 限制在提单上写
     */
    private volatile long writeLimit;

    /**
     * Limit in B/s to apply to read 限制在提单上申请阅读
     */
    private volatile long readLimit;

    /**
     * Max delay in wait 最大延迟等
     */
    protected volatile long maxTime = DEFAULT_MAX_TIME; // default 15 s

    /**
     * Delay between two performance snapshots 两个性能快照之间的延迟
     */
    protected volatile long checkInterval = DEFAULT_CHECK_INTERVAL; // default 1 s

    static final AttributeKey<Boolean> READ_SUSPENDED = AttributeKey
            .valueOf(AbstractTrafficShapingHandler.class.getName() + ".READ_SUSPENDED");
    static final AttributeKey<Runnable> REOPEN_TASK = AttributeKey.valueOf(AbstractTrafficShapingHandler.class
            .getName() + ".REOPEN_TASK");

    /**
     * Max time to delay before proposing to stop writing new objects from next handlers
     * 在建议停止从下一个处理程序中编写新对象之前，需要最大的延迟时间
     */
    volatile long maxWriteDelay = 4 * DEFAULT_CHECK_INTERVAL; // default 4 s
    /**
     * Max size in the list before proposing to stop writing new objects from next handlers
     * 在建议停止从下一个处理程序中写入新对象之前，列表中的最大大小。
     */
    volatile long maxWriteSize = DEFAULT_MAX_SIZE; // default 4MB

    /**
     * Rank in UserDefinedWritability (1 for Channel, 2 for Global TrafficShapingHandler).
     * Set in final constructor. Must be between 1 and 31
     * 用户定义可写性排名(1为频道，2为全球流量塑造者)。在最后的构造函数。必须在1到31之间?
     */
    final int userDefinedWritabilityIndex;

    /**
     * Default value for Channel UserDefinedWritability index
     * 通道用户定义可写性索引的默认值
     */
    static final int CHANNEL_DEFAULT_USER_DEFINED_WRITABILITY_INDEX = 1;

    /**
     * Default value for Global UserDefinedWritability index
     * 全局用户定义可写性索引的默认值
     */
    static final int GLOBAL_DEFAULT_USER_DEFINED_WRITABILITY_INDEX = 2;

    /**
     * Default value for GlobalChannel UserDefinedWritability index
     * GlobalChannel用户定义可写性索引的默认值
     */
    static final int GLOBALCHANNEL_DEFAULT_USER_DEFINED_WRITABILITY_INDEX = 3;

    /**
     * @param newTrafficCounter
     *            the TrafficCounter to set
     */
    void setTrafficCounter(TrafficCounter newTrafficCounter) {
        trafficCounter = newTrafficCounter;
    }

    /**
     * @return the index to be used by the TrafficShapingHandler to manage the user defined writability.
     *              For Channel TSH it is defined as {@value #CHANNEL_DEFAULT_USER_DEFINED_WRITABILITY_INDEX},
     *              for Global TSH it is defined as {@value #GLOBAL_DEFAULT_USER_DEFINED_WRITABILITY_INDEX},
     *              for GlobalChannel TSH it is defined as
     *              {@value #GLOBALCHANNEL_DEFAULT_USER_DEFINED_WRITABILITY_INDEX}.
     */
    protected int userDefinedWritabilityIndex() {
        return CHANNEL_DEFAULT_USER_DEFINED_WRITABILITY_INDEX;
    }

    /**
     * @param writeLimit
     *          0 or a limit in bytes/s
     * @param readLimit
     *          0 or a limit in bytes/s
     * @param checkInterval
     *            The delay between two computations of performances for
     *            channels or 0 if no stats are to be computed.
     * @param maxTime
     *            The maximum delay to wait in case of traffic excess.
     *            Must be positive.
     */
    protected AbstractTrafficShapingHandler(long writeLimit, long readLimit, long checkInterval, long maxTime) {
        if (maxTime <= 0) {
            throw new IllegalArgumentException("maxTime must be positive");
        }

        userDefinedWritabilityIndex = userDefinedWritabilityIndex();
        this.writeLimit = writeLimit;
        this.readLimit = readLimit;
        this.checkInterval = checkInterval;
        this.maxTime = maxTime;
    }

    /**
     * Constructor using default max time as delay allowed value of {@value #DEFAULT_MAX_TIME} ms.
     * @param writeLimit
     *            0 or a limit in bytes/s
     * @param readLimit
     *            0 or a limit in bytes/s
     * @param checkInterval
     *            The delay between two computations of performances for
     *            channels or 0 if no stats are to be computed.
     *            构造函数使用默认的最大时间作为允许的延迟值15,000毫秒。
     */
    protected AbstractTrafficShapingHandler(long writeLimit, long readLimit, long checkInterval) {
        this(writeLimit, readLimit, checkInterval, DEFAULT_MAX_TIME);
    }

    /**
     * Constructor using default Check Interval value of {@value #DEFAULT_CHECK_INTERVAL} ms and
     * default max time as delay allowed value of {@value #DEFAULT_MAX_TIME} ms.
     *
     * @param writeLimit
     *          0 or a limit in bytes/s
     * @param readLimit
     *          0 or a limit in bytes/s
     *          构造函数使用默认的检查间隔值为1000毫秒，默认的最大时间值为15000毫秒。
     */
    protected AbstractTrafficShapingHandler(long writeLimit, long readLimit) {
        this(writeLimit, readLimit, DEFAULT_CHECK_INTERVAL, DEFAULT_MAX_TIME);
    }

    /**
     * Constructor using NO LIMIT, default Check Interval value of {@value #DEFAULT_CHECK_INTERVAL} ms and
     * default max time as delay allowed value of {@value #DEFAULT_MAX_TIME} ms.
     * 构造函数使用无限制，默认检查间隔值为1000 ms，默认最大时间为延迟允许值为15000 ms。
     */
    protected AbstractTrafficShapingHandler() {
        this(0, 0, DEFAULT_CHECK_INTERVAL, DEFAULT_MAX_TIME);
    }

    /**
     * Constructor using NO LIMIT and
     * default max time as delay allowed value of {@value #DEFAULT_MAX_TIME} ms.
     *
     * @param checkInterval
     *            The delay between two computations of performances for
     *            channels or 0 if no stats are to be computed.
     *            构造函数使用无限制和默认的最大时间作为允许的延迟值15000毫秒。
     */
    protected AbstractTrafficShapingHandler(long checkInterval) {
        this(0, 0, checkInterval, DEFAULT_MAX_TIME);
    }

    /**
     * Change the underlying limitations and check interval.
     * <p>Note the change will be taken as best effort, meaning
     * that all already scheduled traffics will not be
     * changed, but only applied to new traffics.</p>
     * <p>So the expected usage of this method is to be used not too often,
     * accordingly to the traffic shaping configuration.</p>
     *
     * @param newWriteLimit The new write limit (in bytes)
     * @param newReadLimit The new read limit (in bytes)
     * @param newCheckInterval The new check interval (in milliseconds)
     *
    更改底层限制并检查间隔。
    请注意，我们将尽最大的努力改变这一改变，这意味着所有已经计划好的车辆将不会被改变，只会被应用到新的车辆上。
    因此，这种方法的预期用途是不太经常地用于流量整形配置。
     */
    public void configure(long newWriteLimit, long newReadLimit,
            long newCheckInterval) {
        configure(newWriteLimit, newReadLimit);
        configure(newCheckInterval);
    }

    /**
     * Change the underlying limitations.
     * <p>Note the change will be taken as best effort, meaning
     * that all already scheduled traffics will not be
     * changed, but only applied to new traffics.</p>
     * <p>So the expected usage of this method is to be used not too often,
     * accordingly to the traffic shaping configuration.</p>
     *
     * @param newWriteLimit The new write limit (in bytes)
     * @param newReadLimit The new read limit (in bytes)
     *                     改变基本的限制。
    请注意，我们将尽最大的努力改变这一改变，这意味着所有已经计划好的车辆将不会被改变，只会被应用到新的车辆上。
    因此，这种方法的预期用途是不太经常地用于流量整形配置。
     */
    public void configure(long newWriteLimit, long newReadLimit) {
        writeLimit = newWriteLimit;
        readLimit = newReadLimit;
        if (trafficCounter != null) {
            trafficCounter.resetAccounting(TrafficCounter.milliSecondFromNano());
        }
    }

    /**
     * Change the check interval.
     *
     * @param newCheckInterval The new check interval (in milliseconds)
     */
    public void configure(long newCheckInterval) {
        checkInterval = newCheckInterval;
        if (trafficCounter != null) {
            trafficCounter.configure(checkInterval);
        }
    }

    /**
     * @return the writeLimit
     */
    public long getWriteLimit() {
        return writeLimit;
    }

    /**
     * <p>Note the change will be taken as best effort, meaning
     * that all already scheduled traffics will not be
     * changed, but only applied to new traffics.</p>
     * <p>So the expected usage of this method is to be used not too often,
     * accordingly to the traffic shaping configuration.</p>
     *
     * @param writeLimit the writeLimit to set
     *                   请注意，我们将尽最大的努力改变这一改变，这意味着所有已经计划好的车辆将不会被改变，只会被应用到新的车辆上。
    因此，这种方法的预期用途是不太经常地用于流量整形配置。
     */
    public void setWriteLimit(long writeLimit) {
        this.writeLimit = writeLimit;
        if (trafficCounter != null) {
            trafficCounter.resetAccounting(TrafficCounter.milliSecondFromNano());
        }
    }

    /**
     * @return the readLimit
     */
    public long getReadLimit() {
        return readLimit;
    }

    /**
     * <p>Note the change will be taken as best effort, meaning
     * that all already scheduled traffics will not be
     * changed, but only applied to new traffics.</p>
     * <p>So the expected usage of this method is to be used not too often,
     * accordingly to the traffic shaping configuration.</p>
     *
     * @param readLimit the readLimit to set
     *                  请注意，我们将尽最大的努力改变这一改变，这意味着所有已经计划好的车辆将不会被改变，只会被应用到新的车辆上。
    因此，这种方法的预期用途是不太经常地用于流量整形配置。
     */
    public void setReadLimit(long readLimit) {
        this.readLimit = readLimit;
        if (trafficCounter != null) {
            trafficCounter.resetAccounting(TrafficCounter.milliSecondFromNano());
        }
    }

    /**
     * @return the checkInterval
     */
    public long getCheckInterval() {
        return checkInterval;
    }

    /**
     * @param checkInterval the interval in ms between each step check to set, default value being 1000 ms.
     */
    public void setCheckInterval(long checkInterval) {
        this.checkInterval = checkInterval;
        if (trafficCounter != null) {
            trafficCounter.configure(checkInterval);
        }
    }

    /**
     * <p>Note the change will be taken as best effort, meaning
     * that all already scheduled traffics will not be
     * changed, but only applied to new traffics.</p>
     * <p>So the expected usage of this method is to be used not too often,
     * accordingly to the traffic shaping configuration.</p>
     *
     * @param maxTime
     *            Max delay in wait, shall be less than TIME OUT in related protocol.
     *            Must be positive.
     *            请注意，我们将尽最大的努力改变这一改变，这意味着所有已经计划好的车辆将不会被改变，只会被应用到新的车辆上。
    因此，这种方法的预期用途是不太经常地用于流量整形配置。
     */
    public void setMaxTimeWait(long maxTime) {
        if (maxTime <= 0) {
            throw new IllegalArgumentException("maxTime must be positive");
        }
        this.maxTime = maxTime;
    }

    /**
     * @return the max delay in wait to prevent TIME OUT
     */
    public long getMaxTimeWait() {
        return maxTime;
    }

    /**
     * @return the maxWriteDelay
     */
    public long getMaxWriteDelay() {
        return maxWriteDelay;
    }

    /**
     * <p>Note the change will be taken as best effort, meaning
     * that all already scheduled traffics will not be
     * changed, but only applied to new traffics.</p>
     * <p>So the expected usage of this method is to be used not too often,
     * accordingly to the traffic shaping configuration.</p>
     *
     * @param maxWriteDelay the maximum Write Delay in ms in the buffer allowed before write suspension is set.
     *              Must be positive.
     *                      请注意，我们将尽最大的努力改变这一改变，这意味着所有已经计划好的车辆将不会被改变，只会被应用到新的车辆上。
    因此，这种方法的预期用途是不太经常地用于流量整形配置。
     */
    public void setMaxWriteDelay(long maxWriteDelay) {
        if (maxWriteDelay <= 0) {
            throw new IllegalArgumentException("maxWriteDelay must be positive");
        }
        this.maxWriteDelay = maxWriteDelay;
    }

    /**
     * @return the maxWriteSize default being {@value #DEFAULT_MAX_SIZE} bytes.
     */
    public long getMaxWriteSize() {
        return maxWriteSize;
    }

    /**
     * <p>Note that this limit is a best effort on memory limitation to prevent Out Of
     * Memory Exception. To ensure it works, the handler generating the write should
     * use one of the way provided by Netty to handle the capacity:</p>
     * <p>- the {@code Channel.isWritable()} property and the corresponding
     * {@code channelWritabilityChanged()}</p>
     * <p>- the {@code ChannelFuture.addListener(new GenericFutureListener())}</p>
     *
     * @param maxWriteSize the maximum Write Size allowed in the buffer
     *            per channel before write suspended is set,
     *            default being {@value #DEFAULT_MAX_SIZE} bytes.
     *                     注意，这个限制是防止内存异常的内存限制的最佳努力。为确保其工作，生成写的处理程序应使用Netty提供的一种方式来处理容量:
    - Channel.isWritable()属性和相应的channelWritabilityChanged()
    ——ChannelFuture。新GenericFutureListener addListener(())
     */
    public void setMaxWriteSize(long maxWriteSize) {
        this.maxWriteSize = maxWriteSize;
    }

    /**
     * Called each time the accounting is computed from the TrafficCounters.
     * This method could be used for instance to implement almost real time accounting.
     * 每次从交通计数器计算会计时调用。该方法可用于实例实现几乎实时计算。
     *
     * @param counter
     *            the TrafficCounter that computes its performance
     */
    protected void doAccounting(TrafficCounter counter) {
        // NOOP by default
    }

    /**
     * Class to implement setReadable at fix time 类以实现在修复时的setread
     */
    static final class ReopenReadTimerTask implements Runnable {
        final ChannelHandlerContext ctx;
        ReopenReadTimerTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            Channel channel = ctx.channel();
            ChannelConfig config = channel.config();
            if (!config.isAutoRead() && isHandlerActive(ctx)) {
                // If AutoRead is False and Active is True, user make a direct setAutoRead(false)
                // Then Just reset the status
                if (logger.isDebugEnabled()) {
                    logger.debug("Not unsuspend: " + config.isAutoRead() + ':' +
                            isHandlerActive(ctx));
                }
                channel.attr(READ_SUSPENDED).set(false);
            } else {
                // Anything else allows the handler to reset the AutoRead
                if (logger.isDebugEnabled()) {
                    if (config.isAutoRead() && !isHandlerActive(ctx)) {
                        logger.debug("Unsuspend: " + config.isAutoRead() + ':' +
                                isHandlerActive(ctx));
                    } else {
                        logger.debug("Normal unsuspend: " + config.isAutoRead() + ':'
                                + isHandlerActive(ctx));
                    }
                }
                channel.attr(READ_SUSPENDED).set(false);
                config.setAutoRead(true);
                channel.read();
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Unsuspend final status => " + config.isAutoRead() + ':'
                        + isHandlerActive(ctx));
            }
        }
    }

    /**
     * Release the Read suspension 释放读悬挂
     */
    void releaseReadSuspended(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        channel.attr(READ_SUSPENDED).set(false);
        channel.config().setAutoRead(true);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        long size = calculateSize(msg);
        long now = TrafficCounter.milliSecondFromNano();
        if (size > 0) {
            // compute the number of ms to wait before reopening the channel
            long wait = trafficCounter.readTimeToWait(size, readLimit, maxTime, now);
            wait = checkWaitReadTime(ctx, wait, now);
            if (wait >= MINIMAL_WAIT) { // At least 10ms seems a minimal
                // time in order to try to limit the traffic
                // Only AutoRead AND HandlerActive True means Context Active
                Channel channel = ctx.channel();
                ChannelConfig config = channel.config();
                if (logger.isDebugEnabled()) {
                    logger.debug("Read suspend: " + wait + ':' + config.isAutoRead() + ':'
                            + isHandlerActive(ctx));
                }
                if (config.isAutoRead() && isHandlerActive(ctx)) {
                    config.setAutoRead(false);
                    channel.attr(READ_SUSPENDED).set(true);
                    // Create a Runnable to reactive the read if needed. If one was create before it will just be
                    // reused to limit object creation
                    Attribute<Runnable> attr = channel.attr(REOPEN_TASK);
                    Runnable reopenTask = attr.get();
                    if (reopenTask == null) {
                        reopenTask = new ReopenReadTimerTask(ctx);
                        attr.set(reopenTask);
                    }
                    ctx.executor().schedule(reopenTask, wait, TimeUnit.MILLISECONDS);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Suspend final status => " + config.isAutoRead() + ':'
                                + isHandlerActive(ctx) + " will reopened at: " + wait);
                    }
                }
            }
        }
        informReadOperation(ctx, now);
        ctx.fireChannelRead(msg);
    }

    /**
     * Method overridden in GTSH to take into account specific timer for the channel.
     * @param wait the wait delay computed in ms
     * @param now the relative now time in ms
     * @return the wait to use according to the context
     * 方法在GTSH中覆盖，以考虑到通道的特定计时器。
     */
    long checkWaitReadTime(final ChannelHandlerContext ctx, long wait, final long now) {
        // no change by default
        return wait;
    }

    /**
     * Method overridden in GTSH to take into account specific timer for the channel.
     * @param now the relative now time in ms
     *            方法在GTSH中重写，以考虑通道的特定计时器。
     */
    void informReadOperation(final ChannelHandlerContext ctx, final long now) {
        // default noop
    }

    protected static boolean isHandlerActive(ChannelHandlerContext ctx) {
        Boolean suspended = ctx.channel().attr(READ_SUSPENDED).get();
        return suspended == null || Boolean.FALSE.equals(suspended);
    }

    @Override
    public void read(ChannelHandlerContext ctx) {
        if (isHandlerActive(ctx)) {
            // For Global Traffic (and Read when using EventLoop in pipeline) : check if READ_SUSPENDED is False
            ctx.read();
        }
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise)
            throws Exception {
        long size = calculateSize(msg);
        long now = TrafficCounter.milliSecondFromNano();
        if (size > 0) {
            // compute the number of ms to wait before continue with the channel
            long wait = trafficCounter.writeTimeToWait(size, writeLimit, maxTime, now);
            if (wait >= MINIMAL_WAIT) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Write suspend: " + wait + ':' + ctx.channel().config().isAutoRead() + ':'
                            + isHandlerActive(ctx));
                }
                submitWrite(ctx, msg, size, wait, now, promise);
                return;
            }
        }
        // to maintain order of write
        submitWrite(ctx, msg, size, 0, now, promise);
    }

    @Deprecated
    protected void submitWrite(final ChannelHandlerContext ctx, final Object msg,
            final long delay, final ChannelPromise promise) {
        submitWrite(ctx, msg, calculateSize(msg),
                delay, TrafficCounter.milliSecondFromNano(), promise);
    }

    abstract void submitWrite(
            ChannelHandlerContext ctx, Object msg, long size, long delay, long now, ChannelPromise promise);

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        setUserDefinedWritability(ctx, true);
        super.channelRegistered(ctx);
    }

    void setUserDefinedWritability(ChannelHandlerContext ctx, boolean writable) {
        ChannelOutboundBuffer cob = ctx.channel().unsafe().outboundBuffer();
        if (cob != null) {
            cob.setUserDefinedWritability(userDefinedWritabilityIndex, writable);
        }
    }

    /**
     * Check the writability according to delay and size for the channel.
     * Set if necessary setUserDefinedWritability status.
     * @param delay the computed delay
     * @param queueSize the current queueSize
     *                  根据通道的延迟和大小检查可写性。如果需要，设置setUserDefinedWritability状态。
     */
    void checkWriteSuspend(ChannelHandlerContext ctx, long delay, long queueSize) {
        if (queueSize > maxWriteSize || delay > maxWriteDelay) {
            setUserDefinedWritability(ctx, false);
        }
    }
    /**
     * Explicitly release the Write suspended status.显式释放写暂停状态。
     */
    void releaseWriteSuspended(ChannelHandlerContext ctx) {
        setUserDefinedWritability(ctx, true);
    }

    /**
     * @return the current TrafficCounter (if
     *         channel is still connected)
     */
    public TrafficCounter trafficCounter() {
        return trafficCounter;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(290)
            .append("TrafficShaping with Write Limit: ").append(writeLimit)
            .append(" Read Limit: ").append(readLimit)
            .append(" CheckInterval: ").append(checkInterval)
            .append(" maxDelay: ").append(maxWriteDelay)
            .append(" maxSize: ").append(maxWriteSize)
            .append(" and Counter: ");
        if (trafficCounter != null) {
            builder.append(trafficCounter);
        } else {
            builder.append("none");
        }
        return builder.toString();
    }

    /**
     * Calculate the size of the given {@link Object}.
     *
     * This implementation supports {@link ByteBuf} and {@link ByteBufHolder}. Sub-classes may override this.
     * @param msg the msg for which the size should be calculated.
     * @return size the size of the msg or {@code -1} if unknown.
     * 计算给定对象的大小。这个实现支持ByteBuf和ByteBufHolder。子类可以重写。
     */
    protected long calculateSize(Object msg) {
        if (msg instanceof ByteBuf) {
            return ((ByteBuf) msg).readableBytes();
        }
        if (msg instanceof ByteBufHolder) {
            return ((ByteBufHolder) msg).content().readableBytes();
        }
        return -1;
    }
}
