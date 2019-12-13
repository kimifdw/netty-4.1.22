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
package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DateFormatter;
import io.netty.handler.codec.Headers;
import io.netty.handler.codec.HeadersUtils;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static io.netty.util.AsciiString.contentEquals;
import static io.netty.util.AsciiString.contentEqualsIgnoreCase;
import static io.netty.util.AsciiString.trim;
import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * Provides the constants for the standard HTTP header names and values and
 * commonly used utility methods that accesses an {@link HttpMessage}.
 * 提供标准HTTP头名和值的常量，以及访问HttpMessage的常用实用工具方法。
 */
public abstract class HttpHeaders implements Iterable<Map.Entry<String, String>> {
    /**
     * @deprecated Use {@link EmptyHttpHeaders#INSTANCE}.
     * <p>
     * The instance is instantiated here to break the cyclic static initialization between {@link EmptyHttpHeaders} and
     * {@link HttpHeaders}. The issue is that if someone accesses {@link EmptyHttpHeaders#INSTANCE} before
     * {@link HttpHeaders#EMPTY_HEADERS} then {@link HttpHeaders#EMPTY_HEADERS} will be {@code null}.
     * 在这里实例化是为了打破EmptyHttpHeaders和HttpHeaders之间的循环静态初始化。问题是如果有人访问EmptyHttpHeaders。在EMPTY_HEADERS之前的实例，然后EMPTY_HEADERS将为空。
     */
    @Deprecated
    public static final HttpHeaders EMPTY_HEADERS = EmptyHttpHeaders.instance();

    /**
     * @deprecated Use {@link HttpHeaderNames} instead.
     *
     * Standard HTTP header names.
     */
    @Deprecated
    public static final class Names {
        /**
         * {@code "Accept"}
         */
        public static final String ACCEPT = "Accept";
        /**
         * {@code "Accept-Charset"}
         */
        public static final String ACCEPT_CHARSET = "Accept-Charset";
        /**
         * {@code "Accept-Encoding"}
         */
        public static final String ACCEPT_ENCODING = "Accept-Encoding";
        /**
         * {@code "Accept-Language"}
         */
        public static final String ACCEPT_LANGUAGE = "Accept-Language";
        /**
         * {@code "Accept-Ranges"}
         */
        public static final String ACCEPT_RANGES = "Accept-Ranges";
        /**
         * {@code "Accept-Patch"}
         */
        public static final String ACCEPT_PATCH = "Accept-Patch";
        /**
         * {@code "Access-Control-Allow-Credentials"}
         */
        public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
        /**
         * {@code "Access-Control-Allow-Headers"}
         */
        public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
        /**
         * {@code "Access-Control-Allow-Methods"}
         */
        public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
        /**
         * {@code "Access-Control-Allow-Origin"}
         */
        public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
        /**
         * {@code "Access-Control-Expose-Headers"}
         */
        public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
        /**
         * {@code "Access-Control-Max-Age"}
         */
        public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
        /**
         * {@code "Access-Control-Request-Headers"}
         */
        public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
        /**
         * {@code "Access-Control-Request-Method"}
         */
        public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
        /**
         * {@code "Age"}
         */
        public static final String AGE = "Age";
        /**
         * {@code "Allow"}
         */
        public static final String ALLOW = "Allow";
        /**
         * {@code "Authorization"}
         */
        public static final String AUTHORIZATION = "Authorization";
        /**
         * {@code "Cache-Control"}
         */
        public static final String CACHE_CONTROL = "Cache-Control";
        /**
         * {@code "Connection"}
         */
        public static final String CONNECTION = "Connection";
        /**
         * {@code "Content-Base"}
         */
        public static final String CONTENT_BASE = "Content-Base";
        /**
         * {@code "Content-Encoding"}
         */
        public static final String CONTENT_ENCODING = "Content-Encoding";
        /**
         * {@code "Content-Language"}
         */
        public static final String CONTENT_LANGUAGE = "Content-Language";
        /**
         * {@code "Content-Length"}
         */
        public static final String CONTENT_LENGTH = "Content-Length";
        /**
         * {@code "Content-Location"}
         */
        public static final String CONTENT_LOCATION = "Content-Location";
        /**
         * {@code "Content-Transfer-Encoding"}
         */
        public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
        /**
         * {@code "Content-MD5"}
         */
        public static final String CONTENT_MD5 = "Content-MD5";
        /**
         * {@code "Content-Range"}
         */
        public static final String CONTENT_RANGE = "Content-Range";
        /**
         * {@code "Content-Type"}
         */
        public static final String CONTENT_TYPE = "Content-Type";
        /**
         * {@code "Cookie"}
         */
        public static final String COOKIE = "Cookie";
        /**
         * {@code "Date"}
         */
        public static final String DATE = "Date";
        /**
         * {@code "ETag"}
         */
        public static final String ETAG = "ETag";
        /**
         * {@code "Expect"}
         */
        public static final String EXPECT = "Expect";
        /**
         * {@code "Expires"}
         */
        public static final String EXPIRES = "Expires";
        /**
         * {@code "From"}
         */
        public static final String FROM = "From";
        /**
         * {@code "Host"}
         */
        public static final String HOST = "Host";
        /**
         * {@code "If-Match"}
         */
        public static final String IF_MATCH = "If-Match";
        /**
         * {@code "If-Modified-Since"}
         */
        public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
        /**
         * {@code "If-None-Match"}
         */
        public static final String IF_NONE_MATCH = "If-None-Match";
        /**
         * {@code "If-Range"}
         */
        public static final String IF_RANGE = "If-Range";
        /**
         * {@code "If-Unmodified-Since"}
         */
        public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
        /**
         * {@code "Last-Modified"}
         */
        public static final String LAST_MODIFIED = "Last-Modified";
        /**
         * {@code "Location"}
         */
        public static final String LOCATION = "Location";
        /**
         * {@code "Max-Forwards"}
         */
        public static final String MAX_FORWARDS = "Max-Forwards";
        /**
         * {@code "Origin"}
         */
        public static final String ORIGIN = "Origin";
        /**
         * {@code "Pragma"}
         */
        public static final String PRAGMA = "Pragma";
        /**
         * {@code "Proxy-Authenticate"}
         */
        public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";
        /**
         * {@code "Proxy-Authorization"}
         */
        public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
        /**
         * {@code "Range"}
         */
        public static final String RANGE = "Range";
        /**
         * {@code "Referer"}
         */
        public static final String REFERER = "Referer";
        /**
         * {@code "Retry-After"}
         */
        public static final String RETRY_AFTER = "Retry-After";
        /**
         * {@code "Sec-WebSocket-Key1"}
         */
        public static final String SEC_WEBSOCKET_KEY1 = "Sec-WebSocket-Key1";
        /**
         * {@code "Sec-WebSocket-Key2"}
         */
        public static final String SEC_WEBSOCKET_KEY2 = "Sec-WebSocket-Key2";
        /**
         * {@code "Sec-WebSocket-Location"}
         */
        public static final String SEC_WEBSOCKET_LOCATION = "Sec-WebSocket-Location";
        /**
         * {@code "Sec-WebSocket-Origin"}
         */
        public static final String SEC_WEBSOCKET_ORIGIN = "Sec-WebSocket-Origin";
        /**
         * {@code "Sec-WebSocket-Protocol"}
         */
        public static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
        /**
         * {@code "Sec-WebSocket-Version"}
         */
        public static final String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";
        /**
         * {@code "Sec-WebSocket-Key"}
         */
        public static final String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";
        /**
         * {@code "Sec-WebSocket-Accept"}
         */
        public static final String SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";
        /**
         * {@code "Server"}
         */
        public static final String SERVER = "Server";
        /**
         * {@code "Set-Cookie"}
         */
        public static final String SET_COOKIE = "Set-Cookie";
        /**
         * {@code "Set-Cookie2"}
         */
        public static final String SET_COOKIE2 = "Set-Cookie2";
        /**
         * {@code "TE"}
         */
        public static final String TE = "TE";
        /**
         * {@code "Trailer"}
         */
        public static final String TRAILER = "Trailer";
        /**
         * {@code "Transfer-Encoding"}
         */
        public static final String TRANSFER_ENCODING = "Transfer-Encoding";
        /**
         * {@code "Upgrade"}
         */
        public static final String UPGRADE = "Upgrade";
        /**
         * {@code "User-Agent"}
         */
        public static final String USER_AGENT = "User-Agent";
        /**
         * {@code "Vary"}
         */
        public static final String VARY = "Vary";
        /**
         * {@code "Via"}
         */
        public static final String VIA = "Via";
        /**
         * {@code "Warning"}
         */
        public static final String WARNING = "Warning";
        /**
         * {@code "WebSocket-Location"}
         */
        public static final String WEBSOCKET_LOCATION = "WebSocket-Location";
        /**
         * {@code "WebSocket-Origin"}
         */
        public static final String WEBSOCKET_ORIGIN = "WebSocket-Origin";
        /**
         * {@code "WebSocket-Protocol"}
         */
        public static final String WEBSOCKET_PROTOCOL = "WebSocket-Protocol";
        /**
         * {@code "WWW-Authenticate"}
         */
        public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

        private Names() {
        }
    }

    /**
     * @deprecated Use {@link HttpHeaderValues} instead.
     *
     * Standard HTTP header values.
     */
    @Deprecated
    public static final class Values {
        /**
         * {@code "application/json"}
         */
        public static final String APPLICATION_JSON = "application/json";
        /**
         * {@code "application/x-www-form-urlencoded"}
         */
        public static final String APPLICATION_X_WWW_FORM_URLENCODED =
            "application/x-www-form-urlencoded";
        /**
         * {@code "base64"}
         */
        public static final String BASE64 = "base64";
        /**
         * {@code "binary"}
         */
        public static final String BINARY = "binary";
        /**
         * {@code "boundary"}
         */
        public static final String BOUNDARY = "boundary";
        /**
         * {@code "bytes"}
         */
        public static final String BYTES = "bytes";
        /**
         * {@code "charset"}
         */
        public static final String CHARSET = "charset";
        /**
         * {@code "chunked"}
         */
        public static final String CHUNKED = "chunked";
        /**
         * {@code "close"}
         */
        public static final String CLOSE = "close";
        /**
         * {@code "compress"}
         */
        public static final String COMPRESS = "compress";
        /**
         * {@code "100-continue"}
         */
        public static final String CONTINUE =  "100-continue";
        /**
         * {@code "deflate"}
         */
        public static final String DEFLATE = "deflate";
        /**
         * {@code "gzip"}
         */
        public static final String GZIP = "gzip";
        /**
         * {@code "gzip,deflate"}
         */
        public static final String GZIP_DEFLATE = "gzip,deflate";
        /**
         * {@code "identity"}
         */
        public static final String IDENTITY = "identity";
        /**
         * {@code "keep-alive"}
         */
        public static final String KEEP_ALIVE = "keep-alive";
        /**
         * {@code "max-age"}
         */
        public static final String MAX_AGE = "max-age";
        /**
         * {@code "max-stale"}
         */
        public static final String MAX_STALE = "max-stale";
        /**
         * {@code "min-fresh"}
         */
        public static final String MIN_FRESH = "min-fresh";
        /**
         * {@code "multipart/form-data"}
         */
        public static final String MULTIPART_FORM_DATA = "multipart/form-data";
        /**
         * {@code "must-revalidate"}
         */
        public static final String MUST_REVALIDATE = "must-revalidate";
        /**
         * {@code "no-cache"}
         */
        public static final String NO_CACHE = "no-cache";
        /**
         * {@code "no-store"}
         */
        public static final String NO_STORE = "no-store";
        /**
         * {@code "no-transform"}
         */
        public static final String NO_TRANSFORM = "no-transform";
        /**
         * {@code "none"}
         */
        public static final String NONE = "none";
        /**
         * {@code "only-if-cached"}
         */
        public static final String ONLY_IF_CACHED = "only-if-cached";
        /**
         * {@code "private"}
         */
        public static final String PRIVATE = "private";
        /**
         * {@code "proxy-revalidate"}
         */
        public static final String PROXY_REVALIDATE = "proxy-revalidate";
        /**
         * {@code "public"}
         */
        public static final String PUBLIC = "public";
        /**
         * {@code "quoted-printable"}
         */
        public static final String QUOTED_PRINTABLE = "quoted-printable";
        /**
         * {@code "s-maxage"}
         */
        public static final String S_MAXAGE = "s-maxage";
        /**
         * {@code "trailers"}
         */
        public static final String TRAILERS = "trailers";
        /**
         * {@code "Upgrade"}
         */
        public static final String UPGRADE = "Upgrade";
        /**
         * {@code "WebSocket"}
         */
        public static final String WEBSOCKET = "WebSocket";

        private Values() {
        }
    }

    /**
     * @deprecated Use {@link HttpUtil#isKeepAlive(HttpMessage)} instead.
     *
     * Returns {@code true} if and only if the connection can remain open and
     * thus 'kept alive'.  This methods respects the value of the
     * {@code "Connection"} header first and then the return value of
     * {@link HttpVersion#isKeepAliveDefault()}.不赞成使用HttpUtil.isKeepAlive(HttpMessage)。当且仅当连接可以保持打开并因此“保持活动”时，返回true。这个方法首先考虑“Connection”头的值，然后是HttpVersion.isKeepAliveDefault()的返回值。
     */
    @Deprecated
    public static boolean isKeepAlive(HttpMessage message) {
        return HttpUtil.isKeepAlive(message);
    }

    /**
     * @deprecated Use {@link HttpUtil#setKeepAlive(HttpMessage, boolean)} instead.
     *
     * Sets the value of the {@code "Connection"} header depending on the
     * protocol version of the specified message.  This getMethod sets or removes
     * the {@code "Connection"} header depending on what the default keep alive
     * mode of the message's protocol version is, as specified by
     * {@link HttpVersion#isKeepAliveDefault()}.
     * <ul>
     * <li>If the connection is kept alive by default:
     *     <ul>
     *     <li>set to {@code "close"} if {@code keepAlive} is {@code false}.</li>
     *     <li>remove otherwise.</li>
     *     </ul></li>
     * <li>If the connection is closed by default:
     *     <ul>
     *     <li>set to {@code "keep-alive"} if {@code keepAlive} is {@code true}.</li>
     *     <li>remove otherwise.</li>
     *     </ul></li>
     * </ul>不赞成使用HttpUtil。setKeepAlive (HttpMessage,布尔型)。根据指定消息的协议版本设置“连接”标头的值。根据HttpVersion.isKeepAliveDefault()所指定的消息协议版本的默认keep alive模式，此getMethod设置或删除“Connection”标头。
    如果连接在默认情况下保持为活动状态:
    如果keepAlive为假，则设置为“关闭”。
    否则删除。
    如果连接默认关闭:
    如果keepAlive为真，则设置为“keepAlive”。
    否则删除。
     */
    @Deprecated
    public static void setKeepAlive(HttpMessage message, boolean keepAlive) {
        HttpUtil.setKeepAlive(message, keepAlive);
    }

    /**
     * @deprecated Use {@link #get(CharSequence)} instead.
     */
    @Deprecated
    public static String getHeader(HttpMessage message, String name) {
        return message.headers().get(name);
    }

    /**
     * @deprecated Use {@link #get(CharSequence)} instead.
     *
     * Returns the header value with the specified header name.  If there are
     * more than one header value for the specified header name, the first
     * value is returned.不赞成使用get(CharSequence)。返回具有指定标题名称的标题值。如果指定的标题名称有多个标题值，则返回第一个值。
     *
     * @return the header value or {@code null} if there is no such header
     */
    @Deprecated
    public static String getHeader(HttpMessage message, CharSequence name) {
        return message.headers().get(name);
    }

    /**
     * @deprecated Use {@link #get(CharSequence, String)} instead.
     *
     * @see #getHeader(HttpMessage, CharSequence, String)
     */
    @Deprecated
    public static String getHeader(HttpMessage message, String name, String defaultValue) {
        return message.headers().get(name, defaultValue);
    }

    /**
     * @deprecated Use {@link #get(CharSequence, String)} instead.
     *
     * Returns the header value with the specified header name.  If there are
     * more than one header value for the specified header name, the first
     * value is returned.
     *
     * @return the header value or the {@code defaultValue} if there is no such
     *         header不赞成使用get(CharSequence、String)。返回具有指定标题名称的标题值。如果指定的标题名称有多个标题值，则返回第一个值。
     */
    @Deprecated
    public static String getHeader(HttpMessage message, CharSequence name, String defaultValue) {
        return message.headers().get(name, defaultValue);
    }

    /**
     * @deprecated Use {@link #set(CharSequence, Object)} instead.
     *
     * @see #setHeader(HttpMessage, CharSequence, Object)
     */
    @Deprecated
    public static void setHeader(HttpMessage message, String name, Object value) {
        message.headers().set(name, value);
    }

    /**
     * @deprecated Use {@link #set(CharSequence, Object)} instead.
     *
     * Sets a new header with the specified name and value.  If there is an
     * existing header with the same name, the existing header is removed.
     * If the specified value is not a {@link String}, it is converted into a
     * {@link String} by {@link Object#toString()}, except for {@link Date}
     * and {@link Calendar} which are formatted to the date format defined in
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">RFC2616</a>.不推荐使用set(CharSequence, Object)。设置具有指定名称和值的新标头。如果存在具有相同名称的现有标头，则删除现有标头。如果指定的值不是字符串，则由Object.toString()将其转换为字符串，但日期和日历将按照RFC2616中定义的日期格式进行格式化。
     */
    @Deprecated
    public static void setHeader(HttpMessage message, CharSequence name, Object value) {
        message.headers().set(name, value);
    }

    /**
     * @deprecated Use {@link #set(CharSequence, Iterable)} instead.
     *
     * @see #setHeader(HttpMessage, CharSequence, Iterable)
     */
    @Deprecated
    public static void setHeader(HttpMessage message, String name, Iterable<?> values) {
        message.headers().set(name, values);
    }

    /**
     * @deprecated Use {@link #set(CharSequence, Iterable)} instead.
     *
     * Sets a new header with the specified name and values.  If there is an
     * existing header with the same name, the existing header is removed.
     * This getMethod can be represented approximately as the following code:
     * <pre>
     * removeHeader(message, name);
     * for (Object v: values) {
     *     if (v == null) {
     *         break;
     *     }
     *     addHeader(message, name, v);
     * }
     * </pre>不推荐使用set(CharSequence, Iterable)。设置具有指定名称和值的新标头。如果存在具有相同名称的现有标头，则删除现有标头。
     */
    @Deprecated
    public static void setHeader(HttpMessage message, CharSequence name, Iterable<?> values) {
        message.headers().set(name, values);
    }

    /**
     * @deprecated Use {@link #add(CharSequence, Object)} instead.
     *
     * @see #addHeader(HttpMessage, CharSequence, Object)
     */
    @Deprecated
    public static void addHeader(HttpMessage message, String name, Object value) {
        message.headers().add(name, value);
    }

    /**
     * @deprecated Use {@link #add(CharSequence, Object)} instead.
     *
     * Adds a new header with the specified name and value.
     * If the specified value is not a {@link String}, it is converted into a
     * {@link String} by {@link Object#toString()}, except for {@link Date}
     * and {@link Calendar} which are formatted to the date format defined in
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">RFC2616</a>.不推荐使用add(CharSequence, Object)。添加具有指定名称和值的新标头。如果指定的值不是字符串，则由Object.toString()将其转换为字符串，但日期和日历将按照RFC2616中定义的日期格式进行格式化。
     */
    @Deprecated
    public static void addHeader(HttpMessage message, CharSequence name, Object value) {
        message.headers().add(name, value);
    }

    /**
     * @deprecated Use {@link #remove(CharSequence)} instead.
     *
     * @see #removeHeader(HttpMessage, CharSequence)
     */
    @Deprecated
    public static void removeHeader(HttpMessage message, String name) {
        message.headers().remove(name);
    }

    /**
     * @deprecated Use {@link #remove(CharSequence)} instead.
     *
     * Removes the header with the specified name.
     */
    @Deprecated
    public static void removeHeader(HttpMessage message, CharSequence name) {
        message.headers().remove(name);
    }

    /**
     * @deprecated Use {@link #clear()} instead.
     *
     * Removes all headers from the specified message.
     */
    @Deprecated
    public static void clearHeaders(HttpMessage message) {
        message.headers().clear();
    }

    /**
     * @deprecated Use {@link #getInt(CharSequence)} instead.
     *
     * @see #getIntHeader(HttpMessage, CharSequence)
     */
    @Deprecated
    public static int getIntHeader(HttpMessage message, String name) {
        return getIntHeader(message, (CharSequence) name);
    }

    /**
     * @deprecated Use {@link #getInt(CharSequence)} instead.
     *
     * Returns the integer header value with the specified header name.  If
     * there are more than one header value for the specified header name, the
     * first value is returned.
     *
     * @return the header value
     * @throws NumberFormatException
     *         if there is no such header or the header value is not a number
     */
    @Deprecated
    public static int getIntHeader(HttpMessage message, CharSequence name) {
        String value = message.headers().get(name);
        if (value == null) {
            throw new NumberFormatException("header not found: " + name);
        }
        return Integer.parseInt(value);
    }

    /**
     * @deprecated Use {@link #getInt(CharSequence, int)} instead.
     *
     * @see #getIntHeader(HttpMessage, CharSequence, int)
     */
    @Deprecated
    public static int getIntHeader(HttpMessage message, String name, int defaultValue) {
        return message.headers().getInt(name, defaultValue);
    }

    /**
     * @deprecated Use {@link #getInt(CharSequence, int)} instead.
     *
     * Returns the integer header value with the specified header name.  If
     * there are more than one header value for the specified header name, the
     * first value is returned.
     *
     * @return the header value or the {@code defaultValue} if there is no such
     *         header or the header value is not a number
     */
    @Deprecated
    public static int getIntHeader(HttpMessage message, CharSequence name, int defaultValue) {
        return message.headers().getInt(name, defaultValue);
    }

    /**
     * @deprecated Use {@link #setInt(CharSequence, int)} instead.
     *
     * @see #setIntHeader(HttpMessage, CharSequence, int)
     */
    @Deprecated
    public static void setIntHeader(HttpMessage message, String name, int value) {
        message.headers().setInt(name, value);
    }

    /**
     * @deprecated Use {@link #setInt(CharSequence, int)} instead.
     *
     * Sets a new integer header with the specified name and value.  If there
     * is an existing header with the same name, the existing header is removed.
     */
    @Deprecated
    public static void setIntHeader(HttpMessage message, CharSequence name, int value) {
        message.headers().setInt(name, value);
    }

    /**
     * @deprecated Use {@link #set(CharSequence, Iterable)} instead.
     *
     * @see #setIntHeader(HttpMessage, CharSequence, Iterable)
     */
    @Deprecated
    public static void setIntHeader(HttpMessage message, String name, Iterable<Integer> values) {
        message.headers().set(name, values);
    }

    /**
     * @deprecated Use {@link #set(CharSequence, Iterable)} instead.
     *
     * Sets a new integer header with the specified name and values.  If there
     * is an existing header with the same name, the existing header is removed.
     */
    @Deprecated
    public static void setIntHeader(HttpMessage message, CharSequence name, Iterable<Integer> values) {
        message.headers().set(name, values);
    }

    /**
     * @deprecated Use {@link #add(CharSequence, Iterable)} instead.
     *
     * @see #addIntHeader(HttpMessage, CharSequence, int)
     */
    @Deprecated
    public static void addIntHeader(HttpMessage message, String name, int value) {
        message.headers().add(name, value);
    }

    /**
     * @deprecated Use {@link #addInt(CharSequence, int)} instead.
     *
     * Adds a new integer header with the specified name and value.
     */
    @Deprecated
    public static void addIntHeader(HttpMessage message, CharSequence name, int value) {
        message.headers().addInt(name, value);
    }

    /**
     * @deprecated Use {@link #getTimeMillis(CharSequence)} instead.
     *
     * @see #getDateHeader(HttpMessage, CharSequence)
     */
    @Deprecated
    public static Date getDateHeader(HttpMessage message, String name) throws ParseException {
        return getDateHeader(message, (CharSequence) name);
    }

    /**
     * @deprecated Use {@link #getTimeMillis(CharSequence)} instead.
     *
     * Returns the date header value with the specified header name.  If
     * there are more than one header value for the specified header name, the
     * first value is returned.
     *
     * @return the header value
     * @throws ParseException
     *         if there is no such header or the header value is not a formatted date
     */
    @Deprecated
    public static Date getDateHeader(HttpMessage message, CharSequence name) throws ParseException {
        String value = message.headers().get(name);
        if (value == null) {
            throw new ParseException("header not found: " + name, 0);
        }
        Date date = DateFormatter.parseHttpDate(value);
        if (date == null) {
            throw new ParseException("header can't be parsed into a Date: " + value, 0);
        }
        return date;
    }

    /**
     * @deprecated Use {@link #getTimeMillis(CharSequence, long)} instead.
     *
     * @see #getDateHeader(HttpMessage, CharSequence, Date)
     */
    @Deprecated
    public static Date getDateHeader(HttpMessage message, String name, Date defaultValue) {
        return getDateHeader(message, (CharSequence) name, defaultValue);
    }

    /**
     * @deprecated Use {@link #getTimeMillis(CharSequence, long)} instead.
     *
     * Returns the date header value with the specified header name.  If
     * there are more than one header value for the specified header name, the
     * first value is returned.
     *
     * @return the header value or the {@code defaultValue} if there is no such
     *         header or the header value is not a formatted date
     */
    @Deprecated
    public static Date getDateHeader(HttpMessage message, CharSequence name, Date defaultValue) {
        final String value = getHeader(message, name);
        Date date = DateFormatter.parseHttpDate(value);
        return date != null ? date : defaultValue;
    }

    /**
     * @deprecated Use {@link #set(CharSequence, Object)} instead.
     *
     * @see #setDateHeader(HttpMessage, CharSequence, Date)
     */
    @Deprecated
    public static void setDateHeader(HttpMessage message, String name, Date value) {
        setDateHeader(message, (CharSequence) name, value);
    }

    /**
     * @deprecated Use {@link #set(CharSequence, Object)} instead.
     *
     * Sets a new date header with the specified name and value.  If there
     * is an existing header with the same name, the existing header is removed.
     * The specified value is formatted as defined in
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">RFC2616</a>
     */
    @Deprecated
    public static void setDateHeader(HttpMessage message, CharSequence name, Date value) {
        if (value != null) {
            message.headers().set(name, DateFormatter.format(value));
        } else {
            message.headers().set(name, null);
        }
    }

    /**
     * @deprecated Use {@link #set(CharSequence, Iterable)} instead.
     *
     * @see #setDateHeader(HttpMessage, CharSequence, Iterable)
     */
    @Deprecated
    public static void setDateHeader(HttpMessage message, String name, Iterable<Date> values) {
        message.headers().set(name, values);
    }

    /**
     * @deprecated Use {@link #set(CharSequence, Iterable)} instead.
     *
     * Sets a new date header with the specified name and values.  If there
     * is an existing header with the same name, the existing header is removed.
     * The specified values are formatted as defined in
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">RFC2616</a>
     */
    @Deprecated
    public static void setDateHeader(HttpMessage message, CharSequence name, Iterable<Date> values) {
        message.headers().set(name, values);
    }

    /**
     * @deprecated Use {@link #add(CharSequence, Object)} instead.
     *
     * @see #addDateHeader(HttpMessage, CharSequence, Date)
     */
    @Deprecated
    public static void addDateHeader(HttpMessage message, String name, Date value) {
        message.headers().add(name, value);
    }

    /**
     * @deprecated Use {@link #add(CharSequence, Object)} instead.
     *
     * Adds a new date header with the specified name and value.  The specified
     * value is formatted as defined in
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">RFC2616</a>
     */
    @Deprecated
    public static void addDateHeader(HttpMessage message, CharSequence name, Date value) {
        message.headers().add(name, value);
    }

    /**
     * @deprecated Use {@link HttpUtil#getContentLength(HttpMessage)} instead.
     *
     * Returns the length of the content.  Please note that this value is
     * not retrieved from {@link HttpContent#content()} but from the
     * {@code "Content-Length"} header, and thus they are independent from each
     * other.
     *
     * @return the content length
     *
     * @throws NumberFormatException
     *         if the message does not have the {@code "Content-Length"} header
     *         or its value is not a number不赞成使用HttpUtil.getContentLength(HttpMessage)。返回内容的长度。请注意，这个值不是从HttpContent.content()检索的，而是从“Content-Length”报头检索的，因此它们彼此独立。
     */
    @Deprecated
    public static long getContentLength(HttpMessage message) {
        return HttpUtil.getContentLength(message);
    }

    /**
     * @deprecated Use {@link HttpUtil#getContentLength(HttpMessage, long)} instead.
     *
     * Returns the length of the content.  Please note that this value is
     * not retrieved from {@link HttpContent#content()} but from the
     * {@code "Content-Length"} header, and thus they are independent from each
     * other.不赞成使用HttpUtil。getContentLength (HttpMessage长)。返回内容的长度。请注意，这个值不是从HttpContent.content()检索的，而是从“Content-Length”报头检索的，因此它们彼此独立。
     *
     * @return the content length or {@code defaultValue} if this message does
     *         not have the {@code "Content-Length"} header or its value is not
     *         a number
     */
    @Deprecated
    public static long getContentLength(HttpMessage message, long defaultValue) {
        return HttpUtil.getContentLength(message, defaultValue);
    }

    /**
     * @deprecated Use {@link HttpUtil#setContentLength(HttpMessage, long)} instead.
     */
    @Deprecated
    public static void setContentLength(HttpMessage message, long length) {
        HttpUtil.setContentLength(message, length);
    }

    /**
     * @deprecated Use {@link #get(CharSequence)} instead.
     *
     * Returns the value of the {@code "Host"} header.
     */
    @Deprecated
    public static String getHost(HttpMessage message) {
        return message.headers().get(HttpHeaderNames.HOST);
    }

    /**
     * @deprecated Use {@link #get(CharSequence, String)} instead.
     *
     * Returns the value of the {@code "Host"} header.  If there is no such
     * header, the {@code defaultValue} is returned.不赞成使用get(CharSequence、String)。返回“Host”报头的值。如果没有这样的标头，则返回defaultValue。
     */
    @Deprecated
    public static String getHost(HttpMessage message, String defaultValue) {
        return message.headers().get(HttpHeaderNames.HOST, defaultValue);
    }

    /**
     * @deprecated Use {@link #set(CharSequence, Object)} instead.
     *
     * @see #setHost(HttpMessage, CharSequence)
     */
    @Deprecated
    public static void setHost(HttpMessage message, String value) {
        message.headers().set(HttpHeaderNames.HOST, value);
    }

    /**
     * @deprecated Use {@link #set(CharSequence, Object)} instead.
     *
     * Sets the {@code "Host"} header.
     */
    @Deprecated
    public static void setHost(HttpMessage message, CharSequence value) {
        message.headers().set(HttpHeaderNames.HOST, value);
    }

    /**
     * @deprecated Use {@link #getTimeMillis(CharSequence)} instead.
     *
     * Returns the value of the {@code "Date"} header.
     *
     * @throws ParseException
     *         if there is no such header or the header value is not a formatted date
     */
    @Deprecated
    public static Date getDate(HttpMessage message) throws ParseException {
        return getDateHeader(message, HttpHeaderNames.DATE);
    }

    /**
     * @deprecated Use {@link #getTimeMillis(CharSequence, long)} instead.
     *
     * Returns the value of the {@code "Date"} header. If there is no such
     * header or the header is not a formatted date, the {@code defaultValue}
     * is returned.
     */
    @Deprecated
    public static Date getDate(HttpMessage message, Date defaultValue) {
        return getDateHeader(message, HttpHeaderNames.DATE, defaultValue);
    }

    /**
     * @deprecated Use {@link #set(CharSequence, Object)} instead.
     *
     * Sets the {@code "Date"} header.
     */
    @Deprecated
    public static void setDate(HttpMessage message, Date value) {
        message.headers().set(HttpHeaderNames.DATE, value);
    }

    /**
     * @deprecated Use {@link HttpUtil#is100ContinueExpected(HttpMessage)} instead.
     *
     * Returns {@code true} if and only if the specified message contains the
     * {@code "Expect: 100-continue"} header.不赞成使用HttpUtil.is100ContinueExpected(HttpMessage)。当且仅当指定的消息包含“Expect: 100-continue”报头时，返回true。
     */
    @Deprecated
    public static boolean is100ContinueExpected(HttpMessage message) {
        return HttpUtil.is100ContinueExpected(message);
    }

    /**
     * @deprecated Use {@link HttpUtil#set100ContinueExpected(HttpMessage, boolean)} instead.
     *
     * Sets the {@code "Expect: 100-continue"} header to the specified message.
     * If there is any existing {@code "Expect"} header, they are replaced with
     * the new one.不赞成使用HttpUtil。set100ContinueExpected (HttpMessage,布尔型)。将“Expect: 100-continue”标头设置为指定的消息。如果存在任何现有的“Expect”标头，则将它们替换为新的标头
     */
    @Deprecated
    public static void set100ContinueExpected(HttpMessage message) {
        HttpUtil.set100ContinueExpected(message, true);
    }

    /**
     * @deprecated Use {@link HttpUtil#set100ContinueExpected(HttpMessage, boolean)} instead.
     *
     * Sets or removes the {@code "Expect: 100-continue"} header to / from the
     * specified message.  If {@code set} is {@code true},
     * the {@code "Expect: 100-continue"} header is set and all other previous
     * {@code "Expect"} headers are removed.  Otherwise, all {@code "Expect"}
     * headers are removed completely.不赞成使用HttpUtil。set100ContinueExpected (HttpMessage,布尔型)。从指定的消息中设置或删除“Expect: 100-continue”标头。如果set为真，则设置“Expect: 100-continue”标头，并删除所有其他以前的“Expect”标头。否则，所有“Expect”标头将被完全删除。
     */
    @Deprecated
    public static void set100ContinueExpected(HttpMessage message, boolean set) {
        HttpUtil.set100ContinueExpected(message, set);
    }

    /**
     * @deprecated Use {@link HttpUtil#isTransferEncodingChunked(HttpMessage)} instead.
     *
     * Checks to see if the transfer encoding in a specified {@link HttpMessage} is chunked不赞成使用HttpUtil.isTransferEncodingChunked(HttpMessage)。检查指定HttpMessage中的传输编码是否分块
     *
     * @param message The message to check
     * @return True if transfer encoding is chunked, otherwise false
     */
    @Deprecated
    public static boolean isTransferEncodingChunked(HttpMessage message) {
        return HttpUtil.isTransferEncodingChunked(message);
    }

    /**
     * @deprecated Use {@link HttpUtil#setTransferEncodingChunked(HttpMessage, boolean)} instead.
     */
    @Deprecated
    public static void removeTransferEncodingChunked(HttpMessage m) {
        HttpUtil.setTransferEncodingChunked(m, false);
    }

    /**
     * @deprecated Use {@link HttpUtil#setTransferEncodingChunked(HttpMessage, boolean)} instead.
     */
    @Deprecated
    public static void setTransferEncodingChunked(HttpMessage m) {
        HttpUtil.setTransferEncodingChunked(m, true);
    }

    /**
     * @deprecated Use {@link HttpUtil#isContentLengthSet(HttpMessage)} instead.
     */
    @Deprecated
    public static boolean isContentLengthSet(HttpMessage m) {
        return HttpUtil.isContentLengthSet(m);
    }

    /**
     * @deprecated Use {@link AsciiString#contentEqualsIgnoreCase(CharSequence, CharSequence)} instead.
     */
    @Deprecated
    public static boolean equalsIgnoreCase(CharSequence name1, CharSequence name2) {
        return contentEqualsIgnoreCase(name1, name2);
    }

    @Deprecated
    public static void encodeAscii(CharSequence seq, ByteBuf buf) {
        if (seq instanceof AsciiString) {
            ByteBufUtil.copy((AsciiString) seq, 0, buf, seq.length());
        } else {
            buf.writeCharSequence(seq, CharsetUtil.US_ASCII);
        }
    }

    /**
     * @deprecated Use {@link AsciiString} instead.
     * <p>
     * Create a new {@link CharSequence} which is optimized for reuse as {@link HttpHeaders} name or value.
     * So if you have a Header name or value that you want to reuse you should make use of this.不推荐使用AsciiString。
    创建一个新的CharSequence，它经过优化可以重用为HttpHeaders名称或值。如果你有一个你想要重用的标题名或值，你应该利用这个。
     */
    @Deprecated
    public static CharSequence newEntity(String name) {
        return new AsciiString(name);
    }

    protected HttpHeaders() { }

    /**
     * @see #get(CharSequence)
     */
    public abstract String get(String name);

    /**
     * Returns the value of a header with the specified name.  If there are
     * more than one values for the specified name, the first value is returned.返回具有指定名称的标头的值。如果指定名称有多个值，则返回第一个值。
     *
     * @param name The name of the header to search
     * @return The first header value or {@code null} if there is no such header
     * @see #getAsString(CharSequence)
     */
    public String get(CharSequence name) {
        return get(name.toString());
    }

    /**
     * Returns the value of a header with the specified name.  If there are
     * more than one values for the specified name, the first value is returned.返回具有指定名称的标头的值。如果指定名称有多个值，则返回第一个值。
     *
     * @param name The name of the header to search
     * @return The first header value or {@code defaultValue} if there is no such header
     */
    public String get(CharSequence name, String defaultValue) {
        String value = get(name);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    /**
     * Returns the integer value of a header with the specified name. If there are more than one values for the
     * specified name, the first value is returned.返回具有指定名称的标题的整数值。如果指定名称有多个值，则返回第一个值。
     *
     * @param name the name of the header to search
     * @return the first header value if the header is found and its value is an integer. {@code null} if there's no
     *         such header or its value is not an integer.
     */
    public abstract Integer getInt(CharSequence name);

    /**
     * Returns the integer value of a header with the specified name. If there are more than one values for the
     * specified name, the first value is returned.返回具有指定名称的标题的整数值。如果指定名称有多个值，则返回第一个值。
     *
     * @param name the name of the header to search
     * @param defaultValue the default value
     * @return the first header value if the header is found and its value is an integer. {@code defaultValue} if
     *         there's no such header or its value is not an integer.
     */
    public abstract int getInt(CharSequence name, int defaultValue);

    /**
     * Returns the short value of a header with the specified name. If there are more than one values for the
     * specified name, the first value is returned.返回具有指定名称的标头的短值。如果指定名称有多个值，则返回第一个值。
     *
     * @param name the name of the header to search
     * @return the first header value if the header is found and its value is a short. {@code null} if there's no
     *         such header or its value is not a short.
     */
    public abstract Short getShort(CharSequence name);

    /**
     * Returns the short value of a header with the specified name. If there are more than one values for the
     * specified name, the first value is returned.返回具有指定名称的标头的短值。如果指定名称有多个值，则返回第一个值。
     *
     * @param name the name of the header to search
     * @param defaultValue the default value
     * @return the first header value if the header is found and its value is a short. {@code defaultValue} if
     *         there's no such header or its value is not a short.
     */
    public abstract short getShort(CharSequence name, short defaultValue);

    /**
     * Returns the date value of a header with the specified name. If there are more than one values for the
     * specified name, the first value is returned.
     *
     * @param name the name of the header to search
     * @return the first header value if the header is found and its value is a date. {@code null} if there's no
     *         such header or its value is not a date.
     */
    public abstract Long getTimeMillis(CharSequence name);

    /**
     * Returns the date value of a header with the specified name. If there are more than one values for the
     * specified name, the first value is returned.
     *
     * @param name the name of the header to search
     * @param defaultValue the default value
     * @return the first header value if the header is found and its value is a date. {@code defaultValue} if
     *         there's no such header or its value is not a date.
     */
    public abstract long getTimeMillis(CharSequence name, long defaultValue);

    /**
     * @see #getAll(CharSequence)
     */
    public abstract List<String> getAll(String name);

    /**
     * Returns the values of headers with the specified name 返回具有指定名称的标头的值
     *
     * @param name The name of the headers to search
     * @return A {@link List} of header values which will be empty if no values
     *         are found
     * @see #getAllAsString(CharSequence)
     */
    public List<String> getAll(CharSequence name) {
        return getAll(name.toString());
    }

    /**
     * Returns a new {@link List} that contains all headers in this object.  Note that modifying the
     * returned {@link List} will not affect the state of this object.  If you intend to enumerate over the header
     * entries only, use {@link #iterator()} instead, which has much less overhead.
     * @see #iteratorCharSequence()
     * 返回包含此对象中所有标题的新列表。注意，修改返回的列表不会影响这个对象的状态。如果您只打算枚举头条目，那么应该使用iterator()，它的开销要小得多。
     */
    public abstract List<Map.Entry<String, String>> entries();

    /**
     * @see #contains(CharSequence)
     */
    public abstract boolean contains(String name);

    /**
     * @deprecated It is preferred to use {@link #iteratorCharSequence()} unless you need {@link String}.
     * If {@link String} is required then use {@link #iteratorAsString()}.
     */
    @Deprecated
    @Override
    public abstract Iterator<Entry<String, String>> iterator();

    /**
     * @return Iterator over the name/value header pairs.
     */
    public abstract Iterator<Entry<CharSequence, CharSequence>> iteratorCharSequence();

    /**
     * Equivalent to {@link #getAll(String)} but it is possible that no intermediate list is generated.相当于getAll(字符串)，但可能不会生成中间列表。
     * @param name the name of the header to retrieve
     * @return an {@link Iterator} of header values corresponding to {@code name}.
     */
    public Iterator<String> valueStringIterator(CharSequence name) {
        return getAll(name).iterator();
    }

    /**
     * Equivalent to {@link #getAll(String)} but it is possible that no intermediate list is generated.相当于getAll(字符串)，但可能不会生成中间列表。
     * @param name the name of the header to retrieve
     * @return an {@link Iterator} of header values corresponding to {@code name}.
     */
    public Iterator<? extends CharSequence> valueCharSequenceIterator(CharSequence name) {
        return valueStringIterator(name);
    }

    /**
     * Checks to see if there is a header with the specified name 检查是否有具有指定名称的标头
     *
     * @param name The name of the header to search for
     * @return True if at least one header is found
     */
    public boolean contains(CharSequence name) {
        return contains(name.toString());
    }

    /**
     * Checks if no header exists.
     */
    public abstract boolean isEmpty();

    /**
     * Returns the number of headers in this object.
     */
    public abstract int size();

    /**
     * Returns a new {@link Set} that contains the names of all headers in this object.  Note that modifying the
     * returned {@link Set} will not affect the state of this object.  If you intend to enumerate over the header
     * entries only, use {@link #iterator()} instead, which has much less overhead.返回一个新集合，该集合包含此对象中所有标头的名称。注意，修改返回的集合不会影响这个对象的状态。如果您只打算枚举头条目，那么应该使用iterator()，它的开销要小得多。
     */
    public abstract Set<String> names();

    /**
     * @see #add(CharSequence, Object)
     */
    public abstract HttpHeaders add(String name, Object value);

    /**
     * Adds a new header with the specified name and value.
     *
     * If the specified value is not a {@link String}, it is converted
     * into a {@link String} by {@link Object#toString()}, except in the cases
     * of {@link Date} and {@link Calendar}, which are formatted to the date
     * format defined in <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">RFC2616</a>.
     *
     * @param name The name of the header being added
     * @param value The value of the header being added
     *
     * @return {@code this}
     * 添加具有指定名称和值的新标头。如果指定的值不是字符串，那么Object.toString()将其转换为字符串，日期和日历的情况除外，日期和日历被格式化为RFC2616中定义的日期格式。
     */
    public HttpHeaders add(CharSequence name, Object value) {
        return add(name.toString(), value);
    }

    /**
     * @see #add(CharSequence, Iterable)
     */
    public abstract HttpHeaders add(String name, Iterable<?> values);

    /**
     * Adds a new header with the specified name and values.
     *
     * This getMethod can be represented approximately as the following code:
     * <pre>
     * for (Object v: values) {
     *     if (v == null) {
     *         break;
     *     }
     *     headers.add(name, v);
     * }
     * </pre>
     *
     * @param name The name of the headers being set
     * @param values The values of the headers being set
     * @return {@code this}
     */
    public HttpHeaders add(CharSequence name, Iterable<?> values) {
        return add(name.toString(), values);
    }

    /**
     * Adds all header entries of the specified {@code headers}.添加指定标题的所有标题项。
     *
     * @return {@code this}
     */
    public HttpHeaders add(HttpHeaders headers) {
        if (headers == null) {
            throw new NullPointerException("headers");
        }
        for (Map.Entry<String, String> e: headers) {
            add(e.getKey(), e.getValue());
        }
        return this;
    }

    /**
     * Add the {@code name} to {@code value}.
     * @param name The name to modify
     * @param value The value
     * @return {@code this}
     */
    public abstract HttpHeaders addInt(CharSequence name, int value);

    /**
     * Add the {@code name} to {@code value}.
     * @param name The name to modify
     * @param value The value
     * @return {@code this}
     */
    public abstract HttpHeaders addShort(CharSequence name, short value);

    /**
     * @see #set(CharSequence, Object)
     */
    public abstract HttpHeaders set(String name, Object value);

    /**
     * Sets a header with the specified name and value.
     *
     * If there is an existing header with the same name, it is removed.
     * If the specified value is not a {@link String}, it is converted into a
     * {@link String} by {@link Object#toString()}, except for {@link Date}
     * and {@link Calendar}, which are formatted to the date format defined in
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">RFC2616</a>.
     *
     * @param name The name of the header being set
     * @param value The value of the header being set
     * @return {@code this}
     * 使用指定的名称和值设置标题。如果存在具有相同名称的现有标头，则将其删除。如果指定的值不是字符串，则由Object.toString()将其转换为字符串，除了日期和日历，它们按照RFC2616中定义的日期格式进行格式化。
     */
    public HttpHeaders set(CharSequence name, Object value) {
        return set(name.toString(), value);
    }

    /**
     * @see #set(CharSequence, Iterable)
     */
    public abstract HttpHeaders set(String name, Iterable<?> values);

    /**
     * Sets a header with the specified name and values.
     *
     * If there is an existing header with the same name, it is removed.
     * This getMethod can be represented approximately as the following code:
     * <pre>
     * headers.remove(name);
     * for (Object v: values) {
     *     if (v == null) {
     *         break;
     *     }
     *     headers.add(name, v);
     * }
     * </pre>
     *
     * @param name The name of the headers being set
     * @param values The values of the headers being set
     * @return {@code this}
     */
    public HttpHeaders set(CharSequence name, Iterable<?> values) {
        return set(name.toString(), values);
    }

    /**
     * Cleans the current header entries and copies all header entries of the specified {@code headers}.清除当前标头条目并复制指定标头的所有标头条目。
     *
     * @return {@code this}
     */
    public HttpHeaders set(HttpHeaders headers) {
        checkNotNull(headers, "headers");

        clear();

        if (headers.isEmpty()) {
            return this;
        }

        for (Entry<String, String> entry : headers) {
            add(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * Retains all current headers but calls {@link #set(String, Object)} for each entry in {@code headers}保留所有当前标题，但调用集(字符串，对象)为标题中的每个条目
     *
     * @param headers The headers used to {@link #set(String, Object)} values in this instance
     * @return {@code this}
     */
    public HttpHeaders setAll(HttpHeaders headers) {
        checkNotNull(headers, "headers");

        if (headers.isEmpty()) {
            return this;
        }

        for (Entry<String, String> entry : headers) {
            set(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * Set the {@code name} to {@code value}. This will remove all previous values associated with {@code name}.
     * @param name The name to modify
     * @param value The value
     * @return {@code this}
     */
    public abstract HttpHeaders setInt(CharSequence name, int value);

    /**
     * Set the {@code name} to {@code value}. This will remove all previous values associated with {@code name}.
     * @param name The name to modify
     * @param value The value
     * @return {@code this}
     */
    public abstract HttpHeaders setShort(CharSequence name, short value);

    /**
     * @see #remove(CharSequence)
     */
    public abstract HttpHeaders remove(String name);

    /**
     * Removes the header with the specified name.
     *
     * @param name The name of the header to remove
     * @return {@code this}
     */
    public HttpHeaders remove(CharSequence name) {
        return remove(name.toString());
    }

    /**
     * Removes all headers from this {@link HttpMessage}.
     *
     * @return {@code this}
     */
    public abstract HttpHeaders clear();

    /**
     * @see #contains(CharSequence, CharSequence, boolean)
     */
    public boolean contains(String name, String value, boolean ignoreCase) {
        Iterator<String> valueIterator = valueStringIterator(name);
        if (ignoreCase) {
            while (valueIterator.hasNext()) {
                if (valueIterator.next().equalsIgnoreCase(value)) {
                    return true;
                }
            }
        } else {
            while (valueIterator.hasNext()) {
                if (valueIterator.next().equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if a header with the {@code name} and {@code value} exists, {@code false} otherwise.
     * This also handles multiple values that are separated with a {@code ,}.
     * <p>
     * If {@code ignoreCase} is {@code true} then a case insensitive compare is done on the value.
     * @param name the name of the header to find
     * @param value the value of the header to find
     * @param ignoreCase {@code true} then a case insensitive compare is run to compare values.
     * otherwise a case sensitive compare is run to compare values.
     *                               如果存在具有名称和值的标头，则返回true，否则返回false。这还可以处理用a、分隔的多个值。
    如果ignoreCase为真，则对值进行不区分大小写的比较。
     */
    public boolean containsValue(CharSequence name, CharSequence value, boolean ignoreCase) {
        Iterator<? extends CharSequence> itr = valueCharSequenceIterator(name);
        while (itr.hasNext()) {
            if (containsCommaSeparatedTrimmed(itr.next(), value, ignoreCase)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsCommaSeparatedTrimmed(CharSequence rawNext, CharSequence expected,
                                                         boolean ignoreCase) {
        int begin = 0;
        int end;
        if (ignoreCase) {
            if ((end = AsciiString.indexOf(rawNext, ',', begin)) == -1) {
                if (contentEqualsIgnoreCase(trim(rawNext), expected)) {
                    return true;
                }
            } else {
                do {
                    if (contentEqualsIgnoreCase(trim(rawNext.subSequence(begin, end)), expected)) {
                        return true;
                    }
                    begin = end + 1;
                } while ((end = AsciiString.indexOf(rawNext, ',', begin)) != -1);

                if (begin < rawNext.length()) {
                    if (contentEqualsIgnoreCase(trim(rawNext.subSequence(begin, rawNext.length())), expected)) {
                        return true;
                    }
                }
            }
        } else {
            if ((end = AsciiString.indexOf(rawNext, ',', begin)) == -1) {
                if (contentEquals(trim(rawNext), expected)) {
                    return true;
                }
            } else {
                do {
                    if (contentEquals(trim(rawNext.subSequence(begin, end)), expected)) {
                        return true;
                    }
                    begin = end + 1;
                } while ((end = AsciiString.indexOf(rawNext, ',', begin)) != -1);

                if (begin < rawNext.length()) {
                    if (contentEquals(trim(rawNext.subSequence(begin, rawNext.length())), expected)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * {@link Headers#get(Object)} and convert the result to a {@link String}.获取(对象)并将结果转换为字符串。
     * @param name the name of the header to retrieve
     * @return the first header value if the header is found. {@code null} if there's no such header.
     */
    public final String getAsString(CharSequence name) {
        return get(name);
    }

    /**
     * {@link Headers#getAll(Object)} and convert each element of {@link List} to a {@link String}.getall(对象)，并将列表的每个元素转换为字符串。
     * @param name the name of the header to retrieve
     * @return a {@link List} of header values or an empty {@link List} if no values are found.
     */
    public final List<String> getAllAsString(CharSequence name) {
        return getAll(name);
    }

    /**
     * {@link Iterator} that converts each {@link Entry}'s key and value to a {@link String}.
     */
    public final Iterator<Entry<String, String>> iteratorAsString() {
        return iterator();
    }

    /**
     * Returns {@code true} if a header with the {@code name} and {@code value} exists, {@code false} otherwise.
     * <p>
     * If {@code ignoreCase} is {@code true} then a case insensitive compare is done on the value.
     * @param name the name of the header to find
     * @param value the value of the header to find
     * @param ignoreCase {@code true} then a case insensitive compare is run to compare values.
     * otherwise a case sensitive compare is run to compare values.
     *                               如果存在具有名称和值的标头，则返回true，否则返回false。
    如果ignoreCase为真，则对值进行不区分大小写的比较。
     */
    public boolean contains(CharSequence name, CharSequence value, boolean ignoreCase) {
        return contains(name.toString(), value.toString(), ignoreCase);
    }

    @Override
    public String toString() {
        return HeadersUtils.toString(getClass(), iteratorCharSequence(), size());
    }

    /**
     * Returns a deap copy of the passed in {@link HttpHeaders}.返回传递到HttpHeaders中的数据的deap副本。
     */
    public HttpHeaders copy() {
        return new DefaultHttpHeaders().set(this);
    }
}
