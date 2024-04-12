package fi.dy.masa.servux.dataproviders.client;

import com.mojang.authlib.GameProfile;
import fi.dy.masa.servux.util.PlayerDimensionPosition;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.util.UUID;

/**
 * I've created a *MUCH* more flexible way to track individual Clients, and in a standard interface to be used across multiple DataProvider's.
 */
public class StructureClient extends ClientBase
{
    private PlayerDimensionPosition dim;
    public NbtCompound enabledStructures;
    public StructureClient(String name, UUID uuid, @Nullable String version)
    {
        super(name, uuid, version);
    }

    @Override
    public void registerClient(SocketAddress addr, GameProfile profile, ServerPlayerEntity player)
    {
        this.updateName(player.getName().getLiteralString());
        UUID id = player.getUuid();
        this.updateUUID(id);
        this.updateAddr(addr);
        this.updateProfile(profile);
        this.dim = new PlayerDimensionPosition(player);
        this.enabledStructures = new NbtCompound();
        this.setClientRegister(true);
    }

    @Override
    public void unregisterClient()
    {
        this.dim = null;
        this.enabledStructures.getKeys().clear();
        this.disableClient();
        this.setClientRegister(false);
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

    public void structuresEnableClient() { this.enableClient(); }

    public void structuresDisableClient()
    {
        this.disableClient();
    }

    public boolean isStructuresEnabled()
    {
        return this.isEnabled();
    }
    public boolean isStructuresClient() { return this.isClientRegistered(); }

    public void setClientDimension(PlayerDimensionPosition dim)
    {
        this.dim = dim;
    }

    public PlayerDimensionPosition getClientDimension()
    {
        return this.dim;
    }

    @Override
    public void tickClient()
    {
        // Things to do during a Client Tick
    }
}
