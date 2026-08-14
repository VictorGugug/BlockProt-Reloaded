/*
 * Copyright (C) 2021 - 2026 spnda
 * Modifications Copyright (C) 2025 - 2026 Zaynr (Zar)
 * This file is part of BlockProt Reloaded <https://github.com/VictorGugug/BlockProt-Reloaded>.
 * Based on BlockProt <https://github.com/spnda/BlockProt>.
 *
 * BlockProt is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.sean.blockprot.bukkit.dialogs;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.config.WorldsConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class LockablesDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xF0A0A0);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xD2B48C);
    private static final TextColor SOFT_BLUE = TextColor.color(0xA0C4E8);
    private static final TextColor PASTEL_PURPLE = TextColor.color(0xC8A0E0);
    private static final TextColor PASTEL_ORANGE = TextColor.color(0xDFB98E);

    private static final int PER_PAGE = 6;

    private LockablesDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        show(player, backOrigin, 0);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin, int page) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        DefaultConfig cfg = BlockProt.getDefaultConfig();
        List<CategoryEntry> categories = buildCategoryInfo(cfg);
        int totalPages = Math.max(1, (int) Math.ceil(categories.size() / (double) PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PER_PAGE;
        int to = Math.min(from + PER_PAGE, categories.size());
        List<CategoryEntry> pageEntries = categories.subList(from, to);

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__LOCKABLES__TITLE)),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__HEADER)), SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PAGE))
                .replace("{current}", String.valueOf(safePage + 1))
                .replace("{total}", String.valueOf(totalPages)),
            TextColor.color(0x888888))));

        List<DialogButton> buttons = new ArrayList<>();

        for (CategoryEntry entry : pageEntries) {
            boolean noneActive = entry.activeCount == 0;
            TextColor c = stateColor(entry.activeCount, entry.totalCount);
            String catLabel = translateCategory(entry.label);
            buttons.add(new DialogButton("cat_" + entry.label,
                Component.text()
                    .append(Component.text(stripColor(Translator.get(noneActive ? TranslationKey.ICON__TOGGLE_OFF : TranslationKey.ICON__TOGGLE_ON)), c))
                    .append(Component.text(catLabel, NamedTextColor.WHITE))
                    .append(Component.text(" (" + entry.activeCount + "/" + entry.totalCount + ")", TextColor.color(0x888888)))
                    .build(),
                Component.join(JoinConfiguration.newlines(),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CAT_PREFIX)), SOFT_GRAY)
                        .append(Component.text(catLabel, NamedTextColor.WHITE)),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_ENABLE))
                        + " / " + stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_DISABLE)), TextColor.color(0x888888))),
                p -> LockableCategoryDialog.show(p, backOrigin, entry.label, entry.materials)
            ));
        }

        buttons.add(new DialogButton("auto_drop",
            Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__AUTO_DROP)), SOFT_BLUE),
            Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__AUTO_DROP_LORE)), TextColor.color(0x888888)),
            p -> AutoDropDialog.show(p, backOrigin, q -> LockablesDialog.show(q, backOrigin))
        ));

        if (cfg.isPerWorldsConfigEnabled()) {
            int worldCount = Bukkit.getWorlds().size();
            int configuredCount = worldsConfigCount();
            buttons.add(new DialogButton("world_lockables",
                Component.text(stripColor(Translator.get(TranslationKey.WORLDS__PER_WORLD_CONFIG)), PASTEL_PURPLE),
                Component.join(JoinConfiguration.newlines(),
                    Component.text(worldCount + " " + stripColor(Translator.get(TranslationKey.WORLDS__WORLDS)), TextColor.color(0x888888)),
                    Component.text(stripColor(Translator.get(TranslationKey.WORLDS__CONFIGURED))
                        + ": " + configuredCount + "/" + worldCount, TextColor.color(0x888888))),
                p -> WorldLockableSelectionDialog.show(p, backOrigin)
            ));
        }

        List<DialogButton> navButtons = new ArrayList<>();
        if (safePage > 0) {
            int prevPage = safePage - 1;
            navButtons.add(new DialogButton("prev",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, prevPage)));
        }
        if (safePage + 1 < totalPages) {
            int nextPage = safePage + 1;
            navButtons.add(new DialogButton("next",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, nextPage)));
        }

        buttons.addAll(navButtons);

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = DialogNavigation.backButton(
            exitOrigin,
            exitOrigin == DialogOrigin.ADMIN_MENU ? p -> AdminMenuDialog.show(p) : null,
            exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__RETURN_PREVIOUS : null
        );

        bridge.showMultiAction(player, title, body, buttons, backBtn, 1);
    }

    private static int worldsConfigCount() {
        WorldsConfig wc = BlockProt.getWorldsConfig();
        if (wc == null) return 0;
        int count = 0;
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            if (wc.hasWorldConfig(w)) count++;
        }
        return count;
    }

    private static List<CategoryEntry> buildCategoryInfo(DefaultConfig cfg) {
        Map<String, List<Material>> catMap = new LinkedHashMap<>();
        catMap.put("Chests", new ArrayList<>());
        catMap.put("Shulkers", new ArrayList<>());
        catMap.put("Furnaces", new ArrayList<>());
        catMap.put("Storage", new ArrayList<>());
        catMap.put("Signs", new ArrayList<>());
        catMap.put("Doors", new ArrayList<>());
        catMap.put("Trapdoors", new ArrayList<>());
        catMap.put("Beds", new ArrayList<>());
        catMap.put("Gates", new ArrayList<>());
        catMap.put("Workstations", new ArrayList<>());
        catMap.put("Interactive", new ArrayList<>());
        catMap.put("Entities", new ArrayList<>());

        for (Material m : Material.values()) {
            if (m.isLegacy()) continue;
            String n = m.name();
            List<Material> dest = null;
            if (n.contains("SHULKER_BOX")) dest = catMap.get("Shulkers");
            else if (n.endsWith("_DOOR") && !n.contains("TRAP") && !n.contains("BOAT")) dest = catMap.get("Doors");
            else if (n.contains("TRAPDOOR")) dest = catMap.get("Trapdoors");
            else if (n.endsWith("_BED")) dest = catMap.get("Beds");
            else if (n.contains("FENCE_GATE")) dest = catMap.get("Gates");
            else if ((n.contains("FURNACE") || n.equals("BLAST_FURNACE") || n.equals("SMOKER")) && m.isBlock()) dest = catMap.get("Furnaces");
            else if ((n.contains("CHEST") || n.equals("ENDER_CHEST")) && m.isBlock() && !n.contains("BOAT")) dest = catMap.get("Chests");
            else if (n.endsWith("_SIGN") || n.endsWith("_WALL_SIGN") || n.endsWith("_HANGING_SIGN") || n.endsWith("_WALL_HANGING_SIGN")) dest = catMap.get("Signs");
            else if ((n.equals("BARREL") || n.endsWith("_SHELF") || n.equals("DECORATED_POT") || n.equals("CHISELED_BOOKSHELF")
                || n.equals("CRAFTER") || n.equals("BREWING_STAND") || n.equals("HOPPER") || n.equals("DISPENSER")
                || n.equals("DROPPER") || n.equals("BEEHIVE") || n.equals("BEE_NEST") || n.equals("JUKEBOX")
                || n.equals("LECTERN") || n.equals("BEACON")) && m.isBlock()) dest = catMap.get("Storage");
            else if ((n.equals("GRINDSTONE") || n.equals("STONECUTTER") || n.equals("LOOM") || n.equals("CARTOGRAPHY_TABLE")
                || n.equals("SMITHING_TABLE") || n.equals("ENCHANTING_TABLE") || n.equals("FLETCHING_TABLE")
                || n.contains("ANVIL")) && m.isBlock()) dest = catMap.get("Workstations");
            else if ((n.contains("BUTTON") || n.contains("LEVER") || n.contains("DAYLIGHT_DETECTOR")
                || n.contains("PRESSURE_PLATE") || n.equals("OBSERVER") || n.equals("TARGET")
                || n.equals("NOTE_BLOCK") || n.equals("BELL") || n.equals("COMPOSTER")
                || n.equals("DRAGON_EGG") || n.contains("CAULDRON")) && m.isBlock()) dest = catMap.get("Interactive");
            else if (BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.ENTITIES).contains(m)) dest = catMap.get("Entities");
            // Every remaining block family member falls back to Interactive so the dialog
            // mirrors LockablesInventory.classify(), which sends everything unclassified to
            // INTERACTIVE. The gate below keeps the list down to real family members: a
            // material that matches a category here but belongs to no lockable family (e.g.
            // buttons, LEVER, DAYLIGHT_DETECTOR when no lockable_blocks family covers them)
            // would produce a button whose toggleLockable() silently no-ops, because
            // configKeyForMaterial() finds no family for it. Gate every category, including
            // Entities, on real family membership so every listed material is guaranteed to
            // actually toggle regardless of how the hand-written heuristics drift.
            if (dest != null && !isKnownLockableMaterial(m)) dest = null;
            if (dest != null) dest.add(m);
        }

        List<CategoryEntry> result = new ArrayList<>();
        for (Map.Entry<String, List<Material>> e : catMap.entrySet()) {
            List<Material> mats = e.getValue();
            long activeCount = mats.stream().filter(m -> cfg.isLockable(m) || cfg.isLockableEntity(m)).count();
            result.add(new CategoryEntry(e.getKey(), mats, activeCount, mats.size()));
        }
        result.removeIf(ce -> ce.totalCount == 0);
        return result;
    }

    /**
     * Picks the state color for an active/total pair. Orange is reserved for
     * counts sitting near the midpoint (a "close call" signal); everything
     * else falls back to coral (none active) or mint (some/all active). The
     * near-half band widens for small totals so a swing of a single item
     * still lands inside it (percentages jump too hard at low counts for a
     * fixed 40-60% band to be meaningful).
     */
    private static TextColor stateColor(long active, long total) {
        if (active == 0) return PASTEL_CORAL;
        if (active == total) return PASTEL_MINT;
        double ratio = (double) active / total;
        double halfBand = total <= 5 ? 0.20 : 0.10;
        return Math.abs(ratio - 0.5) <= halfBand ? PASTEL_ORANGE : PASTEL_MINT;
    }

    private static boolean isKnownLockableMaterial(@NotNull Material m) {
        for (BlockFamilyParser.Family family : BlockFamilyParser.Family.values()) {
            if (BlockFamilyParser.getFamilyMembers(family).contains(m)) return true;
        }
        return false;
    }

    private static String translateCategory(String label) {
        return switch (label) {
            case "Chests" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__CHESTS));
            case "Shulkers" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__SHULKERS));
            case "Furnaces" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__FURNACES));
            case "Storage" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__STORAGE));
            case "Signs" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__SIGNS));
            case "Doors" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__DOORS));
            case "Trapdoors" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__TRAPDOORS));
            case "Beds" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__BEDS));
            case "Gates" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__GATES));
            case "Workstations" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__WORKSTATIONS));
            case "Interactive" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__INTERACTIVE));
            case "Entities" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__ENTITIES));
            default -> label;
        };
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }

    private record CategoryEntry(String label, List<Material> materials, long activeCount, long totalCount) {}
}
