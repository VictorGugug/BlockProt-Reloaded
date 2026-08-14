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
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class EntityBlockSettingsDialog {

    private EntityBlockSettingsDialog() {}

    public static void show(@NotNull Player player, @NotNull Entity entity, @NotNull EntityNBTHandler handler) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_SETTINGS__TITLE)),
            SOFT_BLUE, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            formatMaterialName(entity.getType().name()), SOFT_GRAY)));

        boolean hopperEnabled = handler.isHopperProtectionEnabled();
        String label = stripColor(Translator.get(TranslationKey.INVENTORIES__REDSTONE__HOPPER_PROTECTION));

        List<DialogButton> actions = new ArrayList<>();
        actions.add(new DialogButton("hopper",
            Component.text()
                .append(Component.text(stripColor(Translator.get(hopperEnabled ? TranslationKey.ICON__TOGGLE_ON : TranslationKey.ICON__TOGGLE_OFF)), hopperEnabled ? PASTEL_MINT : PASTEL_CORAL))
                .append(Component.text(label, NamedTextColor.WHITE))
                .build(),
            Component.join(JoinConfiguration.newlines(),
                Component.text(label, SOFT_GRAY),
                Component.text(hopperEnabled
                    ? stripColor(Translator.get(TranslationKey.ENABLED))
                    : stripColor(Translator.get(TranslationKey.DISABLED)),
                    hopperEnabled ? PASTEL_MINT : PASTEL_CORAL)),
            p -> {
                handler.setHopperProtectionEnabled(!hopperEnabled);
                show(p, entity, handler);
            }));

        // Always returns to the parent BlockLockDialog: this is one level of internal
        // navigation within the same entity menu, not an external-origin exit, so it must
        // not be gated by DialogBridgeFactory.resolveOrigin()/areExtraCommandsEnabled().
        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            p -> BlockLockDialog.showForEntity(player, entity, handler));

        bridge.showMultiAction(player, title, body, actions, exitBtn, 2);
    }

    private static String formatMaterialName(String name) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') { sb.append(' '); nextUpper = true; }
            else if (nextUpper) { sb.append(Character.toUpperCase(c)); nextUpper = false; }
            else { sb.append(Character.toLowerCase(c)); }
        }
        return sb.toString();
    }
}
