package it.castigo.classes.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import it.castigo.classes.client.WeaponAnimations;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class WeaponHandMixin {
    @ModifyVariable(method="submitArmWithItem",at=@At("HEAD"),argsOnly=true,ordinal=2)
    private float castigo$vanillaSwing(float attack,AbstractClientPlayer player,float delta,float pitch,InteractionHand hand,float original,ItemStack item,float equip,PoseStack pose,SubmitNodeCollector collector,int light){return it.castigo.classes.client.CombatAnimations.active(player.getId(),hand)?0:attack;}
    @Shadow protected abstract void renderPlayerArm(PoseStack pose,SubmitNodeCollector collector,int light,float equip,float attack,HumanoidArm arm);
    @Inject(method="submitArmWithItem",at=@At("HEAD"))
    private void castigo$begin(AbstractClientPlayer player,float delta,float pitch,InteractionHand hand,float attack,ItemStack item,float equip,PoseStack pose,SubmitNodeCollector collector,int light,CallbackInfo ci) {
        pose.pushPose();if(!player.isUsingItem())WeaponAnimations.firstPerson(player.getId(),hand,pose);
        if(hand==InteractionHand.MAIN_HAND&&(WeaponAnimations.gripping(player.getId())||it.castigo.classes.client.CombatAnimations.twoHanded(player.getId()))&&!player.isInvisible()){
            pose.pushPose();float sign=player.getMainArm()==HumanoidArm.LEFT?-1:1;
            pose.translate(.85f*sign,-.08f,-.08f);if(it.castigo.classes.client.CombatAnimations.active(player.getId(),hand))it.castigo.classes.client.CombatAnimations.supportHand(player.getId(),pose,sign);else WeaponAnimations.supportHand(player.getId(),pose,sign);
            renderPlayerArm(pose,collector,light,equip,attack,player.getMainArm().getOpposite());pose.popPose();
        }
    }
    @Inject(method="submitArmWithItem",at=@At("RETURN"))
    private void castigo$end(AbstractClientPlayer player,float delta,float pitch,InteractionHand hand,float attack,ItemStack item,float equip,PoseStack pose,SubmitNodeCollector collector,int light,CallbackInfo ci) { pose.popPose(); }
}
