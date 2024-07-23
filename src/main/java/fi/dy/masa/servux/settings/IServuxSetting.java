package fi.dy.masa.servux.settings;

import fi.dy.masa.servux.dataproviders.IDataProvider;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public interface IServuxSetting<T>
{
    String name();
    Text prettyName();
    Text comment();
    List<String> examples();
    IDataProvider dataProvider();

    T getDefaultValue();
    T getValue();
    void setValueNoCallback(T value);
    void setValue(T value);
    void setValueFromString(String value);
    boolean validateString(String value);
    String valutToString(Object value);
    T valueFromString(String value);

    default Text shortDisplayName() {
        return prettyName().copy().styled(style ->
            style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, comment().copy().append("\n(%s)".formatted(qualifiedName()))))
                .withColor(Formatting.YELLOW)
        );
    }

    default String qualifiedName() {
        return dataProvider().getName() + ":" + name();
    }
}
