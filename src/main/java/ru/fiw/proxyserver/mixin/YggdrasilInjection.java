package ru.fiw.proxyserver.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import net.minecraft.client.MinecraftClient;
import ru.fiw.proxyserver.Config;
import ru.fiw.proxyserver.ProxyServer;
import ru.fiw.proxyserver.proxiedObjects.SocksProxy;

@Mixin(MinecraftClient.class)
public class YggdrasilInjection {
    @Final
    @Mutable
    @Shadow
    private YggdrasilAuthenticationService authenticationService;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initInject(CallbackInfo info) {
        String playerName = MinecraftClient.getInstance().getSession().getProfile().getName();
        if (!playerName.equals(Config.lastPlayerName)) {
            Config.lastPlayerName = playerName;
            if (Config.accounts.containsKey(playerName)) {
                ProxyServer.proxy = Config.accounts.get(playerName);
            } else {
                if (Config.accounts.containsKey("")) {
                    ProxyServer.proxy = Config.accounts.get("");
                }
            }
        }
        
        java.net.Authenticator.setDefault(new java.net.Authenticator() {
            protected java.net.PasswordAuthentication getPasswordAuthentication() {
                return new java.net.PasswordAuthentication(ProxyServer.proxy.username, ProxyServer.proxy.password.toCharArray());
            }
        });
        this.authenticationService = new YggdrasilAuthenticationService(new SocksProxy());
    }
}
