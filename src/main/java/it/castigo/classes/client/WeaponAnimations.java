package it.castigo.classes.client;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Server-confirmed, short-lived visual poses. Never calls useItem, attack or sends input. */
public final class WeaponAnimations {
    private record Motion(UUID uuid,boolean offhand,boolean left,WeaponPose.Style style,boolean prepare,long start,long duration,ItemStack item) {
        WeaponPose.Pose pose(){return WeaponPose.sample(style,prepare,(System.nanoTime()-start)/(double)duration);}
    }
    private static final Map<Integer,Motion> motions=new ConcurrentHashMap<>();
    private record Grip(UUID uuid,ItemStack item,long expires){}
    private static final Map<Integer,Grip> grips=new ConcurrentHashMap<>();
    public static boolean gripping(int entity){
        var g=grips.get(entity);var mc=Minecraft.getInstance();if(g==null||mc.level==null||System.nanoTime()>g.expires)return false;
        var e=mc.level.getEntity(entity);return e instanceof net.minecraft.world.entity.player.Player p&&p.getUUID().equals(g.uuid)&&p.isAlive()&&!p.isSpectator()&&!p.isUsingItem()&&p.getOffhandItem().isEmpty()&&ItemStack.matches(p.getMainHandItem(),g.item);
    }
    public static void receiveGrip(JsonObject o,Minecraft mc){
        tick(mc);if(mc.level==null||mc.player==null||!o.get("world").getAsString().equals(world))return;
        int id=o.get("entity").getAsInt();UUID uuid=UUID.fromString(o.get("player").getAsString());
        var e=mc.level.getEntity(id);if(!(e instanceof net.minecraft.world.entity.player.Player p)||!p.getUUID().equals(uuid)||p.distanceToSqr(mc.player)>64*64)return;
        if(!o.get("active").getAsBoolean()){grips.remove(id);return;}
        if(grips.size()<128||grips.containsKey(id))grips.put(id,new Grip(uuid,p.getMainHandItem().copy(),System.nanoTime()+1_500_000_000L));
    }
    private static Object level;
    private static String world="";
    private WeaponAnimations() {}
    public static void clear(){motions.clear();grips.clear();level=null;world="";}
    public static void tick(Minecraft mc) {
        if(level!=mc.level||!world.equals(CastigoClient.STATE.world)||!CastigoClient.STATE.active()){clear();level=mc.level;world=CastigoClient.STATE.world;}
        if(mc.level==null)return;
        long now=System.nanoTime();grips.entrySet().removeIf(e->now>e.getValue().expires||!gripping(e.getKey()));
        motions.entrySet().removeIf(entry->{
            var m=entry.getValue();var e=mc.level.getEntity(entry.getKey());
            return now-m.start>=m.duration||!(e instanceof net.minecraft.world.entity.player.Player p)||!p.getUUID().equals(m.uuid)||!p.isAlive()||p.isSpectator()||p.isUsingItem()
                    ||!ItemStack.matches(p.getItemInHand(m.offhand?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND),m.item);
        });
    }
    public static void receive(JsonObject o,Minecraft mc) {
        tick(mc);if(mc.level==null||mc.player==null||!o.get("world").getAsString().equals(world))return;
        int id=o.get("entity").getAsInt();UUID uuid=UUID.fromString(o.get("player").getAsString());
        var e=mc.level.getEntity(id);if(!(e instanceof net.minecraft.world.entity.player.Player p)||!p.getUUID().equals(uuid)||p.distanceToSqr(mc.player)>64*64)return;
        String phase=o.get("phase").getAsString();if(phase.equals("stop")){motions.remove(id);return;}
        if(!phase.equals("prepare")&&!phase.equals("release"))return;
        int ticks=o.get("duration").getAsInt();if(ticks<1||ticks>600||motions.size()>=64&&!motions.containsKey(id))return;
        var style=WeaponPose.Style.valueOf(o.get("style").getAsString());boolean off=o.get("offhand").getAsBoolean();
        boolean left=(p.getMainArm()==HumanoidArm.LEFT)^off;
        motions.put(id,new Motion(uuid,off,left,style,phase.equals("prepare"),System.nanoTime(),ticks*50_000_000L,p.getItemInHand(off?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND).copy()));
    }
    public static void firstPerson(int entity,InteractionHand hand,PoseStack stack) {
        if(hand==InteractionHand.MAIN_HAND&&gripping(entity)){
            var player=Minecraft.getInstance().player;double sign=player!=null&&player.getMainArm()==HumanoidArm.LEFT?-1:1;
            stack.translate(-.16*sign,-.12,-.12);stack.mulPose(Axis.YP.rotationDegrees((float)(-12*sign)));
        }
        var m=motions.get(entity);if(m==null||m.offhand!=(hand==InteractionHand.OFF_HAND))return;
        var p=m.pose();double sign=m.left?-1:1;
        stack.translate(p.x()*sign,p.y(),p.z());stack.mulPose(Axis.XP.rotationDegrees((float)p.pitch()));
        stack.mulPose(Axis.YP.rotationDegrees((float)(p.yaw()*sign)));stack.mulPose(Axis.ZP.rotationDegrees((float)(p.roll()*sign)));
    }
    public static void thirdPerson(HumanoidRenderState state,HumanoidModel<?> model) {
        if(!(state instanceof AvatarRenderState avatar)||avatar.isSpectator||state.isUsingItem)return;
        boolean grip=gripping(avatar.id);
        if(grip){
            var entity=Minecraft.getInstance().level.getEntity(avatar.id);boolean left=((net.minecraft.world.entity.player.Player)entity).getMainArm()==HumanoidArm.LEFT;
            var main=left?model.leftArm:model.rightArm;var support=left?model.rightArm:model.leftArm;float sign=left?-1:1;
            main.xRot=-.95f;main.yRot=-.35f*sign;main.zRot=.15f*sign;
            support.xRot=-1.10f;support.yRot=.75f*sign;support.zRot=-.20f*sign;
        }
        var m=motions.get(avatar.id);if(m==null)return;var p=m.pose();double sign=m.left?-1:1;
        var arm=m.left?model.leftArm:model.rightArm;
        arm.xRot+=(float)Math.toRadians(p.pitch()-45*p.weight());arm.yRot+=(float)Math.toRadians(p.yaw()*sign);arm.zRot+=(float)Math.toRadians(p.roll()*sign);
        if(grip){var support=m.left?model.rightArm:model.leftArm;support.xRot+=(float)Math.toRadians(p.pitch()-45*p.weight());support.yRot+=(float)Math.toRadians(p.yaw()*sign);support.zRot+=(float)Math.toRadians(p.roll()*sign);}
        if(m.style==WeaponPose.Style.BOW) {
            var support=m.left?model.rightArm:model.leftArm;
            support.xRot=(float)(support.xRot*(1-p.weight())-Math.PI/2*p.weight());
            support.yRot+=(float)(.45*sign*p.weight());
            arm.xRot=(float)(arm.xRot*(1-p.weight())-Math.PI/2*p.weight());
        }
    }
}
