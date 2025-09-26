package fi.dy.masa.servux.loggers;

import javax.annotation.Nullable;

public class DataLoggerType<T extends DataLoggerBase<?>>
{
    public static final DataLoggerType<DataLoggerTPS> TPS;
    public static final DataLoggerType<DataLoggerMobCaps> MOB_CAPS;

    private final DataLoggerFactory<? extends T> factory;
    private final DataLogger type;

    private static <T extends DataLoggerBase<?>> DataLoggerType<T> create(DataLoggerFactory<? extends T> factory, DataLogger type)
    {
        return new DataLoggerType<>(factory, type);
    }

    private DataLoggerType(DataLoggerFactory<? extends T> factory, DataLogger type)
    {
        this.type = type;
        this.factory = factory;
    }

    @Nullable
    public T init(DataLogger fmt)
    {
        return this.factory.create(fmt);
    }

    public DataLogger getType()
    {
        return this.type;
    }
    
    @FunctionalInterface
    interface DataLoggerFactory<T extends DataLoggerBase<?>>
    {
        T create(DataLogger type);
    }

    static
    {
        TPS = create(DataLoggerTPS::new, DataLogger.TPS);
        MOB_CAPS = create(DataLoggerMobCaps::new, DataLogger.MOB_CAPS);
    }
}
