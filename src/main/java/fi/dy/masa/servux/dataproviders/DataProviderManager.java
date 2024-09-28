package fi.dy.masa.servux.dataproviders;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.profiler.Profiler;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.util.JsonUtils;

public class DataProviderManager
{
    public static final DataProviderManager INSTANCE = new DataProviderManager();

    /**
     * lower case name to data provider instances.
     */
    protected final HashMap<String, IDataProvider> providers = new HashMap<>();
    protected ImmutableList<IDataProvider> providersImmutable = ImmutableList.of();
    protected ArrayList<IDataProvider> providersTicking = new ArrayList<>();

    public ImmutableList<IDataProvider> getAllProviders()
    {
        return this.providersImmutable;
    }
    protected File configDir = null;
    protected DynamicRegistryManager.Immutable immutable = DynamicRegistryManager.EMPTY;

    /**
     * Registers the given data provider, if it's not already registered
     * @param provider
     * @return true if the provider did not exist yet and was successfully registered
     */
    public boolean registerDataProvider(IDataProvider provider)
    {
        String name = provider.getName().toLowerCase();

        if (this.providers.containsKey(name) == false)
        {
            this.providers.put(name, provider);
            this.providersImmutable = ImmutableList.copyOf(this.providers.values());
            //System.out.printf("registerDataProvider: %s\n", provider);
            return true;
        }

        return false;
    }

    public boolean setProviderEnabled(String providerName, boolean enabled)
    {
        IDataProvider provider = this.providers.get(providerName);
        return provider != null && this.setProviderEnabled(provider, enabled);
    }

    public boolean setProviderEnabled(IDataProvider provider, boolean enabled)
    {
        boolean wasEnabled = provider.isEnabled();
        enabled = true; // FIXME TODO remove debug

        if (enabled || wasEnabled != enabled)
        {
            //System.out.printf("setProviderEnabled: %s (%s)\n", enabled, provider);
            provider.setEnabled(enabled);
            this.updatePacketHandlerRegistration(provider);

            if (enabled && provider.shouldTick() && this.providersTicking.contains(provider) == false)
            {
                this.providersTicking.add(provider);
            }
            else
            {
                this.providersTicking.remove(provider);
            }

            return true;
        }

        return false;
    }

    public void tickProviders(MinecraftServer server, int tickCounter, Profiler profiler)
    {
        if (this.providersTicking.isEmpty() == false)
        {
            for (IDataProvider provider : this.providersTicking)
            {
                if ((tickCounter % provider.getTickInterval()) == 0)
                {
                    provider.tick(server, tickCounter, profiler);
                }
            }
        }
    }

    protected void registerEnabledPacketHandlers()
    {
        for (IDataProvider provider : this.providersImmutable)
        {
            this.updatePacketHandlerRegistration(provider);
        }
    }

    protected void updatePacketHandlerRegistration(IDataProvider provider)
    {
        if (provider.isEnabled())
        {
            provider.registerHandler();
        }
        else
        {
            provider.unregisterHandler();
        }
    }

    public void onCaptureImmutable(@Nonnull DynamicRegistryManager.Immutable immutable)
    {
        this.immutable = immutable;
    }

    public DynamicRegistryManager.Immutable getRegistryManager()
    {
        return this.immutable;
    }

    public void onServerTickEndPre()
    {
        for (IDataProvider provider : this.providersImmutable)
        {
            provider.onTickEndPre();
        }
    }

    public void onServerTickEndPost()
    {
        for (IDataProvider provider : this.providersImmutable)
        {
            provider.onTickEndPost();
        }
    }

    public Optional<IDataProvider> getProviderByName(String providerName)
    {
        return Optional.ofNullable(this.providers.get(providerName));
    }

    public @Nullable IServuxSetting<?> getSettingByName(String name)
    {
        if (name.contains(":"))
        {
            String[] parts = name.split(":");
            String providerName = parts[0];
            String settingName = parts[1];
            IDataProvider provider = this.providers.get(providerName);

            if (provider != null)
            {
                for (IServuxSetting<?> setting : provider.getSettings())
                {
                    if (setting.name().equalsIgnoreCase(settingName))
                    {
                        return setting;
                    }
                }
            }
        }
        else
        {
            for (IDataProvider provider : this.providersImmutable)
            {
                for (IServuxSetting<?> setting : provider.getSettings())
                {
                    if (setting.name().equalsIgnoreCase(name))
                    {
                        return setting;
                    }
                }
            }
        }
        return null;
    }

    public void readFromConfig()
    {
        JsonElement el = JsonUtils.parseJsonFile(this.getConfigFile());
        JsonObject obj = null;

        Servux.debugLog("DataProviderManager#readFromConfig()");

        if (el != null && el.isJsonObject())
        {
            JsonObject root = el.getAsJsonObject();

            if (JsonUtils.hasObject(root, "DataProviderToggles"))
            {
                obj = JsonUtils.getNestedObject(root, "DataProviderToggles", false);
            }

            for (IDataProvider provider : this.providersImmutable)
            {
                String name = provider.getName();

                if (JsonUtils.hasObject(root, name))
                {
                    provider.fromJson(JsonUtils.getNestedObject(root, name, false));
                }
            }
        }

        for (IDataProvider provider : this.providersImmutable)
        {
            String name = provider.getName();
            boolean enabled = obj != null && JsonUtils.getBooleanOrDefault(obj, name, false);
            this.setProviderEnabled(provider, enabled);
        }
    }

    public void writeToConfig()
    {
        JsonObject root = new JsonObject();
        JsonObject objToggles = new JsonObject();

        Servux.debugLog("DataProviderManager#writeToConfig()");

        for (IDataProvider provider : this.providersImmutable)
        {
            String name = provider.getName();
            objToggles.add(name, new JsonPrimitive(provider.isEnabled()));
        }

        root.add("DataProviderToggles", objToggles);

        for (IDataProvider provider : this.providersImmutable)
        {
            String name = provider.getName();
            root.add(name, provider.toJson());
        }

        JsonUtils.writeJsonToFile(root, this.getConfigFile());
    }

    protected File getConfigFile()
    {
        if (this.configDir == null)
        {
            this.configDir = Reference.DEFAULT_CONFIG_DIR;
        }
        return new File(this.configDir, "servux.json");
    }
}
