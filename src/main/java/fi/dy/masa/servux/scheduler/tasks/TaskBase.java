package fi.dy.masa.servux.scheduler.tasks;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import fi.dy.masa.servux.scheduler.ITaskCompletionListener;
import fi.dy.masa.servux.scheduler.ITask;
import fi.dy.masa.servux.scheduler.TaskContext;
import fi.dy.masa.servux.scheduler.TaskTimer;

public abstract class TaskBase implements ITask
{
	protected final TaskContext context;
	protected String name = "";
	private TaskTimer timer = new TaskTimer(1);
	@Nullable private ITaskCompletionListener completionListener;
	protected boolean finished;

	protected TaskBase(TaskContext context)
	{
		this.context = context;
	}

	@Override
	public TaskTimer getTimer()
	{
		return this.timer;
	}

	@Override
	public String getDisplayName()
	{
		return this.name;
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
}
