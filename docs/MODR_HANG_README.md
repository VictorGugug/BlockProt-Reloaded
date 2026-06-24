# BlockProt Reloaded - Modrinth & Hangar description

BlockProt Reloaded is a maintained fork of BlockProt for Spigot/Paper servers (supported Minecraft: 1.21.1 → 26.1.2). It preserves the original NBT-based core while adding features aimed at large, long-running servers.

Key highlights
- Protect blocks and entities via a simple in-game interaction: sneak + right-click to open the lock GUI.
- Protect tile entities (chests, furnaces, shulker boxes, shelves, lecterns, etc.), interactive blocks (doors, trapdoors, anvils), storage entities (chest boats, chest minecarts, hopper minecarts) and item frames.
- Fine-grained access control: per-owner friends/roles (Read, Write, Manager) and a persistent audit trail for `OPENED`, `ITEM_TAKEN`, `ITEM_PLACED`, `ACCESS_DENIED`, and `RAID_EXPLOSION` events.
- Administrative tools: `/bp lockables`, `/bp unlock`, `/bp info`, `/bp reload`, `/bp debug run`, and other server-side utilities.
- Workstation & villager protection: protect linked villagers and their workstation area; includes a "Locate linked villager" particle tool.
- Family expression system for `blocks.yml` and `worlds.yml` (see `BLOCK_FAMILY_SYNTAX.md`) - compact tokens, sub-families, and full expression support.
- Config/blocks merge on reload, SQLite usercache migration, safe NBT writes, chunk-load guards, and purge throttling to avoid main-thread stalls.

Compatibility & support
- Compile target: `paper-api:1.21.1` (plugin.yml: `api-version: '1.21.1'`). Verified to run on Paper and Purpur builds within supported ranges.
- Support tiers: primary focus on 26.1.x and 1.21.1+; legacy 1.20.x and below are no longer supported.

Install & upgrade notes
- Place the plugin JAR in `plugins/` and restart the server.
- On upgrade, configuration keys may be migrated automatically (e.g., `worlds_config_enabled` → `per_worlds_config`).
- The SQLite user cache is moved into the plugin data folder on first run; legacy files are migrated automatically.

Commands (overview)
- `/bp lockables` - admin GUI listing all family members and active/inactive status.
- `/bp unlock <player>` - list and remove protections owned by a player.
- `/bp info <player>` - show owner info, protected locations, and quick teleport links.
- `/bp reload` - reloads config, merges missing keys, and rescans world configs.
- `/bp debug run` - runs internal diagnostic checks (lockable entities, NBT read/write, integrations, raid config, Via details).

Configuration highlights
- `lockable_entities` - empty by default; add entity types or sub-family tokens to enable entity protection.
- `entity_protection` - replaces the old `pet_protection` key (backwards compatible aliases remain).
- `auto_reload_configs` - default `true`; set to `false` to disable automatic reloads.
- `per_worlds_config` - per-world configuration control (auto-migrated from `worlds_config_enabled`).

Integrations
- Towny, WorldGuard, PlaceholderAPI, Folia, WorldEdit/FAWE, Floodgate/Geyser, and others are supported where applicable.

Developer notes
- StatHandler uses temp-write → validate → backup → replace to avoid atomic-move failures on Windows.
- `BlockFamilyParser` introduced for expression parsing; `lockable_entities` load supports family expressions and inactive tracking.
- Raid detection listener logs explosions near lockable blocks and notifies owners (online or queued for delivery at next login).

Further reading
- Full release notes: `RELEASE_NOTES.md`
- Block family syntax: `BLOCK_FAMILY_SYNTAX.md`
- Lockable materials reference: `LOCKABLE_BLOCKS_REFERENCE.md`

Report issues and contribute
- Repository: https://github.com/VictorGugug/BlockProt-Reloaded
- Prefer GitHub issues for bug reports and feature requests.

---

Contact the project on GitHub for maintainers' details and direct support.
