package it.castigo.classes.client.mixin;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;
@Mixin(RenderPipelines.class)
public interface CodexPipelineAccessor {
    @Accessor("GUI_TEXTURED_SNIPPET") static RenderPipeline.Snippet castigo$texturedGui(){throw new AssertionError();}
    @Invoker("register") static RenderPipeline castigo$register(RenderPipeline pipeline){throw new AssertionError();}
}
