package it.castigo.classes.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import it.castigo.classes.client.WeaponAnimations;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class WeaponHandMixin {
    @Inject(method="submitArmWithItem",at=@At("HEAD"))
    private void castigo$begin(AbstractClientPlayer player,float delta,float pitch,InteractionHand hand,float attack,ItemStack item,float equip,PoseStack pose,SubmitNodeCollector collector,int light,CallbackInfo ci) {
        pose.pushPose();if(!player.isUsingItem())WeaponAnimations.firstPerson(player.getId(),hand,pose);
    }
    @Inject(method="submitArmWithItem",at=@At("RETURN"))
    private void castigo$end(AbstractClientPlayer player,float delta,float pitch,InteractionHand hand,float attack,ItemStack item,float equip,PoseStack pose,SubmitNodeCollector collector,int light,CallbackInfo ci) { pose.popPose(); }
}
