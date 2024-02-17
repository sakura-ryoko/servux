package fi.dy.masa.servux.dataproviders.client;

import fi.dy.masa.servux.util.PlayerDimensionPosition;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * I've created a *MUCH* more flexible way to track individual Clients, and in a standard interface to be used across multiple DataProvider's.
 */
public class LitematicClient extends ClientBase
{
    private boolean registered;
    private boolean enabled;
    public LitematicClient(String name, UUID uuid, @Nullable String version)
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
    }

    @Override
    public void unregisterClient()
    {
        this.registered = false;
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
    public void litematicsEnableClient() { this.enabled = true; }

    @Override
    public void litematicsDisableClient() { this.enabled = false; }

    @Override
    public boolean isLitematicsEnabled() {
        return false;
    }

    @Override
    public boolean isLitematicsClient() { return true; }

    @Override
    public void tickClient()
    {
        // Things to do during a Client Tick
    }

    @Override
    public void metadataEnableClient() { }

    @Override
    public void metadataDisableClient() { }

    @Override
    public boolean isMetadataEnabled() { return false; }

    @Override
    public boolean isMetadataClient() { return false; }

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
