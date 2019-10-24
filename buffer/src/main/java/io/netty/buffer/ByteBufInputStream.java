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

import io.netty.util.ReferenceCounted;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * An {@link InputStream} which reads data from a {@link ByteBuf}.
 * <p>
 * A read operation against this stream will occur at the {@code readerIndex}
 * of its underlying buffer and the {@code readerIndex} will increase during
 * the read operation.  Please note that it only reads up to the number of
 * readable bytes determined at the moment of construction.  Therefore,
 * updating {@link ByteBuf#writerIndex()} will not affect the return
 * value of {@link #available()}.
 * <p>
 * This stream implements {@link DataInput} for your convenience.
 * The endianness of the stream is not always big endian but depends on
 * the endianness of the underlying buffer.
 * 从ByteBuf读取数据的InputStream。
 对该流的读操作将在其基础缓冲区的readerIndex处发生，读操作期间readerIndex将增加。请注意，它只读取在构造时确定的可读字节数。因此，更新ByteBuf.writerIndex()不会影响available()的返回值。
 为了方便您，这个流实现了DataInput。流的偶然性并不总是很大，但取决于底层缓冲区的偶然性。
 *
 * @see ByteBufOutputStream
 */
public class ByteBufInputStream extends InputStream implements DataInput {
    private final ByteBuf buffer;
    private final int startIndex;
    private final int endIndex;
    private boolean closed;
    /**
     * To preserve backwards compatibility (which didn't transfer ownership) we support a conditional flag which
     * indicates if {@link #buffer} should be released when this {@link InputStream} is closed.
     * However in future releases ownership should always be transferred and callers of this class should call
     * {@link ReferenceCounted#retain()} if necessary.
     * 为了保持向后兼容性(不转移所有权)，我们支持一个条件标志，它指示当这个InputStream关闭时是否应该释放缓冲区。然而，在将来的版本中，所有权应该总是被转移，该类的调用者应该调用referencecount .retain()，如果需要的话。
     */
    private final boolean releaseOnClose;

    /**
     * Creates a new stream which reads data from the specified {@code buffer}
     * starting at the current {@code readerIndex} and ending at the current
     * {@code writerIndex}.
     * @param buffer The buffer which provides the content for this {@link InputStream}.
     *               创建一个新的流，该流从指定的缓冲区中读取数据，从当前的readerIndex开始，到当前的writerIndex结束。
     */
    public ByteBufInputStream(ByteBuf buffer) {
        this(buffer, buffer.readableBytes());
    }

    /**
     * Creates a new stream which reads data from the specified {@code buffer}
     * starting at the current {@code readerIndex} and ending at
     * {@code readerIndex + length}.
     * @param buffer The buffer which provides the content for this {@link InputStream}.
     * @param length The length of the buffer to use for this {@link InputStream}.
     * @throws IndexOutOfBoundsException
     *         if {@code readerIndex + length} is greater than
     *            {@code writerIndex}
     *            创建一个新流，该流从指定的缓冲区读取数据，从当前读写器索引开始，以读写器索引+长度结束。
     */
    public ByteBufInputStream(ByteBuf buffer, int length) {
        this(buffer, length, false);
    }

    /**
     * Creates a new stream which reads data from the specified {@code buffer}
     * starting at the current {@code readerIndex} and ending at the current
     * {@code writerIndex}.
     * @param buffer The buffer which provides the content for this {@link InputStream}.
     * @param releaseOnClose {@code true} means that when {@link #close()} is called then {@link ByteBuf#release()} will
     *                       be called on {@code buffer}.
     *                                   创建一个新的流，该流从指定的缓冲区中读取数据，从当前的readerIndex开始，到当前的writerIndex结束。
     */
    public ByteBufInputStream(ByteBuf buffer, boolean releaseOnClose) {
        this(buffer, buffer.readableBytes(), releaseOnClose);
    }

    /**
     * Creates a new stream which reads data from the specified {@code buffer}
     * starting at the current {@code readerIndex} and ending at
     * {@code readerIndex + length}.
     * @param buffer The buffer which provides the content for this {@link InputStream}.
     * @param length The length of the buffer to use for this {@link InputStream}.
     * @param releaseOnClose {@code true} means that when {@link #close()} is called then {@link ByteBuf#release()} will
     *                       be called on {@code buffer}.
     * @throws IndexOutOfBoundsException
     *         if {@code readerIndex + length} is greater than
     *            {@code writerIndex}
     *            创建一个新流，该流从指定的缓冲区读取数据，从当前读写器索引开始，以读写器索引+长度结束。
     */
    public ByteBufInputStream(ByteBuf buffer, int length, boolean releaseOnClose) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        if (length < 0) {
            if (releaseOnClose) {
                buffer.release();
            }
            throw new IllegalArgumentException("length: " + length);
        }
        if (length > buffer.readableBytes()) {
            if (releaseOnClose) {
                buffer.release();
            }
            throw new IndexOutOfBoundsException("Too many bytes to be read - Needs "
                    + length + ", maximum is " + buffer.readableBytes());
        }

        this.releaseOnClose = releaseOnClose;
        this.buffer = buffer;
        startIndex = buffer.readerIndex();
        endIndex = startIndex + length;
        buffer.markReaderIndex();
    }

    /**
     * Returns the number of read bytes by this stream so far.返回到目前为止该流所读取的字节数。
     */
    public int readBytes() {
        return buffer.readerIndex() - startIndex;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            // The Closable interface says "If the stream is already closed then invoking this method has no effect."
            if (releaseOnClose && !closed) {
                closed = true;
                buffer.release();
            }
        }
    }

    @Override
    public int available() throws IOException {
        return endIndex - buffer.readerIndex();
    }

    @Override
    public void mark(int readlimit) {
        buffer.markReaderIndex();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public int read() throws IOException {
        if (!buffer.isReadable()) {
            return -1;
        }
        return buffer.readByte() & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int available = available();
        if (available == 0) {
            return -1;
        }

        len = Math.min(available, len);
        buffer.readBytes(b, off, len);
        return len;
    }

    @Override
    public void reset() throws IOException {
        buffer.resetReaderIndex();
    }

    @Override
    public long skip(long n) throws IOException {
        if (n > Integer.MAX_VALUE) {
            return skipBytes(Integer.MAX_VALUE);
        } else {
            return skipBytes((int) n);
        }
    }

    @Override
    public boolean readBoolean() throws IOException {
        checkAvailable(1);
        return read() != 0;
    }

    @Override
    public byte readByte() throws IOException {
        if (!buffer.isReadable()) {
            throw new EOFException();
        }
        return buffer.readByte();
    }

    @Override
    public char readChar() throws IOException {
        return (char) readShort();
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        checkAvailable(len);
        buffer.readBytes(b, off, len);
    }

    @Override
    public int readInt() throws IOException {
        checkAvailable(4);
        return buffer.readInt();
    }

    private final StringBuilder lineBuf = new StringBuilder();

    @Override
    public String readLine() throws IOException {
        lineBuf.setLength(0);

        loop: while (true) {
            if (!buffer.isReadable()) {
                return lineBuf.length() > 0 ? lineBuf.toString() : null;
            }

            int c = buffer.readUnsignedByte();
            switch (c) {
                case '\n':
                    break loop;

                case '\r':
                    if (buffer.isReadable() && (char) buffer.getUnsignedByte(buffer.readerIndex()) == '\n') {
                        buffer.skipBytes(1);
                    }
                    break loop;

                default:
                    lineBuf.append((char) c);
            }
        }

        return lineBuf.toString();
    }

    @Override
    public long readLong() throws IOException {
        checkAvailable(8);
        return buffer.readLong();
    }

    @Override
    public short readShort() throws IOException {
        checkAvailable(2);
        return buffer.readShort();
    }

    @Override
    public String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return readByte() & 0xff;
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return readShort() & 0xffff;
    }

    @Override
    public int skipBytes(int n) throws IOException {
        int nBytes = Math.min(available(), n);
        buffer.skipBytes(nBytes);
        return nBytes;
    }

    private void checkAvailable(int fieldSize) throws IOException {
        if (fieldSize < 0) {
            throw new IndexOutOfBoundsException("fieldSize cannot be a negative number");
        }
        if (fieldSize > available()) {
            throw new EOFException("fieldSize is too long! Length is " + fieldSize
                    + ", but maximum is " + available());
        }
    }
}
