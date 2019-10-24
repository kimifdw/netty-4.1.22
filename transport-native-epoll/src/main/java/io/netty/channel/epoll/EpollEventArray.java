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
package io.netty.channel.epoll;

import io.netty.util.internal.PlatformDependent;

/**
 * This is an internal datastructure which can be directly passed to epoll_wait to reduce the overhead.
 *
 * typedef union epoll_data {
 *     void        *ptr;
 *     int          fd;
 *     uint32_t     u32;
 *     uint64_t     u64;
 * } epoll_data_t;
 *
 * struct epoll_event {
 *     uint32_t     events;      // Epoll events
 *     epoll_data_t data;        // User data variable
 * };
 *
 * We use {@code fd} if the {@code epoll_data union} to store the actual file descriptor of an
 * {@link AbstractEpollChannel} and so be able to map it later.
 * 这是一个内部数据结构，可以直接传递给epoll_wait以减少开销。定义union epoll_data {void *ptr;int fd;uint32_t u32;uint64_t u64;} epoll_data_t;struct epoll_event {uint32_t events;// Epoll事件epoll_data_t数据;//用户数据变量};如果epoll_data联合存储AbstractEpollChannel的实际文件描述符，并且以后能够映射它，则使用fd。
 */
//
final class EpollEventArray {
    // Size of the epoll_event struct
    private static final int EPOLL_EVENT_SIZE = Native.sizeofEpollEvent();
    // The offsiet of the data union in the epoll_event struct
    private static final int EPOLL_DATA_OFFSET = Native.offsetofEpollData();

    private long memoryAddress;
    private int length;
//
    EpollEventArray(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("length must be >= 1 but was " + length);
        }
        this.length = length;
        memoryAddress = allocate(length);
    }
//
    private static long allocate(int length) {
        return PlatformDependent.allocateMemory(length * EPOLL_EVENT_SIZE);
    }

    /**
     * Return the {@code memoryAddress} which points to the start of this {@link EpollEventArray}.返回指向这个EpollEventArray开头的memoryAddress。
     */
    long memoryAddress() {
        return memoryAddress;
    }

    /**
     * Return the length of the {@link EpollEventArray} which represent the maximum number of {@code epoll_events}
     * that can be stored in it.返回EpollEventArray的长度，它表示可以存储在其中的epoll_events的最大数量。
     */
    int length() {
        return length;
    }

    /**
     * Increase the storage of this {@link EpollEventArray}.增加这个EpollEventArray的存储。
     */
    void increase() {
        // double the size
        length <<= 1;
        free();
        memoryAddress = allocate(length);
    }

    /**
     * Free this {@link EpollEventArray}. Any usage after calling this method may segfault the JVM!
     * 这个EpollEventArray免费。调用此方法后的任何用法都可能导致JVM故障!
     */
    void free() {
        PlatformDependent.freeMemory(memoryAddress);
    }

    /**
     * Return the events for the {@code epoll_event} on this index.返回这个索引上epoll_event的事件。
     */
    int events(int index) {
        return PlatformDependent.getInt(memoryAddress + index * EPOLL_EVENT_SIZE);
    }

    /**
     * Return the file descriptor for the {@code epoll_event} on this index.返回这个索引上epoll_event的文件描述符。
     */
    int fd(int index) {
        return PlatformDependent.getInt(memoryAddress + index * EPOLL_EVENT_SIZE + EPOLL_DATA_OFFSET);
    }
}
