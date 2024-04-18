package fi.dy.masa.servux.event;

import java.net.SocketAddress;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayerEntity;
import fi.dy.masa.malilib.interfaces.IPlayerListener;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;

public class PlayerListener implements IPlayerListener
{
    @Override
    public void onPlayerJoin(SocketAddress addr, GameProfile profile, ServerPlayerEntity player)
    {
        StructureDataProvider.INSTANCE.register(player, profile);
    }

    @Override
    public void onPlayerLeave(ServerPlayerEntity player)
    {
        StructureDataProvider.INSTANCE.unregister(player);
    }
}
