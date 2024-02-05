package fi.dy.masa.servux.interfaces;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;

public interface IServuxLitematicsListener
{
    default void reset() { }
    default void receiveServuxLitematics(NbtCompound data, ClientPlayNetworking.Context ctx) { }
    default void sendServuxLitematics(NbtCompound data) { }
    default void encodeServuxLitematics(NbtCompound data) { }
    default void decodeServuxLitematics(NbtCompound data) { }
}
