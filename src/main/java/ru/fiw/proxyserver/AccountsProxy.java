package ru.fiw.proxyserver;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class AccountsProxy {
    public HashMap<String, Proxy> accounts = new HashMap<>();
    private static final String ACCOUNTS_PATH = Minecraft.getMinecraft().mcDataDir + "/config/ProxyAccounts.json";
    private String lastPlayerName = "";

    public AccountsProxy() throws IOException {
        loadProxyAccounts();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void multiplayerOpen(GuiScreenEvent.InitGuiEvent.Post e) {
        if (e.getGui() instanceof GuiMultiplayer) {
            String playerName = Minecraft.getMinecraft().getSession().getProfile().getName();
            if (!playerName.equals(lastPlayerName)) {
                lastPlayerName = playerName;
                if (accounts.containsKey(playerName)) {
                    ProxyServer.proxy = accounts.get(playerName);
                }
            }
        }
    }

    private void loadProxyAccounts() throws IOException {
        File accountsFile = new File(ACCOUNTS_PATH);
        if (!accountsFile.exists()) {
            if (!accountsFile.createNewFile()) {
                return;
            }
        }

        String accountsString = new String(Files.readAllBytes(Paths.get(ACCOUNTS_PATH)), StandardCharsets.UTF_8);
        JsonParser parser = new JsonParser();
        JsonObject accountsJson = parser.parse(accountsString).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : accountsJson.entrySet()) {
            JsonObject proxyInfo = entry.getValue().getAsJsonObject();

            boolean isSocks4 = proxyInfo.get("type").getAsString().equals("SOCKS4");
            String ip = proxyInfo.get("ip").getAsString();
            int port = proxyInfo.get("port").getAsInt();
            String username = proxyInfo.get("username").getAsString();
            String password = proxyInfo.get("password").getAsString();

            Proxy accountProxy = new Proxy(isSocks4, ip, port, username, username, password);
            accounts.put(entry.getKey(), accountProxy);
        }
    }
}
