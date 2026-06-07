package fi.dy.masa.servux.interfaces;

public interface IThreadTaskBase
{
	/**
	 * Check if the task is marked as "finished"
	 * @return (bool)
	 */
	boolean isFinished();

	/**
	 * Mark the task as finished.
	 */
	void finish();

		/**
	 * Run the task using {@link Runnable}
	 */
	default void run()
	{
	}
}
