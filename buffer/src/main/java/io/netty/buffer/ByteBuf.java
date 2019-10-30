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
package io.netty.buffer;

import io.netty.util.ByteProcessor;
import io.netty.util.ReferenceCounted;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

/**
 * A random and sequential accessible sequence of zero or more bytes (octets).
 * This interface provides an abstract view for one or more primitive byte
 * arrays ({@code byte[]}) and {@linkplain ByteBuffer NIO buffers}.
 *
 * <h3>Creation of a buffer</h3>
 *
 * It is recommended to create a new buffer using the helper methods in
 * {@link Unpooled} rather than calling an individual implementation's
 * constructor.
 *
 * <h3>Random Access Indexing</h3>
 *
 * Just like an ordinary primitive byte array, {@link ByteBuf} uses
 * <a href="http://en.wikipedia.org/wiki/Zero-based_numbering">zero-based indexing</a>.
 * It means the index of the first byte is always {@code 0} and the index of the last byte is
 * always {@link #capacity() capacity - 1}.  For example, to iterate all bytes of a buffer, you
 * can do the following, regardless of its internal implementation:
 *
 * <pre>
 * {@link ByteBuf} buffer = ...;
 * for (int i = 0; i &lt; buffer.capacity(); i ++) {
 *     byte b = buffer.getByte(i);
 *     System.out.println((char) b);
 * }
 * </pre>
 *
 * <h3>Sequential Access Indexing</h3>
 *
 * {@link ByteBuf} provides two pointer variables to support sequential
 * read and write operations - {@link #readerIndex() readerIndex} for a read
 * operation and {@link #writerIndex() writerIndex} for a write operation
 * respectively.  The following diagram shows how a buffer is segmented into
 * three areas by the two pointers:
 *
 * <pre>
 *      +-------------------+------------------+------------------+
 *      | discardable bytes |  readable bytes  |  writable bytes  |
 *      |                   |     (CONTENT)    |                  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 * </pre>
 *
 * <h4>Readable bytes (the actual content)</h4>
 *
 * This segment is where the actual data is stored.  Any operation whose name
 * starts with {@code read} or {@code skip} will get or skip the data at the
 * current {@link #readerIndex() readerIndex} and increase it by the number of
 * read bytes.  If the argument of the read operation is also a
 * {@link ByteBuf} and no destination index is specified, the specified
 * buffer's {@link #writerIndex() writerIndex} is increased together.
 * <p>
 * If there's not enough content left, {@link IndexOutOfBoundsException} is
 * raised.  The default value of newly allocated, wrapped or copied buffer's
 * {@link #readerIndex() readerIndex} is {@code 0}.
 *
 * <pre>
 * // Iterates the readable bytes of a buffer.
 * {@link ByteBuf} buffer = ...;
 * while (buffer.isReadable()) {
 *     System.out.println(buffer.readByte());
 * }
 * </pre>
 *
 * <h4>Writable bytes</h4>
 *
 * This segment is a undefined space which needs to be filled.  Any operation
 * whose name starts with {@code write} will write the data at the current
 * {@link #writerIndex() writerIndex} and increase it by the number of written
 * bytes.  If the argument of the write operation is also a {@link ByteBuf},
 * and no source index is specified, the specified buffer's
 * {@link #readerIndex() readerIndex} is increased together.
 * <p>
 * If there's not enough writable bytes left, {@link IndexOutOfBoundsException}
 * is raised.  The default value of newly allocated buffer's
 * {@link #writerIndex() writerIndex} is {@code 0}.  The default value of
 * wrapped or copied buffer's {@link #writerIndex() writerIndex} is the
 * {@link #capacity() capacity} of the buffer.
 *
 * <pre>
 * // Fills the writable bytes of a buffer with random integers.
 * {@link ByteBuf} buffer = ...;
 * while (buffer.maxWritableBytes() >= 4) {
 *     buffer.writeInt(random.nextInt());
 * }
 * </pre>
 *
 * <h4>Discardable bytes</h4>
 *
 * This segment contains the bytes which were read already by a read operation.
 * Initially, the size of this segment is {@code 0}, but its size increases up
 * to the {@link #writerIndex() writerIndex} as read operations are executed.
 * The read bytes can be discarded by calling {@link #discardReadBytes()} to
 * reclaim unused area as depicted by the following diagram:
 *
 * <pre>
 *  BEFORE discardReadBytes()
 *
 *      +-------------------+------------------+------------------+
 *      | discardable bytes |  readable bytes  |  writable bytes  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 *
 *
 *  AFTER discardReadBytes()
 *
 *      +------------------+--------------------------------------+
 *      |  readable bytes  |    writable bytes (got more space)   |
 *      +------------------+--------------------------------------+
 *      |                  |                                      |
 * readerIndex (0) <= writerIndex (decreased)        <=        capacity
 * </pre>
 *
 * Please note that there is no guarantee about the content of writable bytes
 * after calling {@link #discardReadBytes()}.  The writable bytes will not be
 * moved in most cases and could even be filled with completely different data
 * depending on the underlying buffer implementation.
 *
 * <h4>Clearing the buffer indexes</h4>
 *
 * You can set both {@link #readerIndex() readerIndex} and
 * {@link #writerIndex() writerIndex} to {@code 0} by calling {@link #clear()}.
 * It does not clear the buffer content (e.g. filling with {@code 0}) but just
 * clears the two pointers.  Please also note that the semantic of this
 * operation is different from {@link ByteBuffer#clear()}.
 *
 * <pre>
 *  BEFORE clear()
 *
 *      +-------------------+------------------+------------------+
 *      | discardable bytes |  readable bytes  |  writable bytes  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 *
 *
 *  AFTER clear()
 *
 *      +---------------------------------------------------------+
 *      |             writable bytes (got more space)             |
 *      +---------------------------------------------------------+
 *      |                                                         |
 *      0 = readerIndex = writerIndex            <=            capacity
 * </pre>
 *
 * <h3>Search operations</h3>
 *
 * For simple single-byte searches, use {@link #indexOf(int, int, byte)} and {@link #bytesBefore(int, int, byte)}.
 * {@link #bytesBefore(byte)} is especially useful when you deal with a {@code NUL}-terminated string.
 * For complicated searches, use {@link #forEachByte(int, int, ByteProcessor)} with a {@link ByteProcessor}
 * implementation.
 *
 * <h3>Mark and reset</h3>
 *
 * There are two marker indexes in every buffer. One is for storing
 * {@link #readerIndex() readerIndex} and the other is for storing
 * {@link #writerIndex() writerIndex}.  You can always reposition one of the
 * two indexes by calling a reset method.  It works in a similar fashion to
 * the mark and reset methods in {@link InputStream} except that there's no
 * {@code readlimit}.
 *
 * <h3>Derived buffers</h3>
 *
 * You can create a view of an existing buffer by calling one of the following methods:
 * <ul>
 *   <li>{@link #duplicate()}</li>
 *   <li>{@link #slice()}</li>
 *   <li>{@link #slice(int, int)}</li>
 *   <li>{@link #readSlice(int)}</li>
 *   <li>{@link #retainedDuplicate()}</li>
 *   <li>{@link #retainedSlice()}</li>
 *   <li>{@link #retainedSlice(int, int)}</li>
 *   <li>{@link #readRetainedSlice(int)}</li>
 * </ul>
 * A derived buffer will have an independent {@link #readerIndex() readerIndex},
 * {@link #writerIndex() writerIndex} and marker indexes, while it shares
 * other internal data representation, just like a NIO buffer does.
 * <p>
 * In case a completely fresh copy of an existing buffer is required, please
 * call {@link #copy()} method instead.
 *
 * <h4>Non-retained and retained derived buffers</h4>
 *
 * Note that the {@link #duplicate()}, {@link #slice()}, {@link #slice(int, int)} and {@link #readSlice(int)} does NOT
 * call {@link #retain()} on the returned derived buffer, and thus its reference count will NOT be increased. If you
 * need to create a derived buffer with increased reference count, consider using {@link #retainedDuplicate()},
 * {@link #retainedSlice()}, {@link #retainedSlice(int, int)} and {@link #readRetainedSlice(int)} which may return
 * a buffer implementation that produces less garbage.
 *
 * <h3>Conversion to existing JDK types</h3>
 *
 * <h4>Byte array</h4>
 *
 * If a {@link ByteBuf} is backed by a byte array (i.e. {@code byte[]}),
 * you can access it directly via the {@link #array()} method.  To determine
 * if a buffer is backed by a byte array, {@link #hasArray()} should be used.
 *
 * <h4>NIO Buffers</h4>
 *
 * If a {@link ByteBuf} can be converted into an NIO {@link ByteBuffer} which shares its
 * content (i.e. view buffer), you can get it via the {@link #nioBuffer()} method.  To determine
 * if a buffer can be converted into an NIO buffer, use {@link #nioBufferCount()}.
 *
 * <h4>Strings</h4>
 *
 * Various {@link #toString(Charset)} methods convert a {@link ByteBuf}
 * into a {@link String}.  Please note that {@link #toString()} is not a
 * conversion method.
 *
 * <h4>I/O Streams</h4>
 *
 * Please refer to {@link ByteBufInputStream} and
 * {@link ByteBufOutputStream}.
 * 零字节或更多字节的随机和连续可访问序列。此接口为一个或多个基本字节数组(byte[])和NIO缓冲区提供抽象视图。
 建立一个缓冲区
 建议使用Unpooled中的helper方法创建一个新的缓冲区，而不是调用单个实现的构造函数。
 随机访问索引
 与普通的原始字节数组一样，ByteBuf使用基于零的索引。它意味着第一个字节的索引总是0，最后一个字节的索引总是- 1。例如，要迭代缓冲区的所有字节，可以执行以下操作，而不考虑其内部实现:
 索引顺序存取
 ByteBuf提供了两个指针变量来支持顺序读和写操作——读操作的readerIndex和写操作的writerIndex。下图显示了如何通过两个指针将缓冲区分割成三个区域:
 可读字节(实际内容)
 这个段是存储实际数据的地方。任何名称以read或skip开头的操作都将获取或跳过当前readerIndex上的数据，并增加读字节数。如果读操作的参数也是ByteBuf，并且没有指定目标索引，那么将一起增加指定缓冲区的writerIndex。
 如果没有足够的内容，则会引发IndexOutOfBoundsException。新分配、包装或复制缓冲区的readerIndex的默认值为0。
 可写的字节数
 这个段是一个未定义的空间，需要填充。任何名称以write开头的操作都将在当前writerIndex上写入数据，并增加写入的字节数。如果写操作的参数也是ByteBuf，并且没有指定源索引，那么将一起增加指定缓冲区的readerIndex。
 如果没有足够的可写字节，就会提高IndexOutOfBoundsException。新分配缓冲区的writerIndex的默认值是0。包或复制缓冲区的writerIndex的默认值是缓冲区的容量。
 可废弃的字节
 此段包含已被读操作读取的字节。最初，这个段的大小是0，但是当执行读操作时，它的大小增加到writerIndex。可以通过调用discardReadBytes()来回收如下图所示的未使用区域来丢弃读取的字节:

 请注意，在调用discardReadBytes()之后，不能保证可写字节的内容。可写字节在大多数情况下不会移动，甚至可以根据底层缓冲区实现使用完全不同的数据填充。
 清除缓冲区索引
 您可以通过调用clear()将readerIndex和writerIndex都设置为0。它不清除缓冲区内容(例如用0填充)，但只清除两个指针。还请注意，此操作的语义与ByteBuffer.clear()不同。
 搜索操作
 对于简单的单字节搜索，使用indexOf(int, int, byte)和bytesBefore(int, int, byte)。bytesBefore(byte)在处理以null结尾的字符串时特别有用。对于复杂的搜索，使用forEachByte(int, int, ByteProcessor)和ByteProcessor实现。
 马克和重置
 每个缓冲区中有两个标记索引。一个用于存储readerIndex，另一个用于存储writerIndex。您可以通过调用reset方法来重新定位这两个索引中的一个。它与InputStream中的标记和重置方法类似，只是没有重新设置。
 派生的缓冲区
 您可以通过调用以下方法之一来创建现有缓冲区的视图:
 复制()
 片()
 片(int,int)
 readSlice(int)
 retainedDuplicate()
 retainedSlice()
 retainedSlice(int,int)
 readRetainedSlice(int)
 派生缓冲区将具有独立的readerIndex、writerIndex和marker索引，而与NIO缓冲区一样，它共享其他内部数据表示。
 如果需要一个完整的现有缓冲区的副本，请调用copy()方法。
 非保留和保留派生缓冲区
 注意，在返回的派生缓冲区中，duplicate()、slice()、slice(int、int)和readSlice(readSlice)不调用retain()，因此它的引用计数不会增加。如果需要创建具有增加引用计数的派生缓冲区，请考虑使用retainedDuplicate()、retainedSlice()、retainedSlice(int, int)和readRetainedSlice(int)，它们可能返回一个产生较少垃圾的缓冲区实现。
 转换到现有JDK类型
 字节数组
 如果ByteBuf由字节数组(即byte[])支持，您可以通过array()方法直接访问它。要确定缓冲区是否由字节数组支持，应该使用hasArray()。
 NIO缓冲区
 如果ByteBuf可以转换为NIO ByteBuffer，它共享其内容(即视图缓冲区)，您可以通过nioBuffer()方法获得它。要确定缓冲区是否可以转换为NIO缓冲区，请使用nioBufferCount()。
 字符串
 各种toString(Charset)方法将ByteBuf转换为字符串。请注意toString()不是一个转换方法。
 I / O流
 请参阅ByteBufInputStream和ByteBufOutputStream
 */
//
@SuppressWarnings("ClassMayBeInterface")
public abstract class ByteBuf implements ReferenceCounted, Comparable<ByteBuf> {

    /**
     * Returns the number of bytes (octets) this buffer can contain.返回该缓冲区可以包含的字节数。
     */
    public abstract int capacity();

    /**
     * Adjusts the capacity of this buffer.  If the {@code newCapacity} is less than the current
     * capacity, the content of this buffer is truncated.  If the {@code newCapacity} is greater
     * than the current capacity, the buffer is appended with unspecified data whose length is
     * {@code (newCapacity - currentCapacity)}.调整缓冲区的容量。如果新容量小于当前容量，则此缓冲区的内容将被截断。如果新容量大于当前容量，则在缓冲区中添加长度为(newCapacity - currentCapacity)的未指定数据。
     */
    public abstract ByteBuf capacity(int newCapacity);

    /**
     * Returns the maximum allowed capacity of this buffer.  If a user attempts to increase the
     * capacity of this buffer beyond the maximum capacity using {@link #capacity(int)} or
     * {@link #ensureWritable(int)}, those methods will raise an
     * {@link IllegalArgumentException}.返回此缓冲区的最大允许容量。如果用户试图通过使用capacity(int)或ensureWritable(int)来增加这个缓冲区的容量，那么这些方法将引发一个IllegalArgumentException。
     */
    public abstract int maxCapacity();

    /**
     * Returns the {@link ByteBufAllocator} which created this buffer.返回创建此缓冲区的ByteBufAllocator。
     */
    public abstract ByteBufAllocator alloc();

    /**
     * Returns the <a href="http://en.wikipedia.org/wiki/Endianness">endianness</a>
     * of this buffer.
     *
     * @deprecated use the Little Endian accessors, e.g. {@code getShortLE}, {@code getIntLE}
     * instead of creating a buffer with swapped {@code endianness}.
     * 不赞成使用小的Endian访问器，例如getShortLE、getIntLE，而不是使用交换的endianness来创建缓冲区。
     */
    @Deprecated
    public abstract ByteOrder order();

    /**
     * Returns a buffer with the specified {@code endianness} which shares the whole region,
     * indexes, and marks of this buffer.  Modifying the content, the indexes, or the marks of the
     * returned buffer or this buffer affects each other's content, indexes, and marks.  If the
     * specified {@code endianness} is identical to this buffer's byte order, this method can
     * return {@code this}.  This method does not modify {@code readerIndex} or {@code writerIndex}
     * of this buffer.
     *
     * @deprecated use the Little Endian accessors, e.g. {@code getShortLE}, {@code getIntLE}
     * instead of creating a buffer with swapped {@code endianness}.
     * 返回具有指定endianness的缓冲区，该缓冲区共享此缓冲区的整个区域、索引和标记。修改内容、索引或返回缓冲区或此缓冲区的标记会影响到彼此的内容、索引和标记。如果指定的endianness与此缓冲区的字节顺序相同，则该方法可以返回这个。此方法不修改此缓冲区的readerIndex或writerIndex。
    不赞成使用小的Endian访问器，例如getShortLE、getIntLE，而不是使用交换的endianness来创建缓冲区。
     */
    @Deprecated
    public abstract ByteBuf order(ByteOrder endianness);

    /**
     * Return the underlying buffer instance if this buffer is a wrapper of another buffer.如果此缓冲区是另一个缓冲区的包装器，则返回底层缓冲区实例。
     *
     * @return {@code null} if this buffer is not a wrapper
     */

    public abstract ByteBuf unwrap();
    /**
     * Returns {@code true} if and only if this buffer is backed by an
     * NIO direct buffer.如果且仅当此缓冲区由NIO直接缓冲区支持时，返回true。
     */
    public abstract boolean isDirect();

    /**
     * Returns {@code true} if and only if this buffer is read-only.如果且仅当此缓冲区为只读时返回true。
     */
    public abstract boolean isReadOnly();

    /**
     * Returns a read-only version of this buffer.返回此缓冲区的只读版本。
     */
    public abstract ByteBuf asReadOnly();

    /**
     * Returns the {@code readerIndex} of this buffer.返回此缓冲区的读取器索引。
     */
    public abstract int readerIndex();

    /**
     * Sets the {@code readerIndex} of this buffer.设置此缓冲区的读取器索引。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code readerIndex} is
     *            less than {@code 0} or
     *            greater than {@code this.writerIndex}
     */
    public abstract ByteBuf readerIndex(int readerIndex);

    /**
     * Returns the {@code writerIndex} of this buffer.返回此缓冲区的写入索引。
     */
    public abstract int writerIndex();

    /**
     * Sets the {@code writerIndex} of this buffer.设置此缓冲区的写入索引。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code writerIndex} is
     *            less than {@code this.readerIndex} or
     *            greater than {@code this.capacity}
     */
    public abstract ByteBuf writerIndex(int writerIndex);

    /**
     * Sets the {@code readerIndex} and {@code writerIndex} of this buffer
     * in one shot.  This method is useful when you have to worry about the
     * invocation order of {@link #readerIndex(int)} and {@link #writerIndex(int)}
     * methods.  For example, the following code will fail:
     *
     * <pre>
     * // Create a buffer whose readerIndex, writerIndex and capacity are
     * // 0, 0 and 8 respectively.
     * {@link ByteBuf} buf = {@link Unpooled}.buffer(8);
     *
     * // IndexOutOfBoundsException is thrown because the specified
     * // readerIndex (2) cannot be greater than the current writerIndex (0).
     * buf.readerIndex(2);
     * buf.writerIndex(4);
     * </pre>
     *
     * The following code will also fail:
     *
     * <pre>
     * // Create a buffer whose readerIndex, writerIndex and capacity are
     * // 0, 8 and 8 respectively.
     * {@link ByteBuf} buf = {@link Unpooled}.wrappedBuffer(new byte[8]);
     *
     * // readerIndex becomes 8.
     * buf.readLong();
     *
     * // IndexOutOfBoundsException is thrown because the specified
     * // writerIndex (4) cannot be less than the current readerIndex (8).
     * buf.writerIndex(4);
     * buf.readerIndex(2);
     * </pre>
     *
     * By contrast, this method guarantees that it never
     * throws an {@link IndexOutOfBoundsException} as long as the specified
     * indexes meet basic constraints, regardless what the current index
     * values of the buffer are:
     *
     * <pre>
     * // No matter what the current state of the buffer is, the following
     * // call always succeeds as long as the capacity of the buffer is not
     * // less than 4.
     * buf.setIndex(2, 4);
     * </pre>
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code readerIndex} is less than 0,
     *         if the specified {@code writerIndex} is less than the specified
     *         {@code readerIndex} or if the specified {@code writerIndex} is
     *         greater than {@code this.capacity}
     *         一次设置这个缓冲区的读写索引。当您需要考虑readerIndex(int)和writerIndex(int)方法的调用顺序时，这个方法非常有用。例如，以下代码将失败:
     */
    public abstract ByteBuf setIndex(int readerIndex, int writerIndex);

    /**
     * Returns the number of readable bytes which is equal to
     * {@code (this.writerIndex - this.readerIndex)}.返回可读字节的数目，它等于(这个)writerIndex - this.readerIndex)。
     */
    public abstract int readableBytes();

    /**
     * Returns the number of writable bytes which is equal to
     * {@code (this.capacity - this.writerIndex)}.返回可写字节的数目，它等于(这个)能力- this.writerIndex)。
     */
    public abstract int writableBytes();

    /**
     * Returns the maximum possible number of writable bytes, which is equal to
     * {@code (this.maxCapacity - this.writerIndex)}.返回可写字节的最大可能数目，它等于(这个)maxCapacity - this.writerIndex)。
     */
    public abstract int maxWritableBytes();

    /**
     * Returns {@code true}
     * if and only if {@code (this.writerIndex - this.readerIndex)} is greater
     * than {@code 0}.
     * 返回当且仅当。writerIndex - this.readerIndex)大于0。
     */
    public abstract boolean isReadable();

    /**
     * Returns {@code true} if and only if this buffer contains equal to or more than the specified number of elements.如果且仅当此缓冲区包含等于或大于指定的元素个数时返回true。
     */
    public abstract boolean isReadable(int size);

    /**
     * Returns {@code true}
     * if and only if {@code (this.capacity - this.writerIndex)} is greater
     * than {@code 0}.返回当且仅当。容量- this.writerIndex)大于0。
     */
    public abstract boolean isWritable();

    /**
     * Returns {@code true} if and only if this buffer has enough room to allow writing the specified number of
     * elements.如果且仅当此缓冲区有足够的空间允许写入指定数量的元素时，返回true。
     */
    public abstract boolean isWritable(int size);

    /**
     * Sets the {@code readerIndex} and {@code writerIndex} of this buffer to
     * {@code 0}.
     * This method is identical to {@link #setIndex(int, int) setIndex(0, 0)}.
     * <p>
     * Please note that the behavior of this method is different
     * from that of NIO buffer, which sets the {@code limit} to
     * the {@code capacity} of the buffer.
     *
     将此缓冲区的readerIndex和writerIndex设置为0。该方法与setIndex(0,0)相同。
     请注意，该方法的行为与NIO缓冲区不同，NIO缓冲区设置了缓冲区容量的限制。
     */
    public abstract ByteBuf clear();

    /**
     * Marks the current {@code readerIndex} in this buffer.  You can
     * reposition the current {@code readerIndex} to the marked
     * {@code readerIndex} by calling {@link #resetReaderIndex()}.
     * The initial value of the marked {@code readerIndex} is {@code 0}.
     * 在此缓冲区中标记当前读取器索引。可以通过调用resetReaderIndex()将当前readerIndex重定位到标记的readerIndex。标记的readerIndex的初始值为0。
     */
    public abstract ByteBuf markReaderIndex();

    /**
     * Repositions the current {@code readerIndex} to the marked
     * {@code readerIndex} in this buffer.将当前readerIndex重定位到该缓冲区中标记的readerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the current {@code writerIndex} is less than the marked
     *         {@code readerIndex}
     */
    public abstract ByteBuf resetReaderIndex();

    /**
     * Marks the current {@code writerIndex} in this buffer.  You can
     * reposition the current {@code writerIndex} to the marked
     * {@code writerIndex} by calling {@link #resetWriterIndex()}.
     * The initial value of the marked {@code writerIndex} is {@code 0}.在此缓冲区中标记当前写入器索引。您可以通过调用resetWriterIndex()将当前的writerIndex重定位到标记的writerIndex。标记的writerIndex的初始值为0。
     */
    public abstract ByteBuf markWriterIndex();

    /**
     * Repositions the current {@code writerIndex} to the marked
     * {@code writerIndex} in this buffer.将当前writerIndex重定位到缓冲区中标记的writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the current {@code readerIndex} is greater than the marked
     *         {@code writerIndex}
     */
    public abstract ByteBuf resetWriterIndex();

    /**
     * Discards the bytes between the 0th index and {@code readerIndex}.
     * It moves the bytes between {@code readerIndex} and {@code writerIndex}
     * to the 0th index, and sets {@code readerIndex} and {@code writerIndex}
     * to {@code 0} and {@code oldWriterIndex - oldReaderIndex} respectively.
     * <p>
     * Please refer to the class documentation for more detailed explanation.
     * 丢弃第0索引和readerIndex之间的字节。它将readerIndex和writerIndex之间的字节移动到第0个索引中，并将readerIndex和writerIndex分别设置为0和oldWriterIndex - olande index。
     */
    public abstract ByteBuf discardReadBytes();

    /**
     * Similar to {@link ByteBuf#discardReadBytes()} except that this method might discard
     * some, all, or none of read bytes depending on its internal implementation to reduce
     * overall memory bandwidth consumption at the cost of potentially additional memory
     * consumption.
     * 与discardReadBytes()类似的是，该方法可能会丢弃一些、全部或没有读取字节，这取决于它的内部实现，以减少总体内存带宽消耗，代价是可能增加内存消耗。
     */
    public abstract ByteBuf discardSomeReadBytes();

    /**
     * Makes sure the number of {@linkplain #writableBytes() the writable bytes}
     * is equal to or greater than the specified value.  If there is enough
     * writable bytes in this buffer, this method returns with no side effect.
     * Otherwise, it raises an {@link IllegalArgumentException}.
     *
     * @param minWritableBytes
     *        the expected minimum number of writable bytes
     * @throws IndexOutOfBoundsException
     *         if {@link #writerIndex()} + {@code minWritableBytes} &gt; {@link #maxCapacity()}
     *         确保可写字节的数量等于或大于指定的值。如果该缓冲区中有足够的可写字节，则此方法返回时没有副作用。否则，它会引发一个非法争论的例外。
     */
    public abstract ByteBuf ensureWritable(int minWritableBytes);

    /**
     * Tries to make sure the number of {@linkplain #writableBytes() the writable bytes}
     * is equal to or greater than the specified value.  Unlike {@link #ensureWritable(int)},
     * this method does not raise an exception but returns a code.
     *
     * @param minWritableBytes
     *        the expected minimum number of writable bytes
     * @param force
     *        When {@link #writerIndex()} + {@code minWritableBytes} &gt; {@link #maxCapacity()}:
     *        <ul>
     *        <li>{@code true} - the capacity of the buffer is expanded to {@link #maxCapacity()}</li>
     *        <li>{@code false} - the capacity of the buffer is unchanged</li>
     *        </ul>
     * @return {@code 0} if the buffer has enough writable bytes, and its capacity is unchanged.
     *         {@code 1} if the buffer does not have enough bytes, and its capacity is unchanged.
     *         {@code 2} if the buffer has enough writable bytes, and its capacity has been increased.
     *         {@code 3} if the buffer does not have enough bytes, but its capacity has been
     *                   increased to its maximum.
     *                   尝试确保可写字节的数量等于或大于指定的值。与ensureWritable（int）不同，这种方法不会引发异常，但会返回一个代码
     */
    public abstract int ensureWritable(int minWritableBytes, boolean force);

    /**
     * Gets a boolean at the specified absolute (@code index) in this buffer.
     * This method does not modify the {@code readerIndex} or {@code writerIndex}
     * of this buffer.获取缓冲区中指定的绝对值(@code index)的布尔值。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */
    public abstract boolean getBoolean(int index);

    /**
     * Gets a byte at the specified absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     *         获取缓冲区中指定的绝对索引处的字节。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract byte  getByte(int index);

    /**
     * Gets an unsigned byte at the specified absolute {@code index} in this
     * buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.获取缓冲区中指定的绝对索引处的无符号字节。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */
    public abstract short getUnsignedByte(int index);

    /**
     * Gets a 16-bit short integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     *         获取该缓冲区中指定的绝对索引处的16位短整数。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract short getShort(int index);

    /**
     * Gets a 16-bit short integer at the specified absolute {@code index} in
     * this buffer in Little Endian Byte Order. This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     *         在这个缓冲区中，以小的Endian字节顺序获取一个16位的短整数。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract short getShortLE(int index);

    /**
     * Gets an unsigned 16-bit short integer at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     *         获取在此缓冲区中指定的绝对索引处的无符号16位短整数。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract int getUnsignedShort(int index);

    /**
     * Gets an unsigned 16-bit short integer at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     *         以小字节顺序在此缓冲区中指定的绝对索引处获取无符号16位短整数。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract int getUnsignedShortLE(int index);

    /**
     * Gets a 24-bit medium integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 3} is greater than {@code this.capacity}
     *         获取该缓冲区中指定的绝对索引处的24位中整数。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract int   getMedium(int index);

    /**
     * Gets a 24-bit medium integer at the specified absolute {@code index} in
     * this buffer in the Little Endian Byte Order. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.以小字节的顺序获取缓冲区中指定的绝对索引处的24位中整数。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 3} is greater than {@code this.capacity}
     */
    public abstract int getMediumLE(int index);

    /**
     * Gets an unsigned 24-bit medium integer at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.在该缓冲区的指定绝对索引处获取一个未签名的24位中整数。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 3} is greater than {@code this.capacity}
     */
    public abstract int   getUnsignedMedium(int index);

    /**
     * Gets an unsigned 24-bit medium integer at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.以小字节的顺序获取缓冲区中指定的绝对索引处的无符号24位中整数。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 3} is greater than {@code this.capacity}
     */
    public abstract int   getUnsignedMediumLE(int index);

    /**
     * Gets a 32-bit integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     *         获取缓冲区中指定的绝对索引处的32位整数。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract int   getInt(int index);

    /**
     * Gets a 32-bit integer at the specified absolute {@code index} in
     * this buffer with Little Endian Byte Order. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.在这个缓冲区的指定的绝对索引中获取一个32位的整数，并带有小的Endian字节顺序。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract int   getIntLE(int index);

    /**
     * Gets an unsigned 32-bit integer at the specified absolute {@code index}
     * in this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.获取缓冲区中指定的绝对索引处的无符号32位整数。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract long  getUnsignedInt(int index);

    /**
     * Gets an unsigned 32-bit integer at the specified absolute {@code index}
     * in this buffer in Little Endian Byte Order. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.以小字节顺序获取缓冲区中指定的绝对索引处的无符号32位整数。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract long  getUnsignedIntLE(int index);

    /**
     * Gets a 64-bit long integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.获取该缓冲区中指定的绝对索引处的64位长整数。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public abstract long  getLong(int index);

    /**
     * Gets a 64-bit long integer at the specified absolute {@code index} in
     * this buffer in Little Endian Byte Order. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.以小字节顺序获取缓冲区中指定的绝对索引处的64位长整数。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public abstract long  getLongLE(int index);

    /**
     * Gets a 2-byte UTF-16 character at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.获取该缓冲区中指定的绝对索引处的2字节UTF-16字符。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */
    public abstract char  getChar(int index);

    /**
     * Gets a 32-bit floating point number at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.获取此缓冲区中指定的绝对索引处的32位浮点数。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract float getFloat(int index);

    /**
     * Gets a 32-bit floating point number at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.以小字节顺序获取缓冲区中指定的绝对索引处的32位浮点数。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public float getFloatLE(int index) {
        return Float.intBitsToFloat(getIntLE(index));
    }

    /**
     * Gets a 64-bit floating point number at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.获取该缓冲区中指定的绝对索引处的64位浮点数。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public abstract double getDouble(int index);

    /**
     * Gets a 64-bit floating point number at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.以小字节顺序获取缓冲区中指定的绝对索引处的64位浮点数。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public double getDoubleLE(int index) {
        return Double.longBitsToDouble(getLongLE(index));
    }

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index} until the destination becomes
     * non-writable.  This method is basically same with
     * {@link #getBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code writerIndex} of the destination by the
     * number of the transferred bytes while
     * {@link #getBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * the source buffer (i.e. {@code this}).将此缓冲区的数据传输到从指定的绝对索引开始的指定目标，直到目标变为不可写。此方法与getBytes(int, ByteBuf, int, int)基本相同，只是该方法通过传输字节的数量增加目标的writerIndex，而getBytes(int, ByteBuf, int, int)则没有。此方法不修改源缓冲区的readerIndex或writerIndex(即这个)。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + dst.writableBytes} is greater than
     *            {@code this.capacity}
     */
    public abstract ByteBuf getBytes(int index, ByteBuf dst);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.  This method is basically same
     * with {@link #getBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code writerIndex} of the destination by the
     * number of the transferred bytes while
     * {@link #getBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * the source buffer (i.e. {@code this}).
     *
     * @param length the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code length} is greater than {@code dst.writableBytes}
     *         将此缓冲区的数据传输到指定的目的地，从指定的绝对索引开始。此方法与getBytes(int, ByteBuf, int, int)基本相同，只是该方法通过传输字节的数量增加目标的writerIndex，而getBytes(int, ByteBuf, int, int)则没有。此方法不修改源缓冲区的readerIndex或writerIndex(即这个)。
     */
    public abstract ByteBuf getBytes(int index, ByteBuf dst, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex}
     * of both the source (i.e. {@code this}) and the destination.
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if the specified {@code dstIndex} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code dstIndex + length} is greater than
     *            {@code dst.capacity}
     *            将此缓冲区的数据传输到指定的目的地，从指定的绝对索引开始。此方法不修改源(即此)和目标的readerIndex或writerIndex。
     */
    public abstract ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + dst.length} is greater than
     *            {@code this.capacity}
     *            将此缓冲区的数据从指定的绝对索引开始传输到指定的目的地。此方法不修改此缓冲区的readerIndex或writerIndex
     */
    public abstract ByteBuf getBytes(int index, byte[] dst);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex}
     * of this buffer.
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if the specified {@code dstIndex} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code dstIndex + length} is greater than
     *            {@code dst.length}
     *            将此缓冲区的数据从指定的绝对索引开始传输到指定的目的地。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index} until the destination's position
     * reaches its limit.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer while the destination's {@code position} will be increased.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + dst.remaining()} is greater than
     *            {@code this.capacity}
     *            将此缓冲区的数据传输到指定的目的地，从指定的绝对索引开始，直到目的地的位置达到极限。此方法不修改此缓冲区的readerIndex或writerIndex，同时将增加目标的位置。
     */
    public abstract ByteBuf getBytes(int index, ByteBuffer dst);

    /**
     * Transfers this buffer's data to the specified stream starting at the
     * specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.将此缓冲区的数据传输到从指定的绝对索引开始的指定流。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @param length the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than
     *            {@code this.capacity}
     * @throws IOException
     *         if the specified stream threw an exception during I/O
     */
    public abstract ByteBuf getBytes(int index, OutputStream out, int length) throws IOException;

    /**
     * Transfers this buffer's data to the specified channel starting at the
     * specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.从指定的绝对索引开始将此缓冲区的数据传输到指定的流。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes written out to the specified channel
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than
     *            {@code this.capacity}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */
    public abstract int getBytes(int index, GatheringByteChannel out, int length) throws IOException;

    /**
     * Transfers this buffer's data starting at the specified absolute {@code index}
     * to the specified channel starting at the given file position.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer. This method does not modify the channel's position.
     *
     * @param position the file position at which the transfer is to begin
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes written out to the specified channel
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than
     *            {@code this.capacity}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     *         将从指定绝对索引开始的缓冲区数据传输到从指定文件位置开始的指定通道。此方法不修改此缓冲区的readerIndex或writerIndex。此方法不修改通道的位置。
     */
    public abstract int getBytes(int index, FileChannel out, long position, int length) throws IOException;

    /**
     * Gets a {@link CharSequence} with the given length at the given index.获取给定索引处给定长度的CharSequence。
     *
     * @param length the length to read
     * @param charset that should be used
     * @return the sequence
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     */
    public abstract CharSequence getCharSequence(int index, int length, Charset charset);

    /**
     * Sets the specified boolean at the specified absolute {@code index} in this
     * buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.在此缓冲区的指定绝对索引处设置指定的布尔值。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */
    public abstract ByteBuf setBoolean(int index, boolean value);

    /**
     * Sets the specified byte at the specified absolute {@code index} in this
     * buffer.  The 24 high-order bits of the specified value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     *         在此缓冲区中指定的绝对索引处设置指定的字节。指定值的24个高阶位将被忽略。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract ByteBuf setByte(int index, int value);

    /**
     * Sets the specified 16-bit short integer at the specified absolute
     * {@code index} in this buffer.  The 16 high-order bits of the specified
     * value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     *         将指定的16位短整数设置为该缓冲区中指定的绝对索引。指定值的16个高阶位将被忽略。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract ByteBuf setShort(int index, int value);

    /**
     * Sets the specified 16-bit short integer at the specified absolute
     * {@code index} in this buffer with the Little Endian Byte Order.
     * The 16 high-order bits of the specified value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.将指定的16位短整数设置为该缓冲区中指定的绝对索引，其字节顺序为小字节。指定值的16个高阶位将被忽略。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */
    public abstract ByteBuf setShortLE(int index, int value);

    /**
     * Sets the specified 24-bit medium integer at the specified absolute
     * {@code index} in this buffer.  Please note that the most significant
     * byte is ignored in the specified value.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 3} is greater than {@code this.capacity}
     *         在此缓冲区中指定的绝对索引处设置指定的24位中位数。请注意，在指定的值中忽略了最重要的字节。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract ByteBuf setMedium(int index, int value);

    /**
     * Sets the specified 24-bit medium integer at the specified absolute
     * {@code index} in this buffer in the Little Endian Byte Order.
     * Please note that the most significant byte is ignored in the
     * specified value.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 3} is greater than {@code this.capacity}
     *         将指定的24位中整数设置为小Endian字节顺序中这个缓冲区中的指定绝对索引。请注意，在指定的值中忽略了最重要的字节。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract ByteBuf setMediumLE(int index, int value);

    /**
     * Sets the specified 32-bit integer at the specified absolute
     * {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     *         在此缓冲区中指定的绝对索引处设置指定的32位整数。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract ByteBuf setInt(int index, int value);

    /**
     * Sets the specified 32-bit integer at the specified absolute
     * {@code index} in this buffer with Little Endian byte order
     * .
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     *
    将指定的32位整数设置为该缓冲区中指定的绝对索引，字节顺序很小。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract ByteBuf setIntLE(int index, int value);

    /**
     * Sets the specified 64-bit long integer at the specified absolute
     * {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     *
    在此缓冲区中指定的绝对索引处设置指定的64位长整数。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract ByteBuf setLong(int index, long value);

    /**
     * Sets the specified 64-bit long integer at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.在此缓冲区中指定的绝对索引处以小字节顺序设置指定的64位长整数。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public abstract ByteBuf setLongLE(int index, long value);

    /**
     * Sets the specified 2-byte UTF-16 character at the specified absolute
     * {@code index} in this buffer.
     * The 16 high-order bits of the specified value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.将指定的2字节UTF-16字符设置为该缓冲区中指定的绝对索引。指定值的16个高阶位将被忽略。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */
    public abstract ByteBuf setChar(int index, int value);

    /**
     * Sets the specified 32-bit floating-point number at the specified
     * absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.在此缓冲区中指定的绝对索引处设置指定的32位浮点数。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract ByteBuf setFloat(int index, float value);

    /**
     * Sets the specified 32-bit floating-point number at the specified
     * absolute {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.将指定的32位浮点数设置为该缓冲区中指定的绝对索引，按小字节顺序。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public ByteBuf setFloatLE(int index, float value) {
        return setIntLE(index, Float.floatToRawIntBits(value));
    }

    /**
     * Sets the specified 64-bit floating-point number at the specified
     * absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.在此缓冲区中指定的绝对索引处设置指定的64位浮点数。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public abstract ByteBuf setDouble(int index, double value);

    /**
     * Sets the specified 64-bit floating-point number at the specified
     * absolute {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.将指定的64位浮点数设置为这个缓冲区中的指定绝对索引，以小的Endian字节顺序。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public ByteBuf setDoubleLE(int index, double value) {
        return setLongLE(index, Double.doubleToRawLongBits(value));
    }

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index} until the source buffer becomes
     * unreadable.  This method is basically same with
     * {@link #setBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code readerIndex} of the source buffer by
     * the number of the transferred bytes while
     * {@link #setBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * the source buffer (i.e. {@code this}).
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + src.readableBytes} is greater than
     *            {@code this.capacity}
     *            将指定的源缓冲区的数据传输到从指定的绝对索引开始的缓冲区，直到源缓冲区变得不可读为止。此方法与setBytes(int, ByteBuf, int, int)基本相同，只是该方法通过传输字节的数量增加了源缓冲区的readerIndex，而setBytes(int, ByteBuf, int, int)则没有。此方法不修改源缓冲区的readerIndex或writerIndex(即这个)。
     */
    public abstract ByteBuf setBytes(int index, ByteBuf src);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index}.  This method is basically same
     * with {@link #setBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code readerIndex} of the source buffer by
     * the number of the transferred bytes while
     * {@link #setBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * the source buffer (i.e. {@code this}).
     *
     * @param length the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code length} is greater than {@code src.readableBytes}
     *         将指定的源缓冲区的数据从指定的绝对索引开始传输到此缓冲区。此方法与setBytes(int, ByteBuf, int, int)基本相同，只是该方法通过传输字节的数量增加了源缓冲区的readerIndex，而setBytes(int, ByteBuf, int, int)则没有。此方法不修改源缓冲区的readerIndex或writerIndex(即这个)。
     */
    public abstract ByteBuf setBytes(int index, ByteBuf src, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex}
     * of both the source (i.e. {@code this}) and the destination.
     *
     * @param srcIndex the first index of the source
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if the specified {@code srcIndex} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code srcIndex + length} is greater than
     *            {@code src.capacity}
     *
    将指定的源缓冲区的数据从指定的绝对索引开始传输到此缓冲区。此方法不修改源(即此)和目标的readerIndex或writerIndex。
     */
    public abstract ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length);

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.将指定的源数组的数据从指定的绝对索引开始传输到此缓冲区。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + src.length} is greater than
     *            {@code this.capacity}
     */
    public abstract ByteBuf setBytes(int index, byte[] src);

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.将指定的源数组的数据从指定的绝对索引开始传输到此缓冲区。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if the specified {@code srcIndex} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code srcIndex + length} is greater than {@code src.length}
     */
    public abstract ByteBuf setBytes(int index, byte[] src, int srcIndex, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index} until the source buffer's position
     * reaches its limit.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     将指定的源缓冲区的数据从指定的绝对索引开始传输到此缓冲区，直到源缓冲区的位置达到其极限。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + src.remaining()} is greater than
     *            {@code this.capacity}
     */
    public abstract ByteBuf setBytes(int index, ByteBuffer src);

    /**
     * Transfers the content of the specified source stream to this buffer
     * starting at the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.从指定的绝对索引开始将指定源流的内容传输到此缓冲区。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @param length the number of bytes to transfer
     *
     * @return the actual number of bytes read in from the specified channel.
     *         {@code -1} if the specified channel is closed.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than {@code this.capacity}
     * @throws IOException
     *         if the specified stream threw an exception during I/O
     */
    public abstract int setBytes(int index, InputStream in, int length) throws IOException;

    /**
     * Transfers the content of the specified source channel to this buffer
     * starting at the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.将指定源通道的内容从指定的绝对索引开始传输到此缓冲区。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes read in from the specified channel.
     *         {@code -1} if the specified channel is closed.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than {@code this.capacity}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */
    public abstract int setBytes(int index, ScatteringByteChannel in, int length) throws IOException;

    /**
     * Transfers the content of the specified source channel starting at the given file position
     * to this buffer starting at the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer. This method does not modify the channel's position.将从给定文件位置开始的指定源通道的内容传输到从指定的绝对索引开始的缓冲区。此方法不修改此缓冲区的readerIndex或writerIndex。此方法不修改通道的位置。
     *
     * @param position the file position at which the transfer is to begin
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes read in from the specified channel.
     *         {@code -1} if the specified channel is closed.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than {@code this.capacity}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */
    public abstract int setBytes(int index, FileChannel in, long position, int length) throws IOException;

    /**
     * Fills this buffer with <tt>NUL (0x00)</tt> starting at the specified
     * absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.用NUL (0x00)从指定的绝对索引填充这个缓冲区。此方法不修改此缓冲区的readerIndex或writerIndex。
     *
     * @param length the number of <tt>NUL</tt>s to write to the buffer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than {@code this.capacity}
     */
    public abstract ByteBuf setZero(int index, int length);

    /**
     * Writes the specified {@link CharSequence} at the current {@code writerIndex} and increases
     * the {@code writerIndex} by the written bytes.在当前的writerIndex中写入指定的CharSequence，并通过写入的字节增加writerIndex。
     *
     * @param index on which the sequence should be written
     * @param sequence to write
     * @param charset that should be used.
     * @return the written number of bytes.
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is not large enough to write the whole sequence
     */
    public abstract int setCharSequence(int index, CharSequence sequence, Charset charset);

    /**
     * Gets a boolean at the current {@code readerIndex} and increases
     * the {@code readerIndex} by {@code 1} in this buffer.在当前readerIndex中获取一个布尔值，并在这个缓冲区中增加1的readerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 1}
     */
    public abstract boolean readBoolean();

    /**
     * Gets a byte at the current {@code readerIndex} and increases
     * the {@code readerIndex} by {@code 1} in this buffer.获取当前readerIndex处的一个字节，并在此缓冲区中将readerIndex增加1。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 1}
     */
    public abstract byte  readByte();

    /**
     * Gets an unsigned byte at the current {@code readerIndex} and increases
     * the {@code readerIndex} by {@code 1} in this buffer.获取当前readerIndex处的无符号字节，并将该缓冲区中的readerIndex增加1。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 1}
     */
    public abstract short readUnsignedByte();

    /**
     * Gets a 16-bit short integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.获取当前读取器索引处的16位短整数，并在此缓冲区中将读取器索引增加2。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */
    public abstract short readShort();

    /**
     * Gets a 16-bit short integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 2} in this buffer.以小字节顺序获取当前读取器索引处的16位短整数，并在此缓冲区中将读取器索引增加2。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */
    public abstract short readShortLE();

    /**
     * Gets an unsigned 16-bit short integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.在当前readerIndex中获取一个未签名的16位短整数，并在此缓冲区中增加readerIndex 2。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */
    public abstract int   readUnsignedShort();

    /**
     * Gets an unsigned 16-bit short integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 2} in this buffer.在小的Endian字节顺序的当前readerIndex中获取一个未签名的16位短整数，并在这个缓冲区中增加2的readerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */
    public abstract int   readUnsignedShortLE();

    /**
     * Gets a 24-bit medium integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 3} in this buffer.获取当前读取器索引处的24位中位数，并在此缓冲区中将读取器索引增加3。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 3}
     */
    public abstract int   readMedium();

    /**
     * Gets a 24-bit medium integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the
     * {@code readerIndex} by {@code 3} in this buffer.以小字节顺序获取当前读取器索引处的24位中整数，并在此缓冲区中将读取器索引增加3。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 3}
     */
    public abstract int   readMediumLE();

    /**
     * Gets an unsigned 24-bit medium integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 3} in this buffer.获取当前readerIndex处的无符号24位中整数，并将该缓冲区中的readerIndex增加3。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 3}
     */
    public abstract int   readUnsignedMedium();

    /**
     * Gets an unsigned 24-bit medium integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 3} in this buffer.以小字节顺序获取当前readerIndex处的无符号24位中整数，并将该缓冲区中的readerIndex增加3。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 3}
     */
    public abstract int   readUnsignedMediumLE();

    /**
     * Gets a 32-bit integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 4} in this buffer.获取当前readerIndex处的32位整数，并将该缓冲区中的readerIndex增加4。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */
    public abstract int   readInt();

    /**
     * Gets a 32-bit integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 4} in this buffer.以小字节顺序获取当前读取器索引处的32位整数，并在此缓冲区中将读取器索引增加4。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */
    public abstract int   readIntLE();

    /**
     * Gets an unsigned 32-bit integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 4} in this buffer.获取当前readerIndex上的无符号32位整数，并在此缓冲区中将readerIndex增加4。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */
    public abstract long  readUnsignedInt();

    /**
     * Gets an unsigned 32-bit integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 4} in this buffer.以小字节顺序获取当前readerIndex上的无符号32位整数，并在此缓冲区中将readerIndex增加4。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */
    public abstract long  readUnsignedIntLE();

    /**
     * Gets a 64-bit integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 8} in this buffer.获取当前读取器索引处的64位整数，并在此缓冲区中将读取器索引增加8。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 8}
     */
    public abstract long  readLong();

    /**
     * Gets a 64-bit integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 8} in this buffer.在小的Endian字节顺序的当前readerIndex中获取一个64位整数，并在这个缓冲区中增加8的readerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 8}
     */
    public abstract long  readLongLE();

    /**
     * Gets a 2-byte UTF-16 character at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.获取当前readerIndex处的2字节UTF-16字符，并在此缓冲区中将readerIndex增加2个字节。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */
    public abstract char  readChar();

    /**
     * Gets a 32-bit floating point number at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 4} in this buffer.获取当前readerIndex处的32位浮点数，并将该缓冲区中的readerIndex增加4。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */
    public abstract float readFloat();

    /**
     * Gets a 32-bit floating point number at the current {@code readerIndex}
     * in Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 4} in this buffer.在当前readerIndex中获取一个32位的浮点数，以小的Endian字节顺序，并在这个缓冲区中增加4的readerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */
    public float readFloatLE() {
        return Float.intBitsToFloat(readIntLE());
    }

    /**
     * Gets a 64-bit floating point number at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 8} in this buffer.获取当前读取器索引处的64位浮点数，并在此缓冲区中将读取器索引增加8。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 8}
     */
    public abstract double readDouble();

    /**
     * Gets a 64-bit floating point number at the current {@code readerIndex}
     * in Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 8} in this buffer.以小字节顺序获取当前读取器索引处的64位浮点数，并在此缓冲区中将读取器索引增加8。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 8}
     */
    public double readDoubleLE() {
        return Double.longBitsToDouble(readLongLE());
    }

    /**
     * Transfers this buffer's data to a newly created buffer starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).
     * The returned buffer's {@code readerIndex} and {@code writerIndex} are
     * {@code 0} and {@code length} respectively.
     *
     * @param length the number of bytes to transfer
     *
     * @return the newly created buffer which contains the transferred bytes
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     *         将此缓冲区的数据传输到新创建的缓冲区，从当前的readerIndex开始，并通过传输的字节数(=长度)增加readerIndex。返回的缓冲区的readerIndex和writerIndex分别为0和length。
     */
    public abstract ByteBuf readBytes(int length);

    /**
     * Returns a new slice of this buffer's sub-region starting at the current
     * {@code readerIndex} and increases the {@code readerIndex} by the size
     * of the new slice (= {@code length}).
     * <p>
     * Also be aware that this method will NOT call {@link #retain()} and so the
     * reference count will NOT be increased.
     *
     * @param length the size of the new slice
     *
     * @return the newly created slice
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     *         返回从当前readerIndex开始的缓冲区子区域的一个新切片，并通过新切片的大小(= length)增加readerIndex。
    还要注意，此方法不会调用retain()，因此不会增加引用计数。
     */
    public abstract ByteBuf readSlice(int length);

    /**
     * Returns a new retained slice of this buffer's sub-region starting at the current
     * {@code readerIndex} and increases the {@code readerIndex} by the size
     * of the new slice (= {@code length}).
     * <p>
     * Note that this method returns a {@linkplain #retain() retained} buffer unlike {@link #readSlice(int)}.
     * This method behaves similarly to {@code readSlice(...).retain()} except that this method may return
     * a buffer implementation that produces less garbage.
     *
     * @param length the size of the new slice
     *
     * @return the newly created slice
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     *         返回从当前readerIndex开始的缓冲子区域的一个新的保留切片，并根据新切片的大小(= length)增加readerIndex。
    注意，该方法返回一个与readSlice(int)不同的保留缓冲区。这个方法的行为类似于readSlice(…).retain()，只是这个方法可能返回一个产生较少垃圾的缓冲区实现。
     */
    public abstract ByteBuf readRetainedSlice(int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} until the destination becomes
     * non-writable, and increases the {@code readerIndex} by the number of the
     * transferred bytes.  This method is basically same with
     * {@link #readBytes(ByteBuf, int, int)}, except that this method
     * increases the {@code writerIndex} of the destination by the number of
     * the transferred bytes while {@link #readBytes(ByteBuf, int, int)}
     * does not.
     * 将此缓冲区的数据传输到从当前readerIndex开始的指定目标，直到目标变成不可写的，并通过传输字节的数量增加readerIndex。这个方法与readBytes(ByteBuf, int, int)基本相同，只是该方法增加了目的地的writerIndex，而readBytes(ByteBuf, int, int)则不增加。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code dst.writableBytes} is greater than
     *            {@code this.readableBytes}
     */
    public abstract ByteBuf readBytes(ByteBuf dst);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).  This method
     * is basically same with {@link #readBytes(ByteBuf, int, int)},
     * except that this method increases the {@code writerIndex} of the
     * destination by the number of the transferred bytes (= {@code length})
     * while {@link #readBytes(ByteBuf, int, int)} does not.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes} or
     *         if {@code length} is greater than {@code dst.writableBytes}
     *
    将此缓冲区的数据传输到当前readerIndex中指定的目的地，并通过传输的字节数(= length)增加readerIndex。此方法与readBytes(ByteBuf、int、int)基本相同，只是该方法通过传输的字节数(=长度)增加了目标的writerIndex，而readBytes(ByteBuf、int、int)则没有。
     */
    public abstract ByteBuf readBytes(ByteBuf dst, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).将此缓冲区的数据传输到从当前readerIndex开始的指定目的地，并通过传输的字节数(=长度)增加readerIndex。
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code dstIndex} is less than {@code 0},
     *         if {@code length} is greater than {@code this.readableBytes}, or
     *         if {@code dstIndex + length} is greater than
     *            {@code dst.capacity}
     */
    public abstract ByteBuf readBytes(ByteBuf dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code dst.length}).将此缓冲区的数据传输到当前readerIndex中指定的目的地，并通过传输的字节数增加readerIndex (= dst.length)。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code dst.length} is greater than {@code this.readableBytes}
     */
    public abstract ByteBuf readBytes(byte[] dst);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).将此缓冲区的数据传输到从当前readerIndex开始的指定目的地，并通过传输的字节数(=长度)增加readerIndex。
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code dstIndex} is less than {@code 0},
     *         if {@code length} is greater than {@code this.readableBytes}, or
     *         if {@code dstIndex + length} is greater than {@code dst.length}
     */
    public abstract ByteBuf readBytes(byte[] dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} until the destination's position
     * reaches its limit, and increases the {@code readerIndex} by the
     * number of the transferred bytes.将此缓冲区的数据传输到指定的目标，从当前readerIndex开始，直到目标的位置达到其极限，并通过传输字节的数量增加readerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code dst.remaining()} is greater than
     *            {@code this.readableBytes}
     */
    public abstract ByteBuf readBytes(ByteBuffer dst);

    /**
     * Transfers this buffer's data to the specified stream starting at the
     * current {@code readerIndex}.从当前readerIndex开始将此缓冲区的数据传输到指定的流。
     *
     * @param length the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     * @throws IOException
     *         if the specified stream threw an exception during I/O
     */
    public abstract ByteBuf readBytes(OutputStream out, int length) throws IOException;

    /**
     * Transfers this buffer's data to the specified stream starting at the
     * current {@code readerIndex}.从当前readerIndex开始将此缓冲区的数据传输到指定的流。
     *
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes written out to the specified channel
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */
    public abstract int readBytes(GatheringByteChannel out, int length) throws IOException;

    /**
     * Gets a {@link CharSequence} with the given length at the current {@code readerIndex}
     * and increases the {@code readerIndex} by the given length.获取当前readerIndex处给定长度的字符序列，并通过给定长度增加readerIndex。
     *
     * @param length the length to read
     * @param charset that should be used
     * @return the sequence
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     */
    public abstract CharSequence readCharSequence(int length, Charset charset);

    /**
     * Transfers this buffer's data starting at the current {@code readerIndex}
     * to the specified channel starting at the given file position.
     * This method does not modify the channel's position.将此缓冲区的数据从当前的readerIndex开始传输到指定的通道，从给定的文件位置开始。此方法不修改通道的位置。
     *
     * @param position the file position at which the transfer is to begin
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes written out to the specified channel
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */
    public abstract int readBytes(FileChannel out, long position, int length) throws IOException;

    /**
     * Increases the current {@code readerIndex} by the specified
     * {@code length} in this buffer.将当前读取器索引增加到此缓冲区中的指定长度。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     */
    public abstract ByteBuf skipBytes(int length);

    /**
     * Sets the specified boolean at the current {@code writerIndex}
     * and increases the {@code writerIndex} by {@code 1} in this buffer.在当前writerIndex设置指定的布尔值，并在此缓冲区中将writerIndex增加1。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 1}
     */
    public abstract ByteBuf writeBoolean(boolean value);

    /**
     * Sets the specified byte at the current {@code writerIndex}
     * and increases the {@code writerIndex} by {@code 1} in this buffer.
     * The 24 high-order bits of the specified value are ignored.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 1}
     *         在当前writerIndex处设置指定的字节，并在此缓冲区中将writerIndex增加1。指定值的24个高阶位将被忽略。
     */
    public abstract ByteBuf writeByte(int value);

    /**
     * Sets the specified 16-bit short integer at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 2}
     * in this buffer.  The 16 high-order bits of the specified value are ignored.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 2}
     *         在当前writerIndex处设置指定的16位短整数，并将该缓冲区中的writerIndex增加2。指定值的16个高阶位将被忽略。
     */
    public abstract ByteBuf writeShort(int value);

    /**
     * Sets the specified 16-bit short integer in the Little Endian Byte
     * Order at the current {@code writerIndex} and increases the
     * {@code writerIndex} by {@code 2} in this buffer.
     * The 16 high-order bits of the specified value are ignored.在当前writerIndex的小Endian字节顺序中设置指定的16位短整数，并将该缓冲区中的writerIndex增加2。指定值的16个高阶位将被忽略。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 2}
     */
    public abstract ByteBuf writeShortLE(int value);

    /**
     * Sets the specified 24-bit medium integer at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 3}
     * in this buffer.在当前writerIndex处设置指定的24位中位数，并将该缓冲区中的writerIndex增加3。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 3}
     */
    public abstract ByteBuf writeMedium(int value);

    /**
     * Sets the specified 24-bit medium integer at the current
     * {@code writerIndex} in the Little Endian Byte Order and
     * increases the {@code writerIndex} by {@code 3} in this
     * buffer.将指定的24位中整数设置为小Endian字节顺序的当前writerIndex，并在这个缓冲区中增加3的writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 3}
     */
    public abstract ByteBuf writeMediumLE(int value);

    /**
     * Sets the specified 32-bit integer at the current {@code writerIndex}
     * and increases the {@code writerIndex} by {@code 4} in this buffer.
     * 在当前writerIndex处设置指定的32位整数，并将该缓冲区中的writerIndex增加4位。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 4}
     */
    public abstract ByteBuf writeInt(int value);

    /**
     * Sets the specified 32-bit integer at the current {@code writerIndex}
     * in the Little Endian Byte Order and increases the {@code writerIndex}
     * by {@code 4} in this buffer.以小字节的顺序在当前writerIndex上设置指定的32位整数，并在此缓冲区中将writerIndex增加4位。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 4}
     */
    public abstract ByteBuf writeIntLE(int value);

    /**
     * Sets the specified 64-bit long integer at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 8}
     * in this buffer.在当前writerIndex设置指定的64位长整数，并将该缓冲区中的writerIndex增加8。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 8}
     */
    public abstract ByteBuf writeLong(long value);

    /**
     * Sets the specified 64-bit long integer at the current
     * {@code writerIndex} in the Little Endian Byte Order and
     * increases the {@code writerIndex} by {@code 8}
     * in this buffer.以小字节顺序在当前writerIndex上设置指定的64位长整数，并将该缓冲区中的writerIndex增加8。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 8}
     */
    public abstract ByteBuf writeLongLE(long value);

    /**
     * Sets the specified 2-byte UTF-16 character at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 2}
     * in this buffer.  The 16 high-order bits of the specified value are ignored.在当前的writerIndex上设置指定的2字节UTF-16字符，并在这个缓冲区中增加2字节的writerIndex。指定值的16个高阶位将被忽略。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 2}
     */
    public abstract ByteBuf writeChar(int value);

    /**
     * Sets the specified 32-bit floating point number at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 4}
     * in this buffer.在当前的writerIndex上设置指定的32位浮点数，并在这个缓冲区中增加4的writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 4}
     */
    public abstract ByteBuf writeFloat(float value);

    /**
     * Sets the specified 32-bit floating point number at the current
     * {@code writerIndex} in Little Endian Byte Order and increases
     * the {@code writerIndex} by {@code 4} in this buffer.以小字节顺序在当前writerIndex上设置指定的32位浮点数，并在此缓冲区中将writerIndex增加4。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 4}
     */
    public ByteBuf writeFloatLE(float value) {
        return writeIntLE(Float.floatToRawIntBits(value));
    }

    /**
     * Sets the specified 64-bit floating point number at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 8}
     * in this buffer.在当前writerIndex设置指定的64位浮点数，并将该缓冲区中的writerIndex增加8。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 8}
     */
    public abstract ByteBuf writeDouble(double value);

    /**
     * Sets the specified 64-bit floating point number at the current
     * {@code writerIndex} in Little Endian Byte Order and increases
     * the {@code writerIndex} by {@code 8} in this buffer.以小字节顺序在当前writerIndex上设置指定的64位浮点数，并在此缓冲区中将writerIndex增加8。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 8}
     */
    public ByteBuf writeDoubleLE(double value) {
        return writeLongLE(Double.doubleToRawLongBits(value));
    }

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} until the source buffer becomes
     * unreadable, and increases the {@code writerIndex} by the number of
     * the transferred bytes.  This method is basically same with
     * {@link #writeBytes(ByteBuf, int, int)}, except that this method
     * increases the {@code readerIndex} of the source buffer by the number of
     * the transferred bytes while {@link #writeBytes(ByteBuf, int, int)}
     * does not.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code src.readableBytes} is greater than
     *            {@code this.writableBytes}
     *            将指定的源缓冲区的数据传输到从当前writerIndex开始的缓冲区，直到源缓冲区变得不可读，并通过传输字节的数量增加writerIndex。此方法与writeBytes(ByteBuf、int、int)基本相同，只是该方法通过传输字节的数量增加了源缓冲区的readerIndex，而writeBytes(ByteBuf、int、int)则没有。
     */
    public abstract ByteBuf writeBytes(ByteBuf src);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).  This method
     * is basically same with {@link #writeBytes(ByteBuf, int, int)},
     * except that this method increases the {@code readerIndex} of the source
     * buffer by the number of the transferred bytes (= {@code length}) while
     * {@link #writeBytes(ByteBuf, int, int)} does not.
     *
     * @param length the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.writableBytes} or
     *         if {@code length} is greater then {@code src.readableBytes}
     *         将指定的源缓冲区的数据从当前的writerIndex转移到这个缓冲区，并通过传输的字节数(= length)增加writerIndex。此方法与writeBytes(ByteBuf、int、int)基本相同，只是该方法将源缓冲区的readerIndex增加了传输的字节数(=长度)，而writeBytes(ByteBuf、int、int)则没有。
     */
    public abstract ByteBuf writeBytes(ByteBuf src, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).
     *
     * @param srcIndex the first index of the source
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code srcIndex} is less than {@code 0},
     *         if {@code srcIndex + length} is greater than
     *            {@code src.capacity}, or
     *         if {@code length} is greater than {@code this.writableBytes}
     *         将指定的源缓冲区的数据传输到从当前writerIndex开始的缓冲区，并通过传输的字节数(=长度)增加writerIndex。
     */
    public abstract ByteBuf writeBytes(ByteBuf src, int srcIndex, int length);

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code src.length}).
     *
     * @throws IndexOutOfBoundsException
     *         if {@code src.length} is greater than {@code this.writableBytes}
     *         将指定的源数组的数据从当前的writerIndex转移到这个缓冲区，并通过传输的字节数(= src.length)增加writerIndex。
     */
    public abstract ByteBuf writeBytes(byte[] src);

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).将指定的源数组的数据传输到从当前writerIndex开始的缓冲区，并通过传输的字节数(=长度)增加writerIndex。
     *
     * @param srcIndex the first index of the source
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code srcIndex} is less than {@code 0},
     *         if {@code srcIndex + length} is greater than
     *            {@code src.length}, or
     *         if {@code length} is greater than {@code this.writableBytes}
     */
    public abstract ByteBuf writeBytes(byte[] src, int srcIndex, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} until the source buffer's position
     * reaches its limit, and increases the {@code writerIndex} by the
     * number of the transferred bytes.将指定的源缓冲区的数据转移到该缓冲区，从当前的writerIndex开始，直到源缓冲区的位置达到它的极限，并通过传输字节的数量增加writerIndex。
     *
     * @throws IndexOutOfBoundsException
     *         if {@code src.remaining()} is greater than
     *            {@code this.writableBytes}
     */
    public abstract ByteBuf writeBytes(ByteBuffer src);

    /**
     * Transfers the content of the specified stream to this buffer
     * starting at the current {@code writerIndex} and increases the
     * {@code writerIndex} by the number of the transferred bytes.将指定流的内容传输到从当前writerIndex开始的缓冲区，并通过传输字节的数量增加writerIndex。
     *
     * @param length the number of bytes to transfer
     *
     * @return the actual number of bytes read in from the specified stream
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.writableBytes}
     * @throws IOException
     *         if the specified stream threw an exception during I/O
     */
    public abstract int  writeBytes(InputStream in, int length) throws IOException;

    /**
     * Transfers the content of the specified channel to this buffer
     * starting at the current {@code writerIndex} and increases the
     * {@code writerIndex} by the number of the transferred bytes.将指定通道的内容传输到从当前writerIndex开始的缓冲区，并通过传输字节的数量增加writerIndex。
     *
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes read in from the specified channel
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.writableBytes}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */
    public abstract int writeBytes(ScatteringByteChannel in, int length) throws IOException;

    /**
     * Transfers the content of the specified channel starting at the given file position
     * to this buffer starting at the current {@code writerIndex} and increases the
     * {@code writerIndex} by the number of the transferred bytes.
     * This method does not modify the channel's position.将从给定文件位置开始的指定通道的内容传输到从当前writerIndex开始的缓冲区，并将writerIndex增加所传输字节的数量。此方法不修改通道的位置。
     *
     * @param position the file position at which the transfer is to begin
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes read in from the specified channel
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.writableBytes}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */
    public abstract int writeBytes(FileChannel in, long position, int length) throws IOException;

    /**
     * Fills this buffer with <tt>NUL (0x00)</tt> starting at the current
     * {@code writerIndex} and increases the {@code writerIndex} by the
     * specified {@code length}.用从当前writerIndex开始的NUL (0x00)填充这个缓冲区，并增加指定长度的writerIndex。
     *
     * @param length the number of <tt>NUL</tt>s to write to the buffer
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.writableBytes}
     */
    public abstract ByteBuf writeZero(int length);

    /**
     * Writes the specified {@link CharSequence} at the current {@code writerIndex} and increases
     * the {@code writerIndex} by the written bytes.
     * in this buffer.在当前writerIndex上写入指定的字符序列，并通过写入的字节增加writerIndex。在这个缓冲区。
     *
     * @param sequence to write
     * @param charset that should be used
     * @return the written number of bytes
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is not large enough to write the whole sequence
     */
    public abstract int writeCharSequence(CharSequence sequence, Charset charset);

    /**
     * Locates the first occurrence of the specified {@code value} in this
     * buffer.  The search takes place from the specified {@code fromIndex}
     * (inclusive)  to the specified {@code toIndex} (exclusive).
     * <p>
     * If {@code fromIndex} is greater than {@code toIndex}, the search is
     * performed in a reversed order.
     * <p>
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @return the absolute index of the first occurrence if found.
     *         {@code -1} otherwise.
     *
    在此缓冲区中查找指定值的第一个出现位置。搜索从指定的fromIndex(包括)到指定的toIndex(独占)进行。
    如果fromIndex大于toIndex，则按照相反的顺序执行搜索。
    此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract int indexOf(int fromIndex, int toIndex, byte value);

    /**
     * Locates the first occurrence of the specified {@code value} in this
     * buffer.  The search takes place from the current {@code readerIndex}
     * (inclusive) to the current {@code writerIndex} (exclusive).
     * <p>
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @return the number of bytes between the current {@code readerIndex}
     *         and the first occurrence if found. {@code -1} otherwise.
     *         在此缓冲区中查找指定值的第一个出现位置。搜索从当前的readerIndex(包括)到当前的writerIndex(排除)。
    此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract int bytesBefore(byte value);

    /**
     * Locates the first occurrence of the specified {@code value} in this
     * buffer.  The search starts from the current {@code readerIndex}
     * (inclusive) and lasts for the specified {@code length}.
     * <p>
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @return the number of bytes between the current {@code readerIndex}
     *         and the first occurrence if found. {@code -1} otherwise.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     *         在此缓冲区中查找指定值的第一个出现位置。搜索从当前的readerIndex(包括)开始，并持续指定的长度。
    此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract int bytesBefore(int length, byte value);

    /**
     * Locates the first occurrence of the specified {@code value} in this
     * buffer.  The search starts from the specified {@code index} (inclusive)
     * and lasts for the specified {@code length}.
     * <p>
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @return the number of bytes between the specified {@code index}
     *         and the first occurrence if found. {@code -1} otherwise.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code index + length} is greater than {@code this.capacity}
     *         在此缓冲区中查找指定值的第一个出现位置。搜索从指定的索引(包括)开始，并持续到指定的长度。
    此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract int bytesBefore(int index, int length, byte value);

    /**
     * Iterates over the readable bytes of this buffer with the specified {@code processor} in ascending order.使用指定的处理器以升序遍历此缓冲区的可读字节。
     *
     * @return {@code -1} if the processor iterated to or beyond the end of the readable bytes.
     *         The last-visited index If the {@link ByteProcessor#process(byte)} returned {@code false}.
     */
    public abstract int forEachByte(ByteProcessor processor);

    /**
     * Iterates over the specified area of this buffer with the specified {@code processor} in ascending order.
     * (i.e. {@code index}, {@code (index + 1)},  .. {@code (index + length - 1)})
     *
     * @return {@code -1} if the processor iterated to or beyond the end of the specified area.
     *         The last-visited index If the {@link ByteProcessor#process(byte)} returned {@code false}.
     *         按升序使用指定的处理器遍历此缓冲区的指定区域。(即index， (index + 1)(索引+长度- 1)
     */
    public abstract int forEachByte(int index, int length, ByteProcessor processor);

    /**
     * Iterates over the readable bytes of this buffer with the specified {@code processor} in descending order.使用指定的处理器按降序遍历此缓冲区的可读字节。
     *
     * @return {@code -1} if the processor iterated to or beyond the beginning of the readable bytes.
     *         The last-visited index If the {@link ByteProcessor#process(byte)} returned {@code false}.
     */
    public abstract int forEachByteDesc(ByteProcessor processor);

    /**
     * Iterates over the specified area of this buffer with the specified {@code processor} in descending order.
     * (i.e. {@code (index + length - 1)}, {@code (index + length - 2)}, ... {@code index})
     *
     *
     * @return {@code -1} if the processor iterated to or beyond the beginning of the specified area.
     *         The last-visited index If the {@link ByteProcessor#process(byte)} returned {@code false}.
     *         用指定的处理器按降序对该缓冲区的指定区域进行迭代。(即索引+长度- 1)，(索引+长度- 2)，…指数)
     */
    public abstract int forEachByteDesc(int index, int length, ByteProcessor processor);

    /**
     * Returns a copy of this buffer's readable bytes.  Modifying the content
     * of the returned buffer or this buffer does not affect each other at all.
     * This method is identical to {@code buf.copy(buf.readerIndex(), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.返回此缓冲区可读字节的副本。修改返回的缓冲区或此缓冲区的内容不会相互影响。该方法与buf.copy(buf.readerIndex()， buf.readableBytes())相同。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract ByteBuf copy();

    /**
     * Returns a copy of this buffer's sub-region.  Modifying the content of
     * the returned buffer or this buffer does not affect each other at all.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.返回此缓冲区子区域的副本。修改返回的缓冲区或此缓冲区的内容不会相互影响。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract ByteBuf copy(int index, int length);

    /**
     * Returns a slice of this buffer's readable bytes. Modifying the content
     * of the returned buffer or this buffer affects each other's content
     * while they maintain separate indexes and marks.  This method is
     * identical to {@code buf.slice(buf.readerIndex(), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Also be aware that this method will NOT call {@link #retain()} and so the
     * reference count will NOT be increased.
     * 返回缓冲区可读字节的一个切片。修改返回的缓冲区或此缓冲区的内容会影响到彼此的内容，同时它们保持独立的索引和标记。该方法与buf.slice(buf.readerIndex()、buf.readableBytes())相同。此方法不修改此缓冲区的readerIndex或writerIndex。
     还要注意，此方法不会调用retain()，因此不会增加引用计数。
     */
    public abstract ByteBuf slice();

    /**
     * Returns a retained slice of this buffer's readable bytes. Modifying the content
     * of the returned buffer or this buffer affects each other's content
     * while they maintain separate indexes and marks.  This method is
     * identical to {@code buf.slice(buf.readerIndex(), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Note that this method returns a {@linkplain #retain() retained} buffer unlike {@link #slice()}.
     * This method behaves similarly to {@code slice().retain()} except that this method may return
     * a buffer implementation that produces less garbage.
     * 返回该缓冲区可读字节的保留部分。修改返回的缓冲区或此缓冲区的内容会影响到彼此的内容，同时它们保持独立的索引和标记。该方法与buf.slice(buf.readerIndex()， buf.readableBytes())相同。此方法不修改此缓冲区的readerIndex或writerIndex。
     注意，这个方法返回一个保留的缓冲区，不像slice()。此方法的行为与slice().retain()类似，只是该方法可能返回一个产生较少垃圾的缓冲区实现。
     */
    public abstract ByteBuf retainedSlice();

    /**
     * Returns a slice of this buffer's sub-region. Modifying the content of
     * the returned buffer or this buffer affects each other's content while
     * they maintain separate indexes and marks.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Also be aware that this method will NOT call {@link #retain()} and so the
     * reference count will NOT be increased.
     * 返回缓冲区子区域的一个切片。修改返回的缓冲区的内容或这个缓冲区会影响彼此的内容，同时它们保持独立的索引和标记。此方法不修改此缓冲区的readerIndex或writerIndex。
     还要注意，此方法不会调用retain()，因此不会增加引用计数。
     */
    public abstract ByteBuf slice(int index, int length);

    /**
     * Returns a retained slice of this buffer's sub-region. Modifying the content of
     * the returned buffer or this buffer affects each other's content while
     * they maintain separate indexes and marks.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Note that this method returns a {@linkplain #retain() retained} buffer unlike {@link #slice(int, int)}.
     * This method behaves similarly to {@code slice(...).retain()} except that this method may return
     * a buffer implementation that produces less garbage.
     * 返回该缓冲区的子区域的保留部分。修改返回的缓冲区或此缓冲区的内容会影响到彼此的内容，同时它们保持独立的索引和标记。此方法不修改此缓冲区的readerIndex或writerIndex。
     注意，该方法返回一个与slice(int, int)不同的保留缓冲区。这个方法的行为类似于slice(…).retain()，只是这个方法可能返回一个产生较少垃圾的缓冲区实现。
     */
    public abstract ByteBuf retainedSlice(int index, int length);

    /**
     * Returns a buffer which shares the whole region of this buffer.
     * Modifying the content of the returned buffer or this buffer affects
     * each other's content while they maintain separate indexes and marks.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * The reader and writer marks will not be duplicated. Also be aware that this method will
     * NOT call {@link #retain()} and so the reference count will NOT be increased.
     * @return A buffer whose readable content is equivalent to the buffer returned by {@link #slice()}.
     * However this buffer will share the capacity of the underlying buffer, and therefore allows access to all of the
     * underlying content if necessary.
     * 返回一个缓冲区，该缓冲区共享该缓冲区的整个区域。修改返回的缓冲区或此缓冲区的内容会影响到彼此的内容，同时它们保持独立的索引和标记。此方法不修改此缓冲区的readerIndex或writerIndex。
    读者和作者的标记不会被复制。还要注意，此方法不会调用retain()，因此不会增加引用计数。
     */
    public abstract ByteBuf duplicate();

    /**
     * Returns a retained buffer which shares the whole region of this buffer.
     * Modifying the content of the returned buffer or this buffer affects
     * each other's content while they maintain separate indexes and marks.
     * This method is identical to {@code buf.slice(0, buf.capacity())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Note that this method returns a {@linkplain #retain() retained} buffer unlike {@link #slice(int, int)}.
     * This method behaves similarly to {@code duplicate().retain()} except that this method may return
     * a buffer implementation that produces less garbage.
     * 返回共享此缓冲区的整个区域的保留缓冲区。修改返回的缓冲区或此缓冲区的内容会影响到彼此的内容，同时它们保持独立的索引和标记。该方法与buf方法相同。片(0,buf.capacity())。此方法不修改此缓冲区的readerIndex或writerIndex。
     注意，该方法返回一个与slice(int, int)不同的保留缓冲区。此方法的行为与duplicate().retain()类似，只是该方法可能返回一个产生较少垃圾的缓冲区实现。
     */
    public abstract ByteBuf retainedDuplicate();

    /**
     * Returns the maximum number of NIO {@link ByteBuffer}s that consist this buffer.  Note that {@link #nioBuffers()}
     * or {@link #nioBuffers(int, int)} might return a less number of {@link ByteBuffer}s.
     *
     * @return {@code -1} if this buffer has no underlying {@link ByteBuffer}.
     *         the number of the underlying {@link ByteBuffer}s if this buffer has at least one underlying
     *         {@link ByteBuffer}.  Note that this method does not return {@code 0} to avoid confusion.
     *
     * @see #nioBuffer()
     * @see #nioBuffer(int, int)
     * @see #nioBuffers()
     * @see #nioBuffers(int, int)
     * 返回包含此缓冲区的NIO ByteBuffers的最大数目。注意，nioBuffers()或nioBuffers(int, int)返回的字节缓冲区可能比较少。
     */
    public abstract int nioBufferCount();

    /**
     * Exposes this buffer's readable bytes as an NIO {@link ByteBuffer}. The returned buffer
     * either share or contains the copied content of this buffer, while changing the position
     * and limit of the returned NIO buffer does not affect the indexes and marks of this buffer.
     * This method is identical to {@code buf.nioBuffer(buf.readerIndex(), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     * Please note that the returned NIO buffer will not see the changes of this buffer if this buffer
     * is a dynamic buffer and it adjusted its capacity.
     *
     * @throws UnsupportedOperationException
     *         if this buffer cannot create a {@link ByteBuffer} that shares the content with itself
     *
     * @see #nioBufferCount()
     * @see #nioBuffers()
     * @see #nioBuffers(int, int)
     * 将此缓冲区的可读字节公开为NIO ByteBuffer。返回的缓冲区共享或包含该缓冲区的复制内容，同时更改返回的NIO缓冲区的位置和限制不会影响该缓冲区的索引和标记。该方法与buf.nioBuffer(buf.readerIndex()， buf.readableBytes())相同。此方法不修改此缓冲区的readerIndex或writerIndex。请注意，如果这个缓冲区是动态缓冲区，并且它调整了它的容量，那么返回的NIO缓冲区将不会看到这个缓冲区的变化。
     */
    public abstract ByteBuffer nioBuffer();

    /**
     * Exposes this buffer's sub-region as an NIO {@link ByteBuffer}. The returned buffer
     * either share or contains the copied content of this buffer, while changing the position
     * and limit of the returned NIO buffer does not affect the indexes and marks of this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     * Please note that the returned NIO buffer will not see the changes of this buffer if this buffer
     * is a dynamic buffer and it adjusted its capacity.
     *
     * @throws UnsupportedOperationException
     *         if this buffer cannot create a {@link ByteBuffer} that shares the content with itself
     *
     * @see #nioBufferCount()
     * @see #nioBuffers()
     * @see #nioBuffers(int, int)
     * 将该缓冲区的子区域公开为NIO ByteBuffer。返回的缓冲区共享或包含该缓冲区的复制内容，同时更改返回的NIO缓冲区的位置和限制不会影响该缓冲区的索引和标记。此方法不修改此缓冲区的readerIndex或writerIndex。请注意，如果这个缓冲区是动态缓冲区，并且它调整了它的容量，那么返回的NIO缓冲区将不会看到这个缓冲区的变化。
     */
    public abstract ByteBuffer nioBuffer(int index, int length);

    /**
     * Internal use only: Exposes the internal NIO buffer.内部使用:公开内部NIO缓冲区。
     */
    public abstract ByteBuffer internalNioBuffer(int index, int length);

    /**
     * Exposes this buffer's readable bytes as an NIO {@link ByteBuffer}'s. The returned buffer
     * either share or contains the copied content of this buffer, while changing the position
     * and limit of the returned NIO buffer does not affect the indexes and marks of this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     * Please note that the returned NIO buffer will not see the changes of this buffer if this buffer
     * is a dynamic buffer and it adjusted its capacity.
     *
     *
     * @throws UnsupportedOperationException
     *         if this buffer cannot create a {@link ByteBuffer} that shares the content with itself
     *
     * @see #nioBufferCount()
     * @see #nioBuffer()
     * @see #nioBuffer(int, int)
     * 将此缓冲区的可读字节公开为NIO ByteBuffer的字节。返回的缓冲区共享或包含该缓冲区的复制内容，同时更改返回的NIO缓冲区的位置和限制不会影响该缓冲区的索引和标记。此方法不修改此缓冲区的readerIndex或writerIndex。请注意，如果这个缓冲区是动态缓冲区，并且它调整了它的容量，那么返回的NIO缓冲区将不会看到这个缓冲区的变化。
     */
    public abstract ByteBuffer[] nioBuffers();

    /**
     * Exposes this buffer's bytes as an NIO {@link ByteBuffer}'s for the specified index and length
     * The returned buffer either share or contains the copied content of this buffer, while changing
     * the position and limit of the returned NIO buffer does not affect the indexes and marks of this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of this buffer. Please note that the
     * returned NIO buffer will not see the changes of this buffer if this buffer is a dynamic
     * buffer and it adjusted its capacity.
     *
     * @throws UnsupportedOperationException
     *         if this buffer cannot create a {@link ByteBuffer} that shares the content with itself
     *
     * @see #nioBufferCount()
     * @see #nioBuffer()
     * @see #nioBuffer(int, int)
     *
    将该缓冲区的字节作为指定索引和长度的NIO ByteBuffer的字节公开，返回的缓冲区共享或包含该缓冲区复制的内容，而更改返回的NIO缓冲区的位置和限制不会影响该缓冲区的索引和标记。此方法不修改此缓冲区的readerIndex或writerIndex。请注意，如果这个缓冲区是动态缓冲区，并且它调整了它的容量，那么返回的NIO缓冲区将不会看到这个缓冲区的变化。
     */
    public abstract ByteBuffer[] nioBuffers(int index, int length);

    /**
     * Returns {@code true} if and only if this buffer has a backing byte array.
     * If this method returns true, you can safely call {@link #array()} and
     * {@link #arrayOffset()}.
     * 返回true，如果且仅当此缓冲区具有支持字节数组。如果这个方法返回true，您可以安全地调用array()和arrayOffset()。
     */
    public abstract boolean hasArray();

    /**
     * Returns the backing byte array of this buffer.返回此缓冲区的支持字节数组。
     *
     * @throws UnsupportedOperationException
     *         if there no accessible backing byte array
     */
    public abstract byte[] array();

    /**
     * Returns the offset of the first byte within the backing byte array of
     * this buffer.返回该缓冲区支持字节数组中第一个字节的偏移量。
     *
     * @throws UnsupportedOperationException
     *         if there no accessible backing byte array
     */
    public abstract int arrayOffset();

    /**
     * Returns {@code true} if and only if this buffer has a reference to the low-level memory address that points
     * to the backing data.如果且仅当此缓冲区引用指向支持数据的低级内存地址时返回true。
     */
    public abstract boolean hasMemoryAddress();

    /**
     * Returns the low-level memory address that point to the first byte of ths backing data.返回指向支持数据的第一个字节的低级内存地址。
     *
     * @throws UnsupportedOperationException
     *         if this buffer does not support accessing the low-level memory address
     */
    public abstract long memoryAddress();

    /**
     * Decodes this buffer's readable bytes into a string with the specified
     * character set name.  This method is identical to
     * {@code buf.toString(buf.readerIndex(), buf.readableBytes(), charsetName)}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws UnsupportedCharsetException
     *         if the specified character set name is not supported by the
     *         current VM
     *         将缓冲区的可读字节解码为具有指定字符集名称的字符串。此方法与buf.toString(buf.readerIndex()、buf.readableBytes()、charsetName)相同。此方法不修改此缓冲区的readerIndex或writerIndex。
     */
    public abstract String toString(Charset charset);

    /**
     * Decodes this buffer's sub-region into a string with the specified
     * character set.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.使用指定的字符集将缓冲区的子区域解码成字符串。此方法不修改该缓冲区的readerIndex或writerIndex。
     */
    public abstract String toString(int index, int length, Charset charset);

    /**
     * Returns a hash code which was calculated from the content of this
     * buffer.  If there's a byte array which is
     * {@linkplain #equals(Object) equal to} this array, both arrays should
     * return the same value.
     */
    @Override
    public abstract int hashCode();

    /**
     * Determines if the content of the specified buffer is identical to the
     * content of this array.  'Identical' here means:
     * <ul>
     * <li>the size of the contents of the two buffers are same and</li>
     * <li>every single byte of the content of the two buffers are same.</li>
     * </ul>
     * Please note that it does not compare {@link #readerIndex()} nor
     * {@link #writerIndex()}.  This method also returns {@code false} for
     * {@code null} and an object which is not an instance of
     * {@link ByteBuf} type.
     */
    @Override
    public abstract boolean equals(Object obj);

    /**
     * Compares the content of the specified buffer to the content of this
     * buffer. Comparison is performed in the same manner with the string
     * comparison functions of various languages such as {@code strcmp},
     * {@code memcmp} and {@link String#compareTo(String)}.将指定缓冲区的内容与此缓冲区的内容进行比较。与各种语言的字符串比较函数(如strcmp、memcmp和string . compareto)进行比较的方式相同。
     */
    @Override
    public abstract int compareTo(ByteBuf buffer);

    /**
     * Returns the string representation of this buffer.  This method does not
     * necessarily return the whole content of the buffer but returns
     * the values of the key properties such as {@link #readerIndex()},
     * {@link #writerIndex()} and {@link #capacity()}.
     */
    @Override
    public abstract String toString();

    @Override
    public abstract ByteBuf retain(int increment);

    @Override
    public abstract ByteBuf retain();

    @Override
    public abstract ByteBuf touch();

    @Override
    public abstract ByteBuf touch(Object hint);
}
