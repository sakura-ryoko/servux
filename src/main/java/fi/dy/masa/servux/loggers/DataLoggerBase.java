package fi.dy.masa.servux.loggers;

import net.minecraft.server.MinecraftServer;

public abstract class DataLoggerBase<T>
{
    private final DataLogger type;

    public DataLoggerBase(DataLogger type)
    {
        this.type = type;
    }

    public DataLogger getType()
    {
        return this.type;
    }

    public abstract T getResult(MinecraftServer server);
}
