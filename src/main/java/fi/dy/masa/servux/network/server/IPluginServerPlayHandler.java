package fi.dy.masa.servux.network.server;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Interface for ServerPlayHandler
 * @param <T> (Payload)
 */
public interface IPluginServerPlayHandler<T extends CustomPayload> extends ServerPlayNetworking.PlayPayloadHandler<T>
{
    Identifier getPayloadChannel();
    boolean isPlayRegistered(Identifier channel);
    default void setPlayRegistered(Identifier channel) {}
    default void reset(Identifier channel) {}
    default void registerPlayPayload(Identifier channel) {}
    default void registerPlayHandler(Identifier channel) {}
    default void unregisterPlayHandler(Identifier channel) {}
    default void decodeNbtCompound(Identifier channel, NbtCompound data, ServerPlayerEntity player) {}
    default void decodeByteBuf(Identifier channel, ServuxBuf data, ServerPlayerEntity player) {}
    default void decodeObject(Identifier channel, Object data, ServerPlayerEntity player) {}

    // For reference, but required in the packet handler's if you want to actually send data
    default void encodeNbtCompound(NbtCompound data, ServerPlayerEntity player) {}
    default void encodeByteBuf(ServuxBuf data, ServerPlayerEntity player) {}
    default void encodeObject(Object data, ServerPlayerEntity player) {}
    default <P extends CustomPayload> void receivePlayPayload(P payload, ServerPlayNetworking.Context ctx) {}
    default <P extends CustomPayload> void receivePlayPayload(P payload, ServerPlayNetworkHandler handler, CallbackInfo ci) {}
    default <P extends CustomPayload> void sendPlayPayload(P payload, ServerPlayerEntity player) {}
    default <P extends CustomPayload> void sendPlayPayload(P payload, ServerPlayNetworkHandler handler) {}
}
