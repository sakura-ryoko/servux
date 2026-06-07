package fi.dy.masa.servux.util;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import net.minecraft.util.StringRepresentable;

public enum ReplaceBehavior implements StringRepresentable
{
    NONE            ("none"),
    ALL             ("all"),
    WITH_NON_AIR    ("with_non_air");

    public static final StringRepresentable.EnumCodec<@NotNull ReplaceBehavior> CODEC = StringRepresentable.fromEnum(ReplaceBehavior::values);
    public static final ImmutableList<@NotNull ReplaceBehavior> VALUES = ImmutableList.copyOf(values());
    private final String configString;

    ReplaceBehavior(String configString)
    {
        this.configString = configString;
    }

    public static ReplaceBehavior fromStringStatic(String name)
    {
        for (ReplaceBehavior val : VALUES)
        {
            if (val.configString.equalsIgnoreCase(name))
            {
                return val;
            }
        }

        return ReplaceBehavior.NONE;
    }

    @Override
    public @NotNull String getSerializedName()
    {
        return this.configString;
    }
}
