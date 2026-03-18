package fi.dy.masa.servux.schematic.selection;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.servux.util.data.json.JsonUtils;
import fi.dy.masa.servux.util.position.PositionUtils;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class Box
{
    private BlockPos pos1;
    private BlockPos pos2;
    private BlockPos size = BlockPos.ZERO;
    private String name = "Unnamed";
    private PositionUtils.Corner selectedCorner = PositionUtils.Corner.NONE;

    public Box()
    {
        this.pos1 = BlockPos.ZERO;
        this.pos2 = BlockPos.ZERO;
        this.updateSize();
    }

    public Box(BlockPos pos1, BlockPos pos2, String name)
    {
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.name = name;

        this.updateSize();
    }

    public Box copy()
    {
        Box box = new Box(this.pos1, this.pos2, this.name);
        box.setSelectedCorner(this.selectedCorner);
        return box;
    }

    public BlockPos getPos1()
    {
        return this.pos1;
    }

    public BlockPos getPos2()
    {
        return this.pos2;
    }

    public BlockPos getSize()
    {
        return this.size;
    }

    public String getName()
    {
        return this.name;
    }

    public PositionUtils.Corner getSelectedCorner()
    {
        return this.selectedCorner;
    }

    public void setPos1(@Nullable BlockPos pos)
    {
        this.pos1 = pos;
        this.updateSize();
    }

    public void setPos2(@Nullable BlockPos pos)
    {
        this.pos2 = pos;
        this.updateSize();
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setSelectedCorner(PositionUtils.Corner corner)
    {
        this.selectedCorner = corner;
    }

    /*
    public void rotate(Rotation rotation)
    {
        BlockPos pos = PositionUtils.getTransformedBlockPos(this.getSize(), Mirror.NONE, rotation);
        this.setPos2(this.getPos1().add(pos).add(-1, -1, -1));
    }

    public void mirror(Mirror mirror)
    {
        BlockPos pos = PositionUtils.getTransformedBlockPos(this.getSize(), mirror, Rotation.NONE);
        this.setPos2(this.getPos1().add(pos).add(-1, -1, -1));
    }
    */

    private void updateSize()
    {
        if (this.pos1 != null && this.pos2 != null)
        {
            this.size = PositionUtils.getAreaSizeFromRelativeEndPosition(this.pos2.subtract(this.pos1));
        } else if (this.pos1 == null && this.pos2 == null)
        {
            this.size = BlockPos.ZERO;
        } else
        {
            this.size = new BlockPos(1, 1, 1);
        }
    }

    public BlockPos getPosition(PositionUtils.Corner corner)
    {
        return corner == PositionUtils.Corner.CORNER_1 ? this.getPos1() : this.getPos2();
    }

    public int getCoordinate(PositionUtils.Corner corner, PositionUtils.CoordinateType type)
    {
        BlockPos pos = this.getPosition(corner);

        switch (type)
        {
            case X:
                return pos.getX();
            case Y:
                return pos.getY();
            case Z:
                return pos.getZ();
        }

        return 0;
    }

    protected void setPosition(BlockPos pos, PositionUtils.Corner corner)
    {
        if (corner == PositionUtils.Corner.CORNER_1)
        {
            this.setPos1(pos);
        } else if (corner == PositionUtils.Corner.CORNER_2)
        {
            this.setPos2(pos);
        }
    }

    public void setCoordinate(int value, PositionUtils.Corner corner, PositionUtils.CoordinateType type)
    {
        BlockPos pos = this.getPosition(corner);
        pos = PositionUtils.getModifiedPosition(pos, value, type);
        this.setPosition(pos, corner);
    }

    @Nullable
    public static Box fromJson(JsonObject obj)
    {
        if (JsonUtils.hasString(obj, "name"))
        {
            BlockPos pos1 = JsonUtils.getBlockPosOrDefault(obj, "pos1", BlockPos.ZERO);
            BlockPos pos2 = JsonUtils.getBlockPosOrDefault(obj, "pos2", BlockPos.ZERO);

            if (pos1 != null || pos2 != null)
            {
                Box box = new Box();
                box.setName(obj.get("name").getAsString());

                if (pos1 != null)
                {
                    box.setPos1(pos1);
                }

                if (pos2 != null)
                {
                    box.setPos2(pos2);
                }

                return box;
            }
        }

        return null;
    }

    @Nullable
    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        if (this.pos1 != null)
        {
            obj.add("pos1", JsonUtils.blockPosToJson(this.pos1));
        }

        if (this.pos2 != null)
        {
            obj.add("pos2", JsonUtils.blockPosToJson(this.pos2));
        }

        obj.add("name", new JsonPrimitive(this.name));

        return this.pos1 != null || this.pos2 != null ? obj : null;
    }

    public net.minecraft.world.level.levelgen.structure.BoundingBox toVanilla()
    {
		if (this.pos1 == null)
		{
			this.pos1 = BlockPos.ZERO;
		}

	    return new BoundingBox(
			    Math.min(this.pos1.getX(), this.pos2.getX()),
			    Math.min(this.pos1.getY(), this.pos2.getY()),
			    Math.min(this.pos1.getZ(), this.pos2.getZ()),
			    Math.max(this.pos1.getX(), this.pos2.getX()),
			    Math.max(this.pos1.getY(), this.pos2.getY()),
			    Math.max(this.pos1.getZ(), this.pos2.getZ())
	    );
    }
}
