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
                P up=new P(0,1,0);
                double reach=r*(.25+.75*(1-Math.pow(1-life,3)));
                P start=new P(0,h*.6,0).add(axis.mul(.3)),end=start.add(axis.mul(reach));
                for(P normal:List.of(side,new P(0,1,0))) {
                    P offset=normal.mul(.1);quad(out,start.add(offset),end.add(offset),end.add(offset.mul(-1)),start.add(offset.mul(-1)),rgb,alpha);
                }
                var core=new Layer(m.columnTexture(),out);out=new ArrayList<>();
                // Two corkscrew wakes frame the spearhead without hiding the weapon.
                for(int arm=0;arm<2;arm++)for(int i=0;i<n;i++) {
                    double t=i/(double)n,t1=(i+1)/(double)n;
                    P a=thrustWake(start,axis,side,up,reach,t,life,arm),b=thrustWake(start,axis,side,up,reach,t1,life,arm);
                    P w=side.mul(.035+.025*Math.sin(t*Math.PI));
                    strip(out,a.add(w),b.add(w),b.add(w.mul(-1)),a.add(w.mul(-1)),(float)t,(float)t1,rgb,alpha*.8);
                }
                var wake=new Layer(m.columnTexture(),out);out=new ArrayList<>();
                P base=end.add(axis.mul(-Math.min(.55,reach*.35)));
                double width=Math.min(.22,r*.1);
                P[] corners={base.add(side.mul(width)),base.add(up.mul(width)),base.add(side.mul(-width)),base.add(up.mul(-width))};
                for(int i=0;i<4;i++)quad(out,corners[i],corners[(i+1)%4],end,end,i%2==0?rgb:lighten(rgb),alpha);
                for(int i=0;i<(distant?5:12);i++) {
                    double t=(i+.5)/12,phi=i*2.399+life*5;
                    P p=start.add(axis.mul(reach*t)).add(side.mul(Math.cos(phi)*.28*life)).add(up.mul(Math.sin(phi)*.28*life));
                    fragment(out,p,.018+.024*t,rgb,alpha*(.3+.7*t));
                }
                return List.of(core,wake,new Layer("castigoclasses:textures/vfx/shard.png",out));
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
                double sign=m.rotation()<0?-1:1,sweep=Math.PI*1.25;
                double head=sign*(-.35+1.5*(1-Math.pow(1-life,3)));
                // Three staggered crescents: broad colored blade, bright edge and trailing echo.
                for(int band=0;band<3;band++)for(int i=0;i<n;i++) {
                    double t=i/(double)n,a=head-sign*t*sweep-band*sign*.12,b=a-sign*sweep/n;
                    double outer=r*(1-band*.095),inner=outer-r*(band==0?.32:.065)*(1-t*.8);
                    double fade=alpha*Math.pow(1-t,1.35)*(band==2?.4:1);
                    P a0=blade(a,inner,h,yaw,sign),b0=blade(b,inner,h,yaw,sign),b1=blade(b,outer,h,yaw,sign),a1=blade(a,outer,h,yaw,sign);
                    strip(out,a0,b0,b1,a1,(float)t,(float)(t+1.0/n),band==1?lighten(rgb):rgb,fade);
                    // A second face gives the cutting edge thickness from low viewing angles.
                    if(band==0)strip(out,a1,b1,b1.add(new P(0,.055,0)),a1.add(new P(0,.055,0)),(float)t,(float)(t+1.0/n),rgb,fade*.7);
                }
                var blades=new Layer(m.columnTexture(),out);out=new ArrayList<>();
                int sparks=distant?8:24;
                for(int i=0;i<sparks;i++) {
                    double seed=i*2.399,a=head-sign*(i%8)*.16;
                    double travel=life*(.15+(i%5)*.12);
                    P p=blade(a,r*(.78+travel),h,yaw,sign).add(new P(0,Math.sin(seed)*life*.45-life*life*.35,0));
                    fragment(out,p,.022+.012*(i%3),i%3==0?lighten(rgb):rgb,alpha*(.45+.4*Math.sin(seed)*Math.sin(seed)));
                }
                return List.of(blades,new Layer("castigoclasses:textures/vfx/shard.png",out));
            }
            case MESH_SHIELD -> {
                // Curved frontal wall, oriented with the protected character.
                for(int j=0;j<4;j++)for(int i=0;i<n;i++) {
                    double a=yaw-Math.PI/3+i*Math.PI*2/3/n,b=yaw-Math.PI/3+(i+1)*Math.PI*2/3/n;
                    double y0=j*h/4,y1=(j+1)*h/4;
                    // Map the grid across the whole barrier, rather than repeating it per tiny quad.
                    int c=((int)(alpha*180)<<24)|(rgb&0xffffff);
                    P[] p={arc(a,r,y0),arc(b,r,y0),arc(b,r,y1),arc(a,r,y1)};
                    float[][] uv={{i/(float)n,j/4f},{(i+1)/(float)n,j/4f},{(i+1)/(float)n,(j+1)/4f},{i/(float)n,(j+1)/4f}};
                    for(int k=0;k<4;k++)out.add(new Vertex((float)p[k].x,(float)p[k].y,(float)p[k].z,uv[k][0],uv[k][1],c));
                    if(j==0||j==3) {
                        double y=j==0?.025:h-.025;
                        quad(out,arc(a,r,y-.025),arc(b,r,y-.025),arc(b,r,y+.025),arc(a,r,y+.025),lighten(rgb),alpha);
                    }
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
    private static int lighten(int rgb) {
        int r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
        return ((r+(255-r)/3)<<16)|((g+(255-g)/3)<<8)|(b+(255-b)/3);
    }
    private static P blade(double a,double r,double h,double yaw,double sign) {
        // Height controls the silhouette: low sweep, diagonal cut, or near-vertical heavy cleave.
        double tilt=h<.6?0:Math.toRadians(h>2.5?70:25);
        double x=Math.sin(a)*r*Math.cos(tilt),z=Math.cos(a)*r;
        double y=h*.5+Math.sin(a)*Math.min(h*.46,r*Math.sin(tilt))*sign;
        return new P(x*Math.cos(yaw)+z*Math.sin(yaw),y,z*Math.cos(yaw)-x*Math.sin(yaw));
    }
    private static P thrustWake(P origin,P axis,P side,P up,double reach,double t,double life,int arm) {
        double phi=t*Math.PI*3-life*7+arm*Math.PI,width=.22*Math.sin(t*Math.PI);
        return origin.add(axis.mul(reach*t)).add(side.mul(Math.cos(phi)*width)).add(up.mul(Math.sin(phi)*width));
    }
    private static void fragment(List<Vertex> out,P p,double s,int rgb,double alpha) {
        P a=p.add(new P(-s,-s,-s)),b=p.add(new P(s,-s,-s)),c=p.add(new P(s,s,-s)),d=p.add(new P(-s,s,-s));
        P z=new P(0,0,s*2),aa=a.add(z),bb=b.add(z),cc=c.add(z),dd=d.add(z);
        quad(out,a,b,c,d,rgb,alpha);quad(out,aa,dd,cc,bb,rgb,alpha);
        quad(out,a,aa,bb,b,rgb,alpha);quad(out,d,c,cc,dd,lighten(rgb),alpha);
        quad(out,a,d,dd,aa,rgb,alpha);quad(out,b,bb,cc,c,lighten(rgb),alpha);
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
