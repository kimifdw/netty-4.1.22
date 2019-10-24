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
package io.netty.handler.codec.marshalling;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import org.jboss.marshalling.Marshaller;

/**
 * {@link MessageToByteEncoder} implementation which uses JBoss Marshalling to marshal
 * an Object.
 *
 * See <a href="http://www.jboss.org/jbossmarshalling">JBoss Marshalling website</a>
 * for more information
 *
 * Use {@link MarshallingEncoder} if possible.
 * MessageToByteEncoder实现，它使用JBoss编组来编组一个对象。如果可能，请参阅JBoss编组网站，了解更多信息，请使用MarshallingEncoder。
 *
 */
@Sharable
public class CompatibleMarshallingEncoder extends MessageToByteEncoder<Object> {

    private final MarshallerProvider provider;

    /**
     * Create a new instance of the {@link CompatibleMarshallingEncoder}
     *
     * @param provider  the {@link MarshallerProvider} to use to get the {@link Marshaller} for a {@link Channel}
     *                  创建兼容emarshallingencoder的一个新实例
     */
    public CompatibleMarshallingEncoder(MarshallerProvider provider) {
        this.provider = provider;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        Marshaller marshaller = provider.getMarshaller(ctx);
        marshaller.start(new ChannelBufferByteOutput(out));
        marshaller.writeObject(msg);
        marshaller.finish();
        marshaller.close();
    }

}
