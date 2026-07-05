Servux New Features (0.3.7+)
============================

## New Data Provider changes since Servux 0.1.0:
* `DataProviderToggles` in `servux.json` now can actually enable/disable data providers.  `servux.json` file is now usable (in 0.2.0 and lower; this was not possible).  So yes; you can technically disable the `servux_main` data provider; and then lose the ability to use those features, or fix it without replacing the config file.
* `servux_main` - Core Servux-based service that manages the `servux.json` file and the `/servux` command management.
    * Provides backend setting for `permission_level_easy_place` --> Adds the `Easy Place V3` server-side backed for Litematica and Tweakeroo.
      * It now has the `easy_place_validator_enabled` configuration. 
    * Provides backend setting for `default_language` & `debug_log`.
* `hud_data` - Provides MiniHUD with server side data for any misc, and Info Line / HUD data.  It can be activated by the `Generic` -> `hudDataSync` toggle.  It can provide:
  * Spawn Chunk Radius / Spawn Position.  Shared upon metadata handshake, or future changes to the spawn metadata; such as when someone changes the world spawn location.
  * Weather Info `share_weather_status` and related `update_interval` for tick rate limiting; and has a separate permission node.
  * World Seed `share_seed`.  Only shared upon Metadata handshake, and has a separate permission node.
  * (1.21.2+) ServerRecipeBook data dump (For Use with FurnaceXP Info Line); only sent upon request, and normally at server login after the metadata handshake.
  * TPS / Mob Cap loggers.  Needs to be enabled, but then they work similar to receiving the same data from Carpet.  This is meant to be a fallback when Carpet is not available.
* `entity_data` - Provides MiniHUD with entity/tile entity NBT information for various systems such as `inventoryPreview`, various Renderers, and various Info Lines.  It can be activated by `Generic` -> `entityDataSync`.
  * Provides backend setting for `nbtQueryOverride` where you can offer an alternative OP permission level for Vanilla `NbtQuery` packets.
  * The `nbt_allow_player_inventory` and the related `nbt_allow_player_ender_items` with their related permissions levels can control the personal inventory access for this data.
  * The `fix_allay_gathering` setting enables an override for testing Allay based item sorting while the game rule `mob_griefing` is enabled.
* `litematic_data` - Provides Litematica with entity/tile entity NBT information for use with `InfoOverlay`; and also provides Litematic saving and pasting services.  It can be activated by `Generic` -> `entityDataSync`.
  * Provides backend setting for `fix_rail_rotations`, `fix_stairs_mirror`, && `fix_chest_mirror`.
  * Litematic Paste operations has a separate permissions node.
* `tweaks_data` - Provides Tweakeroo with entity/tile entity NBT information for `inventoryPreview`.  Can be expanded in the future to support more advanced Tweaks.  It can be activated by enabling `entityDataSync`.
  * Can provide the server side method for `stackable_shulkers` with the related `stackable_shulkers_count`, simillar to how Carpet can provide this.
  * This implementation also provides a lightweight `stackable_shulkers_fix` config for hoppers coded for Carpet by [KikuGie] under their [stackable-shulkers-fix] mod.
* _**NOTE**_:  All Data Providers also has their related `permission` configs for controlling OP level style permissions.  All Data Providers and permissions are also compatible with Luck Permissions API.
  * Example Luck Permissions API node: `servux.provider.entity_data.nbt_query_override`.

## `/servux` Command reference:
  * `reload` -- Reloads the config file, discarding the existing config in memory.
  * `save` -- Forces a save of the config file, discarding the existing file; and overwriting it with the configuration in memory.
  * `set` [setting] [value] -- Sets a configuration [value] for the [setting].
  * `info` [setting] -- Displays the current configuration for the [setting].
  * `list` [dataprovider] -- Lists all settings and their respective values.  Can be limited to a specific [dataprovider].
  * `search` [pattern] --- Lists all settings matching the search [pattern].
  * All config settings can be clicked upon to auto-complete a `set` command; after using `info`, `list` or `search`; similar to how the `/carpet` command works.
  * Available settings are modularized per their respective [dataprovider].
  * All `/servux` command text can be translated using the available i18n language files.  Currently only English `en_us` and Chinese (Traditional) `zh_cn` is available, but more may become available as people offer translation assistance.  If you wish to contribute translations; please visit https://translate.sakuraryoko.com -- and if you need a language file added; please contact me.

## Default Config File:
* File is loaded / saved upon Server start, or Vanilla data pack `/reload` command.
```json
{
  "DataProviderToggles": {
    "hud_data": true,
    "litematic_data": true,
    "structure_bounding_boxes": true,
    "servux_main": true,
    "tweaks_data": true,
    "entity_data": true
  },
  "hud_data": {
    "permission_level": 0,
    "update_interval": 40,
    "share_weather_status": false,
    "weather_permission_level": 0,
    "share_seed": false,
    "seed_permission_level": 2,
    "loggers_enabled": false,
    "loggers_enable_list": [
      "tps",
      "mob_caps"
    ],
    "logger_permission_level": 0
  },
  "litematic_data": {
    "permission_level": 0,
    "permission_level_paste": 0,
    "fix_rail_rotations": true,
    "fix_stairs_mirror": true,
    "fix_chest_mirror": true
  },
  "structure_bounding_boxes": {
    "permission_level": 0,
    "structures_blacklist_enabled": false,
    "structures_whitelist_enabled": false,
    "structures_blacklist": [
      "minecraft:buried_treasure"
    ],
    "structures_whitelist": [],
    "update_interval": 40,
    "timeout": 600
  },
  "servux_main": {
    "permission_level": 0,
    "permission_level_admin": 3,
    "permission_level_easy_place": 0,
    "easy_place_validator_enabled": true,
    "default_language": "en_us",
    "debug_log": false
  },
  "tweaks_data": {
    "permission_level": 0,
    "update_interval": 120,
    "stackable_shulkers": false,
    "stackable_shulkers_count": 64,
    "stackable_shulkers_fix": true
  },
  "entity_data": {
    "permission_level": 0,
    "nbt_query_override": false,
    "nbt_query_permission_level": 2,
    "fix_allay_gathering": true,
    "nbt_allow_player_inventory": true,
    "nbt_allow_player_ender_items": true,
    "player_inventory_permission_level": 2,
    "player_ender_items_permission_level": 2
  }
}
```

## Future plans:
* Add Syncmatica-like protocol for Litematica.
