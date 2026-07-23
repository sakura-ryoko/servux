package fi.dy.masa.servux.scheduler.tasks;

import java.util.List;

import net.minecraft.world.level.block.Blocks;

import fi.dy.masa.servux.scheduler.TaskContext;
import fi.dy.masa.servux.schematic.selection.Box;
import fi.dy.masa.servux.util.StringUtils;

public class TaskDeleteArea extends TaskFillArea
{
	public TaskDeleteArea(TaskContext ctx, List<Box> boxes, boolean removeEntities)
	{
		super(ctx, boxes, Blocks.AIR.defaultBlockState(), null, removeEntities);
	}

	@Override
	protected void printCompletionMessage()
	{
		if (this.finished)
		{
			this.context.listener().addFeedback(StringUtils.translate("servux.scheduler.task.delete_area.successful"));
		}
		else
		{
			this.context.listener().addFeedback(StringUtils.translate("servux.scheduler.task.delete_area.interrupted"));
		}
	}
}
