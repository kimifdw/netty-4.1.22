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

import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.io.Closeable;

/**
 * Allows to acquire and release {@link Channel} and so act as a pool of these.允许获取和发布通道，因此充当这些通道的池。
 */
public interface ChannelPool extends Closeable {

    /**
     * Acquire a {@link Channel} from this {@link ChannelPool}. The returned {@link Future} is notified once
     * the acquire is successful and failed otherwise.
     *
     * <strong>Its important that an acquired is always released to the pool again, even if the {@link Channel}
     * is explicitly closed..</strong>
     * 从这个通道池中获取一个通道。一旦获取成功，否则失败，将通知返回的未来。重要的是，即使通道是显式关闭的，也要始终将获取的消息释放到池中。
     */
    Future<Channel> acquire();

    /**
     * Acquire a {@link Channel} from this {@link ChannelPool}. The given {@link Promise} is notified once
     * the acquire is successful and failed otherwise.
     *
     * <strong>Its important that an acquired is always released to the pool again, even if the {@link Channel}
     * is explicitly closed..</strong>
     * 从这个通道池中获取一个通道。一旦收购成功且失败，就会通知给定的承诺。重要的是，即使通道是显式关闭的，也要始终将获取的消息释放到池中。
     */
    Future<Channel> acquire(Promise<Channel> promise);

    /**
     * Release a {@link Channel} back to this {@link ChannelPool}. The returned {@link Future} is notified once
     * the release is successful and failed otherwise. When failed the {@link Channel} will automatically closed.
     * 释放一个通道回到这个通道池。一旦发布成功，否则失败，将通知返回的Future。当失败时，通道将自动关闭。
     */
    Future<Void> release(Channel channel);

    /**
     * Release a {@link Channel} back to this {@link ChannelPool}. The given {@link Promise} is notified once
     * the release is successful and failed otherwise. When failed the {@link Channel} will automatically closed.
     * 释放一个通道回到这个通道池。一旦发布成功且失败，就会通知给定的承诺。当失败时，通道将自动关闭。
     */
    Future<Void> release(Channel channel, Promise<Void> promise);

    @Override
    void close();
}
