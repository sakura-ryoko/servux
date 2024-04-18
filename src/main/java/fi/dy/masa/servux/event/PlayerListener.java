package fi.dy.masa.servux.event;

import java.net.SocketAddress;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import fi.dy.masa.malilib.interfaces.IPlayerListener;
import fi.dy.masa.malilib.network.payload.PayloadType;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;
import fi.dy.masa.servux.network.TestServerHandler;

public class PlayerListener implements IPlayerListener
{
    @Override
    public void onPlayerJoin(SocketAddress addr, GameProfile profile, ServerPlayerEntity player)
    {
        StructureDataProvider.INSTANCE.register(player, profile);

        NbtCompound nbt = new NbtCompound();
        nbt.putString("message", "join test message from "+ Reference.MOD_STRING);
        TestServerHandler.getInstance().encodeS2CNbtCompound(PayloadType.MALILIB_TEST, nbt, player);
    }

    @Override
    public void onPlayerLeave(ServerPlayerEntity player)
    {
        StructureDataProvider.INSTANCE.unregister(player);
    }
}
