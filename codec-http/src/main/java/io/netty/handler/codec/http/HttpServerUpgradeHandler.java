/*
 * Copyright 2014 The Netty Project
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
package io.netty.handler.codec.http;

import static io.netty.util.AsciiString.containsContentEqualsIgnoreCase;
import static io.netty.util.AsciiString.containsAllContentEqualsIgnoreCase;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.netty.handler.codec.http.HttpResponseStatus.SWITCHING_PROTOCOLS;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * A server-side handler that receives HTTP requests and optionally performs a protocol switch if
 * the requested protocol is supported. Once an upgrade is performed, this handler removes itself
 * from the pipeline.服务器端处理程序，接收HTTP请求，如果支持所请求的协议，则可选择执行协议切换。一旦执行了升级，此处理程序将从管道中删除自己。
 */
public class HttpServerUpgradeHandler extends HttpObjectAggregator {

    /**
     * The source codec that is used in the pipeline initially.管道中最初使用的源编解码器。
     */
    public interface SourceCodec {
        /**
         * Removes this codec (i.e. all associated handlers) from the pipeline.从管道中删除此编解码器(即所有关联的处理程序)。
         */
        void upgradeFrom(ChannelHandlerContext ctx);
    }

    /**
     * A codec that the source can be upgraded to.一个可以升级源代码的编解码器。
     */
    public interface UpgradeCodec {
        /**
         * Gets all protocol-specific headers required by this protocol for a successful upgrade.
         * Any supplied header will be required to appear in the {@link HttpHeaderNames#CONNECTION} header as well.
         * 获取此协议成功升级所需的所有特定于协议的标头。任何提供的标头都需要出现在HttpHeaderNames中。连接头。
         */
        Collection<CharSequence> requiredUpgradeHeaders();

        /**
         * Prepares the {@code upgradeHeaders} for a protocol update based upon the contents of {@code upgradeRequest}.
         * This method returns a boolean value to proceed or abort the upgrade in progress. If {@code false} is
         * returned, the upgrade is aborted and the {@code upgradeRequest} will be passed through the inbound pipeline
         * as if no upgrade was performed. If {@code true} is returned, the upgrade will proceed to the next
         * step which invokes {@link #upgradeTo}. When returning {@code true}, you can add headers to
         * the {@code upgradeHeaders} so that they are added to the 101 Switching protocols response.
         * 根据upgradeRequest的内容为协议更新准备upgradeHeaders。此方法返回一个布尔值，以继续或中止正在进行中的升级。如果返回false，升级将中止，upgradeRequest将通过入站管道传递，就像没有执行升级一样。如果返回true，升级将继续进行下一个步骤，调用upgradeTo。当返回true时，您可以向upgradeHeaders添加标题，以便它们被添加到101交换协议响应中。
         */
        boolean prepareUpgradeResponse(ChannelHandlerContext ctx, FullHttpRequest upgradeRequest,
                                    HttpHeaders upgradeHeaders);

        /**
         * Performs an HTTP protocol upgrade from the source codec. This method is responsible for
         * adding all handlers required for the new protocol.从源编解码器执行HTTP协议升级。此方法负责添加新协议所需的所有处理程序。
         *
         * @param ctx the context for the current handler.
         * @param upgradeRequest the request that triggered the upgrade to this protocol.
         */
        void upgradeTo(ChannelHandlerContext ctx, FullHttpRequest upgradeRequest);
    }

    /**
     * Creates a new {@link UpgradeCodec} for the requested protocol name.创建一个新的HttpServerUpgradeHandler。请求的协议名称的UpgradeCodec。
     */
    public interface UpgradeCodecFactory {
        /**
         * Invoked by {@link HttpServerUpgradeHandler} for all the requested protocol names in the order of
         * the client preference. The first non-{@code null} {@link UpgradeCodec} returned by this method
         * will be selected.由HttpServerUpgradeHandler根据客户端首选项的顺序调用所有请求的协议名称。第一个非空HttpServerUpgradeHandler。将选择此方法返回的UpgradeCodec。
         *
         * @return a new {@link UpgradeCodec}, or {@code null} if the specified protocol name is not supported
         */
        UpgradeCodec newUpgradeCodec(CharSequence protocol);
    }

    /**
     * User event that is fired to notify about the completion of an HTTP upgrade
     * to another protocol. Contains the original upgrade request so that the response
     * (if required) can be sent using the new protocol.触发用户事件，通知HTTP升级到另一个协议的完成。包含原始的升级请求，以便使用新协议发送响应(如果需要)。
     */
    public static final class UpgradeEvent implements ReferenceCounted {
        private final CharSequence protocol;
        private final FullHttpRequest upgradeRequest;

        UpgradeEvent(CharSequence protocol, FullHttpRequest upgradeRequest) {
            this.protocol = protocol;
            this.upgradeRequest = upgradeRequest;
        }

        /**
         * The protocol that the channel has been upgraded to.通道升级到的协议。
         */
        public CharSequence protocol() {
            return protocol;
        }

        /**
         * Gets the request that triggered the protocol upgrade.获取触发协议升级的请求。
         */
        public FullHttpRequest upgradeRequest() {
            return upgradeRequest;
        }

        @Override
        public int refCnt() {
            return upgradeRequest.refCnt();
        }

        @Override
        public UpgradeEvent retain() {
            upgradeRequest.retain();
            return this;
        }

        @Override
        public UpgradeEvent retain(int increment) {
            upgradeRequest.retain(increment);
            return this;
        }

        @Override
        public UpgradeEvent touch() {
            upgradeRequest.touch();
            return this;
        }

        @Override
        public UpgradeEvent touch(Object hint) {
            upgradeRequest.touch(hint);
            return this;
        }

        @Override
        public boolean release() {
            return upgradeRequest.release();
        }

        @Override
        public boolean release(int decrement) {
            return upgradeRequest.release(decrement);
        }

        @Override
        public String toString() {
            return "UpgradeEvent [protocol=" + protocol + ", upgradeRequest=" + upgradeRequest + ']';
        }
    }

    private final SourceCodec sourceCodec;
    private final UpgradeCodecFactory upgradeCodecFactory;
    private boolean handlingUpgrade;

    /**
     * Constructs the upgrader with the supported codecs.
     * <p>
     * The handler instantiated by this constructor will reject an upgrade request with non-empty content.
     * It should not be a concern because an upgrade request is most likely a GET request.
     * If you have a client that sends a non-GET upgrade request, please consider using
     * {@link #HttpServerUpgradeHandler(SourceCodec, UpgradeCodecFactory, int)} to specify the maximum
     * length of the content of an upgrade request.
     * </p>
     *
     * @param sourceCodec the codec that is being used initially
     * @param upgradeCodecFactory the factory that creates a new upgrade codec
     *                            for one of the requested upgrade protocols
     *                            用支持的编解码器构造升级程序。
    此构造函数实例化的处理程序将拒绝包含非空内容的升级请求。它不应该引起关注，因为升级请求很可能是GET请求。如果您有客户端发送非get升级请求，请考虑使用HttpServerUpgradeHandler(HttpServerUpgradeHandler)。SourceCodec HttpServerUpgradeHandler。UpgradeCodecFactory, int)来指定升级请求内容的最大长度。
     */
    public HttpServerUpgradeHandler(SourceCodec sourceCodec, UpgradeCodecFactory upgradeCodecFactory) {
        this(sourceCodec, upgradeCodecFactory, 0);
    }

    /**
     * Constructs the upgrader with the supported codecs.用支持的编解码器构造升级程序。
     *
     * @param sourceCodec the codec that is being used initially
     * @param upgradeCodecFactory the factory that creates a new upgrade codec
     *                            for one of the requested upgrade protocols
     * @param maxContentLength the maximum length of the content of an upgrade request
     */
    public HttpServerUpgradeHandler(
            SourceCodec sourceCodec, UpgradeCodecFactory upgradeCodecFactory, int maxContentLength) {
        super(maxContentLength);

        this.sourceCodec = checkNotNull(sourceCodec, "sourceCodec");
        this.upgradeCodecFactory = checkNotNull(upgradeCodecFactory, "upgradeCodecFactory");
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out)
            throws Exception {
        // Determine if we're already handling an upgrade request or just starting a new one.
        handlingUpgrade |= isUpgradeRequest(msg);
        if (!handlingUpgrade) {
            // Not handling an upgrade request, just pass it to the next handler.
            ReferenceCountUtil.retain(msg);
            out.add(msg);
            return;
        }

        FullHttpRequest fullRequest;
        if (msg instanceof FullHttpRequest) {
            fullRequest = (FullHttpRequest) msg;
            ReferenceCountUtil.retain(msg);
            out.add(msg);
        } else {
            // Call the base class to handle the aggregation of the full request.
            super.decode(ctx, msg, out);
            if (out.isEmpty()) {
                // The full request hasn't been created yet, still awaiting more data.
                return;
            }

            // Finished aggregating the full request, get it from the output list.
            assert out.size() == 1;
            handlingUpgrade = false;
            fullRequest = (FullHttpRequest) out.get(0);
        }

        if (upgrade(ctx, fullRequest)) {
            // The upgrade was successful, remove the message from the output list
            // so that it's not propagated to the next handler. This request will
            // be propagated as a user event instead.
            out.clear();
        }

        // The upgrade did not succeed, just allow the full request to propagate to the
        // next handler.
    }

    /**
     * Determines whether or not the message is an HTTP upgrade request.确定消息是否为HTTP升级请求。
     */
    private static boolean isUpgradeRequest(HttpObject msg) {
        return msg instanceof HttpRequest && ((HttpRequest) msg).headers().get(HttpHeaderNames.UPGRADE) != null;
    }

    /**
     * Attempts to upgrade to the protocol(s) identified by the {@link HttpHeaderNames#UPGRADE} header (if provided
     * in the request).试图升级到由HttpHeaderNames标识的协议。升级标题(如果在请求中提供)。
     *
     * @param ctx the context for this handler.
     * @param request the HTTP request.
     * @return {@code true} if the upgrade occurred, otherwise {@code false}.
     */
    private boolean upgrade(final ChannelHandlerContext ctx, final FullHttpRequest request) {
        // Select the best protocol based on those requested in the UPGRADE header.
        final List<CharSequence> requestedProtocols = splitHeader(request.headers().get(HttpHeaderNames.UPGRADE));
        final int numRequestedProtocols = requestedProtocols.size();
        UpgradeCodec upgradeCodec = null;
        CharSequence upgradeProtocol = null;
        for (int i = 0; i < numRequestedProtocols; i ++) {
            final CharSequence p = requestedProtocols.get(i);
            final UpgradeCodec c = upgradeCodecFactory.newUpgradeCodec(p);
            if (c != null) {
                upgradeProtocol = p;
                upgradeCodec = c;
                break;
            }
        }

        if (upgradeCodec == null) {
            // None of the requested protocols are supported, don't upgrade.
            return false;
        }

        // Make sure the CONNECTION header is present.
        CharSequence connectionHeader = request.headers().get(HttpHeaderNames.CONNECTION);
        if (connectionHeader == null) {
            return false;
        }

        // Make sure the CONNECTION header contains UPGRADE as well as all protocol-specific headers.
        Collection<CharSequence> requiredHeaders = upgradeCodec.requiredUpgradeHeaders();
        List<CharSequence> values = splitHeader(connectionHeader);
        if (!containsContentEqualsIgnoreCase(values, HttpHeaderNames.UPGRADE) ||
            !containsAllContentEqualsIgnoreCase(values, requiredHeaders)) {
            return false;
        }

        // Ensure that all required protocol-specific headers are found in the request.
        for (CharSequence requiredHeader : requiredHeaders) {
            if (!request.headers().contains(requiredHeader)) {
                return false;
            }
        }

        // Prepare and send the upgrade response. Wait for this write to complete before upgrading,
        // since we need the old codec in-place to properly encode the response.
        final FullHttpResponse upgradeResponse = createUpgradeResponse(upgradeProtocol);
        if (!upgradeCodec.prepareUpgradeResponse(ctx, request, upgradeResponse.headers())) {
            return false;
        }

        // Create the user event to be fired once the upgrade completes.
        final UpgradeEvent event = new UpgradeEvent(upgradeProtocol, request);

        final UpgradeCodec finalUpgradeCodec = upgradeCodec;
        ctx.writeAndFlush(upgradeResponse).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                try {
                    if (future.isSuccess()) {
                        // Perform the upgrade to the new protocol.
                        sourceCodec.upgradeFrom(ctx);
                        finalUpgradeCodec.upgradeTo(ctx, request);

                        // Notify that the upgrade has occurred. Retain the event to offset
                        // the release() in the finally block.
                        ctx.fireUserEventTriggered(event.retain());

                        // Remove this handler from the pipeline.
                        ctx.pipeline().remove(HttpServerUpgradeHandler.this);
                    } else {
                        future.channel().close();
                    }
                } finally {
                    // Release the event if the upgrade event wasn't fired.
                    event.release();
                }
            }
        });
        return true;
    }

    /**
     * Creates the 101 Switching Protocols response message.创建101交换协议响应消息。
     */
    private static FullHttpResponse createUpgradeResponse(CharSequence upgradeProtocol) {
        DefaultFullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, SWITCHING_PROTOCOLS,
                Unpooled.EMPTY_BUFFER, false);
        res.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE);
        res.headers().add(HttpHeaderNames.UPGRADE, upgradeProtocol);
        return res;
    }

    /**
     * Splits a comma-separated header value. The returned set is case-insensitive and contains each
     * part with whitespace removed.将逗号分隔的标头值分开。返回的集合不区分大小写，包含删除空格的每个部分。
     */
    private static List<CharSequence> splitHeader(CharSequence header) {
        final StringBuilder builder = new StringBuilder(header.length());
        final List<CharSequence> protocols = new ArrayList<CharSequence>(4);
        for (int i = 0; i < header.length(); ++i) {
            char c = header.charAt(i);
            if (Character.isWhitespace(c)) {
                // Don't include any whitespace.
                continue;
            }
            if (c == ',') {
                // Add the string and reset the builder for the next protocol.
                protocols.add(builder.toString());
                builder.setLength(0);
            } else {
                builder.append(c);
            }
        }

        // Add the last protocol
        if (builder.length() > 0) {
            protocols.add(builder.toString());
        }

        return protocols;
    }
}
