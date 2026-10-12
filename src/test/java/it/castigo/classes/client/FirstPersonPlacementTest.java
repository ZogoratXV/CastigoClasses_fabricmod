package it.castigo.classes.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FirstPersonPlacementTest {
    @Test void twoHandedRigStaysBelowAndInFrontOfCameraEvenWithExtremeCalibration(){
        for(double value:new double[]{-2,0,2}){
            var packet=new com.google.gson.JsonObject();var c=new com.google.gson.JsonObject();packet.add("calibration",c);
            for(String key:new String[]{"firstX","firstY","firstZ"})c.addProperty(key,value);
            var right=FirstPersonPlacement.of(CombatChoreography.Profile.TWO_HANDED,false,new WeaponCalibration(packet));
            var left=FirstPersonPlacement.of(CombatChoreography.Profile.TWO_HANDED,true,new WeaponCalibration(packet));
            assertTrue(right.y()>0);assertTrue(right.z()>0);assertEquals(-right.x(),left.x());
        }
    }
    @Test void placementDoesNotFadeBackThroughCameraAtEndOfClip()throws Exception{
        try(var in=getClass().getResourceAsStream("/assets/castigoclasses/player_animations/cast_curse.json")){
            var clip=com.zigythebird.playeranimcore.loading.UniversalAnimLoader.loadAnimations(in).values().iterator().next();
            var controller=new com.zigythebird.playeranimcore.animation.HumanoidAnimationController((c,s,set)->com.zigythebird.playeranimcore.enums.PlayState.STOP,com.zigythebird.playeranimcore.molang.MolangLoader::createNewEngine);
            var position=FirstPersonPlacement.of(CombatChoreography.Profile.TWO_HANDED,false,new WeaponCalibration(null));
            var offset=new com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier(bone->bone.equals("body")?java.util.Optional.of(new com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier.PartModifier(com.zigythebird.playeranimcore.math.Vec3f.ZERO,new com.zigythebird.playeranimcore.math.Vec3f(0,(float)(position.y()*16),(float)(position.z()*16)))):java.util.Optional.empty());
            offset.fadeIn=false;offset.fadeOut=false;controller.addModifierBefore(offset);controller.triggerAnimation(clip,clip.length()-2);
            controller.setupAnim(new com.zigythebird.playeranimcore.animation.AnimationData(0,0,true));
            var bone=new com.zigythebird.playeranimcore.bones.PlayerAnimBone("body");controller.get3DTransform(bone);
            assertTrue(bone.position.y>3);assertTrue(bone.position.z>4);
        }
    }
}
