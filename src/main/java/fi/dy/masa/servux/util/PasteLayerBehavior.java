package fi.dy.masa.servux.util;

import com.google.common.collect.ImmutableList;

import net.minecraft.util.StringIdentifiable;

public enum PasteLayerBehavior implements StringIdentifiable
{
    ALL             ("all",             "litematica.gui.label.paste_layer_behavior.all"),
    RENDERED_ONLY   ("rendered_only",   "litematica.gui.label.paste_layer_behavior.rendered_only");

    public static final StringIdentifiable.EnumCodec<PasteLayerBehavior> CODEC = StringIdentifiable.createCodec(PasteLayerBehavior::values);
    public static final ImmutableList<PasteLayerBehavior> VALUES = ImmutableList.copyOf(values());
    private final String configString;
    private final String translationKey;

    PasteLayerBehavior(String configString, String translationKey)
    {
        this.configString = configString;
        this.translationKey = translationKey;
    }

    @Override
    public String asString()
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
