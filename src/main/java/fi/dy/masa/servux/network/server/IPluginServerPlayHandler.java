package fi.dy.masa.servux.network.server;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Interface for ServerPlayHandler
 * @param <T> (Payload Param)
 */
public interface IPluginServerPlayHandler<T extends CustomPayload> extends ServerPlayNetworking.PlayPayloadHandler<T>
{
    /**
     * Returns your HANDLER's CHANNEL ID
     * @return (Channel ID)
     */
    Identifier getPayloadChannel();

    /**
     * Returns if your Channel ID has been registered to your Play Payload.
     * @param channel (Your Channel ID)
     * @return (true / false)
     */
    boolean isPlayRegistered(Identifier channel);

    /**
     * Sets your HANDLER as registered.
     * @param channel (Your Channel ID)
     */
    default void setPlayRegistered(Identifier channel) {}

    /**
     * Send your HANDLER a global reset() event, such as when the server is shutting down.
     * @param channel (Your Channel ID)
     */
    default void reset(Identifier channel) {}

    /**
     * Register your Payload with Fabric API.
     * This is called immediately upon HANDLER registration.
     * See the fabric-networking-api-v1 Java Docs under PayloadTypeRegistry -> register()
     * for more information on how to do this.
     * -
     * @param channel (Your Channel ID)
     */
    default void registerPlayPayload(Identifier channel) {}

    /**
     * Register your Packet Receiver function.
     * You can use the HANDLER itself (Singleton method), or any other class that you choose.
     * See the fabric-network-api-v1 Java Docs under ServerPlayNetworking.registerGlobalReceiver()
     * for more information on how to do this.
     * -
     * @param channel (Your Channel ID)
     */
    default void registerPlayHandler(Identifier channel) {}

    /**
     * Unregisters your Packet Receiver function.
     * You can use the HANDLER itself (Singleton method), or any other class that you choose.
     * See the fabric-network-api-v1 Java Docs under ServerPlayNetworking.unregisterGlobalReceiver()
     * for more information on how to do this.
     * -
     * @param channel (Your Channel ID)
     */
    default void unregisterPlayHandler(Identifier channel) {}

    /**
     * Receive Payload by pointing static receive() method to this to convert Payload to its data decode() function.
     * -
     * @param payload (Payload to decode)
     * @param ctx (Fabric Context)
     * @param <P> (Payload Param)
     */
    default <P extends CustomPayload> void receivePlayPayload(P payload, ServerPlayNetworking.Context ctx) {}

    /**
     * Receive Payload via the legacy "onCustomPayload" from a Network Handler Mixin interface.
     * -
     * @param payload (Payload to decode)
     * @param handler (Network Handler that received the data)
     * @param ci (Callbackinfo for sending ci.cancel(), if wanted)
     * @param <P> (Payload Param)
     */
    default <P extends CustomPayload> void receivePlayPayload(P payload, ServerPlayNetworkHandler handler, CallbackInfo ci) {}

    /**
     * Payload Decoder wrapper function.
     * Implements how the data is processed after being decoded from the receivePayload().
     * You can ignore these and implement your own helper class/methods.
     * These are provided as an example, and can be used in your HANDLER directly.
     * -
     * @param channel (Channel)
     * @param player (Player received from)
     * @param data (Data Codec)
     */
    default void decodeNbtCompound(Identifier channel, ServerPlayerEntity player, NbtCompound data) {}
    default void decodeByteBuf(Identifier channel, ServerPlayerEntity player, ServuxBuf data) {}
    default void decodeObjects(Identifier channel, ServerPlayerEntity player, Object... args) {}

    /**
     * Payload Encoder wrapper function.
     * Implements how to encode() your Payload, then forward complete Payload to sendPayload().
     * -
     * @param player (Player to send the data to)
     * @param data (Data Codec)
     */
    default void encodeNbtCompound(ServerPlayerEntity player, NbtCompound data) {}
    default void encodeByteBuf(ServerPlayerEntity player, ServuxBuf data) {}
    default void encodeObjects(ServerPlayerEntity player, Object... args) {}

    /**
     * Sends the Payload to the player using the Fabric-API interface.
     * -
     * @param player (Player to send the data to)
     * @param payload (The Payload to send)
     * @param <P> (Payload Param)
     */
    default <P extends CustomPayload> void sendPlayPayload(ServerPlayerEntity player, P payload)
    {
        if (payload.getId().id().equals(this.getPayloadChannel()) && this.isPlayRegistered(this.getPayloadChannel()) &&
                ServerPlayNetworking.canSend(player, payload.getId()))
        {
            ServerPlayNetworking.send(player, payload);
        }
    }

    /**
     * Sends the Payload to the player using the ServerPlayNetworkHandler interface.
     * @param handler (ServerPlayNetworkHandler)
     * @param payload (The Payload to send)
     * @param <P> (Payload Param)
     */
    default <P extends CustomPayload> void sendPlayPayload(ServerPlayNetworkHandler handler, P payload)
    {
        if (payload.getId().id().equals(this.getPayloadChannel()) && this.isPlayRegistered(this.getPayloadChannel()))
        {
            Packet<?> packet = new CustomPayloadS2CPacket(payload);

            if (handler != null && handler.accepts(packet))
            {
                handler.sendPacket(packet);
            }
        }
    }
}
