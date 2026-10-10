package it.castigo.classes.client;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;
class AnimationHookTest {
    private ClassNode read(String name)throws Exception{try(var stream=getClass().getClassLoader().getResourceAsStream(name+".class")){assertNotNull(stream);var node=new ClassNode();new ClassReader(stream).accept(node,0);return node;}}
    @Test void attackHookTargetsRealSwingAndPreservesMinecraftCombat()throws Exception{
        var mc=read("net/minecraft/client/Minecraft");var attack=mc.methods.stream().filter(m->m.name.equals("startAttack")&&m.desc.equals("()Z")).findFirst().orElseThrow();
        int swings=0;for(var n:attack.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals("net/minecraft/client/player/LocalPlayer")&&call.name.equals("swing")&&call.desc.equals("(Lnet/minecraft/world/InteractionHand;)V"))swings++;
        assertEquals(2,swings); // Normal and piercing paths; neither is a block-breaking tick hook.
    }
    @Test void firstPersonHookMatchesArgumentLayoutAndOnlyChangesAttackFloat()throws Exception{
        var renderer=read("net/minecraft/client/renderer/ItemInHandRenderer");var method=renderer.methods.stream().filter(m->m.name.equals("submitArmWithItem")).findFirst().orElseThrow();var args=Type.getArgumentTypes(method.desc);
        assertEquals("net.minecraft.client.player.AbstractClientPlayer",args[0].getClassName());assertEquals(Type.FLOAT_TYPE,args[1]);assertEquals(Type.FLOAT_TYPE,args[2]);assertEquals("net.minecraft.world.InteractionHand",args[3].getClassName());assertEquals(Type.FLOAT_TYPE,args[4]);
        var mixin=read("it/castigo/classes/client/mixin/WeaponHandMixin");var hook=mixin.methods.stream().filter(m->m.name.equals("castigo$vanillaSwing")).findFirst().orElseThrow();assertEquals(Type.FLOAT_TYPE,Type.getReturnType(hook.desc));assertEquals(args.length+1,Type.getArgumentTypes(hook.desc).length);
    }
}
