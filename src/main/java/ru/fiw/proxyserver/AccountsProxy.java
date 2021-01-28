package ru.fiw.proxyserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class AccountsProxy {
    private static final String ACCOUNTS_PATH = MinecraftClient.getInstance().runDirectory + "/config/ProxyAccounts.json";
    public static HashMap<String, Proxy> accounts = new HashMap<>();
    public static String lastPlayerName = "";

    public static void loadProxyAccounts() {
        File accountsFile = new File(ACCOUNTS_PATH);

        try {
            if (!accountsFile.exists()) {
                if (!accountsFile.createNewFile()) {
                    System.out.println("Error creating ProxyAccounts.json file");
                }
                return;
            }

            String accountsString = FileUtils.readFileToString(accountsFile, "UTF-8");

            Type type = new TypeToken<HashMap<String, Proxy>>() {
            }.getType();
            accounts = new Gson().fromJson(accountsString, type);
            if (accounts == null) {
                accounts = new HashMap<>();
            }
        } catch (Exception e) {
            System.out.println("Error reading ProxyAccounts.json file");
            e.printStackTrace();
        }
    }

    public static void setDefaultProxy(Proxy proxy) {
        accounts.put("", proxy);
    }

    public static void saveProxyAccounts() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileUtils.write(new File(ACCOUNTS_PATH), gson.toJson(accounts), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Error writing ProxyAccounts.json file");
            e.printStackTrace();
        }
    }
}
