package fi.dy.masa.servux.servux;

import java.net.SocketAddress;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayerEntity;

import fi.dy.masa.servux.dataproviders.*;
import fi.dy.masa.servux.interfaces.IPlayerListener;

public class PlayerListener implements IPlayerListener
{
    @Override
    public void onPlayerJoin(SocketAddress addr, GameProfile profile, ServerPlayerEntity player)
    {
        if (HudDataProvider.INSTANCE.isEnabled())
        {
            HudDataProvider.INSTANCE.register(player);
        }

        if (StructureDataProvider.INSTANCE.isEnabled())
        {
            StructureDataProvider.INSTANCE.register(player);
        }

        if (EntitiesDataProvider.INSTANCE.isEnabled())
        {
            EntitiesDataProvider.INSTANCE.register(player);
        }

        if (LitematicsDataProvider.INSTANCE.isEnabled())
        {
            LitematicsDataProvider.INSTANCE.registerPlayer(player);
        }

        if (TweaksDataProvider.INSTANCE.isEnabled())
        {
            TweaksDataProvider.INSTANCE.register(player);
        }

        if (DebugDataProvider.INSTANCE.isEnabled())
        {
            DebugDataProvider.INSTANCE.register(player);
        }
    }

    @Override
    public void onPlayerLeave(ServerPlayerEntity player)
    {
        if (HudDataProvider.INSTANCE.isEnabled())
        {
            HudDataProvider.INSTANCE.removePlayer(player);
        }

        if (StructureDataProvider.INSTANCE.isEnabled())
        {
            StructureDataProvider.INSTANCE.unregister(player);
        }

        if (EntitiesDataProvider.INSTANCE.isEnabled())
        {
            EntitiesDataProvider.INSTANCE.removePlayer(player);
        }

        if (LitematicsDataProvider.INSTANCE.isEnabled())
        {
            LitematicsDataProvider.INSTANCE.removePlayer(player);
        }

        if (TweaksDataProvider.INSTANCE.isEnabled())
        {
            TweaksDataProvider.INSTANCE.removePlayer(player);
        }

        if (DebugDataProvider.INSTANCE.isEnabled())
        {
            DebugDataProvider.INSTANCE.unregister(player);
        }
    }
}
