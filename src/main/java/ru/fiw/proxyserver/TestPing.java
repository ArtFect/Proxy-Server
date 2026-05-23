package ru.fiw.proxyserver;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.listener.ClientQueryPacketListener;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
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
        try {
            pingDestination = createTestClientConnection(ip, port);
        } catch (UnknownHostException e) {
            state = Formatting.RED + Text.translatable("ui.proxyserver.err.cantConnect").getString();
            return;
        } catch (Exception e) {
            state = Formatting.RED + Text.translatable("ui.proxyserver.err.cantPing", ip).getString();
            return;
        }
        ClientConnection clientConnection = pingDestination;

        ClientQueryPacketListener listener = new ClientQueryPacketListener() {
            private boolean successful;
            private boolean sentQuery;

            @Override
            public void onResponse(QueryResponseS2CPacket packet) {
                if (this.sentQuery) {
                    clientConnection.disconnect(Text.translatable("multiplayer.status.unrequested"));
                    return;
                }

                this.sentQuery = true;
                pingSentAt = Util.getMeasuringTimeMs();
                clientConnection.send(new QueryPingC2SPacket(pingSentAt));
            }

            @Override
            public void onPingResult(PingResultS2CPacket packet) {
                successful = true;
                pingDestination = null;
                long pingToServer = Util.getMeasuringTimeMs() - pingSentAt;
                state = Text.translatable("ui.proxyserver.ping.showPing", pingToServer).getString();
                clientConnection.disconnect(Text.translatable("multiplayer.status.finished"));
            }

            @Override
            public void onDisconnected(DisconnectionInfo info) {
                pingDestination = null;
                if (!this.successful) {
                    state = Formatting.RED + Text.translatable("ui.proxyserver.err.cantPingReason", ip, info.reason().getString()).getString();
                }
            }

            @Override
            public boolean isConnectionOpen() {
                return clientConnection.isOpen();
            }
        };

        try {
            clientConnection.connect(ip, port, listener);
            clientConnection.send(QueryRequestC2SPacket.INSTANCE);
        } catch (Throwable throwable) {
            pingDestination = null;
            state = Formatting.RED + Text.translatable("ui.proxyserver.err.cantPing", ip).getString();
            clientConnection.disconnect(Text.translatable("multiplayer.status.cancelled"));
        }
    }

    private ClientConnection createTestClientConnection(String host, int port) throws UnknownHostException {
        InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(host), port);
        ClientConnection connection = new ClientConnection(NetworkSide.CLIENTBOUND);

        new Bootstrap()
                .group((EventLoopGroup) ClientConnection.CLIENT_IO_GROUP.get())
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        try {
                            channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                        } catch (ChannelException ignored) {
                        }

                        ChannelPipeline pipeline = channel.pipeline();

                        if (proxy.type == Proxy.ProxyType.SOCKS5) {
                            pipeline.addFirst(new Socks5ProxyHandler(new InetSocketAddress(proxy.getIp(), proxy.getPort()),
                                    proxy.username.isEmpty() ? null : proxy.username,
                                    proxy.password.isEmpty() ? null : proxy.password));
                        } else {
                            pipeline.addFirst(new Socks4ProxyHandler(new InetSocketAddress(proxy.getIp(), proxy.getPort()),
                                    proxy.username.isEmpty() ? null : proxy.username));
                        }

                        pipeline.addLast("timeout", new ReadTimeoutHandler(30));
                        ClientConnection.addHandlers(pipeline, NetworkSide.CLIENTBOUND, false, null);
                        connection.addFlowControlHandler(pipeline);
                    }
                })
                .channel(NioSocketChannel.class)
                .connect(address)
                .syncUninterruptibly();

        return connection;
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
