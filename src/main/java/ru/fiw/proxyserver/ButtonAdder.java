package ru.fiw.proxyserver;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ButtonAdder {
    private GuiButton button;

    @SubscribeEvent
    public void open(GuiScreenEvent.InitGuiEvent.Post e) {
        if (e.gui instanceof GuiMultiplayer) {
            ScaledResolution scale = new ScaledResolution(Minecraft.getMinecraft());
            button = new GuiButton(8000, scale.getScaledWidth() - 125, 5, 120, 20, "Proxy: " + ProxyServer.lastProxyIp);
            e.buttonList.add(button);
        }
    }

    @SubscribeEvent
    public void update(GuiScreenEvent.DrawScreenEvent.Post e) {
        if ((e.gui instanceof GuiMultiplayer)) {
            button.displayString = "Proxy: " + ProxyServer.lastProxyIp;
        }
    }

    @SubscribeEvent
    public void action(GuiScreenEvent.ActionPerformedEvent.Post e) {
        if ((e.gui instanceof GuiMultiplayer) && e.button.id == 8000) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiProxy(Minecraft.getMinecraft().currentScreen));
        }
    }
}
