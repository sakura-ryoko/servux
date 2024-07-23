package fi.dy.masa.servux.settings;

import net.minecraft.text.Text;

import java.util.List;

public interface IServuxSetting<T>
{
    String name();
    Text prettyName();
    Text comment();
    List<String> examples();

    T getDefaultValue();
    T getValue();
    void setValueNoCallback(T value);
    void setValue(T value);
    void setValueFromString(String value);
    boolean validateString(String value);
    String valutToString(T value);
    T valueFromString(String value);
}
