# Servux Architecture Analysis

_Persisted from planning-session memory for continuity across future sessions. See also [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) in this folder._

## Project Info
- **Mod**: Servux (Server-side companion for MiniHUD client mod)
- **Type**: Fabric mod
- **Minecraft Version**: 26.1.2 (Java 25)
- **Mod Version**: 0.10.3
- **Main Package**: `fi.dy.masa.servux`

## Architecture Overview

### Top-Level Packages
1. **commands/** - Command registration and handling
2. **dataproviders/** - Server data gathering/exposure (HUD, structures, entities, litematics, tweaks)
3. **event/** - Event handlers (ServerHandler, PlayerHandler, ServerInitHandler)
4. **interfaces/** - Interface definitions for handlers and managers
5. **loggers/** - Data logging functionality
6. **mixin/** - Bytecode injection hooks into Minecraft
7. **network/** - Custom packet payload system
8. **scheduler/** - Task scheduling
9. **schematic/** - Schematic handling (litematics format)
10. **settings/** - Configuration/settings system
11. **util/** - Utility classes

## Network Protocol (Core)

### Packet Channels (5 total)
All use Fabric Networking API v1 with custom payload types:

1. **HUD Metadata** (`servux:hud_metadata`)
   - Channel ID: `servux:hud_metadata`
   - Protocol Version: 2
   - Handler: `ServuxHudHandler<ServuxHudPacket.Payload>`
   - Data: Spawn position, weather, recipe manager, seed, data loggers
   - Payload Type: `CustomPacketPayload.Type<ServuxHudPacket.Payload>`

2. **Structures** (`servux:structures`)
   - Channel ID: `servux:structures`
   - Protocol Version: 2
   - Handler: `ServuxStructuresHandler<ServuxStructuresPacket.Payload>`
   - Data: Structure bounding boxes (Witch Huts, Ocean Monuments, etc.)
   - Uses timeout-based chunk tracking

3. **Entities** (`servux:entity_data`)
   - Channel ID: `servux:entity_data`
   - Protocol Version: 1
   - Handler: `ServuxEntitiesHandler<ServuxEntitiesPacket.Payload>`
   - Data: Entity NBT data, block entity data, player inventory

4. **Litematics** (`servux:litematics`)
   - Channel ID: `servux:litematics`
   - Protocol Version: 2
   - Handler: `ServuxLitematicaHandler<ServuxLitematicaPacket.Payload>`
   - Data: Schematic paste operations and validation

5. **Tweaks** (`servux:tweaks`)
   - Channel ID: `servux:tweaks`
   - Protocol Version: 2
   - Handler: `ServuxTweaksHandler<ServuxTweaksPacket.Payload>`
   - Data: Stackable shulker box settings and fixes

### Network API Architecture

- **IPluginServerPlayHandler<T>**: Interface extending `ServerPlayNetworking.PlayPayloadHandler<T>`
  - Registers payloads via `PayloadTypeRegistry.serverboundPlay()` and `PayloadTypeRegistry.clientboundPlay()`
  - Registers receivers via `ServerPlayNetworking.registerGlobalReceiver()`
  - Handles both C2S and S2C packet directions

- **ServerPlayHandler<T>**: Singleton that manages multiple handlers
  - Uses `ArrayListMultimap<Identifier, IPluginServerPlayHandler<T>>`
  - Coordinates registration/unregistration of all packet handlers

- **IServerPayloadData**: Common interface for packet encoding/decoding
  - `getVersion()`: Protocol version
  - `getPacketType()`: Packet type enum value
  - `getTotalSize()`: Byte allocation
  - `isEmpty()`: Check if packet has data
  - `fromPacket(FriendlyByteBuf)`: Decode from buffer
  - `toPacket(FriendlyByteBuf)`: Encode to buffer
  - `clear()`: Reset/cleanup

- **PacketSplitter**: Handles large NBT payloads that exceed MTU
  - Splits large data across multiple packets

## Data Providers

All extend `DataProviderBase` and implement `IDataProvider`:

1. **HudDataProvider**
   - Sends metadata about server (Servux version, MC version)
   - Spawn position (dimension + coordinates)
   - World seed (permission-gated)
   - Weather state tracking
   - Recipe manager data
   - Data logger framework
   - Tick-based updates (configurable interval)

2. **StructureDataProvider**
   - Exposes structure bounding boxes from world
   - Uses `ChunkPos` and `StructureStart` from Minecraft's structure system
   - Supports blacklist/whitelist filtering
   - Chunk loading detection via mixin
   - Timeout-based caching (600-1200 ticks)

3. **EntitiesDataProvider**
   - Entity NBT data queries
   - Block entity (tile entity) data
   - Player inventory data (ender items, main inventory)
   - Permission-based access control
   - Entity fixing (Allay gathering fix)

4. **LitematicsDataProvider**
   - Schematic paste operations
   - `SchematicPlacement` management
   - `SchematicBufferManager` for transmitting schematics
   - Block rotation/mirror fixes for rail and chest blocks
   - Layer-based pasting

5. **TweaksDataProvider**
   - Stackable shulker boxes (size 1-99)
   - Configuration management
   - Item stack data manipulation

6. **ServuxConfigProvider**
   - Global mod configuration
   - Debug mode setting

## Mixins (28 total)

### Block Mixins (5)
- **MixinBlock_UpdateSuppression**: Suppresses block update packets (optimization)
- **MixinChestBlock**: Fixes mirror placement of double chests (Litematics feature)
- **MixinHopperBlockEntity**: Block entity behavior
- **MixinRailBlocks**: Fixes 180° rotation of straight rails (RailBlock, DetectorRailBlock, PoweredRailBlock)
- **MixinStairsBlock**: Stair block handling

### Entity Mixins (3)
- **MixinAllayEntity**: Forces Allay to gather items (override gamerule check)
- **MixinItemEntity**: Entity behavior
- **MixinMobEntity**: Multi-method Allay gathering fix (aiStep method wrapping)

### Item Mixins (2)
- **IMixinItemInstance**: Marker interface for item access
- **MixinBlockItem_EasyPlace**: Easy placement feature
- **MixinItemStack**: Item stack access (empty stub)

### NBT Mixins (2)
- **IMixinNbtReadView**: Interface for NBT read access
- **IMixinNbtWriteView**: Interface for NBT write access

### Network Mixins (2)
- **MixinServerPlayNetworkHandler_EasyPlace**: Removes hit position check for placement
  - Hooks: `ServerGamePacketListenerImpl.handleUseItemOn()`
  - Modifies: `Vec3.subtract()` invocation → returns ZERO

- **MixinServerPlayNetworkHandler_QueryNbt**: Overrides NBT query permissions
  - Hooks: `ServerGamePacketListenerImpl.handleBlockEntityTagQuery()` and `handleEntityTagQuery()`
  - Replaces: `PermissionSet.hasPermission()` with Servux's own permission system

### Debug Mixin (1)
- **MixinSharedConstants**: DEBUG_ENABLED control

### Server Mixins (6)
- **IMixinServerTickManager**: Interface for tick management
- **MixinCommandManager**: Registers `/servux` commands
  - Hooks: `Commands.<init>()` after whitelist registration
  - Injects commands into brigadier dispatcher

- **MixinMain**: Main class initialization
- **MixinMinecraftDedicatedServer**: Server startup
- **MixinMinecraftServer**: Core server event hooks
  - `tickServer()`: Data provider tick updates
  - `prepareLevels()`: Spawn position capture
  - `runServer()`: Server starting/started events
  - `reloadResources()`: Resource reload pre/post events
  - `stopServer()`: Server stopping/stopped events

- **MixinPlayerManager**: Player event hooks
  - `canPlayerLogin()`: Client connect event
  - `placeNewPlayer()`: Player join event
  - `respawn()`: Player respawn event
  - `op()`/`deop()`: Operator status change events

### World Mixins (5)
- **IMixinWorldTickScheduler**: Interface for world tick scheduling
- **MixinServerChunkLoadingManager**: Chunk load detection
  - Hooks: `ChunkMap.markChunkPendingToSend()` → calls `StructureDataProvider.onStartedWatchingChunk()`

- **MixinServerWorld**: Server world updates
  - `setRespawnData()`: Spawn position tracking
  - `advanceWeatherCycle()`: Weather state tracking

- **MixinWorld_UpdateSuppression**: Suppresses world updates (optimization)
- **MixinWorldChunk_UpdateSuppression**: Suppresses chunk updates (optimization)

## Fabric API Dependencies

From `build.gradle`:
- **fabric-api-base** (${fabric_api_version} = 0.152.1+26.1.2)
  - Base Fabric API module

- **fabric-networking-api-v1** (${fabric_api_version} = 0.152.1+26.1.2)
  - `PayloadTypeRegistry`: Register custom packet payloads (C2S and S2C)
  - `ServerPlayNetworking`: Register global receivers for packets
  - `ServerPlayNetworking.Context`: Context passed to packet handlers

- **Lucko Permissions API** (0.7.0)
  - Permission checking for data access

## Access Widener

Located in `src/main/resources/servux.accesswidener`:
```
mutable field net/minecraft/SharedConstants DEBUG_ENABLED Z
accessible field net/minecraft/world/level/NaturalSpawner MAGIC_NUMBER I
```
Provides access to otherwise private/final fields.

## Event System

### Server Lifecycle Events (via MixinMinecraftServer)
1. `onServerStarting()` - Before server initialization
2. `onServerStarted()` - After server initialization complete
3. `onServerResourceReloadPre()` - Before resource reload
4. `onServerResourceReloadPost()` - After resource reload
5. `onServerStopping()` - Before server stop
6. `onServerStopped()` - After server stop

### Player Events (via MixinPlayerManager)
1. `onClientConnect()` - Client attempts connection
2. `onPlayerJoin()` - Player joins world
3. `onPlayerRespawn()` - Player respawns
4. `onPlayerOp()` - Player becomes operator
5. `onPlayerDeOp()` - Player loses operator status

### World Events (via MixinServerWorld)
1. `onSetSpawnPos()` - Spawn position changes
2. `onTickWeather()` - Weather ticks (every game tick)

### Chunk Events (via MixinServerChunkLoadingManager)
1. `onStartedWatchingChunk()` - Player begins seeing chunk
   - Used by StructureDataProvider to track structure visibility

## PaperMC Port Feasibility (MiniHUD protocol channels only)

**Scope**: Only `hud_data`, `structures`, and `entity_data` channels are relevant to a MiniHUD-focused port — these are the channels MiniHUD actually consumes per FEATURES.md. `litematics` and `tweaks` channels serve Litematica/Tweakeroo respectively and are out of scope.

**Verdict**: Feasible, no Fabric mixins required for this scope. Servux's MiniHUD-facing features are additive (new custom payload channels + data reads), not vanilla-behavior patches. MiniHUD client needs **zero changes**: custom payload/plugin-message channels are a transport-agnostic vanilla protocol mechanism — the same underlying packet is used by Fabric's `ServerPlayNetworking`, Bukkit's `Messenger`, and PacketEvents' `WrapperPlayServerPluginMessage`. Compatibility depends entirely on replicating Servux's exact `CompoundTag` NBT layout and `PacketSplitter` chunking scheme on the Paper side, not on which transport library sends it.

**Toolchain decisions**:
- NMS access: `paperweight-userdev` — Paper's official, Mojang-license-compliant Gradle toolchain, providing a Mojang-mapped dev environment with build-time remapping (analogous to Fabric Loom, but for direct NMS calls only — no bytecode-injection/Mixin equivalent).
- Packet interception: **PacketEvents** (Spigot build variant, e.g. `PacketEvents-Spigot-2.13.0`) covers the 2 vanilla-packet interception needs (chunk-watch trigger via `ClientboundLevelChunkWithLightPacket` for structures; NBT-query permission override via `ServerboundEntityTagQuery`/`ServerboundBlockEntityTagQuery` for entity_data) and optionally the 3 custom channels themselves via `WrapperPlayServerPluginMessage`/`WrapperPlayClientPluginMessage`. ProtocolLib is not used, to avoid running two Netty-injecting libraries simultaneously.
- Target environment: standard Paper (not Folia) — no regionized-threading complications for tick-based data providers or main-thread NMS access.
- Paper is current with MC 26.1.2 (Paper Build 72) — no version-lag gap versus Fabric for this target.

**Per-channel feasibility**:
- `hud_data` — High. Spawn position, seed, weather, TPS all available via public Bukkit/Paper API. Weather's exact tick-countdown integers (not just booleans) are confirmed available via `World#getWeatherDuration()`/`getThunderDuration()`/`getClearWeatherDuration()` — no NMS dip needed (see `paper/IMPLEMENTATION_PLAN.md`, Phase 6). Recipe manager dump needs NMS `RecipeManager`; round-trip NBT fidelity vs. FurnaceXP info-line format unverified.
- `structures` — Medium-High, confirmed and implemented (see `paper/IMPLEMENTATION_PLAN.md`, Phase 3). Structure bounding boxes need NMS access via `paperweight-userdev` (no clean Bukkit API exists for full per-chunk structure piece boxes — `org.bukkit.generator.structure.Structure` only supports nearest-structure search). The chunk-watch trigger (replacing `MixinServerChunkLoadingManager`) turned out **not** to need PacketEvents interception as originally assumed — Paper's public `io.papermc.paper.event.packet.PlayerChunkLoadEvent` covers it directly.
- `entity_data` — Medium-High, confirmed and implemented (see `paper/IMPLEMENTATION_PLAN.md`, Phase 4). Entity/block-entity NBT dumps via NMS `Entity#saveWithoutId`/`BlockEntity#saveWithFullMetadata`; player inventory via `CraftItemStack.asNMSCopy`. The vanilla `NbtQuery` packet permission override (replacing `MixinServerPlayNetworkHandler_QueryNbt`) still presumed to need PacketEvents interception of `ServerboundEntityTagQuery`/`ServerboundBlockEntityTagQuery` — not implemented; sidestepped by routing all MiniHUD entity-data needs through Servux's own custom channel instead of vanilla's NbtQuery packets.

**Known technical wrinkle — NBT "View" abstraction**: `IMixinNbtReadView.java` (a Mixin `@Accessor` into `TagValueInput`/`ValueInputContextHelper`) shows that recent MC versions wrap NBT access behind `ValueInput`/`ValueOutput`-style Views rather than exposing raw `CompoundTag` at save/load call sites. Paper has no Mixin `@Accessor` equivalent, so unwrapping the raw tag requires plain Java reflection on the private field instead — functionally equivalent, but unverified until prototyped against 26.1.2. Affects `entity_data` NBT dumps and possibly structure piece serialization.

**Non-protocol port aspects** (not required for MiniHUD wire compatibility, but relevant to a full plugin):
- Configuration: Servux uses its own JSON config (`servux.json`); a Paper plugin would use YAML config + Bukkit permission nodes (LuckPerms integrates natively with Bukkit/Paper, arguably simpler than on Fabric).
- Command registration: the `/servux` command → straightforward via Paper's Brigadier-based command registration (`LifecycleEventManager`) or plugin.yml + `CommandExecutor`.
- Event registration: server/player lifecycle events (`ServerHandler`, `PlayerHandler`) → standard Bukkit event listeners (`PlayerJoinEvent`, etc.) instead of mixins; some fine-grained events (e.g. resource-reload pre/post) may need Paper-specific events or polling.

**Excluded from scope**: gameplay-behavior mixins tied to litematics/tweaks (rail rotation, chest mirror, stairs, Allay gathering fix) — these modify vanilla behavior rather than transmit protocol data, and would be low feasibility on Paper anyway (no mixin equivalent for direct AI/behavior overrides).

**Remaining unverified items before implementation**: protocol version handshake semantics (`PROTOCOL_VERSION` per channel + `MAX_FAILURES=4` auto-unregister-after-failures logic) and exact MiniHUD client behavior on a version mismatch; weather exact timer fields; recipe manager NBT round-trip shape; the NBT View-unwrapping reflection approach described above. None of these are expected blockers — they should be resolved by prototyping.

## Multi-platform port architecture (recommended pattern for a PR-friendly port)
**Current repo structure confirmed**: `settings.gradle` has no `include` statements at all (single-project build) — `build.gradle` applies `net.fabricmc.fabric-loom` + `maven-publish` directly to the root project. The root project IS the Fabric mod build today.

**Recommended pattern**: add a new Gradle subproject `paper/` alongside the existing (untouched) root Fabric build — NOT a symmetric `common/fabric/paper` monorepo restructure (that would require moving all existing Fabric source files, which is disruptive and a much harder PR to review). Same general pattern used by real multi-platform projects like LuckPerms and ViaVersion (common+bukkit+fabric+... subprojects in one repo), simplified here to "existing root project + one new subproject" since only Paper is being added, no shared common module for now.

**Concrete diff footprint for a PR**: `settings.gradle` gains exactly one line (`include 'paper'`); everything else is a brand-new `paper/` folder (own `build.gradle` using `io.papermc.paperweight.userdev`, own `paper-plugin.yml`, own `src/main/java/fi/dy/masa/servux/paper/...` tree). Zero modifications to any existing Fabric source file, `build.gradle`, `gradle.properties`, or `jitpack.yml`. Caveat: Gradle's default `build` task aggregates subprojects, so a bare `./gradlew build` will also build `paper/` after this change — worth flagging to the maintainer rather than solving unilaterally. No shared `common` module for the MVP (cross-subproject source-sharing between Fabric Loom and paperweight-userdev adds complexity not needed to validate feasibility).

See [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) for the full phased MVP implementation plan (hud_data-channel-only proof of concept) built on this architecture.
