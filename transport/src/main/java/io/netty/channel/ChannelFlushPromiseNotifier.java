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

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * This implementation allows to register {@link ChannelFuture} instances which will get notified once some amount of
 * data was written and so a checkpoint was reached.这个实现允许注册ChannelFuture实例，一旦写入了一些数据，并且到达了一个检查点，这些实例就会得到通知。
 */
public final class ChannelFlushPromiseNotifier {

    private long writeCounter;
    private final Queue<FlushCheckpoint> flushCheckpoints = new ArrayDeque<FlushCheckpoint>();
    private final boolean tryNotify;

    /**
     * Create a new instance
     *
     * @param tryNotify if {@code true} the {@link ChannelPromise}s will get notified with
     *                  {@link ChannelPromise#trySuccess()} and {@link ChannelPromise#tryFailure(Throwable)}.
     *                  Otherwise {@link ChannelPromise#setSuccess()} and {@link ChannelPromise#setFailure(Throwable)}
     *                  is used
     */
    public ChannelFlushPromiseNotifier(boolean tryNotify) {
        this.tryNotify = tryNotify;
    }

    /**
     * Create a new instance which will use {@link ChannelPromise#setSuccess()} and
     * {@link ChannelPromise#setFailure(Throwable)} to notify the {@link ChannelPromise}s.创建一个新实例，它将使用ChannelPromise.setSuccess()和ChannelPromise.setFailure(Throwable)来通知channelpromise。
     */
    public ChannelFlushPromiseNotifier() {
        this(false);
    }

    /**
     * @deprecated use {@link #add(ChannelPromise, long)}
     */
    @Deprecated
    public ChannelFlushPromiseNotifier add(ChannelPromise promise, int pendingDataSize) {
        return add(promise, (long) pendingDataSize);
    }

    /**
     * Add a {@link ChannelPromise} to this {@link ChannelFlushPromiseNotifier} which will be notified after the given
     * {@code pendingDataSize} was reached.添加一个信道承诺给这个信道flush许诺者，它将在到达指定的pendingDataSize之后被通知。
     */
    public ChannelFlushPromiseNotifier add(ChannelPromise promise, long pendingDataSize) {
        if (promise == null) {
            throw new NullPointerException("promise");
        }
        if (pendingDataSize < 0) {
            throw new IllegalArgumentException("pendingDataSize must be >= 0 but was " + pendingDataSize);
        }
        long checkpoint = writeCounter + pendingDataSize;
        if (promise instanceof FlushCheckpoint) {
            FlushCheckpoint cp = (FlushCheckpoint) promise;
            cp.flushCheckpoint(checkpoint);
            flushCheckpoints.add(cp);
        } else {
            flushCheckpoints.add(new DefaultFlushCheckpoint(checkpoint, promise));
        }
        return this;
    }
    /**
     * Increase the current write counter by the given delta 根据给定的增量增加当前写计数器
     */
    public ChannelFlushPromiseNotifier increaseWriteCounter(long delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("delta must be >= 0 but was " + delta);
        }
        writeCounter += delta;
        return this;
    }

    /**
     * Return the current write counter of this {@link ChannelFlushPromiseNotifier} 返回此通道的当前写入计数器
     */
    public long writeCounter() {
        return writeCounter;
    }

    /**
     * Notify all {@link ChannelFuture}s that were registered with {@link #add(ChannelPromise, int)} and
     * their pendingDatasize is smaller after the current writeCounter returned by {@link #writeCounter()}.
     *
     * After a {@link ChannelFuture} was notified it will be removed from this {@link ChannelFlushPromiseNotifier} and
     * so not receive anymore notification.
     * 通知所有注册了add(ChannelPromise, int)的ChannelFutures，它们的pendingDatasize在writeCounter()返回当前的writeCounter之后变小。在一个通道未来被通知后，它将从这个通道被删除，因此不再接收通知。
     */
    public ChannelFlushPromiseNotifier notifyPromises() {
        notifyPromises0(null);
        return this;
    }

    /**
     * @deprecated use {@link #notifyPromises()}
     */
    @Deprecated
    public ChannelFlushPromiseNotifier notifyFlushFutures() {
        return notifyPromises();
    }

    /**
     * Notify all {@link ChannelFuture}s that were registered with {@link #add(ChannelPromise, int)} and
     * their pendingDatasize isis smaller then the current writeCounter returned by {@link #writeCounter()}.
     *
     * After a {@link ChannelFuture} was notified it will be removed from this {@link ChannelFlushPromiseNotifier} and
     * so not receive anymore notification.
     *
     * The rest of the remaining {@link ChannelFuture}s will be failed with the given {@link Throwable}.
     *
     * So after this operation this {@link ChannelFutureListener} is empty.
     * 通知所有注册了add(ChannelPromise, int)的ChannelFutures和它们的pendingdatasiasize的isis比writeCounter()返回的当前writeCounter更小。在一个通道未来被通知后，它将从这个通道被删除，因此不再接收通知。剩余的通道期货将在给定的Throwable中失败。所以在这个操作之后这个通道的futurelistener是空的。
     */
    public ChannelFlushPromiseNotifier notifyPromises(Throwable cause) {
        notifyPromises();
        for (;;) {
            FlushCheckpoint cp = flushCheckpoints.poll();
            if (cp == null) {
                break;
            }
            if (tryNotify) {
                cp.promise().tryFailure(cause);
            } else {
                cp.promise().setFailure(cause);
            }
        }
        return this;
    }

    /**
     * @deprecated use {@link #notifyPromises(Throwable)}
     */
    @Deprecated
    public ChannelFlushPromiseNotifier notifyFlushFutures(Throwable cause) {
        return notifyPromises(cause);
    }

    /**
     * Notify all {@link ChannelFuture}s that were registered with {@link #add(ChannelPromise, int)} and
     * their pendingDatasize is smaller then the current writeCounter returned by {@link #writeCounter()} using
     * the given cause1.
     *
     * After a {@link ChannelFuture} was notified it will be removed from this {@link ChannelFlushPromiseNotifier} and
     * so not receive anymore notification.
     *
     * The rest of the remaining {@link ChannelFuture}s will be failed with the given {@link Throwable}.
     *
     * So after this operation this {@link ChannelFutureListener} is empty.
     *
     * @param cause1    the {@link Throwable} which will be used to fail all of the {@link ChannelFuture}s which
     *                  pendingDataSize is smaller then the current writeCounter returned by {@link #writeCounter()}
     * @param cause2    the {@link Throwable} which will be used to fail the remaining {@link ChannelFuture}s
     *                  通知所有注册了add(ChannelPromise, int)的ChannelFutures，它们的pendingDatasize比writeCounter()使用给定的cause1返回的当前writeCounter要小。在一个通道未来被通知后，它将从这个通道被删除，因此不再接收通知。剩余的通道期货将在给定的Throwable中失败。所以在这个操作之后这个通道的futurelistener是空的。
     */
    public ChannelFlushPromiseNotifier notifyPromises(Throwable cause1, Throwable cause2) {
        notifyPromises0(cause1);
        for (;;) {
            FlushCheckpoint cp = flushCheckpoints.poll();
            if (cp == null) {
                break;
            }
            if (tryNotify) {
                cp.promise().tryFailure(cause2);
            } else {
                cp.promise().setFailure(cause2);
            }
        }
        return this;
    }

    /**
     * @deprecated use {@link #notifyPromises(Throwable, Throwable)}
     */
    @Deprecated
    public ChannelFlushPromiseNotifier notifyFlushFutures(Throwable cause1, Throwable cause2) {
        return notifyPromises(cause1, cause2);
    }

    private void notifyPromises0(Throwable cause) {
        if (flushCheckpoints.isEmpty()) {
            writeCounter = 0;
            return;
        }

        final long writeCounter = this.writeCounter;
        for (;;) {
            FlushCheckpoint cp = flushCheckpoints.peek();
            if (cp == null) {
                // Reset the counter if there's nothing in the notification list.
                this.writeCounter = 0;
                break;
            }

            if (cp.flushCheckpoint() > writeCounter) {
                if (writeCounter > 0 && flushCheckpoints.size() == 1) {
                    this.writeCounter = 0;
                    cp.flushCheckpoint(cp.flushCheckpoint() - writeCounter);
                }
                break;
            }

            flushCheckpoints.remove();
            ChannelPromise promise = cp.promise();
            if (cause == null) {
                if (tryNotify) {
                    promise.trySuccess();
                } else {
                    promise.setSuccess();
                }
            } else {
                if (tryNotify) {
                    promise.tryFailure(cause);
                } else {
                    promise.setFailure(cause);
                }
            }
        }

        // Avoid overflow
        final long newWriteCounter = this.writeCounter;
        if (newWriteCounter >= 0x8000000000L) {
            // Reset the counter only when the counter grew pretty large
            // so that we can reduce the cost of updating all entries in the notification list.
            this.writeCounter = 0;
            for (FlushCheckpoint cp: flushCheckpoints) {
                cp.flushCheckpoint(cp.flushCheckpoint() - newWriteCounter);
            }
        }
    }

    interface FlushCheckpoint {
        long flushCheckpoint();
        void flushCheckpoint(long checkpoint);
        ChannelPromise promise();
    }

    private static class DefaultFlushCheckpoint implements FlushCheckpoint {
        private long checkpoint;
        private final ChannelPromise future;

        DefaultFlushCheckpoint(long checkpoint, ChannelPromise future) {
            this.checkpoint = checkpoint;
            this.future = future;
        }

        @Override
        public long flushCheckpoint() {
            return checkpoint;
        }

        @Override
        public void flushCheckpoint(long checkpoint) {
            this.checkpoint = checkpoint;
        }

        @Override
        public ChannelPromise promise() {
            return future;
        }
    }
}
