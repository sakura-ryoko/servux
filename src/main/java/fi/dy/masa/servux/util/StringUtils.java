package fi.dy.masa.servux.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;

import fi.dy.masa.servux.dataproviders.ServuxConfigProvider;

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

    public static String translateAsString(String translationKey, Object... args)
    {
//        return i18nLang.getInstance().translateAsString(translationKey, args);
        if (ServuxConfigProvider.LANG != null)
        {
            return ServuxConfigProvider.LANG.translate(translationKey, args);
        }

        throw new IllegalStateException("LANG Manager is null");
    }

    /**
     * Can replace I18n
     * @param translationKey (key)
     * @param args (...args)
     */
    public static MutableText translate(String translationKey, Object... args)
    {
//        return i18nLang.getInstance().translate(translationKey, args);
        if (ServuxConfigProvider.LANG != null)
        {
            return ServuxConfigProvider.LANG.translateAsText(translationKey, args);
        }

        throw new IllegalStateException("LANG Manager is null");
    }

    public static CommandSyntaxException translateError(String translationKey, Object... args)
    {
        return new SimpleCommandExceptionType(translate(translationKey, args)).create();
    }
}
