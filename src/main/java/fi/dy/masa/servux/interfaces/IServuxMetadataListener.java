package fi.dy.masa.servux.interfaces;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;


public interface IServuxMetadataListener
{
    default void reset() { }
    default void receiveServuxMetadata(NbtCompound data, ServerPlayNetworking.Context ctx, Identifier id) { }
    default void sendServuxMetadata(NbtCompound data, ServerPlayerEntity player) { }
    default void encodeServuxMetadata(NbtCompound data, ServerPlayerEntity player, Identifier id) { }
    default void decodeServuxMetadata(NbtCompound data, ServerPlayerEntity player, Identifier id) { }
}
