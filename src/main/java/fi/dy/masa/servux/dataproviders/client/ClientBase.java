package fi.dy.masa.servux.dataproviders.client;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public abstract class ClientBase implements IClient
{
    private String name;
    private UUID uuid;
    private String version;

    public ClientBase(String name, UUID uuid, @Nullable String version)
    {
        this.name = name;
        this.uuid = uuid;
        this.version = Objects.requireNonNullElse(version, "");
    }
    public String getName() { return this.name; }
    public UUID getUUID() { return this.uuid; }
    public String getVersion() { return this.version; }
    protected void updateName(String name) { this.name = name; }
    protected void updateUUID(UUID id) { this.uuid = id; }
    protected void updateVersion(String ver) { this.version = ver; }
}
