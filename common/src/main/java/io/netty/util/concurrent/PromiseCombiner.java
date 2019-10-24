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
package io.netty.util.concurrent;

import io.netty.util.internal.ObjectUtil;

/**
 * <p>A promise combiner monitors the outcome of a number of discrete futures, then notifies a final, aggregate promise
 * when all of the combined futures are finished. The aggregate promise will succeed if and only if all of the combined
 * futures succeed. If any of the combined futures fail, the aggregate promise will fail. The cause failure for the
 * aggregate promise will be the failure for one of the failed combined futures; if more than one of the combined
 * futures fails, exactly which cause of failure will be assigned to the aggregate promise is undefined.</p>
 *
 * <p>Callers may populate a promise combiner with any number of futures to be combined via the
 * {@link PromiseCombiner#add(Future)} and {@link PromiseCombiner#addAll(Future[])} methods. When all futures to be
 * combined have been added, callers must provide an aggregate promise to be notified when all combined promises have
 * finished via the {@link PromiseCombiner#finish(Promise)} method.</p>
 *
 “承诺组合”监控一系列离散期货的结果，然后在所有的组合期货交易完成后通知最终的总承诺。如果且仅当所有的联合期货都成功时，总体承诺才会成功。如果任何一个联合期货都失败了，总的承诺也会失败。导致总体承诺失败的原因将是其中一个失败的联合期货的失败;如果不止一个联合期货失效，那么到底是哪一个失效原因将被分配给总承诺是未知的。
 调用者可以通过add(Future)和addAll(Future[])方法将任意数量的期货组合到一个promise combiner中。当所有要合并的期货都被添加时，调用者必须提供一个汇总承诺，当所有合并的承诺通过finish(promise)方法完成时，通知调用者。
 */
public final class PromiseCombiner {
    private int expectedCount;
    private int doneCount;
    private boolean doneAdding;
    private Promise<Void> aggregatePromise;
    private Throwable cause;
    private final GenericFutureListener<Future<?>> listener = new GenericFutureListener<Future<?>>() {
        @Override
        public void operationComplete(Future<?> future) throws Exception {
            ++doneCount;
            if (!future.isSuccess() && cause == null) {
                cause = future.cause();
            }
            if (doneCount == expectedCount && doneAdding) {
                tryPromise();
            }
        }
    };

    /**
     * Adds a new promise to be combined. New promises may be added until an aggregate promise is added via the
     * {@link PromiseCombiner#finish(Promise)} method.
     *
     * @param promise the promise to add to this promise combiner
     *
     * @deprecated Replaced by {@link PromiseCombiner#add(Future)}.
     * 增加了一个新的承诺。在通过finish(promise)方法添加总体承诺之前，可以添加新的承诺。
     */
    @Deprecated
    public void add(Promise promise) {
        add((Future) promise);
    }

    /**
     * Adds a new future to be combined. New futures may be added until an aggregate promise is added via the
     * {@link PromiseCombiner#finish(Promise)} method.
     *
     * @param future the future to add to this promise combiner
     *               增加了一个新的未来。新的期货可以被增加，直到一个总的承诺通过完成(承诺)方法被增加。
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void add(Future future) {
        checkAddAllowed();
        ++expectedCount;
        future.addListener(listener);
    }

    /**
     * Adds new promises to be combined. New promises may be added until an aggregate promise is added via the
     * {@link PromiseCombiner#finish(Promise)} method.
     *
     * @param promises the promises to add to this promise combiner
     *
     * @deprecated Replaced by {@link PromiseCombiner#addAll(Future[])}
     */
    @Deprecated
    public void addAll(Promise... promises) {
        addAll((Future[]) promises);
    }

    /**
     * Adds new futures to be combined. New futures may be added until an aggregate promise is added via the
     * {@link PromiseCombiner#finish(Promise)} method.
     *
     * @param futures the futures to add to this promise combiner
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addAll(Future... futures) {
        for (Future future : futures) {
            this.add(future);
        }
    }

    /**
     * <p>Sets the promise to be notified when all combined futures have finished. If all combined futures succeed,
     * then the aggregate promise will succeed. If one or more combined futures fails, then the aggregate promise will
     * fail with the cause of one of the failed futures. If more than one combined future fails, then exactly which
     * failure will be assigned to the aggregate promise is undefined.</p>
     *
     * <p>After this method is called, no more futures may be added via the {@link PromiseCombiner#add(Future)} or
     * {@link PromiseCombiner#addAll(Future[])} methods.</p>
     *
     * @param aggregatePromise the promise to notify when all combined futures have finished
     *                         设定所有组合期货交易完成后的通知。如果所有联合期货都成功，那么总的承诺将成功。如果一个或多个联合期货失败，那么总的承诺将会因为其中一个失败期货的原因而失败。如果不止一个组合的未来失败，那么将分配给聚合承诺的具体失败是不确定的。
    在调用此方法之后，不再可以通过add(Future)或addAll(Future)方法添加未来。
     */
    public void finish(Promise<Void> aggregatePromise) {
        if (doneAdding) {
            throw new IllegalStateException("Already finished");
        }
        doneAdding = true;
        this.aggregatePromise = ObjectUtil.checkNotNull(aggregatePromise, "aggregatePromise");
        if (doneCount == expectedCount) {
            tryPromise();
        }
    }

    private boolean tryPromise() {
        return (cause == null) ? aggregatePromise.trySuccess(null) : aggregatePromise.tryFailure(cause);
    }

    private void checkAddAllowed() {
        if (doneAdding) {
            throw new IllegalStateException("Adding promises is not allowed after finished adding");
        }
    }
}
