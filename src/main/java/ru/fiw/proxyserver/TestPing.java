package ru.fiw.proxyserver;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.network.*;
import net.minecraft.network.listener.ClientQueryPacketListener;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.network.packet.s2c.query.QueryPongS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

public class TestPing {
    public String state = "";

    private long pingSentAt;
    private ClientConnection pingDestination = null;
    private Proxy proxy;
    private static final ThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(5, (new ThreadFactoryBuilder()).setNameFormat("Server Pinger #%d").setDaemon(true).build());

    public void run(String ip, int port, Proxy proxy) {
        this.proxy = proxy;
        TestPing.EXECUTOR.submit(() -> ping(ip, port));
    }

    private void ping(String ip, int port) {
        state = Text.translatable("ui.proxyserver.ping.pinging", ip).getString();
        ClientConnection clientConnection;
        try {
            clientConnection = createTestClientConnection(InetAddress.getByName(ip), port);
        } catch (UnknownHostException e) {
            state = Formatting.RED + Text.translatable("ui.proxyserver.err.cantConnect").getString();
            return;
        } catch (Exception e) {
            state = Formatting.RED + Text.translatable("ui.proxyserver.err.cantPing", ip).getString();
            return;
        }
        pingDestination = clientConnection;
        clientConnection.setPacketListener(new ClientQueryPacketListener() {
            private boolean successful;

            public void onResponse(QueryResponseS2CPacket packet) {
                pingSentAt = Util.getMeasuringTimeMs();
                clientConnection.send(new QueryPingC2SPacket(pingSentAt));
            }

            public void onPong(QueryPongS2CPacket packet) {
                successful = true;
                pingDestination = null;
                long pingToServer = Util.getMeasuringTimeMs() - pingSentAt;
                state = Text.translatable("ui.proxyserver.ping.showPing", pingToServer).getString();
                clientConnection.disconnect(Text.translatable("multiplayer.status.finished"));
            }

            public void onDisconnected(Text reason) {
                pingDestination = null;
                if (!this.successful) {
                    state = Formatting.RED + Text.translatable("ui.proxyserver.err.cantPingReason", ip, reason.getString()).getString();
                }
            }

            public ClientConnection getConnection() {
                return clientConnection;
            }
        });

        try {
            clientConnection.send(new HandshakeC2SPacket(ip, port, NetworkState.STATUS));
            clientConnection.send(new QueryRequestC2SPacket());
        } catch (Throwable throwable) {
            state = Formatting.RED + Text.translatable("ui.proxyserver.err.cantPing", ip).getString();
        }
    }

    private ClientConnection createTestClientConnection(InetAddress address, int port) {
        final ClientConnection clientConnection = new ClientConnection(NetworkSide.CLIENTBOUND);

        (new Bootstrap()).group(ClientConnection.CLIENT_IO_GROUP.get()).handler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel channel) {
                try {
                    channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                } catch (ChannelException ignored) {
                }

                channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30))
                        .addLast("splitter", new SplitterHandler())
                        .addLast("decoder", new DecoderHandler(NetworkSide.CLIENTBOUND))
                        .addLast("prepender", new SizePrepender())
                        .addLast("encoder", new PacketEncoder(NetworkSide.SERVERBOUND))
                        .addLast("packet_handler", clientConnection);

                if (proxy.type == Proxy.ProxyType.SOCKS5) {
                    channel.pipeline().addFirst(new Socks5ProxyHandler(new InetSocketAddress(proxy.getIp(), proxy.getPort()), proxy.username.isEmpty() ? null : proxy.username, proxy.password.isEmpty() ? null : proxy.password));
                } else {
                    channel.pipeline().addFirst(new Socks4ProxyHandler(new InetSocketAddress(proxy.getIp(), proxy.getPort()), proxy.username.isEmpty() ? null : proxy.username));
                }
            }
        }).channel(NioSocketChannel.class).connect(address, port).syncUninterruptibly();
        return clientConnection;
    }

    public void pingPendingNetworks() {
        if (pingDestination != null) {
            if (pingDestination.isOpen()) {
                pingDestination.tick();
            } else {
                pingDestination.handleDisconnection();
            }
        }
    }
}
