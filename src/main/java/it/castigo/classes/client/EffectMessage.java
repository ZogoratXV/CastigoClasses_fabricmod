package it.castigo.classes.client;

import com.google.gson.*;
import java.util.UUID;

/** Strict bounds before a network message can allocate animation/render work. */
public record EffectMessage(String world,Shape shape,Position from,Position at,int points,int durationTicks,
                            double radius,Particles particles,Audio sound,UUID target,MeshSettings mesh,int targetEntity,UUID source,Position direction,String handle,double anchorHeight) {
    public enum Shape { BURST, LINE, RING, SPIRAL, HEALING_BEAM, MESH_RING, MESH_COLUMN, MESH_SLASH, MESH_SHIELD, MESH_BEAM, MESH_BURST, MESH_VORTEX, MESH_SIGIL, MESH_WAVE, MESH_THRUST }
    public boolean hasMesh() { return shape==Shape.HEALING_BEAM||shape.name().startsWith("MESH_"); }
    public record Position(double x,double y,double z) {
        public double distanceSquared(Position p) { return square(x-p.x)+square(y-p.y)+square(z-p.z); }
    }
    public record Particles(boolean enabled,String id,int count,double spread,int color,float size) {}
    public record Audio(boolean enabled,String id,String category,float volume,float pitch) {}
    public static EffectMessage read(JsonObject o) {
        String world=UUID.fromString(o.get("world").getAsString()).toString();
        Position from=position(o.getAsJsonArray("from")),at=position(o.getAsJsonArray("at"));
        if(from.distanceSquared(at)>128*128)throw new IllegalArgumentException("VFX segment too long");
        JsonObject p=o.getAsJsonObject("particles"),s=o.getAsJsonObject("sound");
        String category=s.get("category").getAsString();
        if(!java.util.Set.of("MASTER","MUSIC","RECORDS","WEATHER","BLOCKS","HOSTILE","NEUTRAL","PLAYERS","AMBIENT","VOICE").contains(category))throw new IllegalArgumentException("Unknown sound category");
        return new EffectMessage(world,Shape.valueOf(o.get("shape").getAsString()),from,at,
                integer(o,"points",2,32),integer(o,"durationTicks",1,40),number(o,"radius",0.1,12),
                new Particles(p.get("enabled").getAsBoolean(),identifier(p,"id"),integer(p,"count",1,32),number(p,"spread",0,2),
                        p.has("color")?integer(p,"color",0,0xFFFFFF):0xFFFFFF,p.has("size")?(float)number(p,"size",0.05,4):1.2f),
                new Audio(s.get("enabled").getAsBoolean(),identifier(s,"id"),category,(float)number(s,"volume",0,2),(float)number(s,"pitch",0.5,2)),
                o.has("target")?UUID.fromString(o.get("target").getAsString()):null,MeshSettings.read(o.has("mesh")?o.getAsJsonObject("mesh"):null),
                o.has("targetEntity")?integer(o,"targetEntity",0,Integer.MAX_VALUE):-1,o.has("source")?UUID.fromString(o.get("source").getAsString()):null,
                o.has("direction")?position(o.getAsJsonArray("direction")):new Position(0,0,1),o.has("handle")?UUID.fromString(o.get("handle").getAsString()).toString():null,
                o.has("anchorHeight")?number(o,"anchorHeight",-2,12):0);
    }
    public EffectMessage positioned(Position start,Position end,Position facing) {
        return new EffectMessage(world,shape,start,end,points,durationTicks,radius,particles,sound,target,mesh,targetEntity,source,facing,handle,anchorHeight);
    }
    public int sampleCount() { return shape==Shape.BURST?1:points; }
    public int samplesThroughTick(int age) {
        if(age<0)return 0;
        return shape==Shape.BURST?1:Math.min(sampleCount(),(sampleCount()*(Math.min(age,durationTicks)+1)+durationTicks-1)/durationTicks);
    }
    public Position sample(int index) {
        double fraction=index/(double)(points-1);
        return switch(shape) {
            case BURST,HEALING_BEAM,MESH_RING,MESH_COLUMN,MESH_SLASH,MESH_SHIELD,MESH_BEAM,MESH_BURST,MESH_VORTEX,MESH_SIGIL,MESH_WAVE,MESH_THRUST -> at;
            case LINE -> new Position(from.x+(at.x-from.x)*fraction,from.y+(at.y-from.y)*fraction,from.z+(at.z-from.z)*fraction);
            case RING -> new Position(at.x+Math.cos(2*Math.PI*index/points)*radius,at.y+0.15,at.z+Math.sin(2*Math.PI*index/points)*radius);
            case SPIRAL -> new Position(at.x+Math.cos(4*Math.PI*fraction)*radius,at.y+2*fraction,at.z+Math.sin(4*Math.PI*fraction)*radius);
        };
    }
    public boolean near(Position observer,double distance) {
        double dx=at.x-from.x,dy=at.y-from.y,dz=at.z-from.z,length=square(dx)+square(dy)+square(dz);
        double t=length<0.001?0:Math.max(0,Math.min(1,((observer.x-from.x)*dx+(observer.y-from.y)*dy+(observer.z-from.z)*dz)/length));
        return observer.distanceSquared(new Position(from.x+t*dx,from.y+t*dy,from.z+t*dz))<=distance*distance;
    }
    private static Position position(JsonArray a) {
        if(a==null||a.size()!=3)throw new IllegalArgumentException("Invalid coordinates");
        double x=a.get(0).getAsDouble(),y=a.get(1).getAsDouble(),z=a.get(2).getAsDouble();
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(x)>30000128||Math.abs(z)>30000128||Math.abs(y)>30000128)throw new IllegalArgumentException("Invalid coordinates");
        return new Position(x,y,z);
    }
    private static String identifier(JsonObject o,String key) {
        String id=o.get(key).getAsString();if(id.length()>160||!id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))throw new IllegalArgumentException("Invalid identifier");return id;
    }
    private static double number(JsonObject o,String key,double min,double max) {
        double n=o.get(key).getAsDouble();if(!Double.isFinite(n)||n<min||n>max)throw new IllegalArgumentException("Invalid "+key);return n;
    }
    private static int integer(JsonObject o,String key,int min,int max) {
        double n=number(o,key,min,max);if(n!=Math.rint(n))throw new IllegalArgumentException("Invalid integer");return (int)n;
    }
    private static double square(double v) { return v*v; }
}
