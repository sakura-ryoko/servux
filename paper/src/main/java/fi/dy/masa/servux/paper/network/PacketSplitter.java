package fi.dy.masa.servux.paper.network;

import java.util.function.Function;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Send-only mirror of Servux's {@code PacketSplitter} (Fabric side), shared by both the
 * {@code structures} channel (structure-start NBT lists) and the {@code hud_data} channel
 * (recipe manager dump) - both payloads can exceed a single plugin-message packet.
 * <p>
 * The chunking algorithm and size limit must match the Fabric implementation exactly, since the
 * MiniHUD client reassembles fragments using the same fixed-size scheme: the first fragment is
 * prefixed with a varint of the total payload length, and all fragments (including the first)
 * are wrapped as raw-byte packets (the caller-supplied {@code fragmentEncoder} determines which
 * channel-specific packet type wraps each fragment).
 *
 * @see <a href="../../../../../../../../../../src/main/java/fi/dy/masa/servux/network/PacketSplitter.java">PacketSplitter.java (Fabric reference)</a>
 */
public final class PacketSplitter
{
    public static final int MAX_TOTAL_PER_PACKET_S2C = 1048576;
    public static final int MAX_PAYLOAD_PER_PACKET_S2C = MAX_TOTAL_PER_PACKET_S2C - 5;

    private PacketSplitter()
    {
    }

    /**
     * @param fragmentEncoder wraps a single fragment's raw bytes into the full wire bytes of the
     *                        channel-specific "raw data" packet type (e.g.
     *                        {@code ServuxStructuresPacket.StructureDataFragment(bytes).toBytes()}
     *                        or {@code ServuxHudPacket.ResponseS2CData(bytes).toBytes()}).
     */
    public static void send(String channel, Plugin plugin, Player player, byte[] payload, Function<byte[], byte[]> fragmentEncoder)
    {
        int len = payload.length;

        for (int offset = 0; offset < len; offset += MAX_PAYLOAD_PER_PACKET_S2C)
        {
            int thisLen = Math.min(len - offset, MAX_PAYLOAD_PER_PACKET_S2C);
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(thisLen + 5));

            if (offset == 0)
            {
                buf.writeVarInt(len);
            }

            buf.writeBytes(payload, offset, thisLen);

            byte[] fragment = fragmentEncoder.apply(ByteBufUtil.getBytes(buf));
            player.sendPluginMessage(plugin, channel, fragment);
        }
    }
}
