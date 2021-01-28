package ru.fiw.proxyserver.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface MultiplayerScreenInvoker {
    @Invoker("addButton")
    public <T extends AbstractButtonWidget> T invokeAddButton(T button);
}
