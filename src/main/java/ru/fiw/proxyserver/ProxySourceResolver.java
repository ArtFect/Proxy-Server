package ru.fiw.proxyserver;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ProxySourceResolver {
    public static final String DEFAULT_PROXY_LIST_FILE = "config/proxy-list.txt";

    private static final String PROPERTY_PROXY = "proxyserver.proxy";
    private static final String PROPERTY_PROXY_FILE = "proxyserver.proxyFile";
    private static final String ARG_PROXY = "--proxy=";
    private static final String ARG_PROXY_FILE = "--proxy-file=";

    private ProxySourceResolver() {
    }

    public static void applyRuntimeOverrides() {
        if (applyFromString(System.getProperty(PROPERTY_PROXY), "system property " + PROPERTY_PROXY)) {
            return;
        }

        if (applyFromFileReference(System.getProperty(PROPERTY_PROXY_FILE), "system property " + PROPERTY_PROXY_FILE)) {
            return;
        }

        String[] launchArguments = FabricLoader.getInstance().getLaunchArguments(true);
        for (String argument : launchArguments) {
            if (argument.startsWith(ARG_PROXY)) {
                if (applyFromString(argument.substring(ARG_PROXY.length()), "command line argument --proxy")) {
                    return;
                }
            } else if (argument.startsWith(ARG_PROXY_FILE)) {
                if (applyFromFileReference(argument.substring(ARG_PROXY_FILE.length()), "command line argument --proxy-file")) {
                    return;
                }
            }
        }
    }

    public static Optional<Proxy> loadDefaultFile() {
        return loadFirstValidFromFile(getDefaultProxyListPath());
    }

    public static Path getDefaultProxyListPath() {
        return FabricLoader.getInstance().getGameDir().resolve(DEFAULT_PROXY_LIST_FILE);
    }

    public static Optional<Proxy> parseProxy(String raw) {
        if (raw == null) {
            return Optional.empty();
        }

        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return Optional.empty();
        }

        Proxy.ProxyType type = Proxy.ProxyType.SOCKS5;
        String working = trimmed;
        int schemeIdx = trimmed.indexOf("://");
        if (schemeIdx > 0) {
            String scheme = trimmed.substring(0, schemeIdx).toLowerCase(Locale.ROOT);
            if ("socks4".equals(scheme)) {
                type = Proxy.ProxyType.SOCKS4;
            } else if ("socks5".equals(scheme)) {
                type = Proxy.ProxyType.SOCKS5;
            } else {
                System.out.println("[ProxyServer] Unsupported proxy scheme: " + scheme);
                return Optional.empty();
            }
            working = trimmed.substring(schemeIdx + 3);
        }

        String[] parts = working.split(":", 4);
        if (parts.length < 2) {
            System.out.println("[ProxyServer] Unable to parse proxy string (expected ip:port[:username[:password]]): " + raw);
            return Optional.empty();
        }

        String ip = parts[0].trim();
        String portPart = parts[1].trim();
        if (ip.isEmpty()) {
            System.out.println("[ProxyServer] Missing proxy host in: " + raw);
            return Optional.empty();
        }

        int port;
        try {
            port = Integer.parseInt(portPart);
        } catch (NumberFormatException e) {
            System.out.println("[ProxyServer] Invalid proxy port in: " + raw);
            return Optional.empty();
        }

        if (port <= 0 || port > 65535) {
            System.out.println("[ProxyServer] Proxy port out of range in: " + raw);
            return Optional.empty();
        }

        String username = parts.length >= 3 ? parts[2] : "";
        String password = parts.length == 4 ? parts[3] : "";

        Proxy proxy = new Proxy();
        proxy.type = type;
        proxy.ipPort = ip + ":" + port;
        proxy.username = username;
        proxy.password = password;

        return Optional.of(proxy);
    }

    public static Optional<Proxy> loadFirstValidFromFile(Path path) {
        if (!Files.exists(path)) {
            System.out.println("[ProxyServer] Proxy file not found: " + path);
            return Optional.empty();
        }

        try (Stream<String> lines = Files.lines(path)) {
            List<Proxy> proxies = lines
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(ProxySourceResolver::parseProxy)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());

            if (proxies.isEmpty()) {
                return Optional.empty();
            }

            Proxy randomProxy = proxies.get(ThreadLocalRandom.current().nextInt(proxies.size()));
            return Optional.of(randomProxy);
        } catch (IOException e) {
            System.out.println("[ProxyServer] Failed to read proxy file " + path + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    private static boolean applyFromString(String raw, String source) {
        Optional<Proxy> proxyOpt = parseProxy(raw);
        if (proxyOpt.isEmpty()) {
            return false;
        }

        applyProxy(proxyOpt.get(), source);
        return true;
    }

    private static boolean applyFromFileReference(String reference, String source) {
        if (reference == null || reference.isBlank()) {
            return false;
        }

        Path path = resolvePath(reference.trim());
        Optional<Proxy> proxyOpt = loadFirstValidFromFile(path);
        if (proxyOpt.isPresent()) {
            applyProxy(proxyOpt.get(), source + " -> " + path);
            return true;
        }

        System.out.println("[ProxyServer] No valid proxy entries found in: " + path);
        return false;
    }

    private static Path resolvePath(String value) {
        Path path = Path.of(value);
        if (path.isAbsolute()) {
            return path;
        }
        return FabricLoader.getInstance().getGameDir().resolve(path).normalize();
    }

    private static void applyProxy(Proxy proxy, String source) {
        ProxyServer.proxy = proxy;
        ProxyServer.proxyEnabled = true;
        ProxyServer.lastUsedProxy = proxy;
        Config.setDefaultProxy(proxy);
        Config.saveConfig();
        System.out.println("[ProxyServer] Loaded proxy from " + source + ": " + proxy.ipPort);
    }
}
