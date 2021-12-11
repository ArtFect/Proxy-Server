package ru.fiw.proxyserver;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.netty.channel.Channel;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;

public class ProxyServer extends DummyModContainer {
    public static Proxy proxy = new Proxy();
    public static String lastProxyIp = "none";

    public ProxyServer() {
        super(new ModMetadata());
        ModMetadata meta = this.getMetadata();
        meta.modId = "proxyserver";
        meta.version = "1.1";
        meta.name = "Proxy Server";
        meta.description = "Allows you to connect to servers through a proxy";
        meta.authorList = Collections.singletonList("Fiw");
    }

    public enum ProxyType {
        SOCKS4,
        SOCKS5
    }

    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }

    @Subscribe
    @Mod.EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        Config.loadCurrentProxy();
        MinecraftForge.EVENT_BUS.register(new ButtonAdder());
        MinecraftForge.EVENT_BUS.register(new AccountsProxy());
    }

    public static void hook(Channel channel) {
        if (proxy.enabled) {
            lastProxyIp = proxy.ip;
            if (proxy.type == ProxyType.SOCKS5) {
                channel.pipeline().addFirst(new Socks5ProxyHandler(new InetSocketAddress(proxy.ip, proxy.port), proxy.username.isEmpty() ? null : proxy.username, proxy.password.isEmpty() ? null : proxy.password));
            } else {
                channel.pipeline().addFirst(new Socks4ProxyHandler(new InetSocketAddress(proxy.ip, proxy.port), proxy.userID.isEmpty() ? null : proxy.userID));
            }
        } else {
            lastProxyIp = "none";
        }
    }
}
