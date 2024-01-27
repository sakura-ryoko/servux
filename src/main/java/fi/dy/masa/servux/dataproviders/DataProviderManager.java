package fi.dy.masa.servux.dataproviders;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.server.MinecraftServer;
import fi.dy.masa.servux.util.JsonUtils;

public class DataProviderManager
{
    public static final DataProviderManager INSTANCE = new DataProviderManager();
    protected final HashMap<String, IDataProvider> providers = new HashMap<>();
    protected ImmutableList<IDataProvider> providersImmutable = ImmutableList.of();
    protected ArrayList<IDataProvider> providersTicking = new ArrayList<>();

    public ImmutableList<IDataProvider> getAllProviders()
    {
        return this.providersImmutable;
    }

    /**
     * Registers the given data provider, if it's not already registered
     * @param provider
     * @return true if the provider did not exist yet and was successfully registered
     */
    public boolean registerDataProvider(IDataProvider provider)
    {
        String name = provider.getName();

        if (!this.providers.containsKey(name))
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
            /**
             * Handled via PacketProvider, we don't need to stop listening, just discard the packets.
            this.updatePacketHandlerRegistration(provider);
             */


            if (enabled && provider.shouldTick() && !this.providersTicking.contains(provider))
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

    public void tickProviders(MinecraftServer server, int tickCounter)
    {
        if (!this.providersTicking.isEmpty())
        {
            for (IDataProvider provider : this.providersTicking)
            {
                if ((tickCounter % provider.getTickRate()) == 0)
                {
                    provider.tick(server, tickCounter);
                }
            }
        }
    }

    @Deprecated
    protected void registerEnabledPacketHandlers()
    {
        /**
         * Handled under ServerNetworkPlayRegister
        for (IDataProvider provider : this.providersImmutable)
        {
            this.updatePacketHandlerRegistration(provider);
        }
        */
    }

    @Deprecated
    protected void updatePacketHandlerRegistration(IDataProvider provider)
    {
        /**
         * Handled under ServerListener via PacketProvider
        IPluginChannelHandler handler = provider.getPacketHandler();

        if (provider.isEnabled())
        {
            ServerPacketChannelHandler.INSTANCE.registerServerChannelHandler(handler);
        }
        else
        {
            ServerPacketChannelHandler.INSTANCE.unregisterServerChannelHandler(handler);
        }
         */
    }

    public void readFromConfig()
    {
        JsonElement el = JsonUtils.parseJsonFile(this.getConfigFile());
        JsonObject obj = null;

        if (el != null && el.isJsonObject())
        {
            JsonObject root = el.getAsJsonObject();

            if (JsonUtils.hasObject(root, "DataProviderToggles"))
            {
                obj = JsonUtils.getNestedObject(root, "DataProviderToggles", false);
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

        for (IDataProvider provider : this.providersImmutable)
        {
            String name = provider.getName();
            objToggles.add(name, new JsonPrimitive(provider.isEnabled()));
        }

        root.add("DataProviderToggles", objToggles);

        JsonUtils.writeJsonToFile(root, this.getConfigFile());
    }

    protected File getConfigFile()
    {
        return new File("servux.json");
    }
}
