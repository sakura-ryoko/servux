package fi.dy.masa.servux.litematics.placement;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;

public class SubRegionModify
{
    public final String name;
    public final BlockPos pos;
    public final BlockRotation rot;
    public final BlockMirror mirror;

    public SubRegionModify(String name, BlockPos pos, BlockRotation rot, BlockMirror mirror)
    {
        this.name = name;
        this.pos = pos;
        this.rot = rot;
        this.mirror = mirror;
    }
    public JsonObject toJson() {
        final JsonObject obj = new JsonObject();

        final JsonArray arr = new JsonArray();
        arr.add(pos.getX());
        arr.add(pos.getY());
        arr.add(pos.getZ());
        obj.add("position", arr);

        obj.add("name", new JsonPrimitive(name));
        obj.add("rotation", new JsonPrimitive(rot.name()));
        obj.add("mirror", new JsonPrimitive(mirror.name()));

        return obj;
    }
    public static SubRegionModify fromJson(final JsonObject obj)
    {
        if (
                !obj.has("name")
                        || !obj.has("position")
                        || !obj.has("rotation")
                        || !obj.has("mirror")
        ) {

            return null;
        }
        final String name = obj.get("name").getAsString();

        final JsonArray arr = obj.get("position").getAsJsonArray();
        if (arr.size() != 3) {

            return null;
        }
        final BlockPos position = new BlockPos(
                arr.get(0).getAsInt(),
                arr.get(1).getAsInt(),
                arr.get(2).getAsInt()
        );

        final BlockRotation rotation = BlockRotation.valueOf(obj.get("rotation").getAsString());
        final BlockMirror mirror = BlockMirror.valueOf(obj.get("mirror").getAsString());

        return new SubRegionModify(name, position, rotation, mirror);
    }
}
