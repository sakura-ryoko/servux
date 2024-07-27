package fi.dy.masa.servux.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.servux.dataproviders.IDataProvider;
import net.minecraft.text.Text;

import java.util.List;

public class ServuxStringSetting extends AbstractServuxSetting<String>
{
    public ServuxStringSetting(IDataProvider dataProvider, String name, String defaultValue, List<String> examples)
    {
        this(dataProvider, name, null, null, defaultValue, examples);
    }

    public ServuxStringSetting(IDataProvider dataProvider, String name, Text prettyName, Text comment, String defaultValue, List<String> examples)
    {
        super(dataProvider, name, prettyName, comment, defaultValue, examples);
    }

    @Override
    public boolean validateString(String value)
    {
        return true;
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
