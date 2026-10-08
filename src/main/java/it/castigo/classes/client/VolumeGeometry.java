package it.castigo.classes.client;

import java.util.*;
import static it.castigo.classes.client.MeshGeometry.*;

/** Bounded, deterministic mesh particles. Age is interpolated; no world mutation or per-frame RNG. */
public final class VolumeGeometry {
    private static final String ROOT="castigoclasses:textures/vfx/";
    private record P(double x,double y,double z) {
        P add(P b){return new P(x+b.x,y+b.y,z+b.z);} P mul(double s){return new P(x*s,y*s,z*s);}
        P cross(P b){return new P(y*b.z-z*b.y,z*b.x-x*b.z,x*b.y-y*b.x);}
        double length(){return Math.sqrt(x*x+y*y+z*z);} P unit(){double n=length();return n<1e-6?new P(0,0,1):mul(1/n);}
    }
    private VolumeGeometry() {}
    public static List<Layer> burst(EffectMessage e,double t,double alpha,int rgb,boolean far) {
        var cloud=new ArrayList<Vertex>();var core=new ArrayList<Vertex>();var chips=new ArrayList<Vertex>();var shock=new ArrayList<Vertex>();var flash=new ArrayList<Vertex>();
        double r=e.radius(), expansion=1-Math.pow(1-t,3), y=Math.min(e.mesh().height()*.45,1.2);
        int count=far?5:9;
        // Separate expanding lobes: bright inner mass, saturated rim, then drifting embers.
        for(int i=0;i<count;i++) {
            double a=i*2.399963, pitch=.25+noise(i+9)*.85;
            P p=new P(Math.cos(a)*r*expansion*.65,y+Math.sin(pitch)*r*expansion*.5,Math.sin(a)*r*expansion*.65);
            double size=r*(.22+.38*expansion)*(.7+noise(i)*.45);
            puff(cloud,p,size,angle(i,t),mix(rgb,0x281B40,t*.3),Math.min(1,alpha*1.3)*(1-t*.45));
            if(t<.62)puff(core,p,size*.65,angle(i,-t),mix(rgb,0xFFFFE8,.85-t*.5),alpha*Math.max(0,1-t/.62));
        }
        for(int i=0;i<(far?8:22);i++) {
            double a=i*2.399963,speed=.8+noise(i+20)*1.25;
            P p=new P(Math.cos(a)*r*t*speed,y+r*(t*(.5+noise(i+40)*1.8)-t*t*.9),Math.sin(a)*r*t*speed);
            cube(chips,p,Math.min(.13,r*.065)*(.6+noise(i))*(1-t*.7),mix(rgb,0xFFFFFF,noise(i+2)*.8),alpha);
        }
        ground(shock,r*(.2+expansion*1.15),.045,t*.6,rgb,alpha*(1-t));
        if(t<.4)puff(flash,new P(0,y,0),r*(.8+t),0,0xFFFFED,alpha*(1-t/.4));
        return List.of(new Layer(ROOT+"cloud.png",cloud),new Layer(ROOT+"cloud.png",core),
                new Layer(ROOT+"shard.png",chips),new Layer(ROOT+"wave.png",shock),new Layer(e.mesh().ringTexture(),flash))
                .stream().filter(l->!l.vertices().isEmpty()).toList();
    }
    public static List<Layer> beam(EffectMessage e,double t,double age,double alpha,int rgb,boolean far) {
        var body=new ArrayList<Vertex>();var light=new ArrayList<Vertex>();var mist=new ArrayList<Vertex>();var chips=new ArrayList<Vertex>();
        P start=new P(e.from().x()-e.at().x(),e.from().y()-e.at().y(),e.from().z()-e.at().z());
        if(start.length()<.01){var d=e.direction();start=new P(d.x(),d.y(),d.z()).unit().mul(-Math.max(.5,e.mesh().height()));}
        P delta=start.mul(-1),axis=delta.unit(),side=axis.cross(Math.abs(axis.y)>.95?new P(1,0,0):new P(0,1,0)).unit(),up=axis.cross(side).unit();
        double width=Math.min(1.5,Math.max(.035,e.radius()*e.mesh().columnRadius()*2.3));int n=far?10:22;
        for(int arm=0;arm<3;arm++)for(int i=0;i<n;i++) {
            double u=i/(double)n,v=(i+1)/(double)n;
            P a=stream(start,delta,side,up,u,arm,age,width),b=stream(start,delta,side,up,v,arm,age,width);
            double w=width*(.25+.55*Math.sin(Math.PI*(u+v)*.5));
            strip(body,a,b,arm%2==0?side:up,w,rgb,alpha*.65);
            if(arm==0)strip(light,start.add(delta.mul(u)),start.add(delta.mul(v)),side,w*.25,mix(rgb,0xFFFFFF,.8),alpha);
        }
        int count=far?5:12;
        for(int i=0;i<count;i++) {
            double u=fract(i/(double)count+age*.035),a=i*2.399963+age*.09;
            P p=start.add(delta.mul(u)).add(side.mul(Math.cos(a)*width)).add(up.mul(Math.sin(a)*width));
            puff(mist,p,width*(.8+noise(i)),a,rgb,alpha*.42);
            cube(chips,p.add(up.mul(width)),Math.min(.075,width*.22),mix(rgb,0xFFFFFF,.65),alpha);
        }
        return List.of(new Layer(e.mesh().columnTexture(),body),new Layer(ROOT+"ribbon.png",light),
                new Layer(ROOT+"cloud.png",mist),new Layer(ROOT+"shard.png",chips));
    }
    private static P stream(P start,P delta,P side,P up,double u,int arm,double age,double width) {
        double a=u*Math.PI*5+arm*Math.PI*2/3-age*.18,spread=width*Math.sin(Math.PI*u);
        return start.add(delta.mul(u)).add(side.mul(Math.cos(a)*spread)).add(up.mul(Math.sin(a)*spread));
    }
    public static List<Layer> accents(EffectMessage e,double t,double age,double alpha,int rgb,boolean far) {
        var motes=new ArrayList<Vertex>();var veils=new ArrayList<Vertex>();
        double r=e.radius(),h=e.mesh().height();int count=far?6:18;
        boolean wave=e.shape()==EffectMessage.Shape.MESH_WAVE;
        boolean heal=e.shape()==EffectMessage.Shape.HEALING_BEAM||e.shape()==EffectMessage.Shape.MESH_COLUMN;
        boolean vortex=e.shape()==EffectMessage.Shape.MESH_VORTEX;
        boolean ring=e.shape()==EffectMessage.Shape.MESH_RING;
        // Directional weapon arcs and shield panels retain their own silhouettes.
        if(!wave&&!heal&&!vortex&&!ring&&e.shape()!=EffectMessage.Shape.MESH_SIGIL)return List.of();
        for(int i=0;i<count;i++) {
            double phase=fract(i*.618033+age*(heal?.021:.012)),a=i*2.399963+age*.035;
            double rr=r*(wave?(.12+.88*t):vortex?(1-phase*.75):(.6+noise(i)*.32));
            double y=wave?.08+Math.sin(t*Math.PI)*(.1+noise(i)*.45):.06+phase*(heal||vortex?h:Math.min(h,1.1));
            P p=new P(Math.cos(a)*rr,y,Math.sin(a)*rr);
            double fade=Math.sin(Math.PI*phase);
            cube(motes,p,Math.min(.09,r*.035)*(.5+noise(i+5)),mix(rgb,0xFFFFFF,.4+noise(i)*.5),alpha*fade);
            if(heal&&i%3==0)puff(veils,p,Math.min(.65,r*.3),a,rgb,alpha*.28*fade);
        }
        return List.of(new Layer(ROOT+"shard.png",motes),new Layer(ROOT+"cloud.png",veils));
    }
    private static double fract(double x){return x-Math.floor(x);}
    private static double noise(int i){return fract(Math.sin(i*127.1+311.7)*43758.5453);}
    private static double angle(int i,double t){return i*1.7+t*1.3;}
    private static int mix(int a,int b,double t){t=Math.clamp(t,0,1);int c=0;for(int s:new int[]{0,8,16})c|=((int)Math.round(((a>>s)&255)*(1-t)+((b>>s)&255)*t))<<s;return c;}
    private static void strip(List<Vertex> out,P a,P b,P normal,double w,int rgb,double alpha){P o=normal.mul(w);quad(out,a.add(o),b.add(o),b.add(o.mul(-1)),a.add(o.mul(-1)),rgb,alpha);}
    private static void puff(List<Vertex> out,P p,double r,double angle,int rgb,double alpha) {
        P side=new P(Math.cos(angle)*r,0,Math.sin(angle)*r),up=new P(0,r,0);
        quad(out,p.add(side.mul(-1)).add(up.mul(-1)),p.add(side).add(up.mul(-1)),p.add(side).add(up),p.add(side.mul(-1)).add(up),rgb,alpha);
        side=new P(-side.z,0,side.x);
        quad(out,p.add(side.mul(-1)).add(up.mul(-1)),p.add(side).add(up.mul(-1)),p.add(side).add(up),p.add(side.mul(-1)).add(up),rgb,alpha);
    }
    private static void ground(List<Vertex> out,double r,double y,double angle,int rgb,double alpha) {
        quad(out,new P(-r,y,-r),new P(-r,y,r),new P(r,y,r),new P(r,y,-r),rgb,alpha);
    }
    private static void cube(List<Vertex> out,P p,double s,int rgb,double alpha) {
        P[] v={p.add(new P(-s,-s,-s)),p.add(new P(s,-s,-s)),p.add(new P(s,s,-s)),p.add(new P(-s,s,-s)),p.add(new P(-s,-s,s)),p.add(new P(s,-s,s)),p.add(new P(s,s,s)),p.add(new P(-s,s,s))};
        for(int[] f:new int[][]{{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}})quad(out,v[f[0]],v[f[1]],v[f[2]],v[f[3]],rgb,alpha);
    }
    private static void quad(List<Vertex> out,P a,P b,P c,P d,int rgb,double alpha) {
        int color=((int)Math.round(Math.clamp(alpha,0,1)*255)<<24)|(rgb&0xFFFFFF);P[] p={a,b,c,d};float[][] uv={{0,0},{1,0},{1,1},{0,1}};
        for(int i=0;i<4;i++)out.add(new Vertex((float)p[i].x,(float)p[i].y,(float)p[i].z,uv[i][0],uv[i][1],color));
    }
}
