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
package io.netty.handler.codec;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCounted;
import io.netty.util.internal.TypeParameterMatcher;

import java.util.List;

/**
 * A Codec for on-the-fly encoding/decoding of message.
 *
 * This can be thought of as a combination of {@link MessageToMessageDecoder} and {@link MessageToMessageEncoder}.
 *
 * Here is an example of a {@link MessageToMessageCodec} which just decode from {@link Integer} to {@link Long}
 * and encode from {@link Long} to {@link Integer}.
 *
 * <pre>
 *     public class NumberCodec extends
 *             {@link MessageToMessageCodec}&lt;{@link Integer}, {@link Long}&gt; {
 *         {@code @Override}
 *         public {@link Long} decode({@link ChannelHandlerContext} ctx, {@link Integer} msg, List&lt;Object&gt; out)
 *                 throws {@link Exception} {
 *             out.add(msg.longValue());
 *         }
 *
 *         {@code @Override}
 *         public {@link Integer} encode({@link ChannelHandlerContext} ctx, {@link Long} msg, List&lt;Object&gt; out)
 *                 throws {@link Exception} {
 *             out.add(msg.intValue());
 *         }
 *     }
 * </pre>
 *
 * Be aware that you need to call {@link ReferenceCounted#retain()} on messages that are just passed through if they
 * are of type {@link ReferenceCounted}. This is needed as the {@link MessageToMessageCodec} will call
 * {@link ReferenceCounted#release()} on encoded / decoded messages.
 * 对消息进行动态编码/解码的编解码器。这可以认为是MessageToMessageDecoder和MessageToMessageEncoder的组合。这里有一个MessageToMessageCodec的例子，它从整数到长解码，从长到整数编码。
 * 请注意，您需要调用referencecouncil .retain()对于刚刚传递的消息，如果这些消息的类型为referencecount。这是必要的，因为MessageToMessageCodec将调用referencecoun.release()对编码/解码消息。
 */
public abstract class MessageToMessageCodec<INBOUND_IN, OUTBOUND_IN> extends ChannelDuplexHandler {

    private final MessageToMessageEncoder<Object> encoder = new MessageToMessageEncoder<Object>() {

        @Override
        public boolean acceptOutboundMessage(Object msg) throws Exception {
            return MessageToMessageCodec.this.acceptOutboundMessage(msg);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
            MessageToMessageCodec.this.encode(ctx, (OUTBOUND_IN) msg, out);
        }
    };

    private final MessageToMessageDecoder<Object> decoder = new MessageToMessageDecoder<Object>() {

        @Override
        public boolean acceptInboundMessage(Object msg) throws Exception {
            return MessageToMessageCodec.this.acceptInboundMessage(msg);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void decode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
            MessageToMessageCodec.this.decode(ctx, (INBOUND_IN) msg, out);
        }
    };

    private final TypeParameterMatcher inboundMsgMatcher;
    private final TypeParameterMatcher outboundMsgMatcher;

    /**
     * Create a new instance which will try to detect the types to decode and encode out of the type parameter
     * of the class.创建一个新的实例，该实例将尝试检测用于解码和编码类的类型参数的类型。
     */
    protected MessageToMessageCodec() {
        inboundMsgMatcher = TypeParameterMatcher.find(this, MessageToMessageCodec.class, "INBOUND_IN");
        outboundMsgMatcher = TypeParameterMatcher.find(this, MessageToMessageCodec.class, "OUTBOUND_IN");
    }

    /**
     * Create a new instance.
     *
     * @param inboundMessageType    The type of messages to decode
     * @param outboundMessageType   The type of messages to encode
     */
    protected MessageToMessageCodec(
            Class<? extends INBOUND_IN> inboundMessageType, Class<? extends OUTBOUND_IN> outboundMessageType) {
        inboundMsgMatcher = TypeParameterMatcher.get(inboundMessageType);
        outboundMsgMatcher = TypeParameterMatcher.get(outboundMessageType);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        decoder.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        encoder.write(ctx, msg, promise);
    }

    /**
     * Returns {@code true} if and only if the specified message can be decoded by this codec.如果且仅当指定的消息可以被此编解码器解码时，返回true。
     *
     * @param msg the message
     */
    public boolean acceptInboundMessage(Object msg) throws Exception {
        return inboundMsgMatcher.match(msg);
    }

    /**
     * Returns {@code true} if and only if the specified message can be encoded by this codec.如果且仅当指定的消息可以由此编解码器编码时，返回true。
     *
     * @param msg the message
     */
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return outboundMsgMatcher.match(msg);
    }

    /**
     * @see MessageToMessageEncoder#encode(ChannelHandlerContext, Object, List)
     */
    protected abstract void encode(ChannelHandlerContext ctx, OUTBOUND_IN msg, List<Object> out)
            throws Exception;

    /**
     * @see MessageToMessageDecoder#decode(ChannelHandlerContext, Object, List)
     */
    protected abstract void decode(ChannelHandlerContext ctx, INBOUND_IN msg, List<Object> out)
            throws Exception;
}
