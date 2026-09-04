package fi.dy.masa.servux.util;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.LitematicsDataProvider;
import fi.dy.masa.servux.schematic.placement.SchematicPlacement;
import fi.dy.masa.servux.schematic.placement.SubRegionPlacement;
import fi.dy.masa.servux.util.data.Constants;
import fi.dy.masa.servux.util.data.tag.CompoundData;
import fi.dy.masa.servux.util.data.tag.ListData;
import fi.dy.masa.servux.util.nbt.NbtView;
import fi.dy.masa.servux.util.position.PositionUtils;

public class EntityUtils
{
    public static final Predicate<Entity> NOT_PLAYER = entity -> (entity instanceof Player) == false;
    private static final ThreadLocalRandom RAND = ThreadLocalRandom.current();

    public static boolean isCreativeMode(Player player)
    {
        return player.getAbilities().instabuild;
    }

    public static Direction getHorizontalLookingDirection(Entity entity)
    {
        return Direction.fromYRot(entity.getYRot());
    }

    public static Direction getVerticalLookingDirection(Entity entity)
    {
        return entity.getXRot() > 0 ? Direction.DOWN : Direction.UP;
    }

    public static Direction getClosestLookingDirection(Entity entity)
    {
        if (entity.getXRot() > 60.0f)
        {
            return Direction.DOWN;
        }
        else if (-entity.getXRot() > 60.0f)
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
            if (entity.getUUID().equals(uuid))
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
        Identifier resourcelocation = EntityType.getKey(entitytype);
        return entitytype.canSerialize() && resourcelocation != null ? resourcelocation.toString() : null;
    }

    @Nullable
    private static Entity createEntityFromDataSingle(CompoundData nbt, Level world)
    {
        try
        {
            NbtView view = NbtView.getReader(nbt, world.registryAccess());
            Optional<Entity> optional = EntityType.create(view.getReader(), world, EntitySpawnReason.LOAD);

            if (optional.isPresent())
            {
                Entity entity = optional.get();

                if (!nbt.containsLenient("UUID"))
                {
                    entity.setUUID(UUID.randomUUID());
                }

                if (nbt.contains("LastEntityID", Constants.NBT.TAG_INT))
                {
                    entity.setId(nbt.getIntOrDefault("LastEntityID", -1));
                }
                else
                {
                    entity.setId(RAND.nextInt(50000, Integer.MAX_VALUE));
                }

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
     * @param nbt ()
     * @param world ()
     * @return ()
     */
    @Nullable
    public static Entity createEntityAndPassengersFromData(CompoundData nbt, Level world)
    {
        Entity entity = createEntityFromDataSingle(nbt, world);

        if (entity == null)
        {
            return null;
        }
        else
        {
            if (nbt.containsList("Passengers", Constants.NBT.TAG_COMPOUND))
            {
                ListData taglist = nbt.getList("Passengers");

                for (int i = 0; i < taglist.size(); ++i)
                {
                    Entity passenger = createEntityAndPassengersFromData(taglist.getCompoundAt(i), world);

                    if (passenger != null)
                    {
                        passenger.startRiding(entity, true, false);
                    }
                }
            }

            return entity;
        }
    }

    public static void spawnEntityAndPassengersInWorld(Entity entity, Level world)
    {
        boolean result;

        Entity other = world.getEntity(entity.getId());

        if (!LitematicsDataProvider.INSTANCE.shouldDeDuplicateEntities())
        {
            if (other != null)
            {
                // We don't like needing to use Random();
                // but I guess there's no other logical method for this.
                entity.setId(RAND.nextInt(entity.getId() * 4, Integer.MAX_VALUE));
            }

            other = world.getEntity(entity.getUUID());

            if (other != null)
            {
                entity.setUUID(UUID.randomUUID());
            }
        }

        try
        {
            result = world.addFreshEntity(entity);
        }
        catch (Exception e)
        {
            Servux.LOGGER.error("EntityUtils#spawnEntityAndPassengersInWorld(): Exception; id({}): [{}/{}]; {}",
                                entity.getId(), entity.getStringUUID(),
                                entity.getType().getDescription().getString(),
                                e.getLocalizedMessage());
            result = false;
        }

        if (result && entity.isVehicle())
        {
            for (Entity passenger : entity.getPassengers())
            {
                passenger.snapTo(
                        entity.getX(),
                        entity.getY() + entity.getPassengerRidingPosition(passenger).y(),
                        entity.getZ(),
                        passenger.getYRot(), passenger.getXRot());
                setEntityRotations(passenger, passenger.getYRot(), passenger.getXRot());
                spawnEntityAndPassengersInWorld(passenger, world);
            }
        }
    }

    public static void setEntityRotations(Entity entity, float yaw, float pitch)
    {
        entity.setYRot(yaw);
        entity.yRotO = yaw;

        entity.setXRot(pitch);
        entity.xRotO = pitch;

        if (entity instanceof LivingEntity livingBase)
        {
            livingBase.yHeadRot = yaw;
            livingBase.yBodyRot = yaw;
            livingBase.yHeadRotO = yaw;
            livingBase.yBodyRotO = yaw;
            //livingBase.renderYawOffset = yaw;
            //livingBase.prevRenderYawOffset = yaw;
        }
    }

    public static List<Entity> getEntitiesWithinSubRegion(Level world, BlockPos origin, BlockPos regionPos, BlockPos regionSize,
                                                          SchematicPlacement schematicPlacement, SubRegionPlacement placement)
    {
        // These are the untransformed relative positions
        BlockPos regionPosRelTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        BlockPos posEndAbs = PositionUtils.getTransformedPlacementPosition(regionSize.offset(-1, -1, -1), schematicPlacement, placement).offset(regionPosRelTransformed).offset(origin);
        BlockPos regionPosAbs = regionPosRelTransformed.offset(origin);
        AABB bb = PositionUtils.createEnclosingAABB(regionPosAbs, posEndAbs);

        return world.getEntities((Entity) null, bb, EntityUtils.NOT_PLAYER);
    }
}
