package fi.dy.masa.servux.dataproviders.client;

import fi.dy.masa.servux.util.PlayerDimensionPosition;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * Interface for logged in client data
 */
public interface IClient
{
    // TODO Generics
    void registerClient(ServerPlayerEntity player);
    void unregisterClient();
    UUID getClientUUID(ServerPlayerEntity player);
    String getClientVersion();
    void setClientVersion(String version);

    // TODO Structures Channel
    void structuresEnableClient();
    void structuresDisableClient();
    boolean isStructuresEnabled();
    boolean isStructuresClient();
    void setClientDimension(PlayerDimensionPosition dim);
    PlayerDimensionPosition getClientDimension();
    void tickClient();

    // TODO Metadata Channel
    void metadataEnableClient();
    void metadataDisableClient();
    boolean isMetadataEnabled();
    boolean isMetadataClient();

    // TODO Litematic Channel
    void litematicsEnableClient();
    void litematicsDisableClient();
    boolean isLitematicsEnabled();
    boolean isLitematicsClient();
}
