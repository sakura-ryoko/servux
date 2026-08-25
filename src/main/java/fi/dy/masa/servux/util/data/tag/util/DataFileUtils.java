package fi.dy.masa.servux.util.data.tag.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.util.data.Constants;
import fi.dy.masa.servux.util.data.tag.BaseData;
import fi.dy.masa.servux.util.data.tag.CompoundData;
import fi.dy.masa.servux.util.data.tag.converter.DataConverterNbt;

public class DataFileUtils
{
    @Nullable
    public static CompoundData readCompoundDataFromNbtFile(Path file)
    {
        if (Files.isReadable(file) == false)
        {
            return null;
        }

        BaseData data = null;

        try (DataInputStream is = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(file)))))
        {
            data = readFromNbtStream(is, SizeTracker.FILE_MAX_BYTES);
        }
        catch (ZipException e)
        {
            // Maybe the file is uncompressed, attempt to read it as such
            try (DataInputStream is = new DataInputStream(new BufferedInputStream(Files.newInputStream(file))))
            {
                data = readFromNbtStream(is, SizeTracker.FILE_MAX_BYTES);
            }
            catch (Exception e2)
            {
                Servux.LOGGER.warn("DataFileUtils.readCompoundDataFromNbtFile: Failed to read (assumed uncompressed) NBT data from file '{}'; {}", file.toAbsolutePath(), e2.getLocalizedMessage());
            }
        }
        catch (Exception e)
        {
	        Servux.LOGGER.warn("DataFileUtils.readCompoundDataFromNbtFile: Failed to read NBT data from file '{}'; {}", file.toAbsolutePath(), e.getLocalizedMessage());
        }

        if (data instanceof CompoundData)
        {
            return (CompoundData) data;
        }

        return null;
    }

    public static boolean writeCompoundDataToCompressedNbtFile(Path file, BaseData data)
    {
        return writeCompoundDataToCompressedNbtFile(file, data, "");
    }

    public static boolean writeCompoundDataToCompressedNbtFile(Path file, BaseData data, String rootTagName)
    {
        try (DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(file)))))
        {
            return writeToNbtStream(os, data, rootTagName, SizeTracker.FILE_MAX_BYTES);
        }
        catch (Exception e)
        {
	        Servux.LOGGER.warn("DataFileUtils.writeCompoundDataToCompressedNbtFile: Failed to write NBT data to file '{}'; {}", file.toAbsolutePath(), e.getLocalizedMessage());
        }

        return false;
    }

	@Nullable
	public static BaseData readFromNbtStream(DataInput input)
	{
		return readFromNbtStream(input, SizeTracker.DEFAULT_MAX_BYTES);
	}

    @Nullable
    public static BaseData readFromNbtStream(DataInput input, long maxBytes)
    {
        try
        {
            byte tagType = input.readByte();

            if (tagType == Constants.NBT.TAG_END)
            {
                return null;
            }

            // Discard the name of the root tag
            input.readUTF();

            return BaseData.createTag(Constants.NBT.TAG_COMPOUND, input, 0, new SizeTracker(maxBytes));
        }
        catch (SizeTrackerException e)
        {
	        Servux.LOGGER.warn("DataFileUtils.readFromNbtStream: SizeTrackerException while reading NBT data; {}", e.getLocalizedMessage());
        }

        catch (IOException e)
        {
	        Servux.LOGGER.warn("DataFileUtils.readFromNbtStream: IOException while reading NBT data; {}", e.getLocalizedMessage());
        }

        return null;
    }

	public static boolean writeToNbtStream(DataOutput output, BaseData data, String tagName)
	{
		return writeToNbtStream(output, data, tagName, SizeTracker.DEFAULT_MAX_BYTES);
	}

    public static boolean writeToNbtStream(DataOutput output, BaseData data, String tagName, long maxBytes)
    {
        try
        {
			DataOutput dost = new DataOutputSizeTracker(output, new SizeTracker(maxBytes));

	        dost.writeByte(data.getType());

            if (data.getType() != Constants.NBT.TAG_END)
            {
	            dost.writeUTF(tagName);
                data.write(dost);
            }

            return true;
        }
        catch (SizeTrackerException e)
        {
	        Servux.LOGGER.warn("DataFileUtils.writeToNbtStream: SizeTrackerException while writing NBT data; {}", e.getLocalizedMessage());
        }
        catch (IOException e)
        {
	        Servux.LOGGER.warn("DataFileUtils.writeToNbtStream: IOException while writing NBT data; {}", e.getLocalizedMessage());
        }

        return false;
    }

	@Deprecated
	public static CompoundData readFromFileUsingNbtIo(@Nonnull Path file)
	{
		return readFromFileUsingNbtIo(file, NbtAccounter.unlimitedHeap());
	}

	@Deprecated
	public static CompoundData readFromFileUsingNbtIo(@Nonnull Path file, NbtAccounter tracker)
	{
		if (!Files.exists(file) || !Files.isReadable(file))
		{
			return null;
		}

		try
		{
			return DataConverterNbt.fromVanillaCompound(NbtIo.readCompressed(Files.newInputStream(file), tracker));
		}
		catch (Exception e)
		{
			Servux.LOGGER.warn("DataFileUtils.readFromFileUsingNbtIo: Failed to read NBT data from file '{}'", file.toString());
		}

		return null;
	}

	@Deprecated
	public static void writeToFileUsingNbtIo(@Nonnull CompoundData tag, @Nonnull Path file)
	{
		try
		{
			NbtIo.writeCompressed(DataConverterNbt.toVanillaCompound(tag), file);
		}
		catch (Exception err)
		{
			Servux.LOGGER.warn("DataFileUtils.writeToFileUsingNbtIo: Failed to write NBT data to file");
		}
	}
}
