package fi.dy.masa.servux.scheduler;

public interface ITaskCompletionListener
{
	/**
	 * Called when a task wants to inform a listener about the task being completed
	 */
	void onTaskCompleted(TaskContext context);

	/**
	 * Called when a task wants to inform a listener about the task being aborted before completion
	 */
	default void onTaskAborted(TaskContext context) {}
}
