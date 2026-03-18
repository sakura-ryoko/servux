package fi.dy.masa.servux.scheduler;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;

import fi.dy.masa.servux.util.StringUtils;

public class TaskFeedbackListener implements ITaskCompletionListener
{
	private final List<Component> feedback = new ArrayList<>();

	public void addFeedback(Component line)
	{
		this.feedback.add(line);
	}

	public boolean hasFeedback()
	{
		return !this.feedback.isEmpty();
	}

	public void displayFeedback(TaskContext context)
	{
		if (!this.feedback.isEmpty())
		{
			for (Component line : this.feedback)
			{
				context.player().sendSystemMessage(line, false);
			}
		}
	}

	public void clearFeedback()
	{
		this.feedback.clear();
	}

	@Override
	public void onTaskCompleted(TaskContext context)
	{
		if (this.hasFeedback())
		{
			this.displayFeedback(context);
			this.clearFeedback();
		}

		context.player().sendSystemMessage(StringUtils.translate("servux.scheduler.feedback.completed", context.name(), (System.currentTimeMillis() - context.startTime())), false);
	}

	@Override
	public void onTaskAborted(TaskContext context)
	{
		if (this.hasFeedback())
		{
			this.displayFeedback(context);
			this.clearFeedback();
		}

		context.player().sendSystemMessage(StringUtils.translate("servux.scheduler.feedback.aborted", context.name(), (System.currentTimeMillis() - context.startTime())), false);
	}
}
