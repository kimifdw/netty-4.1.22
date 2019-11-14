/*
 * Copyright 2015 The Netty Project
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
package io.netty.channel.pool;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoop;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ThrowableUtil;

import java.util.Deque;

import static io.netty.util.internal.ObjectUtil.*;

/**
 * Simple {@link ChannelPool} implementation which will create new {@link Channel}s if someone tries to acquire
 * a {@link Channel} but none is in the pool atm. No limit on the maximal concurrent {@link Channel}s is enforced.
 *
 * This implementation uses LIFO order for {@link Channel}s in the {@link ChannelPool}.
 * 简单的ChannelPool实现，它将创建新的通道，如果有人试图获得一个通道，但没有一个在池atm中。执行对最大并发通道的限制。此实现对ChannelPool中的通道使用LIFO顺序。
 *
 */
public class SimpleChannelPool implements ChannelPool {
    private static final AttributeKey<SimpleChannelPool> POOL_KEY = AttributeKey.newInstance("channelPool");
    private static final IllegalStateException FULL_EXCEPTION = ThrowableUtil.unknownStackTrace(
            new IllegalStateException("ChannelPool full"), SimpleChannelPool.class, "releaseAndOffer(...)");

    private final Deque<Channel> deque = PlatformDependent.newConcurrentDeque();
    private final ChannelPoolHandler handler;
    private final ChannelHealthChecker healthCheck;
    private final Bootstrap bootstrap;
    private final boolean releaseHealthCheck;
    private final boolean lastRecentUsed;

    /**
     * Creates a new instance using the {@link ChannelHealthChecker#ACTIVE}.
     *
     * @param bootstrap         the {@link Bootstrap} that is used for connections
     * @param handler           the {@link ChannelPoolHandler} that will be notified for the different pool actions
     */
    public SimpleChannelPool(Bootstrap bootstrap, final ChannelPoolHandler handler) {
        this(bootstrap, handler, ChannelHealthChecker.ACTIVE);
    }

    /**
     * Creates a new instance.
     *
     * @param bootstrap         the {@link Bootstrap} that is used for connections
     * @param handler           the {@link ChannelPoolHandler} that will be notified for the different pool actions
     * @param healthCheck       the {@link ChannelHealthChecker} that will be used to check if a {@link Channel} is
     *                          still healthy when obtain from the {@link ChannelPool}
     */
    public SimpleChannelPool(Bootstrap bootstrap, final ChannelPoolHandler handler, ChannelHealthChecker healthCheck) {
        this(bootstrap, handler, healthCheck, true);
    }

    /**
     * Creates a new instance.
     *
     * @param bootstrap          the {@link Bootstrap} that is used for connections
     * @param handler            the {@link ChannelPoolHandler} that will be notified for the different pool actions
     * @param healthCheck        the {@link ChannelHealthChecker} that will be used to check if a {@link Channel} is
     *                           still healthy when obtain from the {@link ChannelPool}
     * @param releaseHealthCheck will check channel health before offering back if this parameter set to {@code true};
     *                           otherwise, channel health is only checked at acquisition time
     */
    public SimpleChannelPool(Bootstrap bootstrap, final ChannelPoolHandler handler, ChannelHealthChecker healthCheck,
                             boolean releaseHealthCheck) {
        this(bootstrap, handler, healthCheck, releaseHealthCheck, true);
    }

    /**
     * Creates a new instance.
     *
     * @param bootstrap          the {@link Bootstrap} that is used for connections
     * @param handler            the {@link ChannelPoolHandler} that will be notified for the different pool actions
     * @param healthCheck        the {@link ChannelHealthChecker} that will be used to check if a {@link Channel} is
     *                           still healthy when obtain from the {@link ChannelPool}
     * @param releaseHealthCheck will check channel health before offering back if this parameter set to {@code true};
     *                           otherwise, channel health is only checked at acquisition time
     * @param lastRecentUsed    {@code true} {@link Channel} selection will be LIFO, if {@code false} FIFO.
     */
    public SimpleChannelPool(Bootstrap bootstrap, final ChannelPoolHandler handler, ChannelHealthChecker healthCheck,
                             boolean releaseHealthCheck, boolean lastRecentUsed) {
        this.handler = checkNotNull(handler, "handler");
        this.healthCheck = checkNotNull(healthCheck, "healthCheck");
        this.releaseHealthCheck = releaseHealthCheck;
        // Clone the original Bootstrap as we want to set our own handler
        this.bootstrap = checkNotNull(bootstrap, "bootstrap").clone();
        this.bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                assert ch.eventLoop().inEventLoop();
                handler.channelCreated(ch);
            }
        });
        this.lastRecentUsed = lastRecentUsed;
    }

    /**
     * Returns the {@link Bootstrap} this pool will use to open new connections.返回该池用于打开新连接的引导程序。
     *
     * @return the {@link Bootstrap} this pool will use to open new connections
     */
    protected Bootstrap bootstrap() {
        return bootstrap;
    }

    /**
     * Returns the {@link ChannelPoolHandler} that will be notified for the different pool actions.返回ChannelPoolHandler，它将为不同的池操作发出通知。
     *
     * @return the {@link ChannelPoolHandler} that will be notified for the different pool actions
     */
    protected ChannelPoolHandler handler() {
        return handler;
    }

    /**
     * Returns the {@link ChannelHealthChecker} that will be used to check if a {@link Channel} is healthy.返回用于检查通道是否健康的ChannelHealthChecker。
     *
     * @return the {@link ChannelHealthChecker} that will be used to check if a {@link Channel} is healthy
     */
    protected ChannelHealthChecker healthChecker() {
        return healthCheck;
    }

    /**
     * Indicates whether this pool will check the health of channels before offering them back into the pool.指示此池在将通道返回池之前是否检查通道的健康状况。
     *
     * @return {@code true} if this pool will check the health of channels before offering them back into the pool, or
     * {@code false} if channel health is only checked at acquisition time
     */
    protected boolean releaseHealthCheck() {
        return releaseHealthCheck;
    }

    @Override
    public final Future<Channel> acquire() {
        return acquire(bootstrap.config().group().next().<Channel>newPromise());
    }

    @Override
    public Future<Channel> acquire(final Promise<Channel> promise) {
        checkNotNull(promise, "promise");
        return acquireHealthyFromPoolOrNew(promise);
    }

    /**
     * Tries to retrieve healthy channel from the pool if any or creates a new channel otherwise.尝试从池中检索健康通道(如果有的话)或创建新的通道。
     * @param promise the promise to provide acquire result.
     * @return future for acquiring a channel.
     */
    private Future<Channel> acquireHealthyFromPoolOrNew(final Promise<Channel> promise) {
        try {
//            从deque中获取一个channel，这里是用双端队列存储的channel
            final Channel ch = pollChannel();
            if (ch == null) {
                // No Channel left in the pool bootstrap a new Channel池中没有剩余通道引导新通道
                Bootstrap bs = bootstrap.clone();
                bs.attr(POOL_KEY, this);
//                如果channel不存在就创建一个
                ChannelFuture f = connectChannel(bs);
                if (f.isDone()) {
//                    promise发布连接成功事件
                    notifyConnect(f, promise);
                } else {
                    f.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            notifyConnect(future, promise);
                        }
                    });
                }
                return promise;
            }
            EventLoop loop = ch.eventLoop();
            if (loop.inEventLoop()) {
                doHealthCheck(ch, promise);
            } else {
                loop.execute(new Runnable() {
                    @Override
                    public void run() {
                        doHealthCheck(ch, promise);
                    }
                });
            }
        } catch (Throwable cause) {
            promise.tryFailure(cause);
        }
        return promise;
    }

    private void notifyConnect(ChannelFuture future, Promise<Channel> promise) {
        if (future.isSuccess()) {
            Channel channel = future.channel();
            if (!promise.trySuccess(channel)) {
                // Promise was completed in the meantime (like cancelled), just release the channel again承诺完成的同时(如取消)，只是释放渠道再次
//                如果没有连接成功释放channel
                release(channel);
            }
        } else {
            promise.tryFailure(future.cause());
        }
    }

    private void doHealthCheck(final Channel ch, final Promise<Channel> promise) {
        assert ch.eventLoop().inEventLoop();

        Future<Boolean> f = healthCheck.isHealthy(ch);
        if (f.isDone()) {
            notifyHealthCheck(f, ch, promise);
        } else {
            f.addListener(new FutureListener<Boolean>() {
                @Override
                public void operationComplete(Future<Boolean> future) throws Exception {
                    notifyHealthCheck(future, ch, promise);
                }
            });
        }
    }

    private void notifyHealthCheck(Future<Boolean> future, Channel ch, Promise<Channel> promise) {
        assert ch.eventLoop().inEventLoop();

        if (future.isSuccess()) {
//            如果future执行完毕
            if (future.getNow()) {
                try {
                    ch.attr(POOL_KEY).set(this);
//                    执行channelPoolHandler的acquire方法
                    handler.channelAcquired(ch);
                    promise.setSuccess(ch);
                } catch (Throwable cause) {
//                    异常关闭channel发布promise失败事件
                    closeAndFail(ch, cause, promise);
                }
            } else {
//                如果future没有执行完毕关闭channel
                closeChannel(ch);
//                获取一个channel
                acquireHealthyFromPoolOrNew(promise);
            }
        } else {
//            关闭channel
            closeChannel(ch);
//            获取一个channel
            acquireHealthyFromPoolOrNew(promise);
        }
    }

    /**
     * Bootstrap a new {@link Channel}. The default implementation uses {@link Bootstrap#connect()}, sub-classes may
     * override this.
     * <p>
     * The {@link Bootstrap} that is passed in here is cloned via {@link Bootstrap#clone()}, so it is safe to modify.
     * 引导一个新的通道。默认实现使用Bootstrap.connect()，子类可能会覆盖这个。
     这里传递的引导程序是通过Bootstrap.clone()克隆的，因此修改是安全的。
     */
    protected ChannelFuture connectChannel(Bootstrap bs) {
        return bs.connect();
    }

    @Override
    public final Future<Void> release(Channel channel) {
        return release(channel, channel.eventLoop().<Void>newPromise());
    }

    @Override
    public Future<Void> release(final Channel channel, final Promise<Void> promise) {
        checkNotNull(channel, "channel");
        checkNotNull(promise, "promise");
        try {
            EventLoop loop = channel.eventLoop();
            if (loop.inEventLoop()) {
                doReleaseChannel(channel, promise);
            } else {
                loop.execute(new Runnable() {
                    @Override
                    public void run() {
//                        释放channel
                        doReleaseChannel(channel, promise);
                    }
                });
            }
        } catch (Throwable cause) {
//            关闭channel，发布promise失败事件
            closeAndFail(channel, cause, promise);
        }
        return promise;
    }

    private void doReleaseChannel(Channel channel, Promise<Void> promise) {
        assert channel.eventLoop().inEventLoop();
        // Remove the POOL_KEY attribute from the Channel and check if it was acquired from this pool, if not fail.从通道中删除POOL_KEY属性，如果没有失败，则检查它是否从这个池中获得。
        if (channel.attr(POOL_KEY).getAndSet(null) != this) {
            closeAndFail(channel,
                         // Better include a stacktrace here as this is an user error.最好在这里包含一个stacktrace，因为这是一个用户错误。
                         new IllegalArgumentException(
                                 "Channel " + channel + " was not acquired from this ChannelPool"),
                         promise);
        } else {
            try {
//                健康检查并释放
                if (releaseHealthCheck) {
                    doHealthCheckOnRelease(channel, promise);
                } else {
//                    直接释放
                    releaseAndOffer(channel, promise);
                }
            } catch (Throwable cause) {
                closeAndFail(channel, cause, promise);
            }
        }
    }

    private void doHealthCheckOnRelease(final Channel channel, final Promise<Void> promise) throws Exception {
        final Future<Boolean> f = healthCheck.isHealthy(channel);
        if (f.isDone()) {
            releaseAndOfferIfHealthy(channel, promise, f);
        } else {
            f.addListener(new FutureListener<Boolean>() {
                @Override
                public void operationComplete(Future<Boolean> future) throws Exception {
                    releaseAndOfferIfHealthy(channel, promise, f);
                }
            });
        }
    }

    /**
     * Adds the channel back to the pool only if the channel is healthy.仅当通道是健康的时，才将通道添加回池。
     * @param channel the channel to put back to the pool
     * @param promise offer operation promise.
     * @param future the future that contains information fif channel is healthy or not.
     * @throws Exception in case when failed to notify handler about release operation.
     */
    private void releaseAndOfferIfHealthy(Channel channel, Promise<Void> promise, Future<Boolean> future)
            throws Exception {
        if (future.getNow()) { //channel turns out to be healthy, offering and releasing it.频道被证明是健康的，提供和释放它。
            releaseAndOffer(channel, promise);
        } else { //channel not healthy, just releasing it.通道不健康，只是释放它。
            handler.channelReleased(channel);
            promise.setSuccess(null);
        }
    }

    private void releaseAndOffer(Channel channel, Promise<Void> promise) throws Exception {
//        把channel添加到deque中
        if (offerChannel(channel)) {
//            执行channelPoolHandler的释放逻辑
            handler.channelReleased(channel);
            promise.setSuccess(null);
        } else {
//            如果把channel添加到deque中失败就关闭channel并发布promise失败事件
            closeAndFail(channel, FULL_EXCEPTION, promise);
        }
    }

    private static void closeChannel(Channel channel) {
//        删除channel绑定的channelPool
        channel.attr(POOL_KEY).getAndSet(null);
//        关闭channel
        channel.close();
    }

    private static void closeAndFail(Channel channel, Throwable cause, Promise<?> promise) {
        closeChannel(channel);
        promise.tryFailure(cause);
    }

    /**
     * Poll a {@link Channel} out of the internal storage to reuse it. This will return {@code null} if no
     * {@link Channel} is ready to be reused.
     *
     * Sub-classes may override {@link #pollChannel()} and {@link #offerChannel(Channel)}. Be aware that
     * implementations of these methods needs to be thread-safe!
     * 从内部存储中轮询一个通道以重用它。如果没有可以重用的通道，这将返回null。子类可以覆盖pollChannel()和offerChannel(通道)。请注意，这些方法的实现需要是线程安全的!
     */
    protected Channel pollChannel() {
        return lastRecentUsed ? deque.pollLast() : deque.pollFirst();
    }

    /**
     * Offer a {@link Channel} back to the internal storage. This will return {@code true} if the {@link Channel}
     * could be added, {@code false} otherwise.
     *
     * Sub-classes may override {@link #pollChannel()} and {@link #offerChannel(Channel)}. Be aware that
     * implementations of these methods needs to be thread-safe!
     * 为内部存储提供一个通道。如果可以添加通道，则返回true，否则返回false。子类可以覆盖pollChannel()和offerChannel(通道)。请注意，这些方法的实现需要是线程安全的!
     */
    protected boolean offerChannel(Channel channel) {
        return deque.offer(channel);
    }

    @Override
    public void close() {
        for (;;) {
            Channel channel = pollChannel();
            if (channel == null) {
                break;
            }
            channel.close();
        }
    }
}
