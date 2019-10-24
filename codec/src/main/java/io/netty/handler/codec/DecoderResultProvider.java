/*
 * Copyright 2014 The Netty Project
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

/**
 * Provides the accessor methods for the {@link DecoderResult} property of a decoded message.为解码消息的DecoderResult属性提供访问器方法。
 */
public interface DecoderResultProvider {
    /**
     * Returns the result of decoding this object.返回解码此对象的结果。
     */
    DecoderResult decoderResult();

    /**
     * Updates the result of decoding this object. This method is supposed to be invoked by a decoder.
     * Do not call this method unless you know what you are doing.更新解码此对象的结果。该方法应该由解码器调用。除非你知道你在做什么，否则不要调用这个方法。
     */
    void setDecoderResult(DecoderResult result);
}
