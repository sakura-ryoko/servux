package fi.dy.masa.servux.loggers;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.util.StringRepresentable;

import fi.dy.masa.servux.loggers.data.MobCapData;
import fi.dy.masa.servux.loggers.data.TPSData;

public enum DataLogger implements StringRepresentable
{
    TPS             ("tps",             DataLoggerType.TPS,         TPSData.CODEC),
    MOB_CAPS        ("mob_caps",        DataLoggerType.MOB_CAPS,    MobCapData.CODEC)
    ;

    public static final EnumCodec<@NotNull DataLogger> CODEC = StringRepresentable.fromEnum(DataLogger::values);
    public static final ImmutableList<@NotNull DataLogger> VALUES = ImmutableList.copyOf(values());

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
    public @NotNull String getSerializedName()
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
