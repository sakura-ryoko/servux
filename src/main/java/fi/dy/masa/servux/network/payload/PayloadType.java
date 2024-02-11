package fi.dy.masa.servux.network.payload;

public enum PayloadType
{
    SERVUX_BYTEBUF,
    SERVUX_STRUCTURES,
    SERVUX_METADATA,
    SERVUX_LITEMATICS;
    public boolean exists(PayloadType type)
    {
        for (final PayloadType p : PayloadType.values())
        {
            if (p == type)
                return true;
        }

        return false;
    }
}
