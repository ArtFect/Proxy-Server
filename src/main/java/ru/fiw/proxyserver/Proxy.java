package ru.fiw.proxyserver;

import com.google.gson.annotations.SerializedName;

public class Proxy {
    @SerializedName("IP:PORT")
    public String ipPort = "";
    public ProxyType type = ProxyType.SOCKS5;
    public String username = "";
    public String password = "";

    public Proxy() {

    }

    public Proxy(boolean isSocks4, String ipPort, String username, String password) {
        this.type = isSocks4 ? ProxyType.SOCKS4 : ProxyType.SOCKS5;
        this.ipPort = ipPort;
        this.username = username;
        this.password = password;
    }

    public int getPort() {
        return Integer.parseInt(ipPort.split(":")[1]);
    }

    public String getIp() {
        return ipPort.split(":")[0];
    }

    public enum ProxyType {
        SOCKS4,
        SOCKS5
    }
}
