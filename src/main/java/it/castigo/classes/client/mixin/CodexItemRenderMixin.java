package it.castigo.classes.client.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import it.castigo.classes.client.CodexIcons;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(GuiRenderer.class)
public abstract class CodexItemRenderMixin {
    @ModifyExpressionValue(method="submitBlitFromItemAtlas",at=@At(value="FIELD",target="Lnet/minecraft/client/renderer/RenderPipelines;GUI_TEXTURED_PREMULTIPLIED_ALPHA:Lcom/mojang/blaze3d/pipeline/RenderPipeline;"))
    private RenderPipeline castigo$gray(RenderPipeline original,@Local(argsOnly=true) GuiItemRenderState state){return CodexIcons.pipeline(original,state);}
}
