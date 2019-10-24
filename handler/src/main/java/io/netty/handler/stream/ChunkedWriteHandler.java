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
package io.netty.handler.stream;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A {@link ChannelHandler} that adds support for writing a large data stream
 * asynchronously neither spending a lot of memory nor getting
 * {@link OutOfMemoryError}.  Large data streaming such as file
 * transfer requires complicated state management in a {@link ChannelHandler}
 * implementation.  {@link ChunkedWriteHandler} manages such complicated states
 * so that you can send a large data stream without difficulties.
 * <p>
 * To use {@link ChunkedWriteHandler} in your application, you have to insert
 * a new {@link ChunkedWriteHandler} instance:
 * <pre>
 * {@link ChannelPipeline} p = ...;
 * p.addLast("streamer", <b>new {@link ChunkedWriteHandler}()</b>);
 * p.addLast("handler", new MyHandler());
 * </pre>
 * Once inserted, you can write a {@link ChunkedInput} so that the
 * {@link ChunkedWriteHandler} can pick it up and fetch the content of the
 * stream chunk by chunk and write the fetched chunk downstream:
 * <pre>
 * {@link Channel} ch = ...;
 * ch.write(new {@link ChunkedFile}(new File("video.mkv"));
 * </pre>
 *
 * <h3>Sending a stream which generates a chunk intermittently</h3>
 *
 * Some {@link ChunkedInput} generates a chunk on a certain event or timing.
 * Such {@link ChunkedInput} implementation often returns {@code null} on
 * {@link ChunkedInput#readChunk(ChannelHandlerContext)}, resulting in the indefinitely suspended
 * transfer.  To resume the transfer when a new chunk is available, you have to
 * call {@link #resumeTransfer()}.
 * 一个通道处理程序，它添加了对异步编写大型数据流的支持，既不占用大量内存，也不获取OutOfMemoryError。大型数据流(比如文件传输)需要在ChannelHandler实现中进行复杂的状态管理。ChunkedWriteHandler管理如此复杂的状态，因此您可以毫无困难地发送大数据流。
 *发送一个间歇地生成块的流
 一些ChunkedInput生成了一个特定事件或时间块。这样的ChunkedInput实现通常在chunkedinpu . readchunk (ChannelHandlerContext)上返回null，导致无限期的暂停传输。要在新的块可用时恢复传输，您必须调用resumeTransfer()。
 */
public class ChunkedWriteHandler extends ChannelDuplexHandler {

    private static final InternalLogger logger =
        InternalLoggerFactory.getInstance(ChunkedWriteHandler.class);

    private final Queue<PendingWrite> queue = new ArrayDeque<PendingWrite>();//本地队列实现的异步读写
    private volatile ChannelHandlerContext ctx;
    private PendingWrite currentWrite;

    public ChunkedWriteHandler() {
    }

    /**
     * @deprecated use {@link #ChunkedWriteHandler()}
     */
    @Deprecated
    public ChunkedWriteHandler(int maxPendingWrites) {
        if (maxPendingWrites <= 0) {
            throw new IllegalArgumentException(
                    "maxPendingWrites: " + maxPendingWrites + " (expected: > 0)");
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
    }

    /**
     * Continues to fetch the chunks from the input.继续从输入中获取块。
     */
    public void resumeTransfer() {
        final ChannelHandlerContext ctx = this.ctx;
        if (ctx == null) {
            return;
        }
//        当前线程是否在线程组中
        if (ctx.executor().inEventLoop()) {
            try {
                doFlush(ctx);
            } catch (Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Unexpected exception while sending chunks.", e);
                }
            }
        } else {
            // let the transfer resume on the next event loop round
            ctx.executor().execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        doFlush(ctx);
                    } catch (Exception e) {
                        if (logger.isWarnEnabled()) {
                            logger.warn("Unexpected exception while sending chunks.", e);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        queue.add(new PendingWrite(msg, promise));
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        doFlush(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        doFlush(ctx);
        ctx.fireChannelInactive();
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isWritable()) {
            // channel is writable again try to continue flushing
            doFlush(ctx);
        }
        ctx.fireChannelWritabilityChanged();
    }

    private void discard(Throwable cause) {
        for (;;) {
            PendingWrite currentWrite = this.currentWrite;

            if (this.currentWrite == null) {
//                从队列中取出当前要写的内容
                currentWrite = queue.poll();
            } else {
                this.currentWrite = null;
            }

            if (currentWrite == null) {
                break;
            }
//            获取写的信息
            Object message = currentWrite.msg;
//            如果消息类型是一种不确定长度的消息类型，是大数据消息类型
            if (message instanceof ChunkedInput) {
                ChunkedInput<?> in = (ChunkedInput<?>) message;
                try {
//                    流中还有数据或者流中数据还没读取完
                    if (!in.isEndOfInput()) {
                        if (cause == null) {
                            cause = new ClosedChannelException();
                        }
                        currentWrite.fail(cause);
                    } else {
                        currentWrite.success(in.length());
                    }
                    closeInput(in);
                } catch (Exception e) {
                    currentWrite.fail(e);
                    logger.warn(ChunkedInput.class.getSimpleName() + ".isEndOfInput() failed", e);
                    closeInput(in);
                }
            } else {
                if (cause == null) {
                    cause = new ClosedChannelException();
                }
                currentWrite.fail(cause);
            }
        }
    }

    private void doFlush(final ChannelHandlerContext ctx) throws Exception {
        final Channel channel = ctx.channel();
//        渠道不是激活的
        if (!channel.isActive()) {
//            丢弃
            discard(null);
            return;
        }

        boolean requiresFlush = true;
        ByteBufAllocator allocator = ctx.alloc();
//        如果通道不可写，所有写请求阻塞
        while (channel.isWritable()) {
            if (currentWrite == null) {
//                从队列中获取写的内容
                currentWrite = queue.poll();
            }

            if (currentWrite == null) {
                break;
            }
            final PendingWrite currentWrite = this.currentWrite;
            final Object pendingMessage = currentWrite.msg;

//            如果是大文件流
            if (pendingMessage instanceof ChunkedInput) {
                final ChunkedInput<?> chunks = (ChunkedInput<?>) pendingMessage;
                boolean endOfInput;
                boolean suspend;
                Object message = null;
                try {
//                    从流中读取数据保存在缓存区中
                    message = chunks.readChunk(allocator);
//                    判断输入流中数据是否读取完毕
                    endOfInput = chunks.isEndOfInput();

                    if (message == null) {
                        // No need to suspend when reached at the end.
                        suspend = !endOfInput;
                    } else {
                        suspend = false;
                    }
                } catch (final Throwable t) {
                    this.currentWrite = null;

                    if (message != null) {
                        ReferenceCountUtil.release(message);
                    }

                    currentWrite.fail(t);
                    closeInput(chunks);
                    break;
                }

                if (suspend) {
                    // ChunkedInput.nextChunk() returned null and it has
                    // not reached at the end of input. Let's wait until
                    // more chunks arrive. Nothing to write or notify.
                    break;
                }

                if (message == null) {
                    // If message is null write an empty ByteBuf.
                    // See https://github.com/netty/netty/issues/1671
                    message = Unpooled.EMPTY_BUFFER;
                }

                ChannelFuture f = ctx.write(message);
                if (endOfInput) {
                    this.currentWrite = null;

                    // Register a listener which will close the input once the write is complete.
                    // This is needed because the Chunk may have some resource bound that can not
                    // be closed before its not written.
                    //
                    // See https://github.com/netty/netty/issues/303
                    f.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            currentWrite.progress(chunks.progress(), chunks.length());
                            currentWrite.success(chunks.length());
                            closeInput(chunks);
                        }
                    });
                } else if (channel.isWritable()) {
                    f.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (!future.isSuccess()) {
                                closeInput((ChunkedInput<?>) pendingMessage);
                                currentWrite.fail(future.cause());
                            } else {
                                currentWrite.progress(chunks.progress(), chunks.length());
                            }
                        }
                    });
                } else {
                    f.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (!future.isSuccess()) {
                                closeInput((ChunkedInput<?>) pendingMessage);
                                currentWrite.fail(future.cause());
                            } else {
                                currentWrite.progress(chunks.progress(), chunks.length());
                                if (channel.isWritable()) {
//                                    递归调用
                                    resumeTransfer();
                                }
                            }
                        }
                    });
                }
                // Flush each chunk to conserve memory
                ctx.flush();
                requiresFlush = false;
            } else {
                ctx.write(pendingMessage, currentWrite.promise);
                this.currentWrite = null;
                requiresFlush = true;
            }

            if (!channel.isActive()) {
                discard(new ClosedChannelException());
                break;
            }
        }

        if (requiresFlush) {
            ctx.flush();
        }
    }

    static void closeInput(ChunkedInput<?> chunks) {
        try {
            chunks.close();
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failed to close a chunked input.", t);
            }
        }
    }

    private static final class PendingWrite {
        final Object msg;
        final ChannelPromise promise;

        PendingWrite(Object msg, ChannelPromise promise) {
            this.msg = msg;
            this.promise = promise;
        }

        void fail(Throwable cause) {
            ReferenceCountUtil.release(msg);
            promise.tryFailure(cause);
        }

        void success(long total) {
            if (promise.isDone()) {
                // No need to notify the progress or fulfill the promise because it's done already.
                return;
            }

            if (promise instanceof ChannelProgressivePromise) {
                // Now we know what the total is.
                ((ChannelProgressivePromise) promise).tryProgress(total, total);
            }

            promise.trySuccess();
        }

        void progress(long progress, long total) {
            if (promise instanceof ChannelProgressivePromise) {
                ((ChannelProgressivePromise) promise).tryProgress(progress, total);
            }
        }
    }
}
