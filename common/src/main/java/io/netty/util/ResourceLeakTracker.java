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
package io.netty.util;

public interface ResourceLeakTracker<T>  {

    /**
     * Records the caller's current stack trace so that the {@link ResourceLeakDetector} can tell where the leaked
     * resource was accessed lastly. This method is a shortcut to {@link #record(Object) record(null)}.记录调用者当前的堆栈跟踪，以便ResourceLeakDetector能够知道最后访问泄漏资源的位置。此方法是记录(null)的快捷方式。
     */
    void record();

    /**
     * Records the caller's current stack trace and the specified additional arbitrary information
     * so that the {@link ResourceLeakDetector} can tell where the leaked resource was accessed lastly.记录调用者的当前堆栈跟踪和指定的附加任意信息，以便ResourceLeakDetector能够知道最后访问泄漏资源的位置。
     */
    void record(Object hint);

    /**
     * Close the leak so that {@link ResourceLeakTracker} does not warn about leaked resources.
     * After this method is called a leak associated with this ResourceLeakTracker should not be reported.
     *
     * @return {@code true} if called first time, {@code false} if called already
     * 关闭泄漏，这样资源跟踪器就不会对泄漏的资源发出警告。在此方法被称为与资源跟踪器相关的泄漏之后，不应报告。
     */
    boolean close(T trackedObject);
}
