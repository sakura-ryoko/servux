package fi.dy.masa.servux.network.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.IServerPayloadData;
import fi.dy.masa.servux.network.PacketSplitter;
import fi.dy.masa.servux.util.data.tag.util.DataByteBufUtils;

@Environment(EnvType.SERVER)
public abstract class ServuxStructuresHandler<T extends CustomPacketPayload> implements IPluginServerPlayHandler<T>
{
	private static final ServuxStructuresHandler<ServuxStructuresPacket.Payload> INSTANCE = new ServuxStructuresHandler<>()
	{
		@Override
		public void receive(ServuxStructuresPacket.@NonNull Payload payload, ServerPlayNetworking.@NotNull Context context)
		{
			ServuxStructuresHandler.INSTANCE.receivePlayPayload(payload, context);
		}
	};

	public static ServuxStructuresHandler<ServuxStructuresPacket.Payload> getInstance() {return INSTANCE;}

	public static final Identifier CHANNEL_ID = Identifier.fromNamespaceAndPath("servux", "structures");

	private boolean payloadRegistered = false;
	private final Map<UUID, Integer> failures = new HashMap<>();
	private final Map<UUID, Long> readingSessionKeys = new HashMap<>();

	@Override
	public Identifier getPayloadChannel() {return CHANNEL_ID;}

	@Override
	public boolean isPlayRegistered(Identifier channel)
	{
		if (channel.equals(CHANNEL_ID))
		{
			return payloadRegistered;
		}

		return false;
	}

	@Override
	public void setPlayRegistered(Identifier channel)
	{
		if (channel.equals(CHANNEL_ID))
		{
			this.payloadRegistered = true;
		}
	}

	@Override
	public <P extends IServerPayloadData> void decodeServerData(Identifier channel, ServerPlayer player, P data)
	{
		if (!channel.equals(CHANNEL_ID))
		{
			return;
		}
		if (!StructureDataProvider.INSTANCE.isEnabled() || !this.checkFailures(player))
		{
			return;
		}

		if (data instanceof ServuxStructuresPacket packet)
		{
			switch (packet.getType())
			{
				// Only NBT type packets are received from MiniHUD, not using PacketSplitter
				case PACKET_C2S_STRUCTURES_REGISTER ->
				{
					Servux.debugLog("ServuxStructuresHandler#decodeServerData(): received Structures Register from player {}", player.getName().tryCollapseToString());

					if (StructureDataProvider.INSTANCE.isPlayerRegistered(player))
					{
						StructureDataProvider.INSTANCE.unregister(player, packet.getCompound());
					}

					StructureDataProvider.INSTANCE.register(player, packet.getCompound());
				}
				case PACKET_C2S_STRUCTURES_UNREGISTER ->
				{
					Servux.debugLog("ServuxStructuresHandler#decodeServerData(): received Structures Un-Register from player {}", player.getName().tryCollapseToString());
					StructureDataProvider.INSTANCE.unregister(player, packet.getCompound());
				}
				default ->
						Servux.LOGGER.warn("ServuxStructuresHandler#decodeServerData(): Invalid packetType '{}' from player: {}, of size in bytes: {}.", packet.getPacketType(), player.getName().tryCollapseToString(), packet.getTotalSize());
			}
		}
	}

	@Override
	public void reset(Identifier channel)
	{
		if (channel.equals(CHANNEL_ID))
		{
			this.failures.clear();
		}
	}

	public void resetFailures(Identifier channel, ServerPlayer player)
	{
		if (channel.equals(CHANNEL_ID))
		{
			this.failures.remove(player.getUUID());
		}
	}

	@Override
	public void receivePlayPayload(T payload, ServerPlayNetworking.Context ctx)
	{
		if (payload.type().id().equals(CHANNEL_ID))
		{
			ServerPlayer player = ctx.player();
			ServuxStructuresHandler.INSTANCE.decodeServerData(CHANNEL_ID, player, ((ServuxStructuresPacket.Payload) payload).data());
		}
	}

	@Override
	public void encodeWithSplitter(ServerPlayer player, FriendlyByteBuf buffer, ServerGamePacketListenerImpl networkHandler)
	{
		ServuxStructuresHandler.INSTANCE.encodeServerData(player, ServuxStructuresPacket.StructuresS2CData(buffer));
	}

	@Override
	public <P extends IServerPayloadData> void encodeServerData(ServerPlayer player, P data)
	{
		if (!StructureDataProvider.INSTANCE.isEnabled() || !this.checkFailures(player))
		{
			return;
		}

		if (data instanceof ServuxStructuresPacket packet)
		{
			if (packet.getType().equals(ServuxStructuresPacket.Type.PACKET_S2C_STRUCTURE_DATA_START))
			{
				try
				{
					// Send Structure Data via Packet Splitter
//					FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
//					buffer.writeNbt(packet.getCompound());
					ByteBuf buffer = DataByteBufUtils.toByteBuf(packet.getCompound(), "");
					PacketSplitter.send(this, new FriendlyByteBuf(buffer), player, player.connection);
					// PacketSplitter releases the ByteBuf at the end
				}
				catch (Exception e)
				{
					Servux.LOGGER.error("ServuxStructuresHandler#encodeServerData(): Exception encoding packet for PacketSplitter; {}", e.getLocalizedMessage());
				}
			}
			else if (!ServuxStructuresHandler.INSTANCE.sendPlayPayload(player, new ServuxStructuresPacket.Payload(packet)))
			{
				this.tickFailures(player);
			}
		}
	}

	@Override
	public boolean checkFailures(ServerPlayer player)
	{
		return !(this.failures.getOrDefault(player.getUUID(), 0) > this.maxFailures());
	}

	@Override
	public void tickFailures(ServerPlayer player)
	{
		UUID uuid = player.getUUID();

		if (!this.failures.containsKey(uuid))
		{
			this.failures.put(uuid, 1);
		}
		else if (this.failures.get(uuid) > this.maxFailures())
		{
			if (Reference.DEBUG_MODE)
			{
				Servux.LOGGER.info("Unregistering Structure Client {} after {} failures (MiniHUD not installed perhaps)", player.getName().tryCollapseToString(), this.maxFailures());
			}

			StructureDataProvider.INSTANCE.onPacketFailure(player);
			// For this one, we'll need to reset the failures after they are unregistered...  Because you know ... design
			this.resetFailures(CHANNEL_ID, player);
		}
		else
		{
			int count = this.failures.get(uuid) + 1;
			this.failures.put(uuid, count);
		}
	}
}
