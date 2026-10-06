package it.castigo.classes.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class SkillScreen extends Screen {
    private List<String> order;
    private final String initialClass;
    private int picked=-1,dragging=-1;
    private boolean changed;
    public SkillScreen() {
        super(Component.literal("Personaggio e abilità"));order=new ArrayList<>(CastigoClient.STATE.slots);initialClass=CastigoClient.STATE.classId;
    }
    private int left() { return (width-224)/2; }
    private int rowY() { return Math.min(height-64,132); }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Salva disposizione"),button-> {
            CastigoClient.reorder(order);changed=false;onClose();
        }).bounds(width/2-112,height-29,142,20).build());
        addRenderableWidget(Button.builder(Component.literal("Annulla"),button->onClose()).bounds(width/2+36,height-29,76,20).build());
    }
    @Override public void tick() {
        if(!CastigoClient.STATE.active()||!initialClass.equals(CastigoClient.STATE.classId))onClose();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        g.fill(0,0,width,height,0xEC101421);
        super.extractRenderState(g,mouseX,mouseY,delta);
        ClientState state=CastigoClient.STATE;ClientState.ClassInfo c=state.currentClass();if(c==null)return;
        g.centeredText(font,c.name()+" · Livello "+state.level,width/2,12,0xFFE9D8FF);
        g.centeredText(font,"Trascina le skill o clicca due slot per scambiarli.",width/2,28,0xFFBFC6D8);
        String[] labels={"Forza","Destrezza","Vita",c.resourceName(),"Intelligenza","Attacco","Difesa"};
        String[] ids={"strength","dexterity","health","mana","intelligence","attack","defense"};
        for(int i=0;i<7;i++) {
            int col=i%2,row=i/2;
            String text=labels[i]+": "+String.format(Locale.ITALIAN,"%.1f",state.stats.getOrDefault(ids[i],0d));
            g.text(font,CastigoHud.trim(text,109),left()+col*116,47+row*13,0xFFE0E5F0);
        }
        g.centeredText(font,"Barra abilità · "+CastigoClient.toggle.getTranslatedKeyMessage().getString(),width/2,rowY()-15,0xFFC9ADFA);
        CastigoHud.slots(g,order,left(),rowY(),picked);
        if(changed)g.centeredText(font,"Modifiche da salvare",width/2,rowY()+30,0xFFFFD991);
        int hovered=slot(mouseX,mouseY);
        if(hovered>=0) {
            ClientState.Skill skill=c.skill(order.get(hovered));if(skill!=null) {
                List<net.minecraft.util.FormattedCharSequence> lines=new ArrayList<>();
                lines.add(Component.literal(skill.name()).getVisualOrderText());
                lines.addAll(font.split(Component.literal(skill.description()),230));
                lines.add(Component.literal(c.resourceName()+": "+skill.cost()+" · Ricarica: "+skill.cooldownMs()/1000.0+"s").getVisualOrderText());
                lines.add(Component.literal("Livello richiesto: "+skill.unlockLevel()).getVisualOrderText());
                g.setTooltipForNextFrame(font,lines,mouseX,mouseY);
            }
        }
    }
    private int slot(double x,double y) {
        if(y<rowY()||y>=rowY()+24||x<left()||x>=left()+224)return -1;
        return (int)(x-left())/CastigoHud.SLOT;
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        int hit=slot(event.x(),event.y());
        if(event.button()==0&&hit>=0) {
            if(picked>=0&&picked!=hit) { order=SlotOrder.swap(order,picked,hit);changed=true;picked=-1;dragging=-1; }
            else { picked=hit;dragging=hit; }
            return true;
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        int hit=slot(event.x(),event.y());
        if(event.button()==0&&dragging>=0) {
            if(hit>=0&&hit!=dragging) { order=SlotOrder.swap(order,dragging,hit);changed=true;picked=-1; }
            dragging=-1;return true;
        }
        return super.mouseReleased(event);
    }
}
