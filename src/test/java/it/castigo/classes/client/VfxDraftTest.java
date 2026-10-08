package it.castigo.classes.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class VfxDraftTest {
    @TempDir Path directory;
    @Test void editorPresetRoundTripsAsPortableJsonAndCreatesValidEffect() throws Exception {
        var draft=VfxDraft.defaults();VfxDraft.save(directory,"mage","heal","IMPACT",draft);
        assertEquals(draft,VfxDraft.load(directory,"mage","heal","IMPACT"));
        var packet=VfxDraft.packet(draft,"11111111-1111-1111-1111-111111111111",new EffectMessage.Position(0,64,0),new EffectMessage.Position(3,64,0),null);
        assertEquals(EffectMessage.Shape.RING,EffectMessage.read(packet).shape());
    }
    @Test void invalidValuesAndPathsAreRejected() {
        var draft=VfxDraft.defaults();draft.addProperty("count",100000);assertThrows(IllegalArgumentException.class,()->VfxDraft.validate(draft));
        assertThrows(IllegalArgumentException.class,()->VfxDraft.path(directory,"../outside","heal","IMPACT"));
    }
}
