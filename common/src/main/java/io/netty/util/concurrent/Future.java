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
package io.netty.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;


/**
 * The result of an asynchronous operation.
 */
@SuppressWarnings("ClassNameSameAsAncestorName")
public interface Future<V> extends java.util.concurrent.Future<V> {

    /**
     * Returns {@code true} if and only if the I/O operation was completed
     * successfully.当且仅当I/O操作成功完成时返回true。
     */
    boolean isSuccess();

    /**
     * returns {@code true} if and only if the operation can be cancelled via {@link #cancel(boolean)}.如果且仅当操作可以通过cancel取消时返回true(布尔)。
     */
    boolean isCancellable();

    /**
     * Returns the cause of the failed I/O operation if the I/O operation has
     * failed.如果I/O操作失败，返回导致I/O操作失败的原因。
     *
     * @return the cause of the failure.
     *         {@code null} if succeeded or this future is not
     *         completed yet.
     */
    Throwable cause();

    /**
     * Adds the specified listener to this future.  The
     * specified listener is notified when this future is
     * {@linkplain #isDone() done}.  If this future is already
     * completed, the specified listener is notified immediately.
     * 将指定的侦听器添加到此未来。指定的侦听器将在此将来完成时被通知。如果这个未来已经完成，则会立即通知指定的侦听器。
     */
    Future<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * Adds the specified listeners to this future.  The
     * specified listeners are notified when this future is
     * {@linkplain #isDone() done}.  If this future is already
     * completed, the specified listeners are notified immediately.
     * 将指定的侦听器添加到此未来。指定的侦听器将在此将来完成时被通知。如果这个未来已经完成，将立即通知指定的侦听器。
     */
    Future<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    /**
     * Removes the first occurrence of the specified listener from this future.
     * The specified listener is no longer notified when this
     * future is {@linkplain #isDone() done}.  If the specified
     * listener is not associated with this future, this method
     * does nothing and returns silently.
     * 从这个将来中删除指定侦听器的第一次出现。指定的侦听器在完成此以后不再被通知。如果指定的侦听器与此未来没有关联，则此方法不做任何操作，并以静默方式返回。
     */
    Future<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * Removes the first occurrence for each of the listeners from this future.
     * The specified listeners are no longer notified when this
     * future is {@linkplain #isDone() done}.  If the specified
     * listeners are not associated with this future, this method
     * does nothing and returns silently.
     * 从这个将来删除每个侦听器的第一个事件。指定的侦听器在此将来完成时不再被通知。如果指定的侦听器与此未来没有关联，则此方法不做任何操作，并以静默方式返回。
     */
    Future<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.等待这个未来，直到它完成，如果这个未来失败了，重新抛出失败的原因。
     */
    Future<V> sync() throws InterruptedException;

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.
     * 等待这个未来，直到它完成，如果这个未来失败了，重新抛出失败的原因。
     */
    Future<V> syncUninterruptibly();

    /**
     * Waits for this future to be completed.
     *
     * @throws InterruptedException
     *         if the current thread was interrupted
     */
    Future<V> await() throws InterruptedException;

    /**
     * Waits for this future to be completed without
     * interruption.  This method catches an {@link InterruptedException} and
     * discards it silently.等待这个未来的完成而不被打断。此方法捕获一个InterruptedException并静默丢弃它。
     */
    Future<V> awaitUninterruptibly();

    /**
     * Waits for this future to be completed within the
     * specified time limit.等待这个未来在指定的时间内完成。
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     *
     * @throws InterruptedException
     *         if the current thread was interrupted
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Waits for this future to be completed within the
     * specified time limit.等待这个未来在指定的时间内完成。
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     *
     * @throws InterruptedException
     *         if the current thread was interrupted
     */
    boolean await(long timeoutMillis) throws InterruptedException;

    /**
     * Waits for this future to be completed within the
     * specified time limit without interruption.  This method catches an
     * {@link InterruptedException} and discards it silently.等待这个未来在规定的时间内完成而不被打断。此方法捕获一个InterruptedException并静默丢弃它。
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     */
    boolean awaitUninterruptibly(long timeout, TimeUnit unit);

    /**
     * Waits for this future to be completed within the
     * specified time limit without interruption.  This method catches an
     * {@link InterruptedException} and discards it silently.等待这个未来在规定的时间内完成而不被打断。此方法捕获一个InterruptedException并静默丢弃它。
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     */
    boolean awaitUninterruptibly(long timeoutMillis);

    /**
     * Return the result without blocking. If the future is not done yet this will return {@code null}.
     *
     * As it is possible that a {@code null} value is used to mark the future as successful you also need to check
     * if the future is really done with {@link #isDone()} and not relay on the returned {@code null} value.
     * 返回结果而不阻塞。如果将来还没有完成，则返回null。由于可能使用空值来标记未来是否成功，您还需要检查未来是否真的用isDone()完成，而不是对返回的空值进行中继。
     */
    V getNow();

    /**
     * {@inheritDoc}
     *
     * If the cancellation was successful it will fail the future with an {@link CancellationException}.
     * 尝试取消此任务的执行。如果任务已经完成，已经被取消，或者由于其他原因不能取消，则此尝试将失败。如果成功，并且该任务在调用cancel时还没有启动，则该任务将永远不会运行。如果任务已经启动，则mayInterruptIfRunning参数确定执行此任务的线程是否应该被中断，以试图停止任务。
     在此方法返回之后，对isDone的后续调用将始终返回true。如果该方法返回true，则随后的被取消的调用将始终返回true。如果取消是成功的，它将失败的未来与取消例外。
     */
    @Override
    boolean cancel(boolean mayInterruptIfRunning);
}
