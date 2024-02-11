package fi.dy.masa.servux.network.handlers;

import net.minecraft.network.packet.CustomPayload;

public interface IServerConfigHandler
{
    <P extends CustomPayload> void registerServerConfigHandler(IPluginServerConfigHandler<P> handler);

    <P extends CustomPayload> void unregisterServerConfigHandler(IPluginServerConfigHandler<P> handler);
}
