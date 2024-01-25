package fi.dy.masa.servux.network;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.payload.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class PayloadTypeRegister
{
    // This is how it looks in the static context per a MOD, which must each include its own Custom Payload Records.
    // The send/receive handlers can be made into an interface.
    public static final int MAX_TOTAL_PER_PACKET_S2C = 1048576;
    public static final int MAX_TOTAL_PER_PACKET_C2S = 32767;
    private static final Map<PayloadTypes.PayloadType, PayloadTypes> TYPES = new HashMap<>();
    public static void registerPlayChannels()
    {
        Servux.printDebug("PayloadTypeRegister#registerPlayChannels(): registering play channels.");
        PayloadTypeRegistry.playC2S().register(DataPayload.TYPE, DataPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StringPayload.TYPE, StringPayload.CODEC);
        //PayloadTypeRegistry.playC2S().register(CarpetPayload.TYPE, CarpetPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(DataPayload.TYPE, DataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StringPayload.TYPE, StringPayload.CODEC);
        //PayloadTypeRegistry.playS2C().register(CarpetPayload.TYPE, CarpetPayload.CODEC);
        // For Carpet "hello" packet (NbtCompound type)
    }
    public static Identifier getIdentifier(PayloadTypes.PayloadType type)
    {
        Identifier id = TYPES.get(type).getIdentifier();
        Servux.printDebug("PayloadTypeRegister#getIdentifier(): type: {}, id: {}.", type, id);
        return id;
    }
    public static void initTypes(String namespace)
    {
        Servux.printDebug("PayloadTypeRegister#initTypes(): init PayloadTypes for {}.", namespace);
        TYPES.put(PayloadTypes.PayloadType.STRING, new PayloadTypes(PayloadTypes.PayloadType.STRING, namespace));
        TYPES.put(PayloadTypes.PayloadType.DATA,   new PayloadTypes(PayloadTypes.PayloadType.DATA,   namespace));
        TYPES.put(PayloadTypes.PayloadType.CARPET_HELLO,   new PayloadTypes(PayloadTypes.PayloadType.CARPET_HELLO, "carpet"));
        // For Carpet "hello" packet (NbtCompound type)
    }
}
