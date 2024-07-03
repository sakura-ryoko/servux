package fi.dy.masa.servux.util;

import fi.dy.masa.servux.schematic.placement.SchematicPlacement;
import fi.dy.masa.servux.schematic.placement.SubRegionPlacement;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public class EntityUtils
{
    public static final Predicate<Entity> NOT_PLAYER = entity -> (entity instanceof PlayerEntity) == false;

    public static boolean isCreativeMode(PlayerEntity player)
    {
        return player.getAbilities().creativeMode;
    }

    public static Direction getHorizontalLookingDirection(Entity entity)
    {
        return Direction.fromRotation(entity.getYaw());
    }

    public static Direction getVerticalLookingDirection(Entity entity)
    {
        return entity.getPitch() > 0 ? Direction.DOWN : Direction.UP;
    }

    public static Direction getClosestLookingDirection(Entity entity)
    {
        if (entity.getPitch() > 60.0f)
        {
            return Direction.DOWN;
        }
        else if (-entity.getPitch() > 60.0f)
        {
            return Direction.UP;
        }

        return getHorizontalLookingDirection(entity);
    }

    @Nullable
    public static <T extends Entity> T findEntityByUUID(List<T> list, UUID uuid)
    {
        if (uuid == null)
        {
            return null;
        }

        for (T entity : list)
        {
            if (entity.getUuid().equals(uuid))
            {
                return entity;
            }
        }

        return null;
    }

    @Nullable
    public static String getEntityId(Entity entity)
    {
        EntityType<?> entitytype = entity.getType();
        Identifier resourcelocation = EntityType.getId(entitytype);
        return entitytype.isSaveable() && resourcelocation != null ? resourcelocation.toString() : null;
    }

    @Nullable
    private static Entity createEntityFromNBTSingle(NbtCompound nbt, World world)
    {
        try
        {
            Optional<Entity> optional = EntityType.getEntityFromNbt(nbt, world);

            if (optional.isPresent())
            {
                Entity entity = optional.get();
                entity.setUuid(UUID.randomUUID());
                return entity;
            }
        }
        catch (Exception ignore)
        {
        }

        return null;
    }

    /**
     * Note: This does NOT spawn any of the entities in the world!
     * @param nbt
     * @param world
     * @return
     */
    @Nullable
    public static Entity createEntityAndPassengersFromNBT(NbtCompound nbt, World world)
    {
        Entity entity = createEntityFromNBTSingle(nbt, world);

        if (entity == null)
        {
            return null;
        }
        else
        {
            if (nbt.contains("Passengers", Constants.NBT.TAG_LIST))
            {
                NbtList taglist = nbt.getList("Passengers", Constants.NBT.TAG_COMPOUND);

                for (int i = 0; i < taglist.size(); ++i)
                {
                    Entity passenger = createEntityAndPassengersFromNBT(taglist.getCompound(i), world);

                    if (passenger != null)
                    {
                        passenger.startRiding(entity, true);
                    }
                }
            }

            return entity;
        }
    }

    public static void spawnEntityAndPassengersInWorld(Entity entity, World world)
    {
        if (world.spawnEntity(entity) && entity.hasPassengers())
        {
            for (Entity passenger : entity.getPassengerList())
            {
                passenger.refreshPositionAndAngles(
                        entity.getX(),
                        entity.getY() + entity.getPassengerRidingPos(passenger).getY(),
                        entity.getZ(),
                        passenger.getYaw(), passenger.getPitch());
                setEntityRotations(passenger, passenger.getYaw(), passenger.getPitch());
                spawnEntityAndPassengersInWorld(passenger, world);
            }
        }
    }

    public static void setEntityRotations(Entity entity, float yaw, float pitch)
    {
        entity.setYaw(yaw);
        entity.prevYaw = yaw;

        entity.setPitch(pitch);
        entity.prevPitch = pitch;

        if (entity instanceof LivingEntity livingBase)
        {
            livingBase.headYaw = yaw;
            livingBase.bodyYaw = yaw;
            livingBase.prevHeadYaw = yaw;
            livingBase.prevBodyYaw = yaw;
            //livingBase.renderYawOffset = yaw;
            //livingBase.prevRenderYawOffset = yaw;
        }
    }

    public static List<Entity> getEntitiesWithinSubRegion(World world, BlockPos origin, BlockPos regionPos, BlockPos regionSize,
                                                          SchematicPlacement schematicPlacement, SubRegionPlacement placement)
    {
        // These are the untransformed relative positions
        BlockPos regionPosRelTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        BlockPos posEndAbs = PositionUtils.getTransformedPlacementPosition(regionSize.add(-1, -1, -1), schematicPlacement, placement).add(regionPosRelTransformed).add(origin);
        BlockPos regionPosAbs = regionPosRelTransformed.add(origin);
        Box bb = PositionUtils.createEnclosingAABB(regionPosAbs, posEndAbs);

        return world.getOtherEntities(null, bb, EntityUtils.NOT_PLAYER);
    }
}
