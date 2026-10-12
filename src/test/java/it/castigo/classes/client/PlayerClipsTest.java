package it.castigo.classes.client;
import com.zigythebird.playeranimcore.animation.*;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PlayerClipsTest {
    @Test void runtimeEvaluatesRealKeyframesAndStopsWithoutLeavingAPose()throws Exception{
        try(var in=getClass().getResourceAsStream("/assets/castigoclasses/player_animations/cast_curse.json")){
            var clip=UniversalAnimLoader.loadAnimations(in).values().iterator().next();
            var controller=new HumanoidAnimationController((c,s,set)->com.zigythebird.playeranimcore.enums.PlayState.STOP,com.zigythebird.playeranimcore.molang.MolangLoader::createNewEngine);
            controller.triggerAnimation(clip,8);
            var samples=new java.util.HashSet<org.joml.Vector3f>();
            for(int tick=0;tick<10;tick++){
                controller.tick(new AnimationData(0,0,false));controller.setupAnim(new AnimationData(0,.5f,false));
                var bone=new com.zigythebird.playeranimcore.bones.PlayerAnimBone("right_arm");controller.get3DTransform(bone);
                assertTrue(bone.rotation.isFinite());assertTrue(bone.position.isFinite());samples.add(new org.joml.Vector3f(bone.rotation));
            }
            assertTrue(samples.size()>5,"The runtime must interpolate the clip, not hold one static pose");
            controller.stop();assertFalse(controller.isActive());
        }
    }
    @Test void allAssignedClipsLoadWithTheActualRuntimeAndAnimateBothArms()throws Exception{
        for(var profile:CombatChoreography.Profile.values())for(int variant=0;variant<2;variant++){
            if(profile==CombatChoreography.Profile.VANILLA)continue;
            String id=PlayerClips.clip(profile,variant);
            try(var in=getClass().getResourceAsStream("/assets/"+id.replace(":","/player_animations/")+".json")){
                // Public source checkout excludes the user's private animation assets.
                if(in==null&&id.startsWith("castigoclasses_bc:"))continue;
                assertNotNull(in,id);var clip=UniversalAnimLoader.loadAnimations(in).values().iterator().next();
                assertTrue(clip.length()>8,id);
                assertNotNull(clip.getBone("right_arm"),id);assertNotNull(clip.getBone("left_arm"),id);
                assertTrue(clip.getBone("right_arm").rotationKeyFrames().xKeyframes().size()>2,id);
                float begin=((Number)clip.data().getRaw(ExtraAnimationData.BEGIN_TICK_KEY)).floatValue();
                assertTrue(begin>0&&begin<clip.length(),id);
            }
        }
    }
    @Test void ordinaryAxesDoNotUseTwoHandedClipsAndComboDirectionsDiffer(){
        assertTrue(PlayerClips.clip(CombatChoreography.Profile.AXE,0).contains("one_handed"));
        assertTrue(PlayerClips.clip(CombatChoreography.Profile.HEAVY_TWO_HANDED,0).contains("two_handed"));
        assertNotEquals(PlayerClips.clip(CombatChoreography.Profile.SWORD,0),PlayerClips.clip(CombatChoreography.Profile.SWORD,1));
    }
}
