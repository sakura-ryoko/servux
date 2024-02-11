package fi.dy.masa.servux.network.handlers;

import fi.dy.masa.servux.network.payload.PayloadType;
import fi.dy.masa.servux.network.payload.ServuxByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

public interface IPluginServerPlayHandler<T extends CustomPayload> extends ServerPlayNetworking.PlayPayloadHandler<T>
{
    PayloadType getPayloadType();
    default void reset(PayloadType type) {}
    default void registerPlayPayload(PayloadType type) {}
    default void registerPlayHandler(PayloadType type) {}
    default void unregisterPlayHandler(PayloadType type) {}
    default <P extends CustomPayload> void receiveC2SPlayPayload(PayloadType type, P payload, ServerPlayNetworking.Context ctx) {}
    default void decodeC2SNbtCompound(PayloadType type, NbtCompound data, ServerPlayerEntity player) {}
    default void decodeC2SByteBuf(PayloadType type, ServuxByteBuf data, ServerPlayerEntity player) {}

    // TODO Sender/Encoders need to be implemented on the Mod end,
    //  so we need to provide them with an interface for calling ClientPlay.Send on a standard roadmap
    default <P extends CustomPayload> void sendS2CPlayPayload(PayloadType type, P payload, ServerPlayerEntity player) {}
    default void encodeS2CNbtCompound(PayloadType type, NbtCompound data, ServerPlayerEntity player) {}
    default void encodeS2CByteBuf(PayloadType type, ServuxByteBuf data, ServerPlayerEntity player) {}
}
