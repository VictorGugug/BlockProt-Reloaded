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
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_CORAL;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_GOLD;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_MINT;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_PURPLE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_BLUE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_GRAY;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class LockableCategoryDialog {

    private static final int PER_PAGE = 9;

    private LockableCategoryDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin,
                            @NotNull String categoryName, @NotNull List<Material> materials) {
        show(player, backOrigin, categoryName, materials, 0);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin,
                            @NotNull String categoryName, @NotNull List<Material> materials, int page) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        DefaultConfig cfg = BlockProt.getDefaultConfig();
        int totalPages = Math.max(1, (int) Math.ceil(materials.size() / (double) PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PER_PAGE;
        int to = Math.min(from + PER_PAGE, materials.size());
        List<Material> pageMats = materials.subList(from, to);

        Component title = Component.text(categoryName, PASTEL_GOLD, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(" " + categoryName + " ", NamedTextColor.WHITE))
            .build()));
        body.add(DialogBodyEntry.text(Component.text(
            Translator.get(TranslationKey.DIALOGS__PAGE)
                .replace("{current}", String.valueOf(safePage + 1))
                .replace("{total}", String.valueOf(totalPages)),
            TextColor.color(0x888888))));
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_ENABLE))
                + " / "
                + stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_DISABLE)),
            TextColor.color(0x888888))));
        List<DialogButton> buttons = new ArrayList<>();

        for (Material mat : pageMats) {
            boolean active = cfg.isLockable(mat) || cfg.isLockableEntity(mat);
            TextColor c = active ? PASTEL_MINT : PASTEL_CORAL;
            String displayName = formatMaterialName(mat.name());

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
                    cfg.toggleLockable(mat, p);
                    show(p, backOrigin, categoryName, materials, safePage);
                }
            ));
        }

        long activeCount = materials.stream().filter(m -> cfg.isLockable(m) || cfg.isLockableEntity(m)).count();
        boolean noneActive = activeCount == 0;
        TextColor familyColor = BpDialogStyles.stateColor(activeCount, materials.size());

        List<DialogButton> extraButtons = new ArrayList<>();
        extraButtons.add(new DialogButton("toggle_category",
            Component.text()
                .append(Component.text(stripColor(Translator.get(noneActive ? TranslationKey.ICON__TOGGLE_OFF : TranslationKey.ICON__TOGGLE_ON)), familyColor))
                .append(Component.text(categoryName, familyColor))
                .build(),
            Component.text(stripColor(Translator.get(noneActive ? TranslationKey.DIALOGS__CLICK_ENABLE : TranslationKey.DIALOGS__CLICK_DISABLE)), TextColor.color(0x888888)),
            p -> {
                boolean targetState = noneActive; // If none are active, we want to enable all. If any are active, we disable all.
                BlockProt.getDefaultConfig().batchSetLockable(materials, targetState, p);
                show(p, backOrigin, categoryName, materials, safePage);
            }
        ));

        List<DialogButton> navButtons = new ArrayList<>();
        if (safePage > 0) {
            navButtons.add(new DialogButton("prev",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, categoryName, materials, safePage - 1)));
        }
        if (safePage + 1 < totalPages) {
            navButtons.add(new DialogButton("next",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, categoryName, materials, safePage + 1)));
        }

        buttons.addAll(navButtons);
        buttons.addAll(extraButtons);

        // Always returns to the parent LockablesDialog (category list): this is one level
        // of internal navigation within the same Lockables feature, not an external-origin
        // exit, so it must not be gated by DialogBridgeFactory.resolveOrigin()/
        // areExtraCommandsEnabled(). Only LockablesDialog's own back button (the outermost
        // level of this feature) decides whether to exit to an external menu or close.
        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            p -> LockablesDialog.show(p, backOrigin)
        );

        bridge.showMultiAction(player, title, body, buttons, backBtn, 3);
    }

    static String formatMaterialName(String name) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') {
                sb.append(' ');
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}
