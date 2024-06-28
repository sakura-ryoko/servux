package fi.dy.masa.servux.schematic.placement;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.servux.util.PositionUtils;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public class SubRegionPlacement
{
    private final String name;
    private final BlockPos defaultPos;
    private BlockPos pos;
    private BlockRotation rotation = BlockRotation.NONE;
    private BlockMirror mirror = BlockMirror.NONE;
    private boolean enabled = true;
    private boolean renderingEnabled = true;
    private boolean ignoreEntities;
    private int coordinateLockMask;

    public SubRegionPlacement(BlockPos pos, String name)
    {
        this.pos = pos;
        this.defaultPos = pos;
        this.name = name;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public boolean isRenderingEnabled()
    {
        return this.renderingEnabled;
    }

    public boolean ignoreEntities()
    {
        return this.ignoreEntities;
    }

    public void setCoordinateLocked(PositionUtils.CoordinateType coord, boolean locked)
    {
        int mask = 0x1 << coord.ordinal();

        if (locked)
        {
            this.coordinateLockMask |= mask;
        }
        else
        {
            this.coordinateLockMask &= ~mask;
        }
    }

    public boolean isCoordinateLocked(PositionUtils.CoordinateType coord)
    {
        int mask = 0x1 << coord.ordinal();
        return (this.coordinateLockMask & mask) != 0;
    }

    public boolean matchesRequirement(RequiredEnabled required)
    {
        if (required == RequiredEnabled.ANY)
        {
            return true;
        }

        if (required == RequiredEnabled.PLACEMENT_ENABLED)
        {
            return this.isEnabled();
        }

        return this.isEnabled() && this.isRenderingEnabled();
    }

    public String getName()
    {
        return this.name;
    }

    public BlockPos getPos()
    {
        return this.pos;
    }

    public BlockRotation getRotation()
    {
        return this.rotation;
    }

    public BlockMirror getMirror()
    {
        return this.mirror;
    }

    public void setRenderingEnabled(boolean renderingEnabled)
    {
        this.renderingEnabled = renderingEnabled;
    }

    public void toggleRenderingEnabled()
    {
        this.setRenderingEnabled(! this.isRenderingEnabled());
    }

    void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    void toggleEnabled()
    {
        this.setEnabled(! this.isEnabled());
    }

    void toggleIgnoreEntities()
    {
        this.ignoreEntities = ! this.ignoreEntities;
    }

    void setPos(BlockPos pos)
    {
        this.pos = PositionUtils.getModifiedPartiallyLockedPosition(this.pos, pos, this.coordinateLockMask);
    }

    void setRotation(BlockRotation rotation)
    {
        this.rotation = rotation;
    }

    void setMirror(BlockMirror mirror)
    {
        this.mirror = mirror;
    }

    void resetToOriginalValues()
    {
        this.pos = this.defaultPos;
        this.rotation = BlockRotation.NONE;
        this.mirror = BlockMirror.NONE;
        this.enabled = true;
        this.ignoreEntities = false;
    }

    public boolean isRegionPlacementModifiedFromDefault()
    {
        return this.isRegionPlacementModified(this.defaultPos);
    }

    public boolean isRegionPlacementModified(BlockPos originalPosition)
    {
        return this.isEnabled() == false ||
               this.ignoreEntities() ||
               this.getMirror() != BlockMirror.NONE ||
               this.getRotation() != BlockRotation.NONE ||
               this.getPos().equals(originalPosition) == false;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();

        arr.add(this.pos.getX());
        arr.add(this.pos.getY());
        arr.add(this.pos.getZ());

        obj.add("pos", arr);
        obj.add("name", new JsonPrimitive(this.getName()));
        obj.add("rotation", new JsonPrimitive(this.rotation.name()));
        obj.add("mirror", new JsonPrimitive(this.mirror.name()));
        obj.add("locked_coords", new JsonPrimitive(this.coordinateLockMask));
        obj.add("enabled", new JsonPrimitive(this.enabled));
        obj.add("rendering_enabled", new JsonPrimitive(this.renderingEnabled));
        obj.add("ignore_entities", new JsonPrimitive(this.ignoreEntities));

        return obj;
    }


    public enum RequiredEnabled
    {
        ANY,
        PLACEMENT_ENABLED,
        RENDERING_ENABLED;
    }
}
