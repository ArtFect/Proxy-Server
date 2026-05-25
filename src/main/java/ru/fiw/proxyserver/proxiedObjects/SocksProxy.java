package ru.fiw.proxyserver.proxiedObjects;

import java.net.Proxy;
import java.net.SocketAddress;
import java.net.InetSocketAddress;

public class SocksProxy extends Proxy {
    public SocksProxy() {
        super(Proxy.Type.HTTP, new InetSocketAddress(0));
    }
    
    @Override
    public Type type() {
        return ru.fiw.proxyserver.ProxyServer.proxyEnabled == true ? Proxy.Type.SOCKS : Proxy.Type.DIRECT;
    }

    @Override
    public SocketAddress address() {
        return new InetSocketAddress(
            ru.fiw.proxyserver.ProxyServer.proxy.getIp(),
            ru.fiw.proxyserver.ProxyServer.proxy.getPort()
        );
    }
}
