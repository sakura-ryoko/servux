package fi.dy.masa.servux.interfaces;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

public interface ISyncmaticaPayloadListener
{
    default void sendSyncmaticaPayload(NbtCompound data, ServerPlayerEntity player) { }
    default void receiveSyncmaticaPayload(NbtCompound data, ServerPlayNetworking.Context ctx) { }
    default void encodeSyncmaticaPayload(NbtCompound data, ServerPlayerEntity player) { }
    default void decodeSyncmaticaPayload(NbtCompound data, ServerPlayerEntity player) { }
}
