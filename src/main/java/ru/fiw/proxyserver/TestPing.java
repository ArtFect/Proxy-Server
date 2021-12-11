package ru.fiw.proxyserver;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.status.INetHandlerStatusClient;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.network.status.server.S00PacketServerInfo;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

public class TestPing {
    public String state = "";

    private long pingSentAt;
    private NetworkManager pingDestination = null;
    private String proxyIp = "";
    private static final ThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(5, (new ThreadFactoryBuilder()).setNameFormat("Server Pinger #%d").setDaemon(true).build());

    public void run(final String ip, final int port, String proxyIp) {
        this.proxyIp = proxyIp;
        TestPing.EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                TestPing.this.ping(ip, port);
            }
        });
    }

    private void ping(final String ip, final int port) {
        state = "Pinging " + ip + "...";
        final NetworkManager networkmanager = createNetManager(ip, port);
        if (networkmanager == null) return;
        networkmanager.setNetHandler(new INetHandlerStatusClient() {
            private boolean successful;

            public void handleServerInfo(S00PacketServerInfo packetIn) {
                pingSentAt = Minecraft.getSystemTime();
                networkmanager.sendPacket(new C01PacketPing(pingSentAt));
            }

            public void handlePong(S01PacketPong packetIn) {
                successful = true;
                pingDestination = null;
                long pingToServer = Minecraft.getSystemTime() - pingSentAt;
                if (proxyIp.isEmpty() || proxyIp.equals(ProxyServer.lastProxyIp)) {
                    state = "Ping: " + pingToServer;
                } else {
                    state = ChatFormatting.RED + "Cannot set a proxy, try restarting minecraft";
                }
                networkmanager.closeChannel(new ChatComponentText("Finished"));
            }

            public void onDisconnect(IChatComponent reason) {
                pingDestination = null;
                if (!this.successful) {
                    state = ChatFormatting.RED + "Can't ping " + ip + ": " + reason.getUnformattedText();
                }
            }
        });

        try {
            networkmanager.sendPacket(new C00Handshake(47, ip, port, EnumConnectionState.STATUS));
            networkmanager.sendPacket(new C00PacketServerQuery());
        } catch (Throwable throwable) {
            state = ChatFormatting.RED + "Can't ping " + ip;
        }
    }

    private NetworkManager createNetManager(String ip, int port) {
        try {
            return NetworkManager.func_181124_a(InetAddress.getByName(ip), port, false);
        } catch (UnknownHostException e) {
            state = ChatFormatting.RED + "Can't connect to proxy";
            return null;
        } catch (Exception e) {
            state = ChatFormatting.RED + "Can't ping " + ip;
            return null;
        }
    }

    public void pingPendingNetworks() {
        if (pingDestination != null) {
            if (pingDestination.isChannelOpen()) {
                pingDestination.processReceivedPackets();
            } else {
                pingDestination.checkDisconnected();
            }
        }
    }
}
