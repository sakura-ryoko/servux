package fi.dy.masa.servux.scheduler.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import com.google.common.collect.ArrayListMultimap;

import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.servux.scheduler.TaskContext;
import fi.dy.masa.servux.schematic.selection.Box;
import fi.dy.masa.servux.util.position.IntBoundingBox;
import fi.dy.masa.servux.util.position.LayerMode;
import fi.dy.masa.servux.util.position.LayerRange;
import fi.dy.masa.servux.util.position.PositionUtils;

public abstract class TaskProcessChunkBase extends TaskBase
{
	protected final ArrayListMultimap<ChunkPos, IntBoundingBox> boxesInChunks = ArrayListMultimap.create();
	protected final ArrayList<ChunkPos> pendingChunks = new ArrayList<>();
	protected PositionUtils.ChunkPosComparator comparator = new PositionUtils.ChunkPosComparator();

	protected TaskProcessChunkBase(TaskContext context)
	{
		super(context);
		this.comparator.setClosestFirst(true);
	}

	@Override
	public boolean execute(ProfilerFiller profiler)
	{
		return this.executeForAllPendingChunks(profiler);
	}

	@Override
	public void stop()
	{
		this.context.server().execute(this::onStop);
	}

	protected void onStop()
	{
		this.infoHudSync.onStop(this.context);
		this.notifyListener();
		this.runWhenDone();
	}

	protected abstract boolean canProcessChunk(ChunkPos pos);

	protected boolean processChunk(ChunkPos pos)
	{
		return true;
	}

	protected boolean executeForAllPendingChunks(ProfilerFiller profiler)
	{
		profiler.push("process_chunks");

		Iterator<ChunkPos> iterator = this.pendingChunks.iterator();
		int processed = 0;

		while (iterator.hasNext())
		{
			ChunkPos pos = iterator.next();

			profiler.push("process_chunk");
			if (this.canProcessChunk(pos) && this.processChunk(pos))
			{
				iterator.remove();
				++processed;
			}
			profiler.pop();
		}

		if (processed > 0)
		{
			this.updateInfoHudLinesPendingChunks(this.pendingChunks);
		}

		this.finished = this.pendingChunks.isEmpty();

		profiler.pop();
		return this.finished;
	}

	protected void addPerChunkBoxes(Collection<Box> allBoxes)
	{
		this.addPerChunkBoxes(allBoxes, new LayerRange());
	}

	protected void addPerChunkBoxes(Collection<Box> allBoxes, LayerRange range)
	{
		this.boxesInChunks.clear();
		this.pendingChunks.clear();

		if (range.getLayerMode() == LayerMode.ALL)
		{
			PositionUtils.getPerChunkBoxes(allBoxes, this::clampToWorldHeightAndAddBox);
		}
		else
		{
			PositionUtils.getLayerRangeClampedPerChunkBoxes(allBoxes, range, this::clampToWorldHeightAndAddBox);
		}

		this.pendingChunks.addAll(this.boxesInChunks.keySet());

		this.sortChunkList();
	}

	protected void clampToWorldHeightAndAddBox(ChunkPos pos, IntBoundingBox box)
	{
		box = PositionUtils.clampBoxToWorldHeightRange(box, this.context.level());

		if (box != null)
		{
			this.boxesInChunks.put(pos, box);
		}
	}

	protected void addNonChunkClampedBoxes(Collection<Box> allBoxes)
	{
		this.addNonChunkClampedBoxes(allBoxes, new LayerRange());
	}

	protected void addNonChunkClampedBoxes(Collection<Box> allBoxes, LayerRange range)
	{
		this.boxesInChunks.clear();
		this.pendingChunks.clear();

		if (range.getLayerMode() == LayerMode.ALL)
		{
			addBoxes(allBoxes, this::clampToWorldHeightAndAddBox);
		}
		else
		{
			getLayerRangeClampedBoxes(allBoxes, range, this::clampToWorldHeightAndAddBox);
		}

		this.pendingChunks.addAll(this.boxesInChunks.keySet());

		this.sortChunkList();
	}

	protected static void addBoxes(Collection<Box> boxes, BiConsumer<ChunkPos, IntBoundingBox> consumer)
	{
		for (Box box : boxes)
		{
			int boxMinX = Math.min(box.getPos1().getX(), box.getPos2().getX());
			int boxMinY = Math.min(box.getPos1().getY(), box.getPos2().getY());
			int boxMinZ = Math.min(box.getPos1().getZ(), box.getPos2().getZ());
			int boxMaxX = Math.max(box.getPos1().getX(), box.getPos2().getX());
			int boxMaxY = Math.max(box.getPos1().getY(), box.getPos2().getY());
			int boxMaxZ = Math.max(box.getPos1().getZ(), box.getPos2().getZ());

			consumer.accept(new ChunkPos(boxMinX >> 4, boxMinZ >> 4), new IntBoundingBox(boxMinX, boxMinY, boxMinZ, boxMaxX, boxMaxY, boxMaxZ));
		}
	}

	protected static void getLayerRangeClampedBoxes(Collection<Box> boxes,
	                                                LayerRange range,
	                                                BiConsumer<ChunkPos, IntBoundingBox> consumer)
	{
		for (Box box : boxes)
		{
			final int rangeMin = range.getLayerMin();
			final int rangeMax = range.getLayerMax();
			int boxMinX = Math.min(box.getPos1().getX(), box.getPos2().getX());
			int boxMinY = Math.min(box.getPos1().getY(), box.getPos2().getY());
			int boxMinZ = Math.min(box.getPos1().getZ(), box.getPos2().getZ());
			int boxMaxX = Math.max(box.getPos1().getX(), box.getPos2().getX());
			int boxMaxY = Math.max(box.getPos1().getY(), box.getPos2().getY());
			int boxMaxZ = Math.max(box.getPos1().getZ(), box.getPos2().getZ());

			switch (range.getAxis())
			{
				case X:
					if (rangeMax < boxMinX || rangeMin > boxMaxX) { continue; }
					boxMinX = Math.max(boxMinX, rangeMin);
					boxMaxX = Math.min(boxMaxX, rangeMax);
					break;
				case Y:
					if (rangeMax < boxMinY || rangeMin > boxMaxY) { continue; }
					boxMinY = Math.max(boxMinY, rangeMin);
					boxMaxY = Math.min(boxMaxY, rangeMax);
					break;
				case Z:
					if (rangeMax < boxMinZ || rangeMin > boxMaxZ) { continue; }
					boxMinZ = Math.max(boxMinZ, rangeMin);
					boxMaxZ = Math.min(boxMaxZ, rangeMax);
					break;
			}

			consumer.accept(new ChunkPos(boxMinX >> 4, boxMinZ >> 4), new IntBoundingBox(boxMinX, boxMinY, boxMinZ, boxMaxX, boxMaxY, boxMaxZ));
		}
	}

	protected List<IntBoundingBox> getBoxesInChunk(ChunkPos pos)
	{
		return this.boxesInChunks.get(pos);
	}

	protected void sortChunkList()
	{
		if (this.pendingChunks.size() > 0)
		{
			this.pendingChunks.sort(this.comparator);
			this.onChunkListSorted();
		}
	}

	protected void onChunkListSorted()
	{
	}

	protected void updateInfoHudLines()
	{
		this.updateInfoHudLinesPendingChunks(this.pendingChunks);
	}
}
