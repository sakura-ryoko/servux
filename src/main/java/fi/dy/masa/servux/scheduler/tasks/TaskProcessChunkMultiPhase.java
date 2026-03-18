package fi.dy.masa.servux.scheduler.tasks;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.servux.scheduler.TaskContext;
import fi.dy.masa.servux.util.position.IntBoundingBox;

public abstract class TaskProcessChunkMultiPhase extends TaskProcessChunkBase
{
	protected TaskPhase phase = TaskPhase.INIT;
	@Nullable protected ChunkPos currentChunkPos;
	@Nullable protected IntBoundingBox currentBox;
	@Nullable protected Iterator<Entity> entityIterator;
	@Nullable protected Iterator<BlockPos> positionIterator;

	protected int maxCommandsPerTick = 16;
	protected int processedChunksThisTick;
	protected int sentCommandsThisTick;
	protected long taskStartTimeForCurrentTick;

	protected Runnable initTask = this::initPhase;
	protected Runnable waitForChunkTask = this::fetchNextChunk;
	protected Runnable processBoxBlocksTask;
	protected Runnable processBoxEntitiesTask;

	public enum TaskPhase
	{
		INIT,
		WAIT_FOR_CHUNKS,
		PROCESS_BOX_BLOCKS,
		PROCESS_BOX_ENTITIES,
		FINISHED
	}

	protected TaskProcessChunkMultiPhase(TaskContext context)
	{
		super(context);
	}

	protected boolean executeMultiPhase(ProfilerFiller profiler)
	{
		profiler.push("chunk_multi_phase");
		this.taskStartTimeForCurrentTick = Util.getNanos();
		this.sentCommandsThisTick = 0;
		this.processedChunksThisTick = 0;

		if (this.phase == TaskPhase.INIT)
		{
			this.initTask.run();
		}

		if (this.currentChunkPos != null && this.canProcessChunk(this.currentChunkPos) == false)
		{
			profiler.pop();
			return false;
		}

		int commandsLast = -1;
		int processedChunksLast = -1;

		while (this.sentCommandsThisTick < this.maxCommandsPerTick &&
				(this.sentCommandsThisTick > commandsLast || this.processedChunksThisTick != processedChunksLast))
		{
			long currentTime = Util.getNanos();
			long elapsedTickTime = (currentTime - this.taskStartTimeForCurrentTick);

			if (elapsedTickTime >= 25000000L)
			{
				break;
			}

			commandsLast = this.sentCommandsThisTick;
			processedChunksLast = this.processedChunksThisTick;

			if (this.phase == TaskPhase.WAIT_FOR_CHUNKS)
			{
				this.waitForChunkTask.run();
			}

			if (this.phase == TaskPhase.PROCESS_BOX_BLOCKS && this.processBoxBlocksTask != null)
			{
				this.processBoxBlocksTask.run();
			}

			if (this.phase == TaskPhase.PROCESS_BOX_ENTITIES && this.processBoxEntitiesTask != null)
			{
				this.processBoxEntitiesTask.run();
			}

			if (this.phase == TaskPhase.FINISHED)
			{
				profiler.pop();
				return true;
			}
		}

		if (this.processedChunksThisTick > 0)
		{
//			this.updateInfoHudLines();
		}

		profiler.pop();
		return false;
	}

	protected void initPhase()
	{
		this.phase = TaskPhase.WAIT_FOR_CHUNKS;
	}

	protected void fetchNextChunk()
	{
		if (this.pendingChunks.isEmpty() == false)
		{
			this.sortChunkList();

			ChunkPos pos = this.pendingChunks.get(0);

			if (this.canProcessChunk(pos))
			{
				this.currentChunkPos = pos;
				this.onNextChunkFetched(pos);
			}
		}
		else
		{
			this.phase = TaskPhase.FINISHED;
			this.finished = true;
		}
	}

	protected void onNextChunkFetched(ChunkPos pos)
	{
	}

	protected void startNextBox(ChunkPos pos)
	{
		List<IntBoundingBox> list = this.boxesInChunks.get(pos);

		if (list.isEmpty() == false)
		{
			this.currentBox = list.get(0);
			this.onStartNextBox(this.currentBox);
		}
		else
		{
			this.currentBox = null;
			this.phase = TaskPhase.WAIT_FOR_CHUNKS;
		}
	}

	protected void onStartNextBox(IntBoundingBox box)
	{
	}

	protected void onFinishedProcessingBox(ChunkPos pos, IntBoundingBox box)
	{
		this.boxesInChunks.remove(pos, box);
		this.currentBox = null;
		this.entityIterator = null;
		this.positionIterator = null;

		if (this.boxesInChunks.get(pos).isEmpty())
		{
			this.finishProcessingChunk(pos);
		}
		else
		{
			this.startNextBox(pos);
		}
	}

	protected void finishProcessingChunk(ChunkPos pos)
	{
		this.boxesInChunks.removeAll(pos);
		this.pendingChunks.remove(pos);
		this.currentChunkPos = null;
		++this.processedChunksThisTick;
		this.phase = TaskPhase.WAIT_FOR_CHUNKS;
		this.onFinishedProcessingChunk(pos);
	}

	protected void onFinishedProcessingChunk(ChunkPos pos)
	{
	}
}
