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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.util.AsciiString;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.netty.handler.codec.http.HttpResponseStatus.SWITCHING_PROTOCOLS;
import static io.netty.util.ReferenceCountUtil.release;

/**
 * Client-side handler for handling an HTTP upgrade handshake to another protocol. When the first
 * HTTP request is sent, this handler will add all appropriate headers to perform an upgrade to the
 * new protocol. If the upgrade fails (i.e. response is not 101 Switching Protocols), this handler
 * simply removes itself from the pipeline. If the upgrade is successful, upgrades the pipeline to
 * the new protocol.
 * 客户端处理程序，用于处理到另一个协议的HTTP升级握手。当发送第一个HTTP请求时，该处理程序将添加所有适当的头，以执行对新协议的升级。如果升级失败(即响应不是101交换协议)，该处理程序将自己从管道中删除。如果升级成功，则将管道升级到新协议。
 */
public class HttpClientUpgradeHandler extends HttpObjectAggregator implements ChannelOutboundHandler {

    /**
     * User events that are fired to notify about upgrade status.为了通知升级状态而触发的用户事件。
     */
    public enum UpgradeEvent {
        /**
         * The Upgrade request was sent to the server.升级请求被发送到服务器。
         */
        UPGRADE_ISSUED,

        /**
         * The Upgrade to the new protocol was successful.对新协议的升级是成功的。
         */
        UPGRADE_SUCCESSFUL,

        /**
         * The Upgrade was unsuccessful due to the server not issuing
         * with a 101 Switching Protocols response.升级失败是因为服务器没有发出101个交换协议响应。
         */
        UPGRADE_REJECTED
    }

    /**
     * The source codec that is used in the pipeline initially.管道中最初使用的源编解码器。
     */
    public interface SourceCodec {

        /**
         * Removes or disables the encoder of this codec so that the {@link UpgradeCodec} can send an initial greeting
         * (if any).删除或禁用此编解码器，以便HttpClientUpgradeHandler。UpgradeCodec可以发送一个初始的问候(如果有的话)。
         */
        void prepareUpgradeFrom(ChannelHandlerContext ctx);

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
         * Returns the name of the protocol supported by this codec, as indicated by the {@code 'UPGRADE'} header.返回此编解码器支持的协议的名称，如“升级”标头所示。
         */
        CharSequence protocol();

        /**
         * Sets any protocol-specific headers required to the upgrade request. Returns the names of
         * all headers that were added. These headers will be used to populate the CONNECTION header.设置升级请求所需的任何特定于协议的标头。返回添加的所有标题的名称。这些标头将用于填充连接标头。
         */
        Collection<CharSequence> setUpgradeHeaders(ChannelHandlerContext ctx, HttpRequest upgradeRequest);

        /**
         * Performs an HTTP protocol upgrade from the source codec. This method is responsible for
         * adding all handlers required for the new protocol.从源编解码器执行HTTP协议升级。此方法负责添加新协议所需的所有处理程序。
         *
         * @param ctx the context for the current handler.
         * @param upgradeResponse the 101 Switching Protocols response that indicates that the server
         *            has switched to this protocol.
         */
        void upgradeTo(ChannelHandlerContext ctx, FullHttpResponse upgradeResponse) throws Exception;
    }

    private final SourceCodec sourceCodec;
    private final UpgradeCodec upgradeCodec;
    private boolean upgradeRequested;

    /**
     * Constructs the client upgrade handler.构造客户端升级处理程序。
     *
     * @param sourceCodec the codec that is being used initially.
     * @param upgradeCodec the codec that the client would like to upgrade to.
     * @param maxContentLength the maximum length of the aggregated content.
     */
    public HttpClientUpgradeHandler(SourceCodec sourceCodec, UpgradeCodec upgradeCodec,
                                    int maxContentLength) {
        super(maxContentLength);
        if (sourceCodec == null) {
            throw new NullPointerException("sourceCodec");
        }
        if (upgradeCodec == null) {
            throw new NullPointerException("upgradeCodec");
        }
        this.sourceCodec = sourceCodec;
        this.upgradeCodec = upgradeCodec;
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.bind(localAddress, promise);
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
                        ChannelPromise promise) throws Exception {
        ctx.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.disconnect(promise);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.close(promise);
    }

    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.deregister(promise);
    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
            throws Exception {
        if (!(msg instanceof HttpRequest)) {
            ctx.write(msg, promise);
            return;
        }

        if (upgradeRequested) {
            promise.setFailure(new IllegalStateException(
                    "Attempting to write HTTP request with upgrade in progress"));
            return;
        }

        upgradeRequested = true;
        setUpgradeRequestHeaders(ctx, (HttpRequest) msg);

        // Continue writing the request.
        ctx.write(msg, promise);

        // Notify that the upgrade request was issued.通知已发出升级请求。
        ctx.fireUserEventTriggered(UpgradeEvent.UPGRADE_ISSUED);
        // Now we wait for the next HTTP response to see if we switch protocols.现在我们等待下一个HTTP响应，看看是否切换协议。
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out)
            throws Exception {
        FullHttpResponse response = null;
        try {
            if (!upgradeRequested) {
                throw new IllegalStateException("Read HTTP response without requesting protocol switch");
            }

            if (msg instanceof HttpResponse) {
                HttpResponse rep = (HttpResponse) msg;
                if (!SWITCHING_PROTOCOLS.equals(rep.status())) {
                    // The server does not support the requested protocol, just remove this handler
                    // and continue processing HTTP.
                    // NOTE: not releasing the response since we're letting it propagate to the
                    // next handler.//服务器不支持请求的协议，删除这个处理程序
//并继续处理HTTP。
//注意:不释放响应，因为我们让它传播到
//下一个处理程序。
                    ctx.fireUserEventTriggered(UpgradeEvent.UPGRADE_REJECTED);
                    removeThisHandler(ctx);
                    ctx.fireChannelRead(msg);
                    return;
                }
            }

            if (msg instanceof FullHttpResponse) {
                response = (FullHttpResponse) msg;
                // Need to retain since the base class will release after returning from this method.需要保留，因为基类将在从这个方法返回后释放。
                response.retain();
                out.add(response);
            } else {
                // Call the base class to handle the aggregation of the full request.调用基类来处理完整请求的聚合。
                super.decode(ctx, msg, out);
                if (out.isEmpty()) {
                    // The full request hasn't been created yet, still awaiting more data.完整的请求尚未创建，仍在等待更多的数据。
                    return;
                }

                assert out.size() == 1;
                response = (FullHttpResponse) out.get(0);
            }

            CharSequence upgradeHeader = response.headers().get(HttpHeaderNames.UPGRADE);
            if (upgradeHeader != null && !AsciiString.contentEqualsIgnoreCase(upgradeCodec.protocol(), upgradeHeader)) {
                throw new IllegalStateException(
                        "Switching Protocols response with unexpected UPGRADE protocol: " + upgradeHeader);
            }

            // Upgrade to the new protocol.
            sourceCodec.prepareUpgradeFrom(ctx);
            upgradeCodec.upgradeTo(ctx, response);

            // Notify that the upgrade to the new protocol completed successfully.通知新协议的升级成功完成。
            ctx.fireUserEventTriggered(UpgradeEvent.UPGRADE_SUCCESSFUL);

            // We guarantee UPGRADE_SUCCESSFUL event will be arrived at the next handler
            // before http2 setting frame and http response.//我们保证UPGRADE_SUCCESSFUL事件会在下一个处理程序到达
//在http2设置帧和http响应之前。
            sourceCodec.upgradeFrom(ctx);

            // We switched protocols, so we're done with the upgrade response.
            // Release it and clear it from the output.//我们交换了协议，所以我们完成了升级响应。
//释放它并从输出中清除它。
            response.release();
            out.clear();
            removeThisHandler(ctx);
        } catch (Throwable t) {
            release(response);
            ctx.fireExceptionCaught(t);
            removeThisHandler(ctx);
        }
    }

    private static void removeThisHandler(ChannelHandlerContext ctx) {
        ctx.pipeline().remove(ctx.name());
    }

    /**
     * Adds all upgrade request headers necessary for an upgrade to the supported protocols.将升级所需的所有升级请求标头添加到支持的协议。
     */
    private void setUpgradeRequestHeaders(ChannelHandlerContext ctx, HttpRequest request) {
        // Set the UPGRADE header on the request.设置请求的升级标题。
        request.headers().set(HttpHeaderNames.UPGRADE, upgradeCodec.protocol());

        // Add all protocol-specific headers to the request.将所有协议特定的标头添加到请求中。
        Set<CharSequence> connectionParts = new LinkedHashSet<CharSequence>(2);
        connectionParts.addAll(upgradeCodec.setUpgradeHeaders(ctx, request));

        // Set the CONNECTION header from the set of all protocol-specific headers that were added.从所添加的所有协议特定的标头集设置连接标头。
        StringBuilder builder = new StringBuilder();
        for (CharSequence part : connectionParts) {
            builder.append(part);
            builder.append(',');
        }
        builder.append(HttpHeaderValues.UPGRADE);
        request.headers().set(HttpHeaderNames.CONNECTION, builder.toString());
    }
}
