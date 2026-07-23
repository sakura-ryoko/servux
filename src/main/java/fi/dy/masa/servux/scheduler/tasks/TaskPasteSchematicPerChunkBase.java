package fi.dy.masa.servux.scheduler.tasks;

import java.util.Collection;
import java.util.Set;
import com.google.common.collect.ImmutableList;

import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.servux.scheduler.TaskContext;
import fi.dy.masa.servux.schematic.placement.SchematicPlacement;
import fi.dy.masa.servux.util.PasteLayerBehavior;
import fi.dy.masa.servux.util.ReplaceBehavior;
import fi.dy.masa.servux.util.IntBoundingBox;
import fi.dy.masa.servux.util.LayerRange;
import fi.dy.masa.servux.util.position.PositionUtils;

public abstract class TaskPasteSchematicPerChunkBase extends TaskProcessChunkMultiPhase
{
	protected final ImmutableList<SchematicPlacement> placements;
	protected final LayerRange layerRange;
	protected final ReplaceBehavior replaceBehavior;
	protected final PasteLayerBehavior layerBehavior;
	protected final boolean changedBlockOnly;
	protected final boolean ignoreBlocks;
	protected final boolean ignoreEntities;

	public TaskPasteSchematicPerChunkBase(TaskContext context,
	                                      final Collection<SchematicPlacement> placements,
	                                      final LayerRange layerRange,
	                                      final ReplaceBehavior replaceBehavior,
	                                      final PasteLayerBehavior layerBehavior,
	                                      final boolean changedBlockOnly,
	                                      final boolean ignoreBlocks,
	                                      final boolean ignoreEntities)
	{
		super(context);

		this.placements = ImmutableList.copyOf(placements);
		this.layerRange = layerRange;
		this.replaceBehavior = replaceBehavior;
		this.layerBehavior = layerBehavior;
		this.changedBlockOnly = changedBlockOnly;
		this.ignoreBlocks = ignoreBlocks;
		this.ignoreEntities = ignoreEntities;
	}

	@Override
	public void init()
	{
		for (SchematicPlacement placement : this.placements)
		{
			this.addPlacement(placement, this.layerRange);
		}

		this.pendingChunks.clear();
		this.pendingChunks.addAll(this.boxesInChunks.keySet());
		this.sortChunkList();
	}

	protected void addPlacement(SchematicPlacement placement, LayerRange range)
	{
		Set<ChunkPos> touchedChunks = placement.getTouchedChunks();

		for (ChunkPos pos : touchedChunks)
		{
			int count = 0;

			for (IntBoundingBox box : placement.getBoxesWithinChunk(pos.x, pos.z).values())
			{
				box = PositionUtils.getClampedBox(box, range);

				if (box != null)
				{
					// Clamp the box to the world bounds.
					// This is also important for the fill-based strip generation code to not
					// overflow the work array bounds.
					box = PositionUtils.clampBoxToWorldHeightRange(box, this.context.level());

					if (box != null)
					{
						this.boxesInChunks.put(pos, box);
						++count;
					}
				}
			}

			if (count > 0)
			{
				this.onChunkAddedForHandling(pos, placement);
			}
		}
	}

	protected void onChunkAddedForHandling(ChunkPos pos, SchematicPlacement placement)
	{
	}

	@Override
	protected boolean canProcessChunk(ChunkPos pos)
	{
		return this.areSurroundingChunksLoaded(pos, this.context.level(), 1);
	}
}
