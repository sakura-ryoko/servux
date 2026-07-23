package fi.dy.masa.servux.schematic.conversion;

import java.util.List;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.schematic.LitematicaSchematic;
import fi.dy.masa.servux.util.data.Constants;
import fi.dy.masa.servux.util.data.tag.CompoundData;
import fi.dy.masa.servux.util.data.tag.ListData;
import fi.dy.masa.servux.util.data.tag.util.DataOps;
import fi.dy.masa.servux.util.data.tag.util.DataTypeUtils;

public class SchematicConversionMaps
{
	public static DataFixer datafixer;

	private static DataFixer getDataFixer()
	{
		if (datafixer == null)
		{
			datafixer = DataFixers.getDataFixer();
		}

		return datafixer;
	}

	public static String updateBlockName(String oldName, int oldVersion)
	{
		StringTag tagStr = StringTag.valueOf(oldName);

		try
		{
			return getDataFixer()
					.update(References.BLOCK_NAME, new Dynamic<>(NbtOps.INSTANCE, tagStr), oldVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION)
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
	public static CompoundData updateBlockStates(CompoundData oldBlockState, int oldVersion)
	{
		// Don't update the name yet if block is pre-flattening
		if (oldVersion >= LitematicaSchematic.MINECRAFT_DATA_VERSION_1_13_2)
		{
			String oldName = oldBlockState.getStringOrDefault("Name", "");
			String blockName = updateBlockName(oldName, oldVersion);

			if (!oldName.equalsIgnoreCase(blockName))
			{
				oldBlockState.putString("Name", blockName);
//			      Servux.LOGGER.error("updateBlockName: [{}] -> [{}]", oldName, blockName);
			}
		}

		try
		{
			return (CompoundData) getDataFixer().update(References.BLOCK_STATE, new Dynamic<>(DataOps.INSTANCE, oldBlockState), oldVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION).getValue();
		}
		catch (Exception e)
		{
			Servux.LOGGER.warn("updateBlockStates: failed to update Block State [{}], preserving original state (data may become lost)",
			                   oldBlockState.getStringOrDefault("Name", "?"));
			return oldBlockState;
		}
	}

	public static CompoundData updateBlockEntity(CompoundData oldBlockEntity, int oldVersion)
	{
		try
		{
			return (CompoundData) getDataFixer().update(References.BLOCK_ENTITY, new Dynamic<>(DataOps.INSTANCE, oldBlockEntity), oldVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION).getValue();
		}
		catch (Exception e)
		{
//            BlockPos pos = NbtUtils.readBlockPos(oldBlockEntity);
			BlockPos pos = DataTypeUtils.readBlockPos(oldBlockEntity);
			Servux.LOGGER.warn("updateBlockEntity: failed to update Block Entity [{}] at [{}], preserving original state (data may become lost)",
			                   oldBlockEntity.getStringOrDefault("id", "?"), pos != null ? pos.toShortString() : "?");
			return oldBlockEntity;
		}
	}

	public static CompoundData updateEntity(CompoundData oldEntity, int oldVersion)
	{
		try
		{
			return (CompoundData) getDataFixer().update(References.ENTITY, new Dynamic<>(DataOps.INSTANCE, oldEntity), oldVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION).getValue();
		}
		catch (Exception e)
		{
			Servux.LOGGER.warn("updateEntity: failed to update Entity [{}], preserving original state (data may become lost)",
			                   oldEntity.getStringOrDefault("id", "?"));
			return oldEntity;
		}
	}

	// Fix missing "id" tags.  This seems to be an issue with 1.19.x litematics.
	public static CompoundData checkForIdTag(CompoundData tags)
	{
		if (tags.contains("id", Constants.NBT.TAG_STRING))
		{
			return tags;
		}

		if (tags.contains("Id", Constants.NBT.TAG_STRING))
		{
			tags.putString("id", tags.getStringOrDefault("Id", ""));
			return tags;
		}

		// We don't have an "id" tag, let's try to fix it
		if (tags.containsLenient("Bees") || tags.containsLenient("bees"))
		{
			tags.putString("id", "minecraft:beehive");
		}
		else if (tags.containsLenient("TransferCooldown") && tags.containsLenient("Items"))
		{
			tags.putString("id", "minecraft:hopper");
		}
		else if (tags.containsLenient("SkullOwner"))
		{
			tags.putString("id", "minecraft:skull");
		}
		else if (tags.containsLenient("Patterns") || tags.containsLenient("patterns"))
		{
			tags.putString("id", "minecraft:banner");
		}
		else if (tags.containsLenient("Sherds") || tags.containsLenient("sherds"))
		{
			tags.putString("id", "minecraft:decorated_pot");
		}
		else if (tags.containsLenient("last_interacted_slot") && tags.containsLenient("Items"))
		{
			tags.putString("id", "minecraft:chiseled_bookshelf");
		}
		else if (tags.containsLenient("CookTime") && tags.containsLenient("Items"))
		{
			tags.putString("id", "minecraft:furnace");
		}
		else if (tags.containsLenient("RecordItem"))
		{
			tags.putString("id", "minecraft:jukebox");
		}
		else if (tags.containsLenient("Book") || tags.containsLenient("book"))
		{
			tags.putString("id", "minecraft:lectern");
		}
		else if (tags.containsLenient("front_text"))
		{
			tags.putString("id", "minecraft:sign");
		}
		else if (tags.containsLenient("BrewTime") || tags.containsLenient("Fuel"))
		{
			tags.putString("id", "minecraft:brewing_stand");
		}
		else if ((tags.containsLenient("LootTable") && tags.containsLenient("LootTableSeed")) || (tags.containsLenient("hit_direction") || tags.containsLenient("item")))
		{
			tags.putString("id", "minecraft:suspicious_sand");
		}
		else if (tags.containsLenient("SpawnData") || tags.containsLenient("SpawnPotentials"))
		{
			tags.putString("id", "minecraft:spawner");
		}
		else if (tags.containsLenient("normal_config"))
		{
			tags.putString("id", "minecraft:trial_spawner");
		}
		else if (tags.containsLenient("shared_data"))
		{
			tags.putString("id", "minecraft:vault");
		}
		else if (tags.containsLenient("pool") && tags.containsLenient("final_state") && tags.containsLenient("placement_priority"))
		{
			tags.putString("id", "minecraft:jigsaw");
		}
		else if (tags.containsLenient("author") && tags.containsLenient("metadata") && tags.containsLenient("showboundingbox"))
		{
			tags.putString("id", "minecraft:structure_block");
		}
		else if (tags.containsLenient("ExactTeleport") && tags.containsLenient("Age"))
		{
			tags.putString("id", "minecraft:end_gateway");
		}
		else if (tags.containsLenient("Items"))
		{
			tags.putString("id", "minecraft:chest");
		}
		else if (tags.containsLenient("last_vibration_frequency") || tags.containsLenient("listener"))
		{
			tags.putString("id", "minecraft:sculk_sensor");
		}
		else if (tags.containsLenient("warning_level") || tags.containsLenient("listener"))
		{
			tags.putString("id", "minecraft:sculk_shrieker");
		}
		else if (tags.containsLenient("OutputSignal"))
		{
			tags.putString("id", "minecraft:comparator");
		}
		else if (tags.containsLenient("facing") || tags.containsLenient("extending"))
		{
			tags.putString("id", "minecraft:piston");
		}
		else if (tags.containsLenient("x") && tags.containsLenient("y") && tags.containsLenient("z"))
		{
			// Might only have x y z pos
			tags.putString("id", "minecraft:piston");
		}

		// Fix any erroneous Items tags with the null "tag" tag.
		if (tags.containsList("Items", Constants.NBT.TAG_COMPOUND))
		{
			ListData items = fixItemsTag(tags.getList("Items"));
			tags.put("Items", items);
		}

		return tags;
	}

	// Fix null 'tag' entries.  This seems to be an issue with 1.19.x litematics.
	private static ListData fixItemsTag(ListData items)
	{
		ListData newList = new ListData();

		for (int i = 0; i < items.size(); i++)
		{
			CompoundData itemEntry = fixItemTypesFrom1_21_2(items.getCompoundAt(i));

			if (itemEntry.contains("tag", Constants.NBT.TAG_COMPOUND))
			{
				CompoundData tag = null;
				try
				{
					tag = itemEntry.getCompound("tag");
				}
				catch (Exception ignored)
				{
				}

				// Remove 'tag' if it is set to null
				if (tag == null)
				{
					itemEntry.remove("tag");
				}
				else
				{
					// Fix nested entries if they exist
					if (tag.contains("BlockEntityTag", Constants.NBT.TAG_COMPOUND))
					{
						CompoundData entityEntry = tag.getCompound("BlockEntityTag");

						if (entityEntry.containsList("Items", Constants.NBT.TAG_COMPOUND))
						{
							ListData nestedItems = fixItemsTag(entityEntry.getList("Items"));
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

	private static CompoundData fixItemTypesFrom1_21_2(CompoundData nbt)
	{
		if (!nbt.contains("id", Constants.NBT.TAG_STRING))
		{
			return nbt;
		}

		String id = nbt.getStringOrDefault("id", "");
		Identifier newId = null;

		switch (id)
		{
			case "minecraft:pale_oak_boat" -> newId = Identifier.withDefaultNamespace("oak_boat");
			case "minecraft:pale_oak_chest_boat" -> newId = Identifier.withDefaultNamespace("oak_chest_boat");
		}

		if (newId != null)
		{
			nbt.putString("id", newId.toString());
		}

		return nbt;
	}

	public static CompoundData fixEntityTypesFrom1_21_2(CompoundData nbt)
	{
		if (!nbt.contains("id", Constants.NBT.TAG_STRING))
		{
			return nbt;
		}

		// Fix any erroneous Items tags with the null "tag" tag.
		if (nbt.containsList("Items", Constants.NBT.TAG_COMPOUND))
		{
			ListData items = fixItemsTag(nbt.getList("Items"));
			nbt.put("Items", items);
		}

		String id = nbt.getStringOrDefault("id", "");
		Identifier newId = null;
		String type = "";
		boolean boatFix = false;

		switch (id)
		{
			case "minecraft:oak_boat", "minecraft:pale_oak_boat" ->
			{
				newId = Identifier.withDefaultNamespace("boat");
				type = "oak";
				boatFix = true;
			}
			case "minecraft:spruce_boat" ->
			{
				newId = Identifier.withDefaultNamespace("boat");
				type = "spruce";
				boatFix = true;
			}
			case "minecraft:birch_boat" ->
			{
				newId = Identifier.withDefaultNamespace("boat");
				type = "birch";
				boatFix = true;
			}
			case "minecraft:jungle_boat" ->
			{
				newId = Identifier.withDefaultNamespace("boat");
				type = "jungle";
				boatFix = true;
			}
			case "minecraft:acacia_boat" ->
			{
				newId = Identifier.withDefaultNamespace("boat");
				type = "acacia";
				boatFix = true;
			}
			case "minecraft:cherry_boat" ->
			{
				newId = Identifier.withDefaultNamespace("boat");
				type = "cherry";
				boatFix = true;
			}
			case "minecraft:dark_oak_boat" ->
			{
				newId = Identifier.withDefaultNamespace("boat");
				type = "dark_oak";
				boatFix = true;
			}
			case "minecraft:mangrove_boat" ->
			{
				newId = Identifier.withDefaultNamespace("boat");
				type = "mangrove";
				boatFix = true;
			}
			case "minecraft:bamboo_raft" ->
			{
				newId = Identifier.withDefaultNamespace("boat");
				type = "bamboo";
				boatFix = true;
			}
			case "minecraft:oak_chest_boat", "minecraft:pale_oak_chest_boat" ->
			{
				newId = Identifier.withDefaultNamespace("chest_boat");
				type = "oak";
				boatFix = true;
			}
			case "minecraft:spruce_chest_boat" ->
			{
				newId = Identifier.withDefaultNamespace("chest_boat");
				type = "spruce";
				boatFix = true;
			}
			case "minecraft:birch_chest_boat" ->
			{
				newId = Identifier.withDefaultNamespace("chest_boat");
				type = "birch";
				boatFix = true;
			}
			case "minecraft:jungle_chest_boat" ->
			{
				newId = Identifier.withDefaultNamespace("chest_boat");
				type = "jungle";
				boatFix = true;
			}
			case "minecraft:acacia_chest_boat" ->
			{
				newId = Identifier.withDefaultNamespace("chest_boat");
				type = "acacia";
				boatFix = true;
			}
			case "minecraft:cherry_chest_boat" ->
			{
				newId = Identifier.withDefaultNamespace("chest_boat");
				type = "cherry";
				boatFix = true;
			}
			case "minecraft:dark_oak_chest_boat" ->
			{
				newId = Identifier.withDefaultNamespace("chest_boat");
				type = "dark_oak";
				boatFix = true;
			}
			case "minecraft:mangrove_chest_boat" ->
			{
				newId = Identifier.withDefaultNamespace("chest_boat");
				type = "mangrove";
				boatFix = true;
			}
			case "minecraft:bamboo_chest_raft" ->
			{
				newId = Identifier.withDefaultNamespace("chest_boat");
				type = "bamboo";
				boatFix = true;
			}
			default ->
			{
				if (id.contains("_chest_boat"))
				{
					newId = Identifier.withDefaultNamespace("chest_boat");
					type = "oak";
					boatFix = true;
				}
				else if (id.contains("_boat"))
				{
					newId = Identifier.withDefaultNamespace("boat");
					type = "oak";
					boatFix = true;
				}
			}
		}

		if (newId != null)
		{
			nbt.putString("id", newId.toString());
		}

		if (boatFix)
		{
			nbt.putString("Type", type);
		}

		return nbt;
	}

	private record ConversionData(int idMeta, String newStateString, String[] oldStateStrings)
	{
	}

	public record ConversionDynamic(int idMeta, Dynamic<?> newState, List<Dynamic<?>> oldStates)
	{
	}
}
