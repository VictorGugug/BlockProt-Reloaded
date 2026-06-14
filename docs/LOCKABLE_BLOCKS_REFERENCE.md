# Lockable Blocks Reference — BlockProt Reloaded

Complete list of every block that can be locked, organized by family and sub-family.
Covers all Minecraft versions up to 1.21.x / 26.1 (The Copper Age).

For the family expression syntax used in modern mode: see `BLOCK_FAMILY_SYNTAX.md`.
For the in-game block browser and how to use it: see the `/bp lockables` section below.

---

## The /bp lockables command

`/bp lockables` opens a paged GUI that shows every block the family system knows about,
regardless of whether it is currently enabled or disabled on the server.

**Who can use it:** any player with the `blockprot.user.admin` permission, or any server operator.
Available in both menu mode and CLI mode — it is always accessible regardless of `use_menus`.

**What it shows:**

- Blocks with a normal icon and green **Status: ACTIVE** are currently lockable. Players can protect them.
- Blocks shown in gray with a red **Status: INACTIVE** are known to the system but disabled in `blocks.yml`.
  They are listed so admins can see exactly what is available and add them back if needed.

**Clicking a block in the GUI:**

Clicking does not open anything or change any settings. It sends a clickable message to your chat.
Click that message and the material name is copied to your clipboard, ready to paste into `blocks.yml`.

- **Left-click** a block → copies `MATERIAL_NAME` (green) — paste directly as a flat entry or as an inclusion token in a family expression
- **Right-click** a block → copies `-MATERIAL_NAME` (red) — the exclusion token to remove that material from an active sub-family

The info book in slot 46 shows the server version, your client version (if ViaVersion is installed),
and a count of how many blocks are currently active vs inactive.

---

## Families overview

| Config key                | Family tag      | Sub-families                                              |
|---------------------------|-----------------|-----------------------------------------------------------|
| `lockable_tile_entities`  | `TILE_ENTITIES` | `CHEST`, `FURNACE`, `SHELF`, `TRANSPORT`, `MISC`, `SIGN`  |
| `lockable_shulker_boxes`  | `SHULKER_BOXES` | `SHULKERS`                                                |
| `lockable_blocks`         | `BLOCKS`        | `ANVIL`, `CAULDRON`, `FENCE_GATE`, `TRAPDOOR`, `WORKSTATION` |
| `lockable_doors`          | `DOORS`         | `DOORS`                                                   |
| `lockable_entities`       | `ENTITIES`      | `CHEST_BOATS`, `CHEST_MINECARTS`, `HOPPER_MINECARTS`      |

`lockable_entities` uses entity NBT, not block NBT. Family expressions are fully supported.

---

## lockable_tile_entities

Blocks backed by a block entity. Protection blocks GUI access and hopper interaction.

### Sub-family: CHEST

Token: `*-CHEST`

| Material | MC version | Notes |
|---|---|---|
| `CHEST` | 1.0 | Double-chest: both halves share ownership. |
| `TRAPPED_CHEST` | 1.5 | Emits a redstone signal when opened. Same double-chest rule. |
| `ENDER_CHEST` | 1.0 | Per-player inventory. Locking prevents other players from opening it at that location. |
| `COPPER_CHEST` | 26.1 | Copper Age addition. |
| `EXPOSED_COPPER_CHEST` | 26.1 | |
| `WEATHERED_COPPER_CHEST` | 26.1 | |
| `OXIDIZED_COPPER_CHEST` | 26.1 | |
| `WAXED_COPPER_CHEST` | 26.1 | Does not oxidize further. |
| `WAXED_EXPOSED_COPPER_CHEST` | 26.1 | |
| `WAXED_WEATHERED_COPPER_CHEST` | 26.1 | |
| `WAXED_OXIDIZED_COPPER_CHEST` | 26.1 | |
| `COPPER_TRAPPED_CHEST` | 26.1 | Trapped variant. |
| `EXPOSED_COPPER_TRAPPED_CHEST` | 26.1 | |
| `WEATHERED_COPPER_TRAPPED_CHEST` | 26.1 | |
| `OXIDIZED_COPPER_TRAPPED_CHEST` | 26.1 | |
| `WAXED_COPPER_TRAPPED_CHEST` | 26.1 | |
| `WAXED_EXPOSED_COPPER_TRAPPED_CHEST` | 26.1 | |
| `WAXED_WEATHERED_COPPER_TRAPPED_CHEST` | 26.1 | |
| `WAXED_OXIDIZED_COPPER_TRAPPED_CHEST` | 26.1 | |

### Sub-family: FURNACE

Token: `*-FURNACE`

| Material | MC version | Notes |
|---|---|---|
| `FURNACE` | 1.0 | Blocks retrieval of output and fuel. |
| `SMOKER` | 1.14 | Food only, 2× smelting speed. |
| `BLAST_FURNACE` | 1.14 | Ores and metals only, 2× smelting speed. |

### Sub-family: SHELF

Token: `*-SHELF`

Displays up to three item stacks on its front face. Added in 26.1 (The Copper Age).

| Material | MC version |
|---|---|
| `OAK_SHELF` | 26.1 |
| `SPRUCE_SHELF` | 26.1 |
| `BIRCH_SHELF` | 26.1 |
| `JUNGLE_SHELF` | 26.1 |
| `ACACIA_SHELF` | 26.1 |
| `DARK_OAK_SHELF` | 26.1 |
| `MANGROVE_SHELF` | 26.1 |
| `CHERRY_SHELF` | 26.1 |
| `PALE_OAK_SHELF` | 26.1 |
| `BAMBOO_SHELF` | 26.1 |
| `CRIMSON_SHELF` | 26.1 |
| `WARPED_SHELF` | 26.1 |

### Sub-family: TRANSPORT

Token: `*-TRANSPORT`

| Material | MC version | Notes |
|---|---|---|
| `HOPPER` | 1.5 | Protects against item extraction and redirection. |
| `DISPENSER` | 1.0 | Blocks non-owners from triggering it or accessing its inventory. |
| `DROPPER` | 1.5 | Same as dispenser. |

### Sub-family: MISC

Token: `*-MISC`

Remaining tile entities with no dedicated sub-family.

| Material | MC version | Notes |
|---|---|---|
| `BARREL` | 1.14 | Single-block storage equivalent to a chest. |
| `BREWING_STAND` | 1.0 | Blocks access to potions and ingredients. |
| `DECORATED_POT` | 1.20 | Stores one stack. Accessible via hopper. |
| `CHISELED_BOOKSHELF` | 1.20 | Stores up to six books. Redstone-readable slot index. |
| `CRAFTER` | 1.21 | Automated 3×3 crafting block. |
| `LECTERN` | 1.14 | Blocks other players from taking or replacing the displayed book. |
| `BEEHIVE` | 1.15 | Blocks honey and honeycomb harvesting. |
| `BEE_NEST` | 1.15 | Natural variant of the beehive. Same protection behaviour. |
| `JUKEBOX` | 1.21 | Disc storage added as a block entity in 1.21. Hopper-accessible. |

### Sub-family: SIGN

Token: `*-SIGN`

All sign tile entities: floor signs, wall signs, hanging signs, and wall-mounted hanging signs.
Locking prevents other players from editing the text. Not included in the default `blocks.yml` —
add `*-SIGN` explicitly to enable.

Wood types covered: oak, spruce, birch, jungle, acacia, dark_oak, mangrove, cherry, bamboo, crimson, warped, pale_oak.

| Pattern | MC version |
|---|---|
| `*_SIGN` | 1.0+ — floor-placed sign |
| `*_WALL_SIGN` | 1.0+ — wall-mounted sign |
| `*_HANGING_SIGN` | 1.20 — hanging sign |
| `*_WALL_HANGING_SIGN` | 1.20 — wall-mounted hanging sign |

---

## lockable_shulker_boxes

Shulker boxes carry their NBT when broken. BlockProt preserves the lock data in the dropped item
so the box is still protected when placed again elsewhere.

### Sub-family: SHULKERS

Token: `*-SHULKERS` (also matches `[*]` since this is the only sub-family)

| Material | MC version |
|---|---|
| `SHULKER_BOX` | 1.11 |
| `WHITE_SHULKER_BOX` | 1.12 |
| `ORANGE_SHULKER_BOX` | 1.12 |
| `MAGENTA_SHULKER_BOX` | 1.12 |
| `LIGHT_BLUE_SHULKER_BOX` | 1.12 |
| `YELLOW_SHULKER_BOX` | 1.12 |
| `LIME_SHULKER_BOX` | 1.12 |
| `PINK_SHULKER_BOX` | 1.12 |
| `GRAY_SHULKER_BOX` | 1.12 |
| `LIGHT_GRAY_SHULKER_BOX` | 1.12 |
| `CYAN_SHULKER_BOX` | 1.12 |
| `PURPLE_SHULKER_BOX` | 1.12 |
| `BLUE_SHULKER_BOX` | 1.12 |
| `BROWN_SHULKER_BOX` | 1.12 |
| `GREEN_SHULKER_BOX` | 1.12 |
| `RED_SHULKER_BOX` | 1.12 |
| `BLACK_SHULKER_BOX` | 1.12 |

---

## lockable_blocks

Interactive blocks that are not tile entities. Protection blocks right-click interaction.

### Sub-family: ANVIL

Token: `*-ANVIL`

| Material | MC version | Notes |
|---|---|---|
| `ANVIL` | 1.4 | Blocks the GUI (item naming, repairing, enchanting). |
| `CHIPPED_ANVIL` | 1.4 | Damaged variant. |
| `DAMAGED_ANVIL` | 1.4 | Heavily damaged variant. |

### Sub-family: CAULDRON

Token: `*-CAULDRON`

| Material | MC version | Notes |
|---|---|---|
| `CAULDRON` | 1.0 | Empty cauldron. Blocks filling and draining. |
| `WATER_CAULDRON` | 1.17 | Contains water (1–3 levels). |
| `LAVA_CAULDRON` | 1.17 | Contains lava. |
| `POWDER_SNOW_CAULDRON` | 1.17 | Contains powder snow. |

### Sub-family: WORKSTATION

Token: `*-WORKSTATION`

Blocks non-owners from opening the GUI.

| Material | MC version |
|---|---|
| `GRINDSTONE` | 1.14 |
| `STONECUTTER` | 1.14 |
| `LOOM` | 1.14 |
| `CARTOGRAPHY_TABLE` | 1.14 |
| `SMITHING_TABLE` | 1.14 |
| `ENCHANTING_TABLE` | 1.0 |

### Sub-family: FENCE_GATE

Token: `*-FENCE_GATE`

Blocks non-owners from opening or closing the gate.

| Material | MC version |
|---|---|
| `OAK_FENCE_GATE` | 1.0 |
| `SPRUCE_FENCE_GATE` | 1.8 |
| `BIRCH_FENCE_GATE` | 1.8 |
| `JUNGLE_FENCE_GATE` | 1.8 |
| `ACACIA_FENCE_GATE` | 1.8 |
| `DARK_OAK_FENCE_GATE` | 1.8 |
| `MANGROVE_FENCE_GATE` | 1.19 |
| `CHERRY_FENCE_GATE` | 1.20 |
| `PALE_OAK_FENCE_GATE` | 1.21.4 |
| `BAMBOO_FENCE_GATE` | 1.20 |
| `CRIMSON_FENCE_GATE` | 1.16 |
| `WARPED_FENCE_GATE` | 1.16 |

### Sub-family: TRAPDOOR

Token: `*-TRAPDOOR`

Blocks non-owners from opening or closing the trapdoor.

| Material | MC version |
|---|---|
| `OAK_TRAPDOOR` | 1.0 |
| `SPRUCE_TRAPDOOR` | 1.8 |
| `BIRCH_TRAPDOOR` | 1.8 |
| `JUNGLE_TRAPDOOR` | 1.8 |
| `ACACIA_TRAPDOOR` | 1.8 |
| `DARK_OAK_TRAPDOOR` | 1.8 |
| `MANGROVE_TRAPDOOR` | 1.19 |
| `CHERRY_TRAPDOOR` | 1.20 |
| `PALE_OAK_TRAPDOOR` | 1.21.4 |
| `BAMBOO_TRAPDOOR` | 1.20 |
| `CRIMSON_TRAPDOOR` | 1.16 |
| `WARPED_TRAPDOOR` | 1.16 |
| `IRON_TRAPDOOR` | 1.8 |
| `COPPER_TRAPDOOR` | 1.21 |
| `EXPOSED_COPPER_TRAPDOOR` | 1.21 |
| `WEATHERED_COPPER_TRAPDOOR` | 1.21 |
| `OXIDIZED_COPPER_TRAPDOOR` | 1.21 |
| `WAXED_COPPER_TRAPDOOR` | 1.21 |
| `WAXED_EXPOSED_COPPER_TRAPDOOR` | 1.21 |
| `WAXED_WEATHERED_COPPER_TRAPDOOR` | 1.21 |
| `WAXED_OXIDIZED_COPPER_TRAPDOOR` | 1.21 |

### Ungrouped

Blocks with no dedicated sub-family token. Target individually with `MATERIAL_NAME`.

| Material | MC version | Notes |
|---|---|---|
| `DRAGON_EGG` | 1.0 | Right-clicking teleports it. Protection blocks other players from touching it. |
| `COMPOSTER` | 1.14 | Blocks item deposits and bone meal retrieval. |
| `BELL` | 1.14 | Blocks ringing by non-owners. |
| `NOTE_BLOCK` | 1.0 | Blocks pitch changes, useful for musical builds. |

---

## lockable_doors

Two-block-tall door blocks. Protection blocks non-owners from opening or closing the door.

### Sub-family: DOORS

Token: `*-DOORS` (also matches `[*]`)

| Material | MC version |
|---|---|
| `OAK_DOOR` | 1.0 |
| `SPRUCE_DOOR` | 1.8 |
| `BIRCH_DOOR` | 1.8 |
| `JUNGLE_DOOR` | 1.8 |
| `ACACIA_DOOR` | 1.8 |
| `DARK_OAK_DOOR` | 1.8 |
| `MANGROVE_DOOR` | 1.19 |
| `CHERRY_DOOR` | 1.20 |
| `PALE_OAK_DOOR` | 1.21.4 |
| `BAMBOO_DOOR` | 1.20 |
| `CRIMSON_DOOR` | 1.16 |
| `WARPED_DOOR` | 1.16 |
| `IRON_DOOR` | 1.0 |
| `COPPER_DOOR` | 1.21 |
| `EXPOSED_COPPER_DOOR` | 1.21 |
| `WEATHERED_COPPER_DOOR` | 1.21 |
| `OXIDIZED_COPPER_DOOR` | 1.21 |
| `WAXED_COPPER_DOOR` | 1.21 |
| `WAXED_EXPOSED_COPPER_DOOR` | 1.21 |
| `WAXED_WEATHERED_COPPER_DOOR` | 1.21 |
| `WAXED_OXIDIZED_COPPER_DOOR` | 1.21 |

---

## lockable_entities

Boats and minecarts that carry inventories. Protection is stored as entity NBT and survives
being broken and re-placed as long as the item's NBT is preserved.

Present in `blocks.yml` as an empty list by default — add material names or family expressions
to enable entity protection. The Entities section of `/bp lockables` only appears when at
least one entity type is active.

### Sub-family: CHEST_BOATS

Token: `*-CHEST_BOATS`

| Material | MC version |
|---|---|
| `CHEST_BOAT` | 1.19 — generic (oak) |
| `OAK_CHEST_BOAT` | 1.19 |
| `SPRUCE_CHEST_BOAT` | 1.19 |
| `BIRCH_CHEST_BOAT` | 1.19 |
| `JUNGLE_CHEST_BOAT` | 1.19 |
| `ACACIA_CHEST_BOAT` | 1.19 |
| `DARK_OAK_CHEST_BOAT` | 1.19 |
| `MANGROVE_CHEST_BOAT` | 1.19 |
| `CHERRY_CHEST_BOAT` | 1.20 |
| `BAMBOO_CHEST_BOAT` | 1.20 |

### Sub-family: CHEST_MINECARTS

Token: `*-CHEST_MINECARTS`

| Material | MC version |
|---|---|
| `CHEST_MINECART` | 1.0 |

### Sub-family: HOPPER_MINECARTS

Token: `*-HOPPER_MINECARTS`

| Material | MC version |
|---|---|
| `HOPPER_MINECART` | 1.5 |

Example `blocks.yml` expressions:
```yaml
lockable_entities:
  - "[*]"                                          # all entity types
  - "[*-CHEST_BOATS]"                              # only chest boats
  - "[*-CHEST_MINECARTS *-HOPPER_MINECARTS]"       # minecarts only
  - "[*-CHEST_BOATS -BAMBOO_CHEST_BOAT]"           # all boats except bamboo
```

---

## auto_drop_to_inventory

Controls which blocks deliver their contents directly to the breaker's inventory instead of
dropping items on the ground. Spans all families — a single expression resolves against the
full lockable universe.

In legacy mode, list material names explicitly. In modern mode, use family expressions.

Legacy default (shulkers only):
```yaml
auto_drop_to_inventory:
  enabled: true
  blocks:
    - SHULKER_BOX
    - WHITE_SHULKER_BOX
    - ORANGE_SHULKER_BOX
    # ... (all 17 variants)
```

Modern equivalent:
```yaml
auto_drop_to_inventory:
  enabled: true
  blocks:
    - "[*-SHULKERS]"
```

Other useful modern expressions:
```yaml
    - "[*]"                               # every lockable block
    - "[*-SHULKERS *-CHEST]"             # shulkers + all chest variants
    - "[*-SHULKERS -WHITE_SHULKER_BOX]"  # shulkers except white
```

---

## Item frames (entity protection, always active)

Item frames and glowing item frames are protected by `ItemFrameListener`. They are entities,
not blocks, so they do not appear in `blocks.yml` and cannot be toggled per-world. The feature
is always active.

| Entity type | MC version | Notes |
|---|---|---|
| `ITEM_FRAME` | 1.4 | Standard item frame. |
| `GLOW_ITEM_FRAME` | 1.17 | Glowing variant, always illuminated. |

**How to protect an item frame:**

1. Sneak and right-click the frame with an empty hand.
2. The BlockProt protection menu opens.
3. Assign the frame to yourself as owner. Add friends if needed.
4. Once protected: non-owners cannot rotate or swap the displayed item, and cannot break the frame.

**ImageFrame compatibility:**

Frames placed by the ImageFrame plugin carry a creator UUID in their NBT. BlockProt reads this
tag and treats the creator as the owner on first access, so multi-map image displays are
automatically protected without any extra configuration.

**Admin override:** players with `blockprot.user.admin` always bypass frame protection.
