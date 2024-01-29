package fi.dy.masa.servux.network.packet;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.interfaces.ISyncmaticaPayloadListener;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

public class SyncmaticaPayloadListener implements ISyncmaticaPayloadListener
{
    public void sendSyncmaticaPayload(NbtCompound data, ServerPlayerEntity player)
    {
        Servux.printDebug("SyncmaticaPayloadListener#sendSyncmaticaPayload(): sending payload of size: {}", data.getSizeInBytes());
    }
    public void receiveSyncmaticaPayload(NbtCompound data, ServerPlayNetworking.Context ctx)
    {
        Servux.printDebug("SyncmaticaPayloadListener#receiveSyncmaticaPayload(): receiving payload of size: {}", data.getSizeInBytes());
    }

    public void encodeSyncmaticaPayload(NbtCompound data, ServerPlayerEntity player)
    {
        // Client->Server (C2S) encoder
        Servux.printDebug("SyncmaticaPayloadListener#encodeSyncmaticaPayload(): encoding payload of size: {}", data.getSizeInBytes());
    }

    public void decodeSyncmaticaPayload(NbtCompound data, ServerPlayerEntity player)
    {
        // Server->Client (S2C) decoder
        Servux.printDebug("SyncmaticaPayloadListener#decodeSyncmaticaPayload(): decoding payload of size: {}", data.getSizeInBytes());
    }
}
