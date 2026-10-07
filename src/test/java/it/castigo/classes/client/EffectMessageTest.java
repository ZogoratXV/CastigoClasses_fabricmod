package it.castigo.classes.client;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EffectMessageTest {
    private JsonObject packet() {
        return JsonParser.parseString("""
            {"world":"11111111-1111-1111-1111-111111111111","shape":"LINE","from":[0,64,0],"at":[10,64,0],
             "points":24,"durationTicks":8,"radius":3,
             "particles":{"enabled":true,"id":"minecraft:dust","count":1,"spread":0.08,"color":12290303,"size":1.2},
             "sound":{"enabled":false,"id":"minecraft:block.amethyst_block.chime","category":"PLAYERS","volume":0.7,"pitch":1}}
            """).getAsJsonObject();
    }
    @Test void receivesPluginCueAndInterpolatesLineEndpoints() {
        var e=EffectMessage.read(packet());assertEquals(24,e.sampleCount());assertEquals(e.from(),e.sample(0));assertEquals(e.at(),e.sample(23));
        assertEquals(8,e.durationTicks());assertEquals("minecraft:dust",e.particles().id());
    }
    @Test void checksDistanceToSegmentIncludingMiddle() {
        var e=EffectMessage.read(packet());assertTrue(e.near(new EffectMessage.Position(5,64,2),3));assertFalse(e.near(new EffectMessage.Position(5,64,70),64));
    }
    @Test void animationDistributesSamplesOnceOverItsLifetime() {
        for(int duration:new int[]{1,8,40}) {
            var p=packet();p.addProperty("durationTicks",duration);var e=EffectMessage.read(p);
            int previous=0,total=0;
            for(int age=0;age<duration;age++) { int end=e.samplesThroughTick(age);assertTrue(end>=previous);total+=end-previous;previous=end; }
            assertEquals(24,total);assertEquals(24,e.samplesThroughTick(100));
        }
    }
    @Test void shapesProduceBoundedGeometry() {
        for(String shape:new String[]{"BURST","RING","SPIRAL"}) {
            var p=packet();p.addProperty("shape",shape);var e=EffectMessage.read(p);
            for(int i=0;i<e.sampleCount();i++)assertTrue(e.sample(i).distanceSquared(e.at())<=13.01);
        }
    }
    @Test void rejectsOutOfBoundsCountsDurationsAndCoordinates() {
        for(String key:new String[]{"points","durationTicks","radius"}) {
            var p=packet();p.addProperty(key,100000);assertThrows(IllegalArgumentException.class,()->EffectMessage.read(p));
        }
        var p=packet();p.getAsJsonArray("at").set(0,new JsonPrimitive(1000));assertThrows(IllegalArgumentException.class,()->EffectMessage.read(p));
        var n=packet();n.getAsJsonArray("from").set(1,new JsonPrimitive(Double.NaN));assertThrows(IllegalArgumentException.class,()->EffectMessage.read(n));
    }
    @Test void rejectsInvalidSoundParticleAndWorldIdentifiers() {
        var p=packet();p.getAsJsonObject("sound").addProperty("id","bad sound");assertThrows(IllegalArgumentException.class,()->EffectMessage.read(p));
        var q=packet();q.getAsJsonObject("particles").addProperty("count",1.5);assertThrows(IllegalArgumentException.class,()->EffectMessage.read(q));
        var r=packet();r.addProperty("world","not-a-world");assertThrows(IllegalArgumentException.class,()->EffectMessage.read(r));
    }
}
