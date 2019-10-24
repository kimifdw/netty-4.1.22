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
package io.netty.channel.pool;

/**
 * Allows to map {@link ChannelPool} implementations to a specific key.允许将ChannelPool实现映射到特定的键。
 *
 * @param <K> the type of the key
 * @param <P> the type of the {@link ChannelPool}
 */
public interface ChannelPoolMap<K, P extends ChannelPool> {
    /**
     * Return the {@link ChannelPool} for the {@code code}. This will never return {@code null},
     * but create a new {@link ChannelPool} if non exists for they requested {@code key}.
     *
     * Please note that {@code null} keys are not allowed.
     * 返回代码的通道池。这将永远不会返回空值，但是如果它们请求的键不存在，则创建一个新的通道池。请注意，不允许使用空键。
     */
    P get(K key);

    /**
     * Returns {@code true} if a {@link ChannelPool} exists for the given {@code key}.
     *
     * Please note that {@code null} keys are not allowed.
     * 如果给定键存在通道池，则返回true。请注意，不允许使用空键。
     */
    boolean contains(K key);
}
