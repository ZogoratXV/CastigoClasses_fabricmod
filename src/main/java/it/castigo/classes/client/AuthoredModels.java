package it.castigo.classes.client;

import com.google.gson.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static it.castigo.classes.client.MeshGeometry.*;

/** Original articulated VFX models. Parts have authored start/end transforms, not gameplay entities. */
public final class AuthoredModels {
    private record Part(float[] position,float[] destination,float[] size,float[] rotation,float[] turn,double delay,double scale,int shade) {}
    private static final Map<String,List<Part>> MODELS=load();
    private static Map<String,List<Part>> load() {
        var result=new HashMap<String,List<Part>>();
        try(var in=AuthoredModels.class.getResourceAsStream("/assets/castigoclasses/models/vfx/choreography.json")) {
            if(in==null)return Map.of();
            var root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var entry:root.entrySet()) {
                var parts=new ArrayList<Part>();
                for(var node:entry.getValue().getAsJsonArray()) {
                    var o=node.getAsJsonObject();parts.add(new Part(vector(o,"position"),vector(o,"destination"),vector(o,"size"),vector(o,"rotation"),vector(o,"turn"),o.get("delay").getAsDouble(),o.get("scale").getAsDouble(),o.get("shade").getAsInt()));
                    if(parts.size()>32)throw new IllegalArgumentException("Too many model parts");
                }
                result.put(entry.getKey(),List.copyOf(parts));
            }
        } catch(Exception ex){throw new IllegalStateException("Invalid bundled VFX model",ex);}
        return Map.copyOf(result);
    }
    private static float[] vector(JsonObject o,String key) {
        var a=o.getAsJsonArray(key);if(a.size()!=3)throw new IllegalArgumentException(key);var out=new float[3];
        for(int i=0;i<3;i++){out[i]=a.get(i).getAsFloat();if(!Float.isFinite(out[i])||Math.abs(out[i])>720)throw new IllegalArgumentException(key);}
        return out;
    }
    public static List<Layer> build(EffectMessage e,double age,double alpha,int rgb,boolean distant) {
        var parts=MODELS.get(e.shape().name());if(parts==null||alpha<=0)return List.of();
        double life=e.handle()==null?age/e.durationTicks():(age%60)/60;
        var vertices=new ArrayList<Vertex>();float yaw=(float)Math.atan2(e.direction().x(),e.direction().z());
        int index=0;
        for(var part:parts) {
            if(distant&&index++%2==1)continue;
            double t=Math.clamp((life-part.delay)/(1-part.delay),0,1),ease=1-Math.pow(1-t,3);
            double scale=(.08+(part.scale-.08)*Math.min(1,t*5));
            // Ring/beam decorations stay small enough to keep the character readable.
            var m=new Matrix4f().rotateY(yaw).translate(
                (float)((part.position[0]+(part.destination[0]-part.position[0])*ease)*e.radius()),
                (float)((part.position[1]+(part.destination[1]-part.position[1])*ease)*Math.min(e.mesh().height(),4)),
                (float)((part.position[2]+(part.destination[2]-part.position[2])*ease)*e.radius()));
            m.rotateXYZ((float)Math.toRadians(part.rotation[0]+part.turn[0]*ease),(float)Math.toRadians(part.rotation[1]+part.turn[1]*ease),(float)Math.toRadians(part.rotation[2]+part.turn[2]*ease));
            m.scale((float)(part.size[0]*Math.min(e.radius(),3)*scale),(float)(part.size[1]*Math.min(e.mesh().height(),4)*scale),(float)(part.size[2]*Math.min(e.radius(),3)*scale));
            int color=blend(rgb,part.shade);
            // Six bevel-like diamond faces assembled as quads; original crystal/feather silhouette.
            Vector3f[] v={new Vector3f(0,-.5f,0),new Vector3f(-.5f,0,-.25f),new Vector3f(.5f,0,-.25f),new Vector3f(.5f,0,.25f),new Vector3f(-.5f,0,.25f),new Vector3f(0,.5f,0)};
            for(var p:v)m.transformPosition(p);
            int face=0;
            for(int[] q:new int[][]{{0,2,1,1},{0,3,2,2},{0,4,3,3},{0,1,4,4},{5,1,2,2},{5,2,3,3},{5,3,4,4},{5,4,1,1}}) {
                int shaded=face++%2==0?color:blend(color,-35);int argb=((int)(Math.clamp(alpha,0,1)*255)<<24)|shaded;
                float[][] uv={{.5f,0},{0,.5f},{1,1},{1,1}};
                for(int i=0;i<4;i++){var p=v[q[i]];vertices.add(new Vertex(p.x,p.y,p.z,uv[i][0],uv[i][1],argb));}
            }
        }
        String texture=switch(e.shape()) {case HEALING_BEAM -> "seraph_feather";case MESH_SHIELD -> "aegis_crest";default -> "faceted_energy";};
        return List.of(new Layer("castigoclasses:textures/vfx/"+texture+".png",vertices));
    }
    private static int blend(int rgb,int amount){int result=0;for(int s:new int[]{0,8,16}){int c=(rgb>>s)&255;c=amount>=0?c+(255-c)*amount/100:c*(100+amount)/100;result|=Math.clamp(c,0,255)<<s;}return result;}
}
