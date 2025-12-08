package fi.dy.masa.servux.schematic.placement;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.schematic.LitematicaSchematic;
import fi.dy.masa.servux.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.servux.schematic.selection.Box;
import fi.dy.masa.servux.util.*;
import fi.dy.masa.servux.util.nbt.NbtUtils;
import fi.dy.masa.servux.util.position.PositionUtils;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import java.util.*;

public class SchematicPlacement
{
    private static final Set<Integer> USED_COLORS = new HashSet<>();

    private final Map<String, SubRegionPlacement> relativeSubRegionPlacements = new HashMap<>();
    private final int subRegionCount;
    private final LitematicaSchematic schematic;
    private BlockPos origin;
    private String name;
    private Rotation rotation = Rotation.NONE;
    private Mirror mirror = Mirror.NONE;
    private boolean ignoreEntities;
    private boolean regionPlacementsModified;
    private int coordinateLockMask;
    @Nullable
    private Box enclosingBox;

    private SchematicPlacement(LitematicaSchematic schematic, BlockPos origin, String name, boolean ignoreEntities)
    {
        this.schematic = schematic;
        this.origin = origin;
        this.name = name;
        this.subRegionCount = schematic.getSubRegionCount();
        this.ignoreEntities = ignoreEntities;
    }

    public static SchematicPlacement createFor(LitematicaSchematic schematic, BlockPos origin, String name, boolean ignoreEntities)
    {
        SchematicPlacement placement = new SchematicPlacement(schematic, origin, name, ignoreEntities);
        placement.resetAllSubRegionsToSchematicValues();

        return placement;
    }

    public static SchematicPlacement createFromNbt(CompoundTag tags)
    {
        try
        {
            LitematicaSchematic schematic = new LitematicaSchematic(tags.getCompoundOrEmpty("Schematics"));
            BlockPos origin = NbtUtils.readBlockPosFromIntArray(tags, "Origin");
            String name = tags.getStringOr("Name", "?");
            SchematicPlacement placement = new SchematicPlacement(schematic, origin, name, false);
            placement.mirror = Mirror.values()[tags.getIntOr("Mirror", 0)];
            placement.rotation = Rotation.values()[tags.getIntOr("Rotation", 0)];

            for (String entry : tags.getCompoundOrEmpty("SubRegions").keySet())
            {
                CompoundTag compound = tags.getCompoundOrEmpty("SubRegions").getCompoundOrEmpty(entry);
                origin = NbtUtils.readBlockPosFromIntArray(compound, "Pos");
                name = compound.getStringOr("Name", "?");
                SubRegionPlacement sub = new SubRegionPlacement(origin, name);
                sub.mirror = Mirror.values()[compound.getIntOr("Mirror", 0)];
                sub.rotation = Rotation.values()[compound.getIntOr("Rotation", 0)];
                sub.ignoreEntities = compound.getBooleanOr("IgnoreEntities", false);
                sub.enabled = compound.getBooleanOr("Enabled", true);
                placement.relativeSubRegionPlacements.put(entry, sub);
            }

            return placement;
        }
        catch (CommandSyntaxException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static SchematicPlacement createFromNbt(LitematicaSchematic schematic, CompoundTag tags)
    {
        try
        {
            BlockPos origin = NbtUtils.readBlockPosFromIntArray(tags, "Origin");
            String name = tags.getStringOr("Name", "?");
            SchematicPlacement placement = new SchematicPlacement(schematic, origin, name, false);
            placement.mirror = Mirror.values()[tags.getIntOr("Mirror", 0)];
            placement.rotation = Rotation.values()[tags.getIntOr("Rotation", 0)];

            for (String entry : tags.getCompoundOrEmpty("SubRegions").keySet())
            {
                CompoundTag compound = tags.getCompoundOrEmpty("SubRegions").getCompoundOrEmpty(entry);
                origin = NbtUtils.readBlockPosFromIntArray(compound, "Pos");
                name = compound.getStringOr("Name", "?");
                SubRegionPlacement sub = new SubRegionPlacement(origin, name);
                sub.mirror = Mirror.values()[compound.getIntOr("Mirror", 0)];
                sub.rotation = Rotation.values()[compound.getIntOr("Rotation", 0)];
                sub.ignoreEntities = compound.getBooleanOr("IgnoreEntities", false);
                sub.enabled = compound.getBooleanOr("Enabled", true);
                placement.relativeSubRegionPlacements.put(entry, sub);
            }

            return placement;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public boolean shouldRenderEnclosingBox()
    {
        return false;
    }

    public boolean isRegionPlacementModified()
    {
        return this.regionPlacementsModified;
    }

    public boolean ignoreEntities()
    {
        return this.ignoreEntities;
    }

    public String getName()
    {
        return this.name;
    }

    public LitematicaSchematic getSchematic()
    {
        return schematic;
    }

    @Nullable
    public Box getEclosingBox()
    {
        return this.enclosingBox;
    }

    public void setName(String name)
    {
        this.name = name;
    }


    public BlockPos getOrigin()
    {
        return origin;
    }

    public Rotation getRotation()
    {
        return rotation;
    }

    public Mirror getMirror()
    {
        return mirror;
    }


    public int getSubRegionCount()
    {
        return this.subRegionCount;
    }

    @Nullable
    public SubRegionPlacement getRelativeSubRegionPlacement(String areaName)
    {
        return this.relativeSubRegionPlacements.get(areaName);
    }

    public Collection<SubRegionPlacement> getAllSubRegionsPlacements()
    {
        return this.relativeSubRegionPlacements.values();
    }

    public ImmutableMap<@NotNull String, @NotNull SubRegionPlacement> getEnabledRelativeSubRegionPlacements()
    {
        ImmutableMap.Builder<@NotNull String, @NotNull SubRegionPlacement> builder = ImmutableMap.builder();

        for (Map.Entry<String, SubRegionPlacement> entry : this.relativeSubRegionPlacements.entrySet())
        {
            SubRegionPlacement placement = entry.getValue();

            if (placement.matchesRequirement(RequiredEnabled.PLACEMENT_ENABLED))
            {
                builder.put(entry.getKey(), entry.getValue());
            }
        }

        return builder.build();
    }

    /*
    public ImmutableMap<String, Box> getAllSubRegionBoxes()
    {
        return this.getSubRegionBoxes(RequiredEnabled.ANY);
    }
    */

    private void updateEnclosingBox()
    {
        if (this.shouldRenderEnclosingBox())
        {
            ImmutableMap<@NotNull String, @NotNull Box> boxes = this.getSubRegionBoxes(RequiredEnabled.ANY);
            BlockPos pos1 = null;
            BlockPos pos2 = null;

            for (Box box : boxes.values())
            {
	            BlockPos tmp;
                tmp = PositionUtils.getMinCorner(box.getPos1(), box.getPos2());

                if (pos1 == null)
                {
                    pos1 = tmp;
                } else if (tmp.getX() < pos1.getX() || tmp.getY() < pos1.getY() || tmp.getZ() < pos1.getZ())
                {
                    pos1 = PositionUtils.getMinCorner(tmp, pos1);
                }

                tmp = PositionUtils.getMaxCorner(box.getPos1(), box.getPos2());

                if (pos2 == null)
                {
                    pos2 = tmp;
                } else if (tmp.getX() > pos2.getX() || tmp.getY() > pos2.getY() || tmp.getZ() > pos2.getZ())
                {
                    pos2 = PositionUtils.getMaxCorner(tmp, pos2);
                }
            }

            if (pos1 != null && pos2 != null)
            {
                this.enclosingBox = new Box(pos1, pos2, "Enclosing Box");
            }
        }
    }

    public ImmutableMap<@NotNull String, @NotNull Box> getSubRegionBoxes(RequiredEnabled required)
    {
        ImmutableMap.Builder<@NotNull String, @NotNull Box> builder = ImmutableMap.builder();
        Map<String, BlockPos> areaSizes = this.schematic.getAreaSizes();

        for (Map.Entry<String, SubRegionPlacement> entry : this.relativeSubRegionPlacements.entrySet())
        {
            String name = entry.getKey();
            BlockPos areaSize = areaSizes.get(name);

            if (areaSize == null)
            {
                Servux.LOGGER.warn("SchematicPlacement.getSubRegionBoxes(): Size for sub-region '{}' not found in the schematic '{}'", name, this.schematic.getMetadata().getName());
                continue;
            }

            SubRegionPlacement placement = entry.getValue();

            if (placement.matchesRequirement(required))
            {
                BlockPos boxOriginRelative = placement.getPos();
                BlockPos boxOriginAbsolute = PositionUtils.getTransformedBlockPos(boxOriginRelative, this.mirror, this.rotation).offset(this.origin);
                BlockPos pos2 = PositionUtils.getRelativeEndPositionFromAreaSize(areaSize);
                pos2 = PositionUtils.getTransformedBlockPos(pos2, this.mirror, this.rotation);
                pos2 = PositionUtils.getTransformedBlockPos(pos2, placement.getMirror(), placement.getRotation()).offset(boxOriginAbsolute);

                builder.put(name, new Box(boxOriginAbsolute, pos2, name));
            }
        }

        return builder.build();
    }

    public ImmutableMap<@NotNull String, @NotNull Box> getSubRegionBoxFor(String regionName, RequiredEnabled required)
    {
        ImmutableMap.Builder<@NotNull String, @NotNull Box> builder = ImmutableMap.builder();
        Map<String, BlockPos> areaSizes = this.schematic.getAreaSizes();

        SubRegionPlacement placement = this.relativeSubRegionPlacements.get(regionName);

        if (placement != null)
        {
            if (placement.matchesRequirement(required))
            {
                BlockPos areaSize = areaSizes.get(regionName);

                if (areaSize != null)
                {
                    BlockPos boxOriginRelative = placement.getPos();
                    BlockPos boxOriginAbsolute = PositionUtils.getTransformedBlockPos(boxOriginRelative, this.mirror, this.rotation).offset(this.origin);
                    BlockPos pos2 = PositionUtils.getRelativeEndPositionFromAreaSize(areaSize);
                    pos2 = PositionUtils.getTransformedBlockPos(pos2, this.mirror, this.rotation);
                    pos2 = PositionUtils.getTransformedBlockPos(pos2, placement.getMirror(), placement.getRotation()).offset(boxOriginAbsolute);

                    builder.put(regionName, new Box(boxOriginAbsolute, pos2, regionName));
                } else
                {
                    Servux.LOGGER.warn("SchematicPlacement.getSubRegionBoxFor(): Size for sub-region '{}' not found in the schematic '{}'", regionName, this.schematic.getMetadata().getName());
                }
            }
        }

        return builder.build();
    }

    public Set<String> getRegionsTouchingChunk(int chunkX, int chunkZ)
    {
        ImmutableMap<@NotNull String, @NotNull Box> map = this.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED);
        final int chunkXMin = chunkX << 4;
        final int chunkZMin = chunkZ << 4;
        final int chunkXMax = chunkXMin + 15;
        final int chunkZMax = chunkZMin + 15;
        Set<String> set = new HashSet<>();

        for (Map.Entry<String, Box> entry : map.entrySet())
        {
            Box box = entry.getValue();
            final int boxXMin = Math.min(box.getPos1().getX(), box.getPos2().getX());
            final int boxZMin = Math.min(box.getPos1().getZ(), box.getPos2().getZ());
            final int boxXMax = Math.max(box.getPos1().getX(), box.getPos2().getX());
            final int boxZMax = Math.max(box.getPos1().getZ(), box.getPos2().getZ());

            boolean notOverlapping = boxXMin > chunkXMax || boxZMin > chunkZMax || boxXMax < chunkXMin || boxZMax < chunkZMin;

            if (notOverlapping == false)
            {
                set.add(entry.getKey());
            }
        }

        return set;
    }

    public ImmutableMap<@NotNull String, @NotNull IntBoundingBox> getBoxesWithinChunk(int chunkX, int chunkZ)
    {
        ImmutableMap<@NotNull String, @NotNull Box> subRegions = this.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED);
        return PositionUtils.getBoxesWithinChunk(chunkX, chunkZ, subRegions);
    }

    @Nullable
    public IntBoundingBox getBoxWithinChunkForRegion(String regionName, int chunkX, int chunkZ)
    {
        Box box = this.getSubRegionBoxFor(regionName, RequiredEnabled.PLACEMENT_ENABLED).get(regionName);
        return box != null ? PositionUtils.getBoundsWithinChunkForBox(box, chunkX, chunkZ) : null;
    }

    public Set<ChunkPos> getTouchedChunks()
    {
        return PositionUtils.getTouchedChunks(this.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED));
    }

    public Set<ChunkPos> getTouchedChunksForRegion(String regionName)
    {
        return PositionUtils.getTouchedChunks(this.getSubRegionBoxFor(regionName, RequiredEnabled.PLACEMENT_ENABLED));
    }

    private void checkAreSubRegionsModified()
    {
        Map<String, BlockPos> areaPositions = this.schematic.getAreaPositions();

        if (areaPositions.size() != this.relativeSubRegionPlacements.size())
        {
            this.regionPlacementsModified = true;
            return;
        }

        for (Map.Entry<String, BlockPos> entry : areaPositions.entrySet())
        {
            SubRegionPlacement placement = this.relativeSubRegionPlacements.get(entry.getKey());

            if (placement == null || placement.isRegionPlacementModified(entry.getValue()))
            {
                this.regionPlacementsModified = true;
                return;
            }
        }

        this.regionPlacementsModified = false;
    }

    /**
     * Moves the sub-region to the given <b>absolute</b> position.
     *
     * @param regionName ()
     * @param newPos ()
     */
    public void moveSubRegionTo(String regionName, BlockPos newPos)
    {
        if (this.relativeSubRegionPlacements.containsKey(regionName))
        {
            // Marks the currently touched chunks before doing the modification


            // The input argument position is an absolute position, so need to convert to relative position here
            newPos = newPos.subtract(this.origin);
            // The absolute-based input position needs to be transformed if the entire placement has been rotated or mirrored
            newPos = PositionUtils.getReverseTransformedBlockPos(newPos, this.mirror, this.rotation);

            this.relativeSubRegionPlacements.get(regionName).setPos(newPos);
            this.onModified();
        }
    }

    public void setSubRegionRotation(String regionName, Rotation rotation)
    {


        if (this.relativeSubRegionPlacements.containsKey(regionName))
        {
            // Marks the currently touched chunks before doing the modification


            this.relativeSubRegionPlacements.get(regionName).setRotation(rotation);
            this.onModified();
        }
    }

    public void setSubRegionMirror(String regionName, Mirror mirror)
    {

        if (this.relativeSubRegionPlacements.containsKey(regionName))
        {
            // Marks the currently touched chunks before doing the modification


            this.relativeSubRegionPlacements.get(regionName).setMirror(mirror);
            this.onModified();
        }
    }


    public void resetAllSubRegionsToSchematicValues()
    {
        this.resetAllSubRegionsToSchematicValues(true);
    }

    public void resetAllSubRegionsToSchematicValues(boolean updatePlacementManager)
    {

        if (updatePlacementManager)
        {
            // Marks the currently touched chunks before doing the modification

        }

        Map<String, BlockPos> areaPositions = this.schematic.getAreaPositions();
        this.relativeSubRegionPlacements.clear();
        this.regionPlacementsModified = false;

        for (Map.Entry<String, BlockPos> entry : areaPositions.entrySet())
        {
            String name = entry.getKey();
            this.relativeSubRegionPlacements.put(name, new SubRegionPlacement(entry.getValue(), name));
        }

        if (updatePlacementManager)
        {
            this.updateEnclosingBox();
        }
    }

    public void resetSubRegionToSchematicValues(String regionName)
    {

        BlockPos pos = this.schematic.getSubRegionPosition(regionName);
        SubRegionPlacement placement = this.relativeSubRegionPlacements.get(regionName);

        if (pos != null && placement != null)
        {
            // Marks the currently touched chunks before doing the modification


            placement.resetToOriginalValues();
            this.onModified();
        }
    }

    public SchematicPlacement setOrigin(BlockPos origin)
    {


        origin = PositionUtils.getModifiedPartiallyLockedPosition(this.origin, origin, this.coordinateLockMask);

        if (this.origin.equals(origin) == false)
        {
            // Marks the currently touched chunks before doing the modification


            this.origin = origin;
            this.updateEnclosingBox();
        }

        return this;
    }

    public SchematicPlacement setRotation(Rotation rotation)
    {

        if (this.rotation != rotation)
        {
            // Marks the currently touched chunks before doing the modification


            this.rotation = rotation;
            this.updateEnclosingBox();
        }

        return this;
    }

    public SchematicPlacement setMirror(Mirror mirror)
    {
        if (this.mirror != mirror)
        {
            // Marks the currently touched chunks before doing the modification


            this.mirror = mirror;
            this.updateEnclosingBox();
        }

        return this;
    }

    private void onModified()
    {
        this.checkAreSubRegionsModified();
        this.updateEnclosingBox();
    }

    public void onRemoved()
    {
        if (USED_COLORS.isEmpty())
        {
        }
    }

    private Box getEnclosingBox()
    {
        ImmutableMap<@NotNull String, @NotNull Box> boxes = this.getSubRegionBoxes(RequiredEnabled.ANY);
        BlockPos pos1 = null;
        BlockPos pos2 = null;

        for (Box box : boxes.values())
        {
            BlockPos tmp;
            tmp = PositionUtils.getMinCorner(box.getPos1(), box.getPos2());

            if (pos1 == null)
            {
                pos1 = tmp;
            } else if (tmp.getX() < pos1.getX() || tmp.getY() < pos1.getY() || tmp.getZ() < pos1.getZ())
            {
                pos1 = PositionUtils.getMinCorner(tmp, pos1);
            }

            tmp = PositionUtils.getMaxCorner(box.getPos1(), box.getPos2());

            if (pos2 == null)
            {
                pos2 = tmp;
            } else if (tmp.getX() > pos2.getX() || tmp.getY() > pos2.getY() || tmp.getZ() > pos2.getZ())
            {
                pos2 = PositionUtils.getMaxCorner(tmp, pos2);
            }
        }

        if (pos1 != null && pos2 != null)
        {
            return new Box(pos1, pos2, "Enclosing Box (Servux)");
        }

        return null;
    }

    public void pasteTo(ServerLevel serverWorld, ReplaceBehavior replaceBehavior, PasteLayerBehavior layerBehavior, @Nullable LayerRange layerRange)
    {
		Box bb = this.getEnclosingBox();

		if (bb != null)
		{
			bb.toVanilla().intersectingChunks().forEach(chunkPos ->
					                                            SchematicPlacingUtils.placeToWorldWithinChunk(serverWorld, chunkPos,
					                                                                                          this,
					                                                                                          replaceBehavior,
					                                                                                          layerBehavior,
					                                                                                          layerRange, false)
			);
		}
        // todo CREATE TASK MANAGER
    }
}
