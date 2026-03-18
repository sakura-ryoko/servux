package fi.dy.masa.servux.schematic.transmit;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.util.data.FileType;

public class SchematicBuffer implements AutoCloseable
{
    public static final int BUFFER_SIZE = 16384;
    private final String name;
    private final FileType type;
    private final HashMap<Integer, Slice> buffer;

    public SchematicBuffer(String name)
    {
        this(name, FileType.LITEMATICA_SCHEMATIC);
    }

    public SchematicBuffer(String name, FileType type)
    {
        this.name = name;
        this.type = type;
        this.buffer = new HashMap<>();
    }

    public String getName()
    {
        return this.name;
    }

    public FileType getType()
    {
        return this.type;
    }

    public Path getFileName()
    {
        String ext = FileType.getFileExt(this.type);

        if (this.name.contains(ext))
        {
            return Path.of(this.name);
        }
        else
        {
            return Path.of(this.name + ext);
        }
    }

    public void receiveSlice(final int number, Slice slice)
    {
        this.buffer.put(number, slice);
    }

    public Path writeFile(Path dir)
    {
        if (!Files.isDirectory(dir))
        {
            try
            {
                Files.createDirectory(dir);
                Servux.debugLog("SchematicBuffer#writeFile(): Created directory '{}' successfully", dir.toAbsolutePath().toString());
            }
            catch (IOException err)
            {
                Servux.LOGGER.error("SchematicBuffer#writeFile(): Exception creating directory '{}'; {}", dir.toAbsolutePath().toString(), err.getLocalizedMessage());
                return null;
            }
        }

        Path file = dir.resolve(this.getFileName());

        if (Files.exists(file))
        {
            try
            {
                Files.delete(file);
                Servux.debugLog("SchematicBuffer#writeFile(): Deleted file '{}' successfully", file.toAbsolutePath().toString());
            }
            catch (IOException err)
            {
                Servux.LOGGER.error("SchematicBuffer#writeFile(): Exception deleting file '{}'; {}", file.toAbsolutePath().toString(), err.getLocalizedMessage());
                return null;
            }
        }

        try (OutputStream os = Files.newOutputStream(file))
        {
            // Write in correct Slice order
            for (int i = 0; i < this.buffer.size(); i++)
            {
                Slice entry = this.buffer.get(i);

                if (entry != null)
                {
                    os.write(entry.data(), 0, entry.size());
                }
            }
        }
        catch (Exception err)
        {
            Servux.LOGGER.error("SchematicBuffer#writeFile(): Exception saving file '{}'; {}", file.toAbsolutePath().toString(), err.getLocalizedMessage());
            return null;
        }

        Servux.debugLog("SchematicBuffer#writeFile(): Saved file '{}' successfully", file.toAbsolutePath().toString());
        this.buffer.clear();
        return file;
    }

    @Override
    public void close() throws Exception
    {
        this.buffer.clear();
    }

    public record Slice(byte[] data, int size) {}
}
