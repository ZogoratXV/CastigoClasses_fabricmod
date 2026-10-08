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
        int age,index,phase;
        EffectMessage.Position center;
        Animation(EffectMessage effect,ParticleOptions particle) { this.effect=effect;this.particle=particle;center=effect.at(); }
    }
    public record MeshFrame(EffectMessage effect,EffectMessage.Position center,double age) {}
    private static net.minecraft.world.entity.Entity target(EffectMessage e) {
        if(e.target()==null||level==null)return null;
        var entity=e.targetEntity()<0?level.getPlayerByUUID(e.target()):level.getEntity(e.targetEntity());
        return entity!=null&&entity.getUUID().equals(e.target())&&entity.isAlive()&&!entity.isSpectator()?entity:null;
    }
    public static void stop(String handle) { java.util.UUID.fromString(handle);active.removeIf(a->handle.equals(a.effect.handle())); }
    public static List<MeshFrame> meshFrames(ClientLevel extractingLevel,float partial) {
        if(extractingLevel!=level||!CastigoClient.STATE.active())return List.of();
        var frames=new ArrayList<MeshFrame>();
        for(var a:active)if(a.effect.hasMesh()) {
            var center=a.center;
            if(a.effect.target()!=null) {
                var target=target(a.effect);if(target==null)continue;
                var p=target.getPosition(partial);center=new EffectMessage.Position(p.x,p.y+a.effect.anchorHeight(),p.z);
            }
            var start=a.effect.from();var direction=a.effect.direction();
            if(a.effect.source()!=null) {
                var source=level.getPlayerByUUID(a.effect.source());
                if(source==null||!source.isAlive())continue;
                var p=source.getPosition(partial);start=new EffectMessage.Position(p.x,p.y+source.getEyeHeight(),p.z);
            }
            if(a.effect.shape()==EffectMessage.Shape.MESH_SHIELD&&target(a.effect)!=null) {
                var look=target(a.effect).getLookAngle();direction=new EffectMessage.Position(look.x,look.y,look.z);
            }
            frames.add(new MeshFrame(a.effect.positioned(start,center,direction),center,Math.max(0,a.phase-1+partial)));
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
        if(client.level==null||client.player==null||!CastigoClient.STATE.active())return;
        EffectMessage e=EffectMessage.read(o);
        if(!e.world().equals(world)||!e.near(new EffectMessage.Position(client.player.getX(),client.player.getY(),client.player.getZ()),64))return;
        int phase=0;
        if(e.handle()!=null)for(var it=active.iterator();it.hasNext();) { var a=it.next();if(e.handle().equals(a.effect.handle())) { phase=a.phase;it.remove();break; } }
        if(active.size()>=MAX_ACTIVE)return;
        ParticleOptions particle=null;
        if(e.particles().enabled()) {
            if(e.particles().id().equals("minecraft:dust"))particle=new DustParticleOptions(e.particles().color(),e.particles().size());
            else if(BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.parse(e.particles().id())) instanceof SimpleParticleType simple)particle=simple;
        }
        var animation=new Animation(e,particle);animation.phase=phase;active.add(animation);
    }
    public static void tick(Minecraft client) {
        context(client);
        if(client.level==null||client.player==null||!CastigoClient.STATE.active()) { active.clear();return; }
        int budget=MAX_PARTICLES_PER_TICK,audioBudget=MAX_SOUNDS_PER_TICK;
        var observer=new EffectMessage.Position(client.player.getX(),client.player.getY(),client.player.getZ());
        for(var iterator=active.iterator();iterator.hasNext();) {
            Animation a=iterator.next();EffectMessage e=a.effect;
            if(e.hasMesh()&&e.target()!=null) {
                var target=target(e);
                if(target==null) { iterator.remove();continue; }
                a.center=new EffectMessage.Position(target.getX(),target.getY()+e.anchorHeight(),target.getZ());
            }
            if(e.hasMesh()&&e.shape()!=EffectMessage.Shape.MESH_BEAM?a.center.distanceSquared(observer)>64*64:!e.near(observer,64)) { iterator.remove();continue; }
            if(a.age==0&&e.sound().enabled()&&e.sound().volume()>0&&audioBudget>0) {
                audioBudget--;
                String sound=e.sound().id();
                if(sound.startsWith("castigoclasses_audio:")) {
                    String path=sound.substring(sound.indexOf(':')+1).replace('.','/');
                    if(client.getResourceManager().getResource(Identifier.parse("castigoclasses_audio:sounds/"+path+".ogg")).isEmpty())sound="minecraft:block.amethyst_block.chime";
                }
                client.level.playLocalSound(a.center.x(),a.center.y(),a.center.z(),SoundEvent.createVariableRangeEvent(Identifier.parse(sound)),
                        SoundSource.valueOf(e.sound().category()),e.sound().volume(),e.sound().pitch(),false);
            }
            a.phase++;
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

