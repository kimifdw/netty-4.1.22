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

import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Collection of method to handle objects that may implement {@link ReferenceCounted}.处理可能实现引用的对象的方法集合。
 */
public final class ReferenceCountUtil {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ReferenceCountUtil.class);

    static {
        ResourceLeakDetector.addExclusions(ReferenceCountUtil.class, "touch");
    }

    /**
     * Try to call {@link ReferenceCounted#retain()} if the specified message implements {@link ReferenceCounted}.
     * If the specified message doesn't implement {@link ReferenceCounted}, this method does nothing.
     * 如果指定的消息实现了referencecound()，则尝试调用referencecound()。如果指定的消息没有实现referencount，则此方法什么也不做。
     */
    @SuppressWarnings("unchecked")
    public static <T> T retain(T msg) {
        if (msg instanceof ReferenceCounted) {
            return (T) ((ReferenceCounted) msg).retain();
        }
        return msg;
    }

    /**
     * Try to call {@link ReferenceCounted#retain(int)} if the specified message implements {@link ReferenceCounted}.
     * If the specified message doesn't implement {@link ReferenceCounted}, this method does nothing.
     * 如果指定的消息实现了referencecoun.com .retain(int)。如果指定的消息没有实现referencecount，则该方法不会执行任何操作。
     */
    @SuppressWarnings("unchecked")
    public static <T> T retain(T msg, int increment) {
        if (msg instanceof ReferenceCounted) {
            return (T) ((ReferenceCounted) msg).retain(increment);
        }
        return msg;
    }

    /**
     * Tries to call {@link ReferenceCounted#touch()} if the specified message implements {@link ReferenceCounted}.
     * If the specified message doesn't implement {@link ReferenceCounted}, this method does nothing.
     * 如果指定的消息实现了referencecoun.com .touch()，请尝试调用它。如果指定的消息没有实现referencecount，则该方法不会执行任何操作。
     */
    @SuppressWarnings("unchecked")
    public static <T> T touch(T msg) {
        if (msg instanceof ReferenceCounted) {
            return (T) ((ReferenceCounted) msg).touch();
        }
        return msg;
    }

    /**
     * Tries to call {@link ReferenceCounted#touch(Object)} if the specified message implements
     * {@link ReferenceCounted}.  If the specified message doesn't implement {@link ReferenceCounted},
     * this method does nothing.如果指定的消息实现了referencecound .touch(对象)。如果指定的消息没有实现referencount，则此方法什么也不做。
     */
    @SuppressWarnings("unchecked")
    public static <T> T touch(T msg, Object hint) {
        if (msg instanceof ReferenceCounted) {
            return (T) ((ReferenceCounted) msg).touch(hint);
        }
        return msg;
    }

    /**
     * Try to call {@link ReferenceCounted#release()} if the specified message implements {@link ReferenceCounted}.
     * If the specified message doesn't implement {@link ReferenceCounted}, this method does nothing.
     * 如果指定的消息实现了referencecound .release()，请尝试调用referencecound()。如果指定的消息没有实现referencount，则此方法什么也不做。
     */
    public static boolean release(Object msg) {
        if (msg instanceof ReferenceCounted) {
            return ((ReferenceCounted) msg).release();
        }
        return false;
    }

    /**
     * Try to call {@link ReferenceCounted#release(int)} if the specified message implements {@link ReferenceCounted}.
     * If the specified message doesn't implement {@link ReferenceCounted}, this method does nothing.
     * 如果指定的消息实现了referencecoun.com .release(int)。如果指定的消息没有实现referencecount，则该方法不会执行任何操作。
     */
    public static boolean release(Object msg, int decrement) {
        if (msg instanceof ReferenceCounted) {
            return ((ReferenceCounted) msg).release(decrement);
        }
        return false;
    }

    /**
     * Try to call {@link ReferenceCounted#release()} if the specified message implements {@link ReferenceCounted}.
     * If the specified message doesn't implement {@link ReferenceCounted}, this method does nothing.
     * Unlike {@link #release(Object)} this method catches an exception raised by {@link ReferenceCounted#release()}
     * and logs it, rather than rethrowing it to the caller.  It is usually recommended to use {@link #release(Object)}
     * instead, unless you absolutely need to swallow an exception.
     * 如果指定的消息实现了referencecound .release()，请尝试调用referencecound()。如果指定的消息没有实现referencount，则此方法什么也不做。与release(对象)不同，这个方法捕获了一个由referencecoun.release()引起的异常，并将其记录下来，而不是将其重新发送给调用者。通常建议使用release(Object)代替，除非您绝对需要接受异常。
     */
    public static void safeRelease(Object msg) {
        try {
            release(msg);
        } catch (Throwable t) {
            logger.warn("Failed to release a message: {}", msg, t);
        }
    }

    /**
     * Try to call {@link ReferenceCounted#release(int)} if the specified message implements {@link ReferenceCounted}.
     * If the specified message doesn't implement {@link ReferenceCounted}, this method does nothing.
     * Unlike {@link #release(Object)} this method catches an exception raised by {@link ReferenceCounted#release(int)}
     * and logs it, rather than rethrowing it to the caller.  It is usually recommended to use
     * {@link #release(Object, int)} instead, unless you absolutely need to swallow an exception.
     * 如果指定的消息实现了referencecoun.com .release(int)。如果指定的消息没有实现referencecount，则该方法不会执行任何操作。与release(Object)不同的是，该方法捕获了referencecoung .release(int)引发的异常，并对其进行日志记录，而不是将其重新提交给调用者。通常建议使用release(Object, int)代替，除非您绝对需要吞下一个异常。
     */
    public static void safeRelease(Object msg, int decrement) {
        try {
            release(msg, decrement);
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failed to release a message: {} (decrement: {})", msg, decrement, t);
            }
        }
    }

    /**
     * Schedules the specified object to be released when the caller thread terminates. Note that this operation is
     * intended to simplify reference counting of ephemeral objects during unit tests. Do not use it beyond the
     * intended use case.在调用线程终止时调度要释放的指定对象。注意，此操作旨在简化单元测试期间短暂对象的引用计数。不要超出预期的用例使用它。
     *
     * @deprecated this may introduce a lot of memory usage so it is generally preferable to manually release objects.
     */
    @Deprecated
    public static <T> T releaseLater(T msg) {
        return releaseLater(msg, 1);
    }

    /**
     * Schedules the specified object to be released when the caller thread terminates. Note that this operation is
     * intended to simplify reference counting of ephemeral objects during unit tests. Do not use it beyond the
     * intended use case.在调用线程终止时调度要释放的指定对象。注意，此操作旨在简化单元测试期间短暂对象的引用计数。不要超出预期的用例使用它。
     *
     * @deprecated this may introduce a lot of memory usage so it is generally preferable to manually release objects.
     */
    @Deprecated
    public static <T> T releaseLater(T msg, int decrement) {
        if (msg instanceof ReferenceCounted) {
            ThreadDeathWatcher.watch(Thread.currentThread(), new ReleasingTask((ReferenceCounted) msg, decrement));
        }
        return msg;
    }

    /**
     * Returns reference count of a {@link ReferenceCounted} object. If object is not type of
     * {@link ReferenceCounted}, {@code -1} is returned.返回引用对象的引用计数。如果对象不是的引用类型，则返回-1。
     */
    public static int refCnt(Object msg) {
        return msg instanceof ReferenceCounted ? ((ReferenceCounted) msg).refCnt() : -1;
    }

    /**
     * Releases the objects when the thread that called {@link #releaseLater(Object)} has been terminated.当调用releaseLater(对象)的线程终止时，释放对象。
     */
    private static final class ReleasingTask implements Runnable {

        private final ReferenceCounted obj;
        private final int decrement;

        ReleasingTask(ReferenceCounted obj, int decrement) {
            this.obj = obj;
            this.decrement = decrement;
        }

        @Override
        public void run() {
            try {
                if (!obj.release(decrement)) {
                    logger.warn("Non-zero refCnt: {}", this);
                } else {
                    logger.debug("Released: {}", this);
                }
            } catch (Exception ex) {
                logger.warn("Failed to release an object: {}", obj, ex);
            }
        }

        @Override
        public String toString() {
            return StringUtil.simpleClassName(obj) + ".release(" + decrement + ") refCnt: " + obj.refCnt();
        }
    }

    private ReferenceCountUtil() { }
}
