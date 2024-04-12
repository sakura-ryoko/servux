package fi.dy.masa.servux.dataproviders.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayerEntity;

import java.net.SocketAddress;
import java.util.UUID;

/**
 * Interface for logged in client data
 */
public interface IClient
{
    // Generics
    UUID getClientUUID(ServerPlayerEntity player);
    String getClientVersion();
    SocketAddress getAddr();
    GameProfile getProfile();
    void setClientVersion(String version);
    void registerClient(SocketAddress addr, GameProfile profile, ServerPlayerEntity player);
    void unregisterClient();
    boolean isClientRegistered();
    void enableClient();
    void disableClient();
    boolean isEnabled();
    void tickClient();

    // Structures Channel
    boolean isStructuresClient();
}
