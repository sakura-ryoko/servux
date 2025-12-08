package fi.dy.masa.servux.util;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import net.minecraft.util.StringRepresentable;

public enum PasteLayerBehavior implements StringRepresentable
{
    ALL             ("all",             "litematica.gui.label.paste_layer_behavior.all"),
    RENDERED_ONLY   ("rendered_only",   "litematica.gui.label.paste_layer_behavior.rendered_only");

    public static final EnumCodec<@NotNull PasteLayerBehavior> CODEC = StringRepresentable.fromEnum(PasteLayerBehavior::values);
    public static final ImmutableList<@NotNull PasteLayerBehavior> VALUES = ImmutableList.copyOf(values());
    private final String configString;
    private final String translationKey;

    PasteLayerBehavior(String configString, String translationKey)
    {
        this.configString = configString;
        this.translationKey = translationKey;
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
