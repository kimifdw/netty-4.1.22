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
 * The {@link RecvByteBufAllocator} that always yields the same buffer
 * size prediction.  This predictor ignores the feed back from the I/O thread.RecvByteBufAllocator总是产生相同的缓冲区大小预测。此预测器忽略来自I/O线程的反馈。
 */
public class FixedRecvByteBufAllocator extends DefaultMaxMessagesRecvByteBufAllocator {

    private final int bufferSize;

    private final class HandleImpl extends MaxMessageHandle {
        private final int bufferSize;

        public HandleImpl(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        @Override
        public int guess() {
            return bufferSize;
        }
    }

    /**
     * Creates a new predictor that always returns the same prediction of
     * the specified buffer size.创建一个新的预测器，该预测器总是返回指定缓冲区大小的相同预测。
     */
    public FixedRecvByteBufAllocator(int bufferSize) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException(
                    "bufferSize must greater than 0: " + bufferSize);
        }
        this.bufferSize = bufferSize;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Handle newHandle() {
        return new HandleImpl(bufferSize);
    }

    @Override
    public FixedRecvByteBufAllocator respectMaybeMoreData(boolean respectMaybeMoreData) {
        super.respectMaybeMoreData(respectMaybeMoreData);
        return this;
    }
}
