package it.castigo.classes.client.mixin;

import it.castigo.classes.client.CastigoClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {
    @Inject(method="onScroll",at=@At("HEAD"),cancellable=true)
    private void castigo$keepHeldItem(long window,double horizontal,double vertical,CallbackInfo ci) {
        Minecraft mc=Minecraft.getInstance();
        if(CastigoClient.isSkillMode()&&mc.player!=null&&!mc.player.isSpectator()&&mc.gui.screen()==null&&mc.gui.overlay()==null)ci.cancel();
    }
}
