package ru.fiw.proxyserver.mixin;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Screen.class)
public interface ScreenAccessor {
    //For some reason addDrawableChild invoker dont processed normally outside of the dev environment
    //So here's what it calls
    @Accessor
    List<Drawable> getDrawables();

    @Accessor
    List<Element> getChildren();

    @Accessor
    List<Selectable> getSelectables();
}
