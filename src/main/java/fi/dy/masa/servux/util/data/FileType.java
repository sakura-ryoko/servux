package fi.dy.masa.servux.util.data;

import java.nio.file.Files;
import java.nio.file.Path;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import net.minecraft.util.StringRepresentable;

public enum FileType implements StringRepresentable
{
    INVALID,
    UNKNOWN,
    JSON,
    TEXT,
    LITEMATICA_SCHEMATIC,
    SCHEMATICA_SCHEMATIC,
    SPONGE_SCHEMATIC,
    VANILLA_STRUCTURE;

    public static final StringRepresentable.EnumCodec<@NotNull FileType> CODEC = StringRepresentable.fromEnum(FileType::values);
    public static final ImmutableList<@NotNull FileType> VALUES = ImmutableList.copyOf(values());

    public static FileType fromName(String fileName)
    {
        if (fileName.endsWith(".litematic"))
            {
                return LITEMATICA_SCHEMATIC;
            }
            else if (fileName.endsWith(".schematic"))
            {
                return SCHEMATICA_SCHEMATIC;
            }
            else if (fileName.endsWith(".nbt"))
            {
                return VANILLA_STRUCTURE;
            }
            else if (fileName.endsWith(".schem"))
            {
                return SPONGE_SCHEMATIC;
            }
            else if (fileName.endsWith(".json"))
            {
                return JSON;
            }
            else if (fileName.endsWith(".txt"))
            {
                return TEXT;
            }

            return UNKNOWN;
    }

    public static FileType fromFile(Path file)
    {
        if (Files.exists(file) && Files.isReadable(file))
        {
            return fromName(file.getFileName().toString());
        }
        else
        {
            return INVALID;
        }
    }

    public static String getFileExt(FileType type)
    {
        return switch (type)
        {
            case LITEMATICA_SCHEMATIC   -> ".litematic";
            case SCHEMATICA_SCHEMATIC   -> ".schematic";
            case SPONGE_SCHEMATIC       -> ".schem";
            case VANILLA_STRUCTURE      -> ".nbt";
            case JSON                   -> ".json";
            case TEXT                   -> ".txt";
            case INVALID                -> ".invalid";
            case UNKNOWN                -> ".unknown";
        };
    }

    public static String getString(FileType type)
    {
        return switch (type)
        {
            case LITEMATICA_SCHEMATIC   -> "litematic";
            case SCHEMATICA_SCHEMATIC   -> "schematic";
            case SPONGE_SCHEMATIC       -> "sponge";
            case VANILLA_STRUCTURE      -> "vanilla_nbt";
            case JSON                   -> "JSON";
            case TEXT                   -> "TEXT";
            case INVALID                -> "invalid";
            case UNKNOWN                -> "unknown";
        };
    }

    @Override
    public @NotNull String getSerializedName()
    {
        return getString(this);
    }
}
