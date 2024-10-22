package fi.dy.masa.servux.mixin.debug;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.network.packet.s2c.custom.DebugRedstoneUpdateOrderCustomPayload;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.listener.GameEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.servux.dataproviders.DebugDataProvider;

@Mixin(value = DebugInfoSender.class)
public class MixinDebugInfoSender
{
    @Shadow
    private static List<String> listMemories(LivingEntity entity, long currentTime)
    {
        throw new AssertionError();
    }

    @Inject(method = "addGameTestMarker", at = @At("HEAD"))
    private static void servux_onAddGameTestMarker(ServerWorld world, BlockPos pos, String message, int color, int duration, CallbackInfo ci)
    {
        // NO-OP
    }

    @Inject(method = "clearGameTestMarkers", at = @At("HEAD"))
    private static void servux_onClearGameTestMarkers(ServerWorld world, CallbackInfo ci)
    {
        // NO-OP
    }

    @Inject(method = "sendChunkWatchingChange", at = @At("HEAD"))
    private static void servux_onChunkWatchingChange(ServerWorld world, ChunkPos pos, CallbackInfo ci)
    {
        DebugDataProvider.INSTANCE.sendChunkWatchingChange(world, pos);
    }

    @Inject(method = "sendPoiAddition", at = @At("HEAD"))
    private static void servux_onSendPoiAddition(ServerWorld world, BlockPos pos, CallbackInfo ci)
    {
        DebugDataProvider.INSTANCE.sendPoiAdditions(world, pos);
    }

    @Inject(method = "sendPoiRemoval", at = @At("HEAD"))
    private static void servux_onSendPoiRemoval(ServerWorld world, BlockPos pos, CallbackInfo ci)
    {
        DebugDataProvider.INSTANCE.sendPoiRemoval(world, pos);
    }

    @Inject(method = "sendPointOfInterest", at = @At("HEAD"))
    private static void servux_onSendPointOfInterest(ServerWorld world, BlockPos pos, CallbackInfo ci)
    {
        DebugDataProvider.INSTANCE.sendPointOfInterest(world, pos);
    }

    @Inject(method = "sendPoi", at = @At("HEAD"))
    private static void servux_onSendPoi(ServerWorld world, BlockPos pos, CallbackInfo ci)
    {
        DebugDataProvider.INSTANCE.sendPoi(world, pos);
    }

    //FIXME (CustomPayload Error)
    @Inject(method = "sendPathfindingData", at = @At("HEAD"))
    private static void servux_onSendPathfindingData(World world, MobEntity mob, @Nullable Path path, float nodeReachProximity, CallbackInfo ci)
    {
        if (world instanceof ServerWorld serverWorld)
        {
            DebugDataProvider.INSTANCE.sendPathfindingData(serverWorld, mob, path, nodeReachProximity);
        }
    }

    @Inject(method = "sendNeighborUpdate", at = @At("HEAD"))
    private static void servux_onSendNeighborUpdate(World world, BlockPos pos, CallbackInfo ci)
    {
        if (world instanceof ServerWorld serverWorld)
        {
            DebugDataProvider.INSTANCE.sendNeighborUpdate(serverWorld, pos);
        }
    }

    @Inject(method = "sendRedstoneUpdateOrder", at = @At("HEAD"))
    private static void servux_onSendRedstoneUpdateOrder(World world, DebugRedstoneUpdateOrderCustomPayload payload, CallbackInfo ci)
    {
        // NO-OP
    }

    @Inject(method = "sendStructureStart", at = @At("HEAD"))
    private static void servux_onSendStructureStart(StructureWorldAccess world, StructureStart structureStart, CallbackInfo ci)
    {
        DebugDataProvider.INSTANCE.sendStructureStart(world, structureStart);
    }

    @Inject(method = "sendGoalSelector", at = @At("HEAD"))
    private static void servux_onSendGoalSelector(World world, MobEntity mob, GoalSelector goalSelector, CallbackInfo ci)
    {
        if (world instanceof ServerWorld serverWorld)
        {
            DebugDataProvider.INSTANCE.sendGoalSelector(serverWorld, mob, goalSelector);
        }
    }

    @Inject(method = "sendRaids", at = @At("HEAD"))
    private static void servux_onSendRaids(ServerWorld server, Collection<Raid> raids, CallbackInfo ci)
    {
        DebugDataProvider.INSTANCE.sendRaids(server, raids);
    }

    //FIXME (CustomPayload Error)
    @Inject(method = "sendBrainDebugData", at = @At("HEAD"))
    private static void servux_onSendBrainDebugData(LivingEntity living, CallbackInfo ci)
    {
        if (living.getWorld() instanceof ServerWorld world)
        {
            DebugDataProvider.INSTANCE.sendBrainDebugData(world, living);
        }
    }

    //FIXME (CustomPayload Error)
    @Inject(method = "sendBeeDebugData", at = @At("HEAD"))
    private static void servux_onSendBeeDebugData(BeeEntity bee, CallbackInfo ci)
    {
        if (bee.getWorld() instanceof ServerWorld world)
        {
            DebugDataProvider.INSTANCE.sendBeeDebugData(world, bee);
        }
    }

    @Inject(method = "sendBreezeDebugData", at = @At("HEAD"))
    private static void servux_onSendBreezeDebugData(BreezeEntity breeze, CallbackInfo ci)
    {
        if (breeze.getWorld() instanceof ServerWorld world)
        {
            DebugDataProvider.INSTANCE.sendBreezeDebugData(world, breeze);
        }
    }

    @Inject(method = "sendGameEvent", at = @At("HEAD"))
    private static void servux_onSendGameEvent(World world, RegistryEntry<GameEvent> event, Vec3d pos, CallbackInfo ci)
    {
        if (world instanceof ServerWorld serverWorld)
        {
            DebugDataProvider.INSTANCE.sendGameEvent(serverWorld, event, pos);
        }
    }

    @Inject(method = "sendGameEventListener", at = @At("HEAD"))
    private static void servux_onSendGameEventListener(World world, GameEventListener eventListener, CallbackInfo ci)
    {
        if (world instanceof ServerWorld serverWorld)
        {
            DebugDataProvider.INSTANCE.sendGameEventListener(serverWorld, eventListener);
        }
    }
}
