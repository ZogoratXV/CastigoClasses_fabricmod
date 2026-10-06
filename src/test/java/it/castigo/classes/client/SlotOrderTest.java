package it.castigo.classes.client;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SlotOrderTest {
    private final List<String> ids=List.of("a","b","c","d","e","f","g","h");
    @Test void dragSwapPreservesEightUniqueSkillsAndLeavesOriginalUntouched() {
        var reordered=SlotOrder.swap(ids,0,7);assertEquals("h",reordered.getFirst());
        assertEquals("a",ids.getFirst());assertTrue(SlotOrder.valid(reordered,ids));
    }
    @Test void untrustedDuplicateOrUnknownSkillIsRejected() {
        assertFalse(SlotOrder.valid(List.of("a","a","c","d","e","f","g","h"),ids));
        assertFalse(SlotOrder.valid(List.of("a","b","c","d","e","f","g","unknown"),ids));
    }
    @Test void ninthSlotCannotBeUsed() { assertThrows(IllegalArgumentException.class,()->SlotOrder.swap(ids,0,8)); }
}
