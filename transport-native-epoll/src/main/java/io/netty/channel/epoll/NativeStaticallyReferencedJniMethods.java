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
package io.netty.channel.epoll;

/**
 * This class is necessary to break the following cyclic dependency:
 * <ol>
 * <li>JNI_OnLoad</li>
 * <li>JNI Calls FindClass because RegisterNatives (used to register JNI methods) requires a class</li>
 * <li>FindClass loads the class, but static members variables of that class attempt to call a JNI method which has not
 * yet been registered.</li>
 * <li>java.lang.UnsatisfiedLinkError is thrown because native method has not yet been registered.</li>
 * </ol>
 * Static members which call JNI methods must not be declared in this class!
 * 这个类对于打破以下循环依赖关系是必要的:
 JNI_OnLoad
 JNI调用FindClass是因为registernative(用于注册JNI方法)需要一个类
 FindClass加载该类，但是该类的静态成员变量试图调用尚未注册的JNI方法。
 . lang。因为本机方法尚未注册，所以抛出UnsatisfiedLinkError。
 调用JNI方法的静态成员不能在这个类中声明!
 */
final class NativeStaticallyReferencedJniMethods {

    private NativeStaticallyReferencedJniMethods() { }

    static native int epollin();
    static native int epollout();
    static native int epollrdhup();
    static native int epollet();
    static native int epollerr();
    static native long ssizeMax();
    static native int tcpMd5SigMaxKeyLen();
    static native int iovMax();
    static native int uioMaxIov();
    static native boolean isSupportingSendmmsg();
    static native boolean isSupportingTcpFastopen();
    static native String kernelVersion();
}
