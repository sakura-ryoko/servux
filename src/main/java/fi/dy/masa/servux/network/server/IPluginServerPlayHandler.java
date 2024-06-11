package fi.dy.masa.servux.network.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import fi.dy.masa.servux.Servux;

/**
 * Interface for ServerPlayHandler
 * @param <T> (Payload Param)
 */
public interface IPluginServerPlayHandler<T extends CustomPayload> extends ServerPlayNetworking.PlayPayloadHandler<T>
{
    int FROM_SERVER = 1;
    int TO_SERVER = 2;
    int BOTH_SERVER = 3;
    int TO_CLIENT = 4;
    int FROM_CLIENT = 5;
    int BOTH_CLIENT = 6;

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
    void setPlayRegistered(Identifier channel);

    /**
     * Send your HANDLER a global reset() event, such as when the server is shutting down.
     * @param channel (Your Channel ID)
     */
    void reset(Identifier channel);

    /**
     * Register your Payload with Fabric API.
     * See the fabric-networking-api-v1 Java Docs under PayloadTypeRegistry -> register()
     * for more information on how to do this.
     * -
     * @param direction (Payload Direction)
     * @param id (Your Payload Id<T>)
     * @param codec (Your Payload's CODEC)
     */
    default void registerPlayPayload(@Nonnull CustomPayload.Id<T> id, @Nonnull PacketCodec<? super RegistryByteBuf,T> codec, int direction)
    {
        if (this.isPlayRegistered(this.getPayloadChannel()) == false)
        {
            try
            {
                switch (direction)
                {
                    case TO_SERVER, FROM_CLIENT -> PayloadTypeRegistry.playC2S().register(id, codec);
                    case FROM_SERVER, TO_CLIENT -> PayloadTypeRegistry.playS2C().register(id, codec);
                    default ->
                    {
                        PayloadTypeRegistry.playC2S().register(id, codec);
                        PayloadTypeRegistry.playS2C().register(id, codec);
                    }
                }
            }
            catch (IllegalArgumentException e)
            {
                Servux.logger.error("registerPlayPayload: channel ID [{}] is is already registered", this.getPayloadChannel());
            }

            this.setPlayRegistered(this.getPayloadChannel());
            return;
        }

        Servux.logger.error("registerPlayPayload: channel ID [{}] is invalid, or it is already registered", this.getPayloadChannel());
    }

    /**
     * Register your Packet Receiver function.
     * You can use the HANDLER itself (Singleton method), or any other class that you choose.
     * See the fabric-network-api-v1 Java Docs under ServerPlayNetworking.registerGlobalReceiver()
     * for more information on how to do this.
     * -
     * @param id (Your Payload Id<T>)
     * @param receiver (Your Packet Receiver // if null, uses this::receivePlayPayload)
     * @return (True / False)
     */
    default boolean registerPlayReceiver(@Nonnull CustomPayload.Id<T> id, @Nullable ServerPlayNetworking.PlayPayloadHandler<T> receiver)
    {
        if (this.isPlayRegistered(this.getPayloadChannel()))
        {
            try
            {
                return ServerPlayNetworking.registerGlobalReceiver(id, Objects.requireNonNullElse(receiver, this::receivePlayPayload));
            }
            catch (IllegalArgumentException e)
            {
                Servux.logger.error("registerPlayReceiver: Channel ID [{}] payload has not been registered", this.getPayloadChannel());
            }
        }

        Servux.logger.error("registerPlayReceiver: Channel ID [{}] is invalid, or not registered", this.getPayloadChannel());
        return false;
    }

    /**
     * Unregisters your Packet Receiver function.
     * You can use the HANDLER itself (Singleton method), or any other class that you choose.
     * See the fabric-network-api-v1 Java Docs under ServerPlayNetworking.unregisterGlobalReceiver()
     * for more information on how to do this.
     */
    default void unregisterPlayReceiver()
    {
        ServerPlayNetworking.unregisterGlobalReceiver(this.getPayloadChannel());
    }

    /**
     * Receive Payload by pointing static receive() method to this to convert Payload to its data decode() function.
     * -
     * @param payload (Payload to decode)
     * @param ctx (Fabric Context)
     */
    void receivePlayPayload(T payload, ServerPlayNetworking.Context ctx);

    /**
     * Receive Payload via the legacy "onCustomPayload" from a Network Handler Mixin interface.
     * -
     * @param payload (Payload to decode)
     * @param handler (Network Handler that received the data)
     * @param ci (Callbackinfo for sending ci.cancel(), if wanted)
     */
    default void receivePlayPayload(T payload, ServerPlayNetworkHandler handler, CallbackInfo ci) {}

    /**
     * Payload Decoder wrapper function [OPTIONAL]
     * Implements how the data is processed after being decoded from the receivePlayPayload().
     * You can ignore these and implement your own helper class/methods.
     * These are provided as an example, and can be used in your HANDLER directly.
     * -
     * @param channel (Channel)
     * @param player (Player received from)
     * @param data (Data Codec)
     */
    default void decodeNbtCompound(Identifier channel, ServerPlayerEntity player, NbtCompound data) {}
    default void decodeByteBuf(Identifier channel, ServerPlayerEntity player, ServuxBuf data) {}
    default <D> void decodeObject(Identifier channel, ServerPlayerEntity player, D data1) {}

    /**
     * Payload Encoder wrapper function [OPTIONAL]
     * Implements how to encode() your Payload, then forward complete Payload to sendPlayPayload().
     * -
     * @param player (Player to send the data to)
     * @param data (Data Codec)
     */
    default void encodeNbtCompound(ServerPlayerEntity player, NbtCompound data) {}
    default void encodeByteBuf(ServerPlayerEntity player, ServuxBuf data) {}
    default <D> void encodeObject(ServerPlayerEntity player, D data1) {}

    /**
     * Used as an iterative "wrapper" for Payload Splitter to receive a packet with
     * @param handler (networkHandler only used with Reading Session)
     * @param buf (Buffer to receive)
     */
    default void decodeWithSplitter(ServerPlayNetworkHandler handler, PacketByteBuf buf) {}

    /**
     * Used as an iterative "wrapper" for Payload Splitter to send individual Packets
     * @param player (Player to send the packet to)
     * @param buf (Sliced Buffer to send)
     */
    default void encodeWithSplitter(ServerPlayerEntity player, PacketByteBuf buf) {}

    /**
     * Sends the Payload to the player using the Fabric-API interface.
     * -
     * @param player (Player to send the data to)
     * @param payload (The Payload to send)
     * @return (true/false --> for error control)
     */
    default boolean sendPlayPayload(@Nonnull ServerPlayerEntity player, @Nonnull T payload)
    {
        if (payload.getId().id().equals(this.getPayloadChannel()) && this.isPlayRegistered(this.getPayloadChannel()))
        {
            if (ServerPlayNetworking.canSend(player, payload.getId()))
            {
                ServerPlayNetworking.send(player, payload);
                return true;
            }
        }
        else
        {
            Servux.logger.warn("sendPlayPayload: [Fabric-API] error sending payload for channel: {}, check if channel is registered", payload.getId().id().toString());
        }

        return false;
    }

    /**
     * Sends the Payload to the player using the ServerPlayNetworkHandler interface.
     * @param handler (ServerPlayNetworkHandler)
     * @param payload (The Payload to send)
     * @return (true/false --> for error control)
     */
    default boolean sendPlayPayload(@Nonnull ServerPlayNetworkHandler handler, @Nonnull T payload)
    {
        if (payload.getId().id().equals(this.getPayloadChannel()) && this.isPlayRegistered(this.getPayloadChannel()))
        {
            Packet<?> packet = new CustomPayloadS2CPacket(payload);

            if (handler.accepts(packet))
            {
                handler.sendPacket(packet);
                return true;
            }
        }
        else
        {
            Servux.logger.warn("sendPlayPayload: [NetworkHandler] error sending payload for channel: {}, check if channel is registered", payload.getId().id().toString());
        }

        return false;
    }
}
