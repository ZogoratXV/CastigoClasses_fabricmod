package it.castigo.classes.client;

import java.util.*;
import static it.castigo.classes.client.MeshGeometry.*;

/** Textured surfaces for weapon arcs, barriers, ribbons, sigils and spatial impacts. */
public final class SpellGeometry {
    private record P(double x,double y,double z) {
        P add(P p){return new P(x+p.x,y+p.y,z+p.z);} P mul(double s){return new P(x*s,y*s,z*s);}
        P cross(P p){return new P(y*p.z-z*p.y,z*p.x-x*p.z,x*p.y-y*p.x);}
        double length(){return Math.sqrt(x*x+y*y+z*z);} P unit(){double n=length();return n<1e-6?new P(0,0,1):mul(1/n);}
    }
    private SpellGeometry() {}
    public static List<Layer> build(EffectMessage e,double life,double angle,double alpha,int rgb,boolean distant) {
        var out=new ArrayList<Vertex>();var m=e.mesh();double r=e.radius(),h=m.height();
        var d=e.direction();double yaw=Math.atan2(d.x(),d.z());int n=distant?16:32;
        String texture=m.ringTexture();
        switch(e.shape()) {
            case MESH_THRUST -> {
                P axis=new P(Math.sin(yaw),0,Math.cos(yaw)),side=new P(Math.cos(yaw),0,-Math.sin(yaw));
                P start=new P(0,h*.6,0).add(axis.mul(.3)),end=start.add(axis.mul(r*(.3+.7*life)));
                for(P normal:List.of(side,new P(0,1,0))) {
                    P offset=normal.mul(.1);quad(out,start.add(offset),end.add(offset),end.add(offset.mul(-1)),start.add(offset.mul(-1)),rgb,alpha);
                }
                texture=m.columnTexture();
            }
            case MESH_BEAM -> {
                P start=new P(e.from().x()-e.at().x(),e.from().y()-e.at().y(),e.from().z()-e.at().z()),end=new P(0,0,0);
                if(start.length()<.01)start=new P(d.x(),d.y(),d.z()).unit().mul(-Math.max(.5,h));
                P axis=start.mul(-1).unit(),side=axis.cross(Math.abs(axis.y)>.95?new P(1,0,0):new P(0,1,0)).unit();
                double width=Math.min(1.2,r)*m.columnRadius()*(.55+.45*Math.sin(Math.PI*life));
                for(P normal:List.of(side,axis.cross(side).unit())) {
                    P offset=normal.mul(width);
                    quad(out,start.add(offset),end.add(offset),end.add(offset.mul(-1)),start.add(offset.mul(-1)),rgb,alpha);
                }
                texture=m.columnTexture();
            }
            case MESH_SLASH -> {
                // A curved blade ribbon sweeps through the attacker's facing direction.
                double sweep=Math.PI*1.25,head=yaw+life*sweep-sweep*.5;
                for(int i=0;i<n;i++) {
                    double a=head-i*sweep/n,b=head-(i+1)*sweep/n,fade=alpha*(1-i/(double)n);
                    strip(out,arc(a,r*.55,h*.35),arc(b,r*.55,h*.35),arc(b,r,h*.55),arc(a,r,h*.55),i/(float)n,(i+1)/(float)n,rgb,fade);
                    strip(out,arc(a,r*.83,h*.50+.008),arc(b,r*.83,h*.50+.008),arc(b,r*.92,h*.53+.008),arc(a,r*.92,h*.53+.008),i/(float)n,(i+1)/(float)n,0xFFF1CF,fade*.85);
                }
                texture=m.columnTexture();
            }
            case MESH_SHIELD -> {
                // Curved frontal wall, oriented with the protected character.
                for(int j=0;j<4;j++)for(int i=0;i<n;i++) {
                    double a=yaw-Math.PI/3+i*Math.PI*2/3/n,b=yaw-Math.PI/3+(i+1)*Math.PI*2/3/n;
                    double y0=j*h/4,y1=(j+1)*h/4;
                    quad(out,arc(a,r,y0),arc(b,r,y0),arc(b,r,y1),arc(a,r,y1),rgb,alpha*(.6+.4*Math.sin(Math.PI*(j+.5)/4)));
                }
                texture=m.columnTexture();
            }
            case MESH_BURST -> {
                double size=r*(.25+life*.9),y=h*.5;
                // Three crossed flare planes remain visible from every viewing angle.
                quad(out,new P(-size,y-size,0),new P(size,y-size,0),new P(size,y+size,0),new P(-size,y+size,0),rgb,alpha);
                quad(out,new P(0,y-size,-size),new P(0,y-size,size),new P(0,y+size,size),new P(0,y+size,-size),rgb,alpha);
                plane(out,size,y,angle,rgb,alpha*.7);
            }
            case MESH_SIGIL -> {
                double size=r*(.9+.1*Math.sin(life*Math.PI));
                // A vertical rotating seal over the victim plus a ground mark.
                var vertical=new ArrayList<Vertex>();plane(vertical,size,0,angle,rgb,alpha);
                for(var v:vertical)out.add(new Vertex((float)(v.x()*Math.cos(yaw)),(float)(h+v.z()),(float)(-v.x()*Math.sin(yaw)),v.u(),v.v(),v.color()));
                plane(out,size*.7,.035,-angle,rgb,alpha*.65);
            }
            case MESH_WAVE -> {
                double size=r*(.12+.88*life);
                plane(out,size,.04,angle,rgb,alpha);
                if(m.rings()>1)plane(out,size*.7,.07,-angle,rgb,alpha*.6);
            }
            case MESH_VORTEX -> {
                plane(out,r,.035,-angle,rgb,alpha);
                var ground=new Layer(m.ringTexture(),out);out=new ArrayList<>();
                for(int arm=0;arm<3;arm++)for(int i=0;i<n;i++) {
                    double t=i/(double)n,t1=(i+1)/(double)n,a=t*Math.PI*4+angle+arm*Math.PI*2/3,b=t1*Math.PI*4+angle+arm*Math.PI*2/3;
                    double rr=r*(1-t*.85),rr1=r*(1-t1*.85),width=r*.09;
                    strip(out,arc(a,rr-width,.1+t*h),arc(b,rr1-width,.1+t1*h),arc(b,rr1+width,.1+t1*h),arc(a,rr+width,.1+t*h),(float)t,(float)t1,rgb,alpha*(1-t*.6));
                }
                return List.of(ground,new Layer(m.columnTexture(),out));
            }
            default -> { return List.of(); }
        }
        return List.of(new Layer(texture,out));
    }
    private static P arc(double angle,double r,double y){return new P(Math.sin(angle)*r,y,Math.cos(angle)*r);}
    private static void plane(List<Vertex> out,double r,double y,double a,int rgb,double alpha) {
        P[] p={new P(-r,y,-r),new P(-r,y,r),new P(r,y,r),new P(r,y,-r)};
        for(int i=0;i<4;i++){P v=p[i];p[i]=new P(v.x*Math.cos(a)-v.z*Math.sin(a),v.y,v.x*Math.sin(a)+v.z*Math.cos(a));}
        quad(out,p[0],p[1],p[2],p[3],rgb,alpha);
    }
    private static void quad(List<Vertex> out,P a,P b,P c,P d,int rgb,double alpha) {
        strip(out,a,b,c,d,0,1,rgb,alpha);
    }
    private static void strip(List<Vertex> out,P a,P b,P c,P d,float u0,float u1,int rgb,double alpha) {
        int color=((int)Math.round(Math.clamp(alpha,0,1)*255)<<24)|(rgb&0xFFFFFF);
        P[] p={a,b,c,d};float[][] uv={{u0,0},{u1,0},{u1,1},{u0,1}};
        for(int i=0;i<4;i++)out.add(new Vertex((float)p[i].x,(float)p[i].y,(float)p[i].z,uv[i][0],uv[i][1],color));
    }
}
