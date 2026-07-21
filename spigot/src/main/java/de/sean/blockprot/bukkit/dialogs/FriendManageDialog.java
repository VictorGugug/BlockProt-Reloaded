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
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.inventories.AnvilInput;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.FriendHandler;
import de.sean.blockprot.bukkit.nbt.FriendSupportingHandler;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.LocationListEntry;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class FriendManageDialog {

    private static final TextColor PASTEL_GOLD = TextColor.color(0xD2B48C);
    private static final TextColor SOFT_BLUE = TextColor.color(0xA0C4E8);
    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xF0A0A0);
    private static final TextColor PASTEL_PURPLE = TextColor.color(0xC8A0E0);

    private FriendManageDialog() {}

    public static void showForBlock(@NotNull Player player, @NotNull Block block, @NotNull BlockNBTHandler handler) {
        showForBlock(player, block, handler, 0);
    }

    public static void showForBlock(@NotNull Player player, @NotNull Block block, @NotNull BlockNBTHandler handler, int page) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        List<FriendHandler> allFriends = handler.getFriends();
        int totalPages = Math.max(1, (int) Math.ceil(allFriends.size() / 8.0));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * 8;
        int to = Math.min(from + 8, allFriends.size());
        List<FriendHandler> pageFriends = allFriends.subList(from, to);

        boolean isPublic = allFriends.stream().anyMatch(FriendHandler::doesRepresentPublic);

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__MANAGE)),
            PASTEL_PURPLE, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__HEADER)), SOFT_GRAY)));

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
            for (FriendHandler fh : pageFriends) {
                String uuidStr = fh.getName();
                String name = getPlayerName(uuidStr);
                body.add(DialogBodyEntry.text(Component.text()
                    .append(Component.text("  ", SOFT_GRAY))
                    .append(Component.text(name != null ? name : uuidStr, NamedTextColor.WHITE))
                    .build()));
            }
        }

        body.add(DialogBodyEntry.text(Component.empty()));
        String statusStr = stripColor(Translator.get(isPublic
            ? TranslationKey.DIALOGS__STATUS_PUBLIC
            : TranslationKey.DIALOGS__STATUS_PRIVATE));
        TextColor statusColor = isPublic ? PASTEL_MINT : PASTEL_CORAL;
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__STATUS_LABEL)), SOFT_GRAY))
            .append(Component.text(stripColor(Translator.get(TranslationKey.ICON__TOGGLE_ON)) + statusStr, statusColor))
            .build()));

        List<DialogButton> buttons = new ArrayList<>();

        buttons.add(new DialogButton("add",
            Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__SEARCH)),
                NamedTextColor.WHITE),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__MANAGE_HINT)),
                TextColor.color(0x888888)),
            p -> {
                Consumer<String> handleName = text -> {
                    Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), () -> {
                        OfflinePlayer target = Bukkit.getOfflinePlayer(text);
                        if (target.getName() != null && target.hasPlayedBefore()) {
                            handler.addFriend(target.getUniqueId().toString());
                            handler.applyToOtherContainer();
                            Bukkit.getScheduler().runTask(BlockProt.getInstance(), () -> showForBlock(p, block, handler));
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

        buttons.add(new DialogButton("make_public",
            toggleLabel(stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__MAKE_PUBLIC)), isPublic, PASTEL_MINT, PASTEL_CORAL),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__MANAGE_HINT)), TextColor.color(0x888888)),
            p -> {
                boolean wasPublic = handler.getFriends().stream().anyMatch(FriendHandler::doesRepresentPublic);
                if (wasPublic) {
                    handler.removeFriend(FriendSupportingHandler.publicUuid.toString());
                } else {
                    handler.addFriend(FriendSupportingHandler.publicUuid.toString());
                }
                handler.applyToOtherContainer();
                showForBlock(p, block, handler);
            }
        ));

        if (safePage > 0) {
            int prev = safePage - 1;
            buttons.add(new DialogButton("prev",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                p -> showForBlock(p, block, handler, prev)));
        }
        if (safePage + 1 < totalPages) {
            int next = safePage + 1;
            buttons.add(new DialogButton("next",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                p -> showForBlock(p, block, handler, next)));
        }

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(DialogOrigin.NONE);
        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(exitOrigin != DialogOrigin.NONE ? TranslationKey.DIALOGS__BACK : TranslationKey.DIALOGS__CLOSE)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            exitOrigin != DialogOrigin.NONE ? p -> BlockLockDialog.show(p, block, handler) : null
        );

        bridge.showMultiAction(player, title, body, buttons, exitBtn, 2);
    }

    public static void show(@NotNull Player player) {
        show(player, DialogOrigin.USER_MENU);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        PlayerSettingsHandler handler = new PlayerSettingsHandler(player);
        boolean isPublic = handler.containsFriend(FriendSupportingHandler.publicUuid.toString());

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__MANAGE)),
            PASTEL_PURPLE, TextDecoration.BOLD
        );

        String search = stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__SEARCH));
        String history = stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__SEARCH_HISTORY));
        String makePublic = stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__MAKE_PUBLIC));

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__HEADER)), SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__MANAGE_HINT)), TextColor.color(0x888888))));
        body.add(DialogBodyEntry.text(Component.empty()));
        String statusStr = stripColor(Translator.get(isPublic
            ? TranslationKey.DIALOGS__STATUS_PUBLIC
            : TranslationKey.DIALOGS__STATUS_PRIVATE));
        TextColor statusColor = isPublic ? PASTEL_MINT : PASTEL_CORAL;
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__STATUS_LABEL)), SOFT_GRAY))
            .append(Component.text(stripColor(Translator.get(TranslationKey.ICON__TOGGLE_ON)) + statusStr, statusColor))
            .build()));

        DialogButton searchBtn = new DialogButton("search",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__SEARCH)) + search, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__SEARCH)), SOFT_BLUE),
            p -> {
                p.closeInventory();
                de.sean.blockprot.bukkit.inventories.FriendSearchInventory.openChatInput(p);
            }
        );

        DialogButton historyBtn = new DialogButton("history",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__HISTORY)) + history, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__SEARCH_HISTORY)), PASTEL_GOLD),
            p -> {
                List<String> historyList = new PlayerSettingsHandler(p).getSearchHistory();
                if (historyList.isEmpty()) {
                    p.sendMessage(Component.text(
                        stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__NO_HISTORY)), SOFT_GRAY));
                    return;
                }
                p.sendMessage(Component.text(
                    stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__RECENT_SEARCHES)), PASTEL_GOLD));
                for (String entry : historyList) {
                    p.sendMessage(Component.text(" - ", SOFT_GRAY).append(Component.text(entry, NamedTextColor.WHITE)));
                }
            }
        );

        DialogButton publicBtn = new DialogButton("make_public",
            toggleLabel(makePublic, isPublic, PASTEL_MINT, PASTEL_CORAL),
            publicTooltip(isPublic),
            p -> {
                PlayerSettingsHandler h = new PlayerSettingsHandler(p);
                boolean wasPublic = h.containsFriend(FriendSupportingHandler.publicUuid.toString());
                if (wasPublic) {
                    h.removeFriend(FriendSupportingHandler.publicUuid.toString());
                    updateAllBlocksPublicStatus(p.getUniqueId(), false);
                } else {
                    h.addEveryoneAsFriend();
                    updateAllBlocksPublicStatus(p.getUniqueId(), true);
                }
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
        actions.add(searchBtn);
        actions.add(historyBtn);
        actions.add(publicBtn);
        bridge.showMultiAction(player, title, body, actions, exitBtn, 1);
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

    private static Component originHint(DialogOrigin origin) {
        switch (origin) {
            case USER_MENU: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_USER_MENU)), TextColor.color(0x888888));
            default: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888));
        }
    }

    private static DialogButton.DialogClickHandler originBack(Player player, DialogOrigin origin) {
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

    private static Component publicTooltip(boolean isPublic) {
        if (isPublic) {
            return Component.join(JoinConfiguration.newlines(),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__PUBLIC_MAKE)),
                    SOFT_BLUE, TextDecoration.BOLD),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__PUBLIC_CURRENT)),
                    PASTEL_MINT),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__PRIVATE_CLICK)),
                    PASTEL_CORAL));
        }
        return Component.join(JoinConfiguration.newlines(),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__PUBLIC_MAKE)),
                SOFT_BLUE, TextDecoration.BOLD),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__PRIVATE_CURRENT)),
                PASTEL_CORAL),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__PUBLIC_CLICK)),
                PASTEL_MINT));
    }

    private static Component tooltip(String description, TextColor accent) {
        return Component.join(
            JoinConfiguration.newlines(),
            Component.text(description, accent),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_TO_OPEN)), TextColor.color(0x888888))
        );
    }

    private static void updateAllBlocksPublicStatus(UUID playerUuid, boolean makePublic) {
        PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
        StatHandler.getStatisticByUuid(stat, playerUuid);
        for (LocationListEntry locEntry : stat.get()) {
            Location loc = locEntry.get();
            if (loc.getWorld() == null) continue;
            try {
                BlockNBTHandler handler = new BlockNBTHandler(loc.getBlock());
                if (!handler.isOwner(playerUuid)) continue;
                if (makePublic) {
                    handler.addFriend(FriendSupportingHandler.publicUuid.toString());
                } else {
                    handler.removeFriend(FriendSupportingHandler.publicUuid.toString());
                }
            } catch (RuntimeException ignored) {}
        }
    }
}
