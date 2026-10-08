package it.castigo.classes.client;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MeshGeometryTest {
    private EffectMessage effect(String shape,JsonObject settings) {
        var d=VfxDraft.defaults();d.addProperty("shape",shape);d.addProperty("duration",40);d.add("mesh",settings);
        return EffectMessage.read(VfxDraft.packet(d,"11111111-1111-1111-1111-111111111111",
                new EffectMessage.Position(0,64,0),new EffectMessage.Position(0,64,0),null));
    }
    @Test void healingBeamHasTwoTexturedLayersWithTwoRingsAndARealCylinder() {
        var layers=MeshGeometry.build(effect("HEALING_BEAM",MeshSettings.DEFAULT.json()),10,false);
        assertEquals(5,layers.size());assertEquals(8,layers.getFirst().vertices().size());
        assertTrue(layers.get(1).vertices().size()>=24*8*4);
        assertTrue(layers.get(1).vertices().stream().anyMatch(v->v.y()>2.8));
    }
    @Test void lifecycleFadesAndNeverEmitsBeyondLifetime() {
        var m=MeshSettings.DEFAULT;assertEquals(0,m.alpha(0));assertEquals(0,m.alpha(1));
        assertEquals(m.opacity(),m.alpha(.5),.00001);assertTrue(m.alpha(.95)<m.alpha(.5));
        var e=effect("HEALING_BEAM",m.json());assertTrue(MeshGeometry.build(e,40,false).isEmpty());
        assertTrue(MeshGeometry.build(e,0,false).isEmpty());
    }
    @Test void geometryAndUvsStayBoundedEvenWithNegativeScrollAndMaximumSettings() {
        var settings=MeshSettings.DEFAULT.json();settings.addProperty("height",12);settings.addProperty("rings",4);settings.addProperty("ringGap",3);
        for(double scroll:new double[]{-4,-.2,0,.65,4}) {
            settings.addProperty("scroll",scroll);var e=effect("HEALING_BEAM",settings);
            for(double age:new double[]{.1,5.25,20,39.5}) {
                int count=0;
                for(var layer:MeshGeometry.build(e,age,false)) {
                    assertEquals(0,layer.vertices().size()%4);count+=layer.vertices().size()/4;
                    for(var v:layer.vertices()) {
                        assertTrue(Float.isFinite(v.x())&&Float.isFinite(v.y())&&Float.isFinite(v.z()));
                        assertTrue(v.u()>=0&&v.u()<=1&&v.v()>=0&&v.v()<=1);
                        assertTrue(v.y()>=-.7&&v.y()<=12.7);assertTrue(Math.abs(v.x())<=1.5&&Math.abs(v.z())<=1.5);
                    }
                }
                assertTrue(count<=512);
            }
        }
    }
    @Test void standaloneShapesAndDistanceLodReduceGeometry() {
        var m=MeshSettings.DEFAULT.json();
        assertEquals(3,MeshGeometry.build(effect("MESH_RING",m),10,false).size());
        var column=effect("MESH_COLUMN",m);
        assertTrue(MeshGeometry.build(column,10,true).getFirst().vertices().size()<MeshGeometry.build(column,10,false).getFirst().vertices().size());
        assertTrue(MeshGeometry.build(effect("RING",m),10,false).isEmpty());
    }
    @Test void malformedMeshSettingsAreRejectedBeforeRendering() {
        for(String k:new String[]{"height","rings","rotation","opacity","fadeIn","columnRadius"}) {
            var m=MeshSettings.DEFAULT.json();m.addProperty(k,100000);assertThrows(IllegalArgumentException.class,()->effect("HEALING_BEAM",m));
        }
        var m=MeshSettings.DEFAULT.json();m.addProperty("rings",1.5);assertThrows(IllegalArgumentException.class,()->MeshSettings.read(m));
        var bad=MeshSettings.DEFAULT.json();bad.addProperty("ringTexture","castigoclasses:textures/../../secret.png");
        assertThrows(IllegalArgumentException.class,()->MeshSettings.read(bad));
    }
    @Test void oldDraftsStillLoadWithMeshDefaults() {
        var d=VfxDraft.defaults();assertFalse(d.has("mesh"));
        var e=EffectMessage.read(VfxDraft.packet(d,"11111111-1111-1111-1111-111111111111",new EffectMessage.Position(0,0,0),new EffectMessage.Position(0,0,0),null));
        assertEquals(MeshSettings.DEFAULT,e.mesh());
    }
    @Test void defaultTexturesExistAndContainTranslucentPixels() throws Exception {
        for(String texture:new String[]{"rune_ring.png","healing_column.png"}) {
            var resource=getClass().getResource("/assets/castigoclasses/textures/vfx/"+texture);assertNotNull(resource);
            var image=javax.imageio.ImageIO.read(resource);assertTrue(image.getColorModel().hasAlpha());
            int translucent=0,visible=0;
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
                int alpha=image.getRGB(x,y)>>>24;if(alpha>0)visible++;if(alpha>0&&alpha<255)translucent++;
            }
            assertTrue(visible>100);assertTrue(translucent>100);
        }
    }
}
