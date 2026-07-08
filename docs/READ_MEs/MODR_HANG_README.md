# BlockProt Reloaded - Modrinth & Hangar Description

BlockProt Reloaded is a maintained fork of [BlockProt](https://github.com/spnda/BlockProt) for Paper and Spigot servers (Minecraft 1.21.1 through 26.1.x). It preserves the original NBT-based protection core while adding stability fixes, performance improvements, and new features not present upstream.


## What it does

Players sneak and right-click any lockable block or entity to open the protection GUI - no commands required. From the GUI they can lock or unlock, manage a per-block friend list with Read, Write, or Manager roles, control redstone and hopper access, inspect the access audit trail, copy and paste settings between blocks, and set an optional protection expiry timer.

Admins get a separate toolset: a paged block browser (`/bp lockables`), per-player block lists (`/bp info`), an unlock GUI (`/bp unlock`), diagnostics (`/bp debug run`), and a full audit log per protected block.


## What is protected

**Tile entities (storage):** all chest variants, all 17 shulker box colors, all 12 shelf variants (1.21.9+), furnace, smoker, blast furnace, hopper, dispenser, dropper, barrel, brewing stand, decorated pot, chiseled bookshelf, crafter, jukebox, lectern, beehive, bee nest.

**Interactive blocks:** doors, trapdoors, and fence gates (all wood variants plus iron and all copper oxidation stages), anvils, cauldrons, enchanting table, grindstone, stonecutter, loom, cartography table, smithing table, composter, bell, note block, dragon egg.

**Entities:** item frames, glowing item frames, chest boats (all wood variants), chest minecarts, hopper minecarts. Tamed animals (wolves, cats, parrots, horses, llamas) via the entity protection system.

All block lists are defined in `blocks.yml` and can be changed at runtime without restarting.


## Features added in this fork

### Fixed lock menu layout
Every button in the Block Lock GUI now occupies a fixed slot across all block and entity types. Previously, a missing button (for example, no Redstone button on a workstation) caused all subsequent buttons to shift left, making the menu inconsistent between block types.

### Entity protection
Tamed animals (wolves, cats, parrots, horses, llamas) can be protected. Right-click your tamed animal while holding the configured menu item (default: Stick) to open the protection GUI. Disabled by default; enable under `entity_protection` in `config.yml`. Renamed from `pet_protection` in earlier versions - the old key is migrated automatically on upgrade, no manual edit needed.

### Villager workstation protection
A villager whose job-site memory points to a protected workstation block inherits that block's protection automatically. Non-owners cannot damage or trade with the linked villager and cannot break or interact with blocks in a configurable area around the workstation. A "Locate linked villager" Emerald button appears in the workstation's lock menu, showing a short particle effect at the villager's location visible only to the clicking player. Configurable in `config.yml`:

```yaml
villager_workstation_protection:
  enabled: true
  radius: 2           # horizontal search radius (0-8)
  vertical_radius: 1  # vertical search radius (0-4)
```

### Item frame protection with automatic block linking
Item frames and glowing item frames are protected with the same sneak-and-right-click flow. A frame placed on the face of a lockable block is automatically linked to that block: it shares the block's owner, friends, and lock state exactly, and interacting with it opens the block's lock menu directly. Frames not mounted on a lockable block keep the standalone entity-protection flow. ImageFrame plugin creator tags are respected automatically.

### Storage vehicle protection
Chest boats, storage minecarts, and hopper minecarts support the same sneak-and-right-click flow. Once protected, non-owners cannot open the inventory, and the vehicle is protected from damage and destruction. Hopper extraction can be toggled per vehicle from the in-menu Redstone button.

### Block family expression system
`blocks.yml` and `worlds.yml` support compact family expressions alongside standard flat material lists:

```yaml
lockable_tile_entities:
  - "[*]"                        # all tile entities
  - "[*-CHEST -*SIGN]"           # all chests, no signs
lockable_entities:
  - "[*-CHEST_BOATS ITEM_FRAME]" # chest boats and item frames only
```

Sub-families: `CHEST`, `FURNACE`, `SHELF`, `TRANSPORT`, `MISC`, `SIGN` (tile entities); `SHULKERS`; `ANVIL`, `CAULDRON`, `WORKSTATION`, `TRAPDOOR`, `FENCE_GATE` (blocks); `DOORS`; `CHEST_BOATS`, `CHEST_MINECARTS`, `HOPPER_MINECARTS`, `ITEM_FRAMES` (entities).

Full syntax: [`docs/MODERN SYNTAX AND LEGACY/BLOCK_FAMILY_SYNTAX.md`](../MODERN%20SYNTAX%20AND%20LEGACY/BLOCK_FAMILY_SYNTAX.md). Full material list: [`docs/MODERN SYNTAX AND LEGACY/LOCKABLE_BLOCKS_REFERENCE.md`](../MODERN%20SYNTAX%20AND%20LEGACY/LOCKABLE_BLOCKS_REFERENCE.md).

### Config and blocks auto-merge on every reload
On every startup and `/bp reload`, missing keys are added from JAR defaults and renamed keys are migrated. Existing values - including every family expression already written in `blocks.yml` - are never overwritten or reordered. In legacy flat-list mode, new material entries from a newer JAR are appended automatically so new block types become lockable without manual edits.

### Raid detection
Monitors explosions near lockable blocks. Logs the event, alerts the owner via action bar and chat if online, or queues the alert for delivery at next login. Configurable via `raid_detection.enabled`.

### StatHandler stability
`saveFile()` was rewritten to use a temp-write, validate, backup, replace sequence with a fallback for filesystems that do not support atomic cross-directory moves (fixes `Failed to swap backup NBT file` on Windows). A Caffeine cache throttles the per-player stale-entry scan to once per 60 seconds per player. A chunk-load guard prevents synchronous chunk loads in `purgeStalePbsEntries`.

### Performance
- Caffeine cache on `HopperEventListener` for O(1) hopper early-exit via `ProtectedBlockCache`.
- `purgeStalePbsEntries` skips unloaded chunks entirely to prevent main-thread stalls.
- All SQL and NBT I/O is asynchronous.

### Other additions
- Ownership transfer: `/bp transfer <player>` and `/bp transfer all <player>`.
- Timed friend access: grant a friend temporary access that auto-revokes when the timer expires.
- Protection expiry: set an expiry timer on any lock from the GUI (`7d`, `1mo`, `2d12h`).
- Per-world configuration via `worlds.yml` (`per_worlds_config: true`).
- Auto-reload via `ConfigFileWatcher` (`auto_reload_configs`, default `true`).
- Automatic backup zip on version upgrade.
- Legacy folder migration: copies data from `BlockProt` or `BlockProtPlus` folders automatically on first boot after rename.
- SQLite usercache moved into the plugin data folder; old file is migrated automatically.
- Inactivity cleanup: removes protections owned by long-inactive players (`inactivity_cleanup_days`, disabled by default).
- WorldEdit paste auto-lock (disabled by default).
- PlaceholderAPI placeholders: `%blockprot_global_block_count%`, `%blockprot_own_block_count%`, `%blockprot_default_friends%`.
- Folia compatibility via FoliaLib.
- ViaVersion, ViaBackwards, and ViaRewind detection.


## Commands

| Command | Permission | Description |
|---|---|---|
| `/bp lockables` | `blockprot.user.admin` | Paged GUI listing all blocks and entities with active/inactive status |
| `/bp info <player>` | `blockprot.user.admin` | All blocks owned by a player with teleport links |
| `/bp unlock <player>` | `blockprot.user.admin` | Inspect or remove any player's protections |
| `/bp reload` | op | Reload all config files and merge missing keys |
| `/bp debug run` | `blockprot.debug` | Run internal diagnostics |
| `/bp transfer <player>` | `blockprot.user` | Transfer ownership of the looked-at block |
| `/bp transfer all <player>` | `blockprot.user` | Transfer all owned blocks to another player |


## Permissions

| Permission | Default | Description |
|---|---|---|
| `blockprot.user` | true | All standard player features |
| `blockprot.user.admin` | op | Admin commands and GUIs |
| `blockprot.max_blocks` | false | Exempt from block count limit |
| `blockprot.blocks.tp` | op | Teleport to blocks from stats and admin GUIs |
| `blockprot.debug` | false | Access to `/bp debug` |


## Compatibility

| | |
|---|---|
| Minecraft | 1.21.1, 1.21.x, 26.1.x |
| Server software | Paper, Spigot, Purpur, Folia |
| Java | 25+ required |
| MySQL | MySQL 8+, MariaDB 10.5+ (optional) |
| Languages | EN, ES, DE, FR, IT, PT-BR, RU, JA, KO, ZH-CN, ZH-TW, CS, SK, PL, TR |


## Integrations

Towny, WorldGuard, Lands, ClaimChunk, PlaceholderAPI, SkinsRestorer, WorldEdit/FAWE, Floodgate/Geyser, ImageFrame, ViaVersion/ViaBackwards/ViaRewind, Folia.


## Install

Place the JAR in `plugins/` and restart. Requires Java 25 and Paper or Spigot 1.21.1+. On upgrade, config keys are migrated automatically - no manual edits required.


## Documentation

- Release history: [`docs/RELEASE_NOTES/1.3.3.RELEASE_NOTES.md`](../RELEASE_NOTES/1.3.3.RELEASE_NOTES.md)
- Block family syntax: [`docs/MODERN SYNTAX AND LEGACY/BLOCK_FAMILY_SYNTAX.md`](../MODERN%20SYNTAX%20AND%20LEGACY/BLOCK_FAMILY_SYNTAX.md)
- Full lockable materials reference: [`docs/MODERN SYNTAX AND LEGACY/LOCKABLE_BLOCKS_REFERENCE.md`](../MODERN%20SYNTAX%20AND%20LEGACY/LOCKABLE_BLOCKS_REFERENCE.md)

**Documentation policy from 1.3.4 onward:** `README.md` will be condensed to a short feature summary. [`docs/RELEASE_NOTES/1.3.3.RELEASE_NOTES.md`](../RELEASE_NOTES/1.3.3.RELEASE_NOTES.md) becomes the single authoritative record of what changed between versions. This file (Modrinth/Hangar description) will continue to be updated for store listings.


Repository: https://github.com/VictorGugug/BlockProt-Reloaded
Issues and contributions: https://github.com/VictorGugug/BlockProt-Reloaded/issues
License: GNU General Public License v3 - based on [BlockProt](https://github.com/spnda/BlockProt) by spnda.
