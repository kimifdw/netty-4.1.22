/*
 * Copyright 2013 The Netty Project
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
package io.netty.handler.ssl;


/**
 * Event that is fired once the SSL handshake is complete, which may be because it was successful or there
 * was an error.
 * SSL握手完成后触发的事件，这可能是因为它成功了，或者有错误。
 */
//饿汉式单例模式
public final class SslHandshakeCompletionEvent extends SslCompletionEvent {

    public static final SslHandshakeCompletionEvent SUCCESS = new SslHandshakeCompletionEvent();

    /**
     * Creates a new event that indicates a successful handshake.创建一个表示成功握手的新事件。
     */
    private SslHandshakeCompletionEvent() { }

    /**
     * Creates a new event that indicates an unsuccessful handshake.
     * Use {@link #SUCCESS} to indicate a successful handshake.
     * 创建一个新的事件，指示未成功的握手。使用成功来表示握手成功。
     */
    public SslHandshakeCompletionEvent(Throwable cause) {
        super(cause);
    }
}
