package fi.dy.masa.servux.schematic.selection;

import com.google.gson.JsonElement;
import fi.dy.masa.servux.util.JsonUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SelectionManager
{
    private final Map<String, AreaSelection> selections = new HashMap<>();
    private final Map<String, AreaSelection> readOnlySelections = new HashMap<>();
    @Nullable
    private String currentSelectionId;
    private SelectionMode mode = SelectionMode.SIMPLE;

    @Nullable
    public String getCurrentSelectionId()
    {
        return this.mode == SelectionMode.NORMAL ? this.currentSelectionId : null;
    }

    @Nullable
    public String getCurrentNormalSelectionId()
    {
        return this.currentSelectionId;
    }

    @Nullable
    protected AreaSelection getNormalSelection(@Nullable String selectionId)
    {
        return selectionId != null ? this.selections.get(selectionId) : null;
    }

    @Nullable
    private AreaSelection tryLoadSelectionFromFile(String selectionId)
    {
        return tryLoadSelectionFromFile(new File(selectionId));
    }

    @Nullable
    public static AreaSelection tryLoadSelectionFromFile(File file)
    {
        JsonElement el = JsonUtils.parseJsonFile(file);

        if (el != null && el.isJsonObject())
        {
            return AreaSelection.fromJson(el.getAsJsonObject());
        }

        return null;
    }


    public void clear()
    {
        this.currentSelectionId = null;
        this.selections.clear();
        this.readOnlySelections.clear();
    }

}
