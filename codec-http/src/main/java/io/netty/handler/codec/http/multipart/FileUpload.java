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

/**
 * FileUpload interface that could be in memory, on temporary file or any other implementations.
 *
 * Most methods are inspired from java.io.File API.可以在内存、临时文件或任何其他实现上的FileUpload接口。大多数方法都是受java.io的启发。文件的API。
 */
public interface FileUpload extends HttpData {
    /**
     * Returns the original filename in the client's filesystem,
     * as provided by the browser (or other client software).按照浏览器(或其他客户机软件)提供的方法，返回客户机文件系统中的原始文件名。
     * @return the original filename
     */
    String getFilename();

    /**
     * Set the original filename
     */
    void setFilename(String filename);

    /**
     * Set the Content Type passed by the browser if defined 如果定义了内容类型，则设置浏览器传递的内容类型
     * @param contentType Content Type to set - must be not null
     */
    void setContentType(String contentType);

    /**
     * Returns the content type passed by the browser or null if not defined. 返回浏览器传递的内容类型，如果没有定义则返回null。
     * @return the content type passed by the browser or null if not defined.
     */
    String getContentType();

    /**
     * Set the Content-Transfer-Encoding type from String as 7bit, 8bit or binary 将字符串中的内容传输编码类型设置为7bit、8bit或二进制
     */
    void setContentTransferEncoding(String contentTransferEncoding);

    /**
     * Returns the Content-Transfer-Encoding
     * @return the Content-Transfer-Encoding
     */
    String getContentTransferEncoding();

    @Override
    FileUpload copy();

    @Override
    FileUpload duplicate();

    @Override
    FileUpload retainedDuplicate();

    @Override
    FileUpload replace(ByteBuf content);

    @Override
    FileUpload retain();

    @Override
    FileUpload retain(int increment);

    @Override
    FileUpload touch();

    @Override
    FileUpload touch(Object hint);
}
