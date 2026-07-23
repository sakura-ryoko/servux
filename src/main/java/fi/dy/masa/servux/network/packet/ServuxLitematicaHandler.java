package fi.dy.masa.servux.network.packet;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.LitematicsDataProvider;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.IServerPayloadData;
import fi.dy.masa.servux.network.PacketSplitter;
import fi.dy.masa.servux.network.PacketSplitterException;
import fi.dy.masa.servux.util.data.tag.BaseData;
import fi.dy.masa.servux.util.data.tag.CompoundData;
import fi.dy.masa.servux.util.data.tag.util.DataByteBufUtils;

@Environment(EnvType.SERVER)
public abstract class ServuxLitematicaHandler<T extends CustomPacketPayload> implements IPluginServerPlayHandler<T>
{
	private static final ServuxLitematicaHandler<ServuxLitematicaPacket.Payload> INSTANCE = new ServuxLitematicaHandler<>()
	{
		@Override
		public void receive(ServuxLitematicaPacket.@NonNull Payload payload, ServerPlayNetworking.@NotNull Context context)
		{
			ServuxLitematicaHandler.INSTANCE.receivePlayPayload(payload, context);
		}
	};

	public static ServuxLitematicaHandler<ServuxLitematicaPacket.Payload> getInstance() {return INSTANCE;}

	public static final Identifier CHANNEL_ID = Identifier.fromNamespaceAndPath("servux", "litematics");

	private boolean payloadRegistered = false;
	private final ConcurrentHashMap<UUID, Integer> failures = new ConcurrentHashMap<>(16, 0.9f, 2);
	private final ConcurrentHashMap<UUID, Long> readingSessionKeys = new ConcurrentHashMap<>(16, 0.9f, 2);

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
		if (!LitematicsDataProvider.INSTANCE.isEnabled() || !this.checkFailures(player))
		{
			return;
		}

		if (data instanceof ServuxLitematicaPacket packet)
		{
			switch (packet.getType())
			{
				case PACKET_C2S_METADATA_REQUEST ->
				{
					Servux.debugLog("decodeServerData(): received Litematic Data Register from player {}", player.getName().tryCollapseToString());
					if (LitematicsDataProvider.INSTANCE.isPlayerRegistered(player))
					{
						LitematicsDataProvider.INSTANCE.unregister(player, packet.getCompound());
					}

					LitematicsDataProvider.INSTANCE.register(player, packet.getCompound());
				}
				case PACKET_C2S_UNREGISTER_REPLY ->
						LitematicsDataProvider.INSTANCE.unregister(player, packet.getCompound());
				// TODO
//				case PACKET_C2S_TASK_REQUEST ->
//						LitematicsDataProvider.INSTANCE.onTaskRequest(player, packet.getCompound());
//				case PACKET_C2S_TASK_CANCEL ->
//						LitematicsDataProvider.INSTANCE.onTaskCancel(player, packet.getCompound());
				case PACKET_C2S_BLOCK_ENTITY_REQUEST ->
						LitematicsDataProvider.INSTANCE.onBlockEntityRequest(player, packet.getPos(), packet.getCompound());
				case PACKET_C2S_ENTITY_REQUEST ->
						LitematicsDataProvider.INSTANCE.onEntityRequest(player, packet.getEntityId(), packet.getCompound());
				case PACKET_C2S_BULK_ENTITY_NBT_REQUEST ->
						LitematicsDataProvider.INSTANCE.onBulkEntityRequest(player, packet.getChunkPos(), packet.getCompound());
				case PACKET_C2S_NBT_RESPONSE_DATA ->
				{
					if (!LitematicsDataProvider.INSTANCE.isPlayerRegistered(player))
					{
						return;
					}
					UUID uuid = player.getUUID();
					long readingSessionKey;

					if (!this.readingSessionKeys.containsKey(uuid))
					{
						readingSessionKey = RandomSource.create(Util.getMillis()).nextLong();
						this.readingSessionKeys.put(uuid, readingSessionKey);
					}
					else
					{
						readingSessionKey = this.readingSessionKeys.get(uuid);
					}

					if (Reference.DEV_DEBUG)
					{
						Servux.LOGGER.info("ServuxLitematicaHandler#decodeServerData(): received Litematic Data Packet Slice of size {} (in bytes) // reading session key [{}]", packet.getTotalSize(), readingSessionKey);
					}
					try
					{
						FriendlyByteBuf fullPacket = PacketSplitter.receive(this, readingSessionKey, packet.getBuffer());

						if (fullPacket != null)
						{
							if (Reference.DEV_DEBUG)
							{
								Servux.LOGGER.info("ServuxLitematicaHandler#decodeServerData(): received Litematic Data Full Packet of size {} (in bytes) // reading session key [{}]", fullPacket.readableBytes(), readingSessionKey);
							}

							try
							{
								final int packetSize = fullPacket.readableBytes();
								this.readingSessionKeys.remove(uuid);
								Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(fullPacket);

								opt.ifPresent(baseData -> this.handleBulkData(player, (CompoundData) baseData, packetSize));
							}
							catch (Exception e)
							{
								Servux.LOGGER.error("ServuxLitematicaHandler#decodeServerData(): Litematic Data: error reading fullBuffer [{}]", e.getLocalizedMessage());
							}
						}
					}
					catch (PacketSplitterException e)
					{
						Servux.LOGGER.warn("ServuxLitematicaHandler#decodeServerData(): PacketSplitter Data exception; {}", e.getLocalizedMessage());
						this.readingSessionKeys.remove(uuid);
					}
					catch (NullPointerException e)
					{
						Servux.LOGGER.warn("ServuxLitematicaHandler#decodeServerData(): PacketSplitter Null exception; {}", e.getLocalizedMessage());
						this.readingSessionKeys.remove(uuid);
					}
				}
				default ->
						Servux.LOGGER.warn("ServuxLitematicaHandler#decodeServerData(): Invalid packetType '{}' from player: {}, of size in bytes: {}.", packet.getPacketType(), player.getName().tryCollapseToString(), packet.getTotalSize());
			}
		}
	}

	private void handleBulkData(ServerPlayer player, CompoundData data, final int packetSize)
	{
		String task = data.getStringOrDefault("Task", "LitematicaPaste");
		Servux.debugLog("handleBulkData: received task: {} from {} [Bytes: {} / {}]", task, player.getName().getString(), packetSize, data.sizeInBytes());

		// For future Granular Task Management
//        switch (task)
//        {
//            // File-Transmit support
//            case "Litematic-TransmitStart", "Litematic-TransmitCancel", "Litematic-TransmitData", "Litematic-TransmitEnd" ->
//            {
//                Pair<LitematicaSchematic, CompoundData> schemPair = LitematicaSchematic.receiveFileTransmit(nbt, player);
//
//                if (schemPair != null && schemPair.getLeft().getFile() != null)
//                {
//                    Servux.debugLog("handleBulkData(): Received litematic '{}' from player {}", schemPair.getLeft().getFile().toAbsolutePath().toString(), player.getName().tryCollapseToString());
//                    LitematicsDataProvider.INSTANCE.handleClientPasteRequestPair(player, schemPair);
//                }
//            }
//            default -> LitematicsDataProvider.INSTANCE.handleClientPasteRequest(player, data);
//        }

		LitematicsDataProvider.INSTANCE.handleClientPasteRequest(player, data);
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
			ServuxLitematicaHandler.INSTANCE.decodeServerData(CHANNEL_ID, player, ((ServuxLitematicaPacket.Payload) payload).data());
		}
	}

	@Override
	public void encodeWithSplitter(ServerPlayer player, FriendlyByteBuf buffer, ServerGamePacketListenerImpl networkHandler)
	{
		ServuxLitematicaHandler.INSTANCE.sendPlayPayload(player, new ServuxLitematicaPacket.Payload(ServuxLitematicaPacket.ResponseS2CData(buffer)));
	}

	@Override
	public <P extends IServerPayloadData> void encodeServerData(ServerPlayer player, P data)
	{
		if (!LitematicsDataProvider.INSTANCE.isEnabled() || !this.checkFailures(player))
		{
			return;
		}

		if (data instanceof ServuxLitematicaPacket packet)
		{
			// Send Response Data via Packet Splitter
			if (packet.getType().equals(ServuxLitematicaPacket.Type.PACKET_S2C_NBT_RESPONSE_START))
			{
				try
				{
					// Send Bulk Data via Packet Splitter
//					FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
//					buffer.writeNbt(packet.getCompound());
					ByteBuf buffer = DataByteBufUtils.toByteBuf(packet.getCompound(), "");
					PacketSplitter.send(this, new FriendlyByteBuf(buffer), player, player.connection);
					// PacketSplitter releases the ByteBuf at the end
				}
				catch (Exception e)
				{
					Servux.LOGGER.error("ServuxLitematicaHandler#encodeServerData(): Exception encoding packet for PacketSplitter; {}", e.getLocalizedMessage());
				}
			}
			else if (!ServuxLitematicaHandler.INSTANCE.sendPlayPayload(player, new ServuxLitematicaPacket.Payload(packet)))
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
				Servux.LOGGER.info("ServuxLitematicaHandler#tickFailures(): Unregistering Litematic Client {} after {} failures (Litematica not installed perhaps)", player.getName().tryCollapseToString(), this.maxFailures());
			}

			LitematicsDataProvider.INSTANCE.onPacketFailure(player);
		}
		else
		{
			int count = this.failures.get(uuid) + 1;
			this.failures.put(uuid, count);
		}
	}
}
