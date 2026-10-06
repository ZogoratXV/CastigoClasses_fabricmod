package it.castigo.classes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import java.util.*;

public final class CastigoHud {
    public static final int SLOT=24;
    public static final int SLOT_HEIGHT=22;
    public static final int BAR_WIDTH=8*SLOT;
    private static final int GOLD=0xFFB49350, GOLD_LIGHT=0xFFE0C78D;
    private static final int BRONZE=0xFF655031, INK=0xF0191510, IVORY=0xFFF3E4C5;
    private static final Map<String,ItemStack> ICONS=new HashMap<>();

    private CastigoHud() {}
    static String trim(String s,int width) {
        return Minecraft.getInstance().font.plainSubstrByWidth(s,Math.max(0,width));
    }

    /** Pixel-aligned bevel and rivets: no external textures or resource pack needed. */
    static void frame(GuiGraphicsExtractor g,int x,int y,int w,int h,boolean selected) {
        g.fill(x+1,y+2,x+w+1,y+h+1,0x80000000);
        g.fill(x,y,x+w,y+h,0xFF100D09);
        g.fill(x+1,y+1,x+w-1,y+h-1,selected?GOLD_LIGHT:BRONZE);
        g.fill(x+2,y+2,x+w-2,y+h-2,INK);
        g.fill(x+2,y+1,x+w-2,y+2,selected?IVORY:GOLD);
        g.fill(x+3,y+3,x+w-3,y+4,0xFF352C1F);
        for(int cx:new int[]{x+1,x+w-3})for(int cy:new int[]{y+1,y+h-3}) {
            g.fill(cx,cy,cx+2,cy+2,GOLD);
            g.fill(cx,cy,cx+1,cy+1,GOLD_LIGHT);
        }
    }

    public static void portrait(GuiGraphicsExtractor g) {
        Minecraft mc=Minecraft.getInstance();if(mc.player==null)return;
        ClientState s=CastigoClient.STATE;
        ClientState.ClassInfo c=s.currentClass();if(c==null)return;
        int x=5,y=5,w=Math.min(172,g.guiWidth()-10);
        frame(g,x,y,w,64,false);
        frame(g,x+5,y+5,30,30,false);
        Identifier skin=mc.player.getSkin().body().texturePath();
        g.blit(skin,x+8,y+8,x+32,y+32,8/64f,16/64f,8/64f,16/64f);
        g.blit(skin,x+8,y+8,x+32,y+32,40/64f,48/64f,8/64f,16/64f);
        String level=String.valueOf(s.level);
        int badgeWidth=Math.max(16,mc.font.width(level)+6);
        int badgeX=x+w-badgeWidth-5;
        g.fill(badgeX,y+6,badgeX+badgeWidth,y+17,0xFF44341B);
        g.fill(badgeX,y+6,badgeX+badgeWidth,y+7,GOLD);
        g.centeredText(mc.font,level,badgeX+badgeWidth/2,y+8,GOLD_LIGHT);
        g.text(mc.font,trim(s.name,badgeX-(x+40)-3),x+40,y+7,IVORY);
        g.text(mc.font,trim(c.name(),w-46),x+40,y+18,GOLD_LIGHT);
        if(!s.group.isBlank())g.text(mc.font,trim(s.group,w-46),x+40,y+28,0xFFAC9E82);
        meter(g,x+6,y+39,w-12,s.health,s.maxHealth,0xFFA5343E,
                "Vita "+value(s.health)+" / "+value(s.maxHealth));
        meter(g,x+6,y+50,w-12,s.resource,s.maxResource,0xFF000000|c.resourceColor(),
                c.resourceName()+" "+value(s.resource)+" / "+value(s.maxResource));
        // MMO progress stays as a discreet gold line beneath the resource.
        int xpWidth=w-12;
        g.fill(x+6,y+60,x+6+xpWidth,y+62,0xFF3C3020);
        double progress=s.xpNext==0?1:Math.max(0,Math.min(1,(double)s.xp/s.xpNext));
        g.fill(x+6,y+60,x+6+(int)(xpWidth*progress),y+62,GOLD);
    }

    private static String value(double n) { return String.valueOf((int)Math.ceil(n)); }
    private static void meter(GuiGraphicsExtractor g,int x,int y,int width,double value,double max,int color,String label) {
        g.fill(x,y-1,x+width,y+9,BRONZE);
        g.fill(x+1,y,x+width-1,y+8,0xFF211B16);
        int filled=(int)((width-2)*Math.max(0,Math.min(1,value/Math.max(1,max))));
        if(filled>0) {
            g.fill(x+1,y,x+1+filled,y+8,color);
            g.fill(x+1,y,x+1+filled,y+1,0x55FFFFFF);
            g.fill(x+1,y+7,x+1+filled,y+8,0x60000000);
        }
        g.centeredText(Minecraft.getInstance().font,trim(label,width-6),x+width/2,y,IVORY);
    }

    public static int barX(GuiGraphicsExtractor g) { return (g.guiWidth()-BAR_WIDTH)/2; }
    public static void bar(GuiGraphicsExtractor g,List<String> order,int selected) {
        slots(g,order,barX(g),g.guiHeight()-24,selected);
    }
    public static void slots(GuiGraphicsExtractor g,List<String> order,int x,int y,int selected) {
        Minecraft mc=Minecraft.getInstance();
        ClientState s=CastigoClient.STATE;ClientState.ClassInfo c=s.currentClass();if(c==null)return;
        for(int i=0;i<Math.min(8,order.size());i++) {
            ClientState.Skill skill=c.skill(order.get(i));if(skill==null)continue;
            int sx=x+i*SLOT;
            frame(g,sx,y,22,SLOT_HEIGHT,i==selected);
            ItemStack icon=ICONS.computeIfAbsent(skill.icon(),id-> {
                Identifier key=Identifier.tryParse(id);
                Item item=key==null?Items.AMETHYST_SHARD:BuiltInRegistries.ITEM.getValue(key);
                return new ItemStack(item==null||item==Items.AIR?Items.AMETHYST_SHARD:item);
            });
            g.item(icon,sx+3,y+3);
            g.fill(sx+4,y+19,sx+18,y+20,0xFF000000|skill.color());
            long remaining=s.remaining(skill.id());
            if(s.level<skill.unlockLevel()) {
                g.fill(sx+2,y+2,sx+20,y+20,0xBB100D09);
                g.centeredText(mc.font,"L"+skill.unlockLevel(),sx+11,y+8,0xFFC4B698);
            } else if(remaining>0) {
                int h=(int)(18*Math.min(1,(double)remaining/Math.max(1,skill.cooldownMs())));
                g.fill(sx+2,y+20-h,sx+20,y+20,0xCF100D09);
                g.centeredText(mc.font,String.valueOf((int)Math.ceil(remaining/1000.0)),sx+11,y+8,IVORY);
            } else if(s.resource<skill.cost()) {
                g.fill(sx+2,y+2,sx+20,y+20,0x88520F18);
            }
            g.text(mc.font,trim(mc.options.keyHotbarSlots[i].getTranslatedKeyMessage().getString(),10),sx+2,y+1,GOLD_LIGHT);
        }
    }
}
