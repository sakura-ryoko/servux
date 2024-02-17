package fi.dy.masa.servux.dataproviders.client;

import fi.dy.masa.servux.util.PlayerDimensionPosition;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * I've created a *MUCH* more flexible way to track individual Clients, and in a standard interface to be used across multiple DataProvider's.
 */
public class MetadataClient extends ClientBase
{
    private boolean registered;
    private boolean enabled;
    public MetadataClient(String name, UUID uuid, @Nullable String version)
    {
        super(name, uuid, version);
    }

    @Override
    public void registerClient(ServerPlayerEntity player)
    {
        this.updateName(player.getName().getLiteralString());
        UUID id = player.getUuid();
        this.updateUUID(id);
        this.registered = true;
        this.enabled = false;
    }

    @Override
    public void unregisterClient()
    {
        this.registered = false;
        this.enabled = false;
    }

    @Override
    public UUID getClientUUID(ServerPlayerEntity player)
    {
        return player.getUuid();
    }

    @Override
    public String getClientVersion()
    {
        return this.getVersion();
    }

    @Override
    public void setClientVersion(String version)
    {
        this.updateVersion(version);
    }

    @Override
    public void metadataEnableClient() { this.enabled = true; }

    @Override
    public void metadataDisableClient() { this.enabled = false; }

    @Override
    public boolean isMetadataEnabled() { return this.enabled; }
    @Override
    public boolean isMetadataClient() { return true; }

    @Override
    public void tickClient()
    {
        // Things to do during a Client Tick
    }

    @Override
    public void litematicsEnableClient() {  }

    @Override
    public void litematicsDisableClient() { }

    @Override
    public boolean isLitematicsEnabled() { return false; }

    @Override
    public boolean isLitematicsClient() { return false; }


    @Override
    public void structuresEnableClient() { }

    @Override
    public void structuresDisableClient() { }

    @Override
    public boolean isStructuresEnabled() { return false; }
    @Override
    public boolean isStructuresClient() { return false; }

    @Override
    public void setClientDimension(PlayerDimensionPosition dim) { }

    @Override
    public PlayerDimensionPosition getClientDimension() { return null; }

}
