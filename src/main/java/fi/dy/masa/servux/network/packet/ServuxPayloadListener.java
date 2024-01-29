package fi.dy.masa.servux.network.packet;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;
import fi.dy.masa.servux.interfaces.IServuxPayloadListener;
import fi.dy.masa.servux.network.ServerNetworkPlayHandler;
import fi.dy.masa.servux.network.payload.ServuxPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Objects;

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
        decodeServuxPayload(data, ctx.player(), id);
    }
    // *****************************************************************************************************************************************
    @Override
    public void encodeServuxPayload(NbtCompound data, ServerPlayerEntity player, Identifier id)
    {
        // Encode packet.
        NbtCompound nbt = new NbtCompound();
        nbt.copyFrom(data);
        nbt.putString("id", id.toString());
        Servux.printDebug("ServuxPayloadListener#encodeServuxPayload(): nbt.putByteArray() size in bytes: {}", nbt.getSizeInBytes());
        sendServuxPayload(nbt, player);
    }

    @Override
    public void decodeServuxPayload(NbtCompound data, ServerPlayerEntity player, Identifier id)
    {
        // Packet handshakes from Client
        if (Objects.equals(id.toString(), StructureDataProvider.INSTANCE.getNetworkChannel()))
        {
            int packetType = data.getInt("packetType");
            if (packetType == ServuxPacketType.PACKET_C2S_REQUEST_METADATA)
            {
                Servux.printDebug("ServuxPayloadListener#decodeServuxPayload(): received a metadata request packet from player: {}.", player.getName().getLiteralString());
                StructureDataProvider.INSTANCE.refreshMetadata(player);
            }
            else if (packetType == ServuxPacketType.PACKET_C2S_REQUEST_SPAWN_METADATA)
            {
                Servux.printDebug("ServuxPayloadListener#decodeServuxPayload(): received a Spawn metadata refresh packet from player: {}.", player.getName().getLiteralString());
                StructureDataProvider.INSTANCE.refreshSpawnMetadata(player);
            }
            else if (packetType == ServuxPacketType.PACKET_C2S_STRUCTURES_ACCEPT)
            {
                Servux.printDebug("ServuxPayloadListener#decodeServuxPayload(): received a STRUCTURES_ACCEPT request from player: {}.", player.getName().getLiteralString());
                StructureDataProvider.INSTANCE.acceptStructuresFromPlayer(player);
            }
            else if (packetType == ServuxPacketType.PACKET_C2S_STRUCTURES_DECLINED)
            {
                Servux.printDebug("ServuxPayloadListener#decodeServuxPayload(): received a STRUCTURES_DECLINED request from player: {}.", player.getName().getLiteralString());
                StructureDataProvider.INSTANCE.declineStructuresFromPlayer(player);
            }
            else
                Servux.printDebug("ServuxPayloadListener#decodeServuxPayload(): Invalid packetType from player: {}, of size in bytes: {}.", player.getName(), data.getSizeInBytes());
        }
        else
            Servux.printDebug("ServuxPayloadListener#decodeServuxPayload(): received unhandled packet from player: {}, of size in bytes: {}.", player.getName(), data.getSizeInBytes());
    }
}
