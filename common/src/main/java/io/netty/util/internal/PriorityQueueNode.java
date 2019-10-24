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
package io.netty.util.internal;

/**
 * Provides methods for {@link DefaultPriorityQueue} to maintain internal state. These methods should generally not be
 * used outside the scope of {@link DefaultPriorityQueue}.为DefaultPriorityQueue提供维护内部状态的方法。这些方法通常不应在DefaultPriorityQueue之外使用。
 */
public interface PriorityQueueNode {
    /**
     * This should be used to initialize the storage returned by {@link #priorityQueueIndex(DefaultPriorityQueue)}.这应该用于初始化priorityQueueIndex(DefaultPriorityQueue)返回的存储。
     */
    int INDEX_NOT_IN_QUEUE = -1;

    /**
     * Get the last value set by {@link #priorityQueueIndex(DefaultPriorityQueue, int)} for the value corresponding to
     * {@code queue}.
     * <p>
     * Throwing exceptions from this method will result in undefined behavior.
     *
     获取priorityQueueIndex(DefaultPriorityQueue, int)为队列对应的值设置的最后一个值。
     从该方法中抛出异常将导致未定义的行为。
     */
    int priorityQueueIndex(DefaultPriorityQueue<?> queue);

    /**
     * Used by {@link DefaultPriorityQueue} to maintain state for an element in the queue.
     * <p>
     * Throwing exceptions from this method will result in undefined behavior.
     * @param queue The queue for which the index is being set.
     * @param i The index as used by {@link DefaultPriorityQueue}.
     *          由DefaultPriorityQueue使用，用于维护队列中元素的状态。
    从该方法中抛出异常将导致未定义的行为。
     */
    void priorityQueueIndex(DefaultPriorityQueue<?> queue, int i);
}
