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
package io.netty.util;

/**
 * An attribute which allows to store a value reference. It may be updated atomically and so is thread-safe.允许存储值引用的属性。它可以自动更新，线程安全。
 *
 * @param <T>   the type of the value it holds.
 */
public interface Attribute<T> {

    /**
     * Returns the key of this attribute.
     */
    AttributeKey<T> key();

    /**
     * Returns the current value, which may be {@code null} 返回当前值，该值可能为null
     */
    T get();

    /**
     * Sets the value
     */
    void set(T value);

    /**
     *  Atomically sets to the given value and returns the old value which may be {@code null} if non was set before.原子化设置为给定的值，并返回旧值，如果之前没有设置，旧值可能为空。
     */
    T getAndSet(T value);

    /**
     *  Atomically sets to the given value if this {@link Attribute}'s value is {@code null}.
     *  If it was not possible to set the value as it contains a value it will just return the current value.如果该属性的值为空，则原子性地设置为给定值。如果无法设置值，因为它包含一个值，它将返回当前值。
     */
    T setIfAbsent(T value);

    /**
     * Removes this attribute from the {@link AttributeMap} and returns the old value. Subsequent {@link #get()}
     * calls will return {@code null}.
     *
     * If you only want to return the old value and clear the {@link Attribute} while still keep it in the
     * {@link AttributeMap} use {@link #getAndSet(Object)} with a value of {@code null}.
     *
     * <p>
     * Be aware that even if you call this method another thread that has obtained a reference to this {@link Attribute}
     * via {@link AttributeMap#attr(AttributeKey)} will still operate on the same instance. That said if now another
     * thread or even the same thread later will call {@link AttributeMap#attr(AttributeKey)} again, a new
     * {@link Attribute} instance is created and so is not the same as the previous one that was removed. Because of
     * this special caution should be taken when you call {@link #remove()} or {@link #getAndRemove()}.
     *
     * @deprecated please consider using {@link #getAndSet(Object)} (with value of {@code null}).
     * 从AttributeMap中删除此属性并返回旧值。随后的get()调用将返回null。如果您只想返回旧值并清除属性，同时仍然将其保留在AttributeMap中，请使用值为null的getAndSet(Object)方法。
    请注意，即使您调用此方法，另一个通过AttributeMap.attr(AttributeKey)获得对该属性引用的线程仍然会对同一个实例进行操作。也就是说，如果现在有另一个线程或者甚至是同一个线程以后会再次调用AttributeMap.attr(AttributeKey)，那么就会创建一个新的属性实例，因此与之前删除的属性实例不同。由于这一点，在调用remove()或getAndRemove()时应该特别小心。
     */
    @Deprecated
    T getAndRemove();

    /**
     * Atomically sets the value to the given updated value if the current value == the expected value.
     * If it the set was successful it returns {@code true} otherwise {@code false}.如果当前值==预期值，则原子性地将值设置为给定的更新值。如果集合成功，则返回true，否则返回false。
     */
    boolean compareAndSet(T oldValue, T newValue);

    /**
     * Removes this attribute from the {@link AttributeMap}. Subsequent {@link #get()} calls will return @{code null}.
     *
     * If you only want to remove the value and clear the {@link Attribute} while still keep it in
     * {@link AttributeMap} use {@link #set(Object)} with a value of {@code null}.
     *
     * <p>
     * Be aware that even if you call this method another thread that has obtained a reference to this {@link Attribute}
     * via {@link AttributeMap#attr(AttributeKey)} will still operate on the same instance. That said if now another
     * thread or even the same thread later will call {@link AttributeMap#attr(AttributeKey)} again, a new
     * {@link Attribute} instance is created and so is not the same as the previous one that was removed. Because of
     * this special caution should be taken when you call {@link #remove()} or {@link #getAndRemove()}.
     *
     * @deprecated please consider using {@link #set(Object)} (with value of {@code null}).
     *
    从AttributeMap中删除此属性。随后的get()调用将返回@{code null}。如果您只希望删除值并清除属性，而仍然将其保留在AttributeMap中，那么使用值为null的set(对象)。
    请注意，即使您调用此方法，另一个通过AttributeMap.attr(AttributeKey)获得对该属性引用的线程仍然会对同一个实例进行操作。也就是说，如果现在有另一个线程或者甚至是同一个线程以后会再次调用AttributeMap.attr(AttributeKey)，那么就会创建一个新的属性实例，因此与之前删除的属性实例不同。由于这一点，在调用remove()或getAndRemove()时应该特别小心。
     */
    @Deprecated
    void remove();
}
