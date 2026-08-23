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
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Dialog counterpart of {@code AutoDropFamilyInventory}: per-material toggles for one family. */
public final class AutoDropFamilyDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xF0A0A0);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xD2B48C);

    private static final int PER_PAGE = 9;

    private AutoDropFamilyDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin,
                            @NotNull BlockFamilyParser.Family family) {
        show(player, backOrigin, family, 0, null);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin,
                            @NotNull BlockFamilyParser.Family family,
                            @Nullable DialogButton.DialogClickHandler parentBack) {
        show(player, backOrigin, family, 0, parentBack);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin,
                            @NotNull BlockFamilyParser.Family family, int page) {
        show(player, backOrigin, family, page, null);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin,
                            @NotNull BlockFamilyParser.Family family, int page,
                            @Nullable DialogButton.DialogClickHandler parentBack) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        DefaultConfig cfg = BlockProt.getDefaultConfig();
        Set<Material> autoDropBlocks = cfg.getAutoDropToInventoryBlocks();
        List<Material> materials = new ArrayList<>(BlockFamilyParser.getFamilyMembers(family));
        materials.sort((a, b) -> a.name().compareTo(b.name()));

        int totalPages = Math.max(1, (int) Math.ceil(materials.size() / (double) PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PER_PAGE;
        int to = Math.min(from + PER_PAGE, materials.size());
        List<Material> pageMats = materials.subList(from, to);

        String familyLabel = friendlyName(family.name());
        Component title = Component.text(familyLabel, PASTEL_GOLD, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PAGE))
                .replace("{current}", String.valueOf(safePage + 1))
                .replace("{total}", String.valueOf(totalPages)),
            TextColor.color(0x888888))));

        List<DialogButton> buttons = new ArrayList<>();
        for (Material mat : pageMats) {
            boolean active = autoDropBlocks.contains(mat);
            TextColor c = active ? PASTEL_MINT : PASTEL_CORAL;
            String displayName = LockableCategoryDialog.formatMaterialName(mat.name());

            buttons.add(new DialogButton("mat_" + mat.name(),
                Component.text()
                    .append(Component.text(stripColor(Translator.get(active ? TranslationKey.ICON__TOGGLE_ON : TranslationKey.ICON__TOGGLE_OFF)), c))
                    .append(Component.text(displayName, NamedTextColor.WHITE))
                    .build(),
                Component.join(JoinConfiguration.newlines(),
                    Component.text(mat.name(), SOFT_GRAY),
                    Component.text(active
                        ? stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_DISABLE_SINGLE))
                        : stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_ENABLE_SINGLE)), c)),
                p -> {
                    cfg.toggleAutoDropMaterial(mat, p);
                    show(p, backOrigin, family, safePage, parentBack);
                }
            ));
        }

        long activeCount = materials.stream().filter(autoDropBlocks::contains).count();
        boolean noneActive = activeCount == 0;
        TextColor familyColor = BpDialogStyles.stateColor(activeCount, materials.size());

        List<DialogButton> extraButtons = new ArrayList<>();
        extraButtons.add(new DialogButton("toggle_family",
            Component.text()
                .append(Component.text(stripColor(Translator.get(noneActive ? TranslationKey.ICON__TOGGLE_OFF : TranslationKey.ICON__TOGGLE_ON)), familyColor))
                .append(Component.text(familyLabel, familyColor))
                .build(),
            Component.text(stripColor(Translator.get(noneActive ? TranslationKey.DIALOGS__CLICK_ENABLE : TranslationKey.DIALOGS__CLICK_DISABLE)), TextColor.color(0x888888)),
            p -> {
                cfg.toggleAutoDropFamily(family, p);
                show(p, backOrigin, family, safePage, parentBack);
            }
        ));

        List<DialogButton> navButtons = new ArrayList<>();
        if (safePage > 0) {
            navButtons.add(new DialogButton("prev",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, family, safePage - 1, parentBack)));
        }
        if (safePage + 1 < totalPages) {
            navButtons.add(new DialogButton("next",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, family, safePage + 1, parentBack)));
        }

        buttons.addAll(navButtons);
        buttons.addAll(extraButtons);

        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            p -> AutoDropDialog.show(p, backOrigin, parentBack)
        );

        bridge.showMultiAction(player, title, body, buttons, backBtn, 3);
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
