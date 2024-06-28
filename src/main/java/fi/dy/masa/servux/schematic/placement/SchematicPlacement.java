package fi.dy.masa.servux.schematic.placement;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.schematic.LitematicaSchematic;
import fi.dy.masa.servux.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.servux.schematic.selection.Box;
import fi.dy.masa.servux.util.IntBoundingBox;
import fi.dy.masa.servux.util.PositionUtils;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;

public class SchematicPlacement
{
    private static final Set<Integer> USED_COLORS = new HashSet<>();
    private static int nextColorIndex;

    private final Map<String, SubRegionPlacement> relativeSubRegionPlacements = new HashMap<>();
    private final int subRegionCount;
    private LitematicaSchematic schematic;
    private BlockPos origin;
    private String name;
    private BlockRotation rotation = BlockRotation.NONE;
    private BlockMirror mirror = BlockMirror.NONE;
    private boolean ignoreEntities;
    private boolean enabled;
    private boolean enableRender;
    private boolean renderEnclosingBox;
    private boolean regionPlacementsModified;
    private boolean locked;
    private boolean shouldBeSaved = true;
    private int coordinateLockMask;
    @Nullable
    private Box enclosingBox;
    @Nullable
    private String selectedSubRegionName;

    public SchematicPlacement(LitematicaSchematic schematic, BlockPos origin, String name, boolean enabled, boolean enableRender)
    {
        this.schematic = schematic;
        this.origin = origin;
        this.name = name;
        this.subRegionCount = schematic.getSubRegionCount();
        this.enabled = enabled;
        this.enableRender = enableRender;
    }

    public static SchematicPlacement createFor(LitematicaSchematic schematic, BlockPos origin, String name, boolean enabled, boolean enableRender)
    {
        SchematicPlacement placement = new SchematicPlacement(schematic, origin, name, enabled, enableRender);
        placement.resetAllSubRegionsToSchematicValues();

        return placement;
    }

    /**
     * Creates a placement that can be used for schematic conversions.
     * The origin point will be adjusted such that the actual minimum corner of the sub regions
     * will be at the provided origin point.
     * Also, this placement will not affect the SchematicPlacementManager and cause
     * schematic chunk rebuilds, nor will it affect the rendering related things.
     * @param schematic
     * @param origin
     * @return
     */
    public static SchematicPlacement createForSchematicConversion(LitematicaSchematic schematic, BlockPos origin)
    {
        // Adjust the origin such that the actual sub regions minimum corner is at the provided origin,
        // regardless of where the defined origin point is in relation to the minimum corner.
        Pair<BlockPos, BlockPos> pair = PositionUtils.getEnclosingAreaCorners(schematic.getAreas().values());
        BlockPos originAdjusted = pair != null ? origin.subtract(pair.getLeft()) : origin;

        return createTemporary(schematic, originAdjusted);
    }

    /**
     * Creates a temporary placement which doesn't affect the SchematicPlacementManager
     * @param schematic
     * @param origin
     * @return
     */
    public static SchematicPlacement createTemporary(LitematicaSchematic schematic, BlockPos origin)
    {
        SchematicPlacement placement = new SchematicPlacement(schematic, origin, "?", true, true);
        placement.resetAllSubRegionsToSchematicValues(false);

        return placement;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public boolean isRenderingEnabled()
    {
        return this.isEnabled() && this.enableRender;
    }

    public boolean isLocked()
    {
        return this.locked;
    }

    public boolean shouldRenderEnclosingBox()
    {
        return this.renderEnclosingBox;
    }

    /**
     * Returns whether or not this placement should be saved by the SchematicPlacementManager
     * when it saves the list of placements.
     * @return
     */
    public boolean shouldBeSaved()
    {
        return this.shouldBeSaved;
    }

    public void setShouldBeSaved(boolean shouldbeSaved)
    {
        this.shouldBeSaved = shouldbeSaved;
    }

    public boolean matchesRequirement(RequiredEnabled required)
    {
        switch (required)
        {
            case ANY:
                return true;
            case PLACEMENT_ENABLED:
                return this.isEnabled();
            default:
                return this.isEnabled() && this.enableRender;
        }
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

    public BlockRotation getRotation()
    {
        return rotation;
    }

    public BlockMirror getMirror()
    {
        return mirror;
    }


    public int getSubRegionCount()
    {
        return this.subRegionCount;
    }

    public StructurePlacementData getPlacementSettings()
    {
        StructurePlacementData placement = new StructurePlacementData();

        placement.setMirror(this.mirror);
        placement.setRotation(this.rotation);
        placement.setIgnoreEntities(this.ignoreEntities);

        return placement;
    }

    @Nullable
    public String getSelectedSubRegionName()
    {
        return this.selectedSubRegionName;
    }

    public void setSelectedSubRegionName(@Nullable String name)
    {
        this.selectedSubRegionName = name;
    }

    @Nullable
    public SubRegionPlacement getSelectedSubRegionPlacement()
    {
        return this.selectedSubRegionName != null ? this.relativeSubRegionPlacements.get(this.selectedSubRegionName) : null;
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

    public ImmutableMap<String, SubRegionPlacement> getEnabledRelativeSubRegionPlacements()
    {
        ImmutableMap.Builder<String, SubRegionPlacement> builder = ImmutableMap.builder();

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
            ImmutableMap<String, Box> boxes = this.getSubRegionBoxes(RequiredEnabled.ANY);
            BlockPos pos1 = null;
            BlockPos pos2 = null;

            for (Box box : boxes.values())
            {
                BlockPos tmp;
                tmp = PositionUtils.getMinCorner(box.getPos1(), box.getPos2());

                if (pos1 == null)
                {
                    pos1 = tmp;
                }
                else if (tmp.getX() < pos1.getX() || tmp.getY() < pos1.getY() || tmp.getZ() < pos1.getZ())
                {
                    pos1 = PositionUtils.getMinCorner(tmp, pos1);
                }

                tmp = PositionUtils.getMaxCorner(box.getPos1(), box.getPos2());

                if (pos2 == null)
                {
                    pos2 = tmp;
                }
                else if (tmp.getX() > pos2.getX() || tmp.getY() > pos2.getY() || tmp.getZ() > pos2.getZ())
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

    public ImmutableMap<String, Box> getSubRegionBoxes(RequiredEnabled required)
    {
        ImmutableMap.Builder<String, Box> builder = ImmutableMap.builder();
        Map<String, BlockPos> areaSizes = this.schematic.getAreaSizes();

        for (Map.Entry<String, SubRegionPlacement> entry : this.relativeSubRegionPlacements.entrySet())
        {
            String name = entry.getKey();
            BlockPos areaSize = areaSizes.get(name);

            if (areaSize == null)
            {
                Servux.logger.warn("SchematicPlacement.getSubRegionBoxes(): Size for sub-region '{}' not found in the schematic '{}'", name, this.schematic.getMetadata().getName());
                continue;
            }

            SubRegionPlacement placement = entry.getValue();

            if (placement.matchesRequirement(required))
            {
                BlockPos boxOriginRelative = placement.getPos();
                BlockPos boxOriginAbsolute = PositionUtils.getTransformedBlockPos(boxOriginRelative, this.mirror, this.rotation).add(this.origin);
                BlockPos pos2 = PositionUtils.getRelativeEndPositionFromAreaSize(areaSize);
                pos2 = PositionUtils.getTransformedBlockPos(pos2, this.mirror, this.rotation);
                pos2 = PositionUtils.getTransformedBlockPos(pos2, placement.getMirror(), placement.getRotation()).add(boxOriginAbsolute);

                builder.put(name, new Box(boxOriginAbsolute, pos2, name));
            }
        }

        return builder.build();
    }

    public ImmutableMap<String, Box> getSubRegionBoxFor(String regionName, RequiredEnabled required)
    {
        ImmutableMap.Builder<String, Box> builder = ImmutableMap.builder();
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
                    BlockPos boxOriginAbsolute = PositionUtils.getTransformedBlockPos(boxOriginRelative, this.mirror, this.rotation).add(this.origin);
                    BlockPos pos2 = PositionUtils.getRelativeEndPositionFromAreaSize(areaSize);
                    pos2 = PositionUtils.getTransformedBlockPos(pos2, this.mirror, this.rotation);
                    pos2 = PositionUtils.getTransformedBlockPos(pos2, placement.getMirror(), placement.getRotation()).add(boxOriginAbsolute);

                    builder.put(regionName, new Box(boxOriginAbsolute, pos2, regionName));
                }
                else
                {
                    Servux.logger.warn("SchematicPlacement.getSubRegionBoxFor(): Size for sub-region '{}' not found in the schematic '{}'", regionName, this.schematic.getMetadata().getName());
                }
            }
        }

        return builder.build();
    }

    public Set<String> getRegionsTouchingChunk(int chunkX, int chunkZ)
    {
        ImmutableMap<String, Box> map = this.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED);
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

    public ImmutableMap<String, IntBoundingBox> getBoxesWithinChunk(int chunkX, int chunkZ)
    {
        ImmutableMap<String, Box> subRegions = this.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED);
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
     * @param regionName
     * @param newPos
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

    public void setSubRegionRotation(String regionName, BlockRotation rotation)
    {


        if (this.relativeSubRegionPlacements.containsKey(regionName))
        {
            // Marks the currently touched chunks before doing the modification
            

            this.relativeSubRegionPlacements.get(regionName).setRotation(rotation);
            this.onModified();
        }
    }

    public void setSubRegionMirror(String regionName, BlockMirror mirror)
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

    public void setEnabled(boolean enabled)
    {
        if (enabled != this.enabled)
        {
            // Marks the currently touched chunks before doing the modification
            

            this.enabled = enabled;
            this.updateEnclosingBox();
        }
    }

    public void toggleEnabled()
    {
        this.setEnabled(! this.enabled);
    }

    public void setRenderSchematic(boolean render)
    {
        if (render != this.enableRender)
        {
            // Marks the currently touched chunks before doing the modification
            

            this.enableRender = render;
            this.updateEnclosingBox();
        }
    }

    public void toggleSubRegionRenderingEnabled(String regionName)
    {
        SubRegionPlacement placement = this.relativeSubRegionPlacements.get(regionName);

        if (placement != null)
        {
            placement.toggleRenderingEnabled();
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

    public SchematicPlacement setRotation(BlockRotation rotation)
    {

        if (this.rotation != rotation)
        {
            // Marks the currently touched chunks before doing the modification
            

            this.rotation = rotation;
            this.updateEnclosingBox();
        }

        return this;
    }

    public SchematicPlacement setMirror(BlockMirror mirror)
    {
        if (this.mirror != mirror)
        {
            // Marks the currently touched chunks before doing the modification
            

            this.mirror = mirror;
            this.updateEnclosingBox();
        }

        return this;
    }


    private void onModified(SchematicPlacementManager manager)
    {
        this.updateEnclosingBox();
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
            nextColorIndex = 0;
        }
    }

}
