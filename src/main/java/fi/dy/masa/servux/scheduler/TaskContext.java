package fi.dy.masa.servux.scheduler;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class TaskContext
{
	private final MinecraftServer server;
	private final ServerLevel level;
	private final ServerPlayer player;
	private final String name;
	private final long startTime;
	private final TaskFeedbackListener listener;
	private final @Nullable Runnable whenDone;

	public TaskContext(MinecraftServer server, ServerLevel level, ServerPlayer player, String name, long startTime)
	{
		this(server, level, player, name, startTime, null);
	}

	public TaskContext(MinecraftServer server, ServerLevel level, ServerPlayer player, String name, long startTime, @Nullable Runnable whenDone)
	{
		this.server = server;
		this.level = level;
		this.player = player;
		this.name = name;
		this.startTime = startTime;
		this.listener = new TaskFeedbackListener();
		this.whenDone = whenDone;
	}

	public MinecraftServer server()
	{
		return this.server;
	}

	public ServerLevel level()
	{
		return this.level;
	}

	public ServerPlayer player()
	{
		return this.player;
	}

	public String name()
	{
		return this.name;
	}

	public long startTime()
	{
		return this.startTime;
	}

	public TaskFeedbackListener listener()
	{
		return this.listener;
	}

	public @Nullable Runnable whenDone()
	{
		return this.whenDone;
	}
}
