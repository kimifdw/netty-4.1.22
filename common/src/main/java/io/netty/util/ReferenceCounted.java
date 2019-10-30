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
package io.netty.util;

/**
 * A reference-counted object that requires explicit deallocation.
 * <p>
 * When a new {@link ReferenceCounted} is instantiated, it starts with the reference count of {@code 1}.
 * {@link #retain()} increases the reference count, and {@link #release()} decreases the reference count.
 * If the reference count is decreased to {@code 0}, the object will be deallocated explicitly, and accessing
 * the deallocated object will usually result in an access violation.
 * </p>
 * <p>
 * If an object that implements {@link ReferenceCounted} is a container of other objects that implement
 * {@link ReferenceCounted}, the contained objects will also be released via {@link #release()} when the container's
 * reference count becomes 0.
 * </p>
 * 需要显式释放的引用计数对象。
 当实例化一个新的referencecount时，它的引用计数为1。retain()增加引用计数，release()减少引用计数。如果引用计数减少到0，则对象将被显式地释放，访问释放对象通常会导致访问违反。
 如果实现referencount的对象是实现referencount的其他对象的容器，那么当容器的引用计数变为0时，包含的对象也将通过release()释放。
 */
//
public interface ReferenceCounted {
    /**
     * Returns the reference count of this object.  If {@code 0}, it means this object has been deallocated.返回该对象的引用计数。如果为0，则表示该对象已被释放。
     */
    int refCnt();

    /**
     * Increases the reference count by {@code 1}.将引用计数增加1。
     */
    ReferenceCounted retain();

    /**
     * Increases the reference count by the specified {@code increment}.通过指定的增量增加引用计数。
     */
    ReferenceCounted retain(int increment);

    /**
     * Records the current access location of this object for debugging purposes.
     * If this object is determined to be leaked, the information recorded by this operation will be provided to you
     * via {@link ResourceLeakDetector}.  This method is a shortcut to {@link #touch(Object) touch(null)}.
     * 为调试目的记录此对象的当前访问位置。如果此对象被确定为泄漏，则此操作记录的信息将通过resourceleak检测器提供给您。此方法是触摸(null)的快捷方式。
     */
    ReferenceCounted touch();

    /**
     * Records the current access location of this object with an additional arbitrary information for debugging
     * purposes.  If this object is determined to be leaked, the information recorded by this operation will be
     * provided to you via {@link ResourceLeakDetector}.为调试目的，使用附加的任意信息记录该对象的当前访问位置。如果该对象被判定为泄漏，则此操作记录的信息将通过ResourceLeakDetector提供给您。
     */
    ReferenceCounted touch(Object hint);

    /**
     * Decreases the reference count by {@code 1} and deallocates this object if the reference count reaches at
     * {@code 0}.
     *
     * @return {@code true} if and only if the reference count became {@code 0} and this object has been deallocated
     * 将引用计数减少1并在引用计数达到0时释放该对象。
     */
    boolean release();

    /**
     * Decreases the reference count by the specified {@code decrement} and deallocates this object if the reference
     * count reaches at {@code 0}.如果引用计数达到0，则通过指定的减量减少引用计数，并分配该对象。
     *
     * @return {@code true} if and only if the reference count became {@code 0} and this object has been deallocated
     */
    boolean release(int decrement);
}
