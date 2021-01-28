package ru.fiw.proxyserver.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fiw.proxyserver.AccountsProxy;
import ru.fiw.proxyserver.GuiProxy;
import ru.fiw.proxyserver.ProxyServer;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenOpen {
    @Inject(method = "init()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerScreen;updateButtonActivationStates()V"))
    public void multiplayerGuiOpen(CallbackInfo ci) {
        String playerName = MinecraftClient.getInstance().getSession().getProfile().getName();
        if (!playerName.equals(AccountsProxy.lastPlayerName)) {
            AccountsProxy.lastPlayerName = playerName;
            if (AccountsProxy.accounts.containsKey(playerName)) {
                ProxyServer.proxy = AccountsProxy.accounts.get(playerName);
            } else {
                if (AccountsProxy.accounts.containsKey("")) {
                    ProxyServer.proxy = AccountsProxy.accounts.get("");
                }
            }
        }

        MultiplayerScreen ms = (MultiplayerScreen) (Object) this;
        ProxyServer.proxyMenuButton = new ButtonWidget(ms.width - 125, 5, 120, 20, new LiteralText("Proxy: " + ProxyServer.getLastUsedProxyIp()), (buttonWidget) -> {
            MinecraftClient.getInstance().openScreen(new GuiProxy(ms));
        });
        ((MultiplayerScreenInvoker) ms).invokeAddButton(ProxyServer.proxyMenuButton);
    }
}
