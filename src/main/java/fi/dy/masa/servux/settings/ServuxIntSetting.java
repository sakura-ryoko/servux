package fi.dy.masa.servux.settings;

import net.minecraft.text.Text;

public class ServuxIntSetting extends AbstractServuxSetting<Integer>
{
    private final int maxValue;
    private final int minValue;

    public ServuxIntSetting(String name, Text prettyName, Text comment, Integer defaultValue, int maxValue, int minValue)
    {
        super(name, prettyName, comment, defaultValue);
        this.maxValue = maxValue;
        this.minValue = minValue;
    }

    public ServuxIntSetting(String name, Text prettyName, Text comment, Integer defaultValue)
    {
        this(name, prettyName, comment, defaultValue, Integer.MAX_VALUE, Integer.MIN_VALUE);
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
    public String valutToString(Integer value)
    {
        return value.toString();
    }

    @Override
    public Integer valueFromString(String value)
    {
        return Integer.parseInt(value);
    }
}
