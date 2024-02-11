package fi.dy.masa.servux.deprecated;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.payload.channel.ServuxLitematicsPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

@Deprecated
public class ServuxLitematicsListener
        //implements IServuxLitematicsListener
{
    //@Override
    public void reset()
    {
        // NO-OP
    }
    //@Override
    public void sendServuxLitematics(NbtCompound data, ServerPlayerEntity player)
    {
        ServuxLitematicsPayload payload = new ServuxLitematicsPayload(data);
        Servux.printDebug("ServuxLitematicsListener#sendServuxLitematics(): sending payload of size {} bytes to player: {}.", data.getSizeInBytes(), player.getName().getLiteralString());
        ServerNetworkPlayHandler.sendServuxLitematics(payload, player);
    }
    //@Override
    public void receiveServuxLitematics(NbtCompound data, ServerPlayNetworking.Context ctx, Identifier id)
    {
        decodeServuxLitematics(data, ctx.player(), id);
    }
    // *****************************************************************************************************************************************
    //@Override
    public void encodeServuxLitematics(NbtCompound data, ServerPlayerEntity player, Identifier id)
    {
        // Encode packet.
        NbtCompound nbt = new NbtCompound();
        nbt.copyFrom(data);
        nbt.putString("id", id.toString());
        Servux.printDebug("ServuxLitematicsListener#encodeServuxLitematics(): nbt.putByteArray() size in bytes: {}", nbt.getSizeInBytes());
        sendServuxLitematics(nbt, player);
    }

    //@Override
    public void decodeServuxLitematics(NbtCompound data, ServerPlayerEntity player, Identifier id) {
        // Packet handshakes from Client
//        if (Objects.equals(id.toString(), StructureDataProvider.INSTANCE.getNetworkChannel())) {
//            int packetType = data.getInt("packetType");
//        }
        Servux.printDebug("ServuxLitematicsListener#decodeServuxLitematics(): received unhandled packet from player: {}, of size in bytes: {}.", player.getName().getLiteralString(), data.getSizeInBytes());
    }
}
