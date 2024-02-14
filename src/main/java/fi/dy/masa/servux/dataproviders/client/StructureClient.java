package fi.dy.masa.servux.dataproviders.client;

import fi.dy.masa.servux.util.PlayerDimensionPosition;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * I've created a *MUCH* more flexible way to track individual Clients, and in a standard interface to be used across multiple DataProvider's.
 */
public class StructureClient extends ClientBase
{
    private boolean registered;
    private boolean enabled;
    private PlayerDimensionPosition dim;
    public StructureClient(String name, UUID uuid, @Nullable String version, @Nullable NbtCompound metadata)
    {
        super(name, uuid, version, metadata);
    }

    @Override
    public void registerClient(ServerPlayerEntity player)
    {
        this.updateName(player.getName().getLiteralString());
        UUID id = player.getUuid();
        this.updateUUID(id);
        this.registered = true;
        this.enabled = false;
        this.dim = new PlayerDimensionPosition(player);
    }

    @Override
    public void unregisterClient()
    {
        this.registered = false;
        this.enabled = false;
        this.dim = null;
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
    public void copyClientMetadata(NbtCompound metadata)
    {
        if (!metadata.isEmpty())
        {
            this.copyMetadata(metadata);
        }
    }

    @Override
    public NbtCompound getClientMetadata()
    {
        return this.getMetadata();
    }

    @Override
    public void structuresEnableClient()
    {
        this.enabled = true;
    }

    @Override
    public void structuresDisableClient()
    {
        this.enabled = false;
    }

    @Override
    public boolean isStructuresEnabled()
    {
        return this.enabled;
    }
    @Override
    public boolean isStructuresClient()
    {
        return this.registered;
    }

    @Override
    public void setClientDimension(PlayerDimensionPosition dim)
    {
        this.dim = dim;
    }

    @Override
    public PlayerDimensionPosition getClientDimension()
    {
        return this.dim;
    }

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
    public void litematicsEnableClient() {  }

    @Override
    public void litematicsDisableClient() { }

    @Override
    public boolean isLitematicsEnabled() { return false; }

    @Override
    public boolean isLitematicsClient() { return false; }
}
