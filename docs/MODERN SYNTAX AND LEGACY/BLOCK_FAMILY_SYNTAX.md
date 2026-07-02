![BLOCK FAMILY SYNTAX](https://raw.githubusercontent.com/VictorGugug/BlockProt-Reloaded/main/images/RELEASE%20TITLES/docs/BLOCK%20FAMILY%20SYNTAX.png)

Family expressions are a compact syntax for `blocks.yml`, `worlds.yml`, and
`auto_drop_to_inventory`. They are **always parsed** regardless of the `modern_family_blocks`
flag in `config.yml`.

`modern_family_blocks` controls startup auto-conversion in both directions:
- `true`  -> flat material lists are automatically converted to compact family expressions.
- `false` -> any existing family expressions in the file are automatically converted to flat lists.
It does not gate expression parsing (expressions are always parsed regardless of this flag).

Flipping this flag runs a synchronous, forced sweep across every list key in `blocks.yml`
(all 5 lockable lists plus `auto_drop_to_inventory.blocks`), not just the keys a GUI toggle
happens to touch. A key that is empty or placeholder-only is still normalized, so the whole
file always matches the current mode immediately after startup.

They replace flat material lists with token-based expressions that resolve dynamically
against the full family registry at startup and on `/bp reload`.

## Compression

When a key is saved in modern format, the plugin picks the shortest valid representation
per sub-family independently: direct inclusion (`CHEST BARREL`) if the selected members are
the minority, or exclusion (`*-CHEST -COPPER_CHEST`) if they are the majority. Selecting a
single block from a large sub-family no longer produces a long exclusion list for the whole
family; only that one sub-family's members are ever weighed against each other.

## Empty lists

An empty key is never written as a real empty YAML list (`[]`). In legacy (flat) mode it
is saved as two blank template lines (`-` / `-`), inviting the user to type material names
directly in their place. In modern mode it is saved as the single-element list
`- '[]'`, which is the only representation SnakeYAML round-trips safely for "nothing selected"
without corrupting the rest of the file's structure on the next save.

---

## Two valid formats - do not mix them on one line

Every list key (`lockable_tile_entities`, `lockable_blocks`, `lockable_entities`, etc.)
accepts a normal YAML list. Each line in that list is either:

1. **A plain material name** - one block/entity per line, exactly like every other
   list in `blocks.yml`:
   ```yaml
   lockable_entities:
     - ITEM_FRAME
     - ACACIA_CHEST_BOAT
   ```
2. **A family expression** - a single string wrapped in `[...]`, quoted, that can
   stand for many materials at once:
   ```yaml
   lockable_entities:
     - '[*-ITEM_FRAMES *-CHEST_BOATS]'
   ```

You can mix plain names and expressions across different lines of the same list.
What you **cannot** do is put more than one plain name inside `[...]` without commas -
that is not YAML list syntax, it is the family-expression bracket, and it only
understands the tokens below (`*`, `*-TAG`, `-*TAG`, `NAME`, `-NAME`).

```yaml
# WRONG - square brackets are read as ONE family expression, not a list of two names.
# "ITEM_FRAME" and "ACACIA_CHEST_BOAT" are not valid tokens, so this resolves to nothing
# and silently enables zero entities. No warning is logged for this case.
lockable_entities: [ITEM_FRAME ACACIA_CHEST_BOAT]

# RIGHT - plain list, one entry per line
lockable_entities:
  - ITEM_FRAME
  - ACACIA_CHEST_BOAT

# ALSO RIGHT - YAML flow-list with a comma (now two real list entries)
lockable_entities: [ITEM_FRAME, ACACIA_CHEST_BOAT]

# ALSO RIGHT - family expression, quoted, as the list's only entry
lockable_entities:
  - '[*-ITEM_FRAMES *-CHEST_BOATS]'
```

If you only want one or two specific materials, the plain list format is simpler and
clearer. Reach for a family expression when you want "all of a sub-family" or
"everything except a few" - see the token reference below.

---

## Expression tokens

All expressions are wrapped in `[...]` and contain space-separated tokens.

| Token    | Operation                                                           |
|----------|---------------------------------------------------------------------|
| `*`      | Include all members of the current top-level family                 |
| `*-TAG`  | Include all members of the named sub-family                         |
| `-*TAG`  | Exclude all members of the named sub-family from the result         |
| `NAME`   | Include a specific material (must belong to this family)            |
| `-NAME`  | Exclude a specific material (must belong to this family)            |

**Key rule**: the result set starts empty. `*` or `*-TAG` populate it first.
`-*TAG` removes from it. `NAME` / `-NAME` add or remove individual materials.

**Important**: `-*TAG` alone without a prior `*` or `*-TAG` produces an empty set -
there is nothing to remove from. To disable a sub-family while keeping the rest:
use `[* -*TAG]` (include all, then subtract).

---

## Cross-family validation

`NAME` and `-NAME` tokens are checked against the family of the config key.
A material that does not belong to the current family is rejected with a warning and discarded.

```yaml
# lockable_tile_entities belongs to the TILE_ENTITIES family.
# FLETCHING_TABLE is a BLOCKS (workstation) member — not a TILE_ENTITIES member — rejected.
lockable_tile_entities:
  - "[* -FLETCHING_TABLE]"           # ERROR: FLETCHING_TABLE not in TILE_ENTITIES -> discarded

# COPPER_CHEST is a CHEST sub-family member — valid.
lockable_tile_entities:
  - "[*-CHEST -COPPER_CHEST]"        # OK: all chest variants except COPPER_CHEST
```

---

## Token behaviour by context

| Expression                                   | Result                                                                          |
|----------------------------------------------|---------------------------------------------------------------------------------|
| `[*]`                                        | All members of the family                                                       |
| `[* -CHEST]`                                 | All members except the CHEST material itself                                    |
| `[*-CHEST]`                                  | Only the CHEST sub-family (all chest variants)                                  |
| `[*-CHEST -COPPER_CHEST]`                    | Whole CHEST sub-family minus COPPER_CHEST                                       |
| `[* -*CHEST]`                                | All family members except the CHEST sub-family                                  |
| `[-*CHEST]`                                  | **Empty** - no base inclusion, nothing to remove from                           |
| `[* -*CHEST COPPER_CHEST]`                   | All family except CHEST sub-family, but COPPER_CHEST re-included                |
| `[*-FURNACE *-SHELF *-TRANSPORT *-MISC *-CHEST -COPPER_CHEST]` | All tile-entity sub-families, CHEST minus COPPER_CHEST |
| `[CHEST BARREL]`                             | Only CHEST and BARREL (empty base, explicit inclusions)                         |

---

## Syntax examples

```yaml
# All members of a family
lockable_tile_entities:
  - "[*]"

# All except one block
lockable_blocks:
  - "[* -DRAGON_EGG]"

# Include only specific blocks (empty base + explicit inclusions)
lockable_tile_entities:
  - "[CHEST BARREL]"

# One sub-family only
lockable_tile_entities:
  - "[*-CHEST]"

# Multiple sub-families
lockable_tile_entities:
  - "[*-CHEST *-SHELF]"

# Sub-family minus one member
lockable_shulker_boxes:
  - "[*-SHULKERS -WHITE_SHULKER_BOX]"

# All shulkers except red
lockable_shulker_boxes:
  - "[*-SHULKERS -RED_SHULKER_BOX]"

# All family members except a sub-family (correct syntax)
lockable_tile_entities:
  - "[* -*SIGN]"

# All tile-entity sub-families with one exclusion inside CHEST
lockable_tile_entities:
  - "[*-FURNACE *-SHELF *-TRANSPORT *-MISC *-CHEST -COPPER_CHEST]"

# Entity family - item frames only (already enabled by default)
lockable_entities:
  - "[*-ITEM_FRAMES]"

# Entity family - storage vehicles and item frames
lockable_entities:
  - "[*-CHEST_BOATS *-CHEST_MINECARTS *-HOPPER_MINECARTS *-ITEM_FRAMES]"

# worlds.yml - per-world example
worlds:
  survival:
    enabled: true
    auto_drop_to_inventory_enabled: true
    lockable_tile_entities:
      - "[*]"
    lockable_shulker_boxes:
      - "[*-SHULKERS -WHITE_SHULKER_BOX]"
    lockable_blocks:
      - "[* -DRAGON_EGG]"
    lockable_doors:
      - "[*]"
  creative:
    enabled: false
    lockable_tile_entities: []
    lockable_shulker_boxes: []
    lockable_blocks: []
    lockable_doors: []
```

---

## auto_drop_to_inventory

Resolves expressions against **all families** independently (union).
Each family processes the expression against its own members.
Sub-family tokens that do not belong to a given family are silently skipped for that family.

```yaml
auto_drop_to_inventory:
  enabled: true
  blocks:
    - "[*-SHULKERS]"                       # all shulkers auto-drop to inventory
    - "[*-SHULKERS -RED_SHULKER_BOX]"      # all shulkers except red
    - "[*]"                                # all lockable blocks across all families
    - "[*-SHULKERS *-CHEST]"              # shulkers + all chest variants

# To disable shulker auto-drop entirely:
#   Option A - set enabled: false
#   Option B - remove shulker entries from blocks list
#   Option C - do not include [*-SHULKERS] or individual shulker names
#
# Note: [-*SHULKERS] alone produces an empty set (no base inclusion = nothing to remove from).
# It will NOT disable shulkers that are listed via other entries in the same blocks list.
```

---

## Sub-family reference

| Key                       | Family          | Available sub-family tags                                          |
|---------------------------|-----------------|--------------------------------------------------------------------|
| `lockable_tile_entities`  | `TILE_ENTITIES` | `CHEST`, `FURNACE`, `SHELF`, `TRANSPORT`, `MISC`, `SIGN`           |
| `lockable_shulker_boxes`  | `SHULKER_BOXES` | `SHULKERS`                                                         |
| `lockable_blocks`         | `BLOCKS`        | `ANVIL`, `CAULDRON`, `FENCE_GATE`, `TRAPDOOR`, `WORKSTATION`       |
| `lockable_doors`          | `DOORS`         | `DOORS`                                                            |
| `lockable_entities`       | `ENTITIES`      | `CHEST_BOATS`, `CHEST_MINECARTS`, `HOPPER_MINECARTS`, `ITEM_FRAMES` |

For the full list of materials in each sub-family: see `LOCKABLE_BLOCKS_REFERENCE.md`.
