package fi.dy.masa.servux.dataproviders;

import net.minecraft.util.Identifier;

public abstract class DataProviderBase implements IDataProvider
{
    protected final Identifier networkChannel;
    protected final String name;
    protected final String description;
    protected final int protocolVersion;
    protected boolean enabled;
    private int tickRate = 40;

    protected DataProviderBase(String name, Identifier channel, int protocolVersion, String description)
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
    public Identifier getNetworkChannel()
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
}
