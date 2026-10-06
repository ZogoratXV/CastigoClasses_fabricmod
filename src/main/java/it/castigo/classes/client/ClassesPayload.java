package it.castigo.classes.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.nio.charset.StandardCharsets;

public record ClassesPayload(String json) implements CustomPacketPayload {
    public static final Type<ClassesPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("castigo","classes"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ClassesPayload> CODEC=new StreamCodec<>() {
        @Override public ClassesPayload decode(RegistryFriendlyByteBuf buffer) {
            int size=buffer.readableBytes();
            if(size>30000)throw new IllegalArgumentException("Castigo payload too large");
            byte[] bytes=new byte[size];buffer.readBytes(bytes);
            return new ClassesPayload(new String(bytes,StandardCharsets.UTF_8));
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer,ClassesPayload payload) {
            byte[] bytes=payload.json().getBytes(StandardCharsets.UTF_8);
            if(bytes.length>2048)throw new IllegalArgumentException("Castigo request too large");
            buffer.writeBytes(bytes);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
