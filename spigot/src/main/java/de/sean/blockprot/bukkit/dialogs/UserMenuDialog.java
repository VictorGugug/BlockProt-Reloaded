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
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class UserMenuDialog {

    private static final TextColor PASTEL_GOLD = TextColor.color(0xD2B48C);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor SOFT_BLUE = TextColor.color(0xA0C4E8);
    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_PURPLE = TextColor.color(0xC8A0E0);

    private UserMenuDialog() {}

    public static void show(@NotNull Player player) {
        show(player, DialogOrigin.NONE);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__TITLE)),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        String settings = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__SETTINGS));
        String friends = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__FRIENDS));
        String stats = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__STATS));
        String about = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__ABOUT));

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__USER_MENU__HEADER)), SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__CHOOSE_OPTION)), TextColor.color(0x888888))));

        DialogButton settingsBtn = new DialogButton("settings",
            Component.text("⚙ " + settings, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__SETTINGS)), PASTEL_MINT),
            p -> {
                new PlayerSettingsHandler(p).setHasPlayerInteractedWithMenu(true);
                UserSettingsDialog.show(p);
            }
        );

        DialogButton friendsBtn = new DialogButton("friends",
            Component.text("👤 " + friends, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__FRIENDS)), SOFT_BLUE),
            p -> FriendManageDialog.show(p)
        );

        DialogButton statsBtn = new DialogButton("stats",
            Component.text("📊 " + stats, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__STATS)), PASTEL_PURPLE),
            p -> StatsDialog.show(p, DialogOrigin.USER_MENU)
        );

        DialogButton aboutBtn = new DialogButton("about",
            Component.text("ℹ " + about, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__ABOUT)), PASTEL_GOLD),
            p -> AboutDialog.show(p)
        );

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            originHint(exitOrigin),
            originBack(player, exitOrigin)
        );

        List<DialogButton> actions = new ArrayList<>();
        actions.add(settingsBtn);
        actions.add(friendsBtn);
        actions.add(statsBtn);
        actions.add(aboutBtn);
        bridge.showMultiAction(player, title, body, actions, exitBtn, 2);
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orx]", "");
    }

    private static Component tooltip(String description, TextColor accent) {
        return Component.join(
            JoinConfiguration.newlines(),
            Component.text(description, accent),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_TO_OPEN)), TextColor.color(0x888888))
        );
    }

    private static Component originHint(DialogOrigin origin) {
        switch (origin) {
            case ADMIN_MENU: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_ADMIN_MENU)), TextColor.color(0x888888));
            default: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888));
        }
    }

    static DialogButton.DialogClickHandler originBack(Player player, DialogOrigin origin) {
        switch (origin) {
            case ADMIN_MENU: return p -> AdminMenuDialog.show(p);
            default: return p -> {};
        }
    }
}
