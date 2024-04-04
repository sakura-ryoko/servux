package fi.dy.masa.servux.event;

import com.mojang.authlib.GameProfile;
import fi.dy.masa.malilib.interfaces.IPlayerListener;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.data.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.UUID;

public class PlayerListener implements IPlayerListener
{
    @Override
    public void onClientConnect(SocketAddress addr, GameProfile profile, Text result)
    {
        if (result != null)
        {
            Servux.printDebug("onClientConnect(): received connection from IP {}, profile {} (result: DENIED)", addr.toString(), profile.getName());
            TranslatableTextContent content = (TranslatableTextContent) result.getContent();
            String key = content.getKey();
            String reason = Arrays.toString(content.getArgs());
            Servux.printDebug("onClientConnect(): key {} // reason {}", key, reason);
            // key: translation key (such as banned), reason: if a reason is given.
        }
        else
        {
            Servux.printDebug("onClientConnect(): received connection from IP {}, profile {} (result: SUCCESS)", addr.toString(), profile.getName());
            // We can use this to preload any Clients during Login Phase, maybe populate their IP Address somewhere?
        }
    }

    @Override
    public void onPlayerJoin(SocketAddress addr, GameProfile profile, ServerPlayerEntity player)
    {
        Servux.printDebug("onPlayerJoin(): received connection from IP {}, profile {} // player {}", addr.toString(), profile.getName(), player.getName().getLiteralString());

        StructureDataProvider.INSTANCE.register(player);
    }

    @Override
    public void onPlayerRespawn(ServerPlayerEntity newPlayer, ServerPlayerEntity oldPlayer)
    {
        Servux.printDebug("onPlayerRespawn(): old player {}, new player {}", oldPlayer.getName().getLiteralString(), newPlayer.getName().getLiteralString());
        // This is like for when a player dies, and has their Player Entity regenerated.
    }

    @Override
    public void onPlayerOp(GameProfile profile, UUID uuid, @Nullable ServerPlayerEntity player)
    {
        Servux.printDebug("onPlayerOp(): profile {} // UUID {}", profile.getName(), uuid.toString());
    }

    @Override
    public void onPlayerDeOp(GameProfile profile, UUID uuid, @Nullable ServerPlayerEntity player)
    {
        Servux.printDebug("onPlayerDeOp(): profile {} // UUID {}", profile.getName(), uuid.toString());
    }

    @Override
    public void onPlayerLeave(ServerPlayerEntity player)
    {
        Servux.printDebug("onPlayerLeave(): player {}", player.getName().getLiteralString());
        StructureDataProvider.INSTANCE.unregister(player);
    }

    @Override
    public void onSetSimulDistance(int distance)
    {
        Servux.printDebug("onSetSimulDistance(): detected simulation distance change {}", distance);
    }

    @Override
    public void onSetViewDistance(int distance)
    {
        Servux.printDebug("onSetViewDistance(): detected view distance change {}", distance);
    }
}
