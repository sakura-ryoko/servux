package fi.dy.masa.servux.dataproviders.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayerEntity;

import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.util.UUID;

/**
 * I've created a *MUCH* more flexible way to track individual Clients, and in a standard interface to be used across multiple DataProvider's.
 */
public class MetadataClient extends ClientBase
{
    public MetadataClient(String name, UUID uuid, @Nullable String version)
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
        this.setClientRegister(true);
    }
    @Override
    public void unregisterClient()
    {
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

    public void metadataEnableClient() { this.enableClient(); }

    public void metadataDisableClient() { this.disableClient(); }

    public boolean isMetadataEnabled() { return this.isEnabled(); }
    @Override
    public boolean isMetadataClient() { return this.isClientRegistered(); }

    @Override
    public void tickClient()
    {
        // Things to do during a Client Tick
    }

    @Override
    public boolean isBlocksClient() { return false; }
    @Override
    public boolean isEntitiesClient() { return false; }
    @Override
    public boolean isLitematicsClient() { return false; }
    @Override
    public boolean isStructuresClient() { return false; }
}
