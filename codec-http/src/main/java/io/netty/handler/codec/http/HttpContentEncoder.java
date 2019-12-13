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
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.ReferenceCountUtil;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Encodes the content of the outbound {@link HttpResponse} and {@link HttpContent}.
 * The original content is replaced with the new content encoded by the
 * {@link EmbeddedChannel}, which is created by {@link #beginEncode(HttpResponse, String)}.
 * Once encoding is finished, the value of the <tt>'Content-Encoding'</tt> header
 * is set to the target content encoding, as returned by
 * {@link #beginEncode(HttpResponse, String)}.
 * Also, the <tt>'Content-Length'</tt> header is updated to the length of the
 * encoded content.  If there is no supported or allowed encoding in the
 * corresponding {@link HttpRequest}'s {@code "Accept-Encoding"} header,
 * {@link #beginEncode(HttpResponse, String)} should return {@code null} so that
 * no encoding occurs (i.e. pass-through).
 * <p>
 * Please note that this is an abstract class.  You have to extend this class
 * and implement {@link #beginEncode(HttpResponse, String)} properly to make
 * this class functional.  For example, refer to the source code of
 * {@link HttpContentCompressor}.
 * <p>
 * This handler must be placed after {@link HttpObjectEncoder} in the pipeline
 * so that this handler can intercept HTTP responses before {@link HttpObjectEncoder}
 * converts them into {@link ByteBuf}s.
 * 对出站HttpResponse和HttpContent的内容进行编码。原始内容被由EmbeddedChannel(由beginEncode(HttpResponse, String)创建的新内容所替代。编码完成后，' content - encoding '头值被设置为目标内容编码，由beginEncode(HttpResponse, String)返回。此外，' content - length '标头被更新为编码内容的长度。如果在相应的HttpRequest的“Accept-Encoding”头中不支持或不允许编码，beginEncode(HttpResponse, String)应该返回null，这样就不会发生编码(即传递)。
 请注意，这是一个抽象类。您必须扩展这个类并正确地实现beginEncode(HttpResponse, String)以使这个类具有功能。例如，请参考HttpContentCompressor的源代码。
 这个处理程序必须放在管道中的HttpObjectEncoder之后，这样这个处理程序就可以在HttpObjectEncoder将HTTP响应转换成ByteBufs之前拦截HTTP响应。
 */
public abstract class HttpContentEncoder extends MessageToMessageCodec<HttpRequest, HttpObject> {

    private enum State {
        PASS_THROUGH,
        AWAIT_HEADERS,
        AWAIT_CONTENT
    }

    private static final CharSequence ZERO_LENGTH_HEAD = "HEAD";
    private static final CharSequence ZERO_LENGTH_CONNECT = "CONNECT";
    private static final int CONTINUE_CODE = HttpResponseStatus.CONTINUE.code();

    private final Queue<CharSequence> acceptEncodingQueue = new ArrayDeque<CharSequence>();
    private EmbeddedChannel encoder;
    private State state = State.AWAIT_HEADERS;

    @Override
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return msg instanceof HttpContent || msg instanceof HttpResponse;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpRequest msg, List<Object> out)
            throws Exception {
        CharSequence acceptedEncoding = msg.headers().get(HttpHeaderNames.ACCEPT_ENCODING);
        if (acceptedEncoding == null) {
            acceptedEncoding = HttpContentDecoder.IDENTITY;
        }

        HttpMethod meth = msg.method();
        if (meth == HttpMethod.HEAD) {
            acceptedEncoding = ZERO_LENGTH_HEAD;
        } else if (meth == HttpMethod.CONNECT) {
            acceptedEncoding = ZERO_LENGTH_CONNECT;
        }

        acceptEncodingQueue.add(acceptedEncoding);
        out.add(ReferenceCountUtil.retain(msg));
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) throws Exception {
        final boolean isFull = msg instanceof HttpResponse && msg instanceof LastHttpContent;
        switch (state) {
            case AWAIT_HEADERS: {
                ensureHeaders(msg);
                assert encoder == null;

                final HttpResponse res = (HttpResponse) msg;
                final int code = res.status().code();
                final CharSequence acceptEncoding;
                if (code == CONTINUE_CODE) {
                    // We need to not poll the encoding when response with CONTINUE as another response will follow
                    // for the issued request. See https://github.com/netty/netty/issues/4079//我们不需要轮询编码时，响应与继续，因为另一个响应将跟进
//对于发出的请求。
                    acceptEncoding = null;
                } else {
                    // Get the list of encodings accepted by the peer.获取对等方接受的编码列表。
                    acceptEncoding = acceptEncodingQueue.poll();
                    if (acceptEncoding == null) {
                        throw new IllegalStateException("cannot send more responses than requests");
                    }
                }

                /*
                 * per rfc2616 4.3 Message Body
                 * All 1xx (informational), 204 (no content), and 304 (not modified) responses MUST NOT include a
                 * message-body. All other responses do include a message-body, although it MAY be of zero length.*根据rfc2616 4.3消息体
*所有1xx(信息)、204(无内容)和304(未修改)响应都不能包含a
*消息体。所有其他响应都包含一个消息体，尽管它的长度可能为零。
                 *
                 * 9.4 HEAD
                 * The HEAD method is identical to GET except that the server MUST NOT return a message-body
                 * in the response.
                 *
                 * Also we should pass through HTTP/1.0 as transfer-encoding: chunked is not supported.HEAD方法与GET方法相同，只是服务器不能返回消息体
*在回应中。
*
*我们也应该通过HTTP/1.0作为传输编码:块是不支持的。
                 *
                 * See https://github.com/netty/netty/issues/5382
                 */
                if (isPassthru(res.protocolVersion(), code, acceptEncoding)) {
                    if (isFull) {
                        out.add(ReferenceCountUtil.retain(res));
                    } else {
                        out.add(res);
                        // Pass through all following contents.浏览以下所有内容。
                        state = State.PASS_THROUGH;
                    }
                    break;
                }

                if (isFull) {
                    // Pass through the full response with empty content and continue waiting for the next resp.传递包含空内容的完整响应，并继续等待下一个resp。
                    if (!((ByteBufHolder) res).content().isReadable()) {
                        out.add(ReferenceCountUtil.retain(res));
                        break;
                    }
                }

                // Prepare to encode the content.准备对内容进行编码。
                final Result result = beginEncode(res, acceptEncoding.toString());

                // If unable to encode, pass through.如果无法编码，则通过。
                if (result == null) {
                    if (isFull) {
                        out.add(ReferenceCountUtil.retain(res));
                    } else {
                        out.add(res);
                        // Pass through all following contents.浏览以下所有内容。
                        state = State.PASS_THROUGH;
                    }
                    break;
                }

                encoder = result.contentEncoder();

                // Encode the content and remove or replace the existing headers
                // so that the message looks like a decoded message.//对内容进行编码，并删除或替换现有的标头
//                使消息看起来像解码的消息。
                res.headers().set(HttpHeaderNames.CONTENT_ENCODING, result.targetContentEncoding());

                // Output the rewritten response.输出重写的响应。
                if (isFull) {
                    // Convert full message into unfull one.将完整的消息转换为不完整的消息。
                    HttpResponse newRes = new DefaultHttpResponse(res.protocolVersion(), res.status());
                    newRes.headers().set(res.headers());
                    out.add(newRes);

                    ensureContent(res);
                    encodeFullResponse(newRes, (HttpContent) res, out);
                    break;
                } else {
                    // Make the response chunked to simplify content transformation.将响应分块以简化内容转换。
                    res.headers().remove(HttpHeaderNames.CONTENT_LENGTH);
                    res.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);

                    out.add(res);
                    state = State.AWAIT_CONTENT;
                    if (!(msg instanceof HttpContent)) {
                        // only break out the switch statement if we have not content to process只有当我们不满足于处理时，才中断switch语句
                        // See https://github.com/netty/netty/issues/2006
                        break;
                    }
                    // Fall through to encode the content对内容进行编码
                }
            }
            case AWAIT_CONTENT: {
                ensureContent(msg);
                if (encodeContent((HttpContent) msg, out)) {
                    state = State.AWAIT_HEADERS;
                }
                break;
            }
            case PASS_THROUGH: {
                ensureContent(msg);
                out.add(ReferenceCountUtil.retain(msg));
                // Passed through all following contents of the current response.遍历当前响应的所有以下内容。
                if (msg instanceof LastHttpContent) {
                    state = State.AWAIT_HEADERS;
                }
                break;
            }
        }
    }

    private void encodeFullResponse(HttpResponse newRes, HttpContent content, List<Object> out) {
        int existingMessages = out.size();
        encodeContent(content, out);

        if (HttpUtil.isContentLengthSet(newRes)) {
            // adjust the content-length header调整内容长度标题
            int messageSize = 0;
            for (int i = existingMessages; i < out.size(); i++) {
                Object item = out.get(i);
                if (item instanceof HttpContent) {
                    messageSize += ((HttpContent) item).content().readableBytes();
                }
            }
            HttpUtil.setContentLength(newRes, messageSize);
        } else {
            newRes.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
        }
    }

    private static boolean isPassthru(HttpVersion version, int code, CharSequence httpMethod) {
        return code < 200 || code == 204 || code == 304 ||
               (httpMethod == ZERO_LENGTH_HEAD || (httpMethod == ZERO_LENGTH_CONNECT && code == 200)) ||
                version == HttpVersion.HTTP_1_0;
    }

    private static void ensureHeaders(HttpObject msg) {
        if (!(msg instanceof HttpResponse)) {
            throw new IllegalStateException(
                    "unexpected message type: " +
                    msg.getClass().getName() + " (expected: " + HttpResponse.class.getSimpleName() + ')');
        }
    }

    private static void ensureContent(HttpObject msg) {
        if (!(msg instanceof HttpContent)) {
            throw new IllegalStateException(
                    "unexpected message type: " +
                    msg.getClass().getName() + " (expected: " + HttpContent.class.getSimpleName() + ')');
        }
    }

    private boolean encodeContent(HttpContent c, List<Object> out) {
        ByteBuf content = c.content();

        encode(content, out);

        if (c instanceof LastHttpContent) {
            finishEncode(out);
            LastHttpContent last = (LastHttpContent) c;

            // Generate an additional chunk if the decoder produced
            // the last product on closure,//如果解码器产生，则生成一个附加块
//关闭的最后一个产品，
            HttpHeaders headers = last.trailingHeaders();
            if (headers.isEmpty()) {
                out.add(LastHttpContent.EMPTY_LAST_CONTENT);
            } else {
                out.add(new ComposedLastHttpContent(headers));
            }
            return true;
        }
        return false;
    }

    /**
     * Prepare to encode the HTTP message content.准备编码HTTP消息内容。
     *
     * @param headers
     *        the headers
     * @param acceptEncoding
     *        the value of the {@code "Accept-Encoding"} header
     *
     * @return the result of preparation, which is composed of the determined
     *         target content encoding and a new {@link EmbeddedChannel} that
     *         encodes the content into the target content encoding.
     *         {@code null} if {@code acceptEncoding} is unsupported or rejected
     *         and thus the content should be handled as-is (i.e. no encoding).
     */
    protected abstract Result beginEncode(HttpResponse headers, String acceptEncoding) throws Exception;

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        cleanupSafely(ctx);
        super.handlerRemoved(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        cleanupSafely(ctx);
        super.channelInactive(ctx);
    }

    private void cleanup() {
        if (encoder != null) {
            // Clean-up the previous encoder if not cleaned up correctly.清理前一个编码器，如果没有正确清理。
            encoder.finishAndReleaseAll();
            encoder = null;
        }
    }

    private void cleanupSafely(ChannelHandlerContext ctx) {
        try {
            cleanup();
        } catch (Throwable cause) {
            // If cleanup throws any error we need to propagate it through the pipeline
            // so we don't fail to propagate pipeline events.//如果清理抛出任何错误，我们需要通过管道传播它
//            这样我们就不会传播管道事件失败。
            ctx.fireExceptionCaught(cause);
        }
    }

    private void encode(ByteBuf in, List<Object> out) {
        // call retain here as it will call release after its written to the channel在这里调用retain，因为它将在写入通道后调用release
        encoder.writeOutbound(in.retain());
        fetchEncoderOutput(out);
    }

    private void finishEncode(List<Object> out) {
        if (encoder.finish()) {
            fetchEncoderOutput(out);
        }
        encoder = null;
    }

    private void fetchEncoderOutput(List<Object> out) {
        for (;;) {
            ByteBuf buf = encoder.readOutbound();
            if (buf == null) {
                break;
            }
            if (!buf.isReadable()) {
                buf.release();
                continue;
            }
            out.add(new DefaultHttpContent(buf));
        }
    }

    public static final class Result {
        private final String targetContentEncoding;
        private final EmbeddedChannel contentEncoder;

        public Result(String targetContentEncoding, EmbeddedChannel contentEncoder) {
            if (targetContentEncoding == null) {
                throw new NullPointerException("targetContentEncoding");
            }
            if (contentEncoder == null) {
                throw new NullPointerException("contentEncoder");
            }

            this.targetContentEncoding = targetContentEncoding;
            this.contentEncoder = contentEncoder;
        }

        public String targetContentEncoding() {
            return targetContentEncoding;
        }

        public EmbeddedChannel contentEncoder() {
            return contentEncoder;
        }
    }
}
