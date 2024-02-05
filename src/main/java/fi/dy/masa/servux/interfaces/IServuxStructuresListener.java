package fi.dy.masa.servux.interfaces;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;

public interface IServuxStructuresListener
{
    default void reset() { }
    default void receiveServuxStructures(NbtCompound data, ClientPlayNetworking.Context ctx) { }
    default void sendServuxStructures(NbtCompound data) { }
    default void encodeServuxStructures(NbtCompound data) { }
    default void decodeServuxStructures(NbtCompound data) { }
}
