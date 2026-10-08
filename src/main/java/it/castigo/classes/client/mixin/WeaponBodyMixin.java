package it.castigo.classes.client.mixin;

import it.castigo.classes.client.WeaponAnimations;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class WeaponBodyMixin {
    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",at=@At("TAIL"))
    private void castigo$pose(HumanoidRenderState state,CallbackInfo ci) { WeaponAnimations.thirdPerson(state,(HumanoidModel<?>)(Object)this); }
}
