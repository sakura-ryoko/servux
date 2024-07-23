package fi.dy.masa.servux.settings;

import fi.dy.masa.servux.dataproviders.IDataProvider;
import net.minecraft.text.Text;

import java.util.List;

public abstract class AbstractServuxSetting<T> implements IServuxSetting<T>
{
    private final String name;
    private final Text prettyName;
    private final Text comment;
    private final T defaultValue;
    private final List<String> examples;
    private final IDataProvider dataProvider;

    public AbstractServuxSetting(IDataProvider dataProvider, String name, Text prettyName, Text comment, T defaultValue, List<String> examples)
    {
        this.name = name;
        this.prettyName = prettyName;
        this.comment = comment;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.examples = examples;
        this.dataProvider = dataProvider;
    }

    public AbstractServuxSetting(IDataProvider dataProvider, String name, Text prettyName, Text comment, T defaultValue)
    {
        this.name = name;
        this.prettyName = prettyName;
        this.comment = comment;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.examples = List.of();
        this.dataProvider = dataProvider;
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
        var oldValue = this.value;
        this.value = value;
        onValueChanged(oldValue, value);
    }

    @Override
    public IDataProvider dataProvider()
    {
        return dataProvider;
    }

    protected void onValueChanged(T oldValue, T value)
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
    public Text comment()
    {
        return comment;
    }

    @Override
    public List<String> examples()
    {
        return examples;
    }
}
