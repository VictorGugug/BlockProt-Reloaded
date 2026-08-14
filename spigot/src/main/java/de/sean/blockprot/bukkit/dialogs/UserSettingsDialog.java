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

import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.listeners.BlockEventListener;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class UserSettingsDialog {

    private UserSettingsDialog() {}

    public static void show(@NotNull Player player) {
        show(player, DialogOrigin.USER_MENU);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        PlayerSettingsHandler settings = new PlayerSettingsHandler(player);

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__USER_SETTINGS)),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        boolean hintsEnabled = !settings.hasPlayerInteractedWithMenu();

        String rawLock = stripColor(Translator.get(TranslationKey.INVENTORIES__LOCK_ON_PLACE));
        String rawHints = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__HINTS));
        String rawNotif = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_SETTINGS_NOTIFICATIONS));

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__SETTINGS__HEADER)), SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.empty()));

        String autoLockDesc = stripColor(Translator.get(TranslationKey.DIALOGS__SETTINGS__AUTO_LOCK_DESC));
        String hintsDesc = stripColor(Translator.get(TranslationKey.DIALOGS__SETTINGS__HINTS_DESC));
        String notifDesc = stripColor(Translator.get(TranslationKey.DIALOGS__SETTINGS__NOTIFICATIONS_DESC));

        DialogButton lockBtn = new DialogButton("lock",
            toggleLabel(rawLock, settings.getLockOnPlace(), PASTEL_MINT, PASTEL_CORAL),
            tooltip(rawLock, settings.getLockOnPlace(), autoLockDesc),
            p -> {
                PlayerSettingsHandler h = new PlayerSettingsHandler(p);
                h.setLockOnPlace(!h.getLockOnPlace());
                BlockEventListener.invalidateSettings(p.getUniqueId());
                show(p, backOrigin);
            }
        );

        DialogButton hintsBtn = new DialogButton("hints",
            toggleLabel(rawHints, hintsEnabled, PASTEL_MINT, PASTEL_CORAL),
            tooltip(rawHints, hintsEnabled, hintsDesc),
            p -> {
                PlayerSettingsHandler h = new PlayerSettingsHandler(p);
                h.setHasPlayerInteractedWithMenu(hintsEnabled);
                show(p, backOrigin);
            }
        );

        DialogButton notifBtn = new DialogButton("notifications",
            toggleLabel(rawNotif, settings.getNotificationsEnabled(), PASTEL_MINT, PASTEL_CORAL),
            tooltip(rawNotif, settings.getNotificationsEnabled(), notifDesc),
            p -> {
                PlayerSettingsHandler h = new PlayerSettingsHandler(p);
                h.setNotificationsEnabled(!h.getNotificationsEnabled());
                show(p, backOrigin);
            }
        );

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            originHint(exitOrigin),
            originBack(player, exitOrigin)
        );

        List<DialogButton> actions = new ArrayList<>();
        actions.add(lockBtn);
        actions.add(hintsBtn);
        actions.add(notifBtn);

        if (de.sean.blockprot.bukkit.BlockProt.getDefaultConfig().isDialogsEnabled()) {
            String rawDialogs = stripColor(Translator.get(TranslationKey.DIALOGS__SETTINGS__PREFER_DIALOGS));
            String dialogsDesc = stripColor(Translator.get(TranslationKey.DIALOGS__SETTINGS__PREFER_DIALOGS_DESC));
            boolean preferDialogs = settings.getPreferDialogs();
            DialogButton dialogsBtn = new DialogButton("prefer_dialogs",
                toggleLabel(rawDialogs, preferDialogs, PASTEL_MINT, PASTEL_CORAL),
                tooltip(rawDialogs, preferDialogs, dialogsDesc),
                p -> {
                    PlayerSettingsHandler h = new PlayerSettingsHandler(p);
                    boolean nextState = !h.getPreferDialogs();
                    h.setPreferDialogs(nextState);
                    if (!nextState) {
                        p.openInventory(new de.sean.blockprot.bukkit.inventories.UserSettingsInventory().fill(p));
                    } else {
                        show(p, backOrigin);
                    }
                }
            );
            actions.add(dialogsBtn);
        }

        bridge.showMultiAction(player, title, body, actions, exitBtn, 1);
    }

    static Component originHint(DialogOrigin origin) {
        switch (origin) {
            case USER_MENU: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_USER_MENU)), TextColor.color(0x888888));
            default: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888));
        }
    }

    static DialogButton.DialogClickHandler originBack(Player player, DialogOrigin origin) {
        switch (origin) {
            case USER_MENU: return p -> UserMenuDialog.show(p);
            default: return null;
        }
    }

    private static Component toggleLabel(String name, boolean enabled,
                                         TextColor onColor, TextColor offColor) {
        TextColor color = enabled ? onColor : offColor;
        return Component.text()
            .append(Component.text(stripColor(Translator.get(enabled ? TranslationKey.ICON__TOGGLE_ON : TranslationKey.ICON__TOGGLE_OFF)), color))
            .append(Component.text(name, NamedTextColor.WHITE))
            .build();
    }

    private static Component tooltip(String name, boolean enabled, String description) {
        String status = enabled
            ? stripColor(Translator.get(TranslationKey.DIALOGS__STATUS_ENABLED))
            : stripColor(Translator.get(TranslationKey.DIALOGS__STATUS_DISABLED));
        return Component.join(
            JoinConfiguration.newlines(),
            Component.text(name, SOFT_BLUE, TextDecoration.BOLD),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__STATUS_LABEL)), SOFT_GRAY)
                .append(Component.text(status, enabled ? PASTEL_MINT : PASTEL_CORAL)),
            Component.text(description, TextColor.color(0x888888))
        );
    }
}
