package fi.dy.masa.servux.dataproviders.data;

import com.mojang.authlib.GameProfile;
import fi.dy.masa.malilib.network.payload.PayloadType;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.ServuxReference;
import fi.dy.masa.servux.dataproviders.DataProviderBase;
import fi.dy.masa.servux.dataproviders.client.EntitiesClient;
import fi.dy.masa.servux.network.PacketType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This provides the basic foundations for an Entity Metadata Interface for various things that's not structure related,
 * such as Horse/Villager Health and various Entities NBT Data
 */
public class EntitiesDataProvider extends DataProviderBase
{
    public static final EntitiesDataProvider INSTANCE = new EntitiesDataProvider();
    protected final Map<UUID, EntitiesClient> CLIENTS = new HashMap<>();
    protected final NbtCompound metadata = new NbtCompound();
    protected final int protocolVersion = PacketType.Entities.PROTOCOL_VERSION;

    protected EntitiesDataProvider()
    {
        super(
                "entity_data_provider",
                PacketType.Entities.PROTOCOL_VERSION,
                "Alpha interface for providing Entity Metadata backend for various mods.");

        this.metadata.putString("id", this.getNetworkChannel().toString());
        this.metadata.putInt("version", this.protocolVersion);
        this.metadata.putString("servux", ServuxReference.MOD_STRING);
    }

    @Override
    public PayloadType getNetworkChannel() { return PayloadType.SERVUX_ENTITIES; }

    @Override
    public int getProtocolVersion() { return this.protocolVersion; }

    public void register(SocketAddress addr, GameProfile profile, ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        EntitiesClient newClient = new EntitiesClient(player.getName().getLiteralString(), uuid, null);
        newClient.registerClient(addr, profile, player);
        newClient.entitiesEnableClient();
        CLIENTS.put(uuid, newClient);
        Servux.printDebug("EntitiesDataProvider#register(): new MetadataClient register() for {}", player.getName().getLiteralString());
    }
    public void unregister(ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        EntitiesClient oldClient = CLIENTS.get(uuid);
        oldClient.entitiesDisableClient();
        oldClient.unregisterClient();
        CLIENTS.remove(uuid);
        Servux.printDebug("EntitiesDataProvider#register(): new MetadataClient unregister() for {}", player.getName().getLiteralString());
    }
    public void splitPacketType(int packetType, NbtCompound data, ServerPlayerEntity player)
    {
        // NO-OP
    }
}
