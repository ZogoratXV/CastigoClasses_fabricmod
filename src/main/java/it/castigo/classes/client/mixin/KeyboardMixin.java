package it.castigo.classes.client.mixin;

import it.castigo.classes.client.CastigoClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardMixin {
    @Inject(method="keyPress",at=@At("HEAD"),cancellable=true)
    private void castigo$skillKeys(long window,int action,KeyEvent event,CallbackInfo ci) {
        if(CastigoClient.intercept(event,action))ci.cancel();
    }
}
