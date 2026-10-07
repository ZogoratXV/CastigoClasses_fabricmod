package it.castigo.classes.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HealingBeamTest {
    @Test void doubleRingsHaveDifferentHeightsAndShareTheTargetOrigin() {
        var lower=HealingBeam.sample(8,36,0,.85,0x55FF66,.65f);
        var upper=HealingBeam.sample(8,36,24,.85,0x55FF66,.65f);
        assertEquals(.12,lower.y(),1e-9);assertEquals(.50,upper.y(),1e-9);
        assertEquals(Math.hypot(lower.x(),lower.z()),Math.hypot(upper.x(),upper.z()),1e-9);
    }
    @Test void geometryIsFiniteBoundedAndFadesAtTheEnd() {
        for(int age=0;age<40;age++)for(int i=0;i<HealingBeam.SAMPLES;i++) {
            var p=HealingBeam.sample(age,40,i,12,0x55FF66,.65f);
            assertTrue(Double.isFinite(p.x())&&Double.isFinite(p.z()));
            assertTrue(Math.hypot(p.x(),p.z())<=1.60001);assertTrue(p.y()>=0&&p.y()<=3.4);
            assertTrue(p.size()>0&&p.size()<2);
        }
        assertTrue(HealingBeam.sample(39,40,48,.85,0x55FF66,.65f).size()<HealingBeam.sample(10,40,48,.85,0x55FF66,.65f).size());
    }
    @Test void assetIsBundledAsOggWithRegisteredEvent() throws Exception {
        try(var input=getClass().getResourceAsStream("/assets/castigoclasses/sounds/skills/orison.ogg")) {
            assertNotNull(input);assertArrayEquals(new byte[]{79,103,103,83},input.readNBytes(4));
        }
        try(var input=getClass().getResourceAsStream("/assets/castigoclasses/sounds.json")) {
            assertNotNull(input);assertTrue(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).contains("skill.orison"));
        }
    }
}
