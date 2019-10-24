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

import io.netty.handler.codec.http.HttpContent;

import java.util.List;

/**
 * This decoder will decode Body and can handle POST BODY.
 *
 * You <strong>MUST</strong> call {@link #destroy()} after completion to release all resources.该译码器将对主体进行解码，可以处理后体。完成后，必须调用destroy()来释放所有资源。
 */
public interface InterfaceHttpPostRequestDecoder {
    /**
     * True if this request is a Multipart request 如果此请求是多部分请求，则为真
     *
     * @return True if this request is a Multipart request
     */
    boolean isMultipart();

    /**
     * Set the amount of bytes after which read bytes in the buffer should be discarded.
     * Setting this lower gives lower memory usage but with the overhead of more memory copies.
     * Use {@code 0} to disable it.设置缓冲区中应丢弃的读字节数。设置这个较低的值会降低内存使用量，但会增加内存副本的开销。使用0禁用它。
     */
    void setDiscardThreshold(int discardThreshold);

    /**
     * Return the threshold in bytes after which read data in the buffer should be discarded.返回以字节为单位的阈值，然后应该丢弃缓冲区中的读取数据。
     */
    int getDiscardThreshold();

    /**
     * This getMethod returns a List of all HttpDatas from body.<br>
     *
     * If chunked, all chunks must have been offered using offer() getMethod. If
     * not, NotEnoughDataDecoderException will be raised.
     *
     * @return the list of HttpDatas from Body part for POST getMethod
     * @throws HttpPostRequestDecoder.NotEnoughDataDecoderException
     *             Need more chunks
     *             这个getMethod返回来自body的所有HttpDatas的列表。如果分块，所有块都必须使用offer() getMethod提供。如果没有，则不会引发任何datadecoderexception。
     */
    List<InterfaceHttpData> getBodyHttpDatas();

    /**
     * This getMethod returns a List of all HttpDatas with the given name from
     * body.<br>
     *
     * If chunked, all chunks must have been offered using offer() getMethod. If
     * not, NotEnoughDataDecoderException will be raised.
     *
     * @return All Body HttpDatas with the given name (ignore case)
     * @throws HttpPostRequestDecoder.NotEnoughDataDecoderException
     *             need more chunks
     *             这个getMethod返回一个所有HttpDatas的列表，其中包含来自body的给定名称。如果分块，所有块都必须使用offer() getMethod提供。如果没有，则不会引发任何datadecoderexception。
     */
    List<InterfaceHttpData> getBodyHttpDatas(String name);

    /**
     * This getMethod returns the first InterfaceHttpData with the given name from
     * body.<br>
     *
     * If chunked, all chunks must have been offered using offer() getMethod. If
     * not, NotEnoughDataDecoderException will be raised.
     *
     * @return The first Body InterfaceHttpData with the given name (ignore
     *         case)
     * @throws HttpPostRequestDecoder.NotEnoughDataDecoderException
     *             need more chunks
     *             这个getMethod返回第一个接口httpdata，该接口具有来自body的给定名称。如果分块，所有块都必须使用offer() getMethod提供。如果没有，则不会引发任何datadecoderexception。
     */
    InterfaceHttpData getBodyHttpData(String name);

    /**
     * Initialized the internals from a new chunk
     *
     * @param content
     *            the new received chunk
     * @throws HttpPostRequestDecoder.ErrorDataDecoderException
     *             if there is a problem with the charset decoding or other
     *             errors
     */
    InterfaceHttpPostRequestDecoder offer(HttpContent content);

    /**
     * True if at current getStatus, there is an available decoded
     * InterfaceHttpData from the Body.如果在当前的getStatus中，有来自主体的可用解码接口httpdata。这个getMethod适用于组块请求而不是组块请求。
     *
     * This getMethod works for chunked and not chunked request.
     *
     * @return True if at current getStatus, there is a decoded InterfaceHttpData
     * @throws HttpPostRequestDecoder.EndOfDataDecoderException
     *             No more data will be available
     */
    boolean hasNext();

    /**
     * Returns the next available InterfaceHttpData or null if, at the time it
     * is called, there is no more available InterfaceHttpData. A subsequent
     * call to offer(httpChunk) could enable more data.
     *
     * Be sure to call {@link InterfaceHttpData#release()} after you are done
     * with processing to make sure to not leak any resources
     *
     * @return the next available InterfaceHttpData or null if none
     * @throws HttpPostRequestDecoder.EndOfDataDecoderException
     *             No more data will be available
     *             返回下一个可用的InterfaceHttpData或null，如果在调用该接口时没有更多可用的InterfaceHttpData。后续调用提供(httpChunk)可以启用更多数据。完成处理后，一定要调用InterfaceHttpData.release()，以确保不会泄漏任何资源
     */
    InterfaceHttpData next();

    /**
     * Returns the current InterfaceHttpData if currently in decoding status,
     * meaning all data are not yet within, or null if there is no InterfaceHttpData
     * currently in decoding status (either because none yet decoded or none currently partially
     * decoded). Full decoded ones are accessible through hasNext() and next() methods.
     *
     * @return the current InterfaceHttpData if currently in decoding status or null if none.
     * 如果当前处于解码状态，则返回当前InterfaceHttpData，这意味着所有数据都不在内部，如果当前没有InterfaceHttpData处于解码状态，则返回null(因为还没有解码，或者当前没有部分解码)。完整的解码可以通过hasNext()和next()方法访问。
     */
    InterfaceHttpData currentPartialHttpData();

    /**
     * Destroy the {@link InterfaceHttpPostRequestDecoder} and release all it resources. After this method
     * was called it is not possible to operate on it anymore.销毁接口httppostrequestdecoder并释放所有it资源。在这个方法被调用之后，就不可能再对它进行操作了。
     */
    void destroy();

    /**
     * Clean all HttpDatas (on Disk) for the current request.清除当前请求的所有HttpDatas(磁盘上)。
     */
    void cleanFiles();

    /**
     * Remove the given FileUpload from the list of FileUploads to clean 从文件上传列表中删除给定的文件上传以进行清理
     */
    void removeHttpDataFromClean(InterfaceHttpData data);
}
