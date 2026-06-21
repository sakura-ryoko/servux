package fi.dy.masa.servux.settings;

import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fi.dy.masa.servux.dataproviders.IDataProvider;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

public interface IServuxSetting<T>
{
    String name();

    Component prettyName();

    Component comment();

    List<String> examples();

    IDataProvider dataProvider();

    T getDefaultValue();

    T getValue();

    void setValueNoCallback(T value);

    void setValue(T value) throws CommandSyntaxException;

    void updateExamples(List<String> examples);

    /**
     * Set the value from a string representation, this is used when setting the value from commands
     *
     * @throws CommandSyntaxException if the value is invalid
     */
    void setValueFromString(String value) throws CommandSyntaxException;

    boolean validateString(String value);

    String valueToString(Object value);

    T valueFromString(String value);

    void readFromJson(JsonElement element);

    JsonElement writeToJson();

    default Component shortDisplayName()
    {
        return prettyName().copy().withStyle(style ->
                                                  //style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, comment().copy()
                                                  //.append(Text.literal("\n(%s)".formatted(qualifiedName())).formatted(Formatting.DARK_GRAY))))
                                                  style.withHoverEvent(new HoverEvent.ShowText(comment().copy()
                                                                                                        .append(Component.literal("\n(%s)".formatted(qualifiedName()))
                                                                                                                    .withStyle(ChatFormatting.DARK_GRAY))))
                                                       .withColor(ChatFormatting.YELLOW)
                                         );
    }

    default String qualifiedName()
    {
        return dataProvider().getName() + ":" + name();
    }
}
