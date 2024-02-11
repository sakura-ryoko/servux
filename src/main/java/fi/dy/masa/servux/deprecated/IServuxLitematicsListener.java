package fi.dy.masa.servux.deprecated;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

@Deprecated
public interface IServuxLitematicsListener
{
    default void reset() { }
    default void receiveServuxLitematics(NbtCompound data, ServerPlayNetworking.Context ctx, Identifier id) { }
    default void sendServuxLitematics(NbtCompound data, ServerPlayerEntity player) { }
    default void encodeServuxLitematics(NbtCompound data, ServerPlayerEntity player, Identifier id) { }
    default void decodeServuxLitematics(NbtCompound data, ServerPlayerEntity player, Identifier id) { }
}
