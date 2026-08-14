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

/**
 * Raid detection category of the dialog-based admin config editor.
 */
public final class AdminConfigRaidDialog {

    private AdminConfigRaidDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Component title = Component.text(
            AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_RAID)),
            AdminConfigDialog.PASTEL_CORAL, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_RAID), AdminConfigDialog.SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        boolean raidEnabled = BlockProt.getInstance().getConfig().getBoolean("raid_detection.enabled", false);
        buttons.add(AdminConfigDialog.toggleBtn("raid_detection.enabled", "raid_detection.enabled",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__RAID__ENABLED_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__RAID__ENABLED),
            raidEnabled,
            p -> { cfg.setAndSave("raid_detection.enabled", !raidEnabled); show(p, backOrigin); }));

        AdminConfigDialog.bridgeReturn(player, bridge, title, body, buttons, backOrigin);
    }
}