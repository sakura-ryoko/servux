package fi.dy.masa.servux.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.servux.dataproviders.IDataProvider;
import net.minecraft.text.Text;

public class ServuxIntSetting extends AbstractServuxSetting<Integer>
{
    private final int maxValue;
    private final int minValue;

    public ServuxIntSetting(IDataProvider dataProvider, String name, Text prettyName, Text comment, Integer defaultValue, int maxValue, int minValue)
    {
        super(dataProvider ,name, prettyName, comment, defaultValue);
        this.maxValue = maxValue;
        this.minValue = minValue;
    }

    public ServuxIntSetting(IDataProvider dataProvider, String name, Text prettyName, Text comment, Integer defaultValue)
    {
        this(dataProvider, name, prettyName, comment, defaultValue, Integer.MAX_VALUE, Integer.MIN_VALUE);
    }

    @Override
    public boolean validateString(String value)
    {
        try
        {
            int val = Integer.parseInt(value);
            return val >= this.minValue && val <= this.maxValue;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    @Override
    public String valueToString(Object value)
    {
        return ((Integer) value).toString();
    }

    @Override
    public Integer valueFromString(String value)
    {
        return Integer.parseInt(value);
    }

    @Override
    public void readFromJson(JsonElement element)
    {
        if (element.isJsonPrimitive())
        {
            var value = element.getAsJsonPrimitive();

            if (value.isNumber())
            {
                this.setValue(value.getAsInt());
            }
        }
    }

    @Override
    public JsonElement writeToJson()
    {
        return new JsonPrimitive(this.getValue());
    }
}
