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
package io.netty.handler.ssl;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.internal.ObjectUtil;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;
import java.util.List;

/**
 * Adapter class which allows to wrap another {@link SslContext} and init {@link SSLEngine} instances.适配器类，它允许包装另一个SslContext和init SSLEngine实例。
 */
public abstract class DelegatingSslContext extends SslContext {

    private final SslContext ctx;

    protected DelegatingSslContext(SslContext ctx) {
        this.ctx = ObjectUtil.checkNotNull(ctx, "ctx");
    }

    @Override
    public final boolean isClient() {
        return ctx.isClient();
    }

    @Override
    public final List<String> cipherSuites() {
        return ctx.cipherSuites();
    }

    @Override
    public final long sessionCacheSize() {
        return ctx.sessionCacheSize();
    }

    @Override
    public final long sessionTimeout() {
        return ctx.sessionTimeout();
    }

    @Override
    public final ApplicationProtocolNegotiator applicationProtocolNegotiator() {
        return ctx.applicationProtocolNegotiator();
    }

    @Override
    public final SSLEngine newEngine(ByteBufAllocator alloc) {
        SSLEngine engine = ctx.newEngine(alloc);
        initEngine(engine);
        return engine;
    }

    @Override
    public final SSLEngine newEngine(ByteBufAllocator alloc, String peerHost, int peerPort) {
        SSLEngine engine = ctx.newEngine(alloc, peerHost, peerPort);
        initEngine(engine);
        return engine;
    }

    @Override
    protected final SslHandler newHandler(ByteBufAllocator alloc, boolean startTls) {
        SslHandler handler = ctx.newHandler(alloc, startTls);
        initHandler(handler);
        return handler;
    }

    @Override
    protected final SslHandler newHandler(ByteBufAllocator alloc, String peerHost, int peerPort, boolean startTls) {
        SslHandler handler = ctx.newHandler(alloc, peerHost, peerPort, startTls);
        initHandler(handler);
        return handler;
    }

    @Override
    public final SSLSessionContext sessionContext() {
        return ctx.sessionContext();
    }

    /**
     * Init the {@link SSLEngine}.
     */
    protected abstract void initEngine(SSLEngine engine);

    /**
     * Init the {@link SslHandler}. This will by default call {@link #initEngine(SSLEngine)}, sub-classes may override
     * this.Init SslHandler。这将在默认情况下调用initEngine(SSLEngine)，子类可能会覆盖它。
     */
    protected void initHandler(SslHandler handler) {
        initEngine(handler.engine());
    }
}
