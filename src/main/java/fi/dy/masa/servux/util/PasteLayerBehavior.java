package fi.dy.masa.servux.util;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import net.minecraft.util.StringRepresentable;

public enum PasteLayerBehavior implements StringRepresentable
{
    ALL             ("all"),
    RENDERED_ONLY   ("rendered_only");

    public static final EnumCodec<@NotNull PasteLayerBehavior> CODEC = StringRepresentable.fromEnum(PasteLayerBehavior::values);
    public static final ImmutableList<@NotNull PasteLayerBehavior> VALUES = ImmutableList.copyOf(values());
    private final String configString;

    PasteLayerBehavior(String configString)
    {
        this.configString = configString;
    }

    @Override
    public @NotNull String getSerializedName()
    {
        return this.configString;
    }

    public static PasteLayerBehavior fromStringStatic(String name)
    {
        for (PasteLayerBehavior val : PasteLayerBehavior.values())
        {
            if (val.configString.equalsIgnoreCase(name))
            {
                return val;
            }
        }

        return PasteLayerBehavior.ALL;
    }
}
