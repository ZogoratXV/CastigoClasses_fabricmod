package it.castigo.classes.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WeaponPoseTest {
    @Test void allGesturesRemainFiniteAndReturnExactlyToNeutral() {
        for(var style:WeaponPose.Style.values()) {
            for(int i=0;i<=100;i++)for(boolean prep:new boolean[]{true,false}) {
                var p=WeaponPose.sample(style,prep,i/100.0);
                assertTrue(Double.isFinite(p.pitch()+p.yaw()+p.roll()+p.x()+p.y()+p.z()));
                assertTrue(Math.abs(p.pitch())<=100&&Math.abs(p.x())<1&&Math.abs(p.z())<1);
            }
            assertEquals(new WeaponPose.Pose(0,0,0,0,0,0,0),normalize(WeaponPose.sample(style,false,1)));
            assertEquals(1,WeaponPose.sample(style,true,1).weight());
        }
    }
    private WeaponPose.Pose normalize(WeaponPose.Pose p){return new WeaponPose.Pose(p.x()+0.,p.y()+0.,p.z()+0.,p.pitch()+0.,p.yaw()+0.,p.roll()+0.,p.weight()+0.);}
    @Test void differentWeaponFamiliesHaveDifferentChoreography() {
        var poses=new java.util.HashSet<WeaponPose.Pose>();
        for(var style:WeaponPose.Style.values())poses.add(WeaponPose.sample(style,false,.25));
        assertEquals(6,poses.size());
    }
}
