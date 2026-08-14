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
import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Worlds category of the dialog-based admin config editor: per-world config
 * toggle and the excluded worlds list.
 */
public final class AdminConfigWorldsDialog {

    private AdminConfigWorldsDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        Component title = Component.text(
            AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_WORLDS)),
            AdminConfigDialog.SOFT_BLUE, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_WORLDS), AdminConfigDialog.SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(AdminConfigDialog.toggleBtn("per_worlds_config", "per_worlds_config",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__WORLDS__PER_WORLDS_CONFIG_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__WORLDS__PER_WORLDS_CONFIG),
            cfg.isPerWorldsConfigEnabled(),
            p -> { cfg.setAndSave("per_worlds_config", !cfg.isPerWorldsConfigEnabled()); show(p, backOrigin); }));

        List<String> excludedList = cfg.getBukkitConfig().getStringList("excluded_worlds");
        String excludedJoined = excludedList.isEmpty()
            ? AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__WORLDS__EXCLUDED_WORLDS_EMPTY))
            : String.join(", ", excludedList);
        buttons.add(AdminConfigDialog.valueBtn("excluded_worlds", "excluded_worlds",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__WORLDS__EXCLUDED_WORLDS_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__WORLDS__EXCLUDED_WORLDS), excludedJoined,
            p -> AdminConfigValueDialog.openText(p, "excluded_worlds",
                AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__WORLDS__EXCLUDED_WORLDS_HINT)),
                String.join(", ", excludedList),
                raw -> null,
                v -> {
                    List<String> parsed = v.isBlank()
                        ? List.of()
                        : Arrays.stream(v.split(","))
                            .map(String::trim).filter(s -> !s.isEmpty()).toList();
                    cfg.setAndSave("excluded_worlds", parsed);
                    show(p, backOrigin);
                },
                () -> show(p, backOrigin))));

        AdminConfigDialog.bridgeReturn(player, bridge, title, body, buttons, backOrigin);
    }
}