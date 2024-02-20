package fi.dy.masa.servux.network.handlers;

import fi.dy.masa.servux.network.payload.PayloadType;
import fi.dy.masa.servux.network.payload.ServuxByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public interface IPluginServerConfigHandler<T extends CustomPayload> extends ServerConfigurationNetworking.ConfigurationPacketHandler<T>
{
    PayloadType getPayloadType();
    default void reset(PayloadType type) {}
    default void registerConfigPayload(PayloadType type) {}
    default void registerConfigHandler(PayloadType type) {}
    default void unregisterConfigHandler(PayloadType type) {}
    default <P extends CustomPayload> void receiveC2SConfigPayload(PayloadType type, P payload, ServerConfigurationNetworking.Context ctx) {}
    default <P extends CustomPayload> void receiveC2SConfigPayload(PayloadType type, P payload, ServerConfigurationNetworkHandler handler, CallbackInfo ci) {}
    default void decodeC2SNbtCompound(PayloadType type, NbtCompound data, ServerPlayerEntity player) {}
    default void decodeC2SByteBuf(PayloadType type, ServuxByteBuf data, ServerPlayerEntity player) {}

    // TODO Senders/Encoders need to be implemented on the Mod end,
    //  but we need to provide them with an interface for calling ClientConfig.send on a standard roadmap
    default <P extends CustomPayload> void sendS2CConfigPayload(PayloadType type, P payload, ServerPlayerEntity player) {}
    default <P extends CustomPayload> void sendS2CConfigPayload(PayloadType type, P payload, ServerConfigurationNetworkHandler handler) {}
    default void encodeS2CNbtCompound(PayloadType type, NbtCompound data, ServerPlayerEntity player) {}
    default void encodeS2CByteBuf(PayloadType type, ServuxByteBuf data, ServerPlayerEntity player) {}
}
