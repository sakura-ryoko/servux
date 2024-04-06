package fi.dy.masa.servux.dataproviders.data;

import com.mojang.authlib.GameProfile;
import fi.dy.masa.malilib.network.payload.PayloadType;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.ServuxReference;
import fi.dy.masa.servux.dataproviders.DataProviderBase;
import fi.dy.masa.servux.dataproviders.client.BlocksClient;
import fi.dy.masa.servux.network.PacketType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This provides the basic foundations for a Block Metadata Interface for various things that's not structure related,
 * such as inventory and other various available Block NBT Data
 */
public class BlocksDataProvider extends DataProviderBase
{
    public static final BlocksDataProvider INSTANCE = new BlocksDataProvider();
    protected final Map<UUID, BlocksClient> CLIENTS = new HashMap<>();
    protected final NbtCompound metadata = new NbtCompound();
    protected final int protocolVersion = PacketType.Structures.PROTOCOL_VERSION;

    protected BlocksDataProvider()
    {
        super(
                "block_data_provider",
                PacketType.Blocks.PROTOCOL_VERSION,
                "Alpha interface for providing Block Metadata for various mods.");

        this.metadata.putString("id", this.getNetworkChannel().toString());
        this.metadata.putInt("version", this.protocolVersion);
        this.metadata.putString("servux", ServuxReference.MOD_STRING);
    }

    @Override
    public PayloadType getNetworkChannel() { return PayloadType.SERVUX_BLOCKS; }

    @Override
    public int getProtocolVersion() { return this.protocolVersion; }

    public void register(SocketAddress addr, GameProfile profile, ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        BlocksClient newClient = new BlocksClient(player.getName().getLiteralString(), uuid, null);
        newClient.registerClient(addr, profile, player);
        newClient.blocksEnableClient();
        CLIENTS.put(uuid, newClient);
        Servux.printDebug("BlocksDataProvider#register(): new BlocksClient register() for {}", player.getName().getLiteralString());
    }
    public void unregister(ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        BlocksClient oldClient = CLIENTS.get(uuid);
        oldClient.blocksDisableClient();
        oldClient.unregisterClient();
        CLIENTS.remove(uuid);
        Servux.printDebug("BlocksDataProvider#register(): new BlocksClient unregister() for {}", player.getName().getLiteralString());
    }
    public void splitPacketType(int packetType, NbtCompound data, ServerPlayerEntity player)
    {
        // NO-OP
    }
}
