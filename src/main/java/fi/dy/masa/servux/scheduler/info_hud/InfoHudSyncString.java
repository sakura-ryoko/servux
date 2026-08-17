package fi.dy.masa.servux.scheduler.info_hud;

import java.util.ArrayList;
import java.util.List;

import fi.dy.masa.servux.util.data.tag.CompoundData;
import fi.dy.masa.servux.util.data.tag.ListData;
import fi.dy.masa.servux.util.data.tag.StringData;

/**
 * @deprecated Probably was not the best way to do this
 */
@Deprecated
public class InfoHudSyncString extends AbstractInfoHudSync<String>
{
	private final List<String> infoHudFeedback;

	public InfoHudSyncString()
	{
		super(InfoHudSyncType.STRING);
		this.infoHudFeedback = new ArrayList<>();
	}

	@Override
	public void addInfo(String info)
	{
		this.infoHudFeedback.add(info);
	}

	@Override
	public boolean hasInfo()
	{
		return !this.infoHudFeedback.isEmpty();
	}

	@Override
	public void clearInfo()
	{
		this.infoHudFeedback.clear();
	}

	@Override
	public CompoundData fillInfo()
	{
		CompoundData data = new CompoundData();
		ListData list = new ListData();

		for (String info : this.infoHudFeedback)
		{
			list.add(new StringData(info));
		}

		data.putString("Type", this.type().name());
		data.put("Data", list);

		return data;
	}
}
