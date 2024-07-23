package fi.dy.masa.servux.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import fi.dy.masa.servux.dataproviders.IDataProvider;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ServuxListSetting<T> extends AbstractServuxSetting<List<T>>
{
    private String separatorRegex = ",";

    public ServuxListSetting(IDataProvider dataProvider, String name, Text prettyName, Text comment, List<T> defaultValue, List<String> examples, String separatorRegex)
    {
        this(dataProvider, name, prettyName, comment, defaultValue, examples);
        this.separatorRegex = separatorRegex;
    }

    public ServuxListSetting(IDataProvider dataProvider, String name, Text prettyName, Text comment, List<T> defaultValue, List<String> examples)
    {
        super(dataProvider, name, prettyName, comment, defaultValue, examples);
    }

    @Override
    public boolean validateString(String value)
    {
        return Arrays.stream(value.split(separatorRegex)).allMatch(this::validateStringForElement);
    }

    public abstract boolean validateStringForElement(String value);

    @Override
    public String valutToString(Object value)
    {
        return ((List<?>) value).stream().map(this::elementToString).collect(Collectors.joining(separatorRegex));
    }

    public abstract String elementToString(Object value);

    @Override
    public List<T> valueFromString(String value)
    {
        return Arrays.stream(value.split(separatorRegex)).map(this::elementFromString).collect(Collectors.toList());
    }

    public abstract T elementFromString(String value);

    @Override
    public void readFromJson(JsonElement element)
    {
        if (element.isJsonArray())
        {
            var array = element.getAsJsonArray();
            var list = array.asList().stream().map(this::readElementFromJson).collect(Collectors.toList());
            this.setValue(list);
        }
    }

    public abstract T readElementFromJson(JsonElement element);

    @Override
    public JsonElement writeToJson()
    {
        JsonArray array = new JsonArray();
        for (T value : this.getValue())
        {
            array.add(this.writeElementToJson(value));
        }
        return array;
    }

    public abstract JsonElement writeElementToJson(T value);
}
