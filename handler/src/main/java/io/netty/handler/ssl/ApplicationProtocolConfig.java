/*
 * Copyright 2014 The Netty Project
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
package io.netty.handler.ssl;

import java.util.Collections;
import java.util.List;

import javax.net.ssl.SSLEngine;

import static io.netty.handler.ssl.ApplicationProtocolUtil.toList;
import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * Provides an {@link SSLEngine} agnostic way to configure a {@link ApplicationProtocolNegotiator}.提供一种与SSLEngine无关的方法来配置applicationprotocol谈判者。
 */
public final class ApplicationProtocolConfig {

    /**
     * The configuration that disables application protocol negotiation.禁止应用程序协议协商的配置。
     */
    public static final ApplicationProtocolConfig DISABLED = new ApplicationProtocolConfig();

    private final List<String> supportedProtocols;
    private final Protocol protocol;
    private final SelectorFailureBehavior selectorBehavior;
    private final SelectedListenerFailureBehavior selectedBehavior;

    /**
     * Create a new instance.
     * @param protocol The application protocol functionality to use.
     * @param selectorBehavior How the peer selecting the protocol should behave.
     * @param selectedBehavior How the peer being notified of the selected protocol should behave.
     * @param supportedProtocols The order of iteration determines the preference of support for protocols.
     */
    public ApplicationProtocolConfig(Protocol protocol, SelectorFailureBehavior selectorBehavior,
            SelectedListenerFailureBehavior selectedBehavior, Iterable<String> supportedProtocols) {
        this(protocol, selectorBehavior, selectedBehavior, toList(supportedProtocols));
    }

    /**
     * Create a new instance.
     * @param protocol The application protocol functionality to use.
     * @param selectorBehavior How the peer selecting the protocol should behave.
     * @param selectedBehavior How the peer being notified of the selected protocol should behave.
     * @param supportedProtocols The order of iteration determines the preference of support for protocols.
     */
    public ApplicationProtocolConfig(Protocol protocol, SelectorFailureBehavior selectorBehavior,
            SelectedListenerFailureBehavior selectedBehavior, String... supportedProtocols) {
        this(protocol, selectorBehavior, selectedBehavior, toList(supportedProtocols));
    }

    /**
     * Create a new instance.
     * @param protocol The application protocol functionality to use.
     * @param selectorBehavior How the peer selecting the protocol should behave.
     * @param selectedBehavior How the peer being notified of the selected protocol should behave.
     * @param supportedProtocols The order of iteration determines the preference of support for protocols.
     */
    private ApplicationProtocolConfig(
            Protocol protocol, SelectorFailureBehavior selectorBehavior,
            SelectedListenerFailureBehavior selectedBehavior, List<String> supportedProtocols) {
        this.supportedProtocols = Collections.unmodifiableList(checkNotNull(supportedProtocols, "supportedProtocols"));
        this.protocol = checkNotNull(protocol, "protocol");
        this.selectorBehavior = checkNotNull(selectorBehavior, "selectorBehavior");
        this.selectedBehavior = checkNotNull(selectedBehavior, "selectedBehavior");

        if (protocol == Protocol.NONE) {
            throw new IllegalArgumentException("protocol (" + Protocol.NONE + ") must not be " + Protocol.NONE + '.');
        }
        if (supportedProtocols.isEmpty()) {
            throw new IllegalArgumentException("supportedProtocols must be not empty");
        }
    }

    /**
     * A special constructor that is used to instantiate {@link #DISABLED}.用于实例化禁用的特殊构造函数。
     */
    private ApplicationProtocolConfig() {
        supportedProtocols = Collections.emptyList();
        protocol = Protocol.NONE;
        selectorBehavior = SelectorFailureBehavior.CHOOSE_MY_LAST_PROTOCOL;
        selectedBehavior = SelectedListenerFailureBehavior.ACCEPT;
    }

    /**
     * Defines which application level protocol negotiation to use.定义要使用的应用程序级别协议协商。
     */
    public enum Protocol {
        NONE, NPN, ALPN, NPN_AND_ALPN
    }

    /**
     * Defines the most common behaviors for the peer that selects the application protocol.为选择应用程序协议的对等方定义最常见的行为。
     */
    public enum SelectorFailureBehavior {
        /**
         * If the peer who selects the application protocol doesn't find a match this will result in the failing the
         * handshake with a fatal alert.
         * <p>
         * For example in the case of ALPN this will result in a
         * <a herf="https://tools.ietf.org/html/rfc7301#section-3.2">no_application_protocol(120)</a> alert.
         * 如果选择应用程序协议的对等方没有找到匹配项，这将导致与致命警告握手失败。
         例如在ALPN中，这将导致no_application_protocol(120)警报。
         */
        FATAL_ALERT,
        /**
         * If the peer who selects the application protocol doesn't find a match it will pretend no to support
         * the TLS extension by not advertising support for the TLS extension in the handshake. This is used in cases
         * where a "best effort" is desired to talk even if there is no matching protocol.
         * 如果选择应用程序协议的对等方没有找到匹配项，它将假装不支持TLS扩展，在握手时不宣传对TLS扩展的支持。即使没有匹配协议，也需要“尽最大努力”进行对话。
         */
        NO_ADVERTISE,
        /**
         * If the peer who selects the application protocol doesn't find a match it will just select the last protocol
         * it advertised support for. This is used in cases where a "best effort" is desired to talk even if there
         * is no matching protocol, and the assumption is the "most general" fallback protocol is typically listed last.
         * <p>
         * This may be <a href="https://tools.ietf.org/html/rfc7301#section-3.2">illegal for some RFCs</a> but was
         * observed behavior by some SSL implementations, and is supported for flexibility/compatibility.
         * 如果选择应用程序协议的对等端没有找到匹配项，它将只选择它宣传支持的最后一个协议。这种情况下，即使没有匹配的协议，也需要“尽最大的努力”来进行讨论，假设是“最通用的”备用协议通常排在最后。
         对于某些rfc来说，这可能是非法的，但是一些SSL实现观察到了这种行为，并且支持灵活性/兼容性。
         */
        CHOOSE_MY_LAST_PROTOCOL
    }

    /**
     * Defines the most common behaviors for the peer which is notified of the selected protocol.为指定的协议通知的对等点定义最常见的行为。
     */
    public enum SelectedListenerFailureBehavior {
        /**
         * If the peer who is notified what protocol was selected determines the selection was not matched, or the peer
         * didn't advertise support for the TLS extension then the handshake will continue and the application protocol
         * is assumed to be accepted.如果被告知选择了什么协议的对等方确定选择不匹配，或者对等方没有声明支持TLS扩展，那么握手将继续，并假定应用程序协议被接受。
         */
        ACCEPT,
        /**
         * If the peer who is notified what protocol was selected determines the selection was not matched, or the peer
         * didn't advertise support for the TLS extension then the handshake will be failed with a fatal alert.
         * 如果选择应用程序协议的对等方没有找到匹配项，它将假装不支持TLS扩展，在握手时不宣传对TLS扩展的支持。即使没有匹配协议，也需要“尽最大努力”进行对话。
         */
        FATAL_ALERT,
        /**
         * If the peer who is notified what protocol was selected determines the selection was not matched, or the peer
         * didn't advertise support for the TLS extension then the handshake will continue assuming the last protocol
         * supported by this peer is used. This is used in cases where a "best effort" is desired to talk even if there
         * is no matching protocol, and the assumption is the "most general" fallback protocol is typically listed last.
         * 如果被通知选择了哪个协议的对等节点确定选择不匹配，或者对等节点没有声明支持TLS扩展，那么握手将继续假设使用了该对等节点支持的最后一个协议。这种情况下，即使没有匹配的协议，也需要“尽最大的努力”来进行讨论，假设是“最通用的”备用协议通常排在最后。
         */
        CHOOSE_MY_LAST_PROTOCOL
    }

    /**
     * The application level protocols supported.支持应用程序级协议。
     */
    public List<String> supportedProtocols() {
        return supportedProtocols;
    }

    /**
     * Get which application level protocol negotiation to use.获取要使用的应用程序级别协议协商。
     */
    public Protocol protocol() {
        return protocol;
    }

    /**
     * Get the desired behavior for the peer who selects the application protocol.获取选择应用程序协议的对等方所需的行为。
     */
    public SelectorFailureBehavior selectorFailureBehavior() {
        return selectorBehavior;
    }

    /**
     * Get the desired behavior for the peer who is notified of the selected protocol.获取所选协议通知的对等方所需的行为。
     */
    public SelectedListenerFailureBehavior selectedListenerFailureBehavior() {
        return selectedBehavior;
    }
}
