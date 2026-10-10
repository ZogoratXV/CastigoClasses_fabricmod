package it.castigo.classes.client;
/** Original Castigo key poses, authored independently; degrees and model-relative offsets. */
public final class CombatChoreography {
    public enum Profile { VANILLA,SWORD,TWO_HANDED,AXE,THRUST,SHIELD,BOW,CAST }
    public record Rotation(double pitch,double yaw,double roll){
        Rotation scale(double w){return new Rotation(pitch*w,yaw*w,roll*w);}
        static Rotation mix(Rotation a,Rotation b,double t){return new Rotation(a.pitch+(b.pitch-a.pitch)*t,a.yaw+(b.yaw-a.yaw)*t,a.roll+(b.roll-a.roll)*t);}
    }
    public record Frame(Rotation main,Rotation support,Rotation body,double x,double y,double z,double pitch,double yaw,double roll){
        public Frame scale(double w){return new Frame(main.scale(w),support.scale(w),body.scale(w),x*w,y*w,z*w,pitch*w,yaw*w,roll*w);}
        public static Frame mix(Frame a,Frame b,double t){t=Math.clamp(t,0,1);return new Frame(Rotation.mix(a.main,b.main,t),Rotation.mix(a.support,b.support,t),Rotation.mix(a.body,b.body,t),lerp(a.x,b.x,t),lerp(a.y,b.y,t),lerp(a.z,b.z,t),lerp(a.pitch,b.pitch,t),lerp(a.yaw,b.yaw,t),lerp(a.roll,b.roll,t));}
    }
    public static final Rotation ZERO=new Rotation(0,0,0);
    public static final Frame REST=new Frame(ZERO,ZERO,ZERO,0,0,0,0,0,0);
    private static double lerp(double a,double b,double t){return a+(b-a)*t;}
    private static double smooth(double t){t=Math.clamp(t,0,1);return t*t*(3-2*t);}
    private static Frame pose(double mp,double my,double mr,double sp,double sy,double sr,double bp,double by,double br,double x,double y,double z,double pitch,double yaw,double roll){return new Frame(new Rotation(mp,my,mr),new Rotation(sp,sy,sr),new Rotation(bp,by,br),x,y,z,pitch,yaw,roll);}
    private static Frame windup(Profile p,int variant){double side=(variant&1)==0?1:-1;return switch(p){
        case VANILLA -> REST;
        case SWORD -> pose(-100,35*side,42*side,-20,-12,-8,-4,-24*side,3*side,.12*side,-.06,.08,-28,20*side,22*side);
        case TWO_HANDED -> pose(-125,25*side,28*side,-110,-35*side,-20*side,-8,-28*side,4*side,-.12,-.06,.12,-40,15*side,16*side);
        case AXE -> pose(-155,12,18,-65,-25,-18,-12,-16,4,.08,-.04,.10,-50,12,16);
        case THRUST -> pose(-65,-32,14,-30,15,-10,-5,-18,0,.02,-.08,.20,-14,-18,8);
        case SHIELD -> pose(-80,22,-18,-25,-12,8,-4,12,-3,-.18,-.08,.08,-12,18,-12);
        case BOW -> pose(-85,-20,4,-100,55,-8,0,-12,0,.12,-.20,-.12,-4,5,-3);
        case CAST -> pose(-110,-20,15,-90,28,-18,-4,-10,0,-.04,-.08,-.04,-25,-8,10);
    };}
    private static Frame contact(Profile p,int variant){double side=(variant&1)==0?1:-1;return switch(p){
        case VANILLA -> REST;
        case SWORD -> pose(-72,-32*side,-32*side,-18,12,8,7,22*side,-4*side,-.15*side,-.12,-.20,18,-28*side,-24*side);
        case TWO_HANDED -> pose(-75,-35*side,-20*side,-80,30*side,15*side,10,28*side,-5*side,-.12,-.14,-.26,24,-24*side,-20*side);
        case AXE -> pose(-42,-12,-16,-45,20,15,14,18,-3,.04,-.24,-.25,32,-16,-14);
        case THRUST -> pose(-90,-5,0,-35,15,-10,8,10,0,-.02,-.12,-.42,0,2,0);
        case SHIELD -> pose(-95,12,-12,-35,-12,8,8,-12,2,-.20,-.10,-.26,8,-10,-8);
        case BOW -> pose(-88,-18,4,-82,68,-12,2,-10,0,.14,-.22,-.16,0,7,-4);
        case CAST -> pose(-85,8,10,-80,-8,-10,4,8,0,-.04,-.10,-.28,10,8,6);
    };}
    public static Frame sample(Profile profile,boolean preparing,double progress,int variant){
        if(!Double.isFinite(progress)||profile==Profile.VANILLA)return REST;
        if(preparing&&progress<=0)return REST;
        if(preparing)return windup(profile,variant).scale(smooth(progress*5));
        if(progress>=1||progress<0)return REST;
        Frame impact=contact(profile,variant);
        // The strike begins at contact: vanilla damage is not delayed to wait for a visual wind-up.
        if(progress<.22)return Frame.mix(impact,impact.scale(1.12),smooth(progress/.22));
        if(progress<.55)return Frame.mix(impact.scale(1.12),impact.scale(.32),smooth((progress-.22)/.33));
        return Frame.mix(impact.scale(.32),REST,smooth((progress-.55)/.45));
    }
    public static Profile parse(String id){try{return Profile.valueOf(id);}catch(RuntimeException e){return Profile.VANILLA;}}
}
