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
 * Maintenance category of the dialog-based admin config editor.
 */
public final class AdminConfigMaintenanceDialog {

    private AdminConfigMaintenanceDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        Component title = Component.text(
            AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_MAINTENANCE)),
            AdminConfigDialog.PASTEL_GOLD, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_MAINTENANCE), AdminConfigDialog.SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        int inactivityDays = cfg.getBukkitConfig().getInt("inactivity_cleanup_days", -1);
        buttons.add(AdminConfigDialog.valueBtn("inactivity_cleanup_days", "inactivity_cleanup_days",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__INACTIVITY_CLEANUP_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__INACTIVITY_CLEANUP),
            String.valueOf(inactivityDays),
            p -> AdminConfigValueDialog.openInt(p, "inactivity_cleanup_days",
                AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__INACTIVITY_CLEANUP_HINT)), inactivityDays,
                v -> { cfg.setAndSave("inactivity_cleanup_days", v); show(p, backOrigin); },
                () -> show(p, backOrigin))));
        buttons.add(AdminConfigDialog.toggleBtn("auto_reload_configs", "auto_reload_configs",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__AUTO_RELOAD_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__AUTO_RELOAD),
            cfg.isAutoReloadEnabled(),
            p -> { cfg.setAutoReloadConfigs(!cfg.isAutoReloadEnabled()); show(p, backOrigin); }));
        int delay = cfg.getAutoReloadDelaySeconds();
        buttons.add(AdminConfigDialog.valueBtn("auto_reload_delay_seconds", "auto_reload_delay_seconds",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__AUTO_RELOAD_DELAY_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__AUTO_RELOAD_DELAY),
            String.valueOf(delay),
            p -> AdminConfigValueDialog.openInt(p, "auto_reload_delay_seconds",
                AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__AUTO_RELOAD_DELAY_HINT)), delay,
                v -> {
                    int clamped = Math.max(0, Math.min(5, v));
                    cfg.setAutoReloadDelaySeconds(clamped);
                    show(p, backOrigin);
                },
                () -> show(p, backOrigin))));
        buttons.add(AdminConfigDialog.toggleBtn("enable_session_log", "enable_session_log",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__SESSION_LOG_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__SESSION_LOG),
            cfg.isSessionLogEnabled(),
            p -> { cfg.setSessionLogEnabled(!cfg.isSessionLogEnabled()); show(p, backOrigin); }));
        buttons.add(AdminConfigDialog.toggleBtn("enable_backups", "enable_backups",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__BACKUPS_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__BACKUPS),
            cfg.isBackupsEnabled(),
            p -> { cfg.setBackupsEnabled(!cfg.isBackupsEnabled()); show(p, backOrigin); }));

        AdminConfigDialog.bridgeReturn(player, bridge, title, body, buttons, backOrigin);
    }
}