package it.castigo.classes.client;

import com.google.gson.*;
import java.util.*;

public final class ClientState {
    public record Skill(String id,String name,String description,String icon,int color,int unlockLevel,
                        double cost,long cooldownMs) {}
    public record ClassInfo(String id,String name,String description,String parent,int requiredLevel,
                            String resourceName,int resourceColor,List<Skill> skills) {
        public Skill skill(String id) { return skills.stream().filter(s->s.id().equals(id)).findFirst().orElse(null); }
    }
    private final Gson gson=new Gson();
    public final Map<String,ClassInfo> classes=new LinkedHashMap<>();
    private final Map<String,ClassInfo> pending=new LinkedHashMap<>();
    public boolean enabled;
    public String name="",classId="",group="";
    public int level=1;
    public long xp,xpNext;
    public double health,maxHealth,resource,maxResource;
    public List<String> slots=List.of();
    public final Map<String,Double> stats=new LinkedHashMap<>();
    public boolean statPointSystem;
    public int availablePoints,earnedPoints;
    public final Map<String,Double> allocatedPoints=new LinkedHashMap<>();
    public final Map<String,Double> pointGains=new LinkedHashMap<>();
    public final Map<String,Double> combat=new LinkedHashMap<>();
    public final Set<String> allocatable=new HashSet<>();
    public final Map<String,Long> cooldownEnds=new HashMap<>();
    public long lastUpdate;
    public void clear() {
        enabled=false;classes.clear();pending.clear();slots=List.of();cooldownEnds.clear();stats.clear();lastUpdate=0;
        clearPoints();combat.clear();
    }
    public ClassInfo currentClass() { return classes.get(classId); }
    public boolean active() { return enabled&&currentClass()!=null&&slots.size()==8&&System.currentTimeMillis()-lastUpdate<10000; }
    public void receive(JsonObject o) {
        switch(o.get("type").getAsString()) {
            case "catalog_begin" -> pending.clear();
            case "class" -> {
                ClassInfo c=gson.fromJson(o,ClassInfo.class);
                if(c!=null&&c.id()!=null&&c.name()!=null&&c.resourceName()!=null&&c.skills()!=null&&c.skills().size()==8&&pending.size()<256)pending.put(c.id(),c);
            }
            case "catalog_end" -> { classes.clear();classes.putAll(pending);pending.clear(); }
            case "state" -> {
                String next=o.get("classId").getAsString();ClassInfo c=classes.get(next);if(c==null)return;
                List<String> order=new ArrayList<>();o.getAsJsonArray("slots").forEach(e->order.add(e.getAsString()));
                if(!SlotOrder.valid(order,c.skills().stream().map(Skill::id).toList()))return;
                name=o.get("name").getAsString();classId=next;group=o.get("group").getAsString();
                level=o.get("level").getAsInt();xp=o.get("xp").getAsLong();xpNext=o.get("xpNext").getAsLong();
                health=number(o,"health");maxHealth=number(o,"maxHealth");resource=number(o,"resource");maxResource=number(o,"maxResource");
                slots=List.copyOf(order);stats.clear();o.getAsJsonObject("stats").entrySet().forEach(e->stats.put(e.getKey(),e.getValue().getAsDouble()));
                clearPoints();readNumbers(o,"combat",combat);
                if(o.has("statPoints")&&o.get("statPoints").isJsonObject()) {
                    JsonObject points=o.getAsJsonObject("statPoints");
                    availablePoints=(int)number(points,"available");earnedPoints=(int)number(points,"earned");
                    readNumbers(points,"allocated",allocatedPoints);readNumbers(points,"perPoint",pointGains);
                    if(points.has("canAllocate"))points.getAsJsonArray("canAllocate").forEach(e->allocatable.add(e.getAsString()));
                    statPointSystem=true;
                }
                cooldownEnds.clear();long now=System.currentTimeMillis();
                o.getAsJsonObject("cooldowns").entrySet().forEach(e->cooldownEnds.put(e.getKey(),now+Math.max(0,Math.min(86_400_000,e.getValue().getAsLong()))));
                lastUpdate=now;enabled=true;
            }
            case "disabled" -> clear();
            default -> { }
        }
    }
    private static double number(JsonObject o,String key) {
        double n=o.get(key).getAsDouble();return Double.isFinite(n)?Math.max(0,n):0;
    }
    private void clearPoints() {
        statPointSystem=false;availablePoints=0;earnedPoints=0;allocatedPoints.clear();pointGains.clear();allocatable.clear();
    }
    private static void readNumbers(JsonObject o,String key,Map<String,Double> target) {
        target.clear();
        if(o.has(key)&&o.get(key).isJsonObject())o.getAsJsonObject(key).entrySet().forEach(e-> {
            double n=e.getValue().getAsDouble();if(Double.isFinite(n)&&n>=0)target.put(e.getKey(),n);
        });
    }
    public long remaining(String id) { return Math.max(0,cooldownEnds.getOrDefault(id,0L)-System.currentTimeMillis()); }
}
