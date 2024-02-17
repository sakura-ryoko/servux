package fi.dy.masa.servux.dataproviders.client;

import net.minecraft.server.network.ServerPlayerEntity;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public abstract class ClientBase implements IClient
{
    private String name;
    private UUID uuid;
    private String version;
    private boolean enabled;
    private boolean registered;

    public ClientBase(String name, UUID uuid, @Nullable String version)
    {
        this.name = name;
        this.uuid = uuid;
        this.version = Objects.requireNonNullElse(version, "");
        this.registered = false;
        this.enabled = false;
    }
    public String getName() { return this.name; }
    public UUID getUUID() { return this.uuid; }
    public String getVersion() { return this.version; }
    public void updateName(String name) { this.name = name; }
    public void updateUUID(UUID id) { this.uuid = id; }
    public void updateVersion(String ver) { this.version = ver; }
    public void registerClient(ServerPlayerEntity player)
    {
        updateName(player.getName().getLiteralString());
        updateUUID(player.getUuid());
        this.registered = true;
    }
    protected void setClientRegister(boolean toggle) { this.registered = toggle; }
    public void unregisterClient() { this.registered = false; }
    public boolean isClientRegistered() { return this.registered; }
    public void enableClient() { this.enabled = true; }
    public void disableClient() { this.enabled = false; }
    public boolean isEnabled() { return this.enabled; }
}
