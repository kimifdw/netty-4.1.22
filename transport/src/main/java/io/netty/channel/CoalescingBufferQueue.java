/*
 * Copyright 2015 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;

/**
 * A FIFO queue of bytes where producers add bytes by repeatedly adding {@link ByteBuf} and consumers take bytes in
 * arbitrary lengths. This allows producers to add lots of small buffers and the consumer to take all the bytes
 * out in a single buffer. Conversely the producer may add larger buffers and the consumer could take the bytes in
 * many small buffers.
 *
 * <p>Bytes are added and removed with promises. If the last byte of a buffer added with a promise is removed then
 * that promise will complete when the promise passed to {@link #remove} completes.
 *
 * <p>This functionality is useful for aggregating or partitioning writes into fixed size buffers for framing protocols
 * such as HTTP2.
 * 一个FIFO字节队列，其中生产者通过重复添加ByteBuf添加字节，消费者以任意长度获取字节。这使得生产者可以添加许多小缓冲区，消费者可以在一个缓冲区中取出所有的字节。相反，生产者可以添加更大的缓冲区，消费者可以使用许多小缓冲区中的字节。
 使用承诺添加和删除字节。如果添加承诺的缓冲区的最后一个字节被删除，那么当传递给删除的承诺完成时，该承诺将完成。
 这个功能对于将写聚合或分区写入固定大小的缓冲区中以形成诸如HTTP2之类的协议非常有用。
 */
public final class CoalescingBufferQueue extends AbstractCoalescingBufferQueue {
    private final Channel channel;

    public CoalescingBufferQueue(Channel channel) {
        this(channel, 4);
    }

    public CoalescingBufferQueue(Channel channel, int initSize) {
        this(channel, initSize, false);
    }

    public CoalescingBufferQueue(Channel channel, int initSize, boolean updateWritability) {
        super(updateWritability ? channel : null, initSize);
        this.channel = ObjectUtil.checkNotNull(channel, "channel");
    }

    /**
     * Remove a {@link ByteBuf} from the queue with the specified number of bytes. Any added buffer who's bytes are
     * fully consumed during removal will have it's promise completed when the passed aggregate {@link ChannelPromise}
     * completes.
     *
     * @param bytes the maximum number of readable bytes in the returned {@link ByteBuf}, if {@code bytes} is greater
     *              than {@link #readableBytes} then a buffer of length {@link #readableBytes} is returned.
     * @param aggregatePromise used to aggregate the promises and listeners for the constituent buffers.
     * @return a {@link ByteBuf} composed of the enqueued buffers.
     * 使用指定的字节数从队列中删除ByteBuf。任何添加的缓冲区在删除过程中字节被完全消耗，当传递的聚合ChannelPromise完成时，它的承诺将被完成。
     */
    public ByteBuf remove(int bytes, ChannelPromise aggregatePromise) {
        return remove(channel.alloc(), bytes, aggregatePromise);
    }

    /**
     *  Release all buffers in the queue and complete all listeners and promises.释放队列中的所有缓冲区，并完成所有侦听器和承诺。
     */
    public void releaseAndFailAll(Throwable cause) {
        releaseAndFailAll(channel, cause);
    }

    @Override
    protected ByteBuf compose(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf next) {
        if (cumulation instanceof CompositeByteBuf) {
            CompositeByteBuf composite = (CompositeByteBuf) cumulation;
            composite.addComponent(true, next);
            return composite;
        }
        return composeIntoComposite(alloc, cumulation, next);
    }

    @Override
    protected ByteBuf removeEmptyValue() {
        return Unpooled.EMPTY_BUFFER;
    }
}
