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
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.BlockCountStatistic;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class StatsDialog {

    private static final TextColor PASTEL_GOLD = TextColor.color(0xD2B48C);
    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor PASTEL_PURPLE = TextColor.color(0xC8A0E0);

    private StatsDialog() {}

    static final String KEY_PLAYER = "player";
    static final String KEY_GLOBAL = "global";

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        show(player, backOrigin, KEY_PLAYER);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin, @NotNull String pageKey) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        boolean isPlayerPage = pageKey.equals(KEY_PLAYER);
        TranslationKey titleKey = isPlayerPage
            ? TranslationKey.INVENTORIES__STATISTICS__PLAYER_STATISTICS
            : TranslationKey.INVENTORIES__STATISTICS__GLOBAL_STATISTICS;

        Component title = Component.text(
            stripColor(Translator.get(titleKey)),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__STATS__HEADER)), SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.empty()));

        if (isPlayerPage) {
            PlayerBlocksStatistic pStat = new PlayerBlocksStatistic();
            StatHandler.getStatistic(pStat, player);
            int total = pStat.get().size();
            body.add(DialogBodyEntry.text(Component.text()
                .append(Component.text(
                    stripColor(Translator.get(TranslationKey.DIALOGS__STATS__YOUR_BLOCKS)), NamedTextColor.WHITE))
                .append(Component.text(String.valueOf(total), PASTEL_MINT, TextDecoration.BOLD))
                .build()));
            body.add(DialogBodyEntry.text(Component.empty()));

            List<String> breakdown = pStat.getBreakdownLore();
            if (!breakdown.isEmpty()) {
                body.add(DialogBodyEntry.text(Component.text(
                    stripColor(Translator.get(TranslationKey.DIALOGS__STATS__BREAKDOWN)), SOFT_GRAY)));
                for (String line : breakdown) {
                    String clean = line.replaceAll("[§&][0-9a-fk-orxA-F]", "");
                    body.add(DialogBodyEntry.text(Component.text("  " + clean, TextColor.color(0x888888))));
                }
            } else {
                body.add(DialogBodyEntry.text(Component.text(
                    stripColor(Translator.get(TranslationKey.DIALOGS__STATS__NO_BLOCKS)), TextColor.color(0x888888))));
            }
        } else {
            BlockCountStatistic cStat = new BlockCountStatistic();
            StatHandler.getStatistic(cStat);
            body.add(DialogBodyEntry.text(Component.text()
                .append(Component.text(
                    stripColor(Translator.get(TranslationKey.DIALOGS__STATS__SERVER_TOTAL)), NamedTextColor.WHITE))
                .append(Component.text(String.valueOf(cStat.get()), PASTEL_PURPLE, TextDecoration.BOLD))
                .build()));
        }

        String toggleLabel = isPlayerPage
            ? stripColor(Translator.get(TranslationKey.INVENTORIES__STATISTICS__GLOBAL_STATISTICS))
            : stripColor(Translator.get(TranslationKey.INVENTORIES__STATISTICS__PLAYER_STATISTICS));

        String nextPageKey = isPlayerPage ? KEY_GLOBAL : KEY_PLAYER;

        DialogButton toggleBtn = new DialogButton("toggle",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__STATS)) + toggleLabel, NamedTextColor.WHITE),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__SWITCH_VIEW)), TextColor.color(0x888888)),
            p -> show(p, backOrigin, nextPageKey)
        );

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            backHint(exitOrigin),
            backAction(player, exitOrigin)
        );

        bridge.showMultiAction(player, title, body, List.of(toggleBtn), backBtn, 1);
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }

    private static Component backHint(DialogOrigin origin) {
        switch (origin) {
            case USER_MENU: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_USER_MENU)), TextColor.color(0x888888));
            case ADMIN_MENU: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_ADMIN_MENU)), TextColor.color(0x888888));
            default: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888));
        }
    }

    private static DialogButton.DialogClickHandler backAction(Player player, DialogOrigin origin) {
        switch (origin) {
            case USER_MENU: return p -> UserMenuDialog.show(p);
            case ADMIN_MENU: return p -> AdminMenuDialog.show(p);
            default: return null;
        }
    }
}
