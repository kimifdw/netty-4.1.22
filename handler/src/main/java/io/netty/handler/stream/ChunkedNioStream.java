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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * A {@link ChunkedInput} that fetches data from a {@link ReadableByteChannel}
 * chunk by chunk.  Please note that the {@link ReadableByteChannel} must
 * operate in blocking mode.  Non-blocking mode channels are not supported.
 * 从可读的bytechannel块中逐块获取数据的块。请注意，ReadableByteChannel必须在阻塞模式下运行。不支持非阻塞模式通道。
 */
public class ChunkedNioStream implements ChunkedInput<ByteBuf> {

    private final ReadableByteChannel in;

    private final int chunkSize;
    private long offset;

    /**
     * Associated ByteBuffer
     */
    private final ByteBuffer byteBuffer;

    /**
     * Creates a new instance that fetches data from the specified channel.创建一个从指定通道获取数据的新实例。
     */
    public ChunkedNioStream(ReadableByteChannel in) {
        this(in, ChunkedStream.DEFAULT_CHUNK_SIZE);
    }

    /**
     * Creates a new instance that fetches data from the specified channel.创建一个从指定通道获取数据的新实例。
     *
     * @param chunkSize the number of bytes to fetch on each
     *                  {@link #readChunk(ChannelHandlerContext)} call
     */
    public ChunkedNioStream(ReadableByteChannel in, int chunkSize) {
        if (in == null) {
            throw new NullPointerException("in");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize: " + chunkSize +
                    " (expected: a positive integer)");
        }
        this.in = in;
        offset = 0;
        this.chunkSize = chunkSize;
        byteBuffer = ByteBuffer.allocate(chunkSize);
    }

    /**
     * Returns the number of transferred bytes.返回传输的字节数。
     */
    public long transferredBytes() {
        return offset;
    }

    @Override
    public boolean isEndOfInput() throws Exception {
        if (byteBuffer.position() > 0) {
            // A previous read was not over, so there is a next chunk in the buffer at least前一次读取未结束，因此至少缓冲区中还有下一个块
            return false;
        }
        if (in.isOpen()) {
            // Try to read a new part, and keep this part (no rewind)试着读一个新的部分，保持这个部分(不要倒带)
            int b = in.read(byteBuffer);
            if (b < 0) {
                return true;
            } else {
                offset += b;
                return false;
            }
        }
        return true;
    }

    @Override
    public void close() throws Exception {
        in.close();
    }

    @Deprecated
    @Override
    public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
        return readChunk(ctx.alloc());
    }

    @Override
    public ByteBuf readChunk(ByteBufAllocator allocator) throws Exception {
        if (isEndOfInput()) {
            return null;
        }
        // buffer cannot be not be empty from there缓冲区不能从那里不为空
        int readBytes = byteBuffer.position();
        for (;;) {
            int localReadBytes = in.read(byteBuffer);
            if (localReadBytes < 0) {
                break;
            }
            readBytes += localReadBytes;
            offset += localReadBytes;
            if (readBytes == chunkSize) {
                break;
            }
        }
        byteBuffer.flip();
        boolean release = true;
        ByteBuf buffer = allocator.buffer(byteBuffer.remaining());
        try {
            buffer.writeBytes(byteBuffer);
            byteBuffer.clear();
            release = false;
            return buffer;
        } finally {
            if (release) {
                buffer.release();
            }
        }
    }

    @Override
    public long length() {
        return -1;
    }

    @Override
    public long progress() {
        return offset;
    }
}
