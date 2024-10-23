package fi.dy.masa.servux.dataproviders;

import java.util.*;
import java.util.stream.Collectors;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import me.lucko.fabric.api.permissions.v0.Permissions;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.*;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.custom.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.NameGenerator;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.*;
import net.minecraft.village.VillageGossipType;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.listener.GameEventListener;
import net.minecraft.world.gen.structure.Structure;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.mixin.debug.IMixinMobEntity;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.ServerPlayHandler;
import fi.dy.masa.servux.network.packet.ServuxDebugHandler;
import fi.dy.masa.servux.network.packet.ServuxDebugPacket;
import fi.dy.masa.servux.network.packet.ServuxHudPacket;
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.settings.ServuxBoolSetting;
import fi.dy.masa.servux.settings.ServuxIntSetting;
import fi.dy.masa.servux.settings.ServuxStringListSetting;

@SuppressWarnings({"unchecked", "deprecation"})
public class DebugDataProvider extends DataProviderBase
{
    public static final DebugDataProvider INSTANCE = new DebugDataProvider();

    protected final static ServuxDebugHandler<ServuxDebugPacket.Payload> HANDLER = ServuxDebugHandler.getInstance();
    protected final HashMap<UUID, NbtCompound> registeredPlayers = new HashMap<>();
    protected final NbtCompound metadata = new NbtCompound();

    private final ServuxIntSetting basePermissionLevel = new ServuxIntSetting(this, "permission_level", 2, 4, 0);
    //private final ServuxStringListSetting enabledDebugPackets = new ServuxStringListSetting(this, "debug_enabled", List.of("chunk_watcher", "poi", "pathfinding", "neighbor_update", "structures", "goal_selector", "raids", "brain", "bees", "breeze", "game_event"));
    //private final ServuxBoolSetting enableServerDevelopmentMode = new ServuxBoolSetting(this, "server_development_mode", false);
    private final List<IServuxSetting<?>> settings = List.of(this.basePermissionLevel);

    protected DebugDataProvider()
    {
        super("debug_data",
              ServuxDebugHandler.CHANNEL_ID,
              ServuxDebugPacket.PROTOCOL_VERSION,
              2, Reference.MOD_ID + ".provider.debug_data",
              "Vanilla Debug Data provider.");

        this.metadata.putString("name", this.getName());
        this.metadata.putString("id", this.getNetworkChannel().toString());
        this.metadata.putInt("version", this.getProtocolVersion());
        this.metadata.putString("servux", Reference.MOD_STRING);
    }

    @Override
    public void registerHandler()
    {
        ServerPlayHandler.getInstance().registerServerPlayHandler(HANDLER);
        if (this.isRegistered() == false)
        {
            HANDLER.registerPlayPayload(ServuxDebugPacket.Payload.ID, ServuxDebugPacket.Payload.CODEC, IPluginServerPlayHandler.BOTH_SERVER);
            this.setRegistered(true);
        }
        HANDLER.registerPlayReceiver(ServuxDebugPacket.Payload.ID, HANDLER::receivePlayPayload);
    }

    @Override
    public void unregisterHandler()
    {
        HANDLER.unregisterPlayReceiver();
        ServerPlayHandler.getInstance().unregisterServerPlayHandler(HANDLER);
    }

    @Override
    public IPluginServerPlayHandler<ServuxDebugPacket.Payload> getPacketHandler()
    {
        return HANDLER;
    }

    @Override
    public void onTickEndPre()
    {
        // NO-OP
    }

    @Override
    public void onTickEndPost()
    {
        // NO-OP
    }

    @Override
    public List<IServuxSetting<?>> getSettings()
    {
        return settings;
    }

    @Override
    public boolean hasPermission(ServerPlayerEntity player)
    {
        if (player == null)
        {
            return false;
        }

        return Permissions.check(player, Reference.MOD_ID + ".debug_data", this.basePermissionLevel.getValue());
    }

    /*
    public boolean isServerDevelopmentMode()
    {
        return this.enableServerDevelopmentMode.getValue();
    }
     */

    public boolean register(ServerPlayerEntity player)
    {
        return this.register(player, null);
    }

    public boolean register(ServerPlayerEntity player, @Nullable NbtCompound data)
    {
        // System.out.printf("register\n");
        boolean registered = false;
        MinecraftServer server = player.getServer();
        UUID uuid = player.getUuid();

        if (this.hasPermission(player) == false)
        {
            // No Permission
            Servux.debugLog("debug_data: Denying access for player {}, Insufficient Permissions", player.getName().getLiteralString());
            return registered;
        }

        if (this.registeredPlayers.containsKey(uuid) == false)
        {
            this.registeredPlayers.put(uuid, data != null ? data.copy() : new NbtCompound());
            this.sendMetadata(player);
            registered = true;
        }

        return registered;
    }

    public boolean unregister(ServerPlayerEntity player)
    {
        return this.unregister(player, null);
    }

    public boolean unregister(ServerPlayerEntity player, @Nullable NbtCompound nbt)
    {
        // System.out.printf("unregister\n");
        HANDLER.resetFailures(this.getNetworkChannel(), player);

        return this.registeredPlayers.remove(player.getUuid()) != null;
    }

    public void onPacketFailure(ServerPlayerEntity player)
    {
        // Do something if we fail to register the client
    }

    public void sendMetadata(ServerPlayerEntity player)
    {
        if (this.hasPermission(player) == false)
        {
            // No Permission
            Servux.debugLog("debug_data: Denying access for player {}, Insufficient Permissions", player.getName().getLiteralString());
            return;
        }

        NbtCompound nbt = new NbtCompound();
        nbt.copyFrom(this.metadata);

        Servux.debugLog("debugDataChannel: sendMetadata to player {}", player.getName().getLiteralString());

        // Sends Metadata handshake, it doesn't succeed the first time, so using networkHandler
        if (player.networkHandler != null)
        {
            HANDLER.sendPlayPayload(player.networkHandler, new ServuxDebugPacket.Payload(ServuxDebugPacket.MetadataResponse(this.metadata)));
        }
        else
        {
            HANDLER.sendPlayPayload(player, new ServuxDebugPacket.Payload(ServuxDebugPacket.MetadataResponse(this.metadata)));
        }
    }

    public void confirmMetadata(ServerPlayerEntity player, NbtCompound data)
    {
        UUID uuid = player.getUuid();

        if (this.registeredPlayers.containsKey(uuid))
        {
            this.registeredPlayers.replace(uuid, data.copy());
        }
        else
        {
            this.registeredPlayers.put(uuid, data.copy());
        }

        Servux.debugLog("debugDataChannel: received confirm from player {}", player.getName().getLiteralString());
    }

    private void sendDebugData(ServerWorld world, CustomPayload payload)
    {
        if (this.isEnabled())
        {
            Packet<?> packet = new CustomPayloadS2CPacket(payload);

            for (ServerPlayerEntity player : world.getPlayers())
            {
                if (this.registeredPlayers.containsKey(player.getUuid()) &&
                    player.networkHandler.accepts(packet))
                {
                    player.networkHandler.sendPacket(packet);
                }
            }
        }
    }

    public void sendChunkWatchingChange(ServerWorld world, ChunkPos pos)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        //if (this.enabledDebugPackets.getValue().contains("chunk_watcher"))
        //{
            this.sendDebugData(world, new DebugWorldgenAttemptCustomPayload(pos.getStartPos().up(100), 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));
        //}
    }

    public void sendPoiAdditions(ServerWorld world, BlockPos pos)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        //if (this.enabledDebugPackets.getValue().contains("poi"))
        //{
            world.getPointOfInterestStorage().getType(pos).ifPresent((registryEntry) ->
            {
                int tickets = world.getPointOfInterestStorage().getFreeTickets(pos);
                String name = registryEntry.getIdAsString();
                this.sendDebugData(world, new DebugPoiAddedCustomPayload(pos, name, tickets));
            });
        //}
    }

    public void sendPoiRemoval(ServerWorld world, BlockPos pos)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        //if (this.enabledDebugPackets.getValue().contains("poi"))
        //{
            this.sendDebugData(world, new DebugPoiRemovedCustomPayload(pos));
        //}
    }

    public void sendPointOfInterest(ServerWorld world, BlockPos pos)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        //if (this.enabledDebugPackets.getValue().contains("poi"))
        //{
            int tickets = world.getPointOfInterestStorage().getFreeTickets(pos);
            this.sendDebugData(world, new DebugPoiTicketCountCustomPayload(pos, tickets));
        //}
    }

    public void sendPoi(ServerWorld world, BlockPos pos)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        //if (this.enabledDebugPackets.getValue().contains("poi"))
        //{
            Registry<Structure> registry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
            ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(pos);
            Iterator<RegistryEntry<Structure>> iterator = registry.iterateEntries(StructureTags.VILLAGE).iterator();

            RegistryEntry<Structure> entry;
            do
            {
                if (!iterator.hasNext())
                {
                    this.sendDebugData(world, new DebugVillageSectionsCustomPayload(Set.of(), Set.of(chunkSectionPos)));
                    return;
                }

                entry = iterator.next();
            }
            while (world.getStructureAccessor().getStructureStarts(chunkSectionPos, entry.value()).isEmpty());

            this.sendDebugData(world, new DebugVillageSectionsCustomPayload(Set.of(chunkSectionPos), Set.of()));
        //}
    }

    public void sendPathfindingData(ServerWorld world, MobEntity mob, @Nullable Path path, float nodeReachProximity)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        //if (this.enabledDebugPackets.getValue().contains("pathfinding"))
        //{
            if (path != null)
            {
                this.sendDebugData(world, new DebugPathCustomPayload(mob.getId(), path, nodeReachProximity));
            }
        //}
    }

    public void sendNeighborUpdate(ServerWorld world, BlockPos pos)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        //if (this.enabledDebugPackets.getValue().contains("neighbor_update"))
        //{
            this.sendDebugData(world, new DebugNeighborsUpdateCustomPayload(world.getTime(), pos));
        //}
    }

    public void sendStructureStart(StructureWorldAccess world, StructureStart structureStart)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        //if (this.enabledDebugPackets.getValue().contains("structures"))
        //{
            List<DebugStructuresCustomPayload.Piece> pieces = new ArrayList<>();

            for (int i = 0; i < structureStart.getChildren().size(); ++i)
            {
                pieces.add(new DebugStructuresCustomPayload.Piece(structureStart.getChildren().get(i).getBoundingBox(), i == 0));
            }

            ServerWorld serverWorld = world.toServerWorld();
            this.sendDebugData(serverWorld, new DebugStructuresCustomPayload(serverWorld.getRegistryKey(), structureStart.getBoundingBox(), pieces));
        //}
    }

    public void sendGoalSelector(ServerWorld world, MobEntity mob, GoalSelector goalSelector)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        //if (this.enabledDebugPackets.getValue().contains("goal_selector"))
        //{
            List<DebugGoalSelectorCustomPayload.Goal> goals = ((IMixinMobEntity) mob).servux_getGoalSelector().getGoals().stream().map((goal) ->
                    new DebugGoalSelectorCustomPayload.Goal(goal.getPriority(), goal.isRunning(), goal.getGoal().toString())).toList();

            this.sendDebugData(world, new DebugGoalSelectorCustomPayload(mob.getId(), mob.getBlockPos(), goals));
        //}
    }

    public void sendRaids(ServerWorld world, Collection<Raid> raids)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        //if (this.enabledDebugPackets.getValue().contains("raids"))
        //{
            this.sendDebugData(world, new DebugRaidsCustomPayload(raids.stream().map(Raid::getCenter).toList()));
        //}
    }

    public void sendBrainDebugData(ServerWorld serverWorld, LivingEntity livingEntity)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        //if (this.enabledDebugPackets.getValue().contains("brain"))
        //{
            MobEntity entity = (MobEntity) livingEntity;
            int angerLevel;

            if (entity instanceof WardenEntity wardenEntity)
            {
                angerLevel = wardenEntity.getAnger();
            }
            else
            {
                angerLevel = -1;
            }

            List<String> gossips = new ArrayList<>();
            Set<BlockPos> pois = new HashSet<>();
            Set<BlockPos> potentialPois = new HashSet<>();
            String profession;
            int xp;
            String inventory;
            boolean wantsGolem;

            if (entity instanceof VillagerEntity villager)
            {
                profession = villager.getVillagerData().getProfession().toString();
                xp = villager.getExperience();
                inventory = villager.getInventory().toString();
                wantsGolem = villager.canSummonGolem(serverWorld.getTime());
                villager.getGossip().getEntityReputationAssociatedGossips().forEach((uuid, associatedGossip) ->
                {
                    Entity gossipEntity = serverWorld.getEntity(uuid);

                    if (gossipEntity != null)
                    {
                        String name = NameGenerator.name(gossipEntity);

                        for (Object2IntMap.Entry<VillageGossipType> typeEntry : associatedGossip.object2IntEntrySet())
                        {
                            Map.Entry<VillageGossipType, Integer> entry = (Map.Entry) typeEntry;
                            gossips.add(name + ": " + entry.getKey().asString() + " " + entry.getValue());
                        }
                    }
                });

                Brain<?> brain = villager.getBrain();
                addPoi(brain, MemoryModuleType.HOME, pois);
                addPoi(brain, MemoryModuleType.JOB_SITE, pois);
                addPoi(brain, MemoryModuleType.MEETING_POINT, pois);
                addPoi(brain, MemoryModuleType.HIDING_PLACE, pois);
                addPoi(brain, MemoryModuleType.POTENTIAL_JOB_SITE, potentialPois);
            }
            else
            {
                profession = "";
                xp = 0;
                inventory = "";
                wantsGolem = false;
            }

            this.sendDebugData(serverWorld, new DebugBrainCustomPayload(new DebugBrainCustomPayload.Brain(
                    entity.getUuid(), entity.getId(), entity.getName().getString(),
                    profession, xp, entity.getHealth(), entity.getMaxHealth(),
                    entity.getPos(), inventory, entity.getNavigation().getCurrentPath(),
                    wantsGolem, angerLevel,
                    entity.getBrain().getPossibleActivities().stream().map(Activity::toString).toList(),
                    entity.getBrain().getRunningTasks().stream().map(Task::getName).toList(),
                    this.listMemories(entity, serverWorld.getTime()),
                    //memories,
                    gossips, pois, potentialPois)));
        //}
    }

    public void sendBeeDebugData(ServerWorld world, BeeEntity bee)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        //if (this.enabledDebugPackets.getValue().contains("bees"))
        //{
            this.sendDebugData(world, new DebugBeeCustomPayload(
                    new DebugBeeCustomPayload.Bee(bee.getUuid(), bee.getId(), bee.getPos(),
                            bee.getNavigation().getCurrentPath(), bee.getHivePos(), bee.getFlowerPos(), bee.getMoveGoalTicks(),
                            bee.getGoalSelector().getGoals().stream().map((prioritizedGoal) ->
                                    prioritizedGoal.getGoal().toString()).collect(Collectors.toSet()), bee.getPossibleHives())));

        //}
    }

    public void sendBreezeDebugData(ServerWorld world, BreezeEntity breeze)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        //if (this.enabledDebugPackets.getValue().contains("breeze"))
        //{
            this.sendDebugData(world, new DebugBreezeCustomPayload(new DebugBreezeCustomPayload.BreezeInfo(
                    breeze.getUuid(), breeze.getId(), breeze.getTarget() == null ? null : breeze.getTarget().getId(),
                    breeze.getBrain().getOptionalRegisteredMemory(MemoryModuleType.BREEZE_JUMP_TARGET).orElse(null))));
        //}
    }

    public void sendGameEvent(ServerWorld world, RegistryEntry<GameEvent> event, Vec3d pos)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        //if (this.enabledDebugPackets.getValue().contains("game_event"))
        //{
            event.getKey().ifPresent((key) -> this.sendDebugData(world, new DebugGameEventCustomPayload(key, pos)));
        //}
    }

    public void sendGameEventListener(ServerWorld world, GameEventListener eventListener)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        //if (this.enabledDebugPackets.getValue().contains("game_event"))
        //{
            this.sendDebugData(world, new DebugGameEventListenersCustomPayload(eventListener.getPositionSource(), eventListener.getRange()));
        //}
    }

    // Tools
    private void addPoi(Brain<?> brain, MemoryModuleType<GlobalPos> memoryModuleType, Set<BlockPos> set)
    {
        Optional<BlockPos> opt = brain.getOptionalRegisteredMemory(memoryModuleType).map(GlobalPos::pos);

        Objects.requireNonNull(set);
        opt.ifPresent(set::add);
    }

    private List<String> listMemories(LivingEntity entity, long currentTime)
    {
        Map<MemoryModuleType<?>, Optional<? extends Memory<?>>> map = entity.getBrain().getMemories();
        List<String> list = Lists.newArrayList();

        for (Map.Entry<MemoryModuleType<?>, Optional<? extends Memory<?>>> memoryModuleTypeOptionalEntry : map.entrySet())
        {
            MemoryModuleType<?> memoryModuleType = memoryModuleTypeOptionalEntry.getKey();
            Optional<? extends Memory<?>> optional = memoryModuleTypeOptionalEntry.getValue();
            String string;

            if (optional.isPresent())
            {
                Memory<?> memory = optional.get();
                Object object = memory.getValue();

                if (memoryModuleType == MemoryModuleType.HEARD_BELL_TIME)
                {
                    long l = currentTime - (Long) object;
                    string = l + " ticks ago";
                }
                else if (memory.isTimed())
                {
                    String var10000 = this.format((ServerWorld) entity.getWorld(), object);
                    string = var10000 + " (ttl: " + memory.getExpiry() + ")";
                }
                else
                {
                    string = this.format((ServerWorld) entity.getWorld(), object);
                }
            }
            else
            {
                string = "-";
            }

            String type = Registries.MEMORY_MODULE_TYPE.getId(memoryModuleType).getPath();
            list.add(type + ": " + string);
        }

        list.sort(String::compareTo);
        return list;
    }

    private String format(ServerWorld world, @Nullable Object object)
    {
        if (object == null)
        {
            return "-";
        }
        else if (object instanceof UUID)
        {
            return format(world, world.getEntity((UUID) object));
        }
        else
        {
            Entity entity;
            if (object instanceof LivingEntity)
            {
                entity = (Entity) object;
                return NameGenerator.name(entity);
            }
            else if (object instanceof Nameable)
            {
                return ((Nameable) object).getName().getString();
            }
            else if (object instanceof WalkTarget)
            {
                return format(world, ((WalkTarget) object).getLookTarget());
            }
            else if (object instanceof EntityLookTarget)
            {
                return format(world, ((EntityLookTarget) object).getEntity());
            }
            else if (object instanceof GlobalPos)
            {
                return format(world, ((GlobalPos) object).pos());
            }
            else if (object instanceof BlockPosLookTarget)
            {
                return format(world, ((BlockPosLookTarget) object).getBlockPos());
            }
            else if (object instanceof DamageSource)
            {
                entity = ((DamageSource) object).getAttacker();
                return entity == null ? object.toString() : format(world, entity);
            }
            else if (!(object instanceof Collection))
            {
                return object.toString();
            }
            else
            {
                List<String> list = Lists.newArrayList();

                for (Object object2 : (Iterable) object)
                {
                    list.add(format(world, object2));
                }

                return list.toString();
            }
        }
    }
}
