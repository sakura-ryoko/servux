package fi.dy.masa.servux.event;

import java.net.SocketAddress;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayerEntity;
import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;
import fi.dy.masa.servux.interfaces.IPlayerListener;

public class PlayerListener implements IPlayerListener
{
    @Override
    public void onPlayerJoin(SocketAddress addr, GameProfile profile, ServerPlayerEntity player)
    {
        StructureDataProvider.INSTANCE.register(player);
        EntitiesDataProvider.INSTANCE.sendMetadata(player);
    }

    @Override
    public void onPlayerLeave(ServerPlayerEntity player)
    {
        StructureDataProvider.INSTANCE.unregister(player);
    }
}
