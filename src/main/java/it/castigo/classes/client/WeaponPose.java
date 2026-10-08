package it.castigo.classes.client;

/** Normalized choreography shared by both camera views. Rotations are degrees. */
public final class WeaponPose {
    public enum Style { CAST, BOW, SHIELD, THRUST, HEAVY, SLASH }
    public record Pose(double x,double y,double z,double pitch,double yaw,double roll,double weight) {}
    private WeaponPose() {}
    public static Pose sample(Style style,boolean preparing,double progress) {
        if(!preparing&&progress>=1)return new Pose(0,0,0,0,0,0,0);
        double t=Math.clamp(progress,0,1),w=preparing?Math.min(1,t*5):Math.pow(1-t,1.4);
        double hit=preparing?0:Math.sin(Math.min(1,t*2.5)*Math.PI);
        return switch(style) {
            case CAST -> new Pose(-.12*w,.22*w,-.16*w-hit*.18,-38*w+hit*24,12*w,-16*w,w);
            case BOW -> new Pose(-.23*w,.1*w,-.2*w+hit*.12,-8*w,-28*w,-12*w,w);
            case SHIELD -> new Pose(-.3*w,.2*w,-.12*w-hit*.25,-15*w,35*w,-8*w,w);
            case THRUST -> new Pose(-.1*w,.08*w,.14*w-hit*.65,-12*w+hit*10,-8*w,8*w,w);
            case HEAVY -> new Pose(.12*w,.38*w-hit*.35,.08*w,-75*w+hit*90,18*w,30*w-hit*55,w);
            case SLASH -> new Pose(.2*w-hit*.5,.13*w,.06*w-hit*.15,-25*w+hit*35,40*w-hit*85,30*w-hit*65,w);
        };
    }
}
