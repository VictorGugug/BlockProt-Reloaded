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
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class InfoDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor SOFT_BLUE = TextColor.color(0xA0C4E8);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xD2B48C);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);

    private static final int PER_PAGE = 7;

    private InfoDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        show(player, backOrigin, 0);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin, int page) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        List<PlayerEntry> players = buildPlayerList();
        if (players.isEmpty()) {
            bridge.showNotice(player,
                Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__PLAYER_LIST__TITLE)),
                    SOFT_BLUE, TextDecoration.BOLD),
                List.of(
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__INFO__HEADER)), SOFT_GRAY),
                    Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__PLAYER_LIST__EMPTY)),
                        TextColor.color(0x888888))
                ),
                new DialogButton("back",
                    Component.text(stripColor(Translator.get(
                        DialogBridgeFactory.resolveOrigin(backOrigin) == DialogOrigin.NONE
                            ? TranslationKey.DIALOGS__CLOSE
                            : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
                    backHint(DialogBridgeFactory.resolveOrigin(backOrigin)),
                    backAction(player, DialogBridgeFactory.resolveOrigin(backOrigin)))
            );
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil(players.size() / (double) PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PER_PAGE;
        int to = Math.min(from + PER_PAGE, players.size());
        List<PlayerEntry> pageEntries = players.subList(from, to);

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__PLAYER_LIST__TITLE)),
            SOFT_BLUE, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__INFO__HEADER)), SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PAGE))
                .replace("{current}", String.valueOf(safePage + 1))
                .replace("{total}", String.valueOf(totalPages)),
            TextColor.color(0x888888))));

        List<DialogButton> buttons = new ArrayList<>();

        for (PlayerEntry entry : pageEntries) {
            buttons.add(new DialogButton("player_" + entry.name,
                Component.text(stripColor(Translator.get(TranslationKey.ICON__INFO)) + entry.name, NamedTextColor.WHITE),
                Component.join(JoinConfiguration.newlines(),
                    Component.text(entry.blockCount + stripColor(Translator.get(TranslationKey.DIALOGS__INFO__PROTECTED_BLOCKS)), SOFT_GRAY),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_CHAT)),
                        TextColor.color(0x888888))),
                p -> UnlockDialog.show(p, DialogOrigin.INFO, entry.name, 0)
            ));
        }

        List<DialogButton> navButtons = new ArrayList<>();
        if (safePage > 0) {
            int prev = safePage - 1;
            navButtons.add(new DialogButton("prev",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, prev)));
        }
        if (safePage + 1 < totalPages) {
            int next = safePage + 1;
            navButtons.add(new DialogButton("next",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, next)));
        }
        buttons.addAll(navButtons);

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            backHint(exitOrigin),
            backAction(player, exitOrigin)
        );

        bridge.showMultiAction(player, title, body, buttons, backBtn, 2);
    }

    private static @NotNull List<PlayerEntry> buildPlayerList() {
        List<PlayerEntry> result = new ArrayList<>();
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op.getName() == null) continue;
            PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
            StatHandler.getStatisticByUuid(stat, op.getUniqueId());
            int count = stat.get().size();
            if (count > 0) {
                result.add(new PlayerEntry(op.getUniqueId(), op.getName(), count));
            }
        }
        result.sort(Comparator.comparingInt(PlayerEntry::blockCount).reversed());
        return result;
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }

    private static Component backHint(DialogOrigin origin) {
        switch (origin) {
            case ADMIN_MENU: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_ADMIN_MENU)), TextColor.color(0x888888));
            default: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888));
        }
    }

    private static DialogButton.DialogClickHandler backAction(Player player, DialogOrigin origin) {
        switch (origin) {
            case ADMIN_MENU: return p -> AdminMenuDialog.show(p);
            default: return p -> {};
        }
    }

    private record PlayerEntry(UUID uuid, String name, int blockCount) {}
}
