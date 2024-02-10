package fi.dy.masa.servux.litematics.placement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SubRegion
{
    private boolean isModified;
    private final Map<String, SubRegionModify> modData;

    public SubRegion() { this(false, null); }

    public SubRegion(final boolean isModified, final Map<String, SubRegionModify> mods)
    {
        this.isModified = isModified;
        this.modData = Objects.requireNonNullElseGet(mods, HashMap::new);
    }
    public void reset()
    {
        this.isModified = false;
        this.modData.clear();
    }
    public void modify(
            final String name,
            final BlockPos position,
            final BlockRotation rotation,
            final BlockMirror mirror
    ) {
        modify(
                new SubRegionModify(
                        name,
                        position,
                        rotation,
                        mirror
                )
        );
    }

    private void modify(final SubRegionModify region)
    {
        if (region == null)
            return;
        this.isModified = true;
        this.modData.put(region.name, region);
    }
    public boolean isModified() { return this.isModified; }
    public Map<String, SubRegionModify> getModData() { return this.modData; }

    public JsonElement toJson() { return modDataToJson(); }
    public JsonElement modDataToJson()
    {
        final JsonArray arr = new JsonArray();

        for (final Map.Entry<String, SubRegionModify> entry : this.modData.entrySet())
        {
            arr.add(entry.getValue().toJson());
        }
        return arr;
    }
    public static SubRegion fromJson(final JsonElement obj)
    {
        final SubRegion newSubRegion = new SubRegion();
        newSubRegion.isModified = true;

        for (final JsonElement modification : obj.getAsJsonArray())
        {
            newSubRegion.modify(SubRegionModify.fromJson(modification.getAsJsonObject()));
        }

        return newSubRegion;
    }
    @Override
    public String toString() {
        if (!this.isModified) {

            return "[]";
        }

        return this.modData.isEmpty() ? "[ERROR:null]" : this.modData.toString();
    }
}
