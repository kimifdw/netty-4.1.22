/*
 * Copyright 2016 The Netty Project
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
 * WriteBufferWaterMark is used to set low water mark and high water mark for the write buffer.
 * <p>
 * If the number of bytes queued in the write buffer exceeds the
 * {@linkplain #high high water mark}, {@link Channel#isWritable()}
 * will start to return {@code false}.
 * <p>
 * If the number of bytes queued in the write buffer exceeds the
 * {@linkplain #high high water mark} and then
 * dropped down below the {@linkplain #low low water mark},
 * {@link Channel#isWritable()} will start to return
 * {@code true} again.
 * 使用WriteBufferWaterMark为写缓冲区设置低水位标记和高水位标记。
 如果写缓冲区中排队的字节数超过了高水位标记，Channel.isWritable()将开始返回false。
 如果写缓冲区中排队的字节数超过了高水位标记，然后下降到低水位标记以下，Channel.isWritable()将再次返回true。
 */
public final class WriteBufferWaterMark {

    private static final int DEFAULT_LOW_WATER_MARK = 32 * 1024;
    private static final int DEFAULT_HIGH_WATER_MARK = 64 * 1024;

    public static final WriteBufferWaterMark DEFAULT =
            new WriteBufferWaterMark(DEFAULT_LOW_WATER_MARK, DEFAULT_HIGH_WATER_MARK, false);

    private final int low;
    private final int high;

    /**
     * Create a new instance.
     *
     * @param low low water mark for write buffer.
     * @param high high water mark for write buffer
     */
    public WriteBufferWaterMark(int low, int high) {
        this(low, high, true);
    }

    /**
     * This constructor is needed to keep backward-compatibility.需要这个构造函数来保持向后兼容性。
     */
    WriteBufferWaterMark(int low, int high, boolean validate) {
        if (validate) {
            if (low < 0) {
                throw new IllegalArgumentException("write buffer's low water mark must be >= 0");
            }
            if (high < low) {
                throw new IllegalArgumentException(
                        "write buffer's high water mark cannot be less than " +
                                " low water mark (" + low + "): " +
                                high);
            }
        }
        this.low = low;
        this.high = high;
    }

    /**
     * Returns the low water mark for the write buffer.返回写缓冲区的低水位标记。
     */
    public int low() {
        return low;
    }

    /**
     * Returns the high water mark for the write buffer.返回写入缓冲区的高水位标记。
     */
    public int high() {
        return high;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(55)
            .append("WriteBufferWaterMark(low: ")
            .append(low)
            .append(", high: ")
            .append(high)
            .append(")");
        return builder.toString();
    }

}
