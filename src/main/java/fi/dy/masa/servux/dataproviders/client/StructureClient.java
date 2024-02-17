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
    public NbtCompound enabledStructures;
    public StructureClient(String name, UUID uuid, @Nullable String version)
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
        this.dim = new PlayerDimensionPosition(player);
        this.enabledStructures = new NbtCompound();
    }

    @Override
    public void unregisterClient()
    {
        this.registered = false;
        this.enabled = false;
        this.dim = null;
        this.enabledStructures.getKeys().clear();
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

    public void setEnabledStructures(NbtCompound data)
    {
        this.enabledStructures.copyFrom(data);
    }

    public NbtCompound getEnabledStructures()
    {
        return this.enabledStructures;
    }

    public void updateEnabledStructures(NbtCompound data)
    {
        this.enabledStructures.getKeys().clear();
        this.enabledStructures.copyFrom(data);
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
