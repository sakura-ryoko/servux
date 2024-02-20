package fi.dy.masa.servux.dataproviders.data;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.DataProviderBase;
import fi.dy.masa.servux.dataproviders.client.EntitiesClient;
import fi.dy.masa.servux.network.packet.PacketType;
import fi.dy.masa.servux.network.payload.channel.ServuxS2CEntitiesPayload;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

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
    private static Identifier getChannel() { return ServuxS2CEntitiesPayload.TYPE.id(); }
    protected final Map<UUID, EntitiesClient> CLIENTS = new HashMap<>();
    protected final NbtCompound metadata = new NbtCompound();
    protected EntitiesDataProvider()
    {
        super(
                "entity_data_provider",
                PacketType.Entities.PROTOCOL_VERSION,
                "Alpha interface for providing Entity Metadata backend for various mods.");
        this.metadata.putString("id", this.getNetworkChannel());
        this.metadata.putInt("version", PacketType.Entities.PROTOCOL_VERSION);
    }

    //@Override
    public String getNetworkChannel() { return getChannel().toString(); }

    public void register(ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        EntitiesClient newClient = new EntitiesClient(player.getName().getLiteralString(), uuid, null);
        newClient.registerClient(player);
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
