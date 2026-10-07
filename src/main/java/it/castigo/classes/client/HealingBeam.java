package it.castigo.classes.client;

/** Deterministic bounded geometry: two rings, a luminous core and ascending sparks. */
public final class HealingBeam {
    public static final int SAMPLES=72;
    public record Point(double x,double y,double z,int color,float size) {}
    private HealingBeam() {}
    public static Point sample(int age,int duration,int index,double radius,int color,float scale) {
        if(index<0||index>=SAMPLES)throw new IllegalArgumentException("Invalid beam sample");
        double life=Math.max(0,Math.min(1,age/(double)Math.max(1,duration-1)));
        double envelope=Math.min(1,(age+1)/4.0)*Math.min(1,(duration-age)/8.0);
        double r=Math.min(1.6,radius)*(0.78+0.22*Math.sin(Math.PI*life));
        if(index<48) {
            int ring=index/24;double angle=2*Math.PI*(index%24)/24+(ring==0?1:-1)*age*0.09;
            return new Point(Math.cos(angle)*r,0.12+ring*0.38,Math.sin(angle)*r,color,(float)(scale*0.75*envelope));
        }
        if(index<64) {
            double y=(index-48)/15.0*3.4;double angle=age*0.3+index*2.4;
            return new Point(Math.cos(angle)*0.06,y,Math.sin(angle)*0.06,mixWhite(color,0.6), (float)(scale*1.5*envelope*(1-y/5)));
        }
        double f=((age*0.04+(index-64)/8.0)%1);double angle=index*2.4+age*0.16;
        return new Point(Math.cos(angle)*r*0.65,0.2+f*2.4,Math.sin(angle)*r*0.65,mixWhite(color,0.3),(float)(scale*0.5*envelope));
    }
    private static int mixWhite(int color,double fraction) {
        int r=color>>16&255,g=color>>8&255,b=color&255;
        return ((int)(r+(255-r)*fraction)<<16)|((int)(g+(255-g)*fraction)<<8)|(int)(b+(255-b)*fraction);
    }
}
