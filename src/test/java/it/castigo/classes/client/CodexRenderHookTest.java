package it.castigo.classes.client;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;
class CodexRenderHookTest {
    private ClassNode read(String name)throws Exception{try(var in=getClass().getClassLoader().getResourceAsStream(name+".class")){assertNotNull(in);var node=new ClassNode();new ClassReader(in).accept(node,0);return node;}}
    @Test void extractionCapturesItemArgumentAndFinalStateExactlyOnce()throws Exception{
        var node=read("net/minecraft/client/gui/GuiGraphicsExtractor");var method=node.methods.stream().filter(m->m.name.equals("item")&&m.desc.equals("(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V")).findFirst().orElseThrow();
        int calls=0;for(var instruction:method.instructions)if(instruction instanceof MethodInsnNode call&&call.owner.equals("net/minecraft/client/renderer/state/gui/GuiRenderState")&&call.name.equals("addItem"))calls++;
        assertEquals(1,calls);
    }
    @Test void bothAtlasAndOversizedItemsUseExpectedFinalBlitPipeline()throws Exception{
        for(String[] target:new String[][]{{"net/minecraft/client/gui/render/GuiRenderer","submitBlitFromItemAtlas"},{"net/minecraft/client/gui/render/pip/PictureInPictureRenderer","blitTexture"}}){
            var method=read(target[0]).methods.stream().filter(m->m.name.equals(target[1])).findFirst().orElseThrow();int fields=0;
            for(var instruction:method.instructions)if(instruction instanceof FieldInsnNode field&&field.owner.equals("net/minecraft/client/renderer/RenderPipelines")&&field.name.equals("GUI_TEXTURED_PREMULTIPLIED_ALPHA"))fields++;
            assertEquals(1,fields,target[0]);
        }
        var pipeline=read("net/minecraft/client/renderer/RenderPipelines");assertTrue(pipeline.fields.stream().anyMatch(f->f.name.equals("GUI_TEXTURED_SNIPPET")));assertTrue(pipeline.methods.stream().anyMatch(m->m.name.equals("register")&&m.desc.equals("(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)Lcom/mojang/blaze3d/pipeline/RenderPipeline;")));
    }
}
