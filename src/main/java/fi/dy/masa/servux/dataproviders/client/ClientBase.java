package fi.dy.masa.servux.dataproviders.client;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public abstract class ClientBase implements IClient
{
    private String name;
    private UUID uuid;
    private String version;
    private final NbtCompound metadata;

    public ClientBase(String name, UUID uuid, @Nullable String version, @Nullable NbtCompound metadata)
    {
        this.name = name;
        this.uuid = uuid;
        this.version = Objects.requireNonNullElse(version, "");
        if (metadata != null && metadata.getSizeInBytes() > 0)
        {
            this.metadata = new NbtCompound();
            this.metadata.copyFrom(metadata);
        }
        else
            this.metadata = new NbtCompound();
    }
    public String getName() { return this.name; }
    public UUID getUUID() { return this.uuid; }
    public String getVersion() { return this.version; }
    public NbtCompound getMetadata() { return this.metadata; }
    protected void updateName(String name) { this.name = name; }
    protected void updateUUID(UUID id) { this.uuid = id; }
    protected void updateVersion(String ver) { this.version = ver; }
    protected void copyMetadata(NbtCompound nbt) { this.metadata.copyFrom(nbt); }
    protected void updateMetadataString(String key, String data)
    {
        if (this.metadata.contains(key))
        {
            this.metadata.remove(key);
            this.metadata.putString(key, data);
        }
        else
        {
            this.metadata.putString(key, data);
        }
    }
    public String getMetadataString(String key)
    {
        if (this.metadata.contains(key))
        {
            return this.metadata.getString(key);
        }
        else return null;
    }
    protected void updateMetadataInt(String key, int data)
    {
        if (this.metadata.contains(key))
        {
            this.metadata.remove(key);
            this.metadata.putInt(key, data);
        }
        else
        {
            this.metadata.putInt(key, data);
        }
    }
    public Integer getMetadataInt(String key)
    {
        if (this.metadata.contains(key))
        {
            return this.metadata.getInt(key);
        }
        else return null;
    }
    protected void updateMetadataBoolean(String key, boolean data)
    {
        if (this.metadata.contains(key))
        {
            this.metadata.remove(key);
            this.metadata.putBoolean(key, data);
        }
        else
        {
            this.metadata.putBoolean(key, data);
        }
    }
    public Boolean getMetadataBoolean(String key)
    {
        if (this.metadata.contains(key))
        {
            return this.metadata.getBoolean(key);
        }
        else return null;
    }
    protected void updateMetadataByte(String key, byte data)
    {
        if (this.metadata.contains(key))
        {
            this.metadata.remove(key);
            this.metadata.putByte(key, data);
        }
        else
        {
            this.metadata.putByte(key, data);
        }
    }
    public Byte getMetadataByte(String key)
    {
        if (this.metadata.contains(key))
        {
            return this.metadata.getByte(key);
        }
        else return null;
    }
    protected void updateMetadataLong(String key, long data)
    {
        if (this.metadata.contains(key))
        {
            this.metadata.remove(key);
            this.metadata.putLong(key, data);
        }
        else
        {
            this.metadata.putLong(key, data);
        }
    }
    public Long getMetadataLong(String key)
    {
        if (this.metadata.contains(key))
        {
            return this.metadata.getLong(key);
        }
        else return null;
    }
    protected void updateMetadataDouble(String key, double data)
    {
        if (this.metadata.contains(key))
        {
            this.metadata.remove(key);
            this.metadata.putDouble(key, data);
        }
        else
        {
            this.metadata.putDouble(key, data);
        }
    }
    public Double getMetadataDouble(String key)
    {
        if (this.metadata.contains(key))
        {
            return this.metadata.getDouble(key);
        }
        else return null;
    }
    protected void updateMetadataFloat(String key, float data)
    {
        if (this.metadata.contains(key))
        {
            this.metadata.remove(key);
            this.metadata.putFloat(key, data);
        }
        else
        {
            this.metadata.putFloat(key, data);
        }
    }
    public Float getMetadataFloat(String key)
    {
        if (this.metadata.contains(key))
        {
            return this.metadata.getFloat(key);
        }
        else return null;
    }
    protected void updateMetadataShort(String key, short data)
    {
        if (this.metadata.contains(key))
        {
            this.metadata.remove(key);
            this.metadata.putShort(key, data);
        }
        else
        {
            this.metadata.putShort(key, data);
        }
    }
    public Short getMetadataShort(String key)
    {
        if (this.metadata.contains(key))
        {
            return this.metadata.getShort(key);
        }
        else return null;
    }
    protected void updateMetadataUUID(String key, UUID data)
    {
        if (this.metadata.contains(key))
        {
            this.metadata.remove(key);
            this.metadata.putUuid(key, data);
        }
        else
        {
            this.metadata.putUuid(key, data);
        }
    }
    public UUID getMetadataUUID(String key)
    {
        if (this.metadata.contains(key))
        {
            return this.metadata.getUuid(key);
        }
        else return null;
    }

    protected void updateMetadataByteArray(String key, byte[] data)
    {
        if (this.metadata.contains(key))
        {
            this.metadata.remove(key);
            this.metadata.putByteArray(key, data);
        }
        else
        {
            this.metadata.putByteArray(key, data);
        }
    }
    public byte[] getMetadataByteArray(String key)
    {
        if (this.metadata.contains(key))
        {
            return this.metadata.getByteArray(key);
        }
        else return null;
    }

    protected void updateMetadataByteArray(String key, List<Byte> data)
    {
        if (this.metadata.contains(key))
        {
            this.metadata.remove(key);
            this.metadata.putByteArray(key, data);
        }
        else
        {
            this.metadata.putByteArray(key, data);
        }
    }
    protected void updateMetadataIntArray(String key, int[] data)
    {
        if (this.metadata.contains(key))
        {
            this.metadata.remove(key);
            this.metadata.putIntArray(key, data);
        }
        else
        {
            this.metadata.putIntArray(key, data);
        }
    }
    public int[] getMetadataIntArray(String key)
    {
        if (this.metadata.contains(key))
        {
            return this.metadata.getIntArray(key);
        }
        else return null;
    }

    protected void updateMetadataIntArray(String key, List<Integer> data)
    {
        if (this.metadata.contains(key))
        {
            this.metadata.remove(key);
            this.metadata.putIntArray(key, data);
        }
        else
        {
            this.metadata.putIntArray(key, data);
        }
    }
    protected void updateMetadataLongArray(String key, long[] data)
    {
        if (this.metadata.contains(key))
        {
            this.metadata.remove(key);
            this.metadata.putLongArray(key, data);
        }
        else
        {
            this.metadata.putLongArray(key, data);
        }
    }
    public long[] getMetadataLongArray(String key)
    {
        if (this.metadata.contains(key))
        {
            return this.metadata.getLongArray(key);
        }
        else return null;
    }

    protected void updateMetadataLongArray(String key, List<Long> data)
    {
        if (this.metadata.contains(key))
        {
            this.metadata.remove(key);
            this.metadata.putLongArray(key, data);
        }
        else
        {
            this.metadata.putLongArray(key, data);
        }
    }
    protected void updateMetadataNbt(String key, NbtElement data)
    {
        if (this.metadata.contains(key))
        {
            this.metadata.remove(key);
            this.metadata.put(key, data);
        }
        else
        {
            this.metadata.put(key, data);
        }
    }
    public NbtElement getMetadataNbt(String key)
    {
        if (this.metadata.contains(key))
        {
            return this.metadata.get(key);
        }
        else return null;
    }
}
