package fi.dy.masa.servux.settings;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fi.dy.masa.servux.dataproviders.IDataProvider;
import fi.dy.masa.servux.util.i18nLang;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Objects;

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
        Objects.requireNonNull(name);
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
        this(dataProvider, name, prettyName, comment, defaultValue, null);
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
    /**
     * the value field should not be modified directly, please invoke this method.
     * override this value to handle all the value changes, even caused by reading config.
     */
    public void setValueNoCallback(T value)
    {
        this.value = value;
    }

    @Override
    public void setValue(T value) throws CommandSyntaxException
    {
        var oldValue = this.getValue();
        setValueNoCallback(value);
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
    public void setValueFromString(String value) throws CommandSyntaxException
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
        if (prettyName == null)
        {
            return i18nLang.getInstance().translate("servux.config."+dataProvider.getName()+"."+name+".name");
        }
        return prettyName;
    }

    @Override
    public Text comment()
    {
        if (comment == null)
        {
            return i18nLang.getInstance().translate("servux.config."+dataProvider.getName()+"."+name+".comment");
        }
        return comment;
    }

    @Override
    public List<String> examples()
    {
        if (examples == null)
        {
            return List.of();
        }
        return examples;
    }
}
