package fi.dy.masa.servux.scheduler.tasks;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gamerules.GameRules;

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
	protected long gameRuleProbeTimeout;
	protected long maxGameRuleProbeTime = 2000000000L; // 2 second timeout
	protected long taskStartTimeForCurrentTick;
	protected boolean shouldEnableFeedback;

//	protected final Queue<String> queuedCommands = Queues.newArrayDeque();
	protected Runnable initTask = this::initPhaseStartProbe;
//	protected Runnable probeTask = this::probePhase;
	protected Runnable waitForChunkTask = this::fetchNextChunk;
	protected Runnable processBoxBlocksTask;
	protected Runnable processBoxEntitiesTask;

	public enum TaskPhase
	{
		INIT,
//		GAME_RULE_PROBE,
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

//		if (this.phase == TaskPhase.GAME_RULE_PROBE)
//		{
//			this.probeTask.run();
//			profiler.pop();
//			return false;
//		}

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
			this.updateInfoHudLines();
		}

		profiler.pop();
		return false;
	}

	protected void initPhaseStartProbe()
	{
		this.checkCommandFeedbackGameRuleState(this.context.server());
		this.gameRuleProbeTimeout = Util.getNanos() + this.maxGameRuleProbeTime;
//		this.phase = TaskPhase.GAME_RULE_PROBE;
		this.shouldEnableFeedback = false;
		this.phase = TaskPhase.WAIT_FOR_CHUNKS;
	}

	protected void probePhase()
	{
		if (Util.getNanos() > this.gameRuleProbeTimeout)
		{
			this.shouldEnableFeedback = false;
			this.phase = TaskPhase.WAIT_FOR_CHUNKS;
		}
	}

	private void checkCommandFeedbackGameRuleState(MinecraftServer server)
	{
		boolean value = server.getGameRules().get(GameRules.SEND_COMMAND_FEEDBACK);

		if (value)
		{
			server.getGameRules().set(GameRules.SEND_COMMAND_FEEDBACK, Boolean.FALSE, server);
			this.shouldEnableFeedback = true;
		}
		else
		{
			this.shouldEnableFeedback = false;
		}

		this.phase = TaskPhase.WAIT_FOR_CHUNKS;
	}

	private void enableCommandFeedback(MinecraftServer server)
	{
		if (this.shouldEnableFeedback)
		{
			server.getGameRules().set(GameRules.SEND_COMMAND_FEEDBACK, Boolean.TRUE, server);
		}
	}

	protected void fetchNextChunk()
	{
		if (this.pendingChunks.isEmpty() == false)
		{
			this.sortChunkList();

			ChunkPos pos = this.pendingChunks.getFirst();

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
			this.currentBox = list.getFirst();
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

	protected void sendCommand(final String cmd)
	{
		MinecraftServer server = this.context.server();
		ParseResults<CommandSourceStack> parsed = this.parseCommand(server, cmd);
		server.execute(() -> server.getCommands().performCommand(parsed, cmd));
		++this.sentCommandsThisTick;
	}

	private ParseResults<CommandSourceStack> parseCommand(MinecraftServer server, final String cmd)
	{
		CommandDispatcher<CommandSourceStack> dispatch = server.getCommands().getDispatcher();
		return dispatch.parse(cmd, this.context.player().createCommandSourceStack());
	}

//	protected void sendQueuedCommands()
//	{
//		while (this.sentCommandsThisTick < this.maxCommandsPerTick &&
//				this.queuedCommands.isEmpty() == false)
//		{
//			this.sendCommand(this.queuedCommands.poll());
//		}
//
//		if (this.queuedCommands.isEmpty())
//		{
//			this.finishProcessingChunk(this.currentChunkPos);
//		}
//	}

	protected void sendTaskEndCommands()
	{
		if (this.shouldEnableFeedback)
		{
			this.enableCommandFeedback(this.context.server());
		}
	}
}
