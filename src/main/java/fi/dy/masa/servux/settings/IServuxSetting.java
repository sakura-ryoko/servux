package fi.dy.masa.servux.settings;

import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
    void setValue(T value) throws CommandSyntaxException;

    /**
     * Set the value from a string representation, this is used when setting the value from commands
     * @throws CommandSyntaxException if the value is invalid
     */
    void setValueFromString(String value) throws CommandSyntaxException;
    boolean validateString(String value);
    String valueToString(Object value);
    T valueFromString(String value);
    void readFromJson(JsonElement element);
    JsonElement writeToJson();

    default Text shortDisplayName() {
        return prettyName().copy().styled(style ->
            style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, comment().copy()
                    .append(Text.literal("\n(%s)".formatted(qualifiedName())).formatted(Formatting.DARK_GRAY))))
                .withColor(Formatting.YELLOW)
        );
    }

    default String qualifiedName() {
        return dataProvider().getName() + ":" + name();
    }
}
