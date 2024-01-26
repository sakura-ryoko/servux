package fi.dy.masa.servux.listeners;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.interfaces.IPlayerListener;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerListener implements IPlayerListener
{
    public void onPlayerJoin(ServerPlayerEntity player)
    {
        Servux.printDebug("PlayerManagerEvents#onPlayerJoin(): invoked.");
    }
    public void onPlayerLeave(ServerPlayerEntity player)
    {
        Servux.printDebug("PlayerManagerEvents#onPlayerLeave(): invoked.");
    }
}
