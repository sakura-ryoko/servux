package fi.dy.masa.servux.util.nbt;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.*;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.util.data.Constants;

public class NbtUtils
{
    /**
     * Get the Entity's UUID from NBT.
     *
     * @param nbt ()
     * @return ()
     */
    public static @Nullable UUID getUUIDCodec(@Nonnull CompoundTag nbt)
    {
        return getUUIDCodec(nbt, "UUID");
    }

    /**
     * Get the Entity's UUID from NBT.
     *
     * @param nbt ()
     * @param key ()
     * @return ()
     */
    public static @Nullable UUID getUUIDCodec(@Nonnull CompoundTag nbt, String key)
    {
        if (nbt.contains(key))
        {
            return nbt.read(key, UUIDUtil.CODEC).orElse(null);
        }

        return null;
    }

    /**
     * Get the Entity's UUID from NBT.
     *
     * @param nbtIn ()
     * @param key   ()
     * @param uuid  ()
     * @return ()
     */
    public static CompoundTag putUUIDCodec(@Nonnull CompoundTag nbtIn, @Nonnull UUID uuid, String key)
    {
        nbtIn.store(key, UUIDUtil.CODEC, uuid);
        return nbtIn;
    }

    public static @Nonnull CompoundTag putVec2fCodec(@Nonnull CompoundTag tag, @Nonnull Vec2 pos, String key)
    {
        tag.store(key, Vec2.CODEC, pos);
        return tag;
    }

    public static @Nonnull CompoundTag putVec3iCodec(@Nonnull CompoundTag tag, @Nonnull Vec3i pos, String key)
    {
        tag.store(key, Vec3i.CODEC, pos);
        return tag;
    }

    public static @Nonnull CompoundTag putVec3dCodec(@Nonnull CompoundTag tag, @Nonnull Vec3 pos, String key)
    {
        tag.store(key, Vec3.CODEC, pos);
        return tag;
    }

    public static @Nonnull CompoundTag putPosCodec(@Nonnull CompoundTag tag, @Nonnull BlockPos pos, String key)
    {
        tag.store(key, BlockPos.CODEC, pos);
        return tag;
    }

    public static Vec2 getVec2fCodec(@Nonnull CompoundTag tag, String key)
    {
        return tag.read(key, Vec2.CODEC).orElse(Vec2.ZERO);
    }

    public static Vec3i getVec3iCodec(@Nonnull CompoundTag tag, String key)
    {
        return tag.read(key, Vec3i.CODEC).orElse(Vec3i.ZERO);
    }

    public static Vec3 getVec3dCodec(@Nonnull CompoundTag tag, String key)
    {
        return tag.read(key, Vec3.CODEC).orElse(Vec3.ZERO);
    }

    public static BlockPos getPosCodec(@Nonnull CompoundTag tag, String key)
    {
        return tag.read(key, BlockPos.CODEC).orElse(BlockPos.ZERO);
    }

    @Nonnull
    public static CompoundTag writeVec3iToArray(@Nonnull Vec3i pos, @Nonnull CompoundTag tag, String tagName)
    {
        return writeBlockPosToArrayTag(pos, tag, tagName);
    }

    @Nonnull
    public static CompoundTag writeVec3iToArrayTag(@Nonnull Vec3i pos, @Nonnull CompoundTag tag, String tagName)
    {
        return writeBlockPosToArrayTag(pos, tag, tagName);
    }

    @Nonnull
    public static CompoundTag writeBlockPosToArrayTag(@Nonnull Vec3i pos, @Nonnull CompoundTag tag, String tagName)
    {
        int[] arr = new int[]{pos.getX(), pos.getY(), pos.getZ()};
        tag.putIntArray(tagName, arr);
        return tag;
    }

    @Nullable
    public static BlockPos readBlockPosFromIntArray(@Nonnull CompoundTag nbt, String key)
    {
        return readBlockPosFromArrayTag(nbt, key);
    }

    @Nullable
    public static BlockPos readBlockPosFromArrayTag(@Nonnull CompoundTag tag, String tagName)
    {
        if (tag.contains(tagName))
        {
            int[] pos = tag.getIntArray(tagName).orElse(new int[0]);

            if (pos.length == 3)
            {
                return new BlockPos(pos[0], pos[1], pos[2]);
            }
        }

        return null;
    }

    @Nullable
    public static Vec3i readVec3iFromIntArray(@Nonnull CompoundTag nbt, String key)
    {
        return readVec3iFromIntArrayTag(nbt, key);
    }

    @Nullable
    public static Vec3i readVec3iFromIntArrayTag(@Nonnull CompoundTag tag, String tagName)
    {
        if (tag.contains(tagName))
        {
            int[] pos = tag.getIntArray(tagName).orElse(new int[0]);

            if (pos.length == 3)
            {
                return new Vec3i(pos[0], pos[1], pos[2]);
            }
        }

        return null;
    }

    public static CompoundTag createBlockPosTag(Vec3i pos)
    {
        return writeBlockPosToTag(pos, new CompoundTag());
    }

    public static CompoundTag writeBlockPosToTag(Vec3i pos, CompoundTag tag)
    {
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        return tag;
    }

    @Nullable
    public static BlockPos readBlockPos(@Nullable CompoundTag tag)
    {
        if (tag != null &&
            tag.contains("x") &&
            tag.contains("y") &&
            tag.contains("z"))
        {
            return new BlockPos(tag.getIntOr("x", 0), tag.getIntOr("y", 0), tag.getIntOr("z", 0));
        }

        return null;
    }

    public static CompoundTag writeVec3dToTag(Vec3 vec, CompoundTag tag)
    {
        tag.putDouble("dx", vec.x);
        tag.putDouble("dy", vec.y);
        tag.putDouble("dz", vec.z);
        return tag;
    }

    public static CompoundTag writeEntityPositionToTag(Vec3 pos, CompoundTag tag)
    {
        ListTag posList = new ListTag();

        posList.add(DoubleTag.valueOf(pos.x));
        posList.add(DoubleTag.valueOf(pos.y));
        posList.add(DoubleTag.valueOf(pos.z));
        tag.put("Pos", posList);

        return tag;
    }

    @Nullable
    public static Vec3 readVec3d(@Nullable CompoundTag tag)
    {
        if (tag != null &&
                tag.contains("dx") &&
                tag.contains("dy") &&
                tag.contains("dz"))
        {
            return new Vec3(tag.getDoubleOr("dx", 0d), tag.getDoubleOr("dy", 0d), tag.getDoubleOr("dz", 0d));
        }

        return null;
    }

    @Nullable
    public static Vec3 readEntityPositionFromTag(@Nullable CompoundTag tag)
    {
        if (tag != null && tag.contains("Pos"))
        {
            ListTag tagList = tag.getListOrEmpty("Pos");

            if (tagList.getId() == Constants.NBT.TAG_DOUBLE && tagList.size() == 3)
            {
                return new Vec3(tagList.getDoubleOr(0, 0d), tagList.getDoubleOr(1, 0d), tagList.getDoubleOr(2, 0d));
            }
        }

        return null;
    }

    @Nullable
    public static Vec3i readVec3iFromTag(@Nullable CompoundTag tag)
    {
        if (tag != null &&
            tag.contains("x") &&
            tag.contains("y") &&
            tag.contains("z"))
        {
            return new Vec3i(tag.getIntOr("x", 0), tag.getIntOr("y", 0), tag.getIntOr("z", 0));
        }

        return null;
    }

    @Nullable
    public static CompoundTag readNbtFromFileAsPath(@Nonnull Path file)
    {
        return readNbtFromFileAsPath(file, NbtAccounter.unlimitedHeap());
    }

    @Nullable
    public static CompoundTag readNbtFromFileAsPath(@Nonnull Path file, NbtAccounter tracker)
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
    public static void writeCompressed(@Nonnull CompoundTag tag, @Nonnull OutputStream outputStream)
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

    public static void writeCompressed(@Nonnull CompoundTag tag, @Nonnull Path file)
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

    /**
     * Reads in a Flat Map from NBT -- this way we don't need Mojang's code complexity
     * @param <T> ()
     * @param nbt ()
     * @param mapCodec ()
     * @return ()
     */
    public static <T> Optional<T> readFlatMap(@Nonnull CompoundTag nbt, MapCodec<T> mapCodec)
    {
        DynamicOps<Tag> ops = NbtOps.INSTANCE;

        return switch (ops.getMap(nbt).flatMap(map -> mapCodec.decode(ops, map)))
        {
            case DataResult.Success<T> result -> Optional.of(result.value());
            case DataResult.Error<T> error -> error.partialValue();
            default -> Optional.empty();
        };
    }

    /**
     * Writes a Flat Map to NBT -- this way we don't need Mojang's code complexity
     * @param <T> ()
     * @param mapCodec ()
     * @param value ()
     * @return ()
     */
    public static <T> CompoundTag writeFlatMap(MapCodec<T> mapCodec, T value)
    {
        DynamicOps<Tag> ops = NbtOps.INSTANCE;
        CompoundTag nbt = new CompoundTag();

        switch (mapCodec.encoder().encodeStart(ops, value))
        {
            case DataResult.Success<Tag> result -> nbt.merge((CompoundTag) result.value());
            case DataResult.Error<Tag> error -> error.partialValue().ifPresent(partial -> nbt.merge((CompoundTag) partial));
        }

        return nbt;
    }
}
