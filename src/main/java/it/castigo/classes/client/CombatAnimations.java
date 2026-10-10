package it.castigo.classes.client;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.*;
/** Cosmetic-only v2 transport with local prediction and per-entity sequence checks. */
public final class CombatAnimations {
    private record Motion(UUID player,ItemStack item,boolean off,boolean normal,boolean preparing,boolean left,CombatChoreography.Profile profile,int variant,long start,long duration,long prediction,WeaponCalibration calibration,CombatChoreography.Frame origin){
        CombatChoreography.Frame frame(){long elapsed=System.nanoTime()-start;var target=CombatChoreography.sample(profile,preparing,elapsed/(double)duration,variant);double t=Math.clamp(elapsed/60_000_000d,0,1);return CombatChoreography.Frame.mix(origin,target,t*t*(3-2*t));}
    }
    private static final Map<Integer,Motion> motions=new HashMap<>();
    private record Transition(UUID player,ItemStack item,CombatChoreography.Frame frame,long at){}
    private static final Map<Integer,Transition> transitions=new HashMap<>();
    private static final Map<UUID,Long> sequences=new HashMap<>();
    private static Object level;private static String world="";
    private static boolean supported;private static String profile="VANILLA";private static ItemStack contextItem=ItemStack.EMPTY;
    private static WeaponCalibration calibration=new WeaponCalibration(null);
    private static long prediction,lastAttack;private static int variant,lastSlot=-1;private static ItemStack lastWeapon=ItemStack.EMPTY;
    static boolean sameWeapon(ItemStack a,ItemStack b){
        if(ItemStack.matches(a,b))return true;
        if(a.getItem()!=b.getItem()||a.getCount()!=b.getCount())return false;
        var first=a.copy();var second=b.copy();first.remove(net.minecraft.core.component.DataComponents.DAMAGE);second.remove(net.minecraft.core.component.DataComponents.DAMAGE);
        return ItemStack.matches(first,second);
    }
    public static void clear(){motions.clear();transitions.clear();sequences.clear();supported=false;profile="VANILLA";contextItem=ItemStack.EMPTY;lastWeapon=ItemStack.EMPTY;calibration=new WeaponCalibration(null);lastAttack=0;variant=0;lastSlot=-1;level=null;world="";}
    public static void tick(Minecraft mc){
        if(level!=mc.level||!world.equals(CastigoClient.STATE.world)||!CastigoClient.STATE.active()){clear();level=mc.level;world=CastigoClient.STATE.world;}
        if(mc.level==null)return;
        transitions.values().removeIf(t->System.nanoTime()-t.at>150_000_000L);
        motions.entrySet().removeIf(e->!valid(e.getKey(),e.getValue(),mc));
        if(sequences.size()>256)sequences.clear();
    }
    private static boolean valid(int id,Motion m,Minecraft mc){
        if(System.nanoTime()-m.start>=m.duration||mc.level==null)return false;
        var e=mc.level.getEntity(id);return e instanceof Player p&&p.getUUID().equals(m.player)&&p.isAlive()&&!p.isSpectator()&&!p.isUsingItem()&&sameWeapon(p.getItemInHand(m.off?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND),m.item);
    }
    public static void context(JsonObject state,Minecraft mc){
        tick(mc);supported=state.has("weaponAnimations")&&state.get("weaponAnimations").getAsInt()==2;
        if(!supported||mc.player==null)return;
        // A stale state packet for another hotbar slot must not assign a custom pose to the current item.
        if(!state.has("weaponSlot")||state.get("weaponSlot").getAsInt()!=mc.player.getInventory().getSelectedSlot()){contextItem=ItemStack.EMPTY;return;}
        contextItem=mc.player.getMainHandItem().copy();profile=state.get("weaponProfile").getAsString();var wrapper=new JsonObject();wrapper.add("calibration",state.get("weaponCalibration"));calibration=new WeaponCalibration(wrapper);
    }
    public static void attack(Minecraft mc){
        tick(mc);if(!supported||mc.player==null||mc.gui.screen()!=null||mc.player.isSpectator()||!mc.player.isAlive()||mc.player.isUsingItem()||mc.gameMode==null||mc.gameMode.isDestroying()||mc.hitResult==null||mc.hitResult.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK)return;
        var active=motions.get(mc.player.getId());if(active!=null&&!active.normal)return;
        if(CastigoClient.STATE.castingTotal>0)return;
        // Wait for authoritative per-item context rather than guessing an ItemsAdder identity.
        if(!sameWeapon(contextItem,mc.player.getMainHandItem()))return;
        var kind=CombatChoreography.parse(profile);if(kind==CombatChoreography.Profile.VANILLA)return;
        long now=System.nanoTime();if(now-lastAttack<100_000_000L)return;
        variant=now-lastAttack>1_000_000_000L||lastSlot!=mc.player.getInventory().getSelectedSlot()||!sameWeapon(lastWeapon,contextItem)?0:1-variant;
        lastAttack=now;lastSlot=mc.player.getInventory().getSelectedSlot();lastWeapon=contextItem.copy();long id=++prediction;
        long duration=(long)(Math.clamp(mc.player.getCurrentItemAttackStrengthDelay(),5,20)*50_000_000L);
        motions.put(mc.player.getId(),new Motion(mc.player.getUUID(),contextItem.copy(),false,true,false,mc.player.getMainArm()==HumanoidArm.LEFT,kind,variant,now,duration,id,calibration,active==null?CombatChoreography.REST:active.frame()));
        JsonObject request=new JsonObject();request.addProperty("id",id);CastigoClient.request("attack_visual",request);
    }
    public static void receive(JsonObject o,Minecraft mc){
        tick(mc);if(mc.level==null||mc.player==null||!world.equals(o.get("world").getAsString()))return;
        int id=o.get("entity").getAsInt();UUID uuid=UUID.fromString(o.get("player").getAsString());var e=mc.level.getEntity(id);
        if(!(e instanceof Player p)||!p.getUUID().equals(uuid)||p.distanceToSqr(mc.player)>4096)return;
        long sequence=o.get("sequence").getAsLong();if(sequence<=sequences.getOrDefault(uuid,-1L))return;sequences.put(uuid,sequence);
        String phase=o.get("phase").getAsString();if(phase.equals("stop")){var previous=motions.remove(id);if(previous!=null)transitions.put(id,new Transition(uuid,previous.item,previous.frame(),System.nanoTime()));return;}
        if(!phase.equals("release")&&!phase.equals("prepare"))return;
        int ticks=o.get("duration").getAsInt();if(ticks<1||ticks>600||motions.size()>=128&&!motions.containsKey(id))return;
        boolean normal=o.get("normal").getAsBoolean(),off=o.get("offhand").getAsBoolean();var existing=motions.get(id);
        if(normal&&existing!=null&&!existing.normal)return;
        var kind=CombatChoreography.parse(o.get("profile").getAsString());if(kind==CombatChoreography.Profile.VANILLA){motions.remove(id);return;}
        long request=normal?o.get("prediction").getAsLong():-1;
        // Keep the predicted start time on acknowledgment, preventing a second swing.
        if(normal&&p==mc.player&&(request!=prediction||lastAttack==0||System.nanoTime()-lastAttack>=ticks*50_000_000L||!sameWeapon(p.getMainHandItem(),lastWeapon)))return;
        long start=normal&&p==mc.player?lastAttack:System.nanoTime();
        var transition=transitions.remove(id);var origin=existing!=null?existing.frame():transition!=null&&transition.player.equals(uuid)&&sameWeapon(transition.item,p.getItemInHand(off?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND))?transition.frame:CombatChoreography.REST;
        if(normal&&p==mc.player&&existing!=null&&existing.prediction==request)origin=existing.origin;
        motions.put(id,new Motion(uuid,p.getItemInHand(off?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND).copy(),off,normal,phase.equals("prepare"),(p.getMainArm()==HumanoidArm.LEFT)^off,kind,o.get("variant").getAsInt()&1,start,ticks*50_000_000L,request,new WeaponCalibration(o),origin));
    }
    public static boolean twoHanded(int entity){
        var m=motions.get(entity);var mc=Minecraft.getInstance();if(m==null||m.profile!=CombatChoreography.Profile.TWO_HANDED||!active(entity,InteractionHand.MAIN_HAND))return false;
        return mc.level.getEntity(entity) instanceof Player p&&p.getOffhandItem().isEmpty();
    }
    public static void supportHand(int entity,PoseStack stack,double sign){var m=motions.get(entity);if(m!=null)stack.translate(m.calibration.get("supportX")*sign,m.calibration.get("supportY"),m.calibration.get("supportZ"));}
    public static boolean active(int entity,InteractionHand hand){var mc=Minecraft.getInstance();var m=motions.get(entity);return m!=null&&m.off==(hand==InteractionHand.OFF_HAND)&&valid(entity,m,mc);}
    public static boolean firstPerson(int entity,InteractionHand hand,PoseStack stack){
        if(!active(entity,hand))return false;var m=motions.get(entity);var f=m.frame().scale(m.calibration.get("firstIntensity"));double sign=m.left?-1:1;var c=m.calibration;
        stack.translate((f.x()+c.get("firstX"))*sign,f.y()+c.get("firstY"),f.z()+c.get("firstZ"));
        stack.mulPose(Axis.XP.rotationDegrees((float)(f.pitch()+c.get("firstPitch"))));stack.mulPose(Axis.YP.rotationDegrees((float)((f.yaw()+c.get("firstYaw"))*sign)));stack.mulPose(Axis.ZP.rotationDegrees((float)((f.roll()+c.get("firstRoll"))*sign)));return true;
    }
    private static void rotate(net.minecraft.client.model.geom.ModelPart part,CombatChoreography.Rotation r,double sign){part.xRot+=(float)Math.toRadians(r.pitch());part.yRot+=(float)Math.toRadians(r.yaw()*sign);part.zRot+=(float)Math.toRadians(r.roll()*sign);}
    public static boolean thirdPerson(HumanoidRenderState state,HumanoidModel<?> model){
        if(!(state instanceof AvatarRenderState avatar)||avatar.isSpectator||state.isUsingItem)return false;
        var m=motions.get(avatar.id);if(m==null||!valid(avatar.id,m,Minecraft.getInstance()))return false;
        var f=m.frame().scale(m.calibration.get("thirdIntensity"));double sign=m.left?-1:1;
        var main=m.left?model.leftArm:model.rightArm;var support=m.left?model.rightArm:model.leftArm;
        // Remove the vanilla attack swing for this frame; retain walking on the lower body.
        double progress=(System.nanoTime()-m.start)/(double)m.duration;
        float restore=m.preparing?1-(float)Math.clamp(progress*5,0,1):(float)Math.clamp((progress-.7)/.3,0,1);
        main.xRot*=restore;main.yRot*=restore;main.zRot*=restore;support.xRot*=restore;support.yRot*=restore;support.zRot*=restore;model.body.yRot*=restore;
        rotate(main,f.main(),sign);rotate(support,f.support(),sign);rotate(model.body,f.body(),sign);
        model.head.yRot-=(float)Math.toRadians(f.body().yaw()*sign*.25);
        if(WeaponAnimations.gripping(avatar.id)||twoHanded(avatar.id)){
            support.xRot=main.xRot-.10f;support.yRot=main.yRot+(float)(sign*.55);support.zRot=main.zRot-(float)(sign*.20);
            var c=m.calibration;rotate(main,new CombatChoreography.Rotation(c.get("mainPitch"),c.get("mainYaw"),c.get("mainRoll")),sign);rotate(support,new CombatChoreography.Rotation(c.get("supportPitch"),c.get("supportYaw"),c.get("supportRoll")),sign);
        }
        return true;
    }
}
