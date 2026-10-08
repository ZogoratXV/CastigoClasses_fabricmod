package it.castigo.classes.client;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import static org.junit.jupiter.api.Assertions.*;

class SpellGeometryTest {
    private static final EffectMessage.Position ZERO=new EffectMessage.Position(0,0,0);
    private EffectMessage read(JsonObject d) { return EffectMessage.read(VfxDraft.packet(d,"11111111-1111-1111-1111-111111111111",new EffectMessage.Position(0,0,-8),ZERO,null)); }
    @Test void weaponEffectsHaveVolumeParticlesAndDistanceReduction() {
        for(String shape:java.util.List.of("MESH_SLASH","MESH_THRUST")) {
            var d=VfxDraft.defaults();d.addProperty("shape",shape);d.addProperty("radius",2);
            var e=read(d);var near=MeshGeometry.build(e,8,false);var far=MeshGeometry.build(e,8,true);
            assertTrue(near.stream().anyMatch(l->l.texture().endsWith("shard.png")));
            assertTrue(near.stream().mapToInt(l->l.vertices().size()).sum()>far.stream().mapToInt(l->l.vertices().size()).sum());
            assertTrue(near.stream().flatMap(l->l.vertices().stream()).map(v->v.y()).distinct().count()>20);
            assertNotEquals(near,MeshGeometry.build(e,12,false));
            assertTrue(MeshGeometry.build(e,e.durationTicks(),false).isEmpty());
        }
    }
    @Test void everyShippedSkillDecodesAndRendersWithTexturesPresent() {
        var presets=JsonParser.parseReader(new InputStreamReader(getClass().getResourceAsStream("/skill-vfx-fixtures.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(48,presets.size());int cues=0;
        for(var entry:presets.entrySet())for(var stage:entry.getValue().getAsJsonObject().getAsJsonObject("presentation").entrySet()) {
            var c=stage.getValue().getAsJsonObject();if(!c.get("enabled").getAsBoolean())continue;
            var d=VfxDraft.defaults();d.add("shape",c.get("shape"));d.add("duration",c.get("duration-ticks"));d.add("radius",c.get("radius"));d.add("mesh",c.get("mesh"));
            var e=read(d);var layers=MeshGeometry.build(e,e.durationTicks()*.4,false);
            assertFalse(layers.isEmpty(),entry.getKey()+"/"+stage.getKey());cues++;
            for(var layer:layers) {
                assertEquals(0,layer.vertices().size()%4);String asset="/assets/"+layer.texture().replace(':','/');
                assertNotNull(getClass().getResource(asset),asset);
                assertTrue(layer.vertices().stream().anyMatch(v->(v.color()>>>24)>0));
            }
        }
        assertTrue(cues>100);
    }
    @Test void allMeshShapesAreBoundedFiniteAndProduceValidQuadsAtExtremeSettings() {
        for(var shape:EffectMessage.Shape.values()) {
            var d=VfxDraft.defaults();d.addProperty("shape",shape.name());d.addProperty("radius",12);d.addProperty("duration",40);
            var m=MeshSettings.DEFAULT.json();m.addProperty("height",12);m.addProperty("rotation",720);d.add("mesh",m);var e=read(d);
            if(!e.hasMesh())continue;
            for(double age:new double[]{.01,5,20,39.9})for(boolean far:new boolean[]{false,true}) {
                int vertices=0;
                for(var layer:MeshGeometry.build(e,age,far))for(var v:layer.vertices()) {
                    vertices++;assertTrue(Float.isFinite(v.x())&&Float.isFinite(v.y())&&Float.isFinite(v.z()));
                    assertTrue(Math.abs(v.x())<40&&Math.abs(v.y())<40&&Math.abs(v.z())<40);
                    assertTrue(v.u()>=0&&v.u()<=1&&v.v()>=0&&v.v()<=1);
                }
                assertTrue(vertices>0&&vertices<=2048,shape.name());assertEquals(0,vertices%4);
            }
        }
    }
    @Test void thrustRotatesWithFacingAndLoopGeometryDoesNotExpireItsVisualPhase() {
        var d=VfxDraft.defaults();d.addProperty("shape","MESH_THRUST");var e=read(d).positioned(ZERO,ZERO,new EffectMessage.Position(1,0,0));
        var vertices=MeshGeometry.build(e,10,false).getFirst().vertices();assertTrue(vertices.stream().allMatch(v->Math.abs(v.z())<=.11));assertTrue(vertices.stream().anyMatch(v->v.x()>.8));
        d.addProperty("shape","MESH_RING");var packet=VfxDraft.packet(d,"11111111-1111-1111-1111-111111111111",ZERO,ZERO,null);
        packet.addProperty("handle","22222222-2222-2222-2222-222222222222");
        assertFalse(MeshGeometry.build(EffectMessage.read(packet),10000,false).isEmpty());
        packet.addProperty("handle","bad");assertThrows(IllegalArgumentException.class,()->EffectMessage.read(packet));
    }
}
