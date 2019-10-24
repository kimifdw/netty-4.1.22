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
package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.TooLongFrameException;


/**
 * Decodes {@link ByteBuf}s into {@link HttpResponse}s and
 * {@link HttpContent}s.
 *
 * <h3>Parameters that prevents excessive memory consumption</h3>
 * <table border="1">
 * <tr>
 * <th>Name</th><th>Meaning</th>
 * </tr>
 * <tr>
 * <td>{@code maxInitialLineLength}</td>
 * <td>The maximum length of the initial line (e.g. {@code "HTTP/1.0 200 OK"})
 *     If the length of the initial line exceeds this value, a
 *     {@link TooLongFrameException} will be raised.</td>
 * </tr>
 * <tr>
 * <td>{@code maxHeaderSize}</td>
 * <td>The maximum length of all headers.  If the sum of the length of each
 *     header exceeds this value, a {@link TooLongFrameException} will be raised.</td>
 * </tr>
 * <tr>
 * <td>{@code maxChunkSize}</td>
 * <td>The maximum length of the content or each chunk.  If the content length
 *     exceeds this value, the transfer encoding of the decoded response will be
 *     converted to 'chunked' and the content will be split into multiple
 *     {@link HttpContent}s.  If the transfer encoding of the HTTP response is
 *     'chunked' already, each chunk will be split into smaller chunks if the
 *     length of the chunk exceeds this value.  If you prefer not to handle
 *     {@link HttpContent}s in your handler, insert {@link HttpObjectAggregator}
 *     after this decoder in the {@link ChannelPipeline}.</td>
 * </tr>
 * </table>
 *
 * <h3>Decoding a response for a <tt>HEAD</tt> request</h3>
 * <p>
 * Unlike other HTTP requests, the successful response of a <tt>HEAD</tt>
 * request does not have any content even if there is <tt>Content-Length</tt>
 * header.  Because {@link HttpResponseDecoder} is not able to determine if the
 * response currently being decoded is associated with a <tt>HEAD</tt> request,
 * you must override {@link #isContentAlwaysEmpty(HttpMessage)} to return
 * <tt>true</tt> for the response of the <tt>HEAD</tt> request.
 * </p><p>
 * If you are writing an HTTP client that issues a <tt>HEAD</tt> request,
 * please use {@link HttpClientCodec} instead of this decoder.  It will perform
 * additional state management to handle the responses for <tt>HEAD</tt>
 * requests correctly.
 * </p>
 *
 * <h3>Decoding a response for a <tt>CONNECT</tt> request</h3>
 * <p>
 * You also need to do additional state management to handle the response of a
 * <tt>CONNECT</tt> request properly, like you did for <tt>HEAD</tt>.  One
 * difference is that the decoder should stop decoding completely after decoding
 * the successful 200 response since the connection is not an HTTP connection
 * anymore.
 * </p><p>
 * {@link HttpClientCodec} also handles this edge case correctly, so you have to
 * use {@link HttpClientCodec} if you are writing an HTTP client that issues a
 * <tt>CONNECT</tt> request.
 * </p>
 * 将ByteBufs解码为HttpResponses和HttpContents。
 防止过度内存消耗的参数
 的名字
 意义
 maxInitialLineLength
 起始行的最大长度(例如如果初始行的长度超过这个值，就会引发TooLongFrameException异常。
 maxHeaderSize
 所有标题的最大长度。如果每个标头的长度之和超过这个值，就会引发TooLongFrameException异常。
 maxChunkSize
 内容或每个块的最大长度。如果内容长度超过此值，解码响应的传输编码将被转换为“分块”，内容将被分割为多个HttpContents。如果HTTP响应的传输编码已经“分块”，那么如果块的长度超过这个值，每个块将被分割成更小的块。如果您不想在处理程序中处理HttpContents，请在ChannelPipeline中在这个解码器之后插入HttpObjectAggregator。
 解码头请求的响应
 与其他HTTP请求不同，HEAD请求的成功响应不包含任何内容，即使有内容长度的头。由于HttpResponseDecoder无法确定当前正在解码的响应是否与HEAD请求相关联，因此您必须重写isContentAlwaysEmpty(HttpMessage)以返回true以响应HEAD请求。
 如果您正在编写发出HEAD请求的HTTP客户机，请使用HttpClientCodec而不是这个解码器。它将执行额外的状态管理，以正确地处理HEAD请求的响应。
 解码连接请求的响应
 您还需要进行额外的状态管理，以正确地处理连接请求的响应，就像您为HEAD所做的那样。一个区别是，在解码成功的200个响应之后，解码器应该完全停止解码，因为连接不再是HTTP连接。
 HttpClientCodec也正确地处理了这种边缘情况，所以如果您正在编写发出连接请求的HTTP客户机，那么必须使用HttpClientCodec。
 */
public class HttpResponseDecoder extends HttpObjectDecoder {

    private static final HttpResponseStatus UNKNOWN_STATUS = new HttpResponseStatus(999, "Unknown");

    /**
     * Creates a new instance with the default
     * {@code maxInitialLineLength (4096)}, {@code maxHeaderSize (8192)}, and
     * {@code maxChunkSize (8192)}.使用默认的maxInitialLineLength(4096)、maxHeaderSize(8192)和maxChunkSize(8192)创建一个新实例。
     */
    public HttpResponseDecoder() {
    }

    /**
     * Creates a new instance with the specified parameters.使用指定的参数创建一个新实例。
     */
    public HttpResponseDecoder(
            int maxInitialLineLength, int maxHeaderSize, int maxChunkSize) {
        super(maxInitialLineLength, maxHeaderSize, maxChunkSize, true);
    }

    public HttpResponseDecoder(
            int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders) {
        super(maxInitialLineLength, maxHeaderSize, maxChunkSize, true, validateHeaders);
    }

    public HttpResponseDecoder(
            int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders,
            int initialBufferSize) {
        super(maxInitialLineLength, maxHeaderSize, maxChunkSize, true, validateHeaders, initialBufferSize);
    }

    @Override
    protected HttpMessage createMessage(String[] initialLine) {
        return new DefaultHttpResponse(
                HttpVersion.valueOf(initialLine[0]),
                HttpResponseStatus.valueOf(Integer.parseInt(initialLine[1]), initialLine[2]), validateHeaders);
    }

    @Override
    protected HttpMessage createInvalidMessage() {
        return new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, UNKNOWN_STATUS, validateHeaders);
    }

    @Override
    protected boolean isDecodingRequest() {
        return false;
    }
}
