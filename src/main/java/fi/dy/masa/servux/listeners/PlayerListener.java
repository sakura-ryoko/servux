package fi.dy.masa.servux.listeners;

import fi.dy.masa.malilib.interfaces.IPlayerListener;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.data.StructureDataProvider;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerListener implements IPlayerListener
{
    public void onPlayerJoin(ServerPlayerEntity player)
    {
        Servux.printDebug("PlayerManagerEvents#onPlayerJoin(): invoked.");
        StructureDataProvider.INSTANCE.register(player);
    }
    public void onPlayerLeave(ServerPlayerEntity player)
    {
        Servux.printDebug("PlayerManagerEvents#onPlayerLeave(): invoked.");
        StructureDataProvider.INSTANCE.unregister(player);
    }
}
