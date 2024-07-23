package fi.dy.masa.servux.settings;

import net.minecraft.text.Text;

import java.util.List;

public class ServuxBoolSetting extends AbstractServuxSetting<Boolean>
{
    public ServuxBoolSetting(String name, Text prettyName, Text comment, boolean defaultValue)
    {
        super(name, prettyName, comment, defaultValue, List.of("true", "false"));
    }

    @Override
    public boolean validateString(String value)
    {
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
    }

    @Override
    public String valutToString(Boolean value)
    {
        return value.toString();
    }

    @Override
    public Boolean valueFromString(String value)
    {
        return Boolean.parseBoolean(value);
    }
}
