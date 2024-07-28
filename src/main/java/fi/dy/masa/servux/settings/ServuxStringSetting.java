package fi.dy.masa.servux.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.servux.dataproviders.IDataProvider;
import net.minecraft.text.Text;

import java.util.List;

public class ServuxStringSetting extends AbstractServuxSetting<String>
{
    private final boolean strict;
    public ServuxStringSetting(IDataProvider dataProvider, String name, String defaultValue, List<String> examples, boolean strict)
    {
        this(dataProvider, name, null, null, defaultValue, examples, strict);
    }

    public ServuxStringSetting(IDataProvider dataProvider, String name, Text prettyName, Text comment, String defaultValue, List<String> examples, boolean strict)
    {
        super(dataProvider, name, prettyName, comment, defaultValue, examples);
        this.strict = strict;
    }

    @Override
    public boolean validateString(String value)
    {
        return strict ? this.examples().contains(value) : true;
    }

    @Override
    public String valueToString(Object value)
    {
        return (String) value;
    }

    @Override
    public String valueFromString(String value)
    {
        return value;
    }

    @Override
    public void readFromJson(JsonElement element)
    {
        if (element instanceof JsonPrimitive primitive)
        {
            this.setValueNoCallback(primitive.getAsString());
        }
    }

    @Override
    public JsonElement writeToJson()
    {
        return new JsonPrimitive(this.getValue());
    }
}
