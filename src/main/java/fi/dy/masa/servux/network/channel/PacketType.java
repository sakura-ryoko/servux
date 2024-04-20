package fi.dy.masa.servux.network.channel;

import net.minecraft.util.Identifier;

public class PacketType
{
    public record Structures()
    {
        public static final Identifier CHANNEL_ID = new Identifier("servux", "structures");
        public static final int PROTOCOL_VERSION = 2;
        public static final int PACKET_S2C_METADATA = 1;
        public static final int PACKET_S2C_STRUCTURE_DATA = 2;
        public static final int PACKET_C2S_STRUCTURES_REGISTER = 3;
        public static final int PACKET_C2S_STRUCTURES_UNREGISTER = 4;
        public static final int PACKET_S2C_SPAWN_METADATA = 10;
        public static final int PACKET_C2S_REQUEST_SPAWN_METADATA = 11;
    }
}
