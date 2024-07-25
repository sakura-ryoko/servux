package fi.dy.masa.servux.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.JsonHelper;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;

public class i18nLang
{
    private static final Gson GSON = new Gson();
    private static final Pattern TOKEN_PATTERN = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");
    public static final String DEFAULT_LANG = "en_us";
    public static final String DEFAULT_PATH = "/assets/"+Reference.MOD_ID+"/lang/";
    private static volatile i18nLang instance = create(DEFAULT_PATH+DEFAULT_LANG+".json");

    public static i18nLang create(String path)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        BiConsumer<String, String> biConsumer = builder::put;
        load(biConsumer, path);
        final Map<String, String> map = builder.build();

        return new i18nLang()
        {
            @Override
            public String get(String key, String fallback)
            {
                return map.getOrDefault(key, fallback);
            }

            @Override
            public boolean hasTranslation(String key) {
                return map.containsKey(key);
            }

            @Override
            public boolean isRightToLeft() {
                return false;
            }

            @Override
            public OrderedText reorder(StringVisitable text)
            {
                return visitor -> text.visit(
                                (style, string) -> TextVisitFactory.visitFormatted(string, style, visitor) ? Optional.empty() : StringVisitable.TERMINATE_VISIT, Style.EMPTY
                        )
                        .isPresent();
            }
        };
    }

    public static void load(BiConsumer<String, String> entryConsumer, String path)
    {
        try
        {
            InputStream inputStream = i18nLang.class.getResourceAsStream(path);

            try
            {
                load(inputStream, entryConsumer);
            }
            catch (Throwable var6)
            {
                if (inputStream != null)
                {
                    try
                    {
                        inputStream.close();
                    }
                    catch (Throwable var5)
                    {
                        var6.addSuppressed(var5);
                    }
                }

                throw var6;
            }

            if (inputStream != null)
            {
                inputStream.close();
            }
        }
        catch (JsonParseException | IOException var7)
        {
            Servux.logger.error("Couldn't read strings from {}", path, var7);
        }
    }

    public static void load(InputStream inputStream, BiConsumer<String, String> entryConsumer)
    {
        JsonObject jsonObject = GSON.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), JsonObject.class);

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet())
        {
            String string = TOKEN_PATTERN.matcher(JsonHelper.asString(entry.getValue(), entry.getKey())).replaceAll("%$1s");
            entryConsumer.accept(entry.getKey(), string);
        }
    }

    public static i18nLang getInstance()
    {
        return instance;
    }

    public static void setInstance(i18nLang language)
    {
        instance = language;
    }

    public String get(String key)
    {
        return this.get(key, key);
    }

    public String get(String key, String fallback)
    {
        return null;
    }

    public boolean hasTranslation(String key)
    {
        return false;
    }

    public boolean isRightToLeft()
    {
        return false;
    }

    public OrderedText reorder(StringVisitable text)
    {
        return null;
    }

    public List<OrderedText> reorder(List<StringVisitable> texts)
    {
        return texts.stream().map(this::reorder).collect(ImmutableList.toImmutableList());
    }
}
