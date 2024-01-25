package fi.dy.masa.servux.network.test;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.PayloadTypeRegister;
import fi.dy.masa.servux.network.handler.ServerNetworkPlayHandler;
import fi.dy.masa.servux.network.payload.*;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class TestSuite {
    public static void testS2C(ServerPlayerEntity player, String msg)
    {
        // Server -> Client
        if (Reference.isServer()) {
            // String test
            Servux.printDebug("TestSuite#testS2C() executing S2CString test packet.");
            StringPayload S2CTest1 = new StringPayload(msg);
            ServerNetworkPlayHandler.send(S2CTest1, player);

            // DATA Test
            Servux.printDebug("TestSuite#testS2C() executing S2CData (String encapsulated) test packet.");
            NbtCompound nbt = new NbtCompound();
            PacketByteBuf buf =  new PacketByteBuf(Unpooled.buffer());
            buf.writeString(msg);
            nbt.putByteArray(DataPayload.NBT, buf.readByteArray());

//            nbt.putString(DataPayload.NBT, msg);
            DataPayload S2CTest2 = new DataPayload(nbt);
            ServerNetworkPlayHandler.send(S2CTest2, player);
        }
        else
            Servux.printDebug("TestSuite#testS2C() called from a Client Environment.");
    }
    public static void initTestSuite()
    {
        // Register Payload types
        PayloadTypeRegister.initTypes(Reference.MOD_ID);
        PayloadTypeRegister.registerPlayChannels();
        // Register test command
        CommandTest.registerCommandTest();
        // Setup callbacks for Server Environment
        // --> Client callbacks come from MixinMinecraftClient
//        if (MaLiLibReference.isServer()) {
//            ServerLifecycleEvents.SERVER_STARTED.register((id) -> ServerEvents.started());
//            ServerLifecycleEvents.SERVER_STOPPING.register((id) -> ServerEvents.stopping());
//        }
    }
}
