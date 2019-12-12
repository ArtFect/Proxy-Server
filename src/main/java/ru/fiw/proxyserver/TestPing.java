package ru.fiw.proxyserver;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.status.INetHandlerStatusClient;
import net.minecraft.network.status.client.CPacketPing;
import net.minecraft.network.status.client.CPacketServerQuery;
import net.minecraft.network.status.server.SPacketPong;
import net.minecraft.network.status.server.SPacketServerInfo;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

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

    public void run(String ip, int port, String proxyIp) {
        this.proxyIp = proxyIp;
        TestPing.EXECUTOR.submit(() -> ping(ip, port));
    }

    private void ping(String ip, int port) {
        state = "Pinging " + ip + "...";
        NetworkManager networkmanager;
        try {
            networkmanager = NetworkManager.createNetworkManagerAndConnect(InetAddress.getByName(ip), port, false);
        } catch (UnknownHostException e) {
            state = ChatFormatting.RED + "Can't connect to proxy";
            return;
        } catch (Exception e) {
            state = ChatFormatting.RED + "Can't ping " + ip;
            return;
        }
        pingDestination = networkmanager;
        networkmanager.setNetHandler(new INetHandlerStatusClient() {
            private boolean successful;

            public void handleServerInfo(SPacketServerInfo packetIn) {
                pingSentAt = Minecraft.getSystemTime();
                networkmanager.sendPacket(new CPacketPing(pingSentAt));
            }

            public void handlePong(SPacketPong packetIn) {
                successful = true;
                pingDestination = null;
                long pingToServer = Minecraft.getSystemTime() - pingSentAt;
                if (proxyIp.isEmpty() || proxyIp.equals(ProxyServer.lastProxyIp)) {
                    state = "Ping: " + pingToServer;
                } else {
                    state = ChatFormatting.RED + "Cannot set a proxy, try restarting minecraft";
                }
                networkmanager.closeChannel(new TextComponentString("Finished"));
            }

            public void onDisconnect(ITextComponent reason) {
                pingDestination = null;
                if (!this.successful) {
                    state = ChatFormatting.RED + "Can't ping " + ip + ": " + reason.getUnformattedText();
                }
            }
        });

        try {
            networkmanager.sendPacket(new C00Handshake(ip, port, EnumConnectionState.STATUS));
            networkmanager.sendPacket(new CPacketServerQuery());
        } catch (Throwable throwable) {
            state = ChatFormatting.RED + "Can't ping " + ip;
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
