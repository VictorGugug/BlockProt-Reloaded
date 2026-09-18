# Project Scope

This is the authoritative description of what BlockProt Reloaded is meant to be. It exists so contributors, forks, and users can judge for themselves whether a proposed feature, fix, or pull request fits within the project scope, per [CONTRIBUTING.md](CONTRIBUTING.md) and [LICENSE](LICENSE). This document records what has been decided; it is updated as the project evolves.

## What the Project Is

BlockProt Reloaded is an advanced block and entity protection plugin for Minecraft Java Edition servers running Bukkit-compatible server software, primarily optimized for the Paper server family. It is a fork of [spnda/BlockProt](https://github.com/spnda/BlockProt) (GPL-3.0), maintained as a solo project by Zar (Vic / VictorGugug).

The plugin provides granular protection for containers, workstations, doors, signs, and entities; access control through friend lists, manager roles, and timed access; owner notifications; world expiry and inactivity cleanup; an SQLite audit log; and administrator diagnostic tools.

## Versioning Model and Platform Targets

### Plugin Versioning vs. Minecraft Server Versioning

To ensure clarity across all development discussions, two distinct version layers must be distinguished:

- **Plugin Version**: Represents the release version of BlockProt Reloaded itself (for example, `1.3.6`, `1.3.7`), defined authoritatively in `gradle.properties` alongside the declared Minecraft server version targets for the active cycle, and governed by Semantic Versioning (`MAJOR.MINOR.PATCH`).
- **Minecraft Server Version**: Represents the upstream game edition and server software baseline (for example, Minecraft `1.21.1`, Minecraft `26.2`, Minecraft `26.3`) provided by Paper or compatible forks.

### Minecraft Version Support Policy

- **Targeted Minecraft Baselines**: Each plugin release milestone declares and certifies a specific range of supported Minecraft server versions, documented directly in `gradle.properties` to ensure transparent alignment and eliminate version ambiguity.
- **Mid-Cycle Upstream Releases**: When a new Minecraft update is released while a plugin version is actively in development, compatibility work for that newly released Minecraft version is not rushed into the in-flight plugin milestone. The active plugin release concludes against its declared baseline, and official compatibility for the new Minecraft release is scheduled for the subsequent plugin release cycle.
- **No Unplanned Compatibility Guarantees**: BlockProt Reloaded does not guarantee full functionality on Minecraft versions released mid-cycle that were not formally accounted for or supported in the active plugin release.
- **Legacy Version Separation**: Minecraft server versions requiring legacy runtimes (such as Java 17 and pre-component item NBT handling) are isolated in the dedicated `BlockProt-Legacy` branch rather than compromising the modern Java 21 LTS baseline on `main`.

### Runtime and Build Toolchain

- **Minimum Java runtime**: Java 21 LTS (enforced by Java 21 bytecode target).
- **Build toolchain**: JDK 25.
- **Compilation target**: Pinned to an established Paper API baseline so that every compiled method exists across all supported servers. Newer platform APIs are accessed safely through runtime feature detection (`VersionCompat`) or reflection bridges rather than direct compilation.

### Server Software Support

- **Fully Supported**: Paper, Purpur, Pufferfish, Folia (region-aware asynchronous scheduling via FoliaLib), Leaf, Leaves, Gale.
- **Phasing Out**: Plain Spigot (core features only; modern Paper APIs are preferred, and Spigot-specific support is phased out).
- **Hard-Blocked**: Plain CraftBukkit (the plugin detects CraftBukkit during startup and safely refuses to enable).
- **Out of Scope**: Hybrid server software (such as Mohist, Magma, CatServer, or Arclight). Hybrid Forge and Fabric implementations introduce interaction anomalies and non-standard event behavior that the plugin does not support.

## Core Architecture and Interface Direction

### Dual Menu Presentation

BlockProt Reloaded provides two concurrent menu presentations:

1. Traditional chest inventory GUIs.
2. Native Paper Dialogs (`use_dialogs: true`), styled with a consistent pastel color palette (`PASTEL_MINT`, `PASTEL_CORAL`, `PASTEL_GOLD`, `SOFT_BLUE`, `PASTEL_PURPLE`).

The dialog system is a modern alternative presentation, not a replacement. Both systems exist side by side, allowing server administrators and individual players to choose their preferred presentation. Neither system deprecates the other.

Additionally, native Bedrock Edition forms are supported for players joining through Geyser and Floodgate via the Cumulus API.

### Storage Architecture

- **File-Based NBT**: The primary source of truth. Each protected block stores its data in per-world NBT records.
- **Player PDC**: Stored under the PersistentDataContainer hierarchy `BukkitValues/blockprot/` (`preferences`, `friends`, `history`, `admin`).
- **SQLite**: Used for the per-block audit log (`mysql/blockprot_audit.sqlite`) and offline player profile cache (`blockprot_usercache.sqlite`).
- **MySQL / MariaDB**: Optional high-performance index for large servers, managed through HikariCP.
- **Configuration Preservation**: Plugin updates must never overwrite existing administrator configurations on disk (`config.yml`, `blocks.yml`, `worlds.yml`). Only missing default keys are merged into existing files.

### Internationalization

- All player-facing strings must be localized through the `TranslationKey` enum and `Translator.get()`. No hardcoded visible text is permitted in code.
- English (`translations_en.yml`) and Spanish (`translations_es.yml`) are the two reference translation files maintained directly. All other bundled languages fall back to English automatically for missing keys.

## License and Governance

- Licensed under the GNU General Public License v3.0 (GPL-3.0).
- Contributions follow the process documented in [CONTRIBUTING.md](CONTRIBUTING.md).
- For contributions prepared with AI assistance, the binding rules are summarized in [AGENTS.md](AGENTS.md).

## Out of Scope

The following areas are explicitly outside the scope of BlockProt Reloaded:

- Standalone Fabric, Forge, or NeoForge mod implementations (this project is strictly a Bukkit/Paper-family plugin).
- Direct compilation against unstable or unreleased development APIs that break compatibility with the declared Paper baseline.
- Overwriting existing user configuration files during plugin version updates.
- Hardcoded player-facing messages or non-localizable UI elements.
- Broad, unsolicited architectural refactors that do not address a specific verified bug or targeted feature.
- Premature or speculative compatibility patches for upstream Minecraft releases not yet targeted by the current plugin milestone.
