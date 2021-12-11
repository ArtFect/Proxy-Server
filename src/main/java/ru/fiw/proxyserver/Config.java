package ru.fiw.proxyserver;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class Config {
    private static Configuration config;
    private final static String CATEGORY = "current_proxy";

    public static void loadCurrentProxy() {
        try {
            config = new Configuration(new File(Minecraft.getMinecraft().mcDataDir + "/config", "ProxyServer.cfg"));
            config.load();

            String typeString = config.get(CATEGORY, "type", "SOCKS5", "Type").getString();
            boolean isSocks4 = typeString.equals("SOCKS4");

            String ip = config.get(CATEGORY, "ip", "", "Ip").getString();
            int port = config.get(CATEGORY, "port", 0, "Port").getInt();

            String userID = config.get(CATEGORY, "userid", "", "User ID for socks4").getString();
            String username = config.get(CATEGORY, "username", "", "Username for socks5").getString();
            String password = config.get(CATEGORY, "password", "", "Password for socks5").getString();

            ProxyServer.proxy = new Proxy(isSocks4, ip, port, userID, username, password);
        } catch (Exception e) {
            System.out.println("Error loading config, returning to default variables.");
        } finally {
            config.save();
        }
    }

    public static void saveProxy(Proxy proxy) {
        config.get(CATEGORY, "type", "SOCKS5", "Type").set(proxy.type.name());

        config.get(CATEGORY, "ip", "", "Ip").set(proxy.ip);
        config.get(CATEGORY, "port", 0, "Port").set(proxy.port);

        config.get(CATEGORY, "userid", "", "User ID for socks4").set(proxy.userID);
        config.get(CATEGORY, "username", "", "Username for socks5").set(proxy.username);
        config.get(CATEGORY, "password", "", "Password for socks5").set(proxy.password);
        config.save();
    }
}
