package fi.dy.masa.servux.schematic.transmit;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.util.data.FileType;

public class SchematicBuffer
{
    public static final int BUFFER_SIZE = 16384;
    private final FileType type;
    private final String fileName;
    private Slice[] buffer;
    private final int totalExpectedSlices;
    private final long totalExpectedSize;
    private final AtomicInteger receivedSlices = new AtomicInteger(0);

    public SchematicBuffer(int totalExpectedSlices, long totalExpectedSize)
    {
        this(totalExpectedSlices, totalExpectedSize, FileType.LITEMATICA_SCHEMATIC);
    }

    public SchematicBuffer(int totalExpectedSlices, long totalExpectedSize, FileType type)
    {
        this.type = type;
        this.fileName = UUID.randomUUID().toString();
        this.totalExpectedSlices = totalExpectedSlices;
        this.totalExpectedSize = totalExpectedSize;
        this.buffer = new Slice[totalExpectedSlices];
    }

    public FileType getType()
    {
        return this.type;
    }

    public String getFileName()
    {
        return this.fileName;
    }

    public String getFileNameWithExt()
    {
        return this.fileName + "." + FileType.getFileExt(this.type);
    }

    public void receiveSlice(final int number, Slice slice)
    {
        if (number >= 0 && number < this.totalExpectedSlices)
        {
            if (this.buffer[number] == null)
            {
                this.buffer[number] = slice;
                this.receivedSlices.incrementAndGet();
            }
        }
    }

    public boolean isComplete()
    {
        return this.receivedSlices.get() == this.totalExpectedSlices;
    }

    public Path writeFile(Path dir)
    {
        if (!this.isComplete())
        {
            Servux.LOGGER.error("SchematicBuffer#writeFile(): Attempted to write incomplete buffer! Expected: {}, Received: {}", this.totalExpectedSlices, this.receivedSlices.get());
            return null;
        }

        if (!Files.isDirectory(dir))
        {
            try
            {
                Files.createDirectory(dir);
                Servux.debugLog("LitematicBuffer#writeFile(): Created directory '{}' successfully", dir.toAbsolutePath().toString());
            }
            catch (IOException err)
            {
                Servux.LOGGER.error("LitematicBuffer#writeFile(): Exception creating directory '{}'; {}", dir.toAbsolutePath().toString(), err.getLocalizedMessage());
                return null;
            }
        }

        Path file = dir.resolve(this.getFileName());

        if (Files.exists(file))
        {
            try
            {
                Files.delete(file);
                Servux.debugLog("LitematicBuffer#writeFile(): Deleted file '{}' successfully", file.toAbsolutePath().toString());
            }
            catch (IOException err)
            {
                Servux.LOGGER.error("LitematicBuffer#writeFile(): Exception deleting file '{}'; {}", file.toAbsolutePath().toString(), err.getLocalizedMessage());
                return null;
            }
        }

        try (OutputStream os = Files.newOutputStream(file))
        {
            // Write in correct Slice order
            for (Slice entry : this.buffer)
            {
                os.write(entry.data(), 0, entry.size());
            }
        }
        catch (Exception err)
        {
            Servux.LOGGER.error("LitematicBuffer#writeFile(): Exception saving file '{}'; {}", file.toAbsolutePath().toString(), err.getLocalizedMessage());
            return null;
        }

        try
        {
            long actualSize = Files.size(file);

            if (actualSize != this.totalExpectedSize)
            {
                Servux.LOGGER.error("SchematicBuffer#writeFile(): File size mismatch for '{}'! Expected: {} bytes, Actual: {} bytes. Deleting corrupted file.",
                                        file.getFileName(), this.totalExpectedSize, actualSize);
                Files.deleteIfExists(file);
                return null;
            }
        }
        catch (IOException err)
        {
            Servux.LOGGER.error("SchematicBuffer#writeFile(): Exception verifying file size for '{}'; {}", file.toAbsolutePath().toString(), err.getLocalizedMessage());
            return null;
        }

        Servux.debugLog("SchematicBuffer#writeFile(): Saved file '{}' successfully", file.toAbsolutePath().toString());
        this.buffer = null;
        return file;
    }

    public record Slice(byte[] data, int size) {}
}
