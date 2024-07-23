package fi.dy.masa.servux.dataproviders;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.util.JsonUtils;
import net.minecraft.util.Identifier;

import java.util.List;

public abstract class DataProviderBase implements IDataProvider
{
    protected final Identifier networkChannel;
    protected final String name;
    protected final String permNode;
    protected final String description;
    protected final int protocolVersion;
    protected final int defaultPerm;
    protected boolean enabled;
    protected boolean playRegistered;
    private int tickRate = 40;

    protected DataProviderBase(String name, Identifier channel, int protocolVersion, int defaultPerm, String permNode, String description)
    {
        this.name = name;
        this.networkChannel = channel;
        this.protocolVersion = protocolVersion;
        this.defaultPerm = defaultPerm > -1 && defaultPerm < 5 ? defaultPerm : 0;
        this.permNode = permNode;
        this.description = description;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    @Override
    public Identifier getNetworkChannel()
    {
        return this.networkChannel;
    }

    @Override
    public int getProtocolVersion()
    {
        return this.protocolVersion;
    }

    @Override
    public boolean isEnabled()
    {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    @Override
    public boolean isRegistered()
    {
        return this.playRegistered;
    }

    @Override
    public void setRegistered(boolean toggle)
    {
        this.playRegistered = toggle;
    }

    protected void setTickRate(int tickRate)
    {
        this.tickRate = Math.max(tickRate, 1);
    }

    @Override
    public final int getTickInterval()
    {
        return this.tickRate;
    }

    @Override
    public List<IServuxSetting<?>> getSettings()
    {
        return List.of();
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject object = new JsonObject();
        for (IServuxSetting<?> setting : getSettings())
        {
            object.add(setting.name(), setting.writeToJson());
        }
        return object;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        for (IServuxSetting<?> setting : getSettings())
        {
            JsonElement element = obj.get(setting.name());
            if (element != null)
            {
                setting.readFromJson(element);
            }
        }
    }
}
