package fi.dy.masa.servux.settings;

import net.minecraft.text.Text;

public abstract class AbstractServuxSetting<T> implements IServuxSetting<T>
{
    private final String name;
    private final Text prettyName;
    private final String comment;
    private final T defaultValue;

    public AbstractServuxSetting(
        String name,
        Text prettyName,
        String comment,
        T defaultValue
    )
    {
        this.name = name;
        this.prettyName = prettyName;
        this.comment = comment;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    private T value;

    @Override
    public T getDefaultValue()
    {
        return defaultValue;
    }

    @Override
    public T getValue()
    {
        return value;
    }

    @Override
    public void setValueNoCallback(T value)
    {
        this.value = value;
    }

    @Override
    public void setValue(T value)
    {
        this.value = value;
        onValueChanged(value);
    }

    protected void onValueChanged(T value)
    {

    }

    @Override
    public void setValueFromString(String value)
    {
        if (this.validateString(value))
        {
            setValue(this.valueFromString(value));
        }
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public Text prettyName()
    {
        return prettyName;
    }

    @Override
    public String comment()
    {
        return comment;
    }
}
