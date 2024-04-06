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
            Servux.printDebug("onClientConnect(): received connection from Socket {}, profile {}, uuid {} (result: DENIED)", addr.toString(), profile.getName(), profile.getId());
            TranslatableTextContent content = (TranslatableTextContent) result.getContent();
            String key = content.getKey();
            String reason = Arrays.toString(content.getArgs());
            Servux.printDebug("onClientConnect(): [DENIED] key {} // reason {}", key, reason);
            // key = translation key (such as banned), reason: if a reason is given.

            // We can use this to preload any Clients during Login Phase,
            // Maybe populate their IP Address into some kind of Auto-IP-Ban feature
            // if they are denied login too many times?
        }
        else
        {
            Servux.printDebug("onClientConnect(): received connection from Socket {}, profile {}, uuid {} (result: SUCCESS)", addr.toString(), profile.getName(), profile.getId());
        }
    }

    @Override
    public void onPlayerJoin(SocketAddress addr, GameProfile profile, ServerPlayerEntity player)
    {
        Servux.printDebug("onPlayerJoin(): received connection from Socket {}, profile {} // player {}", addr.toString(), profile.getId(), player.getName().getLiteralString());

        StructureDataProvider.INSTANCE.register(addr, profile, player);
    }

    @Override
    public void onPlayerRespawn(ServerPlayerEntity newPlayer, ServerPlayerEntity oldPlayer)
    {
        Servux.printDebug("onPlayerRespawn(): old player {}, new player {}", oldPlayer.getName().getLiteralString(), newPlayer.getName().getLiteralString());
        // This is for when a player dies, and has their Player Entity regenerated into a new ServerPlayerEntity
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
