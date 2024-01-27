package fi.dy.masa.servux.dataproviders;

public abstract class DataProviderBase implements IDataProvider
{
    // Change Identifier --> String, because it might confuse Minecraft
    protected final String networkChannel;
    protected final String name;
    protected final String description;
    protected final int protocolVersion;
    protected boolean enabled;
    private int tickRate = 40;
    private int spawnChunkRadius = -1;

    protected DataProviderBase(String name, String channel, int protocolVersion, String description)
    {
        this.name = name;
        this.networkChannel = channel;
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
    public String getNetworkChannel()
    {
        return this.networkChannel;
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
    public int getSpawnChunkRadius() { return this.spawnChunkRadius; }

    @Override
    public void setSpawnChunkRadius(int radius) { this.spawnChunkRadius = radius; }
}
