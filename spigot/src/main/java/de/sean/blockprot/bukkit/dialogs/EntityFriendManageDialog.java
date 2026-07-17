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
import de.sean.blockprot.bukkit.inventories.AnvilInput;
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class EntityFriendManageDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor SOFT_BLUE = TextColor.color(0xA0C4E8);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xF0A0A0);

    private static final int PER_PAGE = 8;

    private EntityFriendManageDialog() {}

    public static void show(@NotNull Player player, @NotNull Entity entity, @NotNull EntityNBTHandler handler) {
        show(player, entity, handler, 0);
    }

    public static void show(@NotNull Player player, @NotNull Entity entity, @NotNull EntityNBTHandler handler, int page) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        List<String> friends = handler.getFriendUuids();
        int totalPages = Math.max(1, (int) Math.ceil(friends.size() / (double) PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PER_PAGE;
        int to = Math.min(from + PER_PAGE, friends.size());
        List<String> pageFriends = friends.subList(from, to);

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__MANAGE)),
            SOFT_BLUE, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        if (pageFriends.isEmpty()) {
            body.add(DialogBodyEntry.text(Component.text(
                stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__NO_HISTORY)),
                TextColor.color(0x888888))));
        } else {
            body.add(DialogBodyEntry.text(Component.text(
                stripColor(Translator.get(TranslationKey.DIALOGS__PAGE))
                    .replace("{current}", String.valueOf(safePage + 1))
                    .replace("{total}", String.valueOf(totalPages)),
                TextColor.color(0x888888))));
            for (String friendUuid : pageFriends) {
                String name = getPlayerName(friendUuid);
                boolean isManager = handler.isManager(friendUuid);
                body.add(DialogBodyEntry.text(Component.text()
                    .append(Component.text(isManager ? "● " : "○ ", isManager ? PASTEL_MINT : PASTEL_CORAL))
                    .append(Component.text(name != null ? name : friendUuid, NamedTextColor.WHITE))
                    .append(Component.text(isManager ? stripColor(Translator.get(TranslationKey.DIALOGS__ENTITY__MANAGER_SUFFIX)) : "", SOFT_GRAY))
                    .build()));
            }
        }

        List<DialogButton> buttons = new ArrayList<>();

        buttons.add(new DialogButton("add",
            Component.text("➕ " + stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__SEARCH)),
                NamedTextColor.WHITE),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__MANAGE_HINT)),
                TextColor.color(0x888888)),
            p -> {
                p.closeInventory();
                Consumer<String> handleName = text -> {
                    Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), () -> {
                        @SuppressWarnings("deprecation")
                        OfflinePlayer target = Bukkit.getOfflinePlayer(text);
                        if (target.getName() != null && target.hasPlayedBefore()) {
                            handler.addFriend(target.getUniqueId().toString());
                            Bukkit.getScheduler().runTask(BlockProt.getInstance(), () -> show(p, entity, handler));
                        } else {
                            p.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(
                                Translator.get(TranslationKey.MESSAGES__FRIEND_PLAYER_NOT_FOUND)));
                        }
                    });
                };
                AnvilInput.open(p, BlockProt.getInstance(), "",
                    Translator.get(TranslationKey.INVENTORIES__FRIENDS__SEARCH), handleName);
            }
        ));

        if (safePage > 0) {
            int prev = safePage - 1;
            buttons.add(new DialogButton("prev",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                p -> show(p, entity, handler, prev)));
        }
        if (safePage + 1 < totalPages) {
            int next = safePage + 1;
            buttons.add(new DialogButton("next",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                p -> show(p, entity, handler, next)));
        }

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(DialogOrigin.NONE);
        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(exitOrigin != DialogOrigin.NONE ? TranslationKey.DIALOGS__BACK : TranslationKey.DIALOGS__CLOSE)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            exitOrigin != DialogOrigin.NONE ? p -> BlockLockDialog.showForEntity(player, entity, handler) : p -> {});

        bridge.showMultiAction(player, title, body, buttons, exitBtn, 2);
    }

    private static String getPlayerName(String uuidStr) {
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
            if (op.getName() != null) return op.getName();
        } catch (IllegalArgumentException ignored) {}
        return null;
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }
}
