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

package io.netty.channel;

import java.io.Serializable;

/**
 * Represents the globally unique identifier of a {@link Channel}.
 * <p>
 * The identifier is generated from various sources listed in the following:
 * <ul>
 * <li>MAC address (EUI-48 or EUI-64) or the network adapter, preferably a globally unique one,</li>
 * <li>the current process ID,</li>
 * <li>{@link System#currentTimeMillis()},</li>
 * <li>{@link System#nanoTime()},</li>
 * <li>a random 32-bit integer, and</li>
 * <li>a sequentially incremented 32-bit integer.</li>
 * </ul>
 * </p>
 * <p>
 * The global uniqueness of the generated identifier mostly depends on the MAC address and the current process ID,
 * which are auto-detected at the class-loading time in best-effort manner.  If all attempts to acquire them fail,
 * a warning message is logged, and random values will be used instead.  Alternatively, you can specify them manually
 * via system properties:
 * <ul>
 * <li>{@code io.netty.machineId} - hexadecimal representation of 48 (or 64) bit integer,
 *     optionally separated by colon or hyphen.</li>
 * <li>{@code io.netty.processId} - an integer between 0 and 65535</li>
 * </ul>
 * </p>
 * 表示通道的全局惟一标识符。
 标识符由以下列出的各种来源生成:
 MAC地址(EUI-48或EUI-64)或网络适配器，最好是全局唯一的，
 当前的进程ID,
 System.currentTimeMillis(),
 system . nanotime(),
 一个32位的随机整数
 一个连续递增的32位整数。
 生成的标识符的全局惟一性主要取决于MAC地址和当前进程ID，它们在类加载时以最佳方式自动检测。如果所有获取它们的尝试都失败，则会记录一条警告消息，并使用随机值。您也可以通过系统属性手动指定它们:
 machineid -十六进制表示48(或64)位整数，可选择用冒号或连字符分隔。
 processid——0到65535之间的整数
 */
public interface ChannelId extends Serializable, Comparable<ChannelId> {
    /**
     * Returns the short but globally non-unique string representation of the {@link ChannelId}.返回ChannelId的简短但全局的非唯一字符串表示形式。
     */
    String asShortText();

    /**
     * Returns the long yet globally unique string representation of the {@link ChannelId}.返回ChannelId的长而全局唯一的字符串表示形式。
     */
    String asLongText();
}
