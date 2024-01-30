package fi.dy.masa.servux.network.packet;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.interfaces.ISyncmaticaPayloadServerListener;
import fi.dy.masa.servux.network.ServerNetworkPlayHandler;
import fi.dy.masa.servux.network.payload.SyncmaticaPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class SyncmaticaPayloadServerListener implements ISyncmaticaPayloadServerListener
{
    @Override
    public void sendSyncmaticaServerPayload(NbtCompound data, ServerPlayerEntity player)
    {
        Servux.printDebug("SyncmaticaPayloadServerListener#sendSyncmaticaServerPayload(): sending payload of size: {}", data.getSizeInBytes());
        SyncmaticaPayload payload = new SyncmaticaPayload(data);
        ServerNetworkPlayHandler.sendSyncmaticaServerPayload(payload, player);
    }
    @Override
    public void receiveSyncmaticaServerPayload(NbtCompound data, ServerPlayNetworking.Context ctx, Identifier id)
    {
        decodeSyncmaticaServerPayload(data, ctx.player(), id);
    }

    @Override
    public void encodeSyncmaticaServerPayload(NbtCompound data, ServerPlayerEntity player, Identifier id)
    {
        // Client->Server (C2S) encoder
        NbtCompound nbt = new NbtCompound();
        nbt.copyFrom(data);
        Servux.printDebug("SyncmaticaPayloadServerListener#encodeSyncmaticaServerPayload(): encoding payload of size: {}", data.getSizeInBytes());
        sendSyncmaticaServerPayload(nbt, player);
    }

    @Override
    public void decodeSyncmaticaServerPayload(NbtCompound data, ServerPlayerEntity player, Identifier id)
    {
        // Server->Client (S2C) decoder
        Servux.printDebug("SyncmaticaPayloadServerListener#decodeSyncmaticaServerPayload(): decoding payload of size: {}", data.getSizeInBytes());
        String hello = data.getString("hello");
        Servux.printDebug("SyncmaticaPayloadServerListener#decodeSyncmaticaServerPayload(): id: {}, received: {}", id, hello);
    }
}
