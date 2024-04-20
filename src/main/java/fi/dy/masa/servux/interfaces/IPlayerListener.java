package fi.dy.masa.servux.interfaces;

import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public interface IPlayerListener
{
    default void onClientConnect(SocketAddress addr, GameProfile profile, Text result) {}
    default void onPlayerJoin(SocketAddress addr, GameProfile profile, ServerPlayerEntity player) {}
    default void onPlayerRespawn(ServerPlayerEntity newPlayer, ServerPlayerEntity oldPlayer) {}
    default void onPlayerOp(GameProfile profile, UUID uuid, @Nullable ServerPlayerEntity player) {}
    default void onPlayerDeOp(GameProfile profile, UUID uuid, @Nullable ServerPlayerEntity player) {}
    default void onPlayerLeave(ServerPlayerEntity player) {}
}
