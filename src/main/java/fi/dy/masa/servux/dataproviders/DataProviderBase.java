package fi.dy.masa.servux.dataproviders;

import fi.dy.masa.malilib.network.payload.PayloadType;

public abstract class DataProviderBase implements IDataProvider
{
    protected final String name;
    protected final PayloadType type;
    protected final String description;
    protected final int protocolVersion;
    protected boolean enabled;
    private int tickRate = 40;

    protected DataProviderBase(String name, PayloadType type, int protocolVersion, String description)
    {
        this.name = name;
        this.type = type;
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
    public PayloadType getNetworkChannel() { return this.type; }

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
