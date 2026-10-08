package it.castigo.classes.client;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Portable preset used by the editor, local files and the permission-checked server endpoint. */
public final class VfxDraft {
    public static JsonObject defaults() {
        return JsonParser.parseString("""
            {"enabled":true,"shape":"RING","points":24,"duration":20,"radius":1,
            "particlesEnabled":true,"particle":"DUST","count":1,"spread":0,"color":"55FF66","size":0.65,
            "soundEnabled":false,"sound":"castigoclasses:skill.orison","volume":0.75,"pitch":1}
            """).getAsJsonObject();
    }
    public static JsonObject packet(JsonObject d,String world,EffectMessage.Position from,EffectMessage.Position at,UUID target) {
        JsonObject o=new JsonObject();o.addProperty("world",world);o.addProperty("shape",d.get("shape").getAsString());
        o.add("from",position(from));o.add("at",position(at));o.add("points",d.get("points"));o.add("durationTicks",d.get("duration"));o.add("radius",d.get("radius"));
        JsonObject p=new JsonObject();p.add("enabled",d.get("particlesEnabled"));p.addProperty("id","minecraft:"+d.get("particle").getAsString().toLowerCase(Locale.ROOT));
        p.add("count",d.get("count"));p.add("spread",d.get("spread"));
        String color=d.get("color").getAsString();if(!color.matches("[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Colore: sei cifre RGB");
        p.addProperty("color",Integer.parseInt(color,16));p.add("size",d.get("size"));o.add("particles",p);
        JsonObject s=new JsonObject();s.add("enabled",d.get("soundEnabled"));s.add("id",d.get("sound"));s.addProperty("category","PLAYERS");s.add("volume",d.get("volume"));s.add("pitch",d.get("pitch"));o.add("sound",s);
        if(target!=null)o.addProperty("target",target.toString());EffectMessage.read(o);return o;
    }
    private static JsonArray position(EffectMessage.Position p) { var a=new JsonArray();a.add(p.x());a.add(p.y());a.add(p.z());return a; }
    public static void validate(JsonObject d) { packet(d,"11111111-1111-1111-1111-111111111111",new EffectMessage.Position(0,0,0),new EffectMessage.Position(0,0,0),null); }
    public static Path path(Path root,String c,String skill,String stage) {
        for(String id:new String[]{c,skill,stage})if(!id.matches("[a-zA-Z0-9_]{1,40}"))throw new IllegalArgumentException("Nome preset non valido");
        return root.resolve(c+"__"+skill+"__"+stage+".json");
    }
    public static void save(Path root,String c,String skill,String stage,JsonObject draft) throws Exception {
        validate(draft);Files.createDirectories(root);Path file=path(root,c,skill,stage),temp=Files.createTempFile(root,"vfx-",".tmp");
        try { Files.writeString(temp,new GsonBuilder().setPrettyPrinting().create().toJson(draft));
            try { Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    public static JsonObject load(Path root,String c,String skill,String stage) throws Exception {
        Path file=path(root,c,skill,stage);if(Files.size(file)>4096)throw new IllegalArgumentException("Preset troppo grande");
        var draft=JsonParser.parseString(Files.readString(file)).getAsJsonObject();validate(draft);return draft;
    }
}
