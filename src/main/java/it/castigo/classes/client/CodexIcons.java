package it.castigo.classes.client;

import com.mojang.blaze3d.pipeline.*;
import it.castigo.classes.client.mixin.CodexPipelineAccessor;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Desaturate only the final GUI copy, never shared models, textures or inventory items. */
public final class CodexIcons {
    private static final Identifier LOCKED=Identifier.fromNamespaceAndPath("castigoclasses","codex_locked");
    private static final Map<GuiItemRenderState,Boolean> LOCKED_STATES=Collections.synchronizedMap(new WeakHashMap<>());
    private static RenderPipeline grayscale;
    private CodexIcons(){}
    public static void initialize(){
        grayscale=CodexPipelineAccessor.castigo$register(RenderPipeline.builder(CodexPipelineAccessor.castigo$texturedGui())
            .withLocation(Identifier.fromNamespaceAndPath("castigoclasses","pipeline/codex_locked"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("castigoclasses","core/codex_locked"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)).build());
    }
    public static GuiItemRenderState mark(GuiItemRenderState state,ItemStack item){if(LOCKED.equals(item.get(DataComponents.TOOLTIP_STYLE)))LOCKED_STATES.put(state,true);return state;}
    public static RenderPipeline pipeline(RenderPipeline original,GuiItemRenderState state){return grayscale!=null&&LOCKED_STATES.containsKey(state)?grayscale:original;}
}
