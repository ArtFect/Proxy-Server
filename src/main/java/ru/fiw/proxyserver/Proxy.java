package ru.fiw.proxyserver;

public class Proxy {
    public boolean enabled;
    public ProxyServer.ProxyType type = ProxyServer.ProxyType.SOCKS5;
    public String ip = "";
    public int port = 0;
    public String userID = "";
    public String username = "";
    public String password = "";

    public Proxy(){
        this.enabled = false;
    }

    public Proxy(boolean isSocks4, String ip, int port, String userID, String username, String password) {
        this.type = isSocks4 ? ProxyServer.ProxyType.SOCKS4 : ProxyServer.ProxyType.SOCKS5;
        this.enabled = !ip.isEmpty();
        this.ip = ip;
        this.port = port;
        this.userID = userID;
        this.username = username;
        this.password = password;
    }
}
