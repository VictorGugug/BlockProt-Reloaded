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
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.RedstoneSettingsHandler;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class BlockSettingsDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xF0A0A0);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xD2B48C);
    private static final TextColor SOFT_BLUE = TextColor.color(0xA0C4E8);
    private static final TextColor PASTEL_PURPLE = TextColor.color(0xC8A0E0);

    private BlockSettingsDialog() {}

    public static void show(@NotNull Player player, @NotNull Block block, @NotNull BlockNBTHandler handler) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        RedstoneSettingsHandler rs = handler.getRedstoneHandler();
        String materialName = formatMaterialName(block.getType().name());

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_SETTINGS__TITLE)),
            SOFT_BLUE, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(materialName, SOFT_GRAY)));

        List<DialogButton> actions = new ArrayList<>();
        actions.add(toggleBtn("redstone",
            TranslationKey.INVENTORIES__REDSTONE__REDSTONE_PROTECTION,
            rs.getCurrentProtection(),
            p -> {
                rs.setCurrentProtection(!rs.getCurrentProtection());
                handler.applyToOtherContainer();
                show(p, block, handler);
            }));
        actions.add(toggleBtn("hopper",
            TranslationKey.INVENTORIES__REDSTONE__HOPPER_PROTECTION,
            rs.getHopperProtection(),
            p -> {
                rs.setHopperProtection(!rs.getHopperProtection());
                handler.applyToOtherContainer();
                show(p, block, handler);
            }));
        actions.add(toggleBtn("piston",
            TranslationKey.INVENTORIES__REDSTONE__PISTON_PROTECTION,
            rs.getPistonProtection(),
            p -> {
                rs.setPistonProtection(!rs.getPistonProtection());
                handler.applyToOtherContainer();
                show(p, block, handler);
            }));
        actions.add(cmdBtn("enable_all",
            stripColor(Translator.get(TranslationKey.INVENTORIES__REDSTONE__ENABLE_ALL)),
            PASTEL_MINT,
            p -> {
                rs.setAll(true);
                handler.applyToOtherContainer();
                show(p, block, handler);
            }));
        actions.add(cmdBtn("disable_all",
            stripColor(Translator.get(TranslationKey.INVENTORIES__REDSTONE__DISABLE_ALL)),
            PASTEL_CORAL,
            p -> {
                rs.setAll(false);
                handler.applyToOtherContainer();
                show(p, block, handler);
            }));

        // Always returns to the parent BlockLockDialog: this is one level of internal
        // navigation within the same block menu, not an external-origin exit, so it must
        // not be gated by DialogBridgeFactory.resolveOrigin()/areExtraCommandsEnabled().
        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            p -> BlockLockDialog.show(p, block, handler)
        );

        bridge.showMultiAction(player, title, body, actions, exitBtn, 2);
    }

    private static DialogButton toggleBtn(String id, TranslationKey labelKey, boolean active,
                                           DialogButton.DialogClickHandler handler) {
        TextColor c = active ? PASTEL_MINT : PASTEL_CORAL;
        String label = stripColor(Translator.get(labelKey));
        return new DialogButton(id,
            Component.text()
                .append(Component.text(stripColor(Translator.get(active ? TranslationKey.ICON__TOGGLE_ON : TranslationKey.ICON__TOGGLE_OFF)), c))
                .append(Component.text(label, NamedTextColor.WHITE))
                .build(),
            Component.join(JoinConfiguration.newlines(),
                Component.text(label, SOFT_GRAY),
                Component.text(active
                    ? stripColor(Translator.get(TranslationKey.ENABLED))
                    : stripColor(Translator.get(TranslationKey.DISABLED)), c)),
            handler);
    }

    private static DialogButton cmdBtn(String id, String label, TextColor color, DialogButton.DialogClickHandler handler) {
        return new DialogButton(id, Component.text(label, color),
            Component.text(label, TextColor.color(0x888888)), handler);
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
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
