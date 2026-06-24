<div align="center">

<img src="https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/RELEASE%20TITLES/BPR.png" alt="BlockProt Reloaded" />

---
[![CI](https://img.shields.io/github/actions/workflow/status/VictorGugug/BlockProt-Reloaded/ci.yml?branch=main&style=flat-square&label=CI)](https://github.com/VictorGugug/BlockProt-Reloaded/actions)
[![Release](https://img.shields.io/github/v/release/VictorGugug/BlockProt-Reloaded?style=flat-square&color=brightgreen&label=Release)](https://github.com/VictorGugug/BlockProt-Reloaded/releases)
[![Modrinth](https://img.shields.io/modrinth/dt/C2ZYTu62?style=flat-square&color=00AF5C&logo=modrinth&label=Modrinth)](https://modrinth.com/plugin/blockprot-reloaded)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25+-orange?style=flat-square)](https://openjdk.org/projects/jdk/25/)
[![Paper](https://img.shields.io/badge/Paper-1.21.1%2B%20%7C%2026.x-white?style=flat-square)](https://papermc.io/)

Java 25 · Paper 1.21.1 through 26.x · MySQL index · per-world config · access audit · pet protection · auto-backup · ownership transfer · item frame and vehicle protection

</div>

Block protection plugin for Paper and Spigot servers. Players lock chests, furnaces, doors, and other blocks through a GUI — no commands required. This fork extends the original NBT core with stability fixes, performance improvements, and new features not present in upstream.


## Screenshots

### Block Lock Menu
![Block lock](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/main_menu.png)

The main interface for locking blocks. Sneak and right-click any lockable block to open it. The two-row inventory shows functional buttons on top and utility buttons on the bottom row.

### Friend Settings
![Friend settings](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/friend_settings.png)

Add or remove friends from your protected blocks and assign them Read, Write, or Manager permission levels.

### Player Settings
![Player settings](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/user_settings.png)

Configure personal preferences such as lock-on-place behavior and hint toggles.

### Redstone Settings
![Redstone settings](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/redstone_settings.png)

Control redstone, piston, and hopper interaction for each of your protected blocks independently.

### Block Info
![Block info](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/block_info.png)

View the owner, friend list, protection status, and additional metadata for any protected block.

### Access Log
![Access log](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/access_log.png)

See a summary of all access attempts to your protected blocks. The log records when friends opened or interacted with your blocks.

### Access Log Detail
![Inside log](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/inside_log.png)

Each access record shows a timestamp, player name, and action type: opened, item taken, item placed, or access denied.

### Timed Access
![Timed access](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/timed_access.png)

Grant temporary access to a friend with a time limit. Access is automatically revoked when the timer expires.

### Admin Player Block-List
![Admin view](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/admin_view.png)

Admin tool to view all blocks owned by any player, including offline players. Click any entry to teleport. Shows real block icons and lock timestamps.


## Installing

Download the latest JAR from [Releases](https://github.com/VictorGugug/BlockProt-Reloaded/releases) or [Modrinth](https://modrinth.com/plugin/blockprot-reloaded) and place it in your `plugins/` folder. Requires **Java 25** and **Paper or Spigot 1.21.1+**.

### Build from Source

```bash
git clone https://github.com/VictorGugug/BlockProt-Reloaded.git
cd BlockProt-Reloaded
./gradlew :blockprot-spigot:shadowJar
```

```powershell
# Windows
.\gradlew.bat :blockprot-spigot:shadowJar
```

Output is placed at `spigot/build/libs/BlockProt-VERSION.jar`.

The version suffix is controlled by the `versionSuffix` property in `gradle.properties`:

| Value | Output |
|---|---|
| *(blank)* | `BlockProt-1.3.3.jar` — stable release |
| `SNAPSHOT` | `BlockProt-1.3.3-SNAPSHOT.jar` |
| `beta.1` | `BlockProt-1.3.3-beta.1.jar` |
| `rc.1` | `BlockProt-1.3.3-rc.1.jar` |


## File Layout

```
plugins/BlockProtReloaded/
├── config.yml                    Main configuration
├── blocks.yml                    Lockable block definitions
├── worlds.yml                    Per-world overrides (optional)
├── blockprot_usercache.sqlite    Player UUID cache
├── mysql/
│   ├── mysql.yml                 MySQL / storage configuration
│   └── blockprot_audit.sqlite    SQLite access audit log
├── lang/
│   └── translations_*.yml        15 bundled language files
├── logs/
│   ├── blockprot-current.log     Active session log
│   └── blockprot-YYYY-MM-DD--YYYY-MM-DD.log   Rotated archives
└── backups/
    └── YYYY-MM-DD_HH-MM_vVERSION.zip   Created on version upgrade
```

`blocks.yml` is generated automatically on first start. If your existing `config.yml` had lockable block lists in it, those values are migrated to `blocks.yml` automatically and removed from `config.yml`. The player UUID cache is now stored inside the plugin folder, not next to `server.jar`.


## GUI Overview

### Block Lock Menu

Opened by sneaking and right-clicking any lockable block. Two-row inventory.

**Top row — functional buttons:**

| Slot | Item | Function |
|---|---|---|
| 0 | Block icon | Lock or unlock toggle |
| 1 | Redstone | Redstone, piston, and hopper settings *(storage and traversal blocks only)* |
| 2 | Player Head | Manage friends |
| 3 | Name Tag | Set a custom block name |
| 4 | Ender Pearl | Transfer block ownership |
| 5 | Emerald | Locate the villager linked to this workstation *(workstation blocks only)* |
| 6 | Hopper or Lime Dye | Protection expiry *(storage blocks only, and only when enabled in config)* |

The redstone button is not shown for display-only blocks such as signs, lecterns, and workstations, because those have no hopper or redstone interaction to gate. Every button above lives in a fixed slot regardless of block type — a slot that does not apply to the current block is simply left empty, so the remaining buttons never shift position.

**Bottom row — utility buttons:**

| Slot | Item | When shown |
|---|---|---|
| 9 | Spyglass | Owner or admin, block has an inventory |
| 13 | Clock | Owner or admin, audit logger active |
| 14 | Knowledge Book | Manager role, clipboard has data |
| 15 | Paper | Manager role |
| 16 | Compass | Manager role or admin |
| 17 | Barrier | Always |

Copying sends a confirmation to the action bar. Pasting replaces the entire friend list rather than appending, which matches the expected behavior and resolves upstream issue [#268](https://github.com/spnda/BlockProt/issues/268).

### Item Frame and Vehicle Protection

Item frames, glowing item frames, chest boats, storage minecarts, and hopper minecarts can all be protected using the same sneak-and-right-click flow. The menu shown for these entities is simplified: it only shows lock/unlock and friend management, since redstone and expiry options do not apply to them. See the Features section for full details.

### Lockable Blocks Browser (`/bp lockables`)

Six-row paged GUI listing every block the plugin knows about.

Blocks with a green **Status: ACTIVE** label are currently lockable. Blocks with a red **Status: INACTIVE** label are recognized by the system but not enabled in `blocks.yml`. Clicking any entry sends a clickable message to your chat that copies the material name to your clipboard when clicked.

Left-click copies `MATERIAL_NAME` for use as a flat list entry or include token. Right-click copies `-MATERIAL_NAME` for use as an exclusion token in a family expression.

The info book shows your server version, your client version when ViaVersion is active, and a count of active vs inactive blocks. Full documentation of families and sub-families is in [`docs/LOCKABLE_BLOCKS_REFERENCE.md`](docs/LOCKABLE_BLOCKS_REFERENCE.md).

### Statistics List

Each entry shows the real block icon with its type and coordinates, plus how long ago the block was locked. Clicking an entry teleports you to it, provided you have the `blockprot.blocks.tp` permission. Stale entries where the block no longer exists are filtered automatically.

### Player Block-List (`/bp info <player>`)

Six-row GUI showing every block owned by the selected player with real block icons, coordinates, and lock timestamps. Supports pagination. Works for offline players. Requires `blockprot.user.admin`.

### User Menu (`/bp user`, requires `use_menus: true`)

| Slot | Item | Action |
|---|---|---|
| 11 | Writable Book | Personal settings |
| 12 | Player Head | Default friend list |
| 13 | Book | Block statistics |
| 14 | Nether Star | About this plugin |

### Admin Menu (`/bp admin`, requires `use_menus: true` and `blockprot.user.admin`)

| Slot | Item | Action |
|---|---|---|
| 11 | Comparator | Reload config and translations |
| 12 | Spyglass | Check for updates |
| 13 | Chain | List active integrations |
| 14 | Book | Server statistics |
| 15 | Command Block | Run diagnostics |
| 16 | Player Head | Open player block-list GUI |


## Commands

Command visibility is controlled by `use_menus` in `config.yml`. With `use_menus: false` (the default), all CLI subcommands below are active. With `use_menus: true`, only `/bp user` and `/bp admin` are active and everything else is hidden from tab-complete.

**GUI commands (active when `use_menus: true`):**

| Command | Permission | Description |
|---|---|---|
| `/bp user` | `blockprot.user` | Open the User Menu |
| `/bp admin` | `blockprot.user.admin` | Open the Admin Menu |

**User commands (active when `use_menus: false`):**

| Command | Permission | Description |
|---|---|---|
| `/bp help` | `blockprot.user` | Show available commands |
| `/bp about` | `blockprot.user` | Show plugin version and fork info |
| `/bp settings` | `blockprot.user` | Open personal settings GUI |
| `/bp friends` | `blockprot.user` | Manage your default friend list |
| `/bp friends addall <player>` | `blockprot.user` | Add a player as friend on all blocks you own at once |
| `/bp stats` | `blockprot.user` | Open your block statistics GUI |
| `/bp transfer <player>` | `blockprot.user` | Transfer the looked-at block to another player |
| `/bp transfer all <player>` | `blockprot.user` | Transfer every block you own to another player |
| `/bp disablehints` | `blockprot.user` | Toggle protection hint messages |

**Admin commands (always accessible regardless of `use_menus`):**

| Command | Permission | Description |
|---|---|---|
| `/bp info <player>` | `blockprot.user.admin` | View all blocks owned by a player, including offline players |
| `/bp unlock <player>` | `blockprot.user.admin` | GUI to inspect or remove protections from any player's blocks |
| `/bp lockables` | `blockprot.user.admin` | Browse all blocks the system knows about with active/inactive status |
| `/bp reload` | op | Backup then reload all config files and translations |
| `/bp update` | op | Check for plugin updates |
| `/bp integrations` | op | List active plugin integrations |
| `/bp debug <subcommand>` | `blockprot.debug` | Run diagnostics |


## Permissions

| Permission | Default | Description |
|---|---|---|
| `blockprot.user` | true | All standard player features: lock, friends, settings, stats, transfer |
| `blockprot.user.admin` | op | Admin features: player block-lists, unlock GUI, admin commands |
| `blockprot.max_blocks` | false | Exempt from the `player_max_locked_block_count` limit |
| `blockprot.blocks.tp` | op | Teleport to blocks from the statistics or admin block-list GUI |
| `blockprot.debug` | false | Access to `/bp debug` diagnostics |


## Features

### Core Block Protection

Sneak and right-click any lockable block to open the protection GUI. Add friends with Read, Write, or Manager permission levels. Redstone, hopper, and piston interaction can be toggled per block. Copy and paste protection settings between blocks. A per-player default friend list is applied to all new locks automatically.

### Java 25 and Paper 1.21.1 through 26.x Compatibility

Compiles against the Paper 1.21.1 API and runs on every version from 1.21.1 through 26.1.x. 1.20.x received its last big update in 1.3.3. Both the classic `1.x` and the year-based `26.x` version schemes are detected at runtime.

### Item Frame Protection

Item frames and glowing item frames can be protected with the same sneak-and-right-click flow used for blocks. Once protected, non-owners cannot rotate or swap the displayed item and cannot break or shoot the frame. The ImageFrame plugin is supported: frames carrying an ImageFrame creator tag are automatically treated as owned by the creator, protecting multi-map image displays without any extra configuration. Protection data is stored in the entity's persistent NBT and survives chunk reloads and server restarts.

A frame mounted on a lockable block (a chest, a door, etc.) is automatically linked to that block on placement instead of becoming an independent protection unit. A linked frame has no owner of its own: it shares the underlying block's owner, friends, and lock state exactly, and interacting with it opens that block's lock menu directly. This avoids having to separately protect a decorative frame placed next to a chest you already protected, and stops griefers from placing a frame on someone else's storage block to interfere with it. Frames not mounted on a lockable block keep the standalone entity-protection flow described above.

### Chest Boat and Minecart Protection

Chest boats, storage minecarts, and hopper minecarts can be protected the same way. Once protected, right-clicking the vehicle without sneaking is blocked for non-owners and non-friends, preventing inventory access. The protection menu shows only lock/unlock and friend management, since redstone settings and expiry do not apply to mobile entities.

### Contextual Lock Menu

The lock menu adapts its available options based on the type of block being protected. Storage blocks such as chests, barrels, hoppers, and furnaces show the full set of options including redstone settings and expiry. Traversal blocks such as doors, trapdoors, and fence gates show redstone settings but not expiry. Display blocks such as signs, lecterns, and beehives show only friends, name, and transfer. Workstations and other interactive blocks show the same reduced set.

### Villager Workstation Protection

A villager whose job-site memory points to a protected workstation block inherits that block's protection. Non-owners cannot damage or trade with the linked villager, and cannot break or interact with blocks in a configurable area around the workstation (default: 2 blocks horizontal, 1 block vertical). The protection menu for a linked workstation shows an Emerald button that starts a short particle effect on the villager's location, visible only to the player who clicked it, to help find which villager is linked. The horizontal and vertical search radius and the feature itself are independently configurable in `config.yml` under `villager_workstation_protection`.

### StatHandler Stability Fixes

`StatHandler.saveFile()` was completely rewritten for 1.3.3. The old code used an unconditional `Files.move(ATOMIC_MOVE)` which crashes on Windows and some Linux filesystems that do not support atomic cross-directory moves, causing `Failed to swap backup NBT file` spam every five minutes. The new implementation writes to a temp file first, validates it, copies it to the backup, then replaces the live file — with a fallback to a non-atomic copy-and-delete when atomic move is not available. A Caffeine cache also throttles the per-player stale-entry scan to at most once per 60 seconds per player. The chunk-load guard in `purgeStalePbsEntries` prevents the 20-second server freeze that occurred when that method triggered a synchronous chunk load on an unloaded chunk.

### Block Family Expression System

`blocks.yml` supports a compact family expression syntax alongside the standard flat material lists. Expressions are always parsed regardless of the `modern_family_blocks` flag — that flag only controls whether flat lists are auto-converted to expressions on startup.

```yaml
lockable_tile_entities:
  - "[*]"                          # all tile entities
  - "[*-CHEST]"                    # only chest variants
  - "[* -*SIGN]"                   # all tile entities except signs
  - "[*-FURNACE *-CHEST]"         # only furnaces and chests

lockable_shulker_boxes:
  - "[*-SHULKERS -WHITE_SHULKER_BOX]"   # all shulkers except white

lockable_entities:
  - "[*-CHEST_BOATS]"             # all chest boat variants
  - "[*-CHEST_MINECARTS *-HOPPER_MINECARTS]"
```

Full syntax reference: [`docs/BLOCK_FAMILY_SYNTAX.md`](docs/BLOCK_FAMILY_SYNTAX.md). Full block list: [`docs/LOCKABLE_BLOCKS_REFERENCE.md`](docs/LOCKABLE_BLOCKS_REFERENCE.md).

### Config and Blocks Auto-Merge

On every startup and on `/bp reload`, all config files are updated. Missing keys are added from JAR defaults, renamed keys are migrated, and new blocks are appended to `blocks.yml` in legacy flat-list mode. No existing values are overwritten.

### Per-World Configuration

Enable `per_worlds_config: true` in `config.yml` to activate `worlds.yml`, which lets you override lockable block lists per world and enable or disable protection entirely for individual worlds.

### Auto-Reload

The plugin watches its config files and reloads automatically when it detects a change. If you prefer to control reloads manually, set `auto_reload_configs: false` and use `/bp reload` instead.

### SQLite Usercache

The `blockprot_usercache.sqlite` file is now stored inside the plugin's data folder (`plugins/BlockProtReloaded/`) rather than next to `server.jar`. If an existing file is found at the old location on startup, it is moved automatically.

### Persistent Session Logging

One log file (`blockprot-current.log`) is shared across restarts. After 24 hours it is rotated to a dated archive and a new file is started.

### Hybrid MySQL / NBT Backend

NBT is the source of truth. MySQL or MariaDB can be enabled as an optional index for fast lookups, backed by a HikariCP connection pool. All SQL operations are asynchronous. Configured in `mysql/mysql.yml`.

### SQLite Access Audit Log

Stored at `mysql/blockprot_audit.sqlite`. Records access denied, access granted, opened, item taken, and item placed events. Writes are asynchronous and the log is automatically pruned at 50,000 entries. Accessible from the Clock button in the block lock menu. Owner access is never logged.

### Automatic Backup on Version Upgrade

A zip backup of the plugin data folder is created automatically whenever the plugin version changes. No backup is created on routine restarts.

### Inactivity Cleanup

Set `inactivity_cleanup_days` to a positive number to remove protections owned by long-inactive players on startup. Disabled by default.

### Security Options

| Option | Default | Description |
|---|---|---|
| `protect_locked_blocks_from_explosions` | true | Explosions cannot destroy locked blocks |
| `block_protected_block_piston_movement` | true | Pistons cannot move locked blocks |
| `allow_break_protected_blocks` | false | Any player can break protected blocks (protection is cleared on break) |
| `respect_spawn_protection` | true | Prevent locking blocks inside the spawn protection radius |
| `clear_protection_on_shulker_break` | false | Remove protection data when a shulker box is broken |

### WorldEdit Paste Auto-Lock

Automatically locks unprotected blocks near a WorldEdit paste origin. Disabled by default. Configurable radius and block limit per paste.

### Protection Expiry

Block owners can set an optional expiry date on their lock. When the timer elapses the block auto-unlocks. Open the Block Lock menu, click the Hopper slot, and type a duration such as `7d`, `1mo`, or `2d12h`. A green dye replaces the hopper when an expiry is already active — click it to clear. Disabled by default.

### Entity Protection (Tamed Animals)

Protects tamed animals including wolves, cats, parrots, horses, and llamas. Right-click your pet while holding the configured menu item (default: Stick) to open the settings GUI. Disabled by default. This feature was renamed from `pet_protection` to `entity_protection` in `config.yml`; the old key name is still read automatically for servers upgrading from an earlier version, so no manual edit is required.

### Colored Particle Effects and Sounds

Locking shows a green dust ring with a sound. Unlocking shows a red dust ring. Shulker boxes use their open and close sounds. Effects and sounds can be toggled independently.

### SkinsRestorer Support

Displays correct player head icons on offline-mode servers using the SkinsRestorer skin cache. Skins are pre-fetched asynchronously on login.

### MiniMessage and Adventure Color Support

Translation files accept both legacy color codes (`&a`, `§6`) and MiniMessage format (`<gold>`, `<gradient:...>`).

### PlaceholderAPI Integration

| Placeholder | Description |
|---|---|
| `%blockprot_global_block_count%` | Total blocks locked on the server |
| `%blockprot_own_block_count%` | Blocks locked by the current player |
| `%blockprot_default_friends%` | The player's default friend list |

### Folia Support

The plugin uses the FoliaLib scheduler and is compatible with Folia-based server forks.

### Legacy Folder Migration

On first boot after a plugin rename, BlockProt Reloaded automatically copies data from the old folder (`BlockProt` or `BlockProtPlus`) into the new one. Existing files are never overwritten. The source folder is left intact with a `.migrated` marker so the migration never runs twice.


## Block Coverage

All block lists are defined in `blocks.yml` and can be changed without restarting. Full details are in [`docs/LOCKABLE_BLOCKS_REFERENCE.md`](docs/LOCKABLE_BLOCKS_REFERENCE.md).

**Storage blocks:** all chest variants including all copper oxidation stages (1.21.9+), all 17 shulker box colors, furnace, smoker, blast furnace, hopper, dispenser, dropper, barrel, brewing stand, decorated pot, chiseled bookshelf, crafter, jukebox, lectern, beehive, bee nest, all 12 shelf variants (1.21.9+).

**Interactive blocks:** dragon egg, composter, bell, note block, all cauldron variants, all anvil damage stages, enchanting table, grindstone, stonecutter, loom, cartography table, smithing table.

**Doors, trapdoors, and fence gates:** all 12 wood variants for each, plus iron and all copper oxidation stages for doors and trapdoors.

**Entities (via sneak and right-click):** item frames, glowing item frames, all chest boat wood variants, chest minecart, hopper minecart.


## Configuration Reference

```yaml
# General
language_file: translations_en.yml
replace_translations: true
notify_op_of_updates: false
excluded_worlds: []
per_worlds_config: false           # enables worlds.yml (renamed from worlds_config_enabled)
inactivity_cleanup_days: -1        # -1 = disabled
auto_reload_configs: true          # set to false to disable the file watcher

# Block format
modern_family_blocks: false        # true = auto-convert flat lists to family expressions

# Player defaults
lock_on_place_by_default: true
public_is_friend_by_default: false
player_max_locked_block_count: -1  # -1 = unlimited
friend_search_similarity: 0.5
disable_friend_functionality: false
redstone_disallowed_by_default: false

# Safety
protect_locked_blocks_from_explosions: true
block_protected_block_piston_movement: true
clear_protection_on_shulker_break: false
allow_break_protected_blocks: false
respect_spawn_protection: true

# Owner notifications (server-wide defaults, overridable per player via /bp settings)
owner_notifications:
  enabled: true
  notify_on_open: true
  notify_on_take: true
  notify_on_place: true

# Pet protection (legacy key name, still read automatically)
entity_protection:
  enabled: false
  auto_protect_on_tame: true
  menu_item: STICK
  villager_locate_seconds: 6

# Villager workstation protection
villager_workstation_protection:
  enabled: true
  radius: 2            # horizontal search radius in blocks
  vertical_radius: 1   # vertical search radius in blocks

# Effects
block_lock_effects: true
block_lock_sounds: true

# Timed access
timed_access_max_duration_days: 90

# WorldEdit
worldedit_paste_autolock:
  enabled: false
  radius: 24
  max_blocks_per_paste: 5000
  delay_ticks: 20

# Menus
use_menus: false

# Protection expiry
enable_protection_expiry: false
expiry_scan_on_startup: true

# Logging and backups
enable_session_log: true
enable_backups: true
```

MySQL is configured separately in `mysql/mysql.yml`.


## Integrations

| Plugin | Notes |
|---|---|
| Towny | Respects town and nation permissions |
| WorldGuard | Honors region flags |
| Lands | Supports Lands claim permission checks |
| ClaimChunk | Prevents locking blocks in chunks the player does not own |
| PlaceholderAPI | Exposes stats and protection status as placeholders |
| SkinsRestorer | Correct player head icons on offline-mode servers |
| WorldEdit / FAWE | Optional paste auto-lock |
| Floodgate / Geyser | Bedrock player name resolution |
| ImageFrame | Item frame creator tag is read for automatic ownership |
| ViaVersion | Client version shown in `/bp lockables` info book |
| Folia | Asynchronous chunk handling support |


## Compatibility

| | |
|---|---|
| Minecraft | 1.21.1, 1.21.x, 26.1.x |
| Server software | Paper, Spigot, Folia |
| Java | 25+ required |
| MySQL | MySQL 8+, MariaDB 10.5+ (optional) |
| Languages | EN, ES, DE, FR, IT, PT-BR, RU, JA, KO, ZH-CN, ZH-TW, CS, SK, PL, TR |


## Translating

Language files are in `spigot/src/main/resources/lang/`. Both legacy color codes and MiniMessage format are accepted in all values. The English file `translations_en.yml` is the reference. Missing keys are added automatically on startup and on `/bp reload`. Pull requests for new or improved translations are welcome.


## Roadmap

| Issue | Description | Status |
|---|---|---|
| [#346](https://github.com/spnda/BlockProt/issues/346) | Clear protection when a shulker box is broken so gifted shulkers arrive unlocked | ✅ Implemented |
| [#345](https://github.com/spnda/BlockProt/issues/345) | Official Paper 26.1.x support | ✅ Implemented |
| [#344](https://github.com/spnda/BlockProt/issues/344) | `NbtApiException` spam on shulker box place | ✅ Fixed |
| [#343](https://github.com/spnda/BlockProt/issues/343) | 1.21.11 support | ✅ Implemented |
| [#334](https://github.com/spnda/BlockProt/issues/334) | Configurable message colors via MiniMessage | ✅ Implemented |
| [#329](https://github.com/spnda/BlockProt/issues/329) | `RuntimeException` on AIR block in `EntityChangeBlockEvent` | ✅ Fixed |
| [#324](https://github.com/spnda/BlockProt/issues/324) | Allow breaking protected blocks for reinforcement-plugin compatibility | ✅ Implemented |
| [#318](https://github.com/spnda/BlockProt/issues/318) | Per-world lockable block configuration | ✅ Implemented |
| [#306](https://github.com/spnda/BlockProt/issues/306) | Server lag caused by `HopperEventListener` | ✅ Fixed via Caffeine cache and `ProtectedBlockCache` |
| [#303](https://github.com/spnda/BlockProt/issues/303) | Respect spawn-protection radius | ✅ Implemented |
| [#298](https://github.com/spnda/BlockProt/issues/298) | ClaimChunk integration | ✅ Implemented |
| [#295](https://github.com/spnda/BlockProt/issues/295) | Lock trapdoors and iron doors | ✅ Implemented |
| [#282](https://github.com/spnda/BlockProt/issues/282) | MySQL support | ✅ Implemented |
| — | Filter `/bp lockables` material list by client protocol version when ViaVersion is active | Pending |


## Documentation policy from 1.3.4 onward

1.3.3 is the last release where this README is updated feature-by-feature in full detail. Starting with 1.3.4, this README will be condensed into a short summary of all current features, and `docs/RELEASE_NOTES.md` becomes the single place where what changed between versions is recorded in detail. This keeps documentation effort proportional to development time as the plugin grows. Check `docs/RELEASE_NOTES.md` for the complete version-by-version history going forward.


## Contact and Support

Maintained by **Zar**. [Open an issue](https://github.com/VictorGugug/BlockProt-Reloaded/issues) for bugs or feature suggestions.


## License

Licensed under the **GNU General Public License v3**. See [`LICENSE`](LICENSE) for details.

<sub>Based on <a href="https://github.com/spnda/BlockProt">BlockProt</a> by spnda. Original copyright notices preserved as required by GPL v3.</sub>
