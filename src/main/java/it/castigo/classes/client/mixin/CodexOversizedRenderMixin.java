package it.castigo.classes.client.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import it.castigo.classes.client.CodexIcons;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.pip.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(PictureInPictureRenderer.class)
public abstract class CodexOversizedRenderMixin {
    @ModifyExpressionValue(method="blitTexture",at=@At(value="FIELD",target="Lnet/minecraft/client/renderer/RenderPipelines;GUI_TEXTURED_PREMULTIPLIED_ALPHA:Lcom/mojang/blaze3d/pipeline/RenderPipeline;"))
    private RenderPipeline castigo$gray(RenderPipeline original,@Local(argsOnly=true) PictureInPictureRenderState state){return state instanceof OversizedItemRenderState item?CodexIcons.pipeline(original,item.guiItemRenderState()):original;}
}
