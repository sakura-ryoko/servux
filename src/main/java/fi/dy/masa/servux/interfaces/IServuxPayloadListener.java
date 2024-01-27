package fi.dy.masa.servux.interfaces;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public interface IServuxPayloadListener
{
    default void receiveServuxPayload(NbtCompound data, ServerPlayNetworking.Context ctx, Identifier id) { }
    default void sendServuxPayload(NbtCompound data, ServerPlayerEntity player) { }
    default void encodeServuxPayload(PacketByteBuf packet, ServerPlayerEntity player, Identifier id) { }
    default void encodeServuxPayloadWithType(int packetType, NbtCompound data, ServerPlayerEntity player) { }
    default void decodeServuxPayload(PacketByteBuf packet, ServerPlayerEntity player, Identifier id) { }
}
