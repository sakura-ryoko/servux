package fi.dy.masa.servux.scheduler.info_hud;

import fi.dy.masa.servux.util.data.tag.CompoundData;

public abstract class AbstractInfoHudSync<T>
{
	private final InfoHudSyncType type;

	public AbstractInfoHudSync(InfoHudSyncType type)
	{
		this.type = type;
	}

	public InfoHudSyncType type()
	{
		return this.type;
	}

	public abstract void addInfo(T info);

	public abstract boolean hasInfo();

	public abstract void clearInfo();

	public abstract CompoundData fillInfo();
}
