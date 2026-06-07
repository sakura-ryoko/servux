package fi.dy.masa.servux.util;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;

public class FileUtils
{
    public static Path getConfigDirectory()
    {
        return Reference.DEFAULT_CONFIG_DIR;
    }

    public static Path getMinecraftDirectory()
    {
        return Reference.DEFAULT_RUN_DIR;
    }

    public static Path getRootDirectory()
    {
        return Paths.get("/");
    }

    public static String getNameWithoutExtension(String name)
    {
        int i = name.lastIndexOf(".");
        return i != -1 ? name.substring(0, i) : name;
    }

    public static boolean createDirectoriesIfMissing(Path dir)
    {
        return createDirectoriesIfMissing(dir, Servux.LOGGER::warn);
    }

    public static boolean createDirectoriesIfMissing(Path dir,
                                                     @Nullable Consumer<String> messageConsumer)
    {
        try
        {
            if (Files.isDirectory(dir) == false)
            {
                Files.createDirectories(dir);
            }
        }
        catch (Exception e)
        {
            if (messageConsumer != null)
            {
//                messageConsumer.accept(StringUtils.translate("malilib.message.error.failed_to_create_directory",
//                                                             dir.toAbsolutePath()));
                messageConsumer.accept("Failed to create directory: '" + dir.toAbsolutePath().toString()+"'");
            }

            return false;
        }

        if (Reference.DEV_DEBUG)
        {
            Servux.debugLog("createDirectoriesIfMissing: '{}'", dir.toAbsolutePath().toString());
        }

        return Files.isDirectory(dir);
    }

    public static boolean move(Path srcFile, Path dstFile)
    {
        return move(srcFile, dstFile, true);
    }

    public static boolean move(Path srcFile, Path dstFile, boolean overwrite)
    {
        return move(srcFile, dstFile, overwrite, Servux.LOGGER::warn);
    }

    public static boolean move(Path srcFile, Path dstFile, boolean overwrite, Consumer<String> messageConsumer)
    {
        try
        {
            if (overwrite)
            {
                Files.move(srcFile, dstFile, StandardCopyOption.REPLACE_EXISTING);
            }
            else if (Files.exists(dstFile))
            {
//                messageConsumer.accept(StringUtils.translateAsString("malilib.message.error.file_or_directory_already_exists",
//                                                             dstFile.toAbsolutePath()));
                messageConsumer.accept("File '" + dstFile.toAbsolutePath() + "' already exists.");
            }
            else
            {
                Files.move(srcFile, dstFile);
            }

            if (Reference.DEV_DEBUG)
            {
                Servux.debugLog("move: '{}' -> '{}'", srcFile.toAbsolutePath().toString(), dstFile.toAbsolutePath().toString());
            }

            return true;
        }
        catch (Exception e)
        {
//            messageConsumer.accept(StringUtils.translateAsString("malilib.message.error.failed_to_move_file",
//                                                         srcFile.toAbsolutePath(), dstFile.toAbsolutePath()));
            messageConsumer.accept("Failed to move file '"+srcFile.toAbsolutePath().toString()+"' to '"+dstFile.toAbsolutePath().toString()+"'");

            return false;
        }
    }

    // , FileWriteType writeType
    public static boolean writeDataToFile(final Path file, Consumer<BufferedWriter> dataWriter)
    {
        Path dir = file.getParent();

        if (dir != null && createDirectoriesIfMissing(dir) == false)
        {
            return false;
        }

        if (dir == null)
        {
            dir = Paths.get(".");
        }

        // writeType == FileWriteType.NORMAL_WRITE
        if (Files.isSymbolicLink(file))
        {
            // Don't replace/override symbolic links, but just write to the pointed file directly
            return writeDataToExactFile(file, dataWriter);
        }

//        if (writeType == FileWriteType.TEMP_AND_RENAME)
//        {
            // First write to a separate temporary file, and then rename it over the old file
            Path fileTmp = dir.resolve(file.getFileName() + ".tmp");

            if (Files.exists(fileTmp))
            {
                fileTmp = dir.resolve(UUID.randomUUID() + ".tmp");
            }

            return writeDataToExactFile(fileTmp, dataWriter) && move(fileTmp, file);
//        }
//
//        return false;
    }

    public static boolean writeDataToExactFile(final Path file, Consumer<BufferedWriter> dataWriter)
    {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8))
        {
            dataWriter.accept(writer);
            writer.close();

            if (Reference.DEV_DEBUG)
            {
                Servux.debugLog("writeDataToExactFile: '{}'", file.toAbsolutePath().toString());
            }

            return true;
        }
        catch (Exception e)
        {
            Servux.LOGGER.warn("Failed to write to file '{}'", file.toAbsolutePath(), e);
        }

        return false;
    }
}
