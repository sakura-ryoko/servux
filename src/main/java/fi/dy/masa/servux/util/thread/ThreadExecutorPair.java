package fi.dy.masa.servux.util.thread;

import fi.dy.masa.servux.interfaces.IThreadDaemonExecutor;
import fi.dy.masa.servux.interfaces.IThreadTaskBase;

public record ThreadExecutorPair<T extends IThreadTaskBase>(Thread thread, IThreadDaemonExecutor<T> executor)
{
}
