package fi.dy.masa.servux.util.nbt;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.util.data.Constants;

public class NbtUtils
{
    public static NbtCompound createBlockPosTag(Vec3i pos)
    {
        return writeBlockPosToTag(pos, new NbtCompound());
    }

    public static NbtCompound writeBlockPosToTag(Vec3i pos, NbtCompound tag)
    {
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        return tag;
    }

    @Nullable
    public static BlockPos readBlockPos(@Nullable NbtCompound tag)
    {
        if (tag != null &&
            tag.contains("x", Constants.NBT.TAG_INT) &&
            tag.contains("y", Constants.NBT.TAG_INT) &&
            tag.contains("z", Constants.NBT.TAG_INT))
        {
            return new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        }

        return null;
    }

    public static NbtCompound writeVec3dToTag(Vec3d vec, NbtCompound tag)
    {
        tag.putDouble("dx", vec.x);
        tag.putDouble("dy", vec.y);
        tag.putDouble("dz", vec.z);
        return tag;
    }

    public static NbtCompound writeEntityPositionToTag(Vec3d pos, NbtCompound tag)
    {
        NbtList posList = new NbtList();

        posList.add(NbtDouble.of(pos.x));
        posList.add(NbtDouble.of(pos.y));
        posList.add(NbtDouble.of(pos.z));
        tag.put("Pos", posList);

        return tag;
    }

    @Nullable
    public static Vec3d readVec3d(@Nullable NbtCompound tag)
    {
        if (tag != null &&
            tag.contains("dx", Constants.NBT.TAG_DOUBLE) &&
            tag.contains("dy", Constants.NBT.TAG_DOUBLE) &&
            tag.contains("dz", Constants.NBT.TAG_DOUBLE))
        {
            return new Vec3d(tag.getDouble("dx"), tag.getDouble("dy"), tag.getDouble("dz"));
        }

        return null;
    }

    @Nullable
    public static Vec3d readEntityPositionFromTag(@Nullable NbtCompound tag)
    {
        if (tag != null && tag.contains("Pos", Constants.NBT.TAG_LIST))
        {
            NbtList tagList = tag.getList("Pos", Constants.NBT.TAG_DOUBLE);

            if (tagList.getHeldType() == Constants.NBT.TAG_DOUBLE && tagList.size() == 3)
            {
                return new Vec3d(tagList.getDouble(0), tagList.getDouble(1), tagList.getDouble(2));
            }
        }

        return null;
    }

    @Nullable
    public static Vec3i readVec3iFromTag(@Nullable NbtCompound tag)
    {
        if (tag != null &&
            tag.contains("x", Constants.NBT.TAG_INT) &&
            tag.contains("y", Constants.NBT.TAG_INT) &&
            tag.contains("z", Constants.NBT.TAG_INT))
        {
            return new Vec3i(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        }

        return null;
    }

    @Nullable
    public static BlockPos readBlockPosFromIntArray(@Nonnull NbtCompound nbt, String key)
    {
        return readBlockPosFromArrayTag(nbt, key);
    }

    @Nullable
    public static BlockPos readBlockPosFromArrayTag(@Nonnull NbtCompound tag, String tagName)
    {
        if (tag.contains(tagName, Constants.NBT.TAG_INT_ARRAY))
        {
            int[] pos = tag.getIntArray(tagName);

            if (pos.length == 3)
            {
                return new BlockPos(pos[0], pos[1], pos[2]);
            }
        }

        return null;
    }

    @Nullable
    public static Vec3i readVec3iFromIntArray(@Nonnull NbtCompound nbt, String key)
    {
        return readVec3iFromIntArrayTag(nbt, key);
    }

    @Nullable
    public static Vec3i readVec3iFromIntArrayTag(@Nonnull NbtCompound tag, String tagName)
    {
        if (tag.contains(tagName, Constants.NBT.TAG_INT_ARRAY))
        {
            int[] pos = tag.getIntArray(tagName);

            if (pos.length == 3)
            {
                return new Vec3i(pos[0], pos[1], pos[2]);
            }
        }

        return null;
    }

    @Nullable
    public static Vec3d readVec3dFromListTag(@Nullable NbtCompound tag)
    {
        return readVec3dFromListTag(tag, NbtKeys.POS);
    }

    @Nullable
    public static Vec3d readVec3dFromListTag(@Nullable NbtCompound tag, String tagName)
    {
        if (tag != null && tag.contains(tagName, Constants.NBT.TAG_LIST))
        {
            NbtList tagList = tag.getList(tagName, Constants.NBT.TAG_DOUBLE);

            if (tagList.getHeldType() == Constants.NBT.TAG_DOUBLE && tagList.size() == 3)
            {
                return new Vec3d(tagList.getDouble(0), tagList.getDouble(1), tagList.getDouble(2));
            }
        }

        return null;
    }

    @Nullable
    public static NbtCompound readNbtFromFileAsPath(@Nonnull Path file)
    {
        return readNbtFromFileAsPath(file, NbtSizeTracker.ofUnlimitedBytes());
    }

    @Nullable
    public static NbtCompound readNbtFromFileAsPath(@Nonnull Path file, NbtSizeTracker tracker)
    {
        if (!Files.exists(file) || !Files.isReadable(file))
        {
            return null;
        }

        try
        {
            return NbtIo.readCompressed(Files.newInputStream(file), tracker);
        }
        catch (Exception e)
        {
            Servux.LOGGER.warn("readNbtFromFileAsPath: Failed to read NBT data from file '{}'", file.toString());
        }

        return null;
    }

    /**
     * Write the compound tag, gzipped, to the output stream.
     */
    public static void writeCompressed(@Nonnull NbtCompound tag, @Nonnull OutputStream outputStream)
    {
        try
        {
            NbtIo.writeCompressed(tag, outputStream);
        }
        catch (Exception err)
        {
            Servux.LOGGER.warn("writeCompressed: Failed to write NBT data to output stream");
        }
    }

    public static void writeCompressed(@Nonnull NbtCompound tag, @Nonnull Path file)
    {
        try
        {
            NbtIo.writeCompressed(tag, file);
        }
        catch (Exception err)
        {
            Servux.LOGGER.warn("writeCompressed: Failed to write NBT data to file");
        }
    }
}
