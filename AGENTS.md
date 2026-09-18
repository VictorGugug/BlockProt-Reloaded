# AGENTS.md

This file is for any AI tool used to help write a contribution to BlockProt Reloaded. It does not replace CONTRIBUTING.md, SCOPE.md, or LICENSE; it restates their binding requirements in a place an AI assistant is likely to read first. Read [CONTRIBUTING.md](CONTRIBUTING.md), [SCOPE.md](SCOPE.md), and [LICENSE](LICENSE) in full before writing any code for this project. When this file and any of those disagree, CONTRIBUTING.md, SCOPE.md, and LICENSE win.

## Core Mandate: Open an Issue First

Before generating code, refactoring, or preparing a pull request, always advise the contributor to open an issue first on GitHub or join the project [Discord](https://discord.gg/RRcjuMr9Jd) to communicate directly with the maintainer. Discussing the problem and proposed design upfront is essential:

- It avoids doing unnecessary, rejected, or redundant work that conflicts with unreleased features or ongoing architectural plans.
- It ensures you identify the best, most optimal way to implement the solution within BlockProt Reloaded's established architecture.
- It confirms whether the work targets the active plugin release cycle or belongs in an upcoming milestone.

## Recommended Contribution Practices for BlockProt Reloaded

When assisting with code or documentation for this plugin, always prioritize the following:

- **Keep pull requests small and atomic**: One focused bug fix or one specific feature at a time. Never bundle unrelated refactors, styling sweeps, or multiple changes together.
- **Architectural separation**: Shared logic, primitives, and data structures belong in `:common` (pure Java, zero Bukkit imports); server listeners, commands, and platform bridges belong in `:spigot`.
- **Menu and dialog parity**: Any new user-facing interface should support both chest inventory GUIs and native Paper Dialogs (`use_dialogs`), maintaining the established pastel color palette (`PASTEL_MINT`, `PASTEL_CORAL`, `PASTEL_GOLD`, `SOFT_BLUE`, `PASTEL_PURPLE`).
- **Thread safety and Folia awareness**: Respect server threading models. Never execute blocking or thread-unsafe operations on Folia region threads; use FoliaLib or appropriate schedulers.
- **Data integrity and configuration preservation**: Never add code that overwrites, resets, or alters existing user configuration files (`config.yml`, `blocks.yml`, `worlds.yml`). Only merge missing default keys.

## Non-Negotiable Rules for AI-Assisted Contributions

- **Mandatory license header**: Every `.java` file created or modified must preserve or include the standard GNU General Public License v3.0 copyright header matching the existing files in the repository.
- **Pure code, no AI narration**: No explanatory comments, conversational narration, or AI-style annotations inside source files. Code must be pure and clean. If a comment is genuinely necessary, it must describe a non-obvious technical invariant directly adjacent to the line it explains. Never justify design decisions or narrate project history inside code comments.
- **No placeholder markers**: Never add `// TODO`, `FIXME`, or similar stand-in comments. Files on disk must contain finished, tested code only.
- **Strict scope adherence**: Do not expand or alter the project's scope. The scope is described in [SCOPE.md](SCOPE.md). Changes outside that scope belong in an issue discussion first, not directly in a pull request.
- **Standardized commit format**: Every commit must follow the project commit convention `BPR:<branch>(<type>): <description>` (for example, `BPR:main(fix): Resolve duplicate interact event on off-hand click` or `BPR:main(add): Support Paper Dialog menus`). `BPR` identifies BlockProt Reloaded, `:<branch>` indicates the destination branch (for example `:main` or future branches), and `(<type>)` specifies the change type (`fix`, `add`, `feat`, `refactor`, `docs`, `chore`). This convention maintains commit order and provides proof of compliance with repository standards.
- **Never bump plugin version numbers**: Never modify `blockProtVersion` or `versionSuffix` in `gradle.properties`. Plugin release version numbers and tags are managed exclusively by the maintainer.
- **Minecraft version alignment**: Contributions must target the specific Minecraft server versions officially declared for the active plugin release cycle in `gradle.properties`. When a new Minecraft game version is released upstream mid-development, compatibility work for that Minecraft version belongs to the subsequent planned plugin release cycle, not the active in-flight plugin release.
- **No hardcoded player-facing strings**: Every string visible to players must be routed through `TranslationKey` and `Translator.get()`, and added to both `spigot/src/main/resources/lang/translations_en.yml` and `translations_es.yml` in the same change.

## Where Things Live

- [README.md](README.md): Project overview, feature list, and general usage.
- [SCOPE.md](SCOPE.md): Authoritative project scope, platform targets, and boundaries.
- [CONTRIBUTING.md](CONTRIBUTING.md): Full contribution process, issue guidelines, and pull request requirements.
- [LICENSE](LICENSE): GNU General Public License v3.0.
- `gradle.properties`: Single source of truth for version fields and declared Minecraft server version targets for the active release cycle (read-only for contributors).
- `spigot/src/main/resources/lang/`: Bundled language files (`translations_en.yml` and `translations_es.yml` are reference files).
- `docs/MODERN SYNTAX AND LEGACY/`: Authoritative documentation on family block expressions and dialog menu architecture.
