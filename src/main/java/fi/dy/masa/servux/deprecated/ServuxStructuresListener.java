package fi.dy.masa.servux.deprecated;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;
import fi.dy.masa.servux.network.packet.PacketType;
import fi.dy.masa.servux.network.payload.channel.ServuxStructuresPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Objects;

@Deprecated
public class ServuxStructuresListener
        //implements IServuxStructuresListener
{
    /**
     * StructureDataPacketHandler (Replaced), etc. using new networking API, and greatly simplifies the work flow.
     * --------------------------------------------------------------------------------------------------------------------
     * In here, We don't need to encapsulate Identifiers either, but I'm only passing it along for the DataProvider Code.
     * It's the same channel ID, and the same HashCode() for all players being utilized by ServuxStructures.  The Server
     * only cares what player you want to send a packet to, with what Payload type.  Also, unless there is a magical ton
     * of structure data, PacketSplitter will never be used.  Also, Carpet no longer provides a "Structures" packet...
     * So, let's get this done.
     */

    //@Override
    public void reset()
    {
        // NO-OP
    }
    //@Override
    public void sendServuxStructures(NbtCompound data, ServerPlayerEntity player)
    {
        ServuxStructuresPayload payload = new ServuxStructuresPayload(data);
        Servux.printDebug("ServuxStructuresListener#sendServuxStructures(): sending payload of size {} bytes to player: {}.", data.getSizeInBytes(), player.getName().getLiteralString());
        //ServerNetworkPlayHandler.sendServuxStructures(payload, player);
    }
    //@Override
    public void receiveServuxStructures(NbtCompound data, ServerPlayNetworking.Context ctx, Identifier id)
    {
        decodeServuxStructures(data, ctx.player(), id);
    }
    // *****************************************************************************************************************************************
    //@Override
    public void encodeServuxStructures(NbtCompound data, ServerPlayerEntity player, Identifier id)
    {
        // Encode packet.
        NbtCompound nbt = new NbtCompound();
        nbt.copyFrom(data);
        nbt.putString("id", id.toString());
        Servux.printDebug("ServuxStructuresListener#encodeServuxStructures(): nbt.putByteArray() size in bytes: {}", nbt.getSizeInBytes());
        sendServuxStructures(nbt, player);
    }

    //@Override
    public void decodeServuxStructures(NbtCompound data, ServerPlayerEntity player, Identifier id)
    {
        // Packet handshakes from Client
        if (Objects.equals(id.toString(), StructureDataProvider.INSTANCE.getNetworkChannel()))
        {
            int packetType = data.getInt("packetType");
            if (packetType == PacketType.Structures.PACKET_C2S_REQUEST_METADATA)
            {
                Servux.printDebug("ServuxStructuresListener#decodeServuxStructures(): received a metadata request packet from player: {}.", player.getName().getLiteralString());
                StructureDataProvider.INSTANCE.refreshMetadata(player);
            }
            else if (packetType == PacketType.Structures.PACKET_C2S_REQUEST_SPAWN_METADATA)
            {
                Servux.printDebug("ServuxStructuresListener#decodeServuxStructures(): received a Spawn metadata refresh packet from player: {}.", player.getName().getLiteralString());
                StructureDataProvider.INSTANCE.refreshSpawnMetadata(player);
            }
            else if (packetType == PacketType.Structures.PACKET_C2S_STRUCTURES_ACCEPT)
            {
                Servux.printDebug("ServuxStructuresListener#decodeServuxStructures(): received a STRUCTURES_ACCEPT request from player: {}.", player.getName().getLiteralString());
                StructureDataProvider.INSTANCE.acceptStructuresFromPlayer(player);
            }
            else if (packetType == PacketType.Structures.PACKET_C2S_STRUCTURES_DECLINED)
            {
                Servux.printDebug("ServuxStructuresListener#decodeServuxStructures(): received a STRUCTURES_DECLINED request from player: {}.", player.getName().getLiteralString());
                StructureDataProvider.INSTANCE.declineStructuresFromPlayer(player);
            }
            else
                Servux.printDebug("ServuxStructuresListener#decodeServuxStructures(): Invalid packetType from player: {}, of size in bytes: {}.", player.getName(), data.getSizeInBytes());
        }
        else
            Servux.printDebug("ServuxStructuresListener#decodeServuxStructures(): received unhandled packet from player: {}, of size in bytes: {}.", player.getName(), data.getSizeInBytes());
    }
}
