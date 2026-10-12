package it.castigo.classes.client;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Compiled, bundled Generic models. Network messages select an allowlisted model only. */
public final class BlockbenchModels {
    private record Quad(int texture,float[] vertices) {}
    private record Node(String name,int parent,Vector3f origin,Vector3f rotation,List<Quad> quads) {}
    private record Texture(String path,int frames,int ticks) {}
    private record Key(float time,String interpolation,Vector3f before,Vector3f after) {}
    private record Clip(String name,float length,Map<Integer,Map<String,List<Key>>> channels) {}
    private record Model(List<Node> nodes,List<Texture> textures,List<Clip> clips) {}
    private static final Map<String,Model> CACHE=new HashMap<>();
    private static final Set<String> IDS=loadIds();
    private static final Map<String,float[]> IMPACT_WINDOWS=new HashMap<>();
    private BlockbenchModels() {}
    private static JsonElement json(String name) {
        try(var stream=BlockbenchModels.class.getResourceAsStream("/assets/castigoclasses/models/blockbench/"+name+".json")) {
            if(stream==null)throw new IllegalArgumentException("Missing bundled model "+name);
            return JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8));
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    private static Set<String> loadIds(){var ids=new HashSet<String>();for(var e:json("index").getAsJsonArray())ids.add(e.getAsJsonObject().get("id").getAsString());return Set.copyOf(ids);}
    public static Set<String> ids(){return IDS;}
    public static String validate(String selection){if(selection.equals("-"))return selection;String[] p=selection.split(":",-1);if(p.length!=2||!IDS.contains(p[0])||!Set.of("full","loop","flight","impact","end").contains(p[1]))throw new IllegalArgumentException("Unknown bundled VFX");return selection;}
    private static Vector3f vector(JsonElement e){var a=e.getAsJsonArray();return new Vector3f(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat());}
    private static Model model(String id){return CACHE.computeIfAbsent(id,key->{
        var data=json(id).getAsJsonObject();var nodes=new ArrayList<Node>();var textures=new ArrayList<Texture>();var clips=new ArrayList<Clip>();
        for(var e:data.getAsJsonArray("nodes")){var n=e.getAsJsonObject();var quads=new ArrayList<Quad>();for(var q:n.getAsJsonArray("quads")){var a=q.getAsJsonArray();var values=a.get(1).getAsJsonArray();float[] v=new float[20];for(int i=0;i<20;i++)v[i]=values.get(i).getAsFloat();quads.add(new Quad(a.get(0).getAsInt(),v));}nodes.add(new Node(n.get("name").getAsString(),n.get("parent").getAsInt(),vector(n.get("origin")),vector(n.get("rotation")),List.copyOf(quads)));}
        for(var e:data.getAsJsonArray("textures")){var t=e.getAsJsonObject();textures.add(new Texture(t.get("path").getAsString(),t.get("frames").getAsInt(),t.get("ticks").getAsInt()));}
        for(var e:data.getAsJsonArray("clips")){var c=e.getAsJsonObject();var channels=new HashMap<Integer,Map<String,List<Key>>>();for(var entry:c.getAsJsonObject("channels").entrySet()){var tracks=new HashMap<String,List<Key>>();for(var tr:entry.getValue().getAsJsonObject().entrySet()){var keys=new ArrayList<Key>();for(var k:tr.getValue().getAsJsonArray()){var a=k.getAsJsonArray();keys.add(new Key(a.get(0).getAsFloat(),a.get(1).getAsString(),vector(a.get(2)),vector(a.get(3))));}tracks.put(tr.getKey(),List.copyOf(keys));}channels.put(Integer.parseInt(entry.getKey()),tracks);}clips.add(new Clip(c.get("name").getAsString(),c.get("length").getAsFloat(),channels));}
        return new Model(List.copyOf(nodes),List.copyOf(textures),List.copyOf(clips));
    });}
    private static Vector3f sample(List<Key> keys,float time,float fallback){
        if(keys==null||keys.isEmpty())return new Vector3f(fallback);
        if(time<keys.getFirst().time)return new Vector3f(keys.getFirst().before);
        int i=0;while(i+1<keys.size()&&keys.get(i+1).time<=time)i++;
        var a=keys.get(i);if(i+1==keys.size()||a.interpolation.equals("step"))return new Vector3f(a.after);
        var b=keys.get(i+1);float t=(time-a.time)/(b.time-a.time);
        if(!a.interpolation.equals("catmullrom")&&!b.interpolation.equals("catmullrom"))return new Vector3f(a.after).lerp(b.before,t);
        var p=keys.get(Math.max(0,i-1)).after;var q=keys.get(Math.min(keys.size()-1,i+2)).before;
        return new Vector3f(cat(p.x,a.after.x,b.before.x,q.x,t),cat(p.y,a.after.y,b.before.y,q.y,t),cat(p.z,a.after.z,b.before.z,q.z,t));
    }
    private static float cat(float p,float a,float b,float q,float t){return .5f*((2*a)+(-p+b)*t+(2*p-5*a+4*b-q)*t*t+(-p+3*a-3*b+q)*t*t*t);}
    private static boolean impact(String name){return name.contains("impatto")||name.startsWith("schegg")||name.startsWith("frammento_")||name.startsWith("vincolo_");}
    private static boolean selectedRoot(String name,boolean flight,int number){
        if(flight){
            if(number>=38)return name.equals("freccia_0");
            return name.startsWith("freccia")||name.startsWith("scia")||name.startsWith("particella")||name.startsWith("dardo");
        }
        if(number==38||number==39)return name.equals("impatto_0")||name.startsWith("scheggia_0_");
        return impact(name);
    }
    private static float[] impactWindow(Model m,Clip clip,int number){
        float start=clip.length,end=0;
        for(int i=0;i<m.nodes.size();i++)if(m.nodes.get(i).parent<0&&selectedRoot(m.nodes.get(i).name,false,number)){
            var keys=clip.channels.getOrDefault(i,Map.of()).get("scale");if(keys==null)continue;
            for(int k=0;k<keys.size();k++)if(keys.get(k).after.lengthSquared()>.00001){start=Math.min(start,keys.get(Math.max(0,k-1)).time);end=Math.max(end,keys.get(Math.min(keys.size()-1,k+1)).time);}
        }
        return end>start?new float[]{start,end}:new float[]{0,clip.length};
    }
    public static List<MeshGeometry.Layer> build(EffectMessage e,double age,boolean distant){
        if(e.model()==null||e.model().equals("-"))return List.of();String[] parts=e.model().split(":");var m=model(parts[0]);String phase=parts[1];int number=Integer.parseInt(parts[0].substring(0,2));
        Clip clip=m.clips.getFirst();if(phase.equals("impact"))for(var c:m.clips)if(c.name.contains("impatto")){clip=c;break;}
        if(phase.equals("loop"))for(var c:m.clips)if(c.name.contains("loop")){clip=c;break;}
        if(phase.equals("end"))for(var c:m.clips)if(c.name.contains("scomparsa")){clip=c;break;}
        boolean projectile=number==2||number==9||number==33||number>=37;
        boolean flight=phase.equals("flight"),hit=phase.equals("impact")&&projectile;
        float time=phase.equals("loop")?(float)(age/20%clip.length):(float)Math.clamp(age/e.durationTicks(),0,1)*clip.length;
        if(number==8&&phase.equals("loop")){
            if(age<32){for(var c:m.clips)if(c.name.contains("comparsa")&&!c.name.contains("scomparsa")){clip=c;time=(float)age/20;break;}}
            else time=(float)((age-32)/20%clip.length);
        }
        if(flight)time=clip.length*.35f;
        if(hit&&m.clips.size()==1){float[] window=IMPACT_WINDOWS.get(parts[0]);if(window==null){window=impactWindow(m,clip,number);IMPACT_WINDOWS.put(parts[0],window);}time=window[0]+(window[1]-window[0])*(float)Math.clamp(age/e.durationTicks(),0,1);}
        float yaw=(float)Math.atan2(-e.direction().x(),-e.direction().z());
        var world=new Matrix4f().rotateY(yaw);
        if(flight){double horizontal=Math.hypot(e.direction().x(),e.direction().z());world.rotateX((float)Math.atan2(e.direction().y(),horizontal));}
        Vector3f center=number<=15?new Vector3f(8,0,8):new Vector3f();
        if(number==21)center.set(20,0,0);
        if(number==2||number==9)center.set(0,8,hit&&number==2?-17:0);
        if(projectile&&number>=33)center.set(0,number==33?18:number==37?7:16,hit?-66:0);
        if(projectile&&(flight||hit))for(var node:m.nodes)if(node.parent<0&&Set.of(flight?"freccia":"impatto",flight?"freccia_0":"impatto_0",flight?"dardo_root":"impatto_root",flight?"dardo":"flash_impatto").contains(node.name)){center.set(node.origin);break;}
        world.translate(-center.x/16,-center.y/16,-center.z/16);
        Matrix4f[] matrices=new Matrix4f[m.nodes.size()];boolean[] enabled=new boolean[m.nodes.size()];
        var layers=new ArrayList<ArrayList<MeshGeometry.Vertex>>();for(var ignored:m.textures)layers.add(new ArrayList<>());
        int budget=distant?1800:7000,used=0;
        for(int i=0;i<m.nodes.size();i++){
            var n=m.nodes.get(i);var tracks=clip.channels.getOrDefault(i,Map.of());
            var position=sample(tracks.get("position"),time,0);var rotation=sample(tracks.get("rotation"),time,0).add(n.rotation);var scale=sample(tracks.get("scale"),time,1);
            boolean visible=n.parent<0||enabled[n.parent];
            if(projectile&&n.parent<0){if(flight||hit)visible=selectedRoot(n.name,flight,number);}
            if(flight&&n.parent<0){position.set(0,0,0);scale.set(1,1,1);}
            if(number==9&&phase.equals("loop")&&n.parent<0)visible=n.name.contains("marchio");
            enabled[i]=visible;
            var origin=new Vector3f(n.origin);if(n.parent>=0)origin.sub(m.nodes.get(n.parent).origin);
            var matrix=new Matrix4f(n.parent>=0?matrices[n.parent]:world).translate(origin.add(position).div(16)).rotateXYZ((float)Math.toRadians(rotation.x),(float)Math.toRadians(rotation.y),(float)Math.toRadians(rotation.z)).scale(scale);
            matrices[i]=matrix;if(!visible||Math.abs(matrix.determinant())<1e-8)continue;
            for(var q:n.quads){if(used++>=budget)break;var texture=m.textures.get(q.texture);int frame=(int)(age/texture.ticks)%texture.frames;var vertices=layers.get(q.texture);
                for(int k=0;k<20;k+=5){var v=q.vertices;var pos=matrix.transformPosition(new Vector3f(v[k],v[k+1],v[k+2]));
                    // Two-ended authored bonds are stretched between the actual participants.
                    if((number==5||number==15)&&e.from().distanceSquared(e.at())>.01){var raw=new Vector3f(pos);new Matrix4f(world).invert().transformPosition(raw);float t=(raw.x*16+16)/48;float dx=(float)(e.at().x()-e.from().x()),dz=(float)(e.at().z()-e.from().z());float len=(float)Math.max(.001,Math.hypot(dx,dz));pos.set((t-1)*dx+(raw.z-.5f)*dz/len,raw.y+(float)((1-t)*(e.from().y()-1.62-e.at().y())),(t-1)*dz-(raw.z-.5f)*dx/len);}
                    vertices.add(new MeshGeometry.Vertex(pos.x,pos.y,pos.z,v[k+3],(v[k+4]+frame)/texture.frames,0xFFFFFFFF));}
            }
        }
        var result=new ArrayList<MeshGeometry.Layer>();for(int i=0;i<layers.size();i++)if(!layers.get(i).isEmpty())result.add(new MeshGeometry.Layer(m.textures.get(i).path,layers.get(i)));return List.copyOf(result);
    }
}
