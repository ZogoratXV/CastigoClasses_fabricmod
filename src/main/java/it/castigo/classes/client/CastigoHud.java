package it.castigo.classes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import java.util.*;

public final class CastigoHud {
    public static final int SLOT=28;
    private static final Map<String,ItemStack> ICONS=new HashMap<>();
    private CastigoHud() {}
    static String trim(String s,int width) { return Minecraft.getInstance().font.plainSubstrByWidth(s,Math.max(0,width)); }
    public static void portrait(GuiGraphicsExtractor g) {
        Minecraft mc=Minecraft.getInstance();if(mc.player==null)return;
        ClientState s=CastigoClient.STATE;ClientState.ClassInfo c=s.currentClass();if(c==null)return;
        int x=7,y=7,w=Math.min(225,g.guiWidth()-14);
        g.fill(x,y,x+w,y+91,0xE6121827);g.fill(x,y,x+2,y+91,0xFF000000|c.resourceColor());
        Identifier skin=mc.player.getSkin().body().texturePath();
        g.blit(skin,x+8,y+8,x+38,y+38,8/64f,16/64f,8/64f,16/64f);
        g.blit(skin,x+8,y+8,x+38,y+38,40/64f,48/64f,8/64f,16/64f);
        g.text(mc.font,trim(s.name,w-53),x+46,y+8,0xFFF1ECFF);
        g.text(mc.font,trim(c.name()+" · Lv. "+s.level,w-53),x+46,y+20,0xFFCBB4FF);
        if(!s.group.isBlank())g.text(mc.font,trim(s.group,w-53),x+46,y+32,0xFFB0B9CC);
        meter(g,x+8,y+46,w-16,10,s.health,s.maxHealth,0xFFCE526A,"Vita "+value(s.health)+" / "+value(s.maxHealth));
        meter(g,x+8,y+60,w-16,10,s.resource,s.maxResource,0xFF000000|c.resourceColor(),c.resourceName()+" "+value(s.resource)+" / "+value(s.maxResource));
        meter(g,x+8,y+76,w-16,8,s.xpNext==0?1:s.xp,s.xpNext==0?1:s.xpNext,0xFFB892EB,
                s.xpNext==0?"Livello massimo":"XP "+s.xp+" / "+s.xpNext);
    }
    private static String value(double n) { return String.valueOf((int)Math.ceil(n)); }
    private static void meter(GuiGraphicsExtractor g,int x,int y,int width,int height,double value,double max,int color,String label) {
        g.fill(x,y,x+width,y+height,0xFF293043);
        g.fill(x,y,x+(int)(width*Math.max(0,Math.min(1,value/Math.max(1,max)))),y+height,color);
        g.centeredText(Minecraft.getInstance().font,trim(label,width-4),x+width/2,y+(height-8)/2,0xFFFFFFFF);
    }
    public static int barX(GuiGraphicsExtractor g) { return (g.guiWidth()-8*SLOT)/2; }
    public static void bar(GuiGraphicsExtractor g,List<String> order,int selected) { slots(g,order,barX(g),g.guiHeight()-25,selected); }
    public static void slots(GuiGraphicsExtractor g,List<String> order,int x,int y,int selected) {
        Minecraft mc=Minecraft.getInstance();ClientState s=CastigoClient.STATE;ClientState.ClassInfo c=s.currentClass();if(c==null)return;
        for(int i=0;i<Math.min(8,order.size());i++) {
            ClientState.Skill skill=c.skill(order.get(i));if(skill==null)continue;int sx=x+i*SLOT;
            g.fill(sx,y,sx+26,y+24,i==selected?0xFFFFDA88:0xFF48455D);
            g.fill(sx+1,y+1,sx+25,y+23,0xEF171A29);
            ItemStack icon=ICONS.computeIfAbsent(skill.icon(),id-> {
                Identifier key=Identifier.tryParse(id);Item item=key==null?Items.AMETHYST_SHARD:BuiltInRegistries.ITEM.getValue(key);
                return new ItemStack(item==null||item==Items.AIR?Items.AMETHYST_SHARD:item);
            });
            g.item(icon,sx+5,y+3);g.fill(sx+1,y+22,sx+25,y+24,0xFF000000|skill.color());
            long remaining=s.remaining(skill.id());
            if(s.level<skill.unlockLevel()) {
                g.fill(sx+1,y+1,sx+25,y+23,0xB0000000);g.centeredText(mc.font,"L"+skill.unlockLevel(),sx+13,y+9,0xFFCCCCCC);
            } else if(remaining>0) {
                int h=(int)(22*Math.min(1,(double)remaining/Math.max(1,skill.cooldownMs())));
                g.fill(sx+1,y+23-h,sx+25,y+23,0xC0000000);
                g.centeredText(mc.font,String.valueOf((int)Math.ceil(remaining/1000.0)),sx+13,y+8,0xFFFFFFFF);
            } else if(s.resource<skill.cost())g.fill(sx+1,y+1,sx+25,y+23,0x88770022);
            g.text(mc.font,trim(mc.options.keyHotbarSlots[i].getTranslatedKeyMessage().getString(),12),sx+2,y+2,0xFFFFFFFF);
        }
    }
}
