package it.castigo.classes.client.mixin;

import it.castigo.classes.client.CastigoClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ExperienceBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide only XP, retaining the shared contextual layer's mount jump bar and locator. */
@Mixin(ExperienceBar.class)
public abstract class ExperienceBarMixin {
    @Inject(method={"extractBackground", "extractRenderState"}, at=@At("HEAD"), cancellable=true)
    private void castigo$hideVanillaExperience(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if(CastigoClient.STATE.active())ci.cancel();
    }
}
