package fi.dy.masa.servux.network.payload;

public enum PayloadType
{
    SERVUX_BLOCKS,
    SERVUX_BYTEBUF,
    SERVUX_ENTITIES,
    SERVUX_LITEMATICS,
    SERVUX_METADATA,
    SERVUX_STRUCTURES;
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
