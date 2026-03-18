package fi.dy.masa.servux.network;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.servux.Servux;

/**
 * Interface for ServerPlayHandler
 * @param <T> (Payload Param)
 */
public interface IPluginServerPlayHandler<T extends CustomPacketPayload> extends ServerPlayNetworking.PlayPayloadHandler<@NotNull T>
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
    default void registerPlayPayload(@Nonnull CustomPacketPayload.Type<@NotNull T> id, @Nonnull StreamCodec<? super RegistryFriendlyByteBuf, @NotNull T> codec, int direction)
    {
        if (this.isPlayRegistered(this.getPayloadChannel()) == false)
        {
            try
            {
                switch (direction)
                {
                    case TO_SERVER, FROM_CLIENT -> PayloadTypeRegistry.serverboundPlay().register(id, codec);
                    case FROM_SERVER, TO_CLIENT -> PayloadTypeRegistry.clientboundPlay().register(id, codec);
                    default ->
                    {
                        PayloadTypeRegistry.serverboundPlay().register(id, codec);
                        PayloadTypeRegistry.clientboundPlay().register(id, codec);
                    }
                }
            }
            catch (IllegalArgumentException e)
            {
                Servux.LOGGER.error("registerPlayPayload: channel ID [{}] is is already registered", this.getPayloadChannel());
            }

            this.setPlayRegistered(this.getPayloadChannel());
            return;
        }

        Servux.LOGGER.error("registerPlayPayload: channel ID [{}] is invalid, or it is already registered", this.getPayloadChannel());
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
    default boolean registerPlayReceiver(@Nonnull CustomPacketPayload.Type<@NotNull T> id, @Nullable ServerPlayNetworking.PlayPayloadHandler<@NotNull T> receiver)
    {
        if (this.isPlayRegistered(this.getPayloadChannel()))
        {
            try
            {
                return ServerPlayNetworking.registerGlobalReceiver(id, Objects.requireNonNullElse(receiver, this::receivePlayPayload));
            }
            catch (IllegalArgumentException e)
            {
                Servux.LOGGER.error("registerPlayReceiver: Channel ID [{}] payload has not been registered", this.getPayloadChannel());
            }
        }

        Servux.LOGGER.error("registerPlayReceiver: Channel ID [{}] is invalid, or not registered", this.getPayloadChannel());
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
    default void receivePlayPayload(T payload, ServerGamePacketListenerImpl handler, CallbackInfo ci) {}

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
    default void decodeNbtCompound(Identifier channel, ServerPlayer player, CompoundTag data) {}
    default <D> void decodeObject(Identifier channel, ServerPlayer player, D data1) {}
    default <P extends IServerPayloadData> void decodeServerData(Identifier channel, ServerPlayer player, P data) {}

    /**
     * Payload Encoder wrapper function [OPTIONAL]
     * Implements how to encode() your Payload, then forward complete Payload to sendPlayPayload().
     * -
     * @param player (Player to send the data to)
     * @param data (Data Codec)
     */
    default void encodeNbtCompound(ServerPlayer player, CompoundTag data) {}
    default <D> void encodeObject(ServerPlayer player, D data1) {}
    default <P extends IServerPayloadData> void encodeServerData(ServerPlayer player, P data) {}

    /**
     * Used as an iterative "wrapper" for Payload Splitter to send individual Packets
     * @param player (Player to send the packet to)
     * @param buf (Sliced Buffer to send)
     * @param networkHandler (Network Handler as a fail-over option)
     */
    void encodeWithSplitter(ServerPlayer player, FriendlyByteBuf buf, ServerGamePacketListenerImpl networkHandler);

    /**
     * Sends the Payload to the player using the Fabric-API interface.
     * -
     * @param player (Player to send the data to)
     * @param payload (The Payload to send)
     * @return (true/false --> for error control)
     */
    default boolean sendPlayPayload(@Nonnull ServerPlayer player, @Nonnull T payload)
    {
        if (payload.type().id().equals(this.getPayloadChannel()) && this.isPlayRegistered(this.getPayloadChannel()))
        {
            if (ServerPlayNetworking.canSend(player, payload.type()))
            {
                ServerPlayNetworking.send(player, payload);
                return true;
            }
        }
        else
        {
            Servux.LOGGER.warn("sendPlayPayload: [Fabric-API] error sending payload for channel: {}, check if channel is registered", payload.type().id().toString());
        }

        return false;
    }

    /**
     * Sends the Payload to the player using the ServerPlayNetworkHandler interface.
     * @param handler (ServerPlayNetworkHandler)
     * @param payload (The Payload to send)
     * @return (true/false --> for error control)
     */
    default boolean sendPlayPayload(@Nonnull ServerGamePacketListenerImpl handler, @Nonnull T payload)
    {
        if (payload.type().id().equals(this.getPayloadChannel()) && this.isPlayRegistered(this.getPayloadChannel()))
        {
            Packet<?> packet = new ClientboundCustomPayloadPacket(payload);

            if (handler.shouldHandleMessage(packet))
            {
                handler.send(packet);
                return true;
            }
        }
        else
        {
            Servux.LOGGER.warn("sendPlayPayload: [NetworkHandler] error sending payload for channel: {}, check if channel is registered", payload.type().id().toString());
        }

        return false;
    }
}
