package fi.dy.masa.servux.loggers;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

import javax.annotation.Nullable;

public enum DataLogger implements StringIdentifiable
{
    TPS             ("tps",             DataLoggerType.TPS, DataLoggerTPS.CODEC),
    MOB_CAPS        ("mob_caps",        DataLoggerType.MOB_CAPS, DataLoggerMobCaps.CODEC)
    ;

    public static final EnumCodec<DataLogger> CODEC = StringIdentifiable.createCodec(DataLogger::values);
    public static final ImmutableList<DataLogger> VALUES = ImmutableList.copyOf(values());

    private final String name;
    private final DataLoggerType<?> type;
    private final Codec<?> codec;

    DataLogger(String name, DataLoggerType<?> type, Codec<?> codec)
    {
        this.name = name;
        this.type = type;
        this.codec = codec;
    }

    @Override
    public String asString()
    {
        return this.name;
    }

    public @Nullable DataLoggerBase<?> init()
    {
        return this.type.init(this);
    }

    public Codec<?> codec()
    {
        return this.codec;
    }

    public static @Nullable DataLogger fromStringStatic(String name)
    {
        for (DataLogger type : VALUES)
        {
            if (type.name.equalsIgnoreCase(name))
            {
                return type;
            }
        }

        return null;
    }
}
