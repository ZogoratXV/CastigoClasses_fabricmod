package it.castigo.classes.client;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ClientStateTest {
    private ClientState state() {
        ClientState s=new ClientState();List<ClientState.Skill> skills=new ArrayList<>();
        for(int i=0;i<8;i++)skills.add(new ClientState.Skill("s"+i,"Skill "+i,"","minecraft:stick",0,1,10,2000));
        var c=new ClientState.ClassInfo("mago","Mago","","",1,"Fede",0x5588ff,skills);
        s.receive(JsonParser.parseString("{\"type\":\"catalog_begin\"}").getAsJsonObject());
        JsonObject o=new Gson().toJsonTree(c).getAsJsonObject();o.addProperty("type","class");s.receive(o);
        s.receive(JsonParser.parseString("{\"type\":\"catalog_end\"}").getAsJsonObject());return s;
    }
    private JsonObject packet() { return JsonParser.parseString("""
        {"type":"state","name":"Test","classId":"mago","group":"paladino","level":3,"xp":15,"xpNext":140,
         "health":24,"maxHealth":30,"resource":80,"maxResource":110,
         "slots":["s7","s1","s2","s3","s4","s5","s6","s0"],"stats":{"intelligence":13},"cooldowns":{"s7":1500}}
        """).getAsJsonObject(); }
    @Test void acceptsServerDefinedClassResourceAndServerSlotOrder() {
        ClientState s=state();s.receive(packet());assertTrue(s.active());assertEquals("Fede",s.currentClass().resourceName());
        assertEquals("s7",s.slots.getFirst());assertEquals("paladino",s.group);assertTrue(s.remaining("s7")>0);
    }
    @Test void noServerOrStaleConnectionCannotOverrideVanillaHotbar() {
        ClientState s=state();assertFalse(s.active());s.receive(packet());s.lastUpdate-=11000;assertFalse(s.active());
        s.clear();assertFalse(s.active());assertTrue(s.classes.isEmpty());
    }
    @Test void invalidSlotsAreRejectedWithoutActivatingHud() {
        ClientState s=state();JsonObject o=packet();o.getAsJsonArray("slots").set(0,new JsonPrimitive("foreign"));s.receive(o);assertFalse(s.active());
    }
    @Test void readsAuthoritativePointsAndEffectiveCombatValues() {
        ClientState s=state();JsonObject o=packet();
        o.add("statPoints",JsonParser.parseString("""
            {"available":2,"earned":5,"allocated":{"strength":3},"perPoint":{"strength":1},"canAllocate":["strength","defense"]}
            """));
        o.add("combat",JsonParser.parseString("{\"attackDamage\":7.5,\"attackSpeed\":4.12,\"defenseReductionPercent\":10}"));
        s.receive(o);assertTrue(s.statPointSystem);assertEquals(2,s.availablePoints);assertEquals(5,s.earnedPoints);
        assertEquals(3,s.allocatedPoints.get("strength"));assertTrue(s.allocatable.contains("defense"));
        assertEquals(7.5,s.combat.get("attackDamage"));
        s.clear();assertFalse(s.statPointSystem);assertEquals(0,s.availablePoints);assertTrue(s.combat.isEmpty());
    }
    @Test void olderPluginDisablesPointControlsAndClearsPreviousAllocationState() {
        ClientState s=state();s.statPointSystem=true;s.availablePoints=999;s.allocatable.add("strength");
        s.receive(packet());assertTrue(s.active());assertFalse(s.statPointSystem);
        assertEquals(0,s.availablePoints);assertTrue(s.allocatable.isEmpty());
    }
}
