package fi.dy.masa.servux.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import fi.dy.masa.servux.dataproviders.ServuxConfigProvider;

public class StringUtils
{
    public static String getModVersionString(String modId)
    {
        for (ModContainer container : FabricLoader.getInstance().getAllMods())
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
    public static MutableComponent translate(String translationKey, Object... args)
    {
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
