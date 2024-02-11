package fi.dy.masa.servux.deprecated;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.payload.channel.ServuxMetadataPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

@Deprecated
public class ServuxMetadataListener
//implements IServuxMetadataListener
{
    //@Override
    public void reset()
    {
        // NO-OP
    }
    //@Override
    public void sendServuxMetadata(NbtCompound data, ServerPlayerEntity player)
    {
        ServuxMetadataPayload payload = new ServuxMetadataPayload(data);
        Servux.printDebug("ServuxMetadataListener#sendServuxMetadata(): sending payload of size {} bytes to player: {}.", data.getSizeInBytes(), player.getName().getLiteralString());
        ServerNetworkPlayHandler.sendServuxMetadata(payload, player);
    }
    //@Override
    public void receiveServuxMetadata(NbtCompound data, ServerPlayNetworking.Context ctx, Identifier id)
    {
        decodeServuxMetadata(data, ctx.player(), id);
    }
    // *****************************************************************************************************************************************
    //@Override
    public void encodeServuxMetadata(NbtCompound data, ServerPlayerEntity player, Identifier id)
    {
        // Encode packet.
        NbtCompound nbt = new NbtCompound();
        nbt.copyFrom(data);
        nbt.putString("id", id.toString());
        Servux.printDebug("ServuxMetadataListener#encodeServuxMetadata(): nbt.putByteArray() size in bytes: {}", nbt.getSizeInBytes());
        sendServuxMetadata(nbt, player);
    }

    //@Override
    public void decodeServuxMetadata(NbtCompound data, ServerPlayerEntity player, Identifier id) {
        // Packet handshakes from Client
//        if (Objects.equals(id.toString(), StructureDataProvider.INSTANCE.getNetworkChannel())) {
//            int packetType = data.getInt("packetType");
//        }
        Servux.printDebug("ServuxMetadataListener#decodeServuxMetadata(): received unhandled packet from player: {}, of size in bytes: {}.", player.getName().getLiteralString(), data.getSizeInBytes());
    }
}
