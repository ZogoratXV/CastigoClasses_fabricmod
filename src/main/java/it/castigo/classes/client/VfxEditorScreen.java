package it.castigo.classes.client;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.EntityHitResult;
import java.nio.file.Path;
import java.util.*;

/** In-world editor: preview is local; applying to the server requires admin permission. */
public final class VfxEditorScreen extends Screen {
    private static final String[] STAGES={"CAST","TRAIL","IMPACT","TELEGRAPH","HIT"};
    private static final String[] SHAPES={"BURST","LINE","RING","SPIRAL","HEALING_BEAM","MESH_RING","MESH_COLUMN","MESH_SLASH","MESH_SHIELD","MESH_BEAM","MESH_BURST","MESH_VORTEX","MESH_SIGIL","MESH_WAVE","MESH_THRUST"};
    private static final String[][] KEYS={{"duration","radius","points","color","size","spread"},{"particle","count","sound","volume","pitch"},{"height","opacity","rotation","scroll","rings","ringGap"},{"fadeIn","fadeOut","columnRadius","ringTexture","columnTexture","tint"}};
    private static final String[][] LABELS={{"Durata (1–40 tick)","Raggio (0,1–12)","Punti (2–32)","Colore RGB","Dimensione (0,05–4)","Dispersione (0–2)"},{"Particella Bukkit","Quantità (1–32)","ID suono","Volume (0–2)","Tono (0,5–2)"},{"Altezza (0,1–12)","Opacità (0–1)","Rotazione °/s ±720","Scorrimento ±4","Anelli (0–4)","Distanza anelli (0–3)"},{"Entrata (0–0,5 vita)","Uscita (0–0,5 vita)","Raggio colonna ×0,05–1","Texture anelli PNG","Texture colonna PNG","Colore mesh RGB/auto"}};
    private final Map<String,EditBox> fields=new LinkedHashMap<>();
    private String classId,skillId,status="Carica dal server o crea un preset locale";
    private JsonObject draft=VfxDraft.defaults();
    private int stage=2,page,previewTicks;
    private boolean self=true;
    private long lastRequest;
    private final Path root=FabricLoader.getInstance().getConfigDir().resolve("castigoclasses/vfx");
    public VfxEditorScreen() { super(Component.literal("Editor VFX Castigo"));classId=CastigoClient.STATE.classId;skillId=CastigoClient.STATE.currentClass().skills().getFirst().id(); }
    private int panelWidth() { return Math.min(286,width-12); }
    private int left() { return width-panelWidth()-6; }
    private void button(String text,int x,int y,int w,Runnable run) { addRenderableWidget(Button.builder(Component.literal(text),b->action(run)).bounds(x,y,w,17).build()); }
    private void action(Runnable run) { try { capture();run.run(); } catch(Exception e) { status="Errore: "+e.getMessage(); } }
    @Override protected void init() {
        draft.add("mesh",MeshSettings.read(draft.has("mesh")?draft.getAsJsonObject("mesh"):null).json());
        fields.clear();int x=left(),w=panelWidth(),half=(w-4)/2;
        button("Classe: "+classId,x,5,half,()->select(true));button("Skill: "+skillId,x+half+4,5,half,()->select(false));
        button("Fase: "+STAGES[stage],x,24,half,()->{stage=(stage+1)%STAGES.length;rebuildWidgets();});
        button("Forma: "+draft.get("shape").getAsString(),x+half+4,24,half,()->{int i=Arrays.asList(SHAPES).indexOf(draft.get("shape").getAsString());draft.addProperty("shape",SHAPES[(i+1)%SHAPES.length]);rebuildWidgets();});
        button("Pagina "+(page+1)+"/4",x,43,half,()->{page=(page+1)%4;rebuildWidgets();});
        button(self?"Anteprima: su me":"Anteprima: mira",x+half+4,43,half,()->{self=!self;rebuildWidgets();});
        for(int i=0;i<KEYS[page].length;i++) {
            String key=KEYS[page][i];int bx=x+(i%2)*(half+4),by=73+(i/2)*28;
            var field=new EditBox(font,bx,by,half,15,Component.literal(LABELS[page][i]));field.setMaxLength(160);field.setValue((page>=2?draft.getAsJsonObject("mesh"):draft).get(key).getAsString());fields.put(key,field);addRenderableWidget(field);
        }
        int y=Math.max(159,height-77);
        button("Effetto: "+on("enabled"),x,y,half,()->toggle("enabled"));button("Particelle: "+on("particlesEnabled"),x+half+4,y,half,()->toggle("particlesEnabled"));
        button("Suono: "+on("soundEnabled"),x,y+19,half,()->toggle("soundEnabled"));button("Anteprima (3 s)",x+half+4,y+19,half,()->{preview();previewTicks=60;setFocused(null);});
        button("Salva locale",x,y+38,half/2,()->io(true));button("Apri locale",x+half/2+2,y+38,half-half/2-2,()->io(false));
        button("Dal server",x+half+4,y+38,half,()->request("vfx_get"));
        button("Applica al server",x,y+57,half,()->request("vfx_save"));button("Ripristina server",x+half+4,y+57,half,()->request("vfx_reset"));
    }
    private String on(String key) { return draft.get(key).getAsBoolean()?"ON":"OFF"; }
    private void toggle(String key) { draft.addProperty(key,!draft.get(key).getAsBoolean());rebuildWidgets(); }
    private void capture() {
        for(var entry:fields.entrySet()) {
            String key=entry.getKey(),value=entry.getValue().getValue().trim();
            var destination=page>=2?draft.getAsJsonObject("mesh"):draft;
            if(Set.of("color","particle","sound","ringTexture","columnTexture","tint").contains(key))destination.addProperty(key,value);
            else { double n=Double.parseDouble(value);if(!Double.isFinite(n))throw new IllegalArgumentException("Numero non valido");destination.addProperty(key,n); }
        }
    }
    private void select(boolean changeClass) {
        if(changeClass) { var ids=new ArrayList<>(CastigoClient.STATE.classes.keySet());classId=ids.get((ids.indexOf(classId)+1)%ids.size());skillId=CastigoClient.STATE.classes.get(classId).skills().getFirst().id(); }
        else { var skills=CastigoClient.STATE.classes.get(classId).skills();int i=0;for(;i<skills.size();i++)if(skills.get(i).id().equals(skillId))break;skillId=skills.get((i+1)%skills.size()).id(); }
        status="Bozza mantenuta: carica dal server per leggere questa skill";rebuildWidgets();
    }
    private void io(boolean save) {
        try { if(save)VfxDraft.save(root,classId,skillId,STAGES[stage],draft);else { draft=VfxDraft.load(root,classId,skillId,STAGES[stage]);rebuildWidgets(); }status=save?"Salvato in config/castigoclasses/vfx":"Preset locale caricato"; }
        catch(Exception e) { throw new IllegalArgumentException(e.getMessage()); }
    }
    private void request(String type) {
        if(System.currentTimeMillis()-lastRequest<250) { status="Attendi un istante prima di riprovare";return; }
        if(!type.equals("vfx_get")&&!CastigoClient.STATE.vfxAdmin)throw new IllegalArgumentException("Serve castigo.classes.admin");
        if(type.equals("vfx_save"))VfxDraft.validate(draft);var o=new JsonObject();o.addProperty("classId",classId);o.addProperty("skillId",skillId);o.addProperty("stage",STAGES[stage]);
        if(type.equals("vfx_save"))o.add("draft",draft.deepCopy());lastRequest=System.currentTimeMillis();CastigoClient.request(type,o);status="Richiesta inviata...";
    }
    public void receive(JsonObject o) {
        if(!o.has("classId")||!classId.equals(o.get("classId").getAsString())||!skillId.equals(o.get("skillId").getAsString())||!STAGES[stage].equals(o.get("stage").getAsString()))return;
        if(o.has("draft")) { draft=o.getAsJsonObject("draft").deepCopy();rebuildWidgets(); }
        status=o.get("message").getAsString();
    }
    @Override public void tick() {
        if(!CastigoClient.STATE.active()) { onClose();return; }
        if(previewTicks>0)previewTicks--;
    }
    private void preview() {
        var mc=Minecraft.getInstance();if(mc.player==null||!draft.get("enabled").getAsBoolean())return;
        var from=new EffectMessage.Position(mc.player.getX(),mc.player.getY(),mc.player.getZ());
        var at=from;UUID target=self?mc.player.getUUID():null;int targetId=self?mc.player.getId():-1;
        if(!self) {
            if(mc.hitResult instanceof EntityHitResult hit&&hit.getEntity() instanceof net.minecraft.world.entity.LivingEntity p) { at=new EffectMessage.Position(p.getX(),p.getY(),p.getZ());target=p.getUUID();targetId=p.getId(); }
            else { var look=mc.player.getLookAngle().scale(3).add(mc.player.position());at=new EffectMessage.Position(look.x,look.y,look.z); }
        }
        var packet=VfxDraft.packet(draft,CastigoClient.STATE.world,from,at,target);
        if(targetId>=0)packet.addProperty("targetEntity",targetId);
        var look=mc.player.getLookAngle();var direction=new JsonArray();direction.add(look.x);direction.add(look.y);direction.add(look.z);packet.add("direction",direction);
        ClientEffects.receive(packet,mc);
        status="Anteprima locale: nessuna skill/cura/danno eseguita";
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean doubleClick) {
        return previewTicks>0||super.mouseClicked(event,doubleClick);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        if(previewTicks>0) { g.text(font,"Anteprima locale · Esc per uscire",6,6,0xFFFFE9A5);return; }
        int x=left();g.fill(x-4,0,width,height,0xD91B1710);
        super.extractRenderState(g,mouseX,mouseY,delta);
        int half=(panelWidth()-4)/2;
        for(int i=0;i<LABELS[page].length;i++)g.text(font,LABELS[page][i],x+(i%2)*(half+4),63+(i/2)*28,0xFFE0C78D);
        g.text(font,CastigoHud.trim(status,width-12),6,height-90,0xFFFFE9A5);
    }
}


