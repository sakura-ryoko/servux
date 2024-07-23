package fi.dy.masa.servux.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.servux.dataproviders.IDataProvider;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;
import net.minecraft.text.Text;

import java.util.List;

public class ServuxStringListSetting extends ServuxListSetting<String>
{
    public ServuxStringListSetting(IDataProvider dataProvider, String name, Text prettyName, Text comment, List<String> defaultValue, List<String> examples)
    {
        super(dataProvider, name, prettyName, comment, defaultValue, examples);
    }

    public ServuxStringListSetting(IDataProvider dataProvider, String name, Text prettyName, Text comment, List<String> defaultValue)
    {
        super(dataProvider, name, prettyName, comment, defaultValue, List.of());
    }

    @Override
    public boolean validateStringForElement(String value)
    {
        return true;
    }

    @Override
    public String readElementFromJson(JsonElement element)
    {
        return element.getAsString();
    }

    @Override
    public JsonElement writeElementToJson(String value)
    {
        return new JsonPrimitive(value);
    }
}
