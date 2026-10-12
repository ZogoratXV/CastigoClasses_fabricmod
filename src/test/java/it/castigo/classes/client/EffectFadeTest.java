package it.castigo.classes.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EffectFadeTest {
    @Test void endingPreservesTintAndFadesExistingAlpha() {
        assertEquals(0x8059229B,MeshRenderer.fadedColor(0x8059229B,1));
        assertEquals(0x4059229B,MeshRenderer.fadedColor(0x8059229B,.5f));
        assertEquals(0x0059229B,MeshRenderer.fadedColor(0x8059229B,0));
    }
}
