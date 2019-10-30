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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static io.netty.util.internal.ObjectUtil.checkPositive;

/**
 * Abstract base class for classes wants to implement {@link ReferenceCounted}.类的抽象基类想要实现referencount。
 */
public abstract class AbstractReferenceCounted implements ReferenceCounted {

//    原子方式修改字段值
    private static final AtomicIntegerFieldUpdater<AbstractReferenceCounted> refCntUpdater =
            AtomicIntegerFieldUpdater.newUpdater(AbstractReferenceCounted.class, "refCnt");

//    volatile保证线程可见性但不是线程安全的
    private volatile int refCnt = 1;

//    上面两行代码一般可以实现线程开关类似的功能，当然也可以使用atomic的类

    @Override
    public final int refCnt() {
        return refCnt;
    }

    /**
     * An unsafe operation intended for use by a subclass that sets the reference count of the buffer directly 用于直接设置缓冲区引用计数的子类的不安全操作
     */
//    线程安全的方式设置该属性值
    protected final void setRefCnt(int refCnt) {
        refCntUpdater.set(this, refCnt);
    }

    @Override
    public ReferenceCounted retain() {
        return retain0(1);
    }

    @Override
    public ReferenceCounted retain(int increment) {
        return retain0(checkPositive(increment, "increment"));
    }

    private ReferenceCounted retain0(int increment) {
        int oldRef = refCntUpdater.getAndAdd(this, increment);
        if (oldRef <= 0 || oldRef + increment < oldRef) {
            // Ensure we don't resurrect (which means the refCnt was 0) and also that we encountered an overflow.确保我们没有复活(这意味着refCnt是0)，并且我们遇到了溢出。
            refCntUpdater.getAndAdd(this, -increment);
            throw new IllegalReferenceCountException(oldRef, increment);
        }
        return this;
    }

    @Override
    public ReferenceCounted touch() {
        return touch(null);
    }

    @Override
    public boolean release() {
        return release0(1);
    }

    @Override
    public boolean release(int decrement) {
        return release0(checkPositive(decrement, "decrement"));
    }

    private boolean release0(int decrement) {
        int oldRef = refCntUpdater.getAndAdd(this, -decrement);
        if (oldRef == decrement) {
            deallocate();
            return true;
        } else if (oldRef < decrement || oldRef - decrement > oldRef) {
            // Ensure we don't over-release, and avoid underflow.确保我们不会过度释放，避免下溢。
            refCntUpdater.getAndAdd(this, decrement);
            throw new IllegalReferenceCountException(oldRef, decrement);
        }
        return false;
    }

    /**
     * Called once {@link #refCnt()} is equals 0.当refCnt()等于0时调用。
     */
    protected abstract void deallocate();
}
