package it.castigo.classes.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class SkillScreen extends Screen {
    private static final String[] IDS={"strength","dexterity","health","mana","intelligence","attack","defense"};
    private static final String[] LABELS={"Forza","Destrezza","Vita","Risorsa","Intelligenza","Attacco","Difesa"};
    private static final String[] EFFECTS={
        "Aumenta il danno degli attacchi corpo a corpo.",
        "Aumenta la velocità degli attacchi corpo a corpo.",
        "Aumenta la vita massima; non cura istantaneamente.",
        "Aumenta la capacità della risorsa; non la ricarica.",
        "Aumenta danno, cura e barriera delle skill in base alla loro scala.",
        "Aggiunge danno agli attacchi corpo a corpo.",
        "Riduce i danni ricevuti da entità, prima delle difese vanilla."
    };
    private List<String> order;
    private final String initialClass;
    private int picked=-1,dragging=-1;
    private boolean changed,attributes=true;
    private long nextAllocationAt;
    private final List<Button> pointButtons=new ArrayList<>();
    private Button saveButton,closeButton,attributesTab,skillsTab;

    public SkillScreen() {
        super(Component.literal("Personaggio e abilità"));
        order=new ArrayList<>(CastigoClient.STATE.slots);initialClass=CastigoClient.STATE.classId;
    }
    private int left() { return (width-264)/2; }
    private int slotsLeft() { return (width-CastigoHud.BAR_WIDTH)/2; }
    private int rowY() { return Math.min(height-70,132); }
    private int statRow(int i) { return 65+i*15; }

    @Override protected void init() {
        pointButtons.clear();
        attributesTab=addRenderableWidget(Button.builder(Component.literal("Attributi"),button->{ attributes=true;updateButtons(); })
                .bounds(width/2-112,27,109,18).build());
        skillsTab=addRenderableWidget(Button.builder(Component.literal("Disposizione skill"),button->{ attributes=false;updateButtons(); })
                .bounds(width/2+3,27,109,18).build());
        for(int i=0;i<IDS.length;i++) {
            String id=IDS[i];
            pointButtons.add(addRenderableWidget(Button.builder(Component.literal("+"),button->{
                if(System.currentTimeMillis()<nextAllocationAt)return;
                nextAllocationAt=System.currentTimeMillis()+250;
                CastigoClient.allocate(id);updateButtons();
            }).bounds(left()+242,statRow(i)-2,18,14).build()));
        }
        saveButton=addRenderableWidget(Button.builder(Component.literal("Salva disposizione"),button->{
            CastigoClient.reorder(order);changed=false;onClose();
        }).bounds(width/2-112,height-25,142,20).build());
        closeButton=addRenderableWidget(Button.builder(Component.literal("Chiudi"),button->onClose())
                .bounds(width/2+36,height-25,76,20).build());
        updateButtons();
    }
    private void updateButtons() {
        ClientState s=CastigoClient.STATE;
        saveButton.visible=!attributes;saveButton.active=!attributes;
        closeButton.setMessage(Component.literal(attributes?"Chiudi":"Annulla"));
        attributesTab.active=!attributes;skillsTab.active=attributes;
        for(int i=0;i<pointButtons.size();i++) {
            Button b=pointButtons.get(i);b.visible=attributes;
            b.active=attributes&&s.statPointSystem&&s.availablePoints>0&&s.allocatable.contains(IDS[i])&&System.currentTimeMillis()>=nextAllocationAt;
        }
    }
    @Override public void tick() {
        if(!CastigoClient.STATE.active()||!initialClass.equals(CastigoClient.STATE.classId)) { onClose();return; }
        updateButtons();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        g.fill(0,0,width,height,0xEC17130E);
        super.extractRenderState(g,mouseX,mouseY,delta);
        ClientState s=CastigoClient.STATE;ClientState.ClassInfo c=s.currentClass();if(c==null)return;
        g.centeredText(font,c.name()+" · Livello "+s.level,width/2,10,0xFFE0C78D);
        if(attributes)renderAttributes(g,mouseX,mouseY,s,c);else renderSkills(g,mouseX,mouseY,s,c);
    }
    private void renderAttributes(GuiGraphicsExtractor g,int mouseX,int mouseY,ClientState s,ClientState.ClassInfo c) {
        g.centeredText(font,s.statPointSystem?"Punti disponibili: "+s.availablePoints:"Assegnazione punti non supportata dal server",width/2,50,0xFFE0C78D);
        for(int i=0;i<IDS.length;i++) {
            int y=statRow(i);String id=IDS[i];String label=i==3?c.resourceName():LABELS[i];
            g.text(font,CastigoHud.trim(label,91),left(),y,0xFFF3E4C5);
            g.text(font,format(s.stats.getOrDefault(id,0d)),left()+94,y,0xFFE0C78D);
            g.text(font,"Spesi: "+s.allocatedPoints.getOrDefault(id,0d).intValue(),left()+162,y,0xFFBBAE95);
            if(mouseY>=y-2&&mouseY<y+12&&mouseX>=left()&&mouseX<left()+264) {
                List<net.minecraft.util.FormattedCharSequence> lines=new ArrayList<>();
                lines.add(Component.literal(label+": base + crescita + punti assegnati").getVisualOrderText());
                lines.addAll(font.split(Component.literal(EFFECTS[i]),240));
                lines.add(Component.literal("1 punto → +"+format(s.pointGains.getOrDefault(id,0d))+" "+label).getVisualOrderText());
                lines.add(Component.literal("Il pulsante + assegna e salva subito un punto.").getVisualOrderText());
                if(!s.allocatable.contains(id))lines.add(Component.literal("Punti esauriti, attributo al limite o assegnazione disabilitata.").getVisualOrderText());
                g.setTooltipForNextFrame(font,lines,mouseX,mouseY);
            }
        }
        if(!s.combat.isEmpty()) {
            g.centeredText(font,"Attacco: "+format(s.combat.getOrDefault("attackDamage",0d))+"  |  Velocità: "+format(s.combat.getOrDefault("attackSpeed",0d)),width/2,174,0xFFE0C78D);
            g.centeredText(font,"Riduzione danni da difesa: "+format(s.combat.getOrDefault("defenseReductionPercent",0d))+"%",width/2,185,0xFFBBAE95);
        }
    }
    private void renderSkills(GuiGraphicsExtractor g,int mouseX,int mouseY,ClientState s,ClientState.ClassInfo c) {
        g.centeredText(font,"Trascina le skill o clicca due slot per scambiarli.",width/2,55,0xFFBBAE95);
        g.centeredText(font,"Barra abilità · "+CastigoClient.toggle.getTranslatedKeyMessage().getString(),width/2,rowY()-15,0xFFE0C78D);
        CastigoHud.slots(g,order,slotsLeft(),rowY(),picked);
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
    private static String format(double number) { return String.format(Locale.ITALIAN,"%.2f",number); }
    private int slot(double x,double y) {
        if(attributes||y<rowY()||y>=rowY()+CastigoHud.SLOT_HEIGHT||x<slotsLeft()||x>=slotsLeft()+CastigoHud.BAR_WIDTH)return -1;
        return (int)(x-slotsLeft())/CastigoHud.SLOT;
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
