package it.castigo.classes.client;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.*;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import java.util.List;

public final class CastigoClient implements ClientModInitializer {
    public static final ClientState STATE=new ClientState();
    public static boolean skillMode;
    public static KeyMapping toggle,editor;
    private static int handshakeTicks;
    @Override public void onInitializeClient() {
        PayloadTypeRegistry.clientboundPlay().register(ClassesPayload.TYPE,ClassesPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ClassesPayload.TYPE,ClassesPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ClassesPayload.TYPE,(payload,context)->context.client().execute(()-> {
            try {
                JsonObject o=JsonParser.parseString(payload.json()).getAsJsonObject();if(o.get("v").getAsInt()!=1)return;
                if(o.get("type").getAsString().equals("feedback")) {
                    if(context.client().player!=null)context.client().player.sendOverlayMessage(Component.literal(o.get("message").getAsString()));
                } else STATE.receive(o);
            } catch(RuntimeException ignored) { /* Unknown or malformed server data is ignored. */ }
        }));
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->reset());
        KeyMapping.Category category=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("castigoclasses","controls"));
        toggle=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.castigoclasses.toggle",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_R,category));
        editor=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.castigoclasses.editor",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_K,category));
        ClientTickEvents.END_CLIENT_TICK.register(client-> {
            if(client.player==null)return;
            if(!STATE.active()&&++handshakeTicks>=40) { handshakeTicks=0;request("hello",new JsonObject()); }
            while(toggle.consumeClick()) {
                if(client.gui.screen()==null&&STATE.active()&&!client.player.isSpectator())skillMode=!skillMode;
                else if(!STATE.active())client.player.sendOverlayMessage(Component.translatable("castigoclasses.unavailable"));
            }
            while(editor.consumeClick())if(client.gui.screen()==null&&STATE.active())client.gui.setScreen(new SkillScreen());
            if(!STATE.active()||client.player.isSpectator())skillMode=false;
        });
        HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR,original->(graphics,delta)-> {
            if(isSkillMode())CastigoHud.bar(graphics,STATE.slots,-1);
            else original.extractRenderState(graphics,delta);
        });
        HudElementRegistry.replaceElement(VanillaHudElements.HELD_ITEM_TOOLTIP,original->(graphics,delta)-> {
            if(!isSkillMode())original.extractRenderState(graphics,delta);
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("castigoclasses","portrait"),(graphics,delta)-> {
            if(STATE.active()&&!Minecraft.getInstance().gui.hud.isHidden())CastigoHud.portrait(graphics);
        });
    }
    private static void reset() { STATE.clear();skillMode=false;handshakeTicks=35; }
    public static boolean isSkillMode() { return skillMode&&STATE.active(); }
    public static void request(String type,JsonObject o) {
        if(Minecraft.getInstance().getConnection()==null||!ClientPlayNetworking.canSend(ClassesPayload.TYPE))return;
        o.addProperty("v",1);o.addProperty("type",type);ClientPlayNetworking.send(new ClassesPayload(o.toString()));
    }
    public static void reorder(List<String> slots) {
        JsonObject o=new JsonObject();JsonArray a=new JsonArray();slots.forEach(a::add);o.add("slots",a);request("reorder",o);
    }
    public static boolean intercept(KeyEvent event,int action) {
        Minecraft client=Minecraft.getInstance();
        if(!isSkillMode()||client.player==null||client.player.isSpectator()||client.gui.screen()!=null||client.gui.overlay()!=null)return false;
        for(int i=0;i<9;i++)if(client.options.keyHotbarSlots[i].matches(event)) {
            if(action==GLFW.GLFW_PRESS&&i<8) {
                JsonObject o=new JsonObject();o.addProperty("slot",i);request("cast",o);
            }
            return true; // Consume press/repeat/release so the held inventory slot never changes.
        }
        return false;
    }
}
