package fi.dy.masa.servux.dataproviders;

import java.util.*;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.ServerPlayHandler;
import fi.dy.masa.servux.network.packet.ServuxBlockEntitiesHandler;
import fi.dy.masa.servux.network.packet.ServuxBlockEntitiesPacket;
import fi.dy.masa.servux.util.PlayerDimensionPosition;
import fi.dy.masa.servux.util.Timeout;

public class BlockEntitiesDataProvider extends DataProviderBase
{
    public static final BlockEntitiesDataProvider INSTANCE = new BlockEntitiesDataProvider();

    protected final static ServuxBlockEntitiesHandler<ServuxBlockEntitiesPacket.Payload> HANDLER = ServuxBlockEntitiesHandler.getInstance();
    protected final Map<UUID, PlayerDimensionPosition> registeredPlayers = new HashMap<>();
    protected final Map<UUID, Map<ChunkPos, Timeout>> timeouts = new HashMap<>();
    protected final NbtCompound metadata = new NbtCompound();
    protected int timeout = 30 * 20;
    protected int updateInterval = 40;
    protected int retainDistance;

    protected BlockEntitiesDataProvider()
    {
        super("block_entity_data",
                ServuxBlockEntitiesHandler.CHANNEL_ID,
                ServuxBlockEntitiesPacket.PROTOCOL_VERSION,
                "Block Entity Data provider for Client Side mods.");

        this.metadata.putString("name", this.getName());
        this.metadata.putString("id", this.getNetworkChannel().toString());
        this.metadata.putInt("version", this.getProtocolVersion());
        this.metadata.putString("servux", Reference.MOD_STRING);
        this.metadata.putInt("timeout", this.timeout);
    }

    @Override
    public void registerHandler()
    {
        ServerPlayHandler.getInstance().registerServerPlayHandler(HANDLER);
        HANDLER.registerPlayPayload(ServuxBlockEntitiesPacket.Payload.ID, ServuxBlockEntitiesPacket.Payload.CODEC, IPluginServerPlayHandler.BOTH_SERVER);
        HANDLER.registerPlayReceiver(ServuxBlockEntitiesPacket.Payload.ID, HANDLER::receivePlayPayload);
    }

    @Override
    public void unregisterHandler()
    {
        HANDLER.unregisterPlayReceiver();
        ServerPlayHandler.getInstance().unregisterServerPlayHandler(HANDLER);
    }

    @Override
    public IPluginServerPlayHandler<?> getPacketHandler()
    {
        return HANDLER;
    }

    @Override
    public boolean shouldTick()
    {
        return this.enabled;
    }

    @Override
    public void tick(MinecraftServer server, int tickCounter)
    {
        if ((tickCounter % this.updateInterval) == 0)
        {
            List<ServerPlayerEntity> playerList = server.getPlayerManager().getPlayerList();
            this.retainDistance = server.getPlayerManager().getViewDistance() + 2;

            for (ServerPlayerEntity player : playerList)
            {
                UUID uuid = player.getUuid();

                if (this.registeredPlayers.containsKey(uuid))
                {
                    this.checkForDimensionChange(player);
                    //this.refreshTrackedChunks(player, tickCounter);
                }
            }

            this.checkForInvalidPlayers(server);

        }
    }

    public boolean register(ServerPlayerEntity player)
    {
        // System.out.printf("register\n");
        boolean registered = false;
        MinecraftServer server = player.getServer();
        UUID uuid = player.getUuid();

        if (this.registeredPlayers.containsKey(uuid) == false)
        {
            this.registeredPlayers.put(uuid, new PlayerDimensionPosition(player));
            int tickCounter = server.getTicks();
            ServerPlayNetworkHandler handler = player.networkHandler;

            if (handler != null)
            {
                NbtCompound nbt = new NbtCompound();
                nbt.copyFrom(this.metadata);

                HANDLER.sendPlayPayload(handler, new ServuxBlockEntitiesPacket.Payload(new ServuxBlockEntitiesPacket(ServuxBlockEntitiesPacket.Type.PACKET_S2C_METADATA, nbt)));
                //this.initialSyncStructuresToPlayerWithinRange(player, player.getServer().getPlayerManager().getViewDistance()+2, tickCounter);
                // TODO
            }

            registered = true;
        }

        return registered;
    }

    public void refreshMetadata(ServerPlayerEntity player, NbtCompound tag)
    {
        HANDLER.encodeServerData(player, new ServuxBlockEntitiesPacket(ServuxBlockEntitiesPacket.Type.PACKET_S2C_METADATA, this.metadata));
    }

    public boolean unregister(ServerPlayerEntity player)
    {
        // System.out.printf("unregister\n");
        HANDLER.resetFailures(this.getNetworkChannel(), player);

        return this.registeredPlayers.remove(player.getUuid()) != null;
    }

    public void checkForInvalidPlayers(MinecraftServer server)
    {
        if (!this.registeredPlayers.isEmpty())
        {
            Iterator<UUID> iter = this.registeredPlayers.keySet().iterator();

            while (iter.hasNext())
            {
                UUID uuid = iter.next();

                if (server.getPlayerManager().getPlayer(uuid) == null)
                {
                    this.timeouts.remove(uuid);
                    iter.remove();
                }
            }
        }
    }

    protected void addChunkTimeoutIfHasReferences(final UUID uuid, WorldChunk chunk, final int tickCounter)
    {
        final ChunkPos pos = chunk.getPos();

        // TODO
        /*
        if (this.chunkHasStructureReferences(pos.x, pos.z, chunk.getWorld()))
        {
            final Map<ChunkPos, Timeout> map = this.timeouts.computeIfAbsent(uuid, (u) -> new HashMap<>());
            final int timeout = this.timeout;

            //System.out.printf("addChunkTimeoutIfHasReferences: %s\n", pos);
            // Set the timeout so it's already expired and will cause the chunk to be sent on the next update tick
            map.computeIfAbsent(pos, (p) -> new Timeout(tickCounter - timeout));
        }
         */
    }

    protected void checkForDimensionChange(ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        PlayerDimensionPosition playerPos = this.registeredPlayers.get(uuid);

        if (playerPos == null || playerPos.dimensionChanged(player))
        {
            this.timeouts.remove(uuid);
            this.registeredPlayers.computeIfAbsent(uuid, (u) -> new PlayerDimensionPosition(player)).setPosition(player);
        }
    }

    // FIXME --> for references data
    protected void addOrRefreshTimeouts(final UUID uuid,
                                        final Map<BlockEntity, LongSet> references,
                                        final int tickCounter)
    {
        // System.out.printf("addOrRefreshTimeouts: references: %d\n", references.size());
        Map<ChunkPos, Timeout> map = this.timeouts.computeIfAbsent(uuid, (u) -> new HashMap<>());

        for (LongSet chunks : references.values())
        {
            for (Long chunkPosLong : chunks)
            {
                final ChunkPos pos = new ChunkPos(chunkPosLong);
                map.computeIfAbsent(pos, (p) -> new Timeout(tickCounter)).setLastSync(tickCounter);
            }
        }
    }

    protected void refreshTrackedChunks(ServerPlayerEntity player, int tickCounter)
    {
        UUID uuid = player.getUuid();
        Map<ChunkPos, Timeout> map = this.timeouts.get(uuid);

        if (map != null)
        {
            // System.out.printf("refreshTrackedChunks: timeouts: %d\n", map.size());
            //this.sendAndRefreshExpiredStructures(player, map, tickCounter);
            // TODO
        }
    }

    protected boolean isOutOfRange(ChunkPos pos, ChunkPos center)
    {
        int chunkRadius = this.retainDistance;

        return Math.abs(pos.x - center.x) > chunkRadius ||
                Math.abs(pos.z - center.z) > chunkRadius;
    }

    // TODO --> Add ways to obtain the BlockEntity surrounding a player
}
