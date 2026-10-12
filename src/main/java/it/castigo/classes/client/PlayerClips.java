package it.castigo.classes.client;

import com.zigythebird.playeranim.api.*;
import com.zigythebird.playeranim.animation.*;
import com.zigythebird.playeranimcore.animation.ExtraAnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.*;
import com.zigythebird.playeranimcore.api.firstPerson.*;
import com.zigythebird.playeranimcore.enums.PlayState;
import com.zigythebird.playeranimcore.math.Vec3f;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import java.util.*;

/** Client-only PAL layer. Uses animation assets, never Better Combat's attack protocol. */
public final class PlayerClips {
    private static final Identifier LAYER=Identifier.fromNamespaceAndPath("castigoclasses","combat");
    private static final Map<Integer,Controller> playing=new HashMap<>();
    private static final Set<String> missing=new HashSet<>();
    private PlayerClips() {}

    public static void register(){PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER,2100,Controller::new);}
    public static String clip(CombatChoreography.Profile profile,int variant){
        String side=(variant&1)==0?"right":"left";
        return switch(profile){
            case SWORD -> "castigoclasses_bc:one_handed_slash_horizontal_"+side;
            case TWO_HANDED -> "castigoclasses_bc:two_handed_slash_horizontal_"+side;
            case AXE -> "castigoclasses_bc:one_handed_slam";
            case HEAVY_TWO_HANDED -> "castigoclasses_bc:two_handed_slam";
            case THRUST_TWO_HANDED -> "castigoclasses_bc:two_handed_stab_"+side;
            case THRUST -> "castigoclasses_bc:one_handed_stab";
            case SPIN -> "castigoclasses_bc:two_handed_spin";
            case LOW_SWEEP -> "castigoclasses_bc:two_handed_slash_vertical_"+side;
            case VANILLA -> "";
            default -> "castigoclasses:cast_"+profile.name().toLowerCase(Locale.ROOT);
        };
    }
    public static boolean active(int id){return playing.containsKey(id);}
    public static void stop(int id){var controller=playing.remove(id);if(controller!=null)controller.stop();}
    public static void clear(){for(var c:playing.values())c.stop();playing.clear();}
    public static void play(Player player,CombatChoreography.Profile profile,int variant,boolean preparing,boolean left,int ticks,WeaponCalibration calibration){
        var layer=PlayerAnimationAccess.getPlayerAnimationLayer(player,LAYER);
        if(!(layer instanceof Controller controller))return;
        String name=clip(profile,variant);if(name.isEmpty())return;
        var animation=PlayerAnimResources.getAnimation(Identifier.parse(name));
        if(animation==null){
            stop(player.getId());
            if(missing.add(name))org.slf4j.LoggerFactory.getLogger("CastigoClasses").warn("Animation {} missing; run the local animation importer for Better Combat clips",name);
            return;
        }
        controller.stop();controller.removeAllModifiers();
        var mirror=new MirrorModifier();mirror.enabled=left;controller.addModifierLast(mirror);
        Object marker=animation.data().getRaw(ExtraAnimationData.BEGIN_TICK_KEY);
        float contact=marker instanceof Number n?Math.clamp(n.floatValue(),0,animation.length()-1):0;
        // Purpur already applied the hit: release starts at the authored strike, not at wind-up.
        float start=preparing?0:contact;
        controller.addModifierLast(new SpeedModifier((preparing?contact:animation.length()-contact)/Math.max(1,ticks)));
        controller.addModifierBefore(new AdjustmentModifier((bone,data)->{
            double pitch=0,yaw=0,roll=0,x=0,y=0,z=0;
            if(data.isFirstPersonPass()){
                if(!bone.equals("body"))return Optional.empty();
                pitch=player.getXRot()+calibration.get("firstPitch");yaw=calibration.get("firstYaw");roll=calibration.get("firstRoll");
                x=calibration.get("firstX");y=calibration.get("firstY");z=calibration.get("firstZ");
                // The bow stays below the crosshair; the real two-arm rig is still used.
                if(profile==CombatChoreography.Profile.BOW){y-=.22;z-=.15;}
            }else if(bone.equals(left?"left_arm":"right_arm")){
                pitch=calibration.get("mainPitch");yaw=calibration.get("mainYaw");roll=calibration.get("mainRoll");
            }else if(bone.equals(left?"right_arm":"left_arm")){
                pitch=calibration.get("supportPitch");yaw=calibration.get("supportYaw");roll=calibration.get("supportRoll");
                x=calibration.get("supportX");y=calibration.get("supportY");z=calibration.get("supportZ");
            }else return Optional.empty();
            return Optional.of(new AdjustmentModifier.PartModifier(new Vec3f((float)Math.toRadians(pitch),(float)Math.toRadians(yaw),(float)Math.toRadians(roll)),new Vec3f((float)x,(float)y,(float)z)));
        }));
        controller.addModifierBefore(new AbstractModifier(){
            @Override public void get3DTransform(com.zigythebird.playeranimcore.bones.PlayerAnimBone bone){
                var before=new com.zigythebird.playeranimcore.bones.PlayerAnimBone(bone);
                super.get3DTransform(bone);
                float weight=(float)calibration.get(controller.getAnimationData().isFirstPersonPass()?"firstIntensity":"thirdIntensity");
                bone.position.lerp(before.position,1-weight);bone.rotation.lerp(before.rotation,1-weight);bone.scale.lerp(before.scale,1-weight);
            }
        });
        controller.triggerAnimation(animation,start);
        playing.put(player.getId(),controller);
    }
    private static final class Controller extends PlayerAnimationController {
        Controller(Avatar avatar){
            super(avatar,(controller,state,setter)->PlayState.STOP);
            setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
            setFirstPersonConfiguration(new FirstPersonConfiguration(true,true,true,true));
            setFirstPersonTransitionLength(0);
            setFirstPersonFollowsCamera(true);
            setPostAnimationSetupConsumer(bones->{
                bones.apply("torso").setEnabled(true);
                bones.apply("head").rotXEnabled=false;
                if(avatar.isPassenger()||avatar.isSwimming()||avatar.getDeltaMovement().horizontalDistanceSqr()>.0025){
                    bones.apply("right_leg").setEnabled(false);bones.apply("left_leg").setEnabled(false);
                }
            });
        }
    }
}
