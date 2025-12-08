package fi.dy.masa.servux.interfaces;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import java.net.SocketAddress;
import java.util.UUID;
import com.mojang.authlib.GameProfile;

public interface IPlayerListener
{
    default void onClientConnect(SocketAddress addr, NameAndId profile, Component result) {}
    default void onPlayerJoin(SocketAddress addr, GameProfile profile, ServerPlayer player) {}
    default void onPlayerRespawn(ServerPlayer newPlayer, ServerPlayer oldPlayer) {}
    default void onPlayerOp(NameAndId profile, UUID uuid, @Nullable ServerPlayer player) {}
    default void onPlayerDeOp(NameAndId profile, UUID uuid, @Nullable ServerPlayer player) {}
    default void onPlayerLeave(ServerPlayer player) {}
}
