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
package io.netty.util.internal;

/**
 * A Utility to Call the {@link System#load(String)} or {@link System#loadLibrary(String)}.
 * Because the {@link System#load(String)} and {@link System#loadLibrary(String)} are both
 * CallerSensitive, it will load the native library into its caller's {@link ClassLoader}.
 * In OSGi environment, we need this helper to delegate the calling to {@link System#load(String)}
 * and it should be as simple as possible. It will be injected into the native library's
 * ClassLoader when it is undefined. And therefore, when the defined new helper is invoked,
 * the native library would be loaded into the native library's ClassLoader, not the
 * caller's ClassLoader.
 * 调用System.load(字符串)或System.loadLibrary(字符串)的实用程序。因为System.load(String)和System.loadLibrary(String)都是callersensitivity，所以它会将本机库加载到其调用者的类加载器中。
 * 在OSGi环境中，我们需要这个helper来将调用委托给System.load(String)，并且它应该尽可能简单。当未定义类装入器时，它将被注入本机库的类装入器。因此，当调用已定义的新助手时，本机库将被加载到本机库的类加载器中，而不是加载程序的类加载器中。
 */
final class NativeLibraryUtil {
    /**
     * Delegate the calling to {@link System#load(String)} or {@link System#loadLibrary(String)}.
     * @param libName - The native library path or name
     * @param absolute - Whether the native library will be loaded by path or by name
     */
    public static void loadLibrary(String libName, boolean absolute) {
        if (absolute) {
            System.load(libName);
        } else {
            System.loadLibrary(libName);
        }
    }

    private NativeLibraryUtil() {
        // Utility
    }
}
