# BlockProt — Plus

**Fork of [BlockProt](https://github.com/spnda/BlockProt) created and maintained by [Zar](https://github.com/VictorGugug).**

Players lock chests, furnaces, and other blocks through a modern GUI — no commands to memorize.
This fork extends the original NBT core with production-grade features for large or long-running servers.

![](https://raw.githubusercontent.com/VictorGugug/BlockProt-Plus/main/images/main_menu.png)
![](https://raw.githubusercontent.com/VictorGugug/BlockProt-Plus/main/images/friend_settings.png)
![](https://raw.githubusercontent.com/VictorGugug/BlockProt-Plus/main/images/user_settings.png)
![](https://raw.githubusercontent.com/VictorGugug/BlockProt-Plus/main/images/redstone_settings.png)

---

## Requirements

- Java 25+
- Paper or Spigot 1.21 / 1.21.1 / 1.21.4 / 26.x

---

## What's added vs. the original BlockProt

All additions are **disabled by default**. The upstream NBT core is untouched unless you opt in.

**Java 25 / Paper 26.x compatibility** — detects both `1.x` and year-based `26.x` versioning at runtime; startup compatibility checks logged to console and a session log file.

**Persistent session logging** — one timestamped log file per session under `plugins/BlockProt/logs/`.

**Hybrid MySQL / NBT backend** *(optional)* — HikariCP connection pool, async queries, global trust table, in-memory cache. Disabled by default.

**Cached profile service** — in-memory cache for Mojang profile lookups; eliminates repeated HTTP calls during the same session.

**Master friend list & `/bp friends addall`** — adds a player to every block you own at once; resolves offline players via Mojang API; action bar confirms how many blocks were updated.

**SQLite access audit log** — records `ACCESS_DENIED` / `ACCESS_GRANTED` events with UUID, name, location, and timestamp; in-game GUI with player-head grouping; auto-pruning at 50 000 rows.

![](https://raw.githubusercontent.com/VictorGugug/BlockProt-Plus/main/images/audit-log.png)

**Automatic backup** — ZIP backup of plugin data before startup migration or `/bp reload`; keeps the last 10 backups.

**Inactivity cleanup** *(optional)* — removes protections from players inactive for a configurable number of days.

**Per-world configuration** (`worlds.yml`) — each world can override lockable block lists or disable protection entirely; missing worlds added automatically.

**Config file watcher** — auto-reloads `config.yml`, `worlds.yml`, and lang files on disk change (2 s debounce).

**Hardened security options** — explosion protection, piston blocking, spawn-protection respect, shulker-break NBT clearing.

**`/bp help`** — lists all subcommands with short descriptions; aliased as `/bp ayuda`.

**WorldEdit / FAWE paste auto-lock** *(optional)* — scans a radius after `//paste`, locks unprotected blocks, applies your default friend list. Capped at `max_blocks_per_paste`.

**Floodgate / Geyser support** — resolves Bedrock player names with configurable username prefixes.

**Self-repair / config key merging** — missing config and lang keys are added from the JAR bundle on every startup; no existing values are overwritten.

**ClaimChunk integration** — prevents locking in chunks you don't own; optional chunk-owner-only access for unprotected containers.

**`BlockProtLockEvent` & `BlockProtUnlockEvent`** — cancellable Bukkit events with `Cause` enum for addon developers.

**Shulker box fix** — guards against `NbtApiException` (#344) on shulker place/break.

**Update checker** — queries GitHub Releases API; clickable in-game message for admins when a new version is available.

---

## Integrations

Towny, WorldGuard, PlaceholderAPI, Lands, ClaimChunk, Floodgate / Geyser, WorldEdit / FAWE.

---

## Commands

| Command | Description |
|---|---|
| `/bp help` | List all subcommands |
| `/bp settings` | Open player settings GUI |
| `/bp friends` | Open global friend list GUI |
| `/bp friends addall <player>` | Add player to every block you own |
| `/bp stats` | View your protection statistics |
| `/bp about` | Plugin info and version |
| `/bp reload` | Reload config (backup runs first) |
| `/bp integrations` | Show active integrations |
| `/bp disablehints` | Disable new-player hints |
| `/bp update` | Check for updates manually |

Alias: `/blockprot`. Spanish aliases available when `localized_command_aliases: true`.

---

## Full feature documentation

The complete feature reference, all configuration options, permissions, and the full list of
added files and dependencies are maintained in the
**[GitHub README](https://github.com/VictorGugug/BlockProt-Plus#readme)**.
That is the authoritative source — updated every time something new is added.

---

## Source & issues

[github.com/VictorGugug/BlockProt-Plus](https://github.com/VictorGugug/BlockProt-Plus)

Licensed under **GPL v3**. Based on [BlockProt](https://github.com/spnda/BlockProt) by spnda.
