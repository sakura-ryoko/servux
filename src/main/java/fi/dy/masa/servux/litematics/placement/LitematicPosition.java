package fi.dy.masa.servux.litematics.placement;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class LitematicPosition
{
    private final BlockPos pos;
    private final String dimension;
    public static final String OVERWORLD_DIM = "minecraft:overworld";
    public static final String NETHER_DIM = "minecraft:the_nether";
    public static final String END_DIM = "minecraft:the_end";
    public static final String OVERWORLD_CAVES_DIM = "minecraft:overworld_caves";
    // Sort of a Pseudo-dimension listed in the Minecraft DomainType Registry.

    public LitematicPosition(BlockPos pos, String dim)
    {
        this.pos = pos;
        this.dimension = dim;
    }
    public BlockPos getBlockPos() { return this.pos; }
    public String getDimension() { return this.dimension; }
    public JsonObject toJson() {
        final JsonObject obj = new JsonObject();
        final JsonArray arr = new JsonArray();
        arr.add(new JsonPrimitive(this.pos.getX()));
        arr.add(new JsonPrimitive(this.pos.getY()));
        arr.add(new JsonPrimitive(this.pos.getZ()));
        obj.add("position", arr);
        obj.add("dimension", new JsonPrimitive(this.dimension));
        return obj;
    }
    @Nullable
    public static LitematicPosition fromJson(final JsonObject obj) {
        if (obj.has("position") && obj.has("dimension")) {
            final int x;
            final int y;
            final int z;
            final JsonArray arr = obj.get("position").getAsJsonArray();
            x = arr.get(0).getAsInt();
            y = arr.get(1).getAsInt();
            z = arr.get(2).getAsInt();
            final BlockPos pos = new BlockPos(x, y, z);
            return new LitematicPosition(pos, obj.get("dimension").getAsString());
        }
        return null;
    }
}
