package fi.dy.masa.servux.network.packet;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.interfaces.ISyncmaticaPayloadListener;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;

public class SyncmaticaPayloadListener implements ISyncmaticaPayloadListener
{
    public void sendSyncmaticaPayload(NbtCompound data)
    {
        Servux.printDebug("SyncmaticaPayloadListener#sendSyncmaticaPayload(): sending payload of size: {}", data.getSizeInBytes());
    }
    public void receiveSyncmaticaPayload(NbtCompound data, ClientPlayNetworking.Context ctx)
    {
        Servux.printDebug("SyncmaticaPayloadListener#receiveSyncmaticaPayload(): receiving payload of size: {}", data.getSizeInBytes());
    }

    public void encodeSyncmaticaPayload(NbtCompound data)
    {
        // Client->Server (C2S) encoder
        Servux.printDebug("SyncmaticaPayloadListener#encodeSyncmaticaPayload(): encoding payload of size: {}", data.getSizeInBytes());
    }

    public void decodeSyncmaticaPayload(NbtCompound data)
    {
        // Server->Client (S2C) decoder
        Servux.printDebug("SyncmaticaPayloadListener#decodeSyncmaticaPayload(): decoding payload of size: {}", data.getSizeInBytes());
    }
}
