package it.castigo.classes.client.mixin;
import it.castigo.classes.client.CombatAnimations;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Minecraft.class)
public abstract class CombatAttackMixin {
    @Inject(method="startAttack",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"))
    private void castigo$attack(CallbackInfoReturnable<Boolean> ci){CombatAnimations.attack((Minecraft)(Object)this);}
}
