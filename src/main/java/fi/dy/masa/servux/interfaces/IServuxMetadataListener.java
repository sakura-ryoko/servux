package fi.dy.masa.servux.interfaces;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;

public interface IServuxMetadataListener
{
    default void reset() { }
    default void receiveServuxMetadata(NbtCompound data, ClientPlayNetworking.Context ctx) { }
    default void sendServuxMetadata(NbtCompound data) { }
    default void encodeServuxMetadata(NbtCompound data) { }
    default void decodeServuxMetadata(NbtCompound data) { }
}
