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
import io.netty.channel.ChannelHandlerContext;

/**
 * A data stream of indefinite length which is consumed by {@link ChunkedWriteHandler}.ChunkedWriteHandler使用的一种不确定长度的数据流。
 */
public interface ChunkedInput<B> {

    /**
     * Return {@code true} if and only if there is no data left in the stream
     * and the stream has reached at its end.返回true，如果且仅当流中没有数据，并且流已经到达它的末端。
     */
    boolean isEndOfInput() throws Exception;

    /**
     * Releases the resources associated with the input.释放与输入相关的资源。
     */
    void close() throws Exception;

    /**
     * @deprecated Use {@link #readChunk(ByteBufAllocator)}.
     *
     * <p>Fetches a chunked data from the stream. Once this method returns the last chunk
     * and thus the stream has reached at its end, any subsequent {@link #isEndOfInput()}
     * call must return {@code true}.从流中获取块数据。一旦这个方法返回最后一个块，因此流已经到达它的末端，任何后续的isEndOfInput()调用都必须返回true。
     *
     * @param ctx The context which provides a {@link ByteBufAllocator} if buffer allocation is necessary.
     * @return the fetched chunk.
     *         {@code null} if there is no data left in the stream.
     *         Please note that {@code null} does not necessarily mean that the
     *         stream has reached at its end.  In a slow stream, the next chunk
     *         might be unavailable just momentarily.
     */
    @Deprecated
    B readChunk(ChannelHandlerContext ctx) throws Exception;

    /**
     * Fetches a chunked data from the stream. Once this method returns the last chunk
     * and thus the stream has reached at its end, any subsequent {@link #isEndOfInput()}
     * call must return {@code true}.
     *
     * @param allocator {@link ByteBufAllocator} if buffer allocation is necessary.
     * @return the fetched chunk.
     *         {@code null} if there is no data left in the stream.
     *         Please note that {@code null} does not necessarily mean that the
     *         stream has reached at its end.  In a slow stream, the next chunk
     *         might be unavailable just momentarily.
     *         从流中获取分块数据。一旦该方法返回最后一个块，从而流在其末尾到达，随后的任何isEndOfInput()调用都必须返回true。
     */
    B readChunk(ByteBufAllocator allocator) throws Exception;

    /**
     * Returns the length of the input.返回输入的长度。
     * @return  the length of the input if the length of the input is known.
     *          a negative value if the length of the input is unknown.
     */
    long length();

    /**
     * Returns current transfer progress.返回当前的转移进展。
     */
    long progress();

}
