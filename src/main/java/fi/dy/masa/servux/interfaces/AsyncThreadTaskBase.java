package fi.dy.masa.servux.interfaces;

import java.util.concurrent.CompletableFuture;

/**
 * Basic runAsync() task handler structure --
 * This is meant to be extended and managed by {@link IThreadDaemonExecutor}
 * -
 * NOTE: Async tasks can often run "out of sequence", such as "3, 0, 2, 1, 4"
 */
public abstract class AsyncThreadTaskBase extends AbstractThreadTaskBase
{
	/**
	 * Run the task {@link CompletableFuture}
	 * @return (null)
	 */
	@Override
	public abstract CompletableFuture<Void> runAsync();
}
