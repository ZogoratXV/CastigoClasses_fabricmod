package it.castigo.classes.client;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class PayloadTest {
    @Test void rawUtf8HasNoLengthPrefixAndMatchesBukkitPluginMessages() {
        String json="{\"v\":1,\"type\":\"hello\",\"test\":\"abilità\"}";
        RegistryFriendlyByteBuf buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            ClassesPayload.CODEC.encode(buffer,new ClassesPayload(json));
            assertEquals(json.getBytes(StandardCharsets.UTF_8).length,buffer.readableBytes());
            assertEquals((byte)'{',buffer.getByte(0));assertEquals(json,ClassesPayload.CODEC.decode(buffer).json());
        } finally { buffer.release(); }
    }
    @Test void oversizedRequestsAreRejectedBeforeSending() {
        RegistryFriendlyByteBuf buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try { assertThrows(IllegalArgumentException.class,()->ClassesPayload.CODEC.encode(buffer,new ClassesPayload("a".repeat(2049)))); }
        finally { buffer.release(); }
    }
}
