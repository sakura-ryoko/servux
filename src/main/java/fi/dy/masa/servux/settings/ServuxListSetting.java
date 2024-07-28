package fi.dy.masa.servux.settings;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import fi.dy.masa.servux.dataproviders.IDataProvider;
import net.minecraft.text.Text;

import java.util.List;
import java.util.stream.Collectors;

public abstract class ServuxListSetting<T> extends AbstractServuxSetting<List<T>>
{
    private static final Gson GSON = new Gson();

    public ServuxListSetting(IDataProvider dataProvider, String name, Text prettyName, Text comment, List<T> defaultValue, List<String> examples, String separatorRegex)
    {
        this(dataProvider, name, prettyName, comment, defaultValue, examples);
    }

    public ServuxListSetting(IDataProvider dataProvider, String name, Text prettyName, Text comment, List<T> defaultValue, List<String> examples)
    {
        super(dataProvider, name, prettyName, comment, defaultValue, examples);
    }

    @Override
    public boolean validateString(String value)
    {
        JsonArray array = GSON.fromJson(value, JsonArray.class);
        for (JsonElement element : array)
        {
            if (!this.validateJsonForElement(element))
            {
                return false;
            }
        }
        return true;
    }

    public abstract boolean validateJsonForElement(JsonElement value);

    @SuppressWarnings("unchecked")
    @Override
    public String valueToString(Object value)
    {
        JsonArray array = new JsonArray();
        for (T ele : (List<T>) value)
        {
            array.add(this.writeElementToJson(ele));
        }
        return GSON.toJson(array);
    }

    @Override
    public List<T> valueFromString(String value)
    {
        return GSON.fromJson(value, JsonArray.class).asList().stream().map(this::readElementFromJson).toList();
    }

    @Override
    public void readFromJson(JsonElement element)
    {
        if (element.isJsonArray())
        {
            var array = element.getAsJsonArray();
            var list = array.asList().stream().map(this::readElementFromJson).collect(Collectors.toList());
            this.setValueNoCallback(list);
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
