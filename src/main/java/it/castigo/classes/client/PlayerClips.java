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
    private static final Identifier GRIP=Identifier.fromNamespaceAndPath("castigoclasses","grip");
    private record Idle(Controller controller,net.minecraft.world.item.ItemStack item,String calibration,boolean left){}
    private static final Map<Integer,Idle> idle=new HashMap<>();
    private static final Set<String> missing=new HashSet<>();
    private PlayerClips() {}

    public static void register(){PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER,2100,Controller::new);PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(GRIP,2000,Controller::new);}
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
    public static boolean firstPersonRig(int id){
        var c=playing.get(id);if(c==null){var g=idle.get(id);c=g==null?null:g.controller;}
        return c!=null&&c.getFirstPersonMode()==FirstPersonMode.THIRD_PERSON_MODEL;
    }
    public static boolean rigActive(int id){return playing.containsKey(id)||idle.containsKey(id);}
    public static void pruneGrips(java.util.function.IntPredicate valid){idle.entrySet().removeIf(e->{if(valid.test(e.getKey()))return false;e.getValue().controller.stop();return true;});}
    public static void grip(Player player,WeaponCalibration calibration){
        if(active(player.getId())){stopGrip(player.getId());return;}
        boolean left=player.getMainArm()==net.minecraft.world.entity.HumanoidArm.LEFT;
        var old=idle.get(player.getId());
        if(old!=null&&old.controller.getAvatar()==player&&old.left==left&&old.calibration.equals(calibration.signature())&&CombatAnimations.sameWeapon(old.item,player.getMainHandItem()))return;
        var layer=PlayerAnimationAccess.getPlayerAnimationLayer(player,GRIP);
        var animation=PlayerAnimResources.getAnimation(Identifier.parse("castigoclasses_bc:pose_two_handed_sword"));
        if(!(layer instanceof Controller controller)||animation==null)return;
        controller.stop();controller.removeAllModifiers();var mirror=new MirrorModifier();mirror.enabled=left;controller.addModifierLast(mirror);
        configure(controller,player,CombatChoreography.Profile.TWO_HANDED,left,calibration);
        controller.triggerAnimation(animation);
        idle.put(player.getId(),new Idle(controller,player.getMainHandItem().copy(),calibration.signature(),left));
    }
    private static void stopGrip(int id){var old=idle.remove(id);if(old!=null)old.controller.stop();}
    public static void stop(int id){var controller=playing.remove(id);if(controller!=null)controller.stop();}
    public static void clear(){for(var c:playing.values())c.stop();playing.clear();for(var i:idle.values())i.controller.stop();idle.clear();}
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
        stopGrip(player.getId());
        controller.stop();controller.removeAllModifiers();
        var mirror=new MirrorModifier();mirror.enabled=left;controller.addModifierLast(mirror);
        Object marker=animation.data().getRaw(ExtraAnimationData.BEGIN_TICK_KEY);
        float contact=marker instanceof Number n?Math.clamp(n.floatValue(),0,animation.length()-1):0;
        // Purpur already applied the hit: release starts at the authored strike, not at wind-up.
        float start=preparing?0:contact;
        controller.addModifierLast(new SpeedModifier((preparing?contact:animation.length()-contact)/Math.max(1,ticks)));
        configure(controller,player,profile,left,calibration);
        controller.triggerAnimation(animation,start);
        playing.put(player.getId(),controller);
    }
    static boolean twoHandedProfile(CombatChoreography.Profile profile){return switch(profile){case TWO_HANDED,HEAVY_TWO_HANDED,THRUST_TWO_HANDED,SPIN,LOW_SWEEP->true;default->false;};}
    private static void configure(Controller controller,Player player,CombatChoreography.Profile profile,boolean left,WeaponCalibration calibration){
        // Full-body two-handed clips remain in third person. Native item rendering avoids camera clipping.
        controller.setFirstPersonMode(twoHandedProfile(profile)?FirstPersonMode.DISABLED:FirstPersonMode.THIRD_PERSON_MODEL);
        var adjustment=new AdjustmentModifier((bone,data)->{
            double pitch=0,yaw=0,roll=0,x=0,y=0,z=0;
            if(data.isFirstPersonPass()){
                if(!bone.equals("body"))return Optional.empty();
                // PAL already follows camera pitch; applying it again rotates the sword into the camera.
                var view=FirstPersonPlacement.of(profile,left,calibration);
                pitch=view.pitch();yaw=view.yaw();roll=view.roll();
                x=view.x();y=view.y();z=view.z();
            }else if(bone.equals(left?"left_arm":"right_arm")){
                pitch=calibration.get("mainPitch");yaw=calibration.get("mainYaw");roll=calibration.get("mainRoll");
            }else if(bone.equals(left?"right_arm":"left_arm")){
                pitch=calibration.get("supportPitch");yaw=calibration.get("supportYaw");roll=calibration.get("supportRoll");
                x=calibration.get("supportX");y=calibration.get("supportY");z=calibration.get("supportZ");
            }else return Optional.empty();
            return Optional.of(new AdjustmentModifier.PartModifier(new Vec3f((float)Math.toRadians(pitch),(float)Math.toRadians(yaw),(float)Math.toRadians(roll)),new Vec3f((float)(x*16),(float)(y*16),(float)(z*16))));
        });
        adjustment.fadeIn=false;adjustment.fadeOut=false;
        controller.addModifierBefore(adjustment);
        controller.addModifierBefore(new AbstractModifier(){
            @Override public void get3DTransform(com.zigythebird.playeranimcore.bones.PlayerAnimBone bone){
                var before=new com.zigythebird.playeranimcore.bones.PlayerAnimBone(bone);
                super.get3DTransform(bone);
                float weight=(float)calibration.get(controller.getAnimationData().isFirstPersonPass()?"firstIntensity":"thirdIntensity");
                bone.position.lerp(before.position,1-weight);bone.rotation.lerp(before.rotation,1-weight);bone.scale.lerp(before.scale,1-weight);
            }
        });
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
