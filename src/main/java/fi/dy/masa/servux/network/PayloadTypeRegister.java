package fi.dy.masa.servux.network;

import fi.dy.masa.servux.ServuxReference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.payload.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class PayloadTypeRegister
{
    // This is how it looks in the static context per a MOD, which must each include its own Custom Payload Records.
    // --> The send/receive handlers can be made into an interface.
    private static final Map<PayloadType, PayloadCodec> TYPES = new HashMap<>();

    public static Identifier getIdentifier(PayloadType type)
    {
        return TYPES.get(type).getId();
    }
    public static String getKey(PayloadType type)
    {
        return TYPES.get(type).getKey();
    }
    public static void registerDefaultType(PayloadType type, String key, String namespace)
    {
        if (!TYPES.containsKey(type))
        {
            PayloadCodec codec = new PayloadCodec(type, key, namespace);
            TYPES.put(type, codec);
            Servux.printDebug("PayloadTypeRegister#registerDefaultType(): Successfully registered new Payload id: {} // {}:{}", codec.getId().hashCode(), codec.getId().getNamespace(), codec.getId().getPath());
        }
    }
    public static void registerType(PayloadType type, String key, String namespace, String path)
    {
        if (!TYPES.containsKey(type))
        {
            PayloadCodec codec = new PayloadCodec(type, key, namespace, path);
            TYPES.put(type, codec);
            Servux.printDebug("PayloadTypeRegister#registerDefaultType(): Successfully registered new Payload id: {} // {}:{}", codec.getId().hashCode(), codec.getId().getNamespace(), codec.getId().getPath());
        }
    }
    public static void registerDefaultTypes(String name)
    {
        Servux.printDebug("PayloadTypeRegister#registerDefaultTypes(): executing.");

        String namespace = name;
        if (namespace.isEmpty())
            namespace = ServuxReference.COMMON_NAMESPACE;

        //registerDefaultType(PayloadType.STRING, "string", namespace);
        //registerDefaultType(PayloadType.DATA, "data", namespace);
        //registerType(PayloadType.CARPET_HELLO, "hello", "carpet", "hello");
        // For Carpet "hello" packet (NbtCompound type)
        registerType(PayloadType.SERVUX, "structure_bounding_boxes", "servux", "structures");
        //registerType(PayloadType.SYNCMATICA, "syncmatic", "syncmatica", "syncmatics");
    }
    public static <T extends CustomPayload> void registerDefaultPlayChannel(CustomPayload.Id<T> id, PacketCodec<PacketByteBuf, T> codec)
    {
        PayloadTypeRegistry.playC2S().register(id, codec);
        PayloadTypeRegistry.playS2C().register(id, codec);
    }
    public static void registerDefaultPlayChannels()
    {
        Servux.printDebug("PayloadTypeRegister#registerPlayChannels(): registering play channels.");
        //registerDefaultPlayChannel(DataPayload.TYPE, DataPayload.CODEC);
        //registerDefaultPlayChannel(StringPayload.TYPE, StringPayload.CODEC);
        //registerDefaultPlayChannel(CarpetPayload.TYPE, CarpetPayload.CODEC);
        registerDefaultPlayChannel(ServuxPayload.TYPE, ServuxPayload.CODEC);
        //registerDefaultPlayChannel(SyncmaticaPayload.TYPE, SyncmaticaPayload.CODEC);
    }
}
