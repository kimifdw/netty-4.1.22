/*
 * Copyright 2015 The Netty Project
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

import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * Result of detecting a protocol.
 *
 * @param <T> the type of the protocol
 */
public final class ProtocolDetectionResult<T> {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static final ProtocolDetectionResult NEEDS_MORE_DATE =
            new ProtocolDetectionResult(ProtocolDetectionState.NEEDS_MORE_DATA, null);
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static final ProtocolDetectionResult INVALID =
            new ProtocolDetectionResult(ProtocolDetectionState.INVALID, null);

    private final ProtocolDetectionState state;
    private final T result;

    /**
     * Returns a {@link ProtocolDetectionResult} that signals that more data is needed to detect the protocol.返回一个ProtocolDetectionResult，该结果表示需要更多数据来检测协议。
     */
    @SuppressWarnings("unchecked")
    public static <T> ProtocolDetectionResult<T> needsMoreData() {
        return NEEDS_MORE_DATE;
    }

    /**
     * Returns a {@link ProtocolDetectionResult} that signals the data was invalid for the protocol.返回一个ProtocolDetectionResult，该结果表示该协议的数据无效。
     */
    @SuppressWarnings("unchecked")
    public static <T> ProtocolDetectionResult<T> invalid() {
        return INVALID;
    }

    /**
     * Returns a {@link ProtocolDetectionResult} which holds the detected protocol.返回一个包含检测到的协议的ProtocolDetectionResult。
     */
    @SuppressWarnings("unchecked")
    public static <T> ProtocolDetectionResult<T> detected(T protocol) {
        return new ProtocolDetectionResult<T>(ProtocolDetectionState.DETECTED, checkNotNull(protocol, "protocol"));
    }

    private ProtocolDetectionResult(ProtocolDetectionState state, T result) {
        this.state = state;
        this.result = result;
    }

    /**
     * Return the {@link ProtocolDetectionState}. If the state is {@link ProtocolDetectionState#DETECTED} you
     * can retrieve the protocol via {@link #detectedProtocol()}.返回ProtocolDetectionState。如果状态是ProtocolDetectionState。检测到您可以通过detectedProtocol()检索协议。
     */
    public ProtocolDetectionState state() {
        return state;
    }

    /**
     * Returns the protocol if {@link #state()} returns {@link ProtocolDetectionState#DETECTED}, otherwise {@code null}.如果state()返回ProtocolDetectionState，则返回协议。发现,否则无效。
     */
    public T detectedProtocol() {
        return result;
    }
}
