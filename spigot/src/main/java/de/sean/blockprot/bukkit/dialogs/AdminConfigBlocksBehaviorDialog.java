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
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AdminConfigBlocksBehaviorDialog {

    private AdminConfigBlocksBehaviorDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        Component title = Component.text(
            AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__BEHAVIOR_TITLE)),
            AdminConfigDialog.PASTEL_CORAL, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__BEHAVIOR_TITLE)),
            AdminConfigDialog.SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(AdminConfigDialog.toggleBtn("modern_family_blocks", "modern_family_blocks",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__MODERN_FAMILY_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__MODERN_FAMILY),
            cfg.isModernFamilyBlocks(),
            p -> {
                boolean toModern = !cfg.isModernFamilyBlocks();
                cfg.setAndSave("modern_family_blocks", toModern);
                cfg.convertBlocksFileFormat(toModern);
                show(p, backOrigin);
            }));
        buttons.add(AdminConfigDialog.toggleBtn("redstone_disallowed_by_default", "redstone_disallowed_by_default",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__REDSTONE_DISALLOWED_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__REDSTONE_DISALLOWED),
            cfg.disallowRedstoneOnPlace(),
            p -> { cfg.setRedstoneDisallowedByDefault(!cfg.disallowRedstoneOnPlace()); show(p, backOrigin); }));
        buttons.add(AdminConfigDialog.toggleBtn("simplified_hopper_logic", "simplified_hopper_logic",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__SIMPLIFIED_HOPPER_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__SIMPLIFIED_HOPPER),
            cfg.isSimplifiedHopperLogic(),
            p -> { cfg.setSimplifiedHopperLogic(!cfg.isSimplifiedHopperLogic()); show(p, backOrigin); }));

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = new DialogButton("back",
            Component.text(AdminConfigDialog.stripColor(Translator.get(
                exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), AdminConfigDialog.SOFT_GRAY),
            exitOrigin == DialogOrigin.NONE ?
                Component.text(AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__CLOSE)), TextColor.color(0x888888)) :
                Component.text(AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__RETURN_CATEGORIES)), TextColor.color(0x888888)),
            exitOrigin == DialogOrigin.NONE ? null : p -> AdminConfigBlocksDialog.show(p, backOrigin));

        bridge.showMultiAction(player, title, body, buttons, backBtn, 2);
    }
}