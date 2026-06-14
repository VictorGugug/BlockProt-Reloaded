# BlockProt Reloaded (Modrinth & Hangar Description)

BlockProt Reloaded is a fork of BlockProt by spnda, maintained by Zar. It provides simple yet extremely powerful block and entity protection for Spigot/Paper Minecraft servers (Minecraft 1.20–1.26.x).

Players can lock chests, furnaces, shulker boxes, doors, and other blocks through a clean, intuitive GUI — no commands to memorize. This fork extends the original NBT-based core with production-ready features optimized for large or long-running servers.

---

## Key Features

- **No Commands to Memorize:** Sneak + right-click any lockable block or entity to open the lock GUI.
- **Access Control levels:** Add friends with customized permissions (Read, Write, Manager).
- **Access Audit Log:** A persistent SQLite-backed log recording `OPENED`, `ITEM_TAKEN`, `ITEM_PLACED`, `ACCESS_DENIED`, and `RAID_EXPLOSION` events with clean GUI visualization.
- **Admin Command Tools:**
  - `/bp unlock <player>` — lists every protected container owned by the player, permitting inspections and deletions.
  - `/bp info <player>` — view real-time coordinates, block types, lock timestamps, and click-to-teleport logs.
- **Entity & Vehicle Protection:** Protects chest boats, minecarts (storage/hopper), and item frames.
- **Workstation & Villager Protection:** Protects linked villagers and blocks in a 2x2 horizontal and 1 vertical block radius around locked workstations. Locate linked villagers instantly with a particle tracker tool.
- **Expiry Toggles:** Configure protections to automatically expire and unlock after a set duration.
- **Discord & Action-Bar Alerts:** Action-bar notifications for block opens and Discord webhook integration for real-time raid alerts.

## Integrations

Native support for major server plugins:
- Towny
- WorldGuard
- PlaceholderAPI
- Lands
- ClaimChunk
- SkinsRestorer
- WorldEdit / FAWE
- Floodgate / Geyser
- Folia
