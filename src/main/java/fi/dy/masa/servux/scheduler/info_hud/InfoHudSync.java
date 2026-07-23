package fi.dy.masa.servux.scheduler.info_hud;

import java.util.ArrayList;
import java.util.List;

import fi.dy.masa.servux.dataproviders.LitematicsDataProvider;
import fi.dy.masa.servux.scheduler.TaskContext;
import fi.dy.masa.servux.util.data.tag.CompoundData;
import fi.dy.masa.servux.util.data.tag.ListData;

public class InfoHudSync
{
	private final List<AbstractInfoHudSync<?>> list;

	public InfoHudSync()
	{
		this.list = new ArrayList<>();
	}

	public void addInfo(AbstractInfoHudSync<?> info)
	{
		this.list.add(info);
	}

	public boolean isEmpty()
	{
		return this.list.isEmpty();
	}

	public int size()
	{
		return this.list.size();
	}

	public void clearInfo()
	{
		this.list.forEach(AbstractInfoHudSync::clearInfo);
		this.list.clear();
	}

	public void onTransmitInfo(TaskContext ctx)
	{
		ctx.server().execute(() -> this.transmitInfoInternal(ctx));
	}

	private void transmitInfoInternal(TaskContext ctx)
	{
		CompoundData data = new CompoundData();
		ListData list = new ListData();

		for (AbstractInfoHudSync<?> info : this.list)
		{
			CompoundData entry = info.fillInfo();

			if (entry != null && !entry.isEmpty())
			{
				list.add(entry);
			}
		}

		data.putBoolean("InfoHudComplete", false);
		data.put("InfoHudSync", list);
		this.clearInfo();
		LitematicsDataProvider.INSTANCE.onTaskStatusSync(ctx.player(), data);
	}

	public void onStop(TaskContext ctx)
	{
		ctx.server().execute(() -> this.onTaskCompleteInternal(ctx));
	}

	private void onTaskCompleteInternal(TaskContext ctx)
	{
		CompoundData data = new CompoundData();
		data.putBoolean("InfoHudComplete", true);
		this.clearInfo();
		LitematicsDataProvider.INSTANCE.onTaskStatusSync(ctx.player(), data);
	}
}
