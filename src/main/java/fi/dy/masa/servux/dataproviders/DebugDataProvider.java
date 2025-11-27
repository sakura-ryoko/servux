package fi.dy.masa.servux.dataproviders;

@Deprecated
public class DebugDataProvider
//		extends DataProviderBase
{
//    public static final DebugDataProvider INSTANCE = new DebugDataProvider();
//
//    protected final static ServuxDebugHandler<ServuxDebugPacket.Payload> HANDLER = ServuxDebugHandler.getInstance();
//    protected final HashMap<UUID, NbtCompound> registeredPlayers = new HashMap<>();
//    protected final NbtCompound metadata = new NbtCompound();
//
//    private final ServuxIntSetting basePermissionLevel = new ServuxIntSetting(this, "permission_level", 2, 4, 0);
//    //private final ServuxStringListSetting enabledDebugPackets = new ServuxStringListSetting(this, "debug_enabled", List.of("chunk_watcher", "poi", "pathfinding", "neighbor_update", "structures", "goal_selector", "raids", "brain", "bees", "breeze", "game_event"));
//    private final List<IServuxSetting<?>> settings = List.of(this.basePermissionLevel);
//	private final HashMap<UUID, List<DebugRenderType>> enabledRenderers = new HashMap<>();
//
//    protected DebugDataProvider()
//    {
//        super("debug_data",
//              ServuxDebugHandler.CHANNEL_ID,
//              ServuxDebugPacket.PROTOCOL_VERSION,
//              2, Reference.MOD_ID + ".provider.debug_data",
//              "Vanilla Debug Data provider.");
//
//        this.metadata.putString("name", this.getName());
//        this.metadata.putString("id", this.getNetworkChannel().toString());
//        this.metadata.putInt("version", this.getProtocolVersion());
//        this.metadata.putString("servux", Reference.MOD_STRING);
//    }
//
//	@Override
//	public void setEnabled(boolean toggle)
//	{
//		super.setEnabled(toggle);
//		this.toggleDebugRendering(toggle);
//	}
//
//	private void toggleDebugRendering(boolean toggle)
//	{
//		this.toggleRenderer(DebugRenderType.DEBUG_ENABLED, toggle);
//	}
//
//	@Override
//    public void registerHandler()
//    {
//        ServerPlayHandler.getInstance().registerServerPlayHandler(HANDLER);
//        if (this.isRegistered() == false)
//        {
//            HANDLER.registerPlayPayload(ServuxDebugPacket.Payload.ID, ServuxDebugPacket.Payload.CODEC, IPluginServerPlayHandler.BOTH_SERVER);
//            this.setRegistered(true);
//        }
//        HANDLER.registerPlayReceiver(ServuxDebugPacket.Payload.ID, HANDLER::receivePlayPayload);
//    }
//
//    @Override
//    public void unregisterHandler()
//    {
//        HANDLER.unregisterPlayReceiver();
//        ServerPlayHandler.getInstance().unregisterServerPlayHandler(HANDLER);
//    }
//
//    @Override
//    public IPluginServerPlayHandler<ServuxDebugPacket.Payload> getPacketHandler()
//    {
//        return HANDLER;
//    }
//
//    @Override
//    public boolean isPlayerRegistered(ServerPlayerEntity player)
//    {
//        return this.registeredPlayers.containsKey(player.getUuid());
//    }
//
//    @Override
//    public void onTickEndPre()
//    {
//        // NO-OP
//    }
//
//    @Override
//    public void onTickEndPost()
//    {
//        // NO-OP
//    }
//
//    @Override
//    public List<IServuxSetting<?>> getSettings()
//    {
//        return settings;
//    }
//
//    @Override
//    public boolean hasPermission(ServerPlayerEntity player)
//    {
//        if (player == null)
//        {
//            return false;
//        }
//
//        return Permissions.check(player, Reference.MOD_ID + ".debug_data", this.basePermissionLevel.getValue());
//    }
//
//    public boolean register(ServerPlayerEntity player)
//    {
//        return this.register(player, null);
//    }
//
//    public boolean register(ServerPlayerEntity player, @Nullable NbtCompound data)
//    {
//        if (this.isEnabled())
//        {
//            // System.out.printf("register\n");
//            boolean registered = false;
////            MinecraftServer server = player.getCommandSource().getServer();
//            UUID uuid = player.getUuid();
//
//            if (this.hasPermission(player) == false)
//            {
//                // No Permission
//                Servux.debugLog("debug_data: Denying access for player {}, Insufficient Permissions", player.getName().getLiteralString());
//                return registered;
//            }
//
//            if (this.registeredPlayers.containsKey(uuid) == false)
//            {
//                this.registeredPlayers.put(uuid, data != null ? data.copy() : new NbtCompound());
//                this.sendMetadata(player);
//                registered = true;
//            }
//
//            return registered;
//        }
//
//        return false;
//    }
//
//    public boolean unregister(ServerPlayerEntity player)
//    {
//        return this.unregister(player, null);
//    }
//
//    public boolean unregister(ServerPlayerEntity player, @Nullable NbtCompound nbt)
//    {
//        // System.out.printf("unregister\n");
//        HANDLER.resetFailures(this.getNetworkChannel(), player);
//
//        return this.registeredPlayers.remove(player.getUuid()) != null;
//    }
//
//    public void onPacketFailure(ServerPlayerEntity player)
//    {
//        // Do something if we fail to register the client
//    }
//
//    public void sendMetadata(ServerPlayerEntity player)
//    {
//        if (this.isEnabled())
//        {
//            if (this.hasPermission(player) == false)
//            {
//                // No Permission
//                Servux.debugLog("debug_data: Denying access for player {}, Insufficient Permissions", player.getName().getLiteralString());
//                return;
//            }
//
//            NbtCompound nbt = new NbtCompound();
//            nbt.copyFrom(this.metadata);
//
//            Servux.debugLog("debugDataChannel: sendMetadata to player {}", player.getName().getLiteralString());
//
//            // Sends Metadata handshake, it doesn't succeed the first time, so using networkHandler
//            if (player.networkHandler != null)
//            {
//                HANDLER.sendPlayPayload(player.networkHandler, new ServuxDebugPacket.Payload(ServuxDebugPacket.MetadataResponse(this.metadata)));
//            }
//            else
//            {
//                HANDLER.sendPlayPayload(player, new ServuxDebugPacket.Payload(ServuxDebugPacket.MetadataResponse(this.metadata)));
//            }
//        }
//    }
//
//    public void confirmMetadata(ServerPlayerEntity player, NbtCompound data)
//    {
//        if (this.isEnabled())
//        {
//            UUID uuid = player.getUuid();
//
//            if (this.registeredPlayers.containsKey(uuid))
//            {
//                this.registeredPlayers.replace(uuid, data.copy());
//            }
//            else
//            {
//                this.registeredPlayers.put(uuid, data.copy());
//            }
//
//            Servux.debugLog("debugDataChannel: received confirm from player {}", player.getName().getLiteralString());
//        }
//    }
//
//	public void updateMetadata(ServerPlayerEntity player, NbtCompound data)
//	{
//		if (this.isEnabled())
//		{
//			UUID uuid = player.getUuid();
//
//			if (this.registeredPlayers.containsKey(uuid))
//			{
//				this.registeredPlayers.replace(uuid, data.copy());
//			}
//			else
//			{
//				this.registeredPlayers.put(uuid, data.copy());
//			}
//
//			Servux.debugLog("debugDataChannel: received confirm from player {}", player.getName().getLiteralString());
//		}
//	}
//
//	private void storePlayerRendererList(ServerPlayerEntity player, NbtCompound data)
//	{
//		if (this.isEnabled())
//		{
//			UUID uuid = player.getUuid();
//			String key = "enabledRenderers";
//
//			if (data.contains(key))
//			{
//				NbtList nbt = data.getListOrEmpty(key);
//				List<DebugRenderType> list = new ArrayList<>();
//
//				if (!nbt.isEmpty())
//				{
//					for (NbtElement entry : nbt)
//					{
//						try
//						{
//							DebugRenderType type = DebugRenderType.CODEC.parse(NbtOps.INSTANCE, entry).getOrThrow();
//
//							if (type != null)
//							{
//								list.add(type);
//								this.toggleRenderer(type, true);
//							}
//						}
//						catch (Exception err)
//						{
//							Servux.LOGGER.warn("DebugDataProvider#storePlayerRendererList: Exception parsing data; {}", err.getLocalizedMessage());
//						}
//					}
//				}
//
//				data.remove(key);
//				this.enabledRenderers.remove(uuid);
//
//				if (!list.isEmpty())
//				{
//					this.enabledRenderers.put(uuid, list);
//				}
//			}
//		}
//	}
//
//	private void toggleRenderer(DebugRenderType type, boolean toggle)
//	{
//		type.toggleSharedConstant(toggle);
//	}
//
//	private boolean playerHasRenderer(ServerPlayerEntity player, DebugRenderType type)
//	{
//		if (this.isEnabled())
//		{
//			UUID uuid = player.getUuid();
//
//			if (this.enabledRenderers.containsKey(uuid))
//			{
//				return this.enabledRenderers.get(uuid).contains(type);
//			}
//		}
//
//		return false;
//	}
//
//	private boolean anyPlayerHasRenderer(DebugRenderType type)
//	{
//		if (this.isEnabled())
//		{
//			AtomicBoolean result = new AtomicBoolean(false);
//
//			this.enabledRenderers.forEach(
//					(uuid, list) ->
//					{
//						if (list.contains(type))
//						{
//							result.set(true);
//						}
//					}
//			);
//
//			return result.get();
//		}
//
//		return false;
//	}
}
