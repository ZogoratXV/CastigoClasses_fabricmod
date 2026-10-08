package it.castigo.classes.client;

import java.util.*;

/** Pure local-space geometry; no entities, graphics driver or mutable game state. */
public final class MeshGeometry {
    public record Vertex(float x,float y,float z,float u,float v,int color) {}
    public record Layer(String texture,List<Vertex> vertices) { public Layer { vertices=List.copyOf(vertices); } }
    private MeshGeometry() {}
    public static List<Layer> build(EffectMessage effect,double age,boolean distant) {
        var result=new ArrayList<>(base(effect,age,distant));
        if(effect.hasMesh()) {
            var m=effect.mesh();double life=effect.handle()==null?age/effect.durationTicks():.5;
            int rgb=m.tint().equals("auto")?effect.particles().color():Integer.parseInt(m.tint(),16);
            result.addAll(AuthoredModels.build(effect,age,m.alpha(life),rgb,distant));
        }
        return List.copyOf(result);
    }
    private static List<Layer> base(EffectMessage effect,double age,boolean distant) {
        if(!effect.hasMesh())return List.of();
        var m=effect.mesh();double life=effect.handle()==null?age/effect.durationTicks():.5,alpha=m.alpha(life);
        if(alpha<=0)return List.of();
        var layers=new ArrayList<Layer>();
        double seconds=age/20,angle=Math.toRadians(m.rotation()*seconds),radius=effect.radius()*(.88+.12*Math.min(1,life/.2));
        int rgb=m.tint().equals("auto")?effect.particles().color():Integer.parseInt(m.tint(),16);
        if(effect.shape()==EffectMessage.Shape.MESH_BURST)return VolumeGeometry.burst(effect,life,alpha,rgb,distant);
        if(effect.shape()==EffectMessage.Shape.MESH_BEAM)return VolumeGeometry.beam(effect,life,age,alpha,rgb,distant);
        if(effect.shape()!=EffectMessage.Shape.HEALING_BEAM&&effect.shape()!=EffectMessage.Shape.MESH_RING&&effect.shape()!=EffectMessage.Shape.MESH_COLUMN) {
            layers.addAll(SpellGeometry.build(effect,life,angle,alpha,rgb,distant));
            layers.addAll(VolumeGeometry.accents(effect,life,age,alpha,rgb,distant));
            return layers.stream().filter(l->!l.vertices().isEmpty()).toList();
        }
        if(effect.shape()!=EffectMessage.Shape.MESH_COLUMN&&m.rings()>0) {
            var vertices=new ArrayList<Vertex>();
            for(int i=0;i<m.rings();i++) {
                double r=radius*(1-i*.09),y=.035+i*m.ringGap(),a=angle*(i%2==0?1:-1);
                int color=color(rgb,Math.min(1,alpha*1.3));
                ringVertex(vertices,-r,y,-r,0,0,a,color);ringVertex(vertices,-r,y,r,0,1,a,color);
                ringVertex(vertices,r,y,r,1,1,a,color);ringVertex(vertices,r,y,-r,1,0,a,color);
            }
            layers.add(new Layer(m.ringTexture(),vertices));
        }
        if(effect.shape()!=EffectMessage.Shape.MESH_RING) {
            var vertices=new ArrayList<Vertex>();int segments=distant?12:24,bands=distant?4:8;
            // Split exactly at texture-wrap boundaries: UVs stay [0,1] even with clamp samplers.
            double shift=seconds*m.scroll();shift-=Math.floor(shift);
            var cuts=new TreeSet<Double>();for(int j=0;j<=bands;j++)cuts.add(j/(double)bands);
            if(shift>0&&shift<1)cuts.add(shift);
            var ys=new ArrayList<>(cuts);
            for(int j=0;j<ys.size()-1;j++) {
                double y0=ys.get(j),y1=ys.get(j+1),offset=Math.floor((y0+y1)*.5-shift);
                double v0=y0-shift-offset,v1=y1-shift-offset;
                for(int i=0;i<segments;i++) {
                    double t0=i/(double)segments,t1=(i+1)/(double)segments;
                    columnVertex(vertices,t0,y0,v0,angle,radius,m,rgb,alpha);
                    columnVertex(vertices,t1,y0,v0,angle,radius,m,rgb,alpha);
                    columnVertex(vertices,t1,y1,v1,angle,radius,m,rgb,alpha);
                    columnVertex(vertices,t0,y1,v1,angle,radius,m,rgb,alpha);
                }
            }
            layers.add(new Layer(m.columnTexture(),vertices));
        }
        layers.addAll(VolumeGeometry.accents(effect,life,age,alpha,rgb,distant));
        return layers.stream().filter(l->!l.vertices().isEmpty()).toList();
    }
    private static void ringVertex(List<Vertex> out,double x,double y,double z,float u,float v,double a,int color) {
        out.add(new Vertex((float)(x*Math.cos(a)-z*Math.sin(a)),(float)y,(float)(x*Math.sin(a)+z*Math.cos(a)),u,v,color));
    }
    private static void columnVertex(List<Vertex> out,double t,double y,double v,double angle,double radius,MeshSettings m,int rgb,double alpha) {
        double a=t*Math.PI*2+angle*.25,r=radius*m.columnRadius()*(1+.1*y);
        // Height fade is independent of moving texture, so the top never becomes a hard edge.
        double fade=Math.pow(1-y,1.4)*Math.min(1,y*24+.35);
        out.add(new Vertex((float)(Math.cos(a)*r),(float)(.04+y*m.height()),(float)(Math.sin(a)*r),(float)t,(float)v,color(rgb,alpha*fade)));
    }
    private static int color(int rgb,double alpha) { return ((int)Math.round(Math.clamp(alpha,0,1)*255)<<24)|(rgb&0xFFFFFF); }
}
