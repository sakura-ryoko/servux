package fi.dy.masa.servux.scheduler.tasks;

import java.util.function.BooleanSupplier;

import net.minecraft.util.profiling.ProfilerFiller;

import fi.dy.masa.servux.scheduler.TaskScheduler;

public class TaskDelay extends TaskBase
{
	protected final TaskScheduler scheduler;
	protected final TaskBase task;
	protected final BooleanSupplier startConditionChecker;
	protected final int interval;

	public TaskDelay(TaskBase task, int interval, TaskScheduler scheduler, BooleanSupplier startConditionChecker)
	{
		super(task.context);
		this.task = task;
		this.scheduler = scheduler;
		this.interval = interval;
		this.startConditionChecker = startConditionChecker;
	}

	@Override
	public boolean execute(ProfilerFiller profiler)
	{
		if (this.startConditionChecker.getAsBoolean())
		{
			this.scheduler.scheduleTask(this.task, this.interval);
			this.finished = true;
			return true;
		}

		return false;
	}
}
