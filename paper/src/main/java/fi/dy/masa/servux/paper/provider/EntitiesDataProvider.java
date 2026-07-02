package fi.dy.masa.servux.paper.provider;

import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import fi.dy.masa.servux.paper.ServuxPaperConfig;
import fi.dy.masa.servux.paper.ServuxPaperReference;
import fi.dy.masa.servux.paper.network.ServuxEntitiesPacket;
import fi.dy.masa.servux.paper.util.NbtViewHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Minimal Paper-side mirror of Servux's {@code EntitiesDataProvider} (Fabric side): serves
 * block-entity and entity NBT dumps over the custom {@code servux:entity_data} channel (used by
 * MiniHUD's {@code inventoryPreview}, various renderers, and info lines).
 * <p>
 * Simplifications vs. Fabric: no vanilla {@code NbtQuery} permission override (still presumed
 * to need PacketEvents packet interception - not implemented). Player inventory/enderchest
 * contents are stripped from entity responses by default (matching Fabric's "deny unless granted"
 * default) unless explicitly re-enabled via config + permission.
 *
 * @see <a href="../../../../../../../../../../src/main/java/fi/dy/masa/servux/dataproviders/EntitiesDataProvider.java">EntitiesDataProvider.java (Fabric reference)</a>
 */
public class EntitiesDataProvider
{
    public static final EntitiesDataProvider INSTANCE = new EntitiesDataProvider();

    public static final String CHANNEL_ID = "servux:entity_data";
    public static final int PROTOCOL_VERSION = 1;

    private Plugin plugin;

    private EntitiesDataProvider()
    {
    }

    public void init(Plugin plugin)
    {
        this.plugin = plugin;
    }

    public void sendMetadata(Player player)
    {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("name", "entity_data");
        nbt.putString("id", CHANNEL_ID);
        nbt.putInt("version", PROTOCOL_VERSION);
        nbt.putString("servux", ServuxPaperReference.modString());

        this.send(player, ServuxEntitiesPacket.MetadataResponse(nbt));
    }

    public void onBlockEntityRequest(Player player, BlockPos pos)
    {
        ServerLevel level = level(player);
        BlockEntity be = level.getBlockEntity(pos);
        CompoundTag nbt = be != null ? be.saveWithFullMetadata(level.registryAccess()) : new CompoundTag();

        this.send(player, ServuxEntitiesPacket.SimpleBlockResponse(pos, nbt));
    }

    public void onEntityRequest(Player player, int entityId)
    {
        ServerLevel level = level(player);
        Entity entity = level.getEntity(entityId);

        if (entity == null)
        {
            return;
        }

        Identifier id = EntityType.getKey(entity.getType());
        CompoundTag nbt = NbtViewHelper.saveWithoutId(entity, level.registryAccess());

        if (nbt == null || id == null)
        {
            return;
        }

        if (entity.getType() == EntityType.PLAYER)
        {
            if (!this.hasPlayerInventoryPermission(player))
            {
                nbt.remove("Inventory");
                nbt.put("Inventory", new ListTag());
            }

            if (!this.hasPlayerEnderItemsPermission(player))
            {
                nbt.remove("EnderItems");
                nbt.put("EnderItems", new ListTag());
            }
        }

        nbt.putString("id", id.toString());

        this.send(player, ServuxEntitiesPacket.SimpleEntityResponse(entityId, nbt));
    }

    /** Mirrors Fabric's {@code hasPlayerInventoryPermission} - config toggle + permission node. */
    private boolean hasPlayerInventoryPermission(Player player)
    {
        return ServuxPaperConfig.entityDataAllowPlayerInventory()
               && player.hasPermission("servux.entity_data.nbt_allow_player_inventory");
    }

    /** Mirrors Fabric's {@code hasPlayerEnderItemsPermission} - config toggle + permission node. */
    private boolean hasPlayerEnderItemsPermission(Player player)
    {
        return ServuxPaperConfig.entityDataAllowPlayerEnderItems()
               && player.hasPermission("servux.entity_data.nbt_allow_player_ender_items");
    }

    private void send(Player player, ServuxEntitiesPacket packet)
    {
        player.sendPluginMessage(this.plugin, CHANNEL_ID, packet.toBytes());
    }

    private static ServerLevel level(Player player)
    {
        return ((CraftWorld) player.getWorld()).getHandle();
    }
}
