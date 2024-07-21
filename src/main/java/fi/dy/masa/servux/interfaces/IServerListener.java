package fi.dy.masa.servux.interfaces;

import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;

public interface IServerListener
{
    /**
     * Called at the initial occurrence of a MinecraftServer is starting up
     * @param server (The MinecraftServer object)
     */
    default void onServerStarting(MinecraftServer server) {}

    /**
     * Called when the local MinecraftServer is finished starting
     * @param server (The MinecraftServer object)
     */
    default void onServerStarted(MinecraftServer server) {}

    /**
     * Called when the Resources (Data Packs) are starting to be reloaded
     * @param server (The MinecraftServer object)
     * @param resourceManager (The ResourceManager Object)
     */
    default void onServerResourceReloadPre(MinecraftServer server, ResourceManager resourceManager) {}

    /**
     * Called when the Resources (Data Packs) are finished being reloaded
     * @param server (The MinecraftServer object)
     */
    default void onServerResourceReloadPost(MinecraftServer server, ResourceManager resourceManager, boolean success) {}

    /**
     * Called when the local MinecraftServer enters its initial "stopping" state
     * @param server (The MinecraftServer object)
     */
    default void onServerStopping(MinecraftServer server) {}

    /**
     * Called when the local MinecraftServer finishes it's "stopped" state and before the "server" object itself is killed.
     * @param server (The MinecraftServer object)
     */
    default void onServerStopped(MinecraftServer server) {}
}
