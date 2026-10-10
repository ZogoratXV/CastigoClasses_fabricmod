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
        HudTheme theme=s.hud;
        int x=5,y=5,w=theme.number("width"),h=theme.number("height");
        int border=theme.color("border",GOLD),text=theme.color("text",IVORY);
        Identifier texture=Identifier.tryParse(theme.text("texture"));
        boolean hasTexture=texture!=null&&mc.getResourceManager().getResource(texture).isPresent();
        if(!theme.text("frame").equals("NONE")) {
            g.fill(x,y,x+w,y+h,border);g.fill(x+2,y+2,x+w-2,y+h-2,theme.color("background",INK));
            if(theme.text("frame").equals("MEDIEVAL"))for(int cx:new int[]{x+2,x+w-4})for(int cy:new int[]{y+2,y+h-4})g.fill(cx,cy,cx+2,cy+2,text);
        } else if(!hasTexture)frame(g,x,y,w,h,false);
        if(hasTexture)g.blit(texture,x,y,x+w,y+h,0f,1f,0f,1f);
        int hx=x+theme.number("headX"),hy=y+theme.number("headY"),size=theme.number("headSize");
        Identifier skin=mc.player.getSkin().body().texturePath();
        g.blit(skin,hx,hy,hx+size,hy+size,8/64f,16/64f,8/64f,16/64f);
        g.blit(skin,hx,hy,hx+size,hy+size,40/64f,48/64f,8/64f,16/64f);
        int tx=x+theme.number("textX"),available=w-theme.number("textX")-6;
        g.text(mc.font,trim(s.name+" · "+s.level,available),tx,y+theme.number("nameY"),text);
        g.text(mc.font,trim(c.name(),available),tx,y+theme.number("classY"),border);
        if(!s.group.isBlank())g.text(mc.font,trim(s.group,available),tx,y+theme.number("groupY"),text);
        int bx=x+theme.number("barsX"),bw=theme.number("barsWidth");
        meter(g,bx,y+theme.number("healthY"),bw,s.health,s.maxHealth,theme.color("healthColor",0xFFA5343E),
                "Vita "+value(s.health)+" / "+value(s.maxHealth),border,text);
        meter(g,bx,y+theme.number("resourceY"),bw,s.resource,s.maxResource,theme.color("resourceColor",0xFF000000|c.resourceColor()),
                c.resourceName()+" "+value(s.resource)+" / "+value(s.maxResource),border,text);
        int xpY=y+theme.number("xpY");g.fill(bx,xpY,bx+bw,xpY+2,0xFF3C3020);
        double progress=s.classCapped||s.dailyCapped||s.xpNext==0?1:Math.max(0,Math.min(1,(double)s.xp/s.xpNext));
        g.fill(bx,xpY,bx+(int)(bw*progress),xpY+2,s.xpColor(border));
    }

    private static String value(double n) { return String.valueOf((int)Math.ceil(n)); }
    private static void meter(GuiGraphicsExtractor g,int x,int y,int width,double value,double max,int color,String label,int border,int text) {
        g.fill(x,y-1,x+width,y+9,border);
        g.fill(x+1,y,x+width-1,y+8,0xFF211B16);
        int filled=(int)((width-2)*Math.max(0,Math.min(1,value/Math.max(1,max))));
        if(filled>0) {
            g.fill(x+1,y,x+1+filled,y+8,color);
            g.fill(x+1,y,x+1+filled,y+1,0x55FFFFFF);
            g.fill(x+1,y+7,x+1+filled,y+8,0x60000000);
        }
        g.centeredText(Minecraft.getInstance().font,trim(label,width-6),x+width/2,y,text);
    }

    public static int barX(GuiGraphicsExtractor g) { return (g.guiWidth()-BAR_WIDTH)/2; }
    public static void casting(GuiGraphicsExtractor g) {
        var s=CastigoClient.STATE;if(s.castingTotal<=0||Minecraft.getInstance().player==null||Minecraft.getInstance().player.isSpectator())return;
        int x=(g.guiWidth()-182)/2,y=g.guiHeight()-59;
        g.fill(x-1,y-1,x+183,y+7,0xFF655031);g.fill(x,y,x+182,y+6,0xFF17130E);
        int fill=(int)(182*s.castingProgress(System.currentTimeMillis()));
        g.fill(x,y,x+fill,y+6,0xFFD5AE57);g.fill(x,y,x+fill,y+1,0xFFFFE9A5);
        g.centeredText(Minecraft.getInstance().font,trim(s.castingName,180),x+91,y-11,0xFFFFE9A5);
    }
    public static void bar(GuiGraphicsExtractor g,List<String> order,int selected) {
        slots(g,order,barX(g),g.guiHeight()-24,selected);
    }
    public static void slots(GuiGraphicsExtractor g,List<String> order,int x,int y,int selected) {
        Minecraft mc=Minecraft.getInstance();
        ClientState s=CastigoClient.STATE;ClientState.ClassInfo c=s.currentClass();if(c==null)return;
        for(int i=0;i<Math.min(8,order.size());i++) {
            ClientState.Skill skill=c.skill(order.get(i));if(!s.learned(skill))continue;
            int sx=x+i*SLOT;
            frame(g,sx,y,22,SLOT_HEIGHT,i==selected);
            Identifier texture=skill.icon().startsWith("texture:")?Identifier.tryParse(skill.icon().substring(8)):null;
            if(texture!=null&&mc.getResourceManager().getResource(texture).isPresent()) {
                g.blit(texture,sx+3,y+3,sx+19,y+19,0f,1f,0f,1f);
            } else {
            if(ICONS.size()>2048)ICONS.clear();
            ItemStack icon=ICONS.computeIfAbsent(skill.icon(),id-> {
                Identifier key=Identifier.tryParse(id);
                Item item=key==null?Items.AMETHYST_SHARD:BuiltInRegistries.ITEM.getValue(key);
                return new ItemStack(item==null||item==Items.AIR?Items.AMETHYST_SHARD:item);
            });
            g.item(icon,sx+3,y+3);
            }
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
