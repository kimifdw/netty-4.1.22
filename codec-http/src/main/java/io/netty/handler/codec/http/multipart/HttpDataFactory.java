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

import io.netty.handler.codec.http.HttpRequest;

import java.nio.charset.Charset;

/**
 * Interface to enable creation of InterfaceHttpData objects 接口，以支持创建InterfaceHttpData对象
 */
public interface HttpDataFactory {

    /**
     * To set a max size limitation on fields. Exceeding it will generate an ErrorDataDecoderException.
     * A value of -1 means no limitation (default).设置字段的最大大小限制。超过该值将生成ErrorDataDecoderException。值为-1表示没有限制(默认)。
     */
    void setMaxLimit(long max);

    /**
     *
     * @param request associated request
     * @return a new Attribute with no value
     */
    Attribute createAttribute(HttpRequest request, String name);

    /**
     * @param request associated request
     * @param name name of the attribute
     * @param definedSize defined size from request for this attribute
     * @return a new Attribute with no value
     */
    Attribute createAttribute(HttpRequest request, String name, long definedSize);

    /**
     * @param request associated request
     * @return a new Attribute
     */
    Attribute createAttribute(HttpRequest request, String name, String value);

    /**
     * @param request associated request
     * @param size the size of the Uploaded file
     * @return a new FileUpload
     */
    FileUpload createFileUpload(HttpRequest request, String name, String filename,
                                String contentType, String contentTransferEncoding, Charset charset,
                                long size);

    /**
     * Remove the given InterfaceHttpData from clean list (will not delete the file, except if the file
     * is still a temporary one as setup at construction) 从clean列表中删除给定的InterfaceHttpData(不会删除该文件，除非该文件仍然是一个临时文件，如在构建时设置)
     * @param request associated request
     */
    void removeHttpDataFromClean(HttpRequest request, InterfaceHttpData data);

    /**
     * Remove all InterfaceHttpData from virtual File storage from clean list for the request 从清除请求列表中删除虚拟文件存储中的所有InterfaceHttpData
     *
     * @param request associated request
     */
    void cleanRequestHttpData(HttpRequest request);

    /**
     * Remove all InterfaceHttpData from virtual File storage from clean list for all requests 从清除所有请求的列表中删除虚拟文件存储中的所有InterfaceHttpData
     */
    void cleanAllHttpData();

    /**
     * @deprecated Use {@link #cleanRequestHttpData(HttpRequest)} instead.
     */
    @Deprecated
    void cleanRequestHttpDatas(HttpRequest request);

    /**
     * @deprecated Use {@link #cleanAllHttpData()} instead.
     */
    @Deprecated
    void cleanAllHttpDatas();
}
