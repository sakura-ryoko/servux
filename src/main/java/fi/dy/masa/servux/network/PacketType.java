package fi.dy.masa.servux.network;

public class PacketType
{
    public record Structures()
    {
        public static final int PROTOCOL_VERSION = 2;
        public static final int PACKET_S2C_METADATA = 1;
        public static final int PACKET_C2S_REQUEST_METADATA = 2;
        public static final int PACKET_C2S_STRUCTURES_ACCEPT = 3;
        public static final int PACKET_C2S_STRUCTURES_DECLINED = 4;
        public static final int PACKET_S2C_STRUCTURE_DATA = 5;
        public static final int PACKET_C2S_STRUCTURE_TOGGLE = 6;
        public static final int PACKET_S2C_SPAWN_METADATA = 10;
        public static final int PACKET_C2S_REQUEST_SPAWN_METADATA = 11;
    }
    public record Metadata()
    {
        public static final int PROTOCOL_VERSION = 0;
    }
    public record Entities()
    {
        public static final int PROTOCOL_VERSION = 0;
    }
    public record Blocks()
    {
        public static final int PROTOCOL_VERSION = 0;
    }
    public record Litematics()
    {
        public static final int PROTOCOL_VERSION = 0;
        public static final int PACKET_REGISTER_VERSION = 1;
        public static final int PACKET_FEATURE_REQUEST = 2;
        public static final int PACKET_SEND_FEATURES = 3;
        public static final int PACKET_CONFIRM_PARTNER = 4;
        public static final int PACKET_REGISTER_METADATA = 10;
        public static final int PACKET_REQUEST_METADATA = 11;
        public static final int PACKET_REQUEST_LITEMATIC = 12;
        public static final int PACKET_SEND_LITEMATIC = 13;
        public static final int PACKET_RECEIVE_LITEMATIC = 14;
        public static final int PACKET_FINISHED_LITEMATIC = 15;
        public static final int PACKET_CANCEL_LITEMATIC = 16;
        public static final int PACKET_REMOVE_LITEMATIC = 17;
        public static final int PACKET_CANCEL_SHARE = 18;
        public static final int PACKET_SEND_MODIFY = 20;
        public static final int PACKET_REQUEST_MODIFY = 21;
        public static final int PACKET_REQUEST_MODIFY_ACCEPT = 22;
        public static final int PACKET_REQUEST_MODIFY_DENY = 23;
        public static final int PACKET_FINISH_MODIFY = 24;
        public static final int PACKET_MESSAGE = 30;
    }
}
