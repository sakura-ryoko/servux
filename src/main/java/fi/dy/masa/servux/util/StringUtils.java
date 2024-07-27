package fi.dy.masa.servux.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class StringUtils
{
    public static String getModVersionString(String modId)
    {
        for (net.fabricmc.loader.api.ModContainer container : net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods())
        {
            if (container.getMetadata().getId().equals(modId))
            {
                return container.getMetadata().getVersion().getFriendlyString();
            }
        }

        return "?";
    }

    public static String removeDefaultMinecraftNamespace(Identifier settingId)
    {
        return settingId.getNamespace().equals("minecraft") ? settingId.getPath() : settingId.toString();
    }

    /**
     * Can replace I18n
     * @param translationKey (key)
     * @param args (...args)
     */
    public static MutableText translate(String translationKey, Object... args)
    {
        return i18nLang.getInstance().translate(translationKey, args);
    }
}
