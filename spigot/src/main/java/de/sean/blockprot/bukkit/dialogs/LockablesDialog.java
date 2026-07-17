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
import de.sean.blockprot.bukkit.config.DefaultConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class LockablesDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xF0A0A0);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xD2B48C);

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
            boolean active = entry.activeCount > 0;
            TextColor c = active ? PASTEL_MINT : PASTEL_CORAL;
            String catLabel = translateCategory(entry.label);
            buttons.add(new DialogButton("cat_" + entry.label,
                Component.text()
                    .append(Component.text(stripColor(Translator.get(active ? TranslationKey.ICON__TOGGLE_ON : TranslationKey.ICON__TOGGLE_OFF)), c))
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
        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            backHint(exitOrigin),
            backAction(player, exitOrigin)
        );

        bridge.showMultiAction(player, title, body, buttons, backBtn, 1);
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
            else if (n.contains("FENCE_GATE")) dest = catMap.get("Gates");
            else if ((n.contains("FURNACE") || n.equals("BLAST_FURNACE") || n.equals("SMOKER")) && m.isBlock()) dest = catMap.get("Furnaces");
            else if ((n.contains("CHEST") || n.equals("ENDER_CHEST")) && m.isBlock() && !n.contains("BOAT")) dest = catMap.get("Chests");
            else if (n.endsWith("_SIGN") || n.endsWith("_WALL_SIGN") || n.endsWith("_HANGING_SIGN")) dest = catMap.get("Signs");
            else if ((n.equals("BARREL") || n.equals("COMPOSTER") || n.equals("DECORATED_POT") || n.equals("CHISELED_BOOKSHELF") || n.equals("BEACON")) && m.isBlock()) dest = catMap.get("Storage");
            else if ((n.contains("CRAFTING") || n.contains("CARTOGRAPHY") || n.contains("GRINDSTONE") || n.contains("STONECUTTER") || n.contains("SMITHING") || n.contains("LOOM") || n.contains("BREWING_STAND") || n.contains("CAULDRON") || n.contains("ENCHANTING_TABLE") || n.contains("ANVIL")) && m.isBlock()) dest = catMap.get("Workstations");
            else if ((n.contains("BUTTON") || n.contains("LEVER") || n.contains("DAYLIGHT_DETECTOR") || n.endsWith("_BED") || n.equals("JUKEBOX") || n.equals("NOTE_BLOCK") || n.equals("BELL") || n.equals("DISPENSER") || n.equals("DROPPER") || n.equals("HOPPER") || n.equals("OBSERVER") || n.equals("TARGET") || n.contains("PRESSURE_PLATE")) && m.isBlock()) dest = catMap.get("Interactive");
            else if (cfg.isLockableEntity(m)) dest = catMap.get("Entities");
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

    private static String translateCategory(String label) {
        return switch (label) {
            case "Chests" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__CHESTS));
            case "Shulkers" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__SHULKERS));
            case "Furnaces" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__FURNACES));
            case "Storage" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__STORAGE));
            case "Signs" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__SIGNS));
            case "Doors" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__DOORS));
            case "Trapdoors" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__TRAPDOORS));
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

    private static Component backHint(DialogOrigin origin) {
        switch (origin) {
            case ADMIN_MENU: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_ADMIN_MENU)), TextColor.color(0x888888));
            default: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888));
        }
    }

    private static DialogButton.DialogClickHandler backAction(Player player, DialogOrigin origin) {
        switch (origin) {
            case ADMIN_MENU: return p -> AdminMenuDialog.show(p);
            default: return p -> {};
        }
    }

    private record CategoryEntry(String label, List<Material> materials, long activeCount, long totalCount) {}
}
