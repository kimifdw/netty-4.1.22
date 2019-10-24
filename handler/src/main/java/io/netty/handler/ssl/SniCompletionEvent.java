/*
 * Copyright 2017 The Netty Project
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

import io.netty.util.internal.UnstableApi;

/**
 * Event that is fired once we did a selection of a {@link SslContext} based on the {@code SNI hostname},
 * which may be because it was successful or there was an error.
 * 一旦我们选择了基于SNI主机名的SslContext，就会被触发，这可能是因为它是成功的，或者是有错误的。
 */
@UnstableApi
public final class SniCompletionEvent extends SslCompletionEvent {
    private final String hostname;

    SniCompletionEvent(String hostname) {
        this.hostname = hostname;
    }

    SniCompletionEvent(String hostname, Throwable cause) {
        super(cause);
        this.hostname = hostname;
    }

    SniCompletionEvent(Throwable cause) {
        this(null, cause);
    }

    /**
     * Returns the SNI hostname send by the client if we were able to parse it, {@code null} otherwise.
     * 如果可以解析，返回客户端发送的SNI主机名，否则为空。
     */
    public String hostname() {
        return hostname;
    }

    @Override
    public String toString() {
        final Throwable cause = cause();
        return cause == null ? getClass().getSimpleName() + "(SUCCESS='"  + hostname + "'\")":
                getClass().getSimpleName() +  '(' + cause + ')';
    }
}
