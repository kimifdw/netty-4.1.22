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

import java.net.SocketAddress;

/**
 * {@link ChannelHandler} implementation which represents a combination out of a {@link ChannelInboundHandler} and
 * the {@link ChannelOutboundHandler}.
 *
 * It is a good starting point if your {@link ChannelHandler} implementation needs to intercept operations and also
 * state updates.
 * ChannelHandler实现，它表示一个来自ChannelInboundHandler和ChannelOutboundHandler的组合。如果您的ChannelHandler实现需要拦截操作和状态更新，那么这是一个很好的起点。
 */
public class ChannelDuplexHandler extends ChannelInboundHandlerAdapter implements ChannelOutboundHandler {

    /**
     * Calls {@link ChannelHandlerContext#bind(SocketAddress, ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     * 调用ChannelHandlerContext。绑定(SocketAddress, ChannelPromise)转发到ChannelPipeline中的下一个ChannelOutboundHandler。子类可以重写此方法以更改行为。
     */
    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress,
                     ChannelPromise promise) throws Exception {
        ctx.bind(localAddress, promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#connect(SocketAddress, SocketAddress, ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     * 调用ChannelHandlerContext。连接(SocketAddress、SocketAddress、ChannelPromise)到ChannelOutboundHandler中的下一个channelboundhandler。子类可以重写此方法以更改行为。
     */
    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
                        SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.connect(remoteAddress, localAddress, promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#disconnect(ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     * 调用ChannelHandlerContext.disconnect(ChannelPromise)，将其转发到ChannelOutboundHandler中。子类可以重写此方法以更改行为。
     */
    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise)
            throws Exception {
        ctx.disconnect(promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#close(ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     * 调用ChannelHandlerContext.close(ChannelPromise)以在ChannelPipeline中转发到下一个ChannelOutboundHandler。子类可以重写此方法以更改行为。
     */
    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.close(promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#close(ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     * 调用ChannelHandlerContext.close(ChannelPromise)以在ChannelPipeline中转发到下一个ChannelOutboundHandler。子类可以重写此方法以更改行为。
     */
    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.deregister(promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#read()} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     * 调用ChannelHandlerContext.read()将其转发到ChannelOutboundHandler中。子类可以重写此方法以更改行为。
     */
    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    /**
     * Calls {@link ChannelHandlerContext#write(Object, ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     * 调用ChannelHandlerContext。写入(对象，ChannelPromise)以转发到ChannelOutboundHandler中的下一个channelboundhandler。子类可以重写此方法以更改行为。
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.write(msg, promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#flush()} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     * 调用ChannelHandlerContext.flush()来转发到ChannelPipeline中的下一个ChannelOutboundHandler。子类可以重写此方法以更改行为。
     */
    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
