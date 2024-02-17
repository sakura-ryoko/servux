package fi.dy.masa.servux.dataproviders.client;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * Interface for logged in client data
 */
public interface IClient
{
    // Generics
    UUID getClientUUID(ServerPlayerEntity player);
    String getClientVersion();
    void setClientVersion(String version);
    void registerClient(ServerPlayerEntity player);
    void unregisterClient();
    boolean isClientRegistered();
    void enableClient();
    void disableClient();
    boolean isEnabled();
    void tickClient();

    // Structures Channel
    boolean isStructuresClient();

    // Litematics Channel
    boolean isLitematicsClient();

    // Metadata Channel
    boolean isMetadataClient();

    // Blocks Channel
    boolean isBlocksClient();

    // Entities Channel
    boolean isEntitiesClient();
}
