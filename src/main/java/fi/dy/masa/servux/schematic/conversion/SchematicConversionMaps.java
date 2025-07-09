package fi.dy.masa.servux.schematic.conversion;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.schematic.LitematicaSchematic;
import fi.dy.masa.servux.util.nbt.NbtUtils;
import net.minecraft.datafixer.Schemas;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.math.BlockPos;

public class SchematicConversionMaps
{
    public static DataFixer datafixer;
    
    private static DataFixer getDataFixer()
    {
        if (datafixer == null)
        {
            datafixer = Schemas.getFixer();
        }
        
        return datafixer;
    }
    
    public static String updateBlockName(String oldName, int oldVersion)
    {
        NbtString tagStr = NbtString.of(oldName);

        try
        {
            return getDataFixer()
                                  .update(TypeReferences.BLOCK_NAME, new Dynamic<>(NbtOps.INSTANCE, tagStr), oldVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION)
                                  .getValue().asString()
                                  .orElse(oldName);
        }
        catch (Exception e)
        {
            Servux.LOGGER.warn("updateBlockName: failed to update Block Name [{}], preserving original state (data may become lost)", oldName);
            return oldName;
        }
    }

    /**
     * These are the Vanilla Data Fixer's for the 1.20.x -> 1.20.5 changes
     */
    public static NbtCompound updateBlockStates(NbtCompound oldBlockState, int oldVersion)
    {
        try
        {
            return (NbtCompound) getDataFixer().update(TypeReferences.BLOCK_STATE, new Dynamic<>(NbtOps.INSTANCE, oldBlockState), oldVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION).getValue();
        }
        catch (Exception e)
        {
            Servux.LOGGER.warn("updateBlockStates: failed to update Block State [{}], preserving original state (data may become lost)",
                                   oldBlockState.contains("Name") ? oldBlockState.getString("Name", "?") : "?");
            return oldBlockState;
        }
    }

    public static NbtCompound updateBlockEntity(NbtCompound oldBlockEntity, int oldVersion)
    {
        try
        {
            return (NbtCompound) getDataFixer().update(TypeReferences.BLOCK_ENTITY, new Dynamic<>(NbtOps.INSTANCE, oldBlockEntity), oldVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION).getValue();
        }
        catch (Exception e)
        {
            BlockPos pos = NbtUtils.readBlockPos(oldBlockEntity);
            Servux.LOGGER.warn("updateBlockEntity: failed to update Block Entity [{}] at [{}], preserving original state (data may become lost)",
                                   oldBlockEntity.contains("id") ? oldBlockEntity.getString("id", "?") : "?", pos != null ? pos.toShortString() : "?");
            return oldBlockEntity;
        }
    }

    public static NbtCompound updateEntity(NbtCompound oldEntity, int oldVersion)
    {
        try
        {
            return (NbtCompound) getDataFixer().update(TypeReferences.ENTITY, new Dynamic<>(NbtOps.INSTANCE, oldEntity), oldVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION).getValue();
        }
        catch (Exception e)
        {
            Servux.LOGGER.warn("updateEntity: failed to update Entity [{}], preserving original state (data may become lost)",
                                   oldEntity.contains("id") ? oldEntity.getString("id", "?") : "?");
            return oldEntity;
        }
    }

    // Fix missing "id" tags.  This seems to be an issue with 1.19.x litematics.
    public static NbtCompound checkForIdTag(NbtCompound tags)
    {
        if (tags.contains("id"))
        {
            return tags;
        }
        if (tags.contains("Id"))
        {
            tags.putString("id", tags.getString("Id", ""));
            return tags;
        }

        // We don't have an "id" tag, let's try to fix it
        if (tags.contains("Bees") || tags.contains("bees"))
        {
            tags.putString("id", "minecraft:beehive");
        }
        else if (tags.contains("TransferCooldown") && tags.contains("Items"))
        {
            tags.putString("id", "minecraft:hopper");
        }
        else if (tags.contains("SkullOwner"))
        {
            tags.putString("id", "minecraft:skull");
        }
        else if (tags.contains("Patterns") || tags.contains("patterns"))
        {
            tags.putString("id", "minecraft:banner");
        }
        else if (tags.contains("Sherds") || tags.contains("sherds"))
        {
            tags.putString("id", "minecraft:decorated_pot");
        }
        else if (tags.contains("last_interacted_slot") && tags.contains("Items"))
        {
            tags.putString("id", "minecraft:chiseled_bookshelf");
        }
        else if (tags.contains("CookTime") && tags.contains("Items"))
        {
            tags.putString("id", "minecraft:furnace");
        }
        else if (tags.contains("RecordItem"))
        {
            tags.putString("id", "minecraft:jukebox");
        }
        else if (tags.contains("Book") || tags.contains("book"))
        {
            tags.putString("id", "minecraft:lectern");
        }
        else if (tags.contains("front_text"))
        {
            tags.putString("id", "minecraft:sign");
        }
        else if (tags.contains("BrewTime") || tags.contains("Fuel"))
        {
            tags.putString("id", "minecraft:brewing_stand");
        }
        else if ((tags.contains("LootTable") && tags.contains("LootTableSeed")) || (tags.contains("hit_direction") || tags.contains("item")))
        {
            tags.putString("id", "minecraft:suspicious_sand");
        }
        else if (tags.contains("SpawnData") || tags.contains("SpawnPotentials"))
        {
            tags.putString("id", "minecraft:spawner");
        }
        else if (tags.contains("normal_config"))
        {
            tags.putString("id", "minecraft:trial_spawner");
        }
        else if (tags.contains("shared_data"))
        {
            tags.putString("id", "minecraft:vault");
        }
        else if (tags.contains("pool") && tags.contains("final_state") && tags.contains("placement_priority"))
        {
            tags.putString("id", "minecraft:jigsaw");
        }
        else if (tags.contains("author") && tags.contains("metadata") && tags.contains("showboundingbox"))
        {
            tags.putString("id", "minecraft:structure_block");
        }
        else if (tags.contains("ExactTeleport") && tags.contains("Age"))
        {
            tags.putString("id", "minecraft:end_gateway");
        }
        else if (tags.contains("Items"))
        {
            tags.putString("id", "minecraft:chest");
        }
        else if (tags.contains("last_vibration_frequency") || tags.contains("listener"))
        {
            tags.putString("id", "minecraft:sculk_sensor");
        }
        else if (tags.contains("warning_level") || tags.contains("listener"))
        {
            tags.putString("id", "minecraft:sculk_shrieker");
        }
        else if (tags.contains("OutputSignal"))
        {
            tags.putString("id", "minecraft:comparator");
        }
        else if (tags.contains("facing") || tags.contains("extending"))
        {
            tags.putString("id", "minecraft:piston");
        }
        else if (tags.contains("x") && tags.contains("y") && tags.contains("z"))
        {
            // Might only have x y z pos
            tags.putString("id", "minecraft:piston");
        }

        // Fix any erroneous Items tags with the null "tag" tag.
        if (tags.contains("Items"))
        {
            NbtList items = fixItemsTag(tags.getListOrEmpty("Items"));
            tags.put("Items", items);
        }

        return tags;
    }

    // Fix null 'tag' entries.  This seems to be an issue with 1.19.x litematics.
    private static NbtList fixItemsTag(NbtList items)
    {
        NbtList newList = new NbtList();

        for (int i = 0; i < items.size(); i++)
        {
            NbtCompound itemEntry = items.getCompoundOrEmpty(i);
            if (itemEntry.contains("tag"))
            {
                NbtCompound tag = null;
                try
                {
                    tag = itemEntry.getCompoundOrEmpty("tag");
                }
                catch (Exception ignored) {}

                // Remove 'tag' if it is set to null
                if (tag == null)
                {
                    itemEntry.remove("tag");
                }
                else
                {
                    // Fix nested entries if they exist
                    if (tag.contains("BlockEntityTag"))
                    {
                        NbtCompound entityEntry = tag.getCompoundOrEmpty("BlockEntityTag");

                        if (entityEntry.contains("Items"))
                        {
                            NbtList nestedItems = fixItemsTag(entityEntry.getListOrEmpty("Items"));
                            entityEntry.put("Items", nestedItems);
                        }

                        tag.put("BlockEntityTag", entityEntry);
                    }

                    itemEntry.put("tag", tag);
                }
            }

            newList.add(itemEntry);
        }

        return newList;
    }
}
