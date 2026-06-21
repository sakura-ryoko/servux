package fi.dy.masa.servux.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.chat.Component;

import fi.dy.masa.servux.dataproviders.IDataProvider;
import fi.dy.masa.servux.util.StringUtils;

public abstract class AbstractServuxSetting<T> implements IServuxSetting<T>
{
    private final String name;
    private final Component prettyName;
    private final Component comment;
    private final T defaultValue;
    private final List<String> examples;
    private final IDataProvider dataProvider;
    private final @Nullable IServuxSettingCallback<T> callback;

    public AbstractServuxSetting(IDataProvider dataProvider, String name, Component prettyName, Component comment, T defaultValue, List<String> examples, @Nullable IServuxSettingCallback<T> callback)
    {
        Objects.requireNonNull(name);
        this.name = name;
        this.prettyName = prettyName;
        this.comment = comment;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.examples = examples != null ? new ArrayList<>(examples) : new ArrayList<>();
        this.dataProvider = dataProvider;
        this.callback = callback;
    }

    public AbstractServuxSetting(IDataProvider dataProvider, String name, Component prettyName, Component comment, T defaultValue, IServuxSettingCallback<T> callback)
    {
        this(dataProvider, name, prettyName, comment, defaultValue, null, callback);
    }

    public AbstractServuxSetting(IDataProvider dataProvider, String name, Component prettyName, Component comment, T defaultValue)
    {
        this(dataProvider, name, prettyName, comment, defaultValue, null, null);
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

	/**
	 * the value field should not be modified directly, please invoke this method.
	 * override this value to handle all the value changes, even caused by reading config.
	 */
    @Override
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
    public void updateExamples(List<String> examples)
    {
        this.examples.clear();
        this.examples.addAll(examples);
    }

    @Override
    public IDataProvider dataProvider()
    {
        return dataProvider;
    }

    protected void onValueChanged(T oldValue, T value)
    {
        if (this.callback != null)
        {
            this.callback.onValueChanged(this, oldValue, value);
        }
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
    public Component prettyName()
    {
        if (prettyName == null)
        {
            return StringUtils.translate("servux.config."+dataProvider.getName()+"."+name+".name");
        }
        return prettyName;
    }

    @Override
    public Component comment()
    {
        if (comment == null)
        {
            return StringUtils.translate("servux.config."+dataProvider.getName()+"."+name+".comment");
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
