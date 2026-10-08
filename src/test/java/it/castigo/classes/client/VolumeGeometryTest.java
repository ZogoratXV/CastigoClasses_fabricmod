package it.castigo.classes.client;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VolumeGeometryTest {
    private EffectMessage effect(String shape) {
        var d=VfxDraft.defaults();d.addProperty("shape",shape);d.addProperty("duration",40);d.addProperty("radius",shape.equals("MESH_BEAM")?.6:1.65);
        var m=MeshSettings.DEFAULT.json();m.addProperty("tint",shape.equals("MESH_BURST")?"FF822E":shape.equals("MESH_BEAM")?"963DFF":"49F5AB");
        if(shape.equals("MESH_BURST"))m.addProperty("ringTexture","castigoclasses:textures/vfx/flare.png");
        if(shape.equals("MESH_BEAM"))m.addProperty("columnTexture","castigoclasses:textures/vfx/ribbon.png");
        d.add("mesh",m);
        return EffectMessage.read(VfxDraft.packet(d,"11111111-1111-1111-1111-111111111111",new EffectMessage.Position(-3,1,-3),new EffectMessage.Position(0,0,0),null));
    }
    @Test void particleMotionIsDeterministicAndAllLayersFadeWithTheEffect() {
        for(String shape:List.of("MESH_BURST","MESH_BEAM","HEALING_BEAM","MESH_RING")) {
            var e=effect(shape);var a=MeshGeometry.build(e,8,false);
            assertEquals(a,MeshGeometry.build(e,8,false));assertNotEquals(a,MeshGeometry.build(e,24,false));
            assertTrue(a.stream().anyMatch(l->l.texture().endsWith("shard.png")));
            assertTrue(MeshGeometry.build(e,40,false).isEmpty());
            int near=a.stream().mapToInt(l->l.vertices().size()).sum(),far=MeshGeometry.build(e,8,true).stream().mapToInt(l->l.vertices().size()).sum();
            assertTrue(far<near,shape);assertTrue(near<=2048,shape);
        }
    }
    @Test void burstsHaveColorSeparationAndDispersingThreeDimensionalFragments() {
        var e=effect("MESH_BURST");var a=MeshGeometry.build(e,8,false);var b=MeshGeometry.build(e,28,false);
        assertTrue(a.stream().flatMap(l->l.vertices().stream()).map(v->v.color()&0xffffff).distinct().count()>3);
        var early=a.stream().filter(l->l.texture().endsWith("shard.png")).findFirst().orElseThrow();
        var late=b.stream().filter(l->l.texture().endsWith("shard.png")).findFirst().orElseThrow();
        double r0=early.vertices().stream().mapToDouble(v->Math.hypot(v.x(),v.z())).max().orElseThrow();
        double r1=late.vertices().stream().mapToDouble(v->Math.hypot(v.x(),v.z())).max().orElseThrow();
        assertTrue(r1>r0*2);assertTrue(late.vertices().stream().map(v->v.y()).distinct().count()>8);
    }
    @Test void exportActualGeometryForVisualReview() throws Exception {
        var gallery=new LinkedHashMap<String,List<List<MeshGeometry.Layer>>>();
        for(String shape:List.of("HEALING_BEAM","MESH_BURST","MESH_BEAM","MESH_RING")) {
            var frames=new ArrayList<List<MeshGeometry.Layer>>();for(int age=0;age<40;age++)frames.add(MeshGeometry.build(effect(shape),age,false));
            gallery.put(shape,frames);
        }
        Files.createDirectories(Path.of("build"));Files.writeString(Path.of("build/vfx-preview.json"),new Gson().toJson(gallery));
        var presets=JsonParser.parseReader(new java.io.InputStreamReader(getClass().getResourceAsStream("/skill-vfx-fixtures.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        for(var entry:presets.entrySet()) {
            var preset=entry.getValue().getAsJsonObject();String effect=preset.get("effect").getAsString();
            if(!java.util.Set.of("HEAVY_STRIKE","LONG_THRUST","LOW_SWEEP","SHIELD_BASH","PRECISE_SHOT","HINDERING_SHOT","MASTER_SHOT","DOUBLE_SHOT","COVER_FIRE").contains(effect))continue;
            String stage=preset.get("class").getAsString().equals("arciere")?"trail":"cast";
            var cue=preset.getAsJsonObject("presentation").getAsJsonObject(stage);
            var d=VfxDraft.defaults();d.add("shape",cue.get("shape"));d.add("duration",cue.get("duration-ticks"));d.add("radius",cue.get("radius"));d.add("mesh",cue.get("mesh"));
            var e=EffectMessage.read(VfxDraft.packet(d,"11111111-1111-1111-1111-111111111111",new EffectMessage.Position(-3,1,-3),new EffectMessage.Position(0,0,0),null));
            var frames=new ArrayList<List<MeshGeometry.Layer>>();for(int age=0;age<40;age++)frames.add(MeshGeometry.build(e,age/40.0*e.durationTicks(),false));
            gallery.put(preset.get("name").getAsString(),frames);
        }
        Files.writeString(Path.of("build/vfx-preview.json"),new Gson().toJson(gallery));
        assertEquals(13,gallery.size());
    }
}
