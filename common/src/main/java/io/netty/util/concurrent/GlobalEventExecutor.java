/*
 * Copyright 2012 The Netty Project
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

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Single-thread singleton {@link EventExecutor}.  It starts the thread automatically and stops it when there is no
 * task pending in the task queue for 1 second.  Please note it is not scalable to schedule large number of tasks to
 * this executor; use a dedicated executor.
 * 单线程单例EventExecutor。它自动启动线程，并在任务队列中1秒内没有任务挂起时停止线程。请注意，将大量任务调度给该执行程序是不可伸缩的;使用一个专用的遗嘱执行人。
 */
public final class GlobalEventExecutor extends AbstractScheduledEventExecutor {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(GlobalEventExecutor.class);

    private static final long SCHEDULE_QUIET_PERIOD_INTERVAL = TimeUnit.SECONDS.toNanos(1);

//    饿汉式单例模式
    public static final GlobalEventExecutor INSTANCE = new GlobalEventExecutor();

    final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>();
    final ScheduledFutureTask<Void> quietPeriodTask = new ScheduledFutureTask<Void>(
            this, Executors.<Void>callable(new Runnable() {
        @Override
        public void run() {
            // NOOP
        }
    }, null), ScheduledFutureTask.deadlineNanos(SCHEDULE_QUIET_PERIOD_INTERVAL), -SCHEDULE_QUIET_PERIOD_INTERVAL);

    // because the GlobalEventExecutor is a singleton, tasks submitted to it can come from arbitrary threads and this
    // can trigger the creation of a thread from arbitrary thread groups; for this reason, the thread factory must not
    // be sticky about its thread group
    // visible for testing
    //因为GlobalEventExecutor是单例的，提交给它的任务可以来自任意线程
//可以触发从任意线程组创建线程;由于这个原因，线程工厂不能。
//粘着它的线程组
//用于测试可见
    final ThreadFactory threadFactory =
            new DefaultThreadFactory(DefaultThreadFactory.toPoolName(getClass()), false, Thread.NORM_PRIORITY, null);
    private final TaskRunner taskRunner = new TaskRunner();
    private final AtomicBoolean started = new AtomicBoolean();
    volatile Thread thread;

    private final Future<?> terminationFuture = new FailedFuture<Object>(this, new UnsupportedOperationException());

    private GlobalEventExecutor() {
        scheduledTaskQueue().add(quietPeriodTask);
    }

    /**
     * Take the next {@link Runnable} from the task queue and so will block if no task is currently present.将下一个可运行的对象从任务队列中取出，如果当前没有任务，那么so将阻塞。
     *
     * @return {@code null} if the executor thread has been interrupted or waken up.
     */
    Runnable takeTask() {
        BlockingQueue<Runnable> taskQueue = this.taskQueue;
        for (;;) {
//            从队列中查询任务
            ScheduledFutureTask<?> scheduledTask = peekScheduledTask();
            if (scheduledTask == null) {
                Runnable task = null;
                try {
//                    如果从队列中没找到任务就阻塞
                    task = taskQueue.take();
                } catch (InterruptedException e) {
                    // Ignore
                }
                return task;
            } else {
                long delayNanos = scheduledTask.delayNanos();
                Runnable task;
                if (delayNanos > 0) {
                    try {
                        task = taskQueue.poll(delayNanos, TimeUnit.NANOSECONDS);
                    } catch (InterruptedException e) {
                        // Waken up.
                        return null;
                    }
                } else {
                    task = taskQueue.poll();
                }

                if (task == null) {
                    fetchFromScheduledTaskQueue();
                    task = taskQueue.poll();
                }

                if (task != null) {
                    return task;
                }
            }
        }
    }

    private void fetchFromScheduledTaskQueue() {
        long nanoTime = AbstractScheduledEventExecutor.nanoTime();
        Runnable scheduledTask = pollScheduledTask(nanoTime);
        while (scheduledTask != null) {
            taskQueue.add(scheduledTask);
            scheduledTask = pollScheduledTask(nanoTime);
        }
    }

    /**
     * Return the number of tasks that are pending for processing.
     *
     * <strong>Be aware that this operation may be expensive as it depends on the internal implementation of the
     * SingleThreadEventExecutor. So use it was care!</strong>
     * 返回待处理的任务数量。请注意，这个操作可能比较昂贵，因为它取决于SingleThreadEventExecutor的内部实现。所以好好利用它吧!
     */
    public int pendingTasks() {
        return taskQueue.size();
    }

    /**
     * Add a task to the task queue, or throws a {@link RejectedExecutionException} if this instance was shutdown
     * before.向任务队列添加任务，或者在此实例之前关闭时抛出RejectedExecutionException。
     */
    private void addTask(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        taskQueue.add(task);
    }

    @Override
    public boolean inEventLoop(Thread thread) {
        return thread == this.thread;
    }

    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        return terminationFuture();
    }

    @Override
    public Future<?> terminationFuture() {
        return terminationFuture;
    }

    @Override
    @Deprecated
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShuttingDown() {
        return false;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return false;
    }

    /**
     * Waits until the worker thread of this executor has no tasks left in its task queue and terminates itself.
     * Because a new worker thread will be started again when a new task is submitted, this operation is only useful
     * when you want to ensure that the worker thread is terminated <strong>after</strong> your application is shut
     * down and there's no chance of submitting a new task afterwards.
     *
     * @return {@code true} if and only if the worker thread has been terminated
     * 等待，直到此执行程序的工作线程在其任务队列中没有剩余任务并终止自己。因为在提交新任务时将再次启动一个新的worker线程，所以只有当您希望确保在应用程序关闭后终止worker线程，并且之后没有提交新任务的机会时，这个操作才有用。
     */
    public boolean awaitInactivity(long timeout, TimeUnit unit) throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException("unit");
        }

        final Thread thread = this.thread;
        if (thread == null) {
            throw new IllegalStateException("thread was not started");
        }
//        当前线程等待其他线程执行完毕后在执行
        thread.join(unit.toMillis(timeout));
        return !thread.isAlive();
    }

    @Override
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }

        addTask(task);
        if (!inEventLoop()) {
            startThread();
        }
    }

    private void startThread() {
        if (started.compareAndSet(false, true)) {
            final Thread t = threadFactory.newThread(taskRunner);
            // Set to null to ensure we not create classloader leaks by holds a strong reference to the inherited
            // classloader.
            //设置为null，以确保不通过保存对继承的强引用来创建classloader漏洞
//类加载器。
            // See:
            // - https://github.com/netty/netty/issues/7290
            // - https://bugs.openjdk.java.net/browse/JDK-7008595
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    t.setContextClassLoader(null);
                    return null;
                }
            });

            // Set the thread before starting it as otherwise inEventLoop() may return false and so produce
            // an assert error.
            //在启动线程之前设置线程，否则，尼尼微循环()可能返回false，从而产生
//一个断言错误。
            // See https://github.com/netty/netty/issues/4357
            thread = t;
            t.start();
        }
    }

    final class TaskRunner implements Runnable {
        @Override
        public void run() {
            for (;;) {
//                从队列中查询任务
                Runnable task = takeTask();
                if (task != null) {
                    try {
                        task.run();
                    } catch (Throwable t) {
                        logger.warn("Unexpected exception from the global event executor: ", t);
                    }

                    if (task != quietPeriodTask) {
                        continue;
                    }
                }

                Queue<ScheduledFutureTask<?>> scheduledTaskQueue = GlobalEventExecutor.this.scheduledTaskQueue;
                // Terminate if there is no task in the queue (except the noop task).如果队列中没有任务(noop任务除外)，则终止。
                if (taskQueue.isEmpty() && (scheduledTaskQueue == null || scheduledTaskQueue.size() == 1)) {
                    // Mark the current thread as stopped.
                    // The following CAS must always success and must be uncontended,
                    // because only one thread should be running at the same time.
                    //将当前线程标记为已停止。
//以下的核证机关必须永远成功，必须不受竞争，
//因为同一时间只有一个线程在运行。
//                    自旋锁用做线程开关
                    boolean stopped = started.compareAndSet(true, false);
                    assert stopped;

                    // Check if there are pending entries added by execute() or schedule*() while we do CAS above.检查是否有执行()或schedule*()添加的未决条目，而我们在上面执行CAS。
                    if (taskQueue.isEmpty() && (scheduledTaskQueue == null || scheduledTaskQueue.size() == 1)) {
                        // A) No new task was added and thus there's nothing to handle
                        //    -> safe to terminate because there's nothing left to do
                        // B) A new thread started and handled all the new tasks.
                        //    -> safe to terminate the new thread will take care the rest
                        // A)没有添加新任务，因此没有什么要处理的
// ->可以安全终止，因为没有其他事情可做
// B)一个新的线程开始并处理所有的新任务。
// ->安全终止新线程将负责其余部分
                        break;
                    }

                    // There are pending tasks added again.
                    // 还有待完成的任务。
                    if (!started.compareAndSet(false, true)) {
                        // startThread() started a new thread and set 'started' to true.
                        // -> terminate this thread so that the new thread reads from taskQueue exclusively.
                        // startThread()启动一个新线程并将“started”设置为true。
// ->终止此线程，以便新线程只能从taskQueue读取。
                        break;
                    }

                    // New tasks were added, but this worker was faster to set 'started' to true.
                    // i.e. a new worker thread was not started by startThread().
                    // -> keep this thread alive to handle the newly added entries.
                    //新任务增加了，但是这个工人更快地将“start”设置为true。
//例如，一个新的工作线程不是由startThread()启动的。
// ->使这个线程保持活动状态，以处理新添加的条目。
                }
            }
        }
    }
}
