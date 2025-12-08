package fi.dy.masa.servux.event;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.ApiStatus;
import com.mojang.authlib.GameProfile;
import fi.dy.masa.servux.interfaces.IPlayerListener;
import fi.dy.masa.servux.interfaces.IPlayerManager;

public class PlayerHandler implements IPlayerManager
{
    private static final PlayerHandler INSTANCE = new PlayerHandler();
    private final List<IPlayerListener> handlers = new ArrayList<>();
    public static IPlayerManager getInstance() { return INSTANCE; }

    @Override
    public void registerPlayerHandler(IPlayerListener handler)
    {
        if (!this.handlers.contains(handler))
        {
            this.handlers.add(handler);
        }
    }

    @Override
    public void unregisterPlayerHandler(IPlayerListener handler)
    {
        this.handlers.remove(handler);
    }

    @ApiStatus.Internal
    public void onClientConnect(SocketAddress addr, NameAndId profile, @Nullable Component result)
    {
        if (!this.handlers.isEmpty())
        {
            for (IPlayerListener handler : this.handlers)
            {
                handler.onClientConnect(addr, profile, result);
            }
        }
    }

    @ApiStatus.Internal
    public void onPlayerJoin(SocketAddress addr, GameProfile profile, ServerPlayer player)
    {
        if (!this.handlers.isEmpty())
        {
            for (IPlayerListener handler : this.handlers)
            {
                handler.onPlayerJoin(addr, profile, player);
            }
        }
    }

    @ApiStatus.Internal
    public void onPlayerRespawn(ServerPlayer newPlayer, ServerPlayer oldPlayer)
    {
        if (!this.handlers.isEmpty())
        {
            for (IPlayerListener handler : this.handlers)
            {
                handler.onPlayerRespawn(newPlayer, oldPlayer);
            }
        }
    }

    @ApiStatus.Internal
    public void onPlayerOp(NameAndId profile, UUID uuid, @Nullable ServerPlayer player)
    {
        if (!this.handlers.isEmpty())
        {
            for (IPlayerListener handler : this.handlers)
            {
                handler.onPlayerOp(profile, uuid, player);
            }
        }
    }

    @ApiStatus.Internal
    public void onPlayerDeOp(NameAndId profile, UUID uuid, @Nullable ServerPlayer player)
    {
        if (!this.handlers.isEmpty())
        {
            for (IPlayerListener handler : this.handlers)
            {
                handler.onPlayerDeOp(profile, uuid, player);
            }
        }
    }

    @ApiStatus.Internal
    public void onPlayerLeave(ServerPlayer player)
    {
        if (!this.handlers.isEmpty())
        {
            for (IPlayerListener handler : this.handlers)
            {
                handler.onPlayerLeave(player);
            }
        }
    }
}
