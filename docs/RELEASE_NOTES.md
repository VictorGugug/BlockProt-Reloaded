
![ENTITY PROTECTION](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/RELEASE%20TITLES/ENTITY%20PROTECTION.png)

### Entity protection (renamed from Pet Protection)

The `pet_protection` config section and all related internal identifiers have been renamed to `entity_protection`. The old `pet_protection` keys remain readable as a fallback so existing server configs continue to work without any manual edit.

`config.yml` now reads:
```yaml
entity_protection:
  enabled: false
  auto_protect_on_tame: true
  menu_item: STICK
  villager_locate_seconds: 6
```

The in-game menu title changed from `§dPet Settings` to `§dEntity Settings`. All translation keys (`inventories.pet.*`) are preserved unchanged so custom language files do not need updating.

### Vehicle protection fixes

`VehicleProtectionListener` previously only blocked right-click access (`PlayerInteractEntityEvent`). Hopper pipelines could still extract items from protected chest minecarts and chest boats via `InventoryMoveItemEvent`, which never fires a player interact event. A new `onHopperPullFromVehicle` handler intercepts `InventoryMoveItemEvent` when the source inventory holder is a protected storage vehicle and cancels the extraction.

The fix covers all three protected vehicle types: `StorageMinecart` (chest minecart), `HopperMinecart`, and chest boats (resolved via runtime reflection to support both the 1.20.x and 1.21+ package locations).

A `BLOCKED hopper pipeline extraction` log entry is written to the `entity-protection` log channel whenever an extraction is blocked, making the behavior fully observable in the session log.

### Item frames in /bp lockables

`ITEM_FRAME` and `GLOW_ITEM_FRAME` are now full members of the `ENTITIES` family under the `ITEM_FRAMES` sub-family in `BlockFamilyParser`. They appear in `/bp lockables` like all other entity types with `Status: INACTIVE` when the `lockable_entities` list does not include them (the default), or `Status: ACTIVE` when they are listed.

The sub-family token `*-ITEM_FRAMES` is supported in all expression contexts:
```yaml
lockable_entities:
  - '[*-ITEM_FRAMES]'                                      # only item frames
  - '[*]'                                                  # all entity types including frames
  - '[*-CHEST_BOATS *-CHEST_MINECARTS *-ITEM_FRAMES]'     # boats, minecarts, and frames
```

Item frames are **disabled by default** (`lockable_entities` ships as an empty list). This matches the behavior described in `LOCKABLE_BLOCKS_REFERENCE.md` and is forward-compatible: servers that had no `lockable_entities` key in their `blocks.yml` will have it patched in automatically as an empty list on next startup via `patchBlocksFileIfNeeded()`, with no existing data modified.

How to protect a frame once enabled: sneak + right-click the frame with an empty hand. The BlockProt protection menu opens. Once protected, non-owners cannot rotate, replace the displayed item, or break the frame. Players with `blockprot.user.admin` always bypass frame protection.

### Villager workstation protection

`VillagerWorkstationProtectionListener` is a new listener that extends protection from a locked workstation block to the villager linked to it as a job site.

When a villager's memory contains a job-site location that matches a protected workstation block, the following are blocked for non-owners:
- Damage from players and their projectiles (`EntityDamageByEntityEvent`)
- Right-click interaction — trading GUI (`PlayerInteractEntityEvent`)
- Breaking blocks within a 2×1×2 area around the workstation (`BlockBreakEvent`)
- Interacting with blocks in that same area (`PlayerInteractEvent`) unless the clicked block is the workstation itself

The protection area is: ±2 blocks horizontally (X and Z), ±1 block vertically (Y) relative to the workstation. The exact size is intentionally minimal — it covers the immediate vicinity without affecting adjacent builds.

The linked villager is found at runtime by scanning nearby entities and comparing their `JOB_SITE` memory key to the workstation location via reflection (compatible with 1.14+ without requiring NMS). If no villager is linked to the workstation, no extra protection is applied.

All checks log to the `entity-protection` channel so denials are visible in the session log.

This feature is controlled by `entity_protection.enabled` in `config.yml` (same flag as tamed animal protection). When disabled, no villager protection is active.

### Locate Villager button in workstation lock menu

When a player opens the BlockProt lock menu for a protected workstation block, an Emerald button (`§aLocate Linked Villager`) appears in the slot sequence if the block material is in the `WORKSTATION` sub-family.

Clicking the button:
1. Closes the protection menu.
2. Starts a `VillagerLocateTask` that runs every 10 ticks.
3. The task emits magenta `DUST_COLOR_TRANSITION` particles on the villager's location, visible only to the player who clicked.
4. The task runs for `villager_locate_seconds` (default 6, max 10, configured in `config.yml` under `entity_protection.villager_locate_seconds`).
5. If no villager is linked, nothing happens (the button is still shown but the task finds no target).

The button slot is dynamic — it occupies the next available slot after redstone, friends, name, and transfer buttons, so it does not break the existing layout for non-workstation blocks.

### ViaBackwards and ViaRewind detection

`ViaVersionIntegration` previously detected only ViaVersion itself. It now also probes for ViaBackwards and ViaRewind on enable, logging each companion that is found:
```
[integration] ViaVersion v5.x.x detected — multi-version client support active.
[integration] ViaBackwards detected — v5.x.x (extends ViaVersion protocol translation)
[integration] ViaRewind detected — v4.x.x (extends ViaVersion protocol translation)
```

`getDetailedStatus()` returns a combined string listing ViaVersion and any active companions, used by the debug command and the integrations menu.

Protocol version lookup always goes through the ViaVersion API regardless of which companion plugins are present — they share the same API surface.

### Debug command expanded

`/bp debug run` now tests the following new areas in addition to the existing checks:

| Check | What it verifies |
|---|---|
| Lockable entities | Lists all ENTITIES family members with ACTIVE / INACTIVE status |
| Item frame protection | Reads `isLockableEntity(ITEM_FRAME)` and `isLockableEntity(GLOW_ITEM_FRAME)` and logs the result |
| Raid detection config | Reads `raid_detection.enabled` and `shouldProtectLockedBlocksFromExplosions()` and confirms AuditLogger is active |
| Integrations (expanded) | Calls `ViaVersionIntegration.getDetailedStatus()` for a full Via summary including companions; logs version for each integration |
| NBT entity write/read | Spawns a temporary `ArmorStand`, writes owner UUID via `EntityNBTHandler`, reads it back, then removes the entity |

The summary counter now covers all 18 check groups. The session log includes a separator line before each group for readability.

### Audit log — correct action classification

`InteractEventListener` already logged `ACCESS_DENIED` on blocked access. A review confirmed that `OPENED` is never written for denied accesses — the action code is only written by `InventoryEventListener` for actual inventory openings. No code change was needed; the behavior was already correct. This entry documents the audit trail behavior for clarity:

- `ACCESS_DENIED` — player attempted to access a protected block and was blocked
- `OPENED` — player with access opened the inventory of a protected block
- `RAID_EXPLOSION` — an explosion affected a protected block (written by `ExplodeEventListener`)
- `ITEM_TAKEN` / `ITEM_PLACED` — player moved items in a protected inventory

### blocks.yml comment corrected

The `lockable_entities` comment in `blocks.yml` previously stated that item frame protection was "always active via ItemFrameListener" regardless of the list. This was wrong. Item frames require an explicit entry in `lockable_entities` (or a family expression that includes them) just like all other entity types. The comment has been corrected and expanded with full opt-in examples and sub-family documentation.

## 1.3.3 additions diff summary

| Area | Before | After |
|---|---|---|
| Pet → entity protection naming | `pet_protection` config key | `entity_protection` with `pet_protection` fallback |
| Vehicle hopper extraction | Not blocked | Blocked via `InventoryMoveItemEvent` |
| Item frames in `/bp lockables` | Not shown | ITEM_FRAMES sub-family, INACTIVE by default |
| Item frames in family expressions | Not supported | `[*-ITEM_FRAMES]` token active |
| Villager workstation protection | Not present | `VillagerWorkstationProtectionListener`, 2×1×2 area |
| Locate Villager button | Not present | Emerald slot in workstation lock menu |
| `VillagerLocateTask` | Not present | Particle beacon on linked villager, configurable duration |
| ViaBackwards / ViaRewind detection | Not detected | Probed and logged on enable |
| `getDetailedStatus()` on Via integration | Not present | Returns Via + companions string |
| Debug — entity NBT check | Not present | Spawns ArmorStand, tests EntityNBTHandler read/write |
| Debug — lockable entities check | Not present | Lists active/inactive entity types |
| Debug — item frame check | Not present | Reads frame protection config |
| Debug — raid detection check | Not present | Reads config + AuditLogger presence |
| Debug — integrations (expanded) | Plugin version only | Full Via status via `getDetailedStatus()` |
| `blocks.yml` entity comment | Incorrect ("always active") | Corrected with full opt-in examples |

---

![1.3.3](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/RELEASE%20TITLES/1.3.3.png)

### Support policy change

Based on bStats telemetry, the active install base distributes as follows:
- **26.1.2** — ~80%+ of all installs (primary target)
- **1.21.1x** — second largest segment (actively supported)
- Server software: Paper (~80%+) and Purpur (remainder)

Starting with 1.3.3, the support tiers are:

| Version range | Tier | What to expect |
|---|---|---|
| 26.1.x (Paper/Purpur) | **Primary** | Full feature support, all bug fixes, optimization priority |
| 1.21.1 – 1.21.x | **Active** | Full feature support, all bug fixes |
| 1.20.x – 1.21.0 | **Legacy** | 1.3.3 is the last big update; critical bug fixes only in future releases, no new features |

The compile target has been raised from `paper-api:1.20.6` to `paper-api:1.21.1`. Servers below 1.21.1 will no longer load the plugin (enforced by `api-version: '1.21.1'` in `plugin.yml`). Legacy 1.20.x users must stay on 1.3.3 or earlier.

**Purpur compatibility:** Purpur exposes the full Paper API and adds its own extensions. BlockProt Reloaded compiles against the Paper API only and has no Purpur-specific code, so it loads and runs correctly on Purpur across all supported versions (verified against purpur-api 26.1.2 javadoc). No known incompatibilities.

### Version-specific known issues investigated and resolved

**1.20.x (legacy):**
- `org.bukkit.entity.boat.ChestBoat` does not exist in this API range — the class was in `org.bukkit.entity.ChestBoat`. Fixed in `VehicleProtectionListener` via runtime class resolution (`resolveChestBoatClass()`), which tries the 1.21+ package first and falls back to the 1.20.x location. No `instanceof` against either class at compile time.
- `InventoryView` was a concrete class (not an interface) below 1.21.4. `VersionCompat.hasTypedInventoryViews()` guards any cast.

**1.21.0 (legacy boundary):**
- `org.bukkit.entity.boat` package introduced. `ChestBoat` moved there from `org.bukkit.entity`. Reflection fallback covers both locations.

**1.21.1 – 1.21.3 (active):**
- `InventoryView` still a concrete class; `hasTypedInventoryViews()` returns false correctly.
- `HopperMinecart` and `StorageMinecart` in `org.bukkit.entity.minecart` — present and stable since 1.20.
- NBT-API 2.15.7 adds explicit 1.21.x support.

**1.21.4 (active):**
- `InventoryView` became an interface (Paper hard-forked from Spigot at this version). `hasTypedInventoryViews()` returns true; all inventory-event handlers cast correctly.
- `InventoryAction.HOTBAR_MOVE_AND_READD` deprecated — compiler warning only, runtime behavior unchanged.

**1.21.11 (active):**
- NBT-API 2.15.4 added explicit 1.21.11 support; 2.15.7 (bundled) carries this fix.
- No additional API breaks versus 1.21.4.

**26.1 / 26.1.1 / 26.1.2 (primary):**
- New year-based version format (`Bukkit.getMinecraftVersion()` returns `"26.1.2"` instead of `"1.x.x"`). `VersionCompat` already handles this: `NEW_SCHEME = (MAJOR >= 26 && MAJOR <= 99)`.
- `plugin.yml` `api-version` must be in `major.minor` or `major.minor.patch` format. Single-segment values like `'26'` cause `IllegalArgumentException` at load. We declare `'1.21.1'` which is accepted by both 1.21.1+ and all 26.x builds as a valid minimum.
- `Plugin.getDescription()` deprecated in favor of `Plugin.getPluginMeta()` — affects `ViaVersionIntegration`. Existing call is suppressed; migration deferred.
- NBT-API 2.15.7 adds explicit 26.1.x support with the new versioning format.
- `ViaVersionIntegration` already suppresses the spurious config warning introduced by the ViaVersion API on this version range.
- Purpur 26.1.2 ships `purpur-api:26.1.2.build.2591`; full Paper API superset, no conflicts.

### Dependency updates

| Dependency | Old | New | Reason |
|---|---|---|---|
| `paper-api` (compile) | `1.20.6-R0.1-SNAPSHOT` | `1.21.1-R0.1-SNAPSHOT` | Support policy baseline raise |
| `FoliaLib` | `0.4.3` | `0.5.1` | Bug fixes, optional perf optimizations, latest stable |
| `item-nbt-api` | `2.15.6` | `2.15.7` | Adds explicit 26.1.x and 1.21.11 support |
| `MockBukkit` (test) | `MockBukkit-v1.20:3.93.2` | `MockBukkit-v1.21:4.8.0` | Aligns test environment with new compile baseline |

### StatHandler rewrite

`StatHandler.saveFile()` was completely rewritten. The old implementation called `Files.move(ATOMIC_MOVE)` unconditionally, which throws `IOException: Failed to swap backup NBT file` on Windows and Linux filesystems that do not support atomic cross-directory moves. The new implementation writes to a temp file, validates it with NBT-API before touching the live file, copies the validated temp to the backup, then replaces the live file. When atomic move is not available it falls back to a standard copy-and-delete. A `synchronized` block prevents concurrent writes. The dirty flag is preserved correctly so idle servers never write unnecessarily.

`purgeStalePbsEntries` now checks `World.isChunkLoaded()` before calling `Block.getType()`. The previous code triggered a synchronous chunk load via `ServerChunkCache.syncLoad()` for every stored block location whose chunk was not already resident in memory, freezing the server thread for up to 20 seconds on players with large block lists. The fix skips any entry whose chunk is not already loaded and revisits it on the next call.

A Caffeine cache (`purgeThrottle`, 60-second TTL, 512 entries) was added to throttle the per-player stale-entry scan to at most once per 60 seconds, preventing the scan from running on every block-place event for active players.

`addBlockByUuid()` was added as an offline-player-compatible variant of `addBlock()` that accepts a UUID directly instead of requiring an online `Player` instance.

### Block family expression system

`BlockFamilyParser` is a new class that parses compact family expressions in `blocks.yml` and `worlds.yml`. Expressions are always parsed regardless of the `modern_family_blocks` flag in `config.yml` — that flag only controls whether flat material lists are auto-converted to expressions on startup.

Token reference:

| Token | Meaning |
|---|---|
| `[*]` | All members of the family |
| `[*-TAG]` | All members of the named sub-family |
| `[-*TAG]` | Nothing from the named sub-family (empty base, effectively disables it) |
| `[* -*TAG]` | All family members except the named sub-family |
| `[* -NAME]` | All family members except one specific material |
| `[NAME1 NAME2]` | Only the listed materials |

Sub-families per config key:

| Key | Sub-families |
|---|---|
| `lockable_tile_entities` | `CHEST`, `FURNACE`, `SHELF`, `TRANSPORT`, `MISC`, `SIGN` |
| `lockable_shulker_boxes` | `SHULKERS` |
| `lockable_blocks` | `ANVIL`, `CAULDRON`, `WORKSTATION`, `TRAPDOOR`, `FENCE_GATE` |
| `lockable_doors` | `DOORS` |
| `lockable_entities` | `CHEST_BOATS`, `CHEST_MINECARTS`, `HOPPER_MINECARTS`, `ITEM_FRAMES` |

`BlockFamilyParser.parseFamilyExpressionSilent()` is an identical parser that skips cross-family tokens without emitting warnings. It is used when iterating all families to resolve multi-family expressions such as those in `auto_drop_to_inventory.blocks`, preventing the `WARN: Sub-family 'SHULKERS' belongs to 'shulker_boxes', not 'tile_entities'` spam that was emitted on every reload.

### `lockable_entities` fully integrated

`lockable_entities` is now loaded into a proper `ArrayList<Material>` via `loadBlockListFromConfig` at startup and reload, the same way the other block lists are handled. `isLockableEntity()` reads from that list. `isInactive()` and `getInactiveEntities()` cover entity types. Family expressions are fully supported for this key.

### `/bp lockables` command

New admin command, always accessible regardless of `use_menus`. Opens a six-row paged GUI listing every block the family system knows about with active or inactive status indicators. Left-clicking an entry copies the material name to chat as a clickable link; right-clicking copies it as an exclusion token. The info book shows server version, client version via ViaVersion, and active vs inactive counts. Requires `blockprot.user.admin`.

### Config and blocks auto-merge on every reload

`reloadConfigAndTranslations()` now runs all merge and sync operations on every `/bp reload` call, not only on startup. This means `cleanLegacyConfigKeys()`, `mergeMissingConfigKeys()`, `mergeMissingBlocksKeys()`, lang file merging, and world config re-scan all run on reload. Previously only lang merging ran.

`mergeMissingBlocksKeys()` handles two levels of merging: entire sections missing from `blocks.yml` are added from the JAR wholesale, and in legacy flat-list mode individual missing block entries are appended to existing lists. This ensures new blocks added in a plugin update appear automatically. In modern expression mode the merge is skipped because expressions resolve dynamically.

### Renamed config keys with automatic migration

`worlds_config_enabled` has been renamed to `per_worlds_config`. On first startup after upgrading, the plugin reads the old key, writes its value under the new name, and removes the old entry from `config.yml`. No manual action is required.

### `auto_reload_configs` flag

New config key with default `true`. When set to `false`, the `ConfigFileWatcher` is not started and config files are not reloaded automatically. Use `/bp reload` to apply changes manually. Useful on high-load servers or when editor tools cause spurious filesystem events.

### SQLite usercache moved into plugin folder

`blockprot_usercache.sqlite` is now created inside `plugins/BlockProtReloaded/` rather than next to `server.jar`. On first startup after upgrading, any file found at the old location is moved automatically.

### Legacy folder migration improved

`migrateFromLegacyFolders()` now also calls `mergeYamlUserValues()` after copying files. Admin values from `config.yml` and all lang files in the legacy folder are merged key-by-key into the new location. The destination wins on any conflict so no configured value is ever overwritten.

### ViaVersion registered in `onLoad`

`ViaVersionIntegration` is now registered in `onLoad()` alongside Towny and PlaceholderAPI, making the client version available to any startup code that needs it.

### CraftBukkit error path

The CraftBukkit rejection now loads translations minimally before emitting the unsupported-server error, so the message is properly translated rather than showing a raw key string.

### Bug fixes

`EntityChangeBlockEvent` AIR crash fixed. A missing early-return guard caused `new BlockNBTHandler(AIR)` to throw `RuntimeException: Given block AIR is not a lockable block` when a falling block entity landed on an air block. Guard added at the top of `onEntityChangeBlock`.

`NbtApiException` on shulker box place fixed. The auto-lock task in `BlockEventListener.onBlockPlace` wrote NBT via `block.getState()` on a freshly placed shulker box whose `BlockEntity` had not yet been initialized. Changed to `block.getState(true)` with a `TileState` instanceof guard so the write is skipped when the block entity is not ready.

`/bp protdell` typo fixed. The world-protection-delete command was registered as `protdell` (double L) in 1.3.2. Fixed to `protdel`.


### Raid detection

`RaidDetectionListener` is a new listener that monitors every explosion (`BlockExplodeEvent` and `EntityExplodeEvent`) for proximity to lockable blocks. It does not cancel the explosion; it only performs detection and notification.

Behavior:
- Any lockable block in the explosion's block list triggers a `WARN` line in the session log with the block type, world, coordinates, and the actor (player name, entity type, or "environment").
- If the block is protected and its owner is online: an action-bar alert fires immediately, followed by a chat message with full coordinates. If the owner has `blockprot.blocks.tp`, a clickable `[Teleport]` link is appended.
- If the owner is offline: the alert is queued in memory and delivered as a chat message the next time they join. The TP link is included if they have `blockprot.blocks.tp` at join time.

Controlled by `raid_detection.enabled` in `config.yml` (default: `true`). Disable with `false` to stop all detection and notifications without removing the listener.

New translation keys: `messages.raid_alert`, `messages.raid_coords`, `messages.raid_tp_label`, `messages.raid_pending`.

### Update checker fix

When the plugin version is ahead of the latest GitHub release (e.g. a dev build), the console previously received no log output. Fixed: a silent `update-checker` log entry is now written explaining the ahead-of-release state. No warning or console message is shown in this case.

## 1.3.2 → 1.3.3 diff summary

| Area | 1.3.2 | 1.3.3 |
|---|---|---|
| `StatHandler.saveFile()` | Atomic move only, crashes on Windows | Temp-write, validate, backup, replace with fallback |
| Chunk-load guard | Not present, caused server freeze | `isChunkLoaded()` check before `getType()` |
| Purge throttle | Runs on every block-place event | Caffeine cache, at most once per 60s per player |
| `addBlockByUuid()` | Not present | Added for offline-player use |
| `BlockFamilyParser` | Not present | New class with full expression system |
| `parseFamilyExpressionSilent()` | Not present | Added to suppress cross-family WARN spam |
| `lockable_entities` | Not loaded into ArrayList | Fully integrated with list, reload, and inactive tracking |
| `/bp lockables` | Not present | New admin command, always accessible |
| `LockablesInventory` | Not present | New six-row paged GUI |
| Config merge on reload | Lang files only | All merges run on every reload |
| `worlds_config_enabled` | Active key | Renamed to `per_worlds_config`, auto-migrated |
| `auto_reload_configs` | Not present | New key, default true |
| SQLite location | Server root next to `server.jar` | Plugin data folder, auto-migrated |
| Legacy folder migration | File copy only | File copy plus `mergeYamlUserValues` |
| ViaVersion registration | `onEnable` | `onLoad` |
| CraftBukkit error message | Raw key string | Properly translated |
| AIR block crash | Present | Fixed with early-return guard |
| Shulker box NBT crash | Present | Fixed with `TileState` guard |
| `/bp protdel` | Registered as `protdell` | Corrected |
| Raid detection | Not present | `RaidDetectionListener` — log + owner alerts + offline queue |
| Update checker ahead-of-release | No log output | Silent log entry written |


## 1.3.2

Initial public release.
