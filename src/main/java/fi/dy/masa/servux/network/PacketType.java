package fi.dy.masa.servux.network;

public class PacketType
{
    public record Structures()
    {
        public static final int PROTOCOL_VERSION = 2;
        public static final int PACKET_S2C_METADATA = 1;
        public static final int PACKET_S2C_STRUCTURE_DATA = 2;
        public static final int PACKET_C2S_REQUEST_METADATA = 3;
        public static final int PACKET_C2S_STRUCTURES_ACCEPT = 4;
        public static final int PACKET_C2S_STRUCTURES_DECLINED = 5;
        public static final int PACKET_S2C_SPAWN_METADATA = 10;
        public static final int PACKET_C2S_REQUEST_SPAWN_METADATA = 11;
    }
}
