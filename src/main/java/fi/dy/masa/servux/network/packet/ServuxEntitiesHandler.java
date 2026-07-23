package fi.dy.masa.servux.network.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
import fi.dy.masa.servux.dataproviders.EntitiesDataProvider;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.IServerPayloadData;

@Environment(EnvType.SERVER)
public abstract class ServuxEntitiesHandler<T extends CustomPacketPayload> implements IPluginServerPlayHandler<T>
{
	private static final ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> INSTANCE = new ServuxEntitiesHandler<>()
	{
		@Override
		public void receive(ServuxEntitiesPacket.@NonNull Payload payload, ServerPlayNetworking.@NotNull Context context)
		{
			ServuxEntitiesHandler.INSTANCE.receivePlayPayload(payload, context);
		}
	};

	public static ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> getInstance() {return INSTANCE;}

	public static final Identifier CHANNEL_ID = Identifier.fromNamespaceAndPath("servux", "entity_data");

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
		if (!EntitiesDataProvider.INSTANCE.isEnabled() || !this.checkFailures(player))
		{
			return;
		}

		if (data instanceof ServuxEntitiesPacket packet)
		{
			switch (packet.getType())
			{
				case PACKET_C2S_METADATA_REQUEST ->
				{
					Servux.debugLog("decodeServerData(): received Entity Data Register from player {}", player.getName().tryCollapseToString());

					if (EntitiesDataProvider.INSTANCE.isPlayerRegistered(player))
					{
						EntitiesDataProvider.INSTANCE.unregister(player, packet.getCompound());
					}

					EntitiesDataProvider.INSTANCE.register(player, packet.getCompound());
				}
				case PACKET_C2S_UNREGISTER_REPLY ->
						EntitiesDataProvider.INSTANCE.unregister(player, packet.getCompound());
				case PACKET_C2S_BLOCK_ENTITY_REQUEST ->
						EntitiesDataProvider.INSTANCE.onBlockEntityRequest(player, packet.getPos(), packet.getCompound());
				case PACKET_C2S_ENTITY_REQUEST ->
						EntitiesDataProvider.INSTANCE.onEntityRequest(player, packet.getEntityId(), packet.getCompound());
				default ->
						Servux.LOGGER.warn("ServuxEntitiesHandler#decodeServerData(): Invalid packetType '{}' from player: {}, of size in bytes: {}.", packet.getPacketType(), player.getName().tryCollapseToString(), packet.getTotalSize());
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
			ServuxEntitiesHandler.INSTANCE.decodeServerData(CHANNEL_ID, player, ((ServuxEntitiesPacket.Payload) payload).data());
		}
	}

	@Override
	public void encodeWithSplitter(ServerPlayer player, FriendlyByteBuf buffer, ServerGamePacketListenerImpl networkHandler)
	{
		ServuxEntitiesHandler.INSTANCE.sendPlayPayload(player, new ServuxEntitiesPacket.Payload(ServuxEntitiesPacket.ResponseS2CData(buffer)));
	}

	@Override
	public <P extends IServerPayloadData> void encodeServerData(ServerPlayer player, P data)
	{
		if (!EntitiesDataProvider.INSTANCE.isEnabled() || !this.checkFailures(player))
		{
			return;
		}

		if (data instanceof ServuxEntitiesPacket packet)
		{
			if (!ServuxEntitiesHandler.INSTANCE.sendPlayPayload(player, new ServuxEntitiesPacket.Payload(packet)))
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
				Servux.LOGGER.info("Unregistering Entity Data Client {} after {} failures (MiniHUD not installed perhaps)", player.getName().tryCollapseToString(), this.maxFailures());
			}

			EntitiesDataProvider.INSTANCE.onPacketFailure(player);
		}
		else
		{
			int count = this.failures.get(uuid) + 1;
			this.failures.put(uuid, count);
		}
	}
}
