package fi.dy.masa.servux.scheduler.info_hud;

import java.util.ArrayList;
import java.util.List;

import fi.dy.masa.servux.util.data.tag.CompoundData;
import fi.dy.masa.servux.util.data.tag.ListData;

public class InfoHudSyncChunks extends AbstractInfoHudSync<InfoHudSyncChunks.Entry>
{
	private final List<Entry> infoHudFeedback;

	public InfoHudSyncChunks()
	{
		super(InfoHudSyncType.REMAINING_CHUNKS);
		this.infoHudFeedback = new ArrayList<>();
	}

	@Override
	public void addInfo(Entry info)
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

		for (Entry info : this.infoHudFeedback)
		{
			list.add(info.toData());
		}

		data.putString("Type", this.type().name());
		data.put("Data", list);

		return data;
	}

	public record Entry(String n, int rc, int cx, int cz)
	{
		public CompoundData toData()
		{
			CompoundData data = new CompoundData();

			data.putString("n", this.n());
			data.putInt("rc", this.rc());
			data.putInt("cx", this.cx());
			data.putInt("cz", this.cz());

			return data;
		}

		public Entry nextChunk(final int cx, final int cz)
		{
			return new Entry(this.n(), this.rc(), cx, cz);
		}
	}
}
