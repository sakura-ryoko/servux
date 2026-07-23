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
import fi.dy.masa.servux.dataproviders.TweaksDataProvider;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.IServerPayloadData;
import fi.dy.masa.servux.network.PacketSplitter;
import fi.dy.masa.servux.util.data.tag.util.DataByteBufUtils;

@Environment(EnvType.SERVER)
public abstract class ServuxTweaksHandler<T extends CustomPacketPayload> implements IPluginServerPlayHandler<T>
{
	private static final ServuxTweaksHandler<ServuxTweaksPacket.Payload> INSTANCE = new ServuxTweaksHandler<>()
	{
		@Override
		public void receive(ServuxTweaksPacket.@NonNull Payload payload, ServerPlayNetworking.@NotNull Context context)
		{
			ServuxTweaksHandler.INSTANCE.receivePlayPayload(payload, context);
		}
	};

	public static ServuxTweaksHandler<ServuxTweaksPacket.Payload> getInstance() {return INSTANCE;}

	public static final Identifier CHANNEL_ID = Identifier.fromNamespaceAndPath("servux", "tweaks");

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
		if (!TweaksDataProvider.INSTANCE.isEnabled() || !this.checkFailures(player))
		{
			return;
		}

		if (data instanceof ServuxTweaksPacket packet)
		{
			switch (packet.getType())
			{
				case PACKET_C2S_METADATA_REQUEST ->
				{
					Servux.debugLog("decodeServerData(): received Tweaks Data Register from player {}", player.getName().tryCollapseToString());
					if (TweaksDataProvider.INSTANCE.isPlayerRegistered(player))
					{
						TweaksDataProvider.INSTANCE.unregister(player, packet.getCompound());
					}

					TweaksDataProvider.INSTANCE.register(player, packet.getCompound());
				}
				case PACKET_C2S_UNREGISTER_REPLY ->
						TweaksDataProvider.INSTANCE.unregister(player, packet.getCompound());
				case PACKET_C2S_BLOCK_ENTITY_REQUEST ->
						TweaksDataProvider.INSTANCE.onBlockEntityRequest(player, packet.getPos(), packet.getCompound());
				case PACKET_C2S_ENTITY_REQUEST ->
						TweaksDataProvider.INSTANCE.onEntityRequest(player, packet.getEntityId(), packet.getCompound());
				default ->
						Servux.LOGGER.warn("ServuxTweaksHandler#decodeServerData(): Invalid packetType '{}' from player: {}, of size in bytes: {}.", packet.getPacketType(), player.getName().tryCollapseToString(), packet.getTotalSize());
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
			ServuxTweaksHandler.INSTANCE.decodeServerData(CHANNEL_ID, player, ((ServuxTweaksPacket.Payload) payload).data());
		}
	}

	@Override
	public void encodeWithSplitter(ServerPlayer player, FriendlyByteBuf buffer, ServerGamePacketListenerImpl networkHandler)
	{
		ServuxTweaksHandler.INSTANCE.sendPlayPayload(player, new ServuxTweaksPacket.Payload(ServuxTweaksPacket.ResponseS2CData(buffer)));
	}

	@Override
	public <P extends IServerPayloadData> void encodeServerData(ServerPlayer player, P data)
	{
		if (!TweaksDataProvider.INSTANCE.isEnabled() || !this.checkFailures(player))
		{
			return;
		}

		if (data instanceof ServuxTweaksPacket packet)
		{
			// Send Response Data via Packet Splitter
			if (packet.getType().equals(ServuxTweaksPacket.Type.PACKET_S2C_NBT_RESPONSE_START))
			{
				try
				{
//                    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
//                    buffer.writeNbt(packet.getCompound());
					ByteBuf buffer = DataByteBufUtils.toByteBuf(packet.getCompound(), "");
					PacketSplitter.send(this, new FriendlyByteBuf(buffer), player, player.connection);
					// PacketSplitter releases the ByteBuf at the end
				}
				catch (Exception e)
				{
					Servux.LOGGER.error("ServuxTweaksHandler#encodeServerData(): Exception encoding packet for PacketSplitter; {}", e.getLocalizedMessage());
				}
			}
			else if (!ServuxTweaksHandler.INSTANCE.sendPlayPayload(player, new ServuxTweaksPacket.Payload(packet)))
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
			if (Reference.DEV_DEBUG)
			{
				Servux.LOGGER.info("Unregistering Tweaks Client {} after {} failures (Tweakeroo not installed perhaps)", player.getName().tryCollapseToString(), this.maxFailures());
			}

			TweaksDataProvider.INSTANCE.onPacketFailure(player);
		}
		else
		{
			int count = this.failures.get(uuid) + 1;
			this.failures.put(uuid, count);
		}
	}
}
