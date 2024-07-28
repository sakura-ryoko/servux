package fi.dy.masa.servux.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.servux.dataproviders.IDataProvider;
import net.minecraft.text.Text;

import java.util.List;

public class ServuxBoolSetting extends AbstractServuxSetting<Boolean>
{
    public ServuxBoolSetting(IDataProvider dataProvider, String name, Text prettyName, Text comment, boolean defaultValue)
    {
        super(dataProvider, name, prettyName, comment, defaultValue, List.of("true", "false"));
    }

    public ServuxBoolSetting(IDataProvider dataProvider, String name, boolean defaultValue)
    {
        super(dataProvider, name, null, null, defaultValue, List.of("true", "false"));
    }

    @Override
    public boolean validateString(String value)
    {
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
    }

    @Override
    public String valueToString(Object value)
    {
        return ((Boolean) value).toString();
    }

    @Override
    public Boolean valueFromString(String value)
    {
        return Boolean.parseBoolean(value);
    }

    @Override
    public void readFromJson(JsonElement element)
    {
        if (element.isJsonPrimitive())
        {
            var value = element.getAsJsonPrimitive();

            if (value.isBoolean())
            {
                this.setValueNoCallback(value.getAsBoolean());
            }
        }
    }

    @Override
    public JsonElement writeToJson()
    {
        return new JsonPrimitive(this.getValue());
    }
}
