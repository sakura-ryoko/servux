package fi.dy.masa.servux.dataproviders;

import net.minecraft.util.math.BlockPos;

public abstract class DataProviderBase implements IDataProvider
{
    // Change Identifier --> String, because it might confuse Minecraft
    protected final String name;
    protected final String description;
    protected final int protocolVersion;
    protected boolean enabled;
    private int tickRate = 40;
    private BlockPos spawnPos;
    private int spawnChunkRadius = -1;
    private boolean refreshSpawnMetadata;

    protected DataProviderBase(String name, int protocolVersion, String description)
    {
        this.name = name;
        this.protocolVersion = protocolVersion;
        this.description = description;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    @Override
    public int getProtocolVersion()
    {
        return this.protocolVersion;
    }

    @Override
    public boolean isEnabled()
    {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    protected void setTickRate(int tickRate)
    {
        this.tickRate = Math.max(tickRate, 1);
    }

    @Override
    public final int getTickRate()
    {
        return this.tickRate;
    }

    @Override
    public BlockPos getSpawnPos()
    {
        if (this.spawnPos == null)
            this.setSpawnPos(new BlockPos(0, 0,0));
        return this.spawnPos;
    }

    @Override
    public void setSpawnPos(BlockPos spawnPos)
    {
        if (this.spawnPos != spawnPos)
            this.refreshSpawnMetadata = true;
        this.spawnPos = spawnPos;
    }

    @Override
    public int getSpawnChunkRadius()
    {
        if (this.spawnChunkRadius < 0)
            this.spawnChunkRadius = 2;
        return this.spawnChunkRadius;
    }

    @Override
    public void setSpawnChunkRadius(int radius)
    {
        if (this.spawnChunkRadius != radius)
            this.refreshSpawnMetadata = true;
        this.spawnChunkRadius = radius;
    }
    @Override
    public boolean refreshSpawnMetadata() { return this.refreshSpawnMetadata; }
    @Override
    public void setRefreshSpawnMetadataComplete() { this.refreshSpawnMetadata = false; }
}
