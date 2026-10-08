package it.castigo.classes.client;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Extract immutable geometry first, then submit to Minecraft's translucent feature pass. */
public final class MeshRenderer {
    private record Draw(EffectMessage.Position center,MeshGeometry.Layer layer) {}
    private static final RenderStateDataKey<List<Draw>> FRAMES=RenderStateDataKey.create(()->"Castigo VFX meshes");
    private MeshRenderer() {}
    public static void register() {
        LevelExtractionEvents.END_EXTRACTION.register(context->{
            var camera=context.levelState().cameraRenderState.pos;
            var eye=new EffectMessage.Position(camera.x,camera.y,camera.z);
            var effects=new ArrayList<>(ClientEffects.meshFrames(context.level(),context.deltaTracker().getGameTimeDeltaPartialTick(false)));
            effects.sort(Comparator.comparingDouble(e->e.center().distanceSquared(eye)));
            var draws=new ArrayList<Draw>();int quads=0,instances=0;
            for(var e:effects) {
                if(++instances>32)break;
                for(var layer:MeshGeometry.build(e.effect(),e.age(),e.center().distanceSquared(eye)>24*24)) {
                    quads+=layer.vertices().size()/4;if(quads>8192)break;
                    draws.add(new Draw(e.center(),layer));
                }
            }
            context.levelState().setData(FRAMES,List.copyOf(draws));
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(context->{
            var camera=context.levelState().cameraRenderState.pos;
            for(var draw:context.levelState().getDataOrDefault(FRAMES,List.of())) {
                var pose=context.poseStack();pose.pushPose();
                try {
                    pose.translate(draw.center.x()-camera.x,draw.center.y()-camera.y,draw.center.z()-camera.z);
                    context.submitNodeCollector().submitCustomGeometry(pose,
                            RenderTypes.entityTranslucentEmissive(Identifier.parse(draw.layer.texture())),(transform,vertices)->{
                        for(var v:draw.layer.vertices())vertices.addVertex(transform,v.x(),v.y(),v.z()).setColor(v.color())
                                .setUv(v.u(),v.v()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(transform,0,1,0);
                    });
                } finally { pose.popPose(); }
            }
        });
    }
}

