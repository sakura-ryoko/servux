package fi.dy.masa.servux.network.test;

import fi.dy.masa.servux.ServuxReference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.ServerNetworkPlayHandler;
import fi.dy.masa.servux.network.payload.DataPayload;
import fi.dy.masa.servux.network.payload.StringPayload;
import fi.dy.masa.servux.util.PayloadUtils;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Iterator;
import java.util.Set;

public class ServerDebugSuite {
    public static void checkGlobalChannels() {
        Servux.printDebug("ServerDebugSuite#checkGlobalChannels(): Start.");
        Set<Identifier> channels = ServerPlayNetworking.getGlobalReceivers();
        Iterator<Identifier> iterator = channels.iterator();
        int i = 0;
        while (iterator.hasNext())
        {
            Identifier id = iterator.next();
            i++;
            Servux.printDebug("ServerDebugSuite#checkGlobalChannels(): id("+i+") hash: "+id.hashCode()+" //name: "+id.getNamespace()+" path: "+id.getPath());
        }
        Servux.printDebug("ServerDebugSuite#checkGlobalChannels(): END. Total Channels: "+i);
    }
    public static void testS2C(ServerPlayerEntity player, String msg)
    {
        // Server -> Client
        if (ServuxReference.isServer()) {
            // String test
            Servux.printDebug("TestSuite#testS2C() executing S2CString test packet.");
            StringPayload S2CTest1 = new StringPayload(msg);
            ServerNetworkPlayHandler.sendString(S2CTest1, player);

            // DATA Test
            Servux.printDebug("TestSuite#testS2C() executing S2CData (String encapsulated) test packet.");
            PacketByteBuf buf =  new PacketByteBuf(Unpooled.buffer());
            buf.writeString(msg);
            NbtCompound nbt = PayloadUtils.fromByteBuf(buf, DataPayload.KEY);

//            nbt.putString(DataPayload.NBT, msg);
            DataPayload S2CTest2 = new DataPayload(nbt);
            ServerNetworkPlayHandler.sendData(S2CTest2, player);
        }
        else
            Servux.printDebug("TestSuite#testS2C() called from a Client Environment.");
    }
}
