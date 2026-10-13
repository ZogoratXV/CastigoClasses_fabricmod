package it.castigo.classes.client.mixin;
import com.llamalad7.mixinextras.sugar.Local;
import it.castigo.classes.client.CodexIcons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(GuiGraphicsExtractor.class)
public abstract class CodexItemExtractionMixin {
    @ModifyArg(method="item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addItem(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;)V"),index=0)
    private GuiItemRenderState castigo$mark(GuiItemRenderState state,@Local(argsOnly=true) ItemStack item){return CodexIcons.mark(state,item);}
}
