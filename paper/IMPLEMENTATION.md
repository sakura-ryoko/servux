# Fabric → Paper Equivalency Guide

**How to read this**: each channel/feature has a short table of "Fabric mechanism → Paper mechanism", followed by any caveats. Class names are fully qualified on first use per section. Unless noted otherwise, the on-the-wire NBT schema is byte-for-byte identical between the two implementations — MiniHUD requires no client-side changes.

---

## Shared infrastructure

| Area | Fabric | Paper |
|---|---|---|
| NMS/internals access | Fabric Loom (Mixin, mapped dev environment) | `paperweight-userdev` 2.0.0-beta.21 (Mojang-mapped dev bundle, remapping only — no bytecode injection equivalent) |
| Custom channel transport | `ServerPlayNetworking` (Fabric API) | Bukkit `Messenger` (`Plugin#getServer().getMessenger()`, `registerOutgoingPluginChannel`/`registerIncomingPluginChannel` + `PluginMessageListener`) |
| Packet envelope | `FriendlyByteBuf.writeVarInt(typeId)` + `writeNbt(tag)` | Identical — same Mojang-mapped `FriendlyByteBuf`/`CompoundTag` classes are on the paperweight-userdev classpath, so the codec code is a near-verbatim port |
| Large-payload chunking | `fi.dy.masa.servux.network.PacketSplitter` (varint total length on first fragment, then raw byte fragments, keyed `ReadingSession` reassembly) | `fi.dy.masa.servux.paper.network.PacketSplitter` — identical chunking scheme, generalized to accept a `Function<byte[], byte[]> fragmentEncoder` so it can be shared by both `structures` and `hud_data` instead of being duplicated per-channel |
| Permissions | LuckPerms/Fabric-permissions-api style nodes | Bukkit's native permission system (`plugin.yml` `permissions:` block + `Player#hasPermission(node)`) — works transparently with any Bukkit permission plugin, no direct dependency needed |
| Config | Fabric's config file/`ConfigOptionWrapper` pattern | `JavaPlugin#getConfig()`/`saveDefaultConfig()`/`reloadConfig()` against a bundled `config.yml`, cached into static fields, refreshed on `/servux reload` |
| Commands | Fabric command dispatcher (Brigadier via Fabric API) | Paper's native Brigadier API (`io.papermc.paper.command.brigadier.Commands`), registered via `JavaPlugin#getLifecycleManager()` + `LifecycleEvents.COMMANDS` — same Brigadier library Fabric uses, just Paper's first-party wiring instead of Fabric API's |
| Debug logging | `Servux.debugLog(...)` (config-gated) | `ServuxPaperReference.debugLog(String, Object...)` — same config-gated pattern, centralized instead of duplicated per channel |

---

## `hud_data` channel (`servux:hud_metadata`)

### Metadata handshake & spawn data

Reference: Fabric's `ServuxHudHandler.java`/`ServuxHudPacket.java` ↔ Paper's `HudMetadataChannel.java`/`ServuxHudPacket.java`/`HudDataProvider.java`.

| Field/behavior | Fabric | Paper |
|---|---|---|
| Spawn location | `ServerWorld#getSpawnPos()` (+ `DimensionType` for the dimension key) | `World#getSpawnLocation()` (public Bukkit API); first world in `Bukkit.getWorlds()` treated as the overworld |
| Mod identifier string | `Reference.MOD_STRING` = `servux-fabric-{mc}-{mod}` | `"servux-paper-" + ServerBuildInfo.buildInfo().minecraftVersionId() + "-" + pluginMeta.getVersion()` — same pattern, different platform version source |
| NBT field names | `name`, `id`, `version`, `servux`, `spawnDimension`, `spawnPosX/Y/Z`, conditional `worldSeed`, conditional `Loggers` | Identical field names/types — this is the part that must match byte-for-byte for MiniHUD to parse it |

### World seed (`share_seed` toggle)

`World#getSeed()` is plain public Bukkit API — no Fabric-side mechanism to translate at all; both platforms call an equivalent one-line getter. Gated on `servux.hud_data.share_seed` permission (default `op`, matching Fabric) + `hud_data.share_seed` config toggle (default `false`).

### Weather status (`share_weather_status` toggle)

| Fabric | Paper |
|---|---|
| Direct NMS field reads on `ServerLevel`'s weather-timer state (raw ints, `-1` sentinel = inactive) | `World#getWeatherDuration()` → rain timer, `World#getThunderDuration()` → thunder timer, `World#getClearWeatherDuration()` → clear-weather timer — all public Bukkit API, backed internally by the same `WeatherData` object Fabric reads via NMS, just exposed without needing the NMS dip |
| Sentinel check: timer `> -1` means active | Paper's public getters return `0` (not `-1`) when inactive, so the Paper port checks `> 0` instead. **This is the one documented field-level behavioral difference in this feature** — functionally equivalent for the "is this timer active" boundary, but worth flagging in case MiniHUD ever special-cases the exact sentinel value. |
| Push mechanism | Custom periodic broadcast tied into the mod's own tick hook | `Bukkit.getScheduler().runTaskTimer` on `HudMetadataChannel`, same polling-and-push pattern already used for `structures`, at `hud_data.update_interval` (default 40 ticks) |

### TPS logger

| Fabric | Paper |
|---|---|
| `IMixinServerTickManager` custom Mixin accessor into vanilla's tick-rate manager | **`Bukkit.getServerTickManager()`** (`org.bukkit.ServerTickManager`, fully public) — exposes `isFrozen()`/`isSprinting()`/`isStepping()`/`getTickRate()` directly. This is a full public-API replacement, not a workaround: the Mixin accessor Fabric needs has **no Paper-side equivalent requirement at all**. |
| TPS/MSPT values | Custom NMS-level tick timing collection | `Bukkit.getTPS()` (1m/5m/15m rolling averages) and `Bukkit.getAverageTickTime()` — both fully public static API, zero NMS |
| `sprintTicks` field (`TPSData` Codec) | Read via the same custom Mixin accessor above | **No clean public equivalent exists.** Approximated as `getFrozenTicksToRun()` while frozen, `0` otherwise. This is the one field in the whole port with a known, documented behavioral gap rather than a byte-for-byte translation — flagged in code comments in `TpsLogger.java`. |

### Mob-cap logger

| Fabric | Paper |
|---|---|
| Hand-rolled `MathUtils.clamp(...)` cap-limit calculation against NMS `NaturalSpawner` state | `NaturalSpawner.globalLimitForCategory(level, category, chunks)` — a **Paper-added** static helper (confirmed via Paper's own `NaturalSpawner.java.patch`, "Add mobcaps commands") that does the exact same cap-limit math for free |
| Reference implementation used to find the above | (none needed — original author) | Paper's own first-party `/paper mobcaps` command (`io.papermc.paper.command.subcommands.MobcapsCommand`) was read as a template; the Paper port mirrors its `CraftWorld#getHandle()` → `NaturalSpawner.SpawnState` → `getSpawnableChunkCount()`/`getMobCategoryCounts()` pattern exactly |
| Category enum mapping | `EntityCategory` enum, declaration order: `MONSTER, CREATURE, AMBIENT, AXOLOTLS, UNDERGROUND_WATER_CREATURE, WATER_CREATURE, WATER_AMBIENT, MISC` | `org.bukkit.entity.SpawnCategory` public enum + `CraftSpawnCategory`/`World#getSpawnLimit(SpawnCategory)`. **The wire format's `cap_data` list is positional** (no per-entry category label), so the Paper port hardcodes iteration in the exact same order as Fabric's `EntityCategory` declaration to preserve byte-for-byte compatibility — this ordering dependency is easy to silently break if either side's enum order ever changes. |

### Recipe manager dump

| Fabric | Paper |
|---|---|
| Recipe source | `RecipeManager` (accessed via the server's recipe manager singleton) | `ServerLevel#recipeAccess().getRecipes()` — same underlying vanilla data, reached via the CraftBukkit bridge (`CraftWorld#getHandle()`) instead of a directly-injected reference |
| Serialization | `Recipe.CODEC.encodeStart(NbtOps.INSTANCE, recipe)` | Identical call — same Mojang Codec, no translation needed at all |
| Large payload transport | `PacketSplitter` | Same generalized `PacketSplitter` described in "Shared infrastructure" above |

### Data-logger subscription plumbing (shared by TPS + mob-caps)

Fabric ties per-player logger subscriptions to its own tick hook and permission/config checks inline in `HudDataProvider`. Paper's `HudDataProvider.updateLoggerSubscription(player, requestNbt)` mirrors this: validates each requested logger name against `hud_data.loggers_enable_list` (config) + `servux.hud_data.loggers` (permission), stores a per-player `Set<String>` subscription, and computes each active logger's NBT **once per tick** (not once per subscriber) before fanning out — same efficiency characteristic as Fabric's implementation.

---

## `structures` channel (`servux:structures`)

Reference: Fabric's `ServuxStructuresHandler.java`/`StructureDataProvider.java` ↔ Paper's `StructuresChannel.java`/`StructureDataProvider.java`.

| Area | Fabric | Paper |
|---|---|---|
| Chunk-watch trigger (fires when a player should receive structure data for a newly-watched chunk) | `MixinServerChunkLoadingManager#markChunkPendingToSend` (custom Mixin injection into vanilla's chunk-sending logic) | **`io.papermc.paper.event.packet.PlayerChunkLoadEvent`** — a public Bukkit/Paper API event fired exactly when a player receives a chunk packet. This is a drop-in public-API replacement; no Netty/packet interception library needed at all, contradicting the original feasibility assumption that packet interception would be required here. |
| Structure NMS extraction | `ChunkStatus.STRUCTURE_REFERENCES` + vanilla `StructureStart` lookups directly against `ServerLevel` | Identical vanilla calls (`getStructureReferencesFromChunk`, `getStructureStartsFromReferences`), reached via `CraftWorld#getHandle()` → `ServerLevel` bridge. Uses the real vanilla `StructureStart#createTag(StructurePieceSerializationContext, ChunkPos)` for serialization — guarantees byte-for-byte NBT parity with Fabric rather than reconstructing the format by hand from Bukkit's simplified `Chunk#getStructures()` API. |
| Dimension/world-change detection | `PlayerDimensionPosition` — `DimensionType` + block-distance threshold check | Simplified to a plain `World` equality check — sufficient since Bukkit's per-player chunk tracking doesn't need Fabric's extra distance-threshold logic |
| Periodic refresh + timeout | Custom tick hook, `Timeout` util class | `Bukkit.getScheduler().runTaskTimer` (every 40 ticks) + a direct port of `fi.dy.masa.servux.util.Timeout` (tick-delta expiry tracker) — same class, ported near-verbatim since it has no NMS dependency to begin with |

---

## `entity_data` channel (`servux:entity_data`)

Reference: Fabric's `ServuxEntitiesHandler.java`/`EntitiesDataProvider.java` ↔ Paper's `EntitiesChannel.java`/`EntitiesDataProvider.java`.

| Area | Fabric | Paper |
|---|---|---|
| Block entity NBT | `BlockEntity#saveWithFullMetadata(RegistryAccess)` — returns a plain `CompoundTag` directly | Identical call, reached via `CraftWorld#getHandle()` → `ServerLevel#getBlockEntity(BlockPos)`. No translation needed — this method never went through the `ValueOutput`/View-abstraction change. |
| Entity NBT | `Entity#saveWithoutId(ValueOutput)` | Identical call, but **does** need the View-abstraction workaround below since it now takes the new `ValueOutput` interface, not a raw `CompoundTag`. |
| **NBT View-abstraction wrinkle** | `NbtView`/`IMixinNbtWriteView` — a Mixin `@Accessor` reaching into `TagValueOutput`'s private `output` field (a `CompoundTag`) | **No Mixin system exists in Paper**, so the same result is achieved with plain reflection instead, since paperweight-userdev provides the identical `net.minecraft.world.level.storage.TagValueOutput` class on the classpath: `Field OUTPUT_FIELD = TagValueOutput.class.getDeclaredField("output"); OUTPUT_FIELD.setAccessible(true);` then read it back after calling `entity.saveWithoutId(writer)`. Isolated in `fi.dy.masa.servux.paper.util.NbtViewHelper` so this one fragile piece is easy to find/replace later. This is the **only** place in the whole port needing raw field reflection instead of a clean public method call. |
| `ProblemReporter` (needed to construct a `TagValueOutput`) | `new ProblemReporter.ScopedCollector(LOGGER)` | Identical call — plain public NMS utility class, no translation issue |
| `id` field (EntityType registry key) added to response | Same | Same — added in both after the base NBT is built |
| Player inventory/ender-items stripping | Conditionally stripped based on `nbt_allow_player_inventory`/`nbt_allow_player_ender_items` config + permission checks | Same config keys + `servux.hud_data.entity_data.nbt_allow_player_inventory`/`nbt_allow_player_ender_items` permission nodes (default `op`), checked identically |
| **`NbtQuery` vanilla-packet permission override** (`nbtQueryOverride`/`hasNbtQueryPermission`, backed by `MixinServerPlayNetworkHandler_QueryNbt`) | Patches vanilla's own packet handler for `ServerboundEntityTagQuery`/`ServerboundBlockEntityTagQuery` | **Not ported.** This patches a vanilla packet handler rather than a Servux custom channel, and would need Netty/PacketEvents packet interception to reproduce on Paper. Since MiniHUD can get equivalent data through the custom `entity_data` channel instead, this was intentionally left out of scope — this is the only Fabric-side mechanism in the whole protocol surface with no Paper port at all (by choice, not by blocker). |

---

## Summary: where Paper needed something other than a direct public-API call

Every feature above has a byte-for-byte-compatible wire format. The translation effort was almost entirely mechanical (same Mojang-mapped class names via paperweight-userdev, same Codecs, same field names) with exactly **three** exceptions worth the Fabric maintainer's attention:

1. **Entity NBT save** (`entity_data` channel) — needs one reflective field read (`TagValueOutput#output`) to work around the lack of a Mixin `@Accessor` equivalent. Isolated in `NbtViewHelper`.
2. **TPS logger's `sprintTicks` field** — no public Paper API exposes this; approximated rather than translated exactly (documented gap, low risk).
3. **`NbtQuery` vanilla-packet permission override** — intentionally not ported; would require packet interception (PacketEvents) that every other channel managed to avoid needing.