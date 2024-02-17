package fi.dy.masa.servux.litematics;

import com.google.gson.*;
import fi.dy.masa.servux.dataproviders.data.LitematicsDataProvider;
import fi.dy.masa.servux.litematics.placement.LitematicPlacement;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Trying to make this DataProvider directly compatible with Syncmatica clients
 */
public class LitematicManager
{
    public static final String PLACEMENTS_JSON_KEY = "placements";
    private final Map<UUID, LitematicPlacement> litematics = new HashMap<>();
    private final Collection<Consumer<LitematicPlacement>> cons = new ArrayList<>();

    public void addPlacement(final LitematicPlacement pl)
    {
        this.litematics.putIfAbsent(pl.getUuid(), pl);
        updatePlacement(pl);
    }
    public LitematicPlacement getPlacement(final UUID uuid) { return this.litematics.get(uuid); }
    public Collection<LitematicPlacement> getAll() { return this.litematics.values(); }
    public void removePlacement(final LitematicPlacement pl)
    {
        this.litematics.remove(pl.getUuid());
        updatePlacement(pl);
    }
    private void updatePlacement(LitematicPlacement pl)
    {
        for (final Consumer<LitematicPlacement> con : this.cons)
        {
            con.accept(pl);
        }
        save();
    }
    public void addPlacementConsumer(final Consumer<LitematicPlacement> con) { this.cons.add(con); }
    public void removePlacementConsumer(final Consumer<LitematicPlacement> con) { this.cons.remove(con); }
    private void save()
    {
        final JsonObject obj = new JsonObject();
        final JsonArray arr = new JsonArray();

        for (final LitematicPlacement p : getAll())
        {
            arr.add(p.toJson());
        }

        obj.add(PLACEMENTS_JSON_KEY, arr);
        final File backup =   new File(LitematicsDataProvider.INSTANCE.getConfigFolder(), "placements.json.bak");
        final File incoming = new File(LitematicsDataProvider.INSTANCE.getConfigFolder(), "placements.json.new");
        final File current =  new File(LitematicsDataProvider.INSTANCE.getConfigFolder(), "placements.json");

        try (final FileWriter writer = new FileWriter(incoming))
        {
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(obj));
        }
        catch (final IOException ignored) {}
    }
    private void load()
    {
        final File file = new File(LitematicsDataProvider.INSTANCE.getConfigFolder(), "placements.json");

        if (file.exists() && file.isFile() && file.canRead())
        {
            JsonElement ele = null;
            try
            {
                final FileReader fr = new FileReader(file);

                ele = JsonParser.parseReader(fr);
                fr.close();
            }
            catch (final Exception ignored) {}
            if (ele == null)
                return;
            try
            {
                final JsonObject obj = ele.getAsJsonObject();
                if (obj == null || !obj.has(PLACEMENTS_JSON_KEY))
                    return;

                final JsonArray arr = obj.getAsJsonArray(PLACEMENTS_JSON_KEY);
                for (final JsonElement elem : arr)
                {
                    final LitematicPlacement placement = LitematicPlacement.fromJson(elem.getAsJsonObject());
                    assert placement != null;
                    litematics.put(placement.getUuid(), placement);
                }

            }
            catch (final IllegalStateException | NullPointerException ignored) {}
        }
    }
    public void init()
    {
        load();
    }
    public void shut() { }
}
