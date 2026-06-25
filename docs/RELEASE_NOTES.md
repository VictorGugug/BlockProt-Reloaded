![1.3.3](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/RELEASE%20TITLES/1.3.3.png)

This page lists what changed in each release, in the order it shipped. Each entry describes the final, working behavior - not the intermediate steps taken to get there.

Hello everyone, I apologize for how long this release took. The main reason is my studies, but I am also running a server alone, developing 2 plugins and 1 mod, and managing 14 personal projects on top of this one. I want to thank everyone for the 100 downloads on Modrinth -- it is an honor to contribute to the Minecraft community and help people. I kindly ask for your patience with each release and its timing. I am just one person, Zar, working on this project alone. I do it for the love of the craft and to share my ideas in something as beautiful as this. I will keep an eye on all your issues and suggestions. I will soon open a Discord server where you can interact with me more directly and where I can assist you as attentively and quickly as I can. Thank you all very much. Below is a detailed changelog of everything added, fixed, changed, and more. This is a MAJOR UPDATE that adds a lot of new content. I strongly recommend reading it in full, as well as the following documents: [BLOCK FAMILY SYNTAX](https://github.com/VictorGugug/BlockProt-Reloaded/blob/main/docs/BLOCK_FAMILY_SYNTAX.md) and [LOCKABLE BLOCKS REFERENCE](https://github.com/VictorGugug/BlockProt-Reloaded/blob/main/docs/LOCKABLE_BLOCKS_REFERENCE.md). These documents will be updated periodically so I recommend keeping an eye on them. Since this is a major update, many more will follow. This update focuses on simplicity and explaining the entire project for better understanding and usability. Future 1.3.x / 1.x.x updates will focus on bug fixes, new features, and more. I hope you understand that I do what I can to offer something complete and that you understand the time it takes. Good night, and I hope that by tomorrow, July 24, 2026, version 1.3.3 will finally be released. Have a lovely night.

---

## 1.3.3

## Additions

### Fixed menu layout for all block and entity types

The Block Lock menu previously placed its buttons using a counter that incremented for each button actually shown. When a button did not apply to the current block (for example, no Redstone button on a workstation), every button after it shifted one slot to the left. This meant the same menu looked different depending on which block type opened it, and the bottom-row buttons (Inspect, Audit Log, Paste, Copy, Info) ended up in inconsistent slots between block types.

Every button now has one fixed slot, used by every block and entity type. A button that does not apply to the current type simply leaves its slot empty; the other buttons never move.

**Fixed slots, top row:**

| Slot | Item | Shown for |
|---|---|---|
| 0 | Block icon | Always |
| 1 | Redstone | Storage and traversal blocks |
| 2 | Player Head | Always, unless friend functionality is disabled |
| 3 | Name Tag | Always |
| 4 | Ender Pearl | Always |
| 5 | Emerald | Workstation blocks only |
| 6 | Hopper / Lime Dye | Storage blocks only, and only when expiry is enabled |

**Fixed slots, bottom row:**

| Slot | Item | Shown for |
|---|---|---|
| 9 | Spyglass | Storage blocks with an inventory, owner or admin |
| 13 | Clock | Owner or admin, audit logger active |
| 14 | Knowledge Book | Manager role, clipboard has data |
| 15 | Paper | Manager role |
| 16 | Compass | Manager role or admin |
| 17 | Barrier | Always |

The entity menu (`fillForEntity`) was fixed the same way: Name Tag stays at slot 0, Redstone and Ender Pearl only appear for storage vehicles (chest boats, storage/hopper minecarts) and always sit at slots 1 and 4, Player Head always sits at slot 2, Spyglass and Clock sit at 9 and 10 for storage vehicles, and Compass sits at 13.

### Entity protection rename

The protection system covering tamed animals (wolves, cats, parrots, horses, llamas) was renamed end-to-end from "pet protection" to "entity protection" across class names, package names, config keys, NBT persistent-data keys, and method names. This is purely a naming and organization change; behavior is identical to the previous pet protection feature.

The `pet_protection` key in `config.yml` is renamed to `entity_protection`. Upgrading servers do not need to edit anything: on first startup after the upgrade, the plugin reads the old key, writes its value under the new name, and removes the old entry automatically. Old NBT data stored under the legacy `pet_owner` / `pet_protected` keys is migrated to the new keys automatically as well.

### Villager workstation protection

A villager whose job-site memory points to a protected workstation block (grindstone, stonecutter, loom, cartography table, smithing table, enchanting table, fletching table, lectern, composter, brewing stand, blast furnace, smoker, barrel, cauldron) inherits that block's protection automatically - no separate configuration is needed per villager.

Protected actions:
- Damage to the villager by non-owners (melee and projectiles)
- Trading interaction by non-owners
- Breaking blocks in a search area around the workstation
- Interacting with blocks in that area (except the workstation block itself)

The search area is configurable independently of whether the feature is enabled:

```yaml
villager_workstation_protection:
  enabled: true
  radius: 2            # horizontal search radius in blocks, clamped to 0-8
  vertical_radius: 1   # vertical search radius in blocks, clamped to 0-4
```

A new Emerald button ("Locate Linked Villager") appears in the lock menu for a protected workstation that has a villager linked to it. Clicking it closes the menu and shows a short particle effect at the villager's location, visible only to the clicking player, for a configurable duration (`entity_protection.villager_locate_seconds`, 1-10 seconds, default 6). If no villager is linked, nothing happens and no error is shown.

### Item frame protection and automatic block linking

`ITEM_FRAME` and `GLOW_ITEM_FRAME` are full members of the `ENTITIES` family under the `ITEM_FRAMES` sub-family. They appear in `/bp lockables` with `Status: INACTIVE` by default (not included in `lockable_entities`) or `ACTIVE` when listed.

A frame mounted on the face of a lockable block is automatically linked to that block at the moment it is placed. A linked frame has no protection state of its own: no owner, no friend list, no lock toggle. Interacting with it opens the underlying block's lock menu directly, and the frame's protection always matches the block's exactly - same owner, same friends, locks and unlocks together. The link is stored bidirectionally (the frame stores the block's coordinates, the block stores the frame's entity UUID) so it survives chunk reloads and server restarts, and is validated on each interaction in case the frame or block changed since linking.

A frame not mounted on a lockable block, or mounted on a block that is not currently lockable, keeps the standalone entity-protection flow: sneak and right-click with an empty hand opens the BlockProt entity menu directly, exactly like 1.3.2.

Supported token: `*-ITEM_FRAMES` to include only frames, or `-*ITEM_FRAMES` to exclude all frames, in a `lockable_entities` family expression.

### Storage vehicle protection completed

Chest boats, storage minecarts, and hopper minecarts use the same sneak-and-right-click flow as item frames. Once protected:
- Right-clicking without sneaking is blocked for non-owners and non-friends.
- Damaging or destroying the vehicle (`VehicleDamageEvent`, `VehicleDestroyEvent`) is blocked for everyone except the owner and admins.
- Hopper-pipeline extraction from the vehicle's inventory is blocked unless the owner has disabled that protection from the in-menu toggle.
- Placing the vehicle auto-locks it to the placing player if their "lock on place" setting is enabled and they are not sneaking, mirroring the behavior already in place for blocks and item frames.

`ChestBoat` is resolved at runtime via reflection so the same code path supports both the 1.20.x class location (`org.bukkit.entity.ChestBoat`) and the 1.21+ location (`org.bukkit.entity.boat.ChestBoat`).

### `EntityInfoInventory` location display

The info panel for a protected entity shows an Oak Sign in slot 1 displaying the entity's type and current world/coordinates, replacing the Compass-based block-info display that does not apply to entities (entities do not have NBT-stored placement coordinates the way blocks do).

### `BlockFamilyParser` cross-family validation and `/bp lockables`

`BlockFamilyParser` parses compact family expressions in `blocks.yml` and `worlds.yml`. Expressions are always parsed regardless of the `modern_family_blocks` flag in `config.yml` - that flag only controls whether flat material lists are auto-converted to expressions on startup.

A `NAME` or `-NAME` token inside an expression is now validated against the family of the config key it appears in. A material that does not belong to that family is rejected with a console warning and discarded, rather than silently producing an incorrect or empty result. For example, `FLETCHING_TABLE` (a `BLOCKS` workstation) inside a `lockable_tile_entities` expression is rejected with a warning, while `COPPER_CHEST` (a valid `CHEST` sub-family member) is accepted.

`/bp lockables` is a new admin command (`blockprot.user.admin`) that opens a six-row paged GUI listing every block and entity the family system knows about, regardless of whether it is currently enabled. Active entries show a green `Status: ACTIVE` label; disabled entries show a red `Status: INACTIVE` label. Left-clicking an entry sends a clickable chat message that copies the material name; right-clicking copies the exclusion token (`-MATERIAL_NAME`). The info book in the GUI shows server version, client version (via ViaVersion, if installed), and active/inactive counts.

Token reference:

| Token | Meaning |
|---|---|
| `*` | All members of the family |
| `*-TAG` | All members of the named sub-family |
| `-*TAG` | Exclude all members of the named sub-family |
| `NAME` | Include one specific material |
| `-NAME` | Exclude one specific material |

Sub-families per config key:

| Key | Sub-families |
|---|---|
| `lockable_tile_entities` | CHEST, FURNACE, SHELF, TRANSPORT, MISC, SIGN |
| `lockable_shulker_boxes` | SHULKERS |
| `lockable_blocks` | ANVIL, CAULDRON, WORKSTATION, TRAPDOOR, FENCE_GATE |
| `lockable_doors` | DOORS |
| `lockable_entities` | CHEST_BOATS, CHEST_MINECARTS, HOPPER_MINECARTS, ITEM_FRAMES |

Full syntax guide and worked examples: [`BLOCK_FAMILY_SYNTAX.md`](BLOCK_FAMILY_SYNTAX.md). Both the plain flat-list format (one material per line with `-`) and the bracketed expression format are accepted, and can be mixed across different lines of the same list - see the "Two valid formats" section of that document.

### ViaBackwards and ViaRewind detection

`ViaVersionIntegration` now detects ViaBackwards and ViaRewind in addition to ViaVersion, logging the version of each detected companion plugin.

### Debug command expanded

`/bp debug run` now also tests:

| Check | What it verifies |
|---|---|
| Lockable entities | Lists all ENTITIES family members with ACTIVE / INACTIVE status |
| Item frame protection | Reads `isLockableEntity(ITEM_FRAME)` and `isLockableEntity(GLOW_ITEM_FRAME)` and logs the result |
| Raid detection config | Reads `raid_detection.enabled` and confirms the audit logger is active |
| Integrations (expanded) | Calls `ViaVersionIntegration.getDetailedStatus()` for the full Via summary including companion plugins |
| NBT entity write/read | Spawns a temporary ArmorStand, writes an owner UUID, reads it back, then removes the entity |

### Audit log action classification

Clarified and corrected audit-trail event meanings:
- `ACCESS_DENIED` - a player attempted to access a protected block/entity and was blocked
- `OPENED` - a player with access opened the inventory of a protected block
- `RAID_EXPLOSION` - an explosion affected a protected block
- `ITEM_TAKEN` / `ITEM_PLACED` - a player moved items in a protected inventory

### StatHandler rewrite

`StatHandler.saveFile()` was rewritten. The previous implementation called `Files.move(ATOMIC_MOVE)` unconditionally, which throws `IOException: Failed to swap backup NBT file` on Windows and on Linux filesystems that do not support atomic cross-directory moves. The new implementation writes to a temp file, validates it with NBT-API before touching the live file, copies the validated temp file to the backup location, then replaces the live file. When atomic move is unavailable it falls back to a standard copy-and-delete. A `synchronized` block prevents concurrent writes. The dirty flag is preserved across the rewrite.

### Performance improvements

`purgeStalePbsEntries` now checks `World.isChunkLoaded()` before calling `Block.getType()`. Previously, every stored block location whose chunk was not already resident in memory triggered a synchronous chunk load, freezing the server thread for up to 20 seconds for players with large block lists. The fix skips any entry whose chunk is not already loaded and revisits it on a later call.

A Caffeine cache (`purgeThrottle`, 60-second TTL, 512 entries) throttles the per-player stale-entry scan to at most once per 60 seconds, preventing the scan from running on every block-place event for active players.

### Block family expression system

See "`BlockFamilyParser` cross-family validation and `/bp lockables`" above for the full token reference and validation behavior.

### Config and blocks auto-merge on every reload, with content preserved

`reloadConfigAndTranslations()` runs all merge and sync operations on every `/bp reload` call, not only on startup: `cleanLegacyConfigKeys()`, `mergeMissingConfigKeys()`, `mergeMissingBlocksKeys()`, language-file key merging, and the world-config re-scan all run on reload.

The merge guarantee for `blocks.yml`, `config.yml`, and `worlds.yml` is: any key or section that is entirely missing from the file on disk is added from the bundled JAR defaults, and a console line records what was added. Any key or section that already exists is never overwritten - this includes the contents of `blocks` lists inside `auto_drop_to_inventory`, and every line inside `lockable_tile_entities`, `lockable_shulker_boxes`, `lockable_blocks`, `lockable_doors`, and `lockable_entities`, whether written as plain material names or as bracketed family expressions. A future version that adds a brand-new top-level key (for example, a new boolean toggle) will see that key appended automatically; a family expression you already wrote, such as `'[*-CHEST_BOATS ITEM_FRAME]'`, is never rewritten, reordered, or stripped, regardless of how many versions you upgrade through.

In legacy flat-list mode specifically, individual new material entries shipped in a newer JAR are also appended to the matching list on disk if not already present - this is how a new release that adds a new block type makes that block immediately lockable for legacy-mode servers without requiring a manual edit. In modern expression mode this entry-level merge is skipped, because a `*` or `*-TAG` token already resolves dynamically against the current material set on every load; a brand-new sub-family tag introduced in a future version is not automatically added to an existing expression that does not already reference it, and must be added manually if wanted.

### Renamed config keys with automatic migration

`worlds_config_enabled` is renamed to `per_worlds_config`. On first startup after upgrading, the plugin reads the old key, writes its value under the new name, and removes the old entry from `config.yml`. No manual action is required.

### `auto_reload_configs` flag

New config key, default `true`. When set to `false`, the `ConfigFileWatcher` does not start and config files are not reloaded automatically; use `/bp reload` to apply changes manually. Useful on high-load servers or when editor tooling causes spurious filesystem events.

### SQLite usercache moved into plugin folder

`blockprot_usercache.sqlite` is now created inside `plugins/BlockProtReloaded/` rather than next to `server.jar`. On first startup after upgrading, any file found at the old location is moved automatically.

### Legacy folder migration improved

`migrateFromLegacyFolders()` also calls `mergeYamlUserValues()` after copying files. Admin values from `config.yml` and all language files in the legacy folder are merged key-by-key into the new location. The destination's existing value always wins on conflict, so no configured value is ever overwritten by the migration.

### Raid detection

`RaidDetectionListener` monitors every explosion (`BlockExplodeEvent` and `EntityExplodeEvent`) for proximity to lockable blocks. It does not cancel the explosion; it only detects and notifies.

Behavior:
- Any lockable block in the explosion's block list triggers a `WARN` line in the session log with block type, world, coordinates, and the actor (player name, entity type, or "environment").
- If the block is protected and its owner is online: an action-bar alert fires immediately, followed by a chat message with full coordinates. If the owner has `blockprot.blocks.tp`, a clickable `[Teleport]` link is appended.
- If the owner is offline: the alert is queued in memory and delivered as a chat message the next time they join, with the teleport link included if they have `blockprot.blocks.tp` at join time.

Controlled by `raid_detection.enabled` in `config.yml` (default `true`). Set to `false` to stop all detection and notification without removing the listener.

New translation keys: `messages.raid_alert`, `messages.raid_coords`, `messages.raid_tp_label`, `messages.raid_pending`.

### Support policy change

Starting with 1.3.3, support tiers are:

| Version range | Tier |
|---|---|
| 26.1.x (Paper/Purpur) | Primary development target |
| 1.21.1 through 1.21.x | Active support |
| 1.20.x and below | Not supported |

The compile target is raised from `paper-api:1.20.6` to `paper-api:1.21.1`. Servers below 1.21.1 no longer load the plugin (enforced by `api-version: 1.21.1` in `plugin.yml`). Legacy 1.20.x users must stay on 1.3.3 or earlier.

### Documentation policy change

Starting with 1.3.4, the project's `README.md` will be condensed to a short summary of all current features rather than documented feature-by-feature in full detail. `RELEASE_NOTES.md` becomes the single authoritative place where what changed between every version is recorded, including this entry. `docs/MODR_HANG_README.md`, the short description used on Modrinth and Hangar, will continue to be updated for store listings but is not a substitute for this file.

### BEACON added to lockable tile entities

BEACON is now recognized as a lockable tile entity. It can be locked via flat list (add `BEACON` to `lockable_tile_entities`) or family expression (`*-MISC` catches it as an ungrouped tile entity). The default `blocks.yml` now includes `BEACON`.

### SIGNS enabled by default in blocks.yml

All sign variants (floor, wall, hanging, wall-hanging for all 12 wood types) are now enabled by default via the family expression `[*-SIGN]` in `lockable_tile_entities`. This works in both flat-list and modern-expression modes since family expressions are always parsed.

### FLETCHING_TABLE added to WORKSTATION sub-family

FLETCHING_TABLE was present in the default flat list of `lockable_blocks` but was missing from the `isWorkstationMaterial()` check in `BlockFamilyParser` and the `blockValidator` in `DefaultConfig`. This meant it was silently rejected during family expression resolution and the `loadBlocksFromConfig()` validator. It is now a proper member of the `*-WORKSTATION` sub-family and the `lockable_blocks` BLOCKS family.

### /bp lockables now toggles items in blocks.yml

The /bp lockables GUI no longer only copies material names to clipboard. Left-clicking any block now toggles its active state: if inactive, it is added to blocks.yml; if active, it is removed. The change is saved to disk immediately, the relevant lockable lists are reloaded in memory, and a console log entry is written. The GUI refreshes to reflect the new state. Right-click still copies the exclusion token (`-MATERIAL_NAME`) to clipboard for manual config editing.

### /bp lockables "Enable all" entry per category

A NETHER_STAR "Enable all" entry appears at the top of every category section (Chests, Shulkers, Furnaces, Storage, Signs, Doors, Trapdoors, Fence Gates, Workstations, Interactive, Entities). Clicking it adds the appropriate family expression (e.g. `[*-CHEST]`, `[*]`, `[*-WORKSTATION]`) to blocks.yml, enabling every material in that category at once. The change is saved to disk and logged.

### /bp lockables active-first sorting

Both the category-grouped entries and the entity entries in the lockables GUI are now sorted with active (currently lockable) items first, followed by inactive items. Previously the order was inactive-first.

---

## Fixed & Improved

### Bug fixes

- **Block Lock menu button shifting**: every button used a shared incrementing slot counter, so any block type missing one button (for example, no Redstone on a workstation) shifted every later button one slot to the left. The same menu rendered differently for a chest versus a workstation versus a door. Fixed by giving every button a fixed slot in both `fill()` and `fillForEntity()` - see "Fixed menu layout" above.
- **StatHandler NBT save crash**: `Files.move(ATOMIC_MOVE)` failed on cross-device paths. Rewritten to temp-write, validate, backup, replace with fallback copy.
- **Server freeze on stats purge**: `purgeStalePbsEntries` called `Block.getType()` on unloaded chunks, triggering synchronous chunk loads. Fixed with a `World.isChunkLoaded()` guard.
- **EntityChangeBlockEvent AIR crash**: missing early-return for AIR blocks caused a `RuntimeException` in `BlockNBTHandler`.
- **Shulker auto-lock NPE**: wrote NBT via `block.getState()` on freshly placed shulkers whose `BlockEntity` was not yet initialized. Fixed with `block.getState(true)` and a `TileState` guard.
- **`/bp protdel` typo**: was registered as `protdell` (double L); corrected to `protdel`.
- **Update checker silent failure**: produced no output when running ahead of the latest release; now logs silently instead of erroring.
- **EntitySettingsInventory permission**: used the raw string `"blockprot.admin"` instead of `Permissions.USER_ADMIN` and lacked an `isOp()` fallback. Fixed.
- **CraftBukkit error path**: the error message shown to plain-CraftBukkit servers is now properly translated instead of showing a raw key string.

### Inherited bug fixes

| Issue | Title | Fixed in |
|---|---|---|
| [#302](https://github.com/spnda/BlockProt/issues/302) | 1.21.1 not supported properly | Upstream |
| [#304](https://github.com/spnda/BlockProt/issues/304) | Unknown warning/error occurred | Upstream |
| [#305](https://github.com/spnda/BlockProt/issues/305) | Taking map from Search Players slot | Upstream |
| [#319](https://github.com/spnda/BlockProt/issues/319) | Emptying large chests incorrectly on hoppers | Upstream |
| [#323](https://github.com/spnda/BlockProt/issues/323) | 1.21.3 not supported properly | Upstream |
| [#326](https://github.com/spnda/BlockProt/issues/326) | Redstone protection bugged with trapdoors | Upstream |
| [#327](https://github.com/spnda/BlockProt/issues/327) | Paper NoClassDefFoundError Container | Upstream |
| [#328](https://github.com/spnda/BlockProt/issues/328) | Colored shulker boxes reset when broken | Upstream |
| [#329](https://github.com/spnda/BlockProt/issues/329) | AIR block error on entity change | Upstream |
| [#330](https://github.com/spnda/BlockProt/issues/330) | Pale oak door/gate cannot be locked | Upstream |
| [#332](https://github.com/spnda/BlockProt/issues/332) | Unable to use BlockProt on 1.21.7 | Upstream |
| [#338](https://github.com/spnda/BlockProt/issues/338) | Support for Minecraft 1.21.9 | Upstream |
| [#339](https://github.com/spnda/BlockProt/issues/339) | Shulker box vanishes when broken | Upstream |
| [#344](https://github.com/spnda/BlockProt/issues/344) | Console spam (NbtApiException) | Upstream |
| [#165](https://github.com/spnda/BlockProt/issues/165) | Random inventory close on interaction | Upstream |
| [#266](https://github.com/spnda/BlockProt/issues/266) | `public_is_friend_by_default` setting | Upstream |
| [#294](https://github.com/spnda/BlockProt/issues/294) | Geyser players missing from friend search | Upstream |
| [#185](https://github.com/spnda/BlockProt/issues/185) | Hopper protection below chest not working | Upstream |
| [#214](https://github.com/spnda/BlockProt/issues/214) | Making doors public only half the door | Upstream |
| [#224](https://github.com/spnda/BlockProt/issues/224) | Locked shulker boxes break without dropping | Upstream |
| [#227](https://github.com/spnda/BlockProt/issues/227) | Hopper and piston protection swapped | Upstream |
| [#235](https://github.com/spnda/BlockProt/issues/235) | Lectern protection issues | Upstream |
| [#240](https://github.com/spnda/BlockProt/issues/240) | Hopper protecting not working | Upstream |
| [#250](https://github.com/spnda/BlockProt/issues/250) | Can get Player Head from menu | Upstream |
| [#256](https://github.com/spnda/BlockProt/issues/256) | Can't copy/paste settings between blocks | Upstream |
| [#263](https://github.com/spnda/BlockProt/issues/263) | Can take Telescope from Menu | Upstream |
| [#272](https://github.com/spnda/BlockProt/issues/272) | Can't move items as admin/op | Upstream |
| [#276](https://github.com/spnda/BlockProt/issues/276) | Items can still be placed into chests | Upstream |
| [#279](https://github.com/spnda/BlockProt/issues/279) | Items being deleted on interact | Upstream |
| [#280](https://github.com/spnda/BlockProt/issues/280) | Shulker box items deleted after breaking | Upstream |
| [#281](https://github.com/spnda/BlockProt/issues/281) | Dropper not working after unlock | Upstream |
| [#287](https://github.com/spnda/BlockProt/issues/287) | Double chest not handled on inventory click | Upstream |
| [#289](https://github.com/spnda/BlockProt/issues/289) | Chest locked but friends can open | Upstream |
| [#296](https://github.com/spnda/BlockProt/issues/296) | Item lost on interact | Upstream |
| [#297](https://github.com/spnda/BlockProt/issues/297) | Item drops can't be picked by hopper | Upstream |
| [#321](https://github.com/spnda/BlockProt/issues/321) | Wither explosions breaking protected blocks | Upstream |

### Fork-specific fixes

| Issue | Title | Fix |
|---|---|---|
| [#268](https://github.com/spnda/BlockProt/issues/268) | Friend list copy/paste should replace instead of appending | Paste replaces friend list |
| [#306](https://github.com/spnda/BlockProt/issues/306) | Plugin causes server lag (hopper checks) | ProtectedBlockCache with O(1) early-exit |
| [#324](https://github.com/spnda/BlockProt/issues/324) | Option to allow all protected blocks to be broken | `allow_break_protected_blocks` config key |
| [#274](https://github.com/spnda/BlockProt/issues/274) | Players can access second row of large chest | Inventory double-chest gating |
| [#349](https://github.com/spnda/BlockProt/issues/349) | Warn spam in console (NBT swap error) | StatHandler rewrite with temp-write-validate-replace |

### Dependency updates

| Dependency | Old | New | Reason |
|---|---|---|---|
| `paper-api` (compile) | `1.20.6-R0.1-SNAPSHOT` | `1.21.1-R0.1-SNAPSHOT` | Support baseline raise |
| `FoliaLib` | `0.4.3` | `0.5.1` | Bug fixes, latest stable |
| `item-nbt-api` | `2.15.6` | `2.15.7` | 26.1.x and 1.21.11 support |
| `MockBukkit` (test) | `v1.20:3.93.2` | `v1.21:4.8.0` | Aligns with new compile baseline |

### Known Issues

- **MockBukkit unresolvable**: `MockBukkit-v1.21:4.8.0` returns HTTP 401 from jitpack.io. Build with `-x test` to skip tests.
- **Gradle `shadowJar` false up-to-date**: on some local setups, running `:blockprot-spigot:shadowJar` after only source changes (no `clean`) can report `BUILD SUCCESSFUL` while reusing a stale or absent output if Gradle's up-to-date check does not detect the change. If the jar is missing or unexpectedly small after a build, rerun with `--rerun-tasks`, or run `gradlew clean :blockprot-spigot:shadowJar` to force a full rebuild.

## Complete Issue Reference (>= #250)

Every non-trivial issue from the upstream repository sorted by category. Issues closed upstream are fixed in our fork by inheritance. Issues open upstream but resolved in our fork are noted as fork-specific fixes.

### Bugs fixed upstream (inherited)

These were closed in the upstream repository and are fixed in our fork:

| Issue | Title |
|---|---|
| [#344](https://github.com/spnda/BlockProt/issues/344) | Console spam (NbtApiException) |
| [#339](https://github.com/spnda/BlockProt/issues/339) | Shulker box vanishes when broken |
| [#338](https://github.com/spnda/BlockProt/issues/338) | Support for 1.21.9 |
| [#337](https://github.com/spnda/BlockProt/issues/337) | Bug (untitled) |
| [#336](https://github.com/spnda/BlockProt/issues/336) | Bug (untitled) |
| [#335](https://github.com/spnda/BlockProt/issues/335) | Bug (untitled) |
| [#332](https://github.com/spnda/BlockProt/issues/332) | Unable to use on 1.21.7 server |
| [#330](https://github.com/spnda/BlockProt/issues/330) | Pale oak door/gate cannot be locked |
| [#329](https://github.com/spnda/BlockProt/issues/329) | Air block error on entity change |
| [#328](https://github.com/spnda/BlockProt/issues/328) | Colored shulker boxes reset when broken |
| [#327](https://github.com/spnda/BlockProt/issues/327) | Paper NoClassDefFoundError Container |
| [#326](https://github.com/spnda/BlockProt/issues/326) | Redstone protection bugged with trapdoors |
| [#323](https://github.com/spnda/BlockProt/issues/323) | 1.21.3 not supported properly |
| [#319](https://github.com/spnda/BlockProt/issues/319) | Emptying large chests incorrectly on hoppers |
| [#305](https://github.com/spnda/BlockProt/issues/305) | Taking map from Search Players slot |
| [#304](https://github.com/spnda/BlockProt/issues/304) | Unknown warning or error occurred |
| [#302](https://github.com/spnda/BlockProt/issues/302) | 1.21.1 not supported properly |
| [#297](https://github.com/spnda/BlockProt/issues/297) | Item drops can't be picked by hopper |
| [#296](https://github.com/spnda/BlockProt/issues/296) | Item lost on interact |
| [#293](https://github.com/spnda/BlockProt/issues/293) | Problem with chests regarding hoppers |
| [#290](https://github.com/spnda/BlockProt/issues/290) | Bug (untitled) |
| [#289](https://github.com/spnda/BlockProt/issues/289) | Chest locked but friends can open |
| [#281](https://github.com/spnda/BlockProt/issues/281) | Dropper not working after unlock |
| [#280](https://github.com/spnda/BlockProt/issues/280) | Shulker box items deleted after breaking |
| [#279](https://github.com/spnda/BlockProt/issues/279) | Items being deleted on interact |
| [#276](https://github.com/spnda/BlockProt/issues/276) | Items can still be placed into chests |
| [#275](https://github.com/spnda/BlockProt/issues/275) | Hoppers can take items from protected chests |
| [#274](https://github.com/spnda/BlockProt/issues/274) | Players can access second row of large chest |
| [#272](https://github.com/spnda/BlockProt/issues/272) | Can't move items as admin/op |
| [#263](https://github.com/spnda/BlockProt/issues/263) | Can take Telescope from Menu |
| [#256](https://github.com/spnda/BlockProt/issues/256) | Can't copy/paste settings between blocks |
| [#255](https://github.com/spnda/BlockProt/issues/255) | Can't remove everyone from settings |
| [#250](https://github.com/spnda/BlockProt/issues/250) | Can get Player Head from menu |
| [#240](https://github.com/spnda/BlockProt/issues/240) | Hopper protecting not working |
| [#236](https://github.com/spnda/BlockProt/issues/236) | Copy Paste not working properly |
| [#235](https://github.com/spnda/BlockProt/issues/235) | Lectern protection issues |
| [#232](https://github.com/spnda/BlockProt/issues/232) | Sign not in lockable_blocks |
| [#230](https://github.com/spnda/BlockProt/issues/230) | Can't unlock wall sign |
| [#227](https://github.com/spnda/BlockProt/issues/227) | Hopper and piston protection swapped |
| [#226](https://github.com/spnda/BlockProt/issues/226) | Chest unlocked but shown as locked |
| [#224](https://github.com/spnda/BlockProt/issues/224) | Locked shulker boxes break without dropping |
| [#221](https://github.com/spnda/BlockProt/issues/221) | Chest locked but friends can open |
| [#215](https://github.com/spnda/BlockProt/issues/215) | Chests cannot be placed on public |
| [#214](https://github.com/spnda/BlockProt/issues/214) | Making doors public only half the door |
| [#185](https://github.com/spnda/BlockProt/issues/185) | Hopper protection below chest not working |

### Bugs open upstream, fixed in fork

| Issue | Title | Fork Fix |
|---|---|---|
| [#306](https://github.com/spnda/BlockProt/issues/306) | Plugin causes server lag | ProtectedBlockCache with Caffeine O(1) early-exit |
| [#324](https://github.com/spnda/BlockProt/issues/324) | Allow breaking protected blocks | `allow_break_protected_blocks` config key |
| [#349](https://github.com/spnda/BlockProt/issues/349) | Warn spam in console (NBT swap error) | StatHandler rewrite with temp-write-validate-replace |

### Features open upstream, not yet implemented

| Issue | Title |
|---|---|
| [#346](https://github.com/spnda/BlockProt/issues/346) | New configuration option |
| [#334](https://github.com/spnda/BlockProt/issues/334) | Colored prompt messages |
| [#322](https://github.com/spnda/BlockProt/issues/322) | Toggle for simplified hopper logic |
| [#318](https://github.com/spnda/BlockProt/issues/318) | Ability to configure lockable blocks per world |
| [#303](https://github.com/spnda/BlockProt/issues/303) | Respect spawn protection radius |
| [#298](https://github.com/spnda/BlockProt/issues/298) | Integration support for ClaimChunk |
| [#295](https://github.com/spnda/BlockProt/issues/295) | Lock trapdoors and iron doors |
| [#282](https://github.com/spnda/BlockProt/issues/282) | MySQL Support |
| [#269](https://github.com/spnda/BlockProt/issues/269) | Shows the owner of the box |
| [#268](https://github.com/spnda/BlockProt/issues/268) | Removed old friends when copy/pasting |
| [#267](https://github.com/spnda/BlockProt/issues/267) | Expire locks after owner offline for x time |
| [#228](https://github.com/spnda/BlockProt/issues/228) | Separate Redstone in config |
| [#225](https://github.com/spnda/BlockProt/issues/225) | Stats more Features |
| [#192](https://github.com/spnda/BlockProt/issues/192) | Support Inspect contents block name i18n |
| [#183](https://github.com/spnda/BlockProt/issues/183) | Support Residence |
| [#154](https://github.com/spnda/BlockProt/issues/154) | Support for GriefPrevention |

### Currently open bugs (upstream, confirmed not yet fixed)

| Issue | Title |
|---|---|
| [#238](https://github.com/spnda/BlockProt/issues/238) | Piston Protection Shulker Box |

## Reporting an Issue

If you reached this point in the document, thank you for reading. When reporting a bug, please include the following information so the issue can be diagnosed and fixed faster:

| Field | What to include |
|---|---|
| Discord (optional) | Your Discord username so I can follow up directly |
| Error description | Exactly what the error says and step-by-step how to reproduce it |
| Multimedia | Screenshots or videos showing the issue |
| Server info | Server version, software (Paper / Purpur / Spigot), and a list of other plugins installed |
| Error log | The full error log (use a paste site like https://pastes.dev if it is long) |



