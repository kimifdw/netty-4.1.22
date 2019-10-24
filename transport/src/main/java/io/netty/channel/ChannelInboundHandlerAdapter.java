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

/**
 * Abstract base class for {@link ChannelInboundHandler} implementations which provide
 * implementations of all of their methods.
 *
 * <p>
 * This implementation just forward the operation to the next {@link ChannelHandler} in the
 * {@link ChannelPipeline}. Sub-classes may override a method implementation to change this.
 * </p>
 * <p>
 * Be aware that messages are not released after the {@link #channelRead(ChannelHandlerContext, Object)}
 * method returns automatically. If you are looking for a {@link ChannelInboundHandler} implementation that
 * releases the received messages automatically, please see {@link SimpleChannelInboundHandler}.
 * </p>
 * 提供了所有方法的实现的ChannelInboundHandler实现的基类。
 这个实现只是将操作转发到ChannelPipeline中的下一个ChannelHandler。子类可以重写方法实现来改变这一点。
 请注意，在channelRead(ChannelHandlerContext, Object)方法自动返回后，没有释放消息。如果您正在寻找自动释放接收到的消息的ChannelInboundHandler实现，请参见SimpleChannelInboundHandler。
 */
//适配器模式继承实现
public class ChannelInboundHandlerAdapter extends ChannelHandlerAdapter implements ChannelInboundHandler {

    /**
     * Calls {@link ChannelHandlerContext#fireChannelRegistered()} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     * 调用channelhandlercontext . firechannelregistration()将其转发到ChannelPipeline中的下一个channelelinboundhandler。子类可以重写此方法以更改行为。ChannelHandlerContext的通道被注册到它的EventLoop中
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelRegistered();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelUnregistered()} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     * 调用channelhandlercontext . firechannelunregistration()，将其转发到ChannelPipeline中的下一个channelelinboundhandler。子类可以重写此方法以更改行为。
     */
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelUnregistered();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelActive()} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     * 调用ChannelHandlerContext.fireChannelActive()将其转发到ChannelPipeline中的下一个channelelinboundhandler。子类可以重写此方法以更改行为。
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelInactive()} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     * 调用ChannelHandlerContext.fireChannelInactive()来转发到ChannelPipeline中的下一个ChannelInboundHandler。子类可以重写此方法以更改行为。
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelRead(Object)} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     * 调用ChannelHandlerContext.fireChannelRead(Object)，将其转发到ChannelPipeline中的下一个channelelinboundhandler。子类可以重写此方法以更改行为。
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.fireChannelRead(msg);
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelReadComplete()} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     * 调用ChannelHandlerContext.fireChannelReadComplete()，将其转发到ChannelPipeline中的下一个channelelinboundhandler。子类可以重写此方法以更改行为。
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelReadComplete();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireUserEventTriggered(Object)} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     * 调用channelhandlercontext . fireuserevent触发器(Object)，以在ChannelPipeline中转发到下一个channelelinboundhandler。子类可以重写此方法以更改行为。
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        ctx.fireUserEventTriggered(evt);
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelWritabilityChanged()} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     * 调用ChannelHandlerContext.fireChannelWritabilityChanged()，以转发到ChannelPipeline中的下一个ChannelInboundHandler。子类可以重写此方法以更改行为。
     */
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelWritabilityChanged();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireExceptionCaught(Throwable)} to forward
     * to the next {@link ChannelHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     * 调用ChannelHandlerContext.fireExceptionCaught(可抛出)以转发到ChannelPipeline中的下一个ChannelHandler。子类可以重写此方法以更改行为。
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.fireExceptionCaught(cause);
    }
}
