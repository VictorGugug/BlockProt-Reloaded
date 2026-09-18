# Contributing to BlockProt Reloaded

## Before Contributing

Read [README.md](README.md), [SCOPE.md](SCOPE.md), and [LICENSE](LICENSE) before opening an issue or pull request. Contributions must remain consistent with the project scope, roadmap, and license.

If you are using an AI assistant to help write, review, or research a contribution, read [AGENTS.md](AGENTS.md) in full before writing any code or opening a pull request.

A pull request must demonstrate that its author has read these documents and has a working understanding of the repository structure and release targets. A pull request that is clearly disconnected from how the plugin actually works or ignores the active release cycle will be sent back for revision or closed.

## Understanding Version Terminology

To avoid confusion, note the clear distinction between two separate version concepts:

- **Plugin Version**: The release number of BlockProt Reloaded itself (for example, `1.3.6`, `1.3.7`), defined in `gradle.properties` and structured under Semantic Versioning (`MAJOR.MINOR.PATCH`).
- **Minecraft Version**: The upstream game and server release (for example, Minecraft `1.21.1`, Minecraft `26.2`, Minecraft `26.3`) running on Paper or compatible server software.

Each **plugin version** targets, tests, and officially certifies a declared range of **Minecraft versions**. To eliminate ambiguity and avoid mid-cycle scope misalignment, the targeted Minecraft server versions for the active plugin release cycle are explicitly documented as reference metadata directly in `gradle.properties`.

## Issues First (Required Before Contributing)

Before writing any code or opening a pull request, open an issue first to discuss the problem, proposal, or bug report. You can also join the project [Discord](https://discord.gg/RRcjuMr9Jd) to communicate directly with me before submitting an issue or pull request.

Both human contributors and AI assistants must follow this workflow. Opening an issue first is essential for several key reasons:

- **Prevents unnecessary or discarded work**: It ensures you do not spend time implementing something that is out of scope, already being refactored, or incompatible with the active plugin roadmap.
- **Finds the optimal technical approach**: Discussing the solution beforehand allows evaluating the best implementation strategy together (such as selecting the proper event priority, ensuring Folia thread safety, avoiding performance overhead, or maintaining clean API boundaries).
- **Coordinates release cycle alignment**: It confirms whether the proposed change targets the active plugin release cycle or belongs in an upcoming milestone.

Pull requests submitted without a prior issue or discussion may be placed on hold while an issue is opened to evaluate the design and ensure the approach is optimal.

## Pull Request Guidelines

Pull requests are the required path for submitting code changes that fit within the intended scope of BlockProt Reloaded.

### Scope and Size

- Keep pull requests small and focused: one specific bug fix or one specific feature at a time. Do not bundle unrelated refactors, formatting changes, or multiple features into a single pull request. A pull request that touches many unrelated areas is difficult to review and test, and will be requested to be split apart.
- Refactoring existing code should only occur when strictly necessary for the fix or feature being introduced. Avoid broad stylistic rewrites of code that is already functioning correctly.

### Branching and Commit History

- Create a feature branch from the latest `main` branch with a clear, descriptive name (for example, `fix/interaction-offhand-handling` or `feat/custom-flag`).
- Use the standardized commit message format:
  `BPR:<branch>(<type>): <summary>`
  - `BPR`: Prefix identifying BlockProt Reloaded.
  - `:<branch>`: The destination branch targeted by the commit (for example, `:main` for the primary branch, or another target branch in the future).
  - `(<type>)`: The category of change:
    - `fix`: Bug fixes and behavioral corrections.
    - `add`: New capabilities, features, or assets.
    - `refactor`: Internal code adjustments without changing external behavior.
    - `docs`: Documentation and specification updates.
    - `chore`: Tooling, build scripts, or maintenance tasks.
  - Examples:
    - `BPR:main(fix): Resolve duplicate interact event on off-hand click`
    - `BPR:main(add): Introduce protectable entities configuration in blocks.yml`
    - `BPR:main(docs): Add contributing guidelines and agent specifications`
- Keep commit history clean and linear. Avoid unnecessary merge commits in your pull request branch; rebase on current `main` when updates are needed.

### Description and Context

Include a structured, factual description in your pull request:

- **Problem**: Describe the issue being resolved, including reproduction steps and observed behavior.
- **Root Cause**: Explain why the issue occurred at the technical level (events fired, state mismatch, API behavior).
- **Proposed Solution**: Explain how your change resolves the problem and why this approach was chosen.
- **Testing and Verification**: State the exact server software, build version, Java runtime, and test scenarios executed.

## Development Cycle and Platform Alignment

BlockProt Reloaded coordinates its release cycles around declared Minecraft version milestones:

- Each plugin release cycle targets an explicitly declared set of supported Minecraft server versions, documented authoritatively in `gradle.properties`.
- When an intermediate or major Minecraft version is released in the middle of an active plugin development cycle (for example, while plugin version X is in progress targeting Minecraft version A, and Minecraft version B is released upstream), compatibility work for Minecraft version B is not rushed into plugin version X. Official compatibility for Minecraft version B is scheduled and developed in the subsequent plugin release cycle (plugin version X+1).
- Pull requests that introduce compatibility for a newly released Minecraft version not officially targeted in the active plugin development cycle will be placed in draft status. They will be reviewed and scheduled once the active plugin release finishes its cycle and the next plugin milestone begins.
- BlockProt Reloaded does not guarantee full functionality on Minecraft versions released mid-cycle that were not accounted for or officially targeted in the active plugin release.
- Server software targets are restricted to the Paper family (Paper, Purpur, Pufferfish, Folia). Pull requests introducing support for plain CraftBukkit or hybrid Forge/Fabric/Bukkit servers (such as Mohist, Magma, or CatServer) are out of scope.

## Testing and Verification Requirements

Every pull request must be verified on a live server environment before submission:

- Verify both positive cases (the intended behavior works correctly) and negative cases (permissions are enforced, unauthorized players cannot interact or bypass protections, and events do not fire duplicate actions).
- Verify edge cases: main-hand versus off-hand interactions, sneaking states, player inventory full, and block placement restrictions.
- Test across Paper and region-aware Folia environments where applicable. Ensure no blocking calls are made on Folia region threads.

## Code Standards and Architecture

- **Java baseline**: Java 21 LTS bytecode, built with the JDK 25 toolchain.
- **Configuration preservation**: Configuration files (`config.yml`, `blocks.yml`, `worlds.yml`) must never be overwritten on plugin update. Code must only merge missing default keys, preserving existing administrator settings.
- **Menu and dialog parity**: Where applicable, user-facing interfaces should support both traditional inventory menus and native Paper Dialogs (`use_dialogs`), maintaining the pastel color palette used throughout the plugin.
- **Thread safety**: Respect server thread models. Asynchronous tasks and region threads must not access Bukkit world state without proper scheduling.
- **Multi-module separation**: Core primitives and shared data structures belong in `:common` without Bukkit dependencies; server-specific listeners and commands belong in `:spigot`.

## Use of AI

Using AI tools to assist in writing contributions is permitted, provided that every one of the following requirements is strictly followed:

- The resulting code must be thoroughly reviewed, verified, and tested by the human author before submission. Submitting raw, unreviewed, or untested AI output is prohibited.
- The code must be clean and pure: no AI-generated explanatory comments, no conversational narration, and no inline justification of why a decision was made. Source code contains only code. If a comment is genuinely necessary, it must describe a non-obvious technical invariant or safety boundary directly adjacent to the line it explains.
- Do not add stand-in placeholder comments such as `// TODO` or `FIXME`.
- Every new Java source file must include the standard GPL-3.0 copyright header matching the existing files in the repository.
- Changes must strictly respect [SCOPE.md](SCOPE.md). AI tools must not be allowed to introduce speculative abstractions, unrequested dependencies, or scope expansions.
- Never modify plugin version numbers or version suffixes in `gradle.properties`. Plugin release versioning is managed exclusively by the repository maintainer.
- Every commit created with or by an AI assistant must follow the standardized commit format `BPR:<branch>(<type>): <description>`.
- No hardcoded player-facing strings: every message, button label, or notification visible to players must use the `TranslationKey` enum and `Translator.get()`, and must be added to both `spigot/src/main/resources/lang/translations_en.yml` and `translations_es.yml` in the same change.

## Pull Request Lifecycle

1. **Submission**: Open a pull request with the required factual description. If the work is still in progress or awaits an upcoming plugin release cycle, mark it as a draft.
2. **Review**: The pull request will be reviewed for scope alignment, code cleanliness, test evidence, and adherence to project invariants.
3. **Revisions**: If changes or tests are requested, push additional commits to the same branch or rebase as appropriate.
4. **Merge**: Once approved and aligned with the plugin release cycle, the pull request will be merged into `main`.

## Communication

I am a native Spanish speaker and welcome issues, pull request discussions, and questions in Spanish or English. Clear, direct, and respectful communication is appreciated.

Contributors are warmly invited to join the project [Discord](https://discord.gg/RRcjuMr9Jd) for closer coordination, architectural discussions, and direct communication with me.
