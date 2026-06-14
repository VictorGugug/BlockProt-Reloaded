package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.VersionCompat;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.integrations.PluginIntegration;
import de.sean.blockprot.bukkit.integrations.ViaVersionIntegration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Paged inventory listing every lockable material known to the plugin.
 *
 * Shows ALL blocks that CAN be locked (active) and ALL blocks that EXIST in the
 * family but are currently DISABLED — always, without filtering. Active blocks
 * appear with their normal colour; disabled blocks appear in gray with a [OFF] tag.
 *
 * Each block's icon is resolved semantically by sub-family:
 *   - Chests       → the real material (CHEST, TRAPPED_CHEST, etc.)
 *   - Shulkers     → the real coloured shulker box material
 *   - Furnaces     → the real material (FURNACE, SMOKER, BLAST_FURNACE)
 *   - Shelves      → the real wood shelf material
 *   - Transport    → the real material (HOPPER, DISPENSER, DROPPER)
 *   - Misc storage → the real material (BARREL, BREWING_STAND, etc.)
 *   - Signs        → floor sign form of the same wood type (wall variants have no item)
 *   - Doors        → the real door material
 *   - Trapdoors    → the real trapdoor material
 *   - Fence gates  → the real fence gate material
 *   - Workstations → the real material (GRINDSTONE, ENCHANTING_TABLE, etc.)
 *   - Interactive  → the real material (DRAGON_EGG, COMPOSTER, etc.)
 *   - Cauldrons    → CAULDRON (water/lava/snow variants have no distinct item form)
 *   - Entities     → representative item icon (CHEST_MINECART, OAK_BOAT, etc.)
 *
 * Layout (54 slots):
 *   0-44  — block items
 *   45    — version info book
 *   47    — prev page arrow
 *   49    — next page arrow
 *   53    — back button
 *
 * Click on a block item:
 *   Left-click  → copies MATERIAL_NAME to clipboard (green — include)
 *   Right-click → copies -MATERIAL_NAME to clipboard (red — exclude)
 */
public final class LockablesInventory extends BlockProtInventory {

    private static final int CONTENT_SLOTS = 45;
    private static final int SLOT_INFO     = 45;
    private static final int SLOT_PREV     = 47;
    private static final int SLOT_NEXT     = 49;
    private static final int SLOT_BACK     = 53;

    private enum Category {
        CHESTS("Chests"),
        SHULKERS("Shulker Boxes"),
        FURNACES("Furnaces"),
        STORAGE("Storage"),
        SIGNS("Signs"),
        DOORS("Doors"),
        TRAPDOORS("Trapdoors"),
        GATES("Fence Gates"),
        WORKSTATIONS("Workstations"),
        INTERACTIVE("Interactive"),
        ENTITIES("Entities");

        final String label;
        Category(String l) { this.label = l; }
    }

    /** Entry in the display list. null material = category separator. */
    private record Entry(@Nullable Material material, boolean active, @Nullable String categoryLabel) {
        static Entry separator(String label) { return new Entry(null, false, label); }
        static Entry block(Material m, boolean active) { return new Entry(m, active, null); }
    }

    private List<Entry> pagedList = List.of();
    private int cachedPage = 0;

    public LockablesInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    String getTranslatedInventoryName() { return "Lockable Blocks"; }

    @NotNull
    public Inventory fill(@NotNull Player player, int page) {
        pagedList = buildAllEntries();

        int totalPages = Math.max(1, (int) Math.ceil(pagedList.size() / (double) CONTENT_SLOTS));
        int safePage   = Math.max(0, Math.min(page, totalPages - 1));
        cachedPage     = safePage;
        int start      = safePage * CONTENT_SLOTS;
        int end        = Math.min(start + CONTENT_SLOTS, pagedList.size());

        inventory = createInventory(buildTitle(player, safePage, totalPages));

        for (int i = start; i < end; i++) {
            Entry e = pagedList.get(i);
            int slot = i - start;
            if (e.material() == null) {
                inventory.setItem(slot, separatorItem(e.categoryLabel() != null ? e.categoryLabel() : ""));
            } else {
                inventory.setItem(slot, blockItem(e.material(), e.active()));
            }
        }

        long activeCount   = pagedList.stream().filter(e -> e.material() != null && e.active()).count();
        long inactiveCount = pagedList.stream().filter(e -> e.material() != null && !e.active()).count();

        ItemStack info = new ItemStack(Material.BOOK, 1);
        ItemMeta im = info.getItemMeta();
        if (im != null) {
            im.displayName(Component.text("Lockable Blocks Info").color(NamedTextColor.GOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Server: " + VersionCompat.getVersionString()).color(NamedTextColor.YELLOW));
            lore.add(Component.text("Client: " + resolveClientVersion(player)).color(NamedTextColor.YELLOW));
            lore.add(Component.text("Active:   " + activeCount).color(NamedTextColor.GREEN));
            lore.add(Component.text("Inactive: " + inactiveCount).color(NamedTextColor.GRAY));
            lore.add(Component.text("Page: " + (safePage + 1) + "/" + totalPages).color(NamedTextColor.DARK_GRAY));
            im.lore(lore);
            info.setItemMeta(im);
        }
        inventory.setItem(SLOT_INFO, info);

        if (safePage > 0)              setItemStack(SLOT_PREV, Material.ARROW, "< Previous");
        if (safePage < totalPages - 1) setItemStack(SLOT_NEXT, Material.ARROW, "Next >");
        setBackButton(SLOT_BACK);
        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        if (slot == SLOT_BACK)  { goBack(player, state); return; }

        if (slot == SLOT_PREV && cachedPage > 0) {
            state.currentPageIndex = cachedPage - 1;
            InventoryState.set(player.getUniqueId(), state);
            player.openInventory(fill(player, state.currentPageIndex));
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil(pagedList.size() / (double) CONTENT_SLOTS));
        if (slot == SLOT_NEXT && cachedPage < totalPages - 1) {
            state.currentPageIndex = cachedPage + 1;
            InventoryState.set(player.getUniqueId(), state);
            player.openInventory(fill(player, state.currentPageIndex));
            return;
        }

        if (slot < CONTENT_SLOTS) {
            int absIdx = cachedPage * CONTENT_SLOTS + slot;
            if (absIdx < pagedList.size()) {
                Entry e = pagedList.get(absIdx);
                if (e.material() != null) {
                    boolean right = event.getClick() == ClickType.RIGHT
                        || event.getClick() == ClickType.SHIFT_RIGHT;
                    String token = right ? "-" + e.material().name() : e.material().name();
                    // COPY_TO_CLIPBOARD tells the Minecraft client to write the token
                    // to the system clipboard when the player clicks the chat message.
                    net.kyori.adventure.text.event.ClickEvent copyEvent =
                        net.kyori.adventure.text.event.ClickEvent.copyToClipboard(token);
                    net.kyori.adventure.text.event.HoverEvent<?> hoverEvent =
                        net.kyori.adventure.text.event.HoverEvent.showText(
                            Component.text("Click to copy to clipboard").color(NamedTextColor.GRAY));
                    Component msg = Component.text("[Copy] ").color(NamedTextColor.DARK_GRAY)
                        .append(Component.text(token)
                            .color(right ? NamedTextColor.RED : NamedTextColor.GREEN)
                            .clickEvent(copyEvent)
                            .hoverEvent(hoverEvent));
                    player.sendMessage(msg);
                }
            }
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    // ── Build the full list from the family parser ────────────────────────────

    @NotNull
    private static List<Entry> buildAllEntries() {
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        Map<Category, List<Entry>> grouped = new LinkedHashMap<>();
        for (Category c : Category.values()) grouped.put(c, new ArrayList<>());

        // Block families (tile entities, shulkers, blocks, doors)
        for (BlockFamilyParser.Family family : BlockFamilyParser.Family.values()) {
            if (family == BlockFamilyParser.Family.ENTITIES) continue; // entities handled separately
            for (Material m : BlockFamilyParser.getFamilyMembers(family)) {
                boolean active = cfg.isLockable(m);
                grouped.get(classify(m, cfg)).add(Entry.block(m, active));
            }
        }

        // Entity family: only include if at least one entity is active in the config.
        // When lockable_entities is empty or commented out, the Entities category is hidden
        // entirely rather than showing a section full of INACTIVE items.
        List<Entry> entityEntries = new ArrayList<>();
        for (Material m : BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.ENTITIES)) {
            boolean active = cfg.isLockableEntity(m);
            entityEntries.add(Entry.block(m, active));
        }
        boolean hasActiveEntities = entityEntries.stream().anyMatch(Entry::active);
        if (hasActiveEntities) {
            entityEntries.sort(Comparator
                .<Entry, Boolean>comparing(e -> !e.active())
                .thenComparing(e -> e.material() != null ? e.material().name() : ""));
            grouped.get(Category.ENTITIES).addAll(entityEntries);
        }

        // Sort each category: active first, then alphabetical
        for (List<Entry> list : grouped.values()) {
            list.sort(Comparator
                .<Entry, Boolean>comparing(e -> !e.active())
                .thenComparing(e -> e.material() != null ? e.material().name() : ""));
        }

        List<Entry> result = new ArrayList<>();
        for (Map.Entry<Category, List<Entry>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            result.add(Entry.separator(entry.getKey().label));
            result.addAll(entry.getValue());
        }
        if (!result.isEmpty() && result.get(0).material() == null) result.remove(0);
        return result;
    }

    @NotNull
    private static Category classify(@NotNull Material m, @NotNull DefaultConfig cfg) {
        String n = m.name();
        if (cfg.isLockableShulkerBox(m) || n.contains("SHULKER_BOX")) return Category.SHULKERS;
        if (n.endsWith("_DOOR") && !n.contains("TRAP"))               return Category.DOORS;
        if (n.contains("TRAPDOOR"))                                    return Category.TRAPDOORS;
        if (n.contains("FENCE_GATE"))                                  return Category.GATES;
        if (n.contains("FURNACE") || n.equals("SMOKER")
            || n.equals("BLAST_FURNACE"))                              return Category.FURNACES;
        if (n.endsWith("_SIGN") || n.endsWith("_WALL_SIGN")
            || n.endsWith("_HANGING_SIGN")
            || n.endsWith("_WALL_HANGING_SIGN"))                       return Category.SIGNS;
        if (n.contains("CHEST") || n.equals("BARREL") || n.equals("ENDER_CHEST")
            || n.endsWith("_SHELF") || n.equals("DECORATED_POT")
            || n.equals("CHISELED_BOOKSHELF") || n.equals("CRAFTER")
            || n.equals("BREWING_STAND") || n.equals("HOPPER")
            || n.equals("DISPENSER") || n.equals("DROPPER")
            || n.equals("BEEHIVE") || n.equals("BEE_NEST")
            || n.equals("JUKEBOX") || n.equals("LECTERN"))            return Category.STORAGE;
        if (n.equals("GRINDSTONE") || n.equals("STONECUTTER")
            || n.equals("LOOM") || n.equals("CARTOGRAPHY_TABLE")
            || n.equals("SMITHING_TABLE") || n.equals("ENCHANTING_TABLE")
            || n.contains("ANVIL"))                                    return Category.WORKSTATIONS;
        return Category.INTERACTIVE;
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    @NotNull
    private static ItemStack separatorItem(@NotNull String label) {
        ItemStack sep = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = sep.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("── " + label + " ──").color(NamedTextColor.AQUA));
            meta.lore(List.of());
            sep.setItemMeta(meta);
        }
        return sep;
    }

    @NotNull
    private static ItemStack blockItem(@NotNull Material mat, boolean active) {
        Material display = resolveDisplayMaterial(mat);
        ItemStack stack;
        try {
            stack = new ItemStack(display, 1);
            if (stack.getType() == Material.AIR) stack = new ItemStack(Material.PAPER, 1);
        } catch (Exception e) {
            stack = new ItemStack(Material.PAPER, 1);
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            NamedTextColor nameColor = active ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY;
            String suffix = active ? "" : " §8[OFF]";
            meta.displayName(Component.text(friendlyName(mat) + suffix).color(nameColor));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(mat.name()).color(NamedTextColor.DARK_GRAY));
            lore.add(Component.text(active ? "Status: ACTIVE" : "Status: INACTIVE")
                .color(active ? NamedTextColor.GREEN : NamedTextColor.RED));
            lore.add(Component.text("Left-click: copy name to clipboard").color(NamedTextColor.GREEN));
            lore.add(Component.text("Right-click: copy -name to clipboard").color(NamedTextColor.RED));
            meta.lore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * Resolves the best item-form Material for display in an inventory slot.
     *
     * Rules by sub-family:
     *   Signs (wall / wall-hanging) → use the placeable floor/hanging form; wall variants have no item.
     *   Cauldrons (water/lava/snow) → use plain CAULDRON; the filled variants are block-only states.
     *   Everything else             → use the material directly (it has an item form on all versions).
     */
    @NotNull
    private static Material resolveDisplayMaterial(@NotNull Material mat) {
        String name = mat.name();

        // Wall sign → floor sign of same wood
        if (name.endsWith("_WALL_SIGN") && !name.endsWith("_HANGING_SIGN")) {
            Material m = Material.matchMaterial(name.replace("_WALL_SIGN", "_SIGN"));
            return m != null ? m : Material.OAK_SIGN;
        }
        // Wall hanging sign → hanging sign of same wood
        if (name.endsWith("_WALL_HANGING_SIGN")) {
            Material m = Material.matchMaterial(name.replace("_WALL_HANGING_SIGN", "_HANGING_SIGN"));
            return m != null ? m : Material.OAK_HANGING_SIGN;
        }
        // Filled cauldrons are block states, not items
        if (name.equals("WATER_CAULDRON") || name.equals("LAVA_CAULDRON")
                || name.equals("POWDER_SNOW_CAULDRON")) {
            return Material.CAULDRON;
        }
        // Chest boats: use OAK_CHEST_BOAT as fallback if the specific variant doesn't exist
        if (name.contains("CHEST_BOAT")) {
            Material m = Material.matchMaterial(name);
            if (m != null) return m;
            Material fallback = Material.matchMaterial("OAK_CHEST_BOAT");
            return fallback != null ? fallback : Material.CHEST;
        }
        return mat;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    @NotNull
    private static String resolveClientVersion(@NotNull Player player) {
        for (PluginIntegration integration : BlockProt.getInstance().getIntegrations()) {
            if (integration instanceof ViaVersionIntegration via && via.isEnabled()) {
                int proto = via.getPlayerProtocolVersion(player);
                if (proto > 0) return via.getPlayerVersionString(player);
            }
        }
        return VersionCompat.getVersionString();
    }

    @NotNull
    private static String buildTitle(@NotNull Player player, int page, int total) {
        String client = resolveClientVersion(player);
        String server = VersionCompat.getVersionString();
        String suffix = client.equals(server) ? server : server + "/" + client;
        return "Lockables [" + suffix + "]" + (page > 0 ? " p" + (page + 1) + "/" + total : "");
    }

    @NotNull
    private static String friendlyName(@NotNull Material mat) {
        String[] words = mat.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
