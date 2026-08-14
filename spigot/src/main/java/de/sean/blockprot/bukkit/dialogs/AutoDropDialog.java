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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dialog counterpart of {@code AutoDropInventory}: lists the families eligible
 * for auto_drop_to_inventory and drills into {@link AutoDropFamilyDialog} per family.
 */
public final class AutoDropDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xF0A0A0);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xD2B48C);
    private static final TextColor PASTEL_ORANGE = TextColor.color(0xDFB98E);

    private static final List<BlockFamilyParser.Family> FAMILIES = List.of(
        BlockFamilyParser.Family.TILE_ENTITIES,
        BlockFamilyParser.Family.SHULKER_BOXES,
        BlockFamilyParser.Family.BLOCKS,
        BlockFamilyParser.Family.DOORS,
        BlockFamilyParser.Family.ENTITIES
    );

    private AutoDropDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        show(player, backOrigin, null);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin,
                            @Nullable DialogButton.DialogClickHandler parentBack) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        DefaultConfig cfg = BlockProt.getDefaultConfig();
        Set<Material> autoDropBlocks = cfg.getAutoDropToInventoryBlocks();

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__TITLE)),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__AUTO_DROP_LORE)), SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        for (BlockFamilyParser.Family family : FAMILIES) {
            Set<Material> members = BlockFamilyParser.getFamilyMembers(family);
            long active = members.stream().filter(autoDropBlocks::contains).count();
            long total = members.size();
            boolean noneActive = active == 0;
            TextColor c = stateColor(active, total);
            String label = friendlyName(family.name());

            buttons.add(new DialogButton("family_" + family.name(),
                Component.text()
                    .append(Component.text(stripColor(Translator.get(noneActive ? TranslationKey.ICON__TOGGLE_OFF : TranslationKey.ICON__TOGGLE_ON)), c))
                    .append(Component.text(label, NamedTextColor.WHITE))
                    .append(Component.text(" (" + active + "/" + total + ")", TextColor.color(0x888888)))
                    .build(),
                Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__LEFT_CLICK_HINT)), TextColor.color(0x888888)),
                p -> AutoDropFamilyDialog.show(p, backOrigin, family, parentBack)
            ));
        }

        DialogButton searchBtn = new DialogButton("search",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__SEARCH))
                + stripColor(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__SEARCH)), NamedTextColor.WHITE),
            Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__SEARCH_LORE)), TextColor.color(0x888888)),
            p -> {
                bridge.closeDialog(p);
                de.sean.blockprot.bukkit.inventories.AutoDropSearchInventory.startSearchFromDialog(p, backOrigin, parentBack);
            }
        );
        buttons.add(searchBtn);

        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            parentBack != null
                ? parentBack
                : backOrigin == DialogOrigin.ADMIN_MENU ? p -> AdminMenuDialog.show(p) : null
        );

        bridge.showMultiAction(player, title, body, buttons, backBtn, 1);
    }

    private static TextColor stateColor(long active, long total) {
        if (active == 0) return PASTEL_CORAL;
        if (active == total) return PASTEL_MINT;
        double ratio = (double) active / total;
        double halfBand = total <= 5 ? 0.20 : 0.10;
        return Math.abs(ratio - 0.5) <= halfBand ? PASTEL_ORANGE : PASTEL_MINT;
    }

    @NotNull
    private static String friendlyName(@NotNull String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }
}
