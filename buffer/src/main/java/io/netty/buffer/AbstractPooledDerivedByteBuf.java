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

package io.netty.buffer;

import io.netty.util.Recycler.Handle;
import io.netty.util.ReferenceCounted;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Abstract base class for derived {@link ByteBuf} implementations.抽象基类派生的ByteBuf实现。
 */
abstract class AbstractPooledDerivedByteBuf extends AbstractReferenceCountedByteBuf {

    private final Handle<AbstractPooledDerivedByteBuf> recyclerHandle;
    private AbstractByteBuf rootParent;
    /**
     * Deallocations of a pooled derived buffer should always propagate through the entire chain of derived buffers.
     * This is because each pooled derived buffer maintains its own reference count and we should respect each one.
     * If deallocations cause a release of the "root parent" then then we may prematurely release the underlying
     * content before all the derived buffers have been released.
     * 池化的派生缓冲区的释放应该始终传播到派生缓冲区的整个链中。这是因为每个池派生缓冲区都维护自己的引用计数，我们应该尊重每个引用计数。如果解除分配导致“根父类”的释放，那么我们可以在释放所有派生缓冲区之前提前释放底层内容。
     */
    private ByteBuf parent;

    @SuppressWarnings("unchecked")
    AbstractPooledDerivedByteBuf(Handle<? extends AbstractPooledDerivedByteBuf> recyclerHandle) {
        super(0);
        this.recyclerHandle = (Handle<AbstractPooledDerivedByteBuf>) recyclerHandle;
    }

//
    // Called from within SimpleLeakAwareByteBuf and AdvancedLeakAwareByteBuf.
    final void parent(ByteBuf newParent) {
        assert newParent instanceof SimpleLeakAwareByteBuf;
        parent = newParent;
    }

    @Override
    public final AbstractByteBuf unwrap() {
        return rootParent;
    }

    final <U extends AbstractPooledDerivedByteBuf> U init(
            AbstractByteBuf unwrapped, ByteBuf wrapped, int readerIndex, int writerIndex, int maxCapacity) {
        wrapped.retain(); // Retain up front to ensure the parent is accessible before doing more work.在做更多的工作之前，预先保留以确保可以访问父进程。
        parent = wrapped;
        rootParent = unwrapped;

        try {
            maxCapacity(maxCapacity);
            setIndex0(readerIndex, writerIndex); // It is assumed the bounds checking is done by the caller.假定边界检查是由调用者完成的。
            setRefCnt(1);

            @SuppressWarnings("unchecked")
            final U castThis = (U) this;
            wrapped = null;
            return castThis;
        } finally {
            if (wrapped != null) {
                parent = rootParent = null;
                wrapped.release();
            }
        }
    }

    @Override
    protected final void deallocate() {
        // We need to first store a reference to the parent before recycle this instance. This is needed as
        // otherwise it is possible that the same AbstractPooledDerivedByteBuf is again obtained and init(...) is
        // called before we actually have a chance to call release(). This leads to call release() on the wrong parent.//在回收这个实例之前，我们需要先存储一个对父类的引用。这是需要的
//否则，可能会再次获得相同的AbstractPooledDerivedByteBuf，而init(…)是
//在我们有机会调用release()之前调用。这将导致在错误的父节点上调用release()。
        ByteBuf parent = this.parent;
        recyclerHandle.recycle(this);
        parent.release();
    }

    @Override
    public final ByteBufAllocator alloc() {
        return unwrap().alloc();
    }

    @Override
    @Deprecated
    public final ByteOrder order() {
        return unwrap().order();
    }

    @Override
    public boolean isReadOnly() {
        return unwrap().isReadOnly();
    }

    @Override
    public final boolean isDirect() {
        return unwrap().isDirect();
    }

    @Override
    public boolean hasArray() {
        return unwrap().hasArray();
    }

    @Override
    public byte[] array() {
        return unwrap().array();
    }

    @Override
    public boolean hasMemoryAddress() {
        return unwrap().hasMemoryAddress();
    }

    @Override
    public final int nioBufferCount() {
        return unwrap().nioBufferCount();
    }

    @Override
    public final ByteBuffer internalNioBuffer(int index, int length) {
        return nioBuffer(index, length);
    }

    @Override
    public final ByteBuf retainedSlice() {
        final int index = readerIndex();
        return retainedSlice(index, writerIndex() - index);
    }

    @Override
    public ByteBuf slice(int index, int length) {
        // All reference count methods should be inherited from this object (this is the "parent").所有引用计数方法都应该继承自这个对象(这是“父”对象)。
        return new PooledNonRetainedSlicedByteBuf(this, unwrap(), index, length);
    }

    final ByteBuf duplicate0() {
        // All reference count methods should be inherited from this object (this is the "parent").
        return new PooledNonRetainedDuplicateByteBuf(this, unwrap());
    }

    private static final class PooledNonRetainedDuplicateByteBuf extends UnpooledDuplicatedByteBuf {
        private final ReferenceCounted referenceCountDelegate;

        PooledNonRetainedDuplicateByteBuf(ReferenceCounted referenceCountDelegate, AbstractByteBuf buffer) {
            super(buffer);
            this.referenceCountDelegate = referenceCountDelegate;
        }

        @Override
        int refCnt0() {
            return referenceCountDelegate.refCnt();
        }

        @Override
        ByteBuf retain0() {
            referenceCountDelegate.retain();
            return this;
        }

        @Override
        ByteBuf retain0(int increment) {
            referenceCountDelegate.retain(increment);
            return this;
        }

        @Override
        ByteBuf touch0() {
            referenceCountDelegate.touch();
            return this;
        }

        @Override
        ByteBuf touch0(Object hint) {
            referenceCountDelegate.touch(hint);
            return this;
        }

        @Override
        boolean release0() {
            return referenceCountDelegate.release();
        }

        @Override
        boolean release0(int decrement) {
            return referenceCountDelegate.release(decrement);
        }

        @Override
        public ByteBuf duplicate() {
            return new PooledNonRetainedDuplicateByteBuf(referenceCountDelegate, this);
        }

        @Override
        public ByteBuf retainedDuplicate() {
            return PooledDuplicatedByteBuf.newInstance(unwrap(), this, readerIndex(), writerIndex());
        }

        @Override
        public ByteBuf slice(int index, int length) {
            checkIndex0(index, length);
            return new PooledNonRetainedSlicedByteBuf(referenceCountDelegate, unwrap(), index, length);
        }

        @Override
        public ByteBuf retainedSlice() {
            // Capacity is not allowed to change for a sliced ByteBuf, so length == capacity()对于切片的ByteBuf，容量不允许改变，因此length == Capacity ()
            return retainedSlice(readerIndex(), capacity());
        }

        @Override
        public ByteBuf retainedSlice(int index, int length) {
            return PooledSlicedByteBuf.newInstance(unwrap(), this, index, length);
        }
    }

    private static final class PooledNonRetainedSlicedByteBuf extends UnpooledSlicedByteBuf {
        private final ReferenceCounted referenceCountDelegate;

        PooledNonRetainedSlicedByteBuf(ReferenceCounted referenceCountDelegate,
                                       AbstractByteBuf buffer, int index, int length) {
            super(buffer, index, length);
            this.referenceCountDelegate = referenceCountDelegate;
        }

        @Override
        int refCnt0() {
            return referenceCountDelegate.refCnt();
        }

        @Override
        ByteBuf retain0() {
            referenceCountDelegate.retain();
            return this;
        }

        @Override
        ByteBuf retain0(int increment) {
            referenceCountDelegate.retain(increment);
            return this;
        }

        @Override
        ByteBuf touch0() {
            referenceCountDelegate.touch();
            return this;
        }

        @Override
        ByteBuf touch0(Object hint) {
            referenceCountDelegate.touch(hint);
            return this;
        }

        @Override
        boolean release0() {
            return referenceCountDelegate.release();
        }

        @Override
        boolean release0(int decrement) {
            return referenceCountDelegate.release(decrement);
        }

        @Override
        public ByteBuf duplicate() {
            return new PooledNonRetainedDuplicateByteBuf(referenceCountDelegate, unwrap())
                    .setIndex(idx(readerIndex()), idx(writerIndex()));
        }

        @Override
        public ByteBuf retainedDuplicate() {
            return PooledDuplicatedByteBuf.newInstance(unwrap(), this, idx(readerIndex()), idx(writerIndex()));
        }

        @Override
        public ByteBuf slice(int index, int length) {
            checkIndex0(index, length);
            return new PooledNonRetainedSlicedByteBuf(referenceCountDelegate, unwrap(), idx(index), length);
        }

        @Override
        public ByteBuf retainedSlice() {
            // Capacity is not allowed to change for a sliced ByteBuf, so length == capacity()对于切片的ByteBuf，容量不允许改变，因此length == Capacity ()
            return retainedSlice(0, capacity());
        }

        @Override
        public ByteBuf retainedSlice(int index, int length) {
            return PooledSlicedByteBuf.newInstance(unwrap(), this, idx(index), length);
        }
    }
}
