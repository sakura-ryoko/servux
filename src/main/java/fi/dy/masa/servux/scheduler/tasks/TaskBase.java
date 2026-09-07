package fi.dy.masa.servux.scheduler.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import fi.dy.masa.servux.scheduler.ITask;
import fi.dy.masa.servux.scheduler.ITaskCompletionListener;
import fi.dy.masa.servux.scheduler.TaskContext;
import fi.dy.masa.servux.scheduler.TaskTimer;
import fi.dy.masa.servux.scheduler.info_hud.InfoHudSync;
import fi.dy.masa.servux.scheduler.info_hud.InfoHudSyncChunks;
import fi.dy.masa.servux.util.MathUtils;
import fi.dy.masa.servux.util.position.PositionUtils;

public abstract class TaskBase implements ITask
{
	protected PositionUtils.ChunkPosComparator chunkPosComparator = new PositionUtils.ChunkPosComparator();
//	protected PositionUtils.BlockPosComparator blockPosComparator = new PositionUtils.BlockPosComparator();
	protected final TaskContext context;
	protected final InfoHudSync infoHudSync;
	private TaskTimer timer = new TaskTimer(1);
	@Nullable private ITaskCompletionListener completionListener;
	protected boolean finished;

	protected TaskBase(TaskContext context)
	{
		this.context = context;
		this.setCompletionListener(this.context.listener());
		this.infoHudSync = new InfoHudSync();
		this.chunkPosComparator = this.chunkPosComparator.setReferencePosition(context.player().blockPosition());
//		this.blockPosComparator = this.blockPosComparator.setReferencePosition(context.player().blockPosition());
	}

	@Override
	public TaskTimer getTimer()
	{
		return this.timer;
	}

	@Override
	public String getDisplayName()
	{
		return this.context.name();
	}

	@Override
	public void createTimer(int interval)
	{
		this.timer = new TaskTimer(interval);
	}

	public void setCompletionListener(@Nullable ITaskCompletionListener listener)
	{
		this.completionListener = listener;
	}

	@Override
	public boolean canExecute()
	{
		return true;
	}

	@Override
	public boolean shouldRemove()
	{
		return this.canExecute() == false;
	}

	@Override
	public void init()
	{
	}

	@Override
	public void stop()
	{
		this.notifyListener();
	}

	protected void notifyListener()
	{
		if (this.completionListener != null)
		{
			this.context.server()
			            .execute(() ->
			                     {
									 if (this.finished)
									 {
										 this.completionListener.onTaskCompleted(this.context);
									 }
									 else
									 {
										 this.completionListener.onTaskAborted(this.context);
									 }
								 });
		}
	}

	protected void runWhenDone()
	{
		if (this.context.whenDone() != null)
		{
			this.context.whenDone().run();
		}
	}

	protected boolean areSurroundingChunksLoaded(ChunkPos pos, ServerLevel world, int radius)
	{
		if (radius <= 0)
		{
			return this.isServerChunkLoaded(world, pos.x(), pos.z());
		}

		int chunkX = pos.x();
		int chunkZ = pos.z();

		for (int cx = chunkX - radius; cx <= chunkX + radius; ++cx)
		{
			for (int cz = chunkZ - radius; cz <= chunkZ + radius; ++cz)
			{
				if (this.isServerChunkLoaded(world, cx, cz) == false)
				{
					return false;
				}
			}
		}

		return true;
	}

	protected boolean isServerChunkLoaded(ServerLevel world, int chunkX, int chunkZ)
	{
		return world.getChunkSource().hasChunk(chunkX, chunkZ);
	}

	protected ChunkAccess loadServerChunk(ServerLevel world, int chunkX, int chunkZ)
	{
		return world.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
	}

	protected void updateInfoHudLinesPendingChunks(Collection<ChunkPos> pendingChunks)
	{
		if (!pendingChunks.isEmpty())
		{
			List<ChunkPos> list = new ArrayList<>(pendingChunks);
			InfoHudSyncChunks infoHud = new InfoHudSyncChunks();

			this.chunkPosComparator.setReferencePosition(BlockPos.containing(this.context.player().position()));
			this.chunkPosComparator.setClosestFirst(true);
			list.sort(this.chunkPosComparator);

//			final String pre = ChatFormatting.WHITE.toString() + ChatFormatting.BOLD.toString();
//			final String title = StringUtils.translateAsString("servux.scheduler.hud_sync.title.remaining_chunks", this.getDisplayName(), pendingChunks.size());
			final String title = this.getDisplayName();
			final int maxLines = MathUtils.min(list.size(), 10);

			InfoHudSyncChunks.Entry entry = new InfoHudSyncChunks.Entry(title, pendingChunks.size(), -1, -1);
//			infoHud.addInfo(String.format("%s%s%s", pre, title, ChatFormatting.RESET.toString()));

			for (int i = 0; i < maxLines; ++i)
			{
				ChunkPos pos = list.get(i);
//				infoHud.addInfo(String.format("cx: %5d, cz: %5d (x: %d, z: %d)", pos.x(), pos.z(), pos.x() << 4, pos.z() << 4));
				infoHud.addInfo(entry.nextChunk(pos.x(), pos.z()));
			}

			this.infoHudSync.addInfo(infoHud);
			this.infoHudSync.onTransmitInfo(this.context);
		}
	}
}
