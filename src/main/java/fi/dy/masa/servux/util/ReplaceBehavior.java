package fi.dy.masa.servux.util;

import com.google.common.collect.ImmutableList;

import net.minecraft.util.StringIdentifiable;

public enum ReplaceBehavior implements StringIdentifiable
{
    NONE            ("none",            "litematica.gui.label.replace_behavior.none"),
    ALL             ("all",             "litematica.gui.label.replace_behavior.all"),
    WITH_NON_AIR    ("with_non_air",    "litematica.gui.label.replace_behavior.with_non_air");

    public static final StringIdentifiable.EnumCodec<ReplaceBehavior> CODEC = StringIdentifiable.createCodec(ReplaceBehavior::values);
    public static final ImmutableList<ReplaceBehavior> VALUES = ImmutableList.copyOf(values());
    private final String configString;
    private final String translationKey;

    ReplaceBehavior(String configString, String translationKey)
    {
        this.configString = configString;
        this.translationKey = translationKey;
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
    public String asString()
    {
        return this.configString;
    }
}
