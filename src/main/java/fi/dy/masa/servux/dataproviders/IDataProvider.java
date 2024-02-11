package fi.dy.masa.servux.dataproviders;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

public interface IDataProvider
{
    /**
     * Returns the simple name for this data provider.
     * This should preferably be a lower case alphanumeric string with no
     * other special characters than '-' and '_'.
     * This name will be used in the enable/disable commands as the argument
     * and also as the config file key/identifier.
     * @return
     */
    String getName();

    /**
     * Returns the description of this data provider.
     * Used in the command to list the available providers and to check the status
     * of a given provider.
     * @return
     */
    String getDescription();

    /**
     * Returns the network channel name used by this data provider to listen
     * for incoming data requests and to respond and send the requested data.
     * @return
     */
    String getNetworkChannel();

    /**
     * Returns the current protocol version this provider supports
     * @return
     */
    int getProtocolVersion();

    /**
     * Returns true if this data provider is currently enabled.
     * @return
     */
    boolean isEnabled();

    /**
     * Enables or disables this data provider
     * @param enabled
     */
    void setEnabled(boolean enabled);

    /**
     * Returns whether or not this data provider should get ticked to periodically send some data,
     * or if it's only listening for incoming requests and responds to them directly.
     * @return
     */
    default boolean shouldTick()
    {
        return false;
    }

    /**
     * Returns the interval in game ticks that this data provider should be ticked at
     * @return
     */
    int getTickRate();

    /**
     * Called at the given tick rate
     * @param server
     * @param tickCounter The current server tick (since last server start)
     */
    default void tick(MinecraftServer server, int tickCounter)
    {
    }
    // FIXME --> should move out of here in the future
    /**
     * Overworld Spawn Pos
     */
    BlockPos getSpawnPos();
    void setSpawnPos(BlockPos spawnPos);

    /**
     * Spawn Chunk Radius
     */
    int getSpawnChunkRadius();
    void setSpawnChunkRadius(int radius);

    /**
     * Logic control to refresh Spawn Metadata
     * @return
     */
    boolean refreshSpawnMetadata();
    void setRefreshSpawnMetadataComplete();
    // TODO
}
