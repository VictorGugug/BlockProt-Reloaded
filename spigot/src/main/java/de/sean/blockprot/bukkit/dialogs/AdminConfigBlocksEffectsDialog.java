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

public final class AdminConfigBlocksEffectsDialog {

    private AdminConfigBlocksEffectsDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        Component title = Component.text(
            AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__EFFECTS_TITLE)),
            AdminConfigDialog.PASTEL_CORAL, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__EFFECTS_TITLE)),
            AdminConfigDialog.SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(AdminConfigDialog.toggleBtn("block_lock_effects", "block_lock_effects",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__LOCK_EFFECTS_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__LOCK_EFFECTS),
            cfg.isLockEffectEnabled(),
            p -> { cfg.setLockEffects(!cfg.isLockEffectEnabled()); show(p, backOrigin); }));
        buttons.add(AdminConfigDialog.toggleBtn("block_lock_sounds", "block_lock_sounds",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__LOCK_SOUNDS_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__LOCK_SOUNDS),
            cfg.isLockSoundEnabled(),
            p -> { cfg.setLockSounds(!cfg.isLockSoundEnabled()); show(p, backOrigin); }));
        boolean useMenus = cfg.getBukkitConfig().getBoolean("use_menus", false);
        buttons.add(AdminConfigDialog.dialogToggleBtn("use_menus", "use_menus [Dialog]",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__USE_MENUS_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__USE_MENUS_DE),
            useMenus,
            p -> { cfg.setAndSave("use_menus", !useMenus); show(p, backOrigin); }));
        buttons.add(AdminConfigDialog.dialogToggleBtn("use_dialogs", "use_dialogs [Dialog]",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__USE_DIALOGS_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__USE_DIALOGS_DE),
            cfg.isDialogsEnabled(),
            p -> { cfg.setAndSave("use_dialogs", !cfg.isDialogsEnabled()); show(p, backOrigin); }));
        int timedAccessDays = cfg.getBukkitConfig().getInt("timed_access_max_duration_days", 90);
        buttons.add(AdminConfigDialog.valueBtn("timed_access_max_duration_days", "timed_access_max_duration_days",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__TIMED_ACCESS_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__TIMED_ACCESS),
            String.valueOf(timedAccessDays),
            p -> AdminConfigValueDialog.openInt(p, "timed_access_max_duration_days",
                AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__TIMED_ACCESS_HINT)), timedAccessDays,
                v -> { cfg.setAndSave("timed_access_max_duration_days", v); show(p, backOrigin); },
                () -> show(p, backOrigin))));

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