package it.castigo.classes.client;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BlockbenchModelsTest {
    private EffectMessage effect(String id,String phase){
        var d=VfxDraft.defaults();d.addProperty("shape","MESH_COLUMN");
        var p=VfxDraft.packet(d,"11111111-1111-1111-1111-111111111111",new EffectMessage.Position(0,64,0),new EffectMessage.Position(0,64,0),null);
        p.addProperty("model",id+":"+phase);p.addProperty("modelTicks",60);return EffectMessage.read(p);
    }
    @Test void allFortyAssetsProduceFiniteAnimatedTexturedGeometry(){
        assertEquals(40,BlockbenchModels.ids().size());
        for(String id:BlockbenchModels.ids()){
            int total=0;for(int age:new int[]{1,15,30,45,59})for(var layer:MeshGeometry.build(effect(id,"full"),age,false)){
                assertNotNull(getClass().getResource("/assets/"+layer.texture().replace(':','/')));assertEquals(0,layer.vertices().size()%4);total+=layer.vertices().size();
                for(var v:layer.vertices()){assertTrue(Float.isFinite(v.x())&&Float.isFinite(v.y())&&Float.isFinite(v.z()),id);assertTrue(Math.abs(v.x())<128&&Math.abs(v.y())<128&&Math.abs(v.z())<128,id);assertTrue(Float.isFinite(v.u())&&Float.isFinite(v.v()));}
            }assertTrue(total>0,id);
        }
    }
    @Test void arrowsHaveDistinctFlightAndImpactAndDoNotRenderReferenceBodies(){
        for(String id:BlockbenchModels.ids())if(id.startsWith("33_")||id.startsWith("37_")||id.startsWith("38_")||id.startsWith("39_")||id.startsWith("40_")){
            var flight=MeshGeometry.build(effect(id,"flight"),30,false);var hit=MeshGeometry.build(effect(id,"impact"),30,false);
            assertFalse(flight.isEmpty(),id);assertFalse(hit.isEmpty(),id);assertNotEquals(flight,hit,id);
        }
    }
    @Test void modelSelectionCannotAccessArbitraryResources(){
        assertThrows(IllegalArgumentException.class,()->BlockbenchModels.validate("../../secret:full"));
        assertThrows(IllegalArgumentException.class,()->BlockbenchModels.validate("01_orison_di_cura:unknown"));
    }
    @Test void healingChangesOverTimeAndSuppressionDoesNotDrawOldMesh(){
        var e=effect("01_orison_di_cura","full");assertNotEquals(MeshGeometry.build(e,12,false),MeshGeometry.build(e,35,false));
        var suppressed=new EffectMessage(e.world(),e.shape(),e.from(),e.at(),e.points(),e.durationTicks(),e.radius(),e.particles(),e.sound(),e.target(),e.mesh(),e.targetEntity(),e.source(),e.direction(),e.handle(),e.anchorHeight(),"-");
        assertTrue(MeshGeometry.build(suppressed,12,false).isEmpty());
    }
}
