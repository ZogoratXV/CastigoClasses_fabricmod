package it.castigo.classes.client;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.*;
import java.util.*;

/** Local animation engine. No gameplay decisions and no return packets. */
public final class ClientEffects {
    private static final int MAX_ACTIVE=64,MAX_PARTICLES_PER_TICK=512,MAX_SOUNDS_PER_TICK=8;
    private static final List<Animation> active=new ArrayList<>();
    private static final Random random=new Random();
    private static ClientLevel level;
    private static String world="";
    private static final class Animation {
        final EffectMessage effect;
        final ParticleOptions particle;
        int age,index;
        EffectMessage.Position center;
        Animation(EffectMessage effect,ParticleOptions particle) { this.effect=effect;this.particle=particle;center=effect.at(); }
    }
    public record MeshFrame(EffectMessage effect,EffectMessage.Position center,double age) {}
    public static List<MeshFrame> meshFrames(ClientLevel extractingLevel,float partial) {
        if(extractingLevel!=level||!CastigoClient.STATE.active())return List.of();
        var frames=new ArrayList<MeshFrame>();
        for(var a:active)if(a.effect.hasMesh()) {
            var center=a.center;
            if(a.effect.target()!=null) {
                var target=level.getPlayerByUUID(a.effect.target());
                if(target==null||!target.isAlive()||target.isSpectator())continue;
                var p=target.getPosition(partial);center=new EffectMessage.Position(p.x,p.y,p.z);
            }
            frames.add(new MeshFrame(a.effect,center,Math.max(0,a.age-1+partial)));
        }
        return List.copyOf(frames);
    }
    private ClientEffects() {}
    public static void clear() { active.clear();level=null;world=""; }
    private static void context(Minecraft client) {
        if(level!=client.level||!world.equals(CastigoClient.STATE.world)) {
            active.clear();level=client.level;world=CastigoClient.STATE.world;
        }
    }
    public static void receive(JsonObject o,Minecraft client) {
        context(client);
        if(client.level==null||client.player==null||!CastigoClient.STATE.active()||active.size()>=MAX_ACTIVE)return;
        EffectMessage e=EffectMessage.read(o);
        if(!e.world().equals(world)||!e.near(new EffectMessage.Position(client.player.getX(),client.player.getY(),client.player.getZ()),64))return;
        ParticleOptions particle=null;
        if(e.particles().enabled()) {
            if(e.particles().id().equals("minecraft:dust"))particle=new DustParticleOptions(e.particles().color(),e.particles().size());
            else if(BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.parse(e.particles().id())) instanceof SimpleParticleType simple)particle=simple;
        }
        active.add(new Animation(e,particle));
    }
    public static void tick(Minecraft client) {
        context(client);
        if(client.level==null||client.player==null||!CastigoClient.STATE.active()) { active.clear();return; }
        int budget=MAX_PARTICLES_PER_TICK,audioBudget=MAX_SOUNDS_PER_TICK;
        var observer=new EffectMessage.Position(client.player.getX(),client.player.getY(),client.player.getZ());
        for(var iterator=active.iterator();iterator.hasNext();) {
            Animation a=iterator.next();EffectMessage e=a.effect;
            if(e.hasMesh()&&e.target()!=null) {
                var target=client.level.getPlayerByUUID(e.target());
                if(target==null||!target.isAlive()||target.isSpectator()) { iterator.remove();continue; }
                a.center=new EffectMessage.Position(target.getX(),target.getY(),target.getZ());
            }
            if(e.hasMesh()?a.center.distanceSquared(observer)>64*64:!e.near(observer,64)) { iterator.remove();continue; }
            if(a.age==0&&e.sound().enabled()&&e.sound().volume()>0&&audioBudget>0) {
                audioBudget--;
                client.level.playLocalSound(a.center.x(),a.center.y(),a.center.z(),SoundEvent.createVariableRangeEvent(Identifier.parse(e.sound().id())),
                        SoundSource.valueOf(e.sound().category()),e.sound().volume(),e.sound().pitch(),false);
            }
            if(e.hasMesh()) {
                if(a.particle!=null&&a.age%2==0)for(int i=64;i<HealingBeam.SAMPLES&&budget>0;i++) {
                    var point=HealingBeam.sample(a.age,e.durationTicks(),i,e.radius(),e.particles().color(),e.particles().size());
                    var particle=a.particle instanceof DustParticleOptions?new DustParticleOptions(point.color(),Math.max(0.05f,point.size())):a.particle;
                    int count=Math.min(e.particles().count(),budget);budget-=count;double spread=e.particles().spread();
                    for(int j=0;j<count;j++)client.level.addParticle(particle,
                            a.center.x()+point.x()+random.nextGaussian()*spread,
                            a.center.y()+point.y()*e.mesh().height()/2.8+random.nextGaussian()*spread,
                            a.center.z()+point.z()+random.nextGaussian()*spread,0,0.025,0);
                }
                if(++a.age>=e.durationTicks())iterator.remove();
                continue;
            }
            int end=e.samplesThroughTick(a.age);
            for(;a.index<end;a.index++) {
                if(a.particle==null||budget<=0)continue;
                var point=e.sample(a.index);int count=Math.min(e.particles().count(),budget);budget-=count;
                for(int i=0;i<count;i++) {
                    double spread=e.particles().spread();
                    client.level.addParticle(a.particle,point.x()+random.nextGaussian()*spread,
                            point.y()+random.nextGaussian()*spread,point.z()+random.nextGaussian()*spread,0,0,0);
                }
            }
            if(++a.age>=e.durationTicks()||e.shape()==EffectMessage.Shape.BURST)iterator.remove();
        }
    }
}

