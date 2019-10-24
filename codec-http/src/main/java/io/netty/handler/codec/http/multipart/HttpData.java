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
package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Extended interface for InterfaceHttpData 扩展接口InterfaceHttpData
 */
public interface HttpData extends InterfaceHttpData, ByteBufHolder {

    /**
     * Returns the maxSize for this HttpData.返回此HttpData的最大大小。
     */
    long getMaxSize();

    /**
     * Set the maxSize for this HttpData. When limit will be reached, an exception will be raised.
     * Setting it to (-1) means no limitation.
     *
     * By default, to be set from the HttpDataFactory.
     * 设置此HttpData的最大大小。当达到极限时，会提出异常。将其设置为(-1)意味着没有限制。默认情况下，从HttpDataFactory设置。
     */
    void setMaxSize(long maxSize);

    /**
     * Check if the new size is not reaching the max limit allowed.
     * The limit is always computed in term of bytes.
     * 检查新尺寸是否未达到允许的最大值。极限总是以字节为单位计算的。
     */
    void checkSize(long newSize) throws IOException;

    /**
     * Set the content from the ChannelBuffer (erase any previous data)
     * 从ChannelBuffer设置内容(删除以前的数据)
     *
     * @param buffer
     *            must be not null
     * @throws IOException
     */
    void setContent(ByteBuf buffer) throws IOException;

    /**
     * Add the content from the ChannelBuffer 从ChannelBuffer添加内容
     *
     * @param buffer
     *            must be not null except if last is set to False
     * @param last
     *            True of the buffer is the last one
     * @throws IOException
     */
    void addContent(ByteBuf buffer, boolean last) throws IOException;

    /**
     * Set the content from the file (erase any previous data) 从文件中设置内容(删除以前的数据)
     *
     * @param file
     *            must be not null
     * @throws IOException
     */
    void setContent(File file) throws IOException;

    /**
     * Set the content from the inputStream (erase any previous data) 从inputStream中设置内容(删除以前的数据)
     *
     * @param inputStream
     *            must be not null
     * @throws IOException
     */
    void setContent(InputStream inputStream) throws IOException;

    /**
     *
     * @return True if the InterfaceHttpData is completed (all data are stored)
     */
    boolean isCompleted();

    /**
     * Returns the size in byte of the InterfaceHttpData 返回InterfaceHttpData的字节大小
     *
     * @return the size of the InterfaceHttpData
     */
    long length();

    /**
     * Returns the defined length of the HttpData.
     *
     * If no Content-Length is provided in the request, the defined length is
     * always 0 (whatever during decoding or in final state).
     *
     * If Content-Length is provided in the request, this is this given defined length.
     * This value does not change, whatever during decoding or in the final state.
     *
     * This method could be used for instance to know the amount of bytes transmitted for
     * one particular HttpData, for example one {@link FileUpload} or any known big {@link Attribute}.
     *
     * @return the defined length of the HttpData
     * 返回已定义的HttpData长度。如果请求中没有提供内容长度，则定义的长度总是0(无论在解码期间还是在最终状态)。如果请求中提供了内容长度，这就是给定的定义长度。无论在解码过程中还是在最终状态中，这个值都不会改变。例如，这个方法可以用来知道一个特定HttpData传输的字节数，例如一个FileUpload或任何已知的大属性。
     */
    long definedLength();

    /**
     * Deletes the underlying storage for a file item, including deleting any
     * associated temporary disk file.删除文件项的基础存储，包括删除任何关联的临时磁盘文件。
     */
    void delete();

    /**
     * Returns the contents of the file item as an array of bytes.以字节数组的形式返回文件项的内容。
     *
     * @return the contents of the file item as an array of bytes.
     * @throws IOException
     */
    byte[] get() throws IOException;

    /**
     * Returns the content of the file item as a ByteBuf 以ByteBuf的形式返回文件项的内容
     *
     * @return the content of the file item as a ByteBuf
     * @throws IOException
     */
    ByteBuf getByteBuf() throws IOException;

    /**
     * Returns a ChannelBuffer for the content from the current position with at
     * most length read bytes, increasing the current position of the Bytes
     * read. Once it arrives at the end, it returns an EMPTY_BUFFER and it
     * resets the current position to 0.返回当前位置的内容的ChannelBuffer，读取字节的长度最多，增加读取字节的当前位置。当它到达末尾时，它返回一个EMPTY_BUFFER，并将当前位置重置为0。
     *
     * @return a ChannelBuffer for the content from the current position or an
     *         EMPTY_BUFFER if there is no more data to return
     */
    ByteBuf getChunk(int length) throws IOException;

    /**
     * Returns the contents of the file item as a String, using the default
     * character encoding.使用默认字符编码，以字符串的形式返回文件项的内容。
     *
     * @return the contents of the file item as a String, using the default
     *         character encoding.
     * @throws IOException
     */
    String getString() throws IOException;

    /**
     * Returns the contents of the file item as a String, using the specified
     * charset.使用指定的字符集以字符串的形式返回文件项的内容。
     *
     * @param encoding
     *            the charset to use
     * @return the contents of the file item as a String, using the specified
     *         charset.
     * @throws IOException
     */
    String getString(Charset encoding) throws IOException;

    /**
     * Set the Charset passed by the browser if defined 设置浏览器传递的字符集(如果定义的话)
     *
     * @param charset
     *            Charset to set - must be not null
     */
    void setCharset(Charset charset);

    /**
     * Returns the Charset passed by the browser or null if not defined. 返回浏览器传递的字符集，如果没有定义则返回null。
     *
     * @return the Charset passed by the browser or null if not defined.
     */
    Charset getCharset();

    /**
     * A convenience getMethod to write an uploaded item to disk. If a previous one
     * exists, it will be deleted. Once this getMethod is called, if successful,
     * the new file will be out of the cleaner of the factory that creates the
     * original InterfaceHttpData object.一种方便的getMethod，用于将上传的项写入磁盘。如果之前的一个存在，它将被删除。一旦调用了这个getMethod，如果成功，新文件将离开创建原始InterfaceHttpData对象的工厂。
     *
     * @param dest
     *            destination file - must be not null
     * @return True if the write is successful
     * @throws IOException
     */
    boolean renameTo(File dest) throws IOException;

    /**
     * Provides a hint as to whether or not the file contents will be read from
     * memory.提供有关是否从内存中读取文件内容的提示。
     *
     * @return True if the file contents is in memory.
     */
    boolean isInMemory();

    /**
     *
     * @return the associated File if this data is represented in a file
     * @exception IOException
     *                if this data is not represented by a file
     */
    File getFile() throws IOException;

    @Override
    HttpData copy();

    @Override
    HttpData duplicate();

    @Override
    HttpData retainedDuplicate();

    @Override
    HttpData replace(ByteBuf content);

    @Override
    HttpData retain();

    @Override
    HttpData retain(int increment);

    @Override
    HttpData touch();

    @Override
    HttpData touch(Object hint);
}
