package fi.dy.masa.servux.network.packet;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.interfaces.IServuxPayloadListener;
import fi.dy.masa.servux.network.ServerNetworkPlayHandler;
import fi.dy.masa.servux.network.payload.ServuxPayload;
import fi.dy.masa.servux.util.PayloadUtils;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ServuxPayloadListener implements IServuxPayloadListener
{
    /**
     * StructureDataPacketHandler (Replaced), etc. using new networking API, and greatly simplifies the work flow.
     * --------------------------------------------------------------------------------------------------------------------
     * In here, We don't need to encapsulate Identifiers either, but I'm only passing it along for the DataProvider Code.
     * It's the same channel ID, and the same HashCode() for all players being utilized by ServuxPayload.  The Server
     * only cares what player you want to send a packet to, with what Payload type.  Also, unless there is a magical ton
     * of structure data, PacketSplitter will never be used.  Also, Carpet no longer provides a "Structures" packet...
     * So, let's get this done.
     */

    @Override
    public void sendServuxPayload(NbtCompound data, ServerPlayerEntity player)
    {
        ServuxPayload payload = new ServuxPayload(data);
        Servux.printDebug("ServuxPayloadListener#sendServuxPayload(): sending payload of size {} bytes to player: {}.", data.getSizeInBytes(), player.getName().getLiteralString());
        ServerNetworkPlayHandler.sendServUX(payload, player);
    }
    @Override
    public void receiveServuxPayload(NbtCompound data, ServerPlayNetworking.Context ctx, Identifier id)
    {
        PacketByteBuf buf = PayloadUtils.fromNbt(data, ServuxPayload.KEY);
        decodeServuxPayload(buf, ctx.player(), id);
    }
    // *****************************************************************************************************************************************
    @Override
    public void encodeServuxPayload(PacketByteBuf packet, ServerPlayerEntity player, Identifier id)
    {
        // Encode packet.
        NbtCompound nbt = new NbtCompound();
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        nbt.putByteArray(ServuxPayload.KEY, packet.readByteArray());
        nbt.putString("id", id.toString());
        Servux.printDebug("ServuxPayloadListener#encodeServuxPayload(): nbt.putByteArray() size in bytes: {}", nbt.getSizeInBytes());
        sendServuxPayload(nbt, player);
    }

    @Override
    public void encodeServuxPayloadWithType(int packetType, NbtCompound data, ServerPlayerEntity player)
    {
        // Encode packet.
        NbtCompound nbt = new NbtCompound();
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeNbt(data);
        Servux.printDebug("ServuxPayloadListener#encodeServuxPayloadWithType(): buf.writeNbt() size in bytes: {}", buf.readableBytes());
        nbt.putInt("packetType", packetType);
        Servux.printDebug("ServuxPayloadListener#encodeServuxPayloadWithType(): nbt.putInt() size in bytes: {}", nbt.getSizeInBytes());
        nbt.putByteArray(ServuxPayload.KEY, buf.readByteArray());
        Servux.printDebug("ServuxPayloadListener#encodeServuxPayloadWithType(): nbt.putByteArray() size in bytes: {}", nbt.getSizeInBytes());
        sendServuxPayload(nbt, player);
    }
    @Override
    public void decodeServuxPayload(PacketByteBuf packet, ServerPlayerEntity player, Identifier id)
    {
        Servux.printDebug("ServuxPayloadListener#decodeServuxPayload(): received unhandled packet from player: {}, of size in bytes: {}.", player.getName(), packet.readableBytes());
    }
}
