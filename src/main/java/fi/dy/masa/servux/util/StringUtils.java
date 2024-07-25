package fi.dy.masa.servux.util;

import javax.annotation.Nullable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class StringUtils
{
    private static volatile i18nLang LANG = i18nLang.getInstance();

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

    public static Text getTranslatedOrFallback(String key, @Nullable String fallback)
    {
        Text translated = translate(key);

        if (key.equals(translated) == false)
        {
            return translated;
        }

        return Text.of(fallback);
    }

    /**
     * Can replace I18n
     * @param translationKey (key)
     * @param args (...args)
     * @return (Translated String)
     */
    public static Text translate(String translationKey, Object... args)
    {
        String str = LANG.get(translationKey);

        try
        {
            return Text.of(String.format(str, args));
        }
        catch (IllegalArgumentException e)
        {
            return Text.of("translate: format error: "+str);
        }
    }

    /**
     * Load a new lang file, such as "en_us" based on the default assets path
     * @param name (Lang Code)
     */
    public static void loadLangCode(String name)
    {
        LANG = i18nLang.create(i18nLang.DEFAULT_PATH+ name +".json");
    }

    public static void loadLangPath(String path)
    {
        LANG = i18nLang.create(path);
    }

    public static void setLanguage(i18nLang language)
    {
        LANG = language;
    }

    public static boolean hasTranslation(String key)
    {
        return LANG.hasTranslation(key);
    }
}
