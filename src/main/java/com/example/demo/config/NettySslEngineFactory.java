package com.example.demo.config;

import com.datastax.oss.driver.api.core.ssl.SslEngineFactory;
import io.netty.handler.ssl.SslContext;
import io.netty.buffer.UnpooledByteBufAllocator;
import com.datastax.oss.driver.api.core.metadata.EndPoint;

import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.util.Map;

public class NettySslEngineFactory implements SslEngineFactory {
    private final SslContext nettySslContext;

    public NettySslEngineFactory(SslContext nettySslContext) {
        this.nettySslContext = nettySslContext;
    }

    public void init(Map<String, Object> config) {}

    public SSLEngine newSslEngine(InetSocketAddress remoteEndpoint, String s) {
        return nettySslContext.newEngine(UnpooledByteBufAllocator.DEFAULT);
    }

    @Override
    public SSLEngine newSslEngine(EndPoint endpoint) {
        return nettySslContext.newEngine(UnpooledByteBufAllocator.DEFAULT);
    }

    @Override
    public void close() {}
} 