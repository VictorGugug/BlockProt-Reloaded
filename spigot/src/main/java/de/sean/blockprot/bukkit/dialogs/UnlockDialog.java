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
import de.sean.blockprot.bukkit.listeners.HopperEventListener;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.LocationListEntry;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class UnlockDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xF0A0A0);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor SOFT_BLUE = TextColor.color(0xA0C4E8);

    private static final int PER_PAGE = 6;

    private UnlockDialog() {}

    public static void show(@NotNull Player player, @NotNull String targetName) {
        show(player, DialogOrigin.ADMIN_MENU, targetName, 0);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin,
                             @NotNull String targetName, int page) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || !target.hasPlayedBefore()) {
            player.sendMessage(Component.text(
                stripColor(Translator.get(TranslationKey.DIALOGS__UNLOCK__PLAYER_NOT_FOUND))
                    .replace("{player}", targetName), PASTEL_CORAL));
            return;
        }

        PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
        StatHandler.getStatisticByUuid(stat, target.getUniqueId());
        List<LocationListEntry> allBlocks = stat.get();

        if (allBlocks.isEmpty()) {
            player.sendMessage(Component.text(
                stripColor(Translator.get(TranslationKey.DIALOGS__UNLOCK__NO_BLOCKS))
                    .replace("{player}", targetName), SOFT_GRAY));
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil(allBlocks.size() / (double) PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PER_PAGE;
        int to = Math.min(from + PER_PAGE, allBlocks.size());
        List<LocationListEntry> pageBlocks = allBlocks.subList(from, to);

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__BP_UNLOCK__TITLE)),
            SOFT_BLUE, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__UNLOCK__HEADER)), SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.text(
            targetName + " - "
            + stripColor(Translator.get(TranslationKey.DIALOGS__PAGE))
                .replace("{current}", String.valueOf(safePage + 1))
                .replace("{total}", String.valueOf(totalPages)),
            TextColor.color(0x888888))));

        List<DialogButton> buttons = new ArrayList<>();

        UUID targetUuid = target.getUniqueId();
        for (LocationListEntry locEntry : pageBlocks) {
            Location loc = locEntry.get();
            String desc = (loc.getWorld() != null ? loc.getWorld().getName() : "?") + " @ "
                + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();

            buttons.add(new DialogButton("block_" + safePage + "_" + buttons.size(),
                Component.text(stripColor(Translator.get(backOrigin == DialogOrigin.INFO ? TranslationKey.ICON__SEARCH : TranslationKey.ICON__UNLOCK)) + desc, NamedTextColor.WHITE),
                Component.join(JoinConfiguration.newlines(),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__UNLOCK__CLICK_HINT)),
                        TextColor.color(0x888888)),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__UNLOCK__LOCATION)), SOFT_GRAY)
                        .append(Component.text(desc, NamedTextColor.WHITE))),
                p -> {
                    if (loc.getWorld() == null) return;
                    if (backOrigin == DialogOrigin.INFO) {
                        try {
                            var handler = new BlockNBTHandler(loc.getWorld().getBlockAt(loc));
                            String ownerName = Bukkit.getOfflinePlayer(UUID.fromString(handler.getOwner())).getName();
                            Component info = Component.text()
                                .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__UNLOCK__MATERIAL_LABEL)), SOFT_GRAY))
                                .append(Component.text(loc.getBlock().getType().name(), NamedTextColor.WHITE))
                                .append(Component.newline())
                                .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__UNLOCK__OWNER_LABEL)), SOFT_GRAY))
                                .append(Component.text(ownerName != null ? ownerName : handler.getOwner(), NamedTextColor.WHITE))
                                .append(Component.newline())
                                .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__UNLOCK__FRIENDS_LABEL)), SOFT_GRAY))
                                .append(Component.text(String.valueOf(handler.getFriends().size()), NamedTextColor.WHITE))
                                .build();
                            DialogBridge infoBridge = DialogBridgeFactory.getBridge();
                            if (infoBridge != null) {
                                DialogOrigin innerExit = DialogBridgeFactory.resolveOrigin(backOrigin);
                                infoBridge.showNotice(p,
                                    Component.text(desc, SOFT_BLUE, TextDecoration.BOLD),
                                    List.of(info),
                                    new DialogButton("back",
                                        Component.text(stripColor(Translator.get(innerExit == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
                                        Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
                                        innerExit == DialogOrigin.NONE ? pl -> {} : pl -> show(pl, backOrigin, targetName, safePage)));
                            }
                        } catch (Exception e) {
                            show(p, backOrigin, targetName, safePage);
                        }
                    } else {
                        try {
                            var block = loc.getWorld().getBlockAt(loc);
                            var handler = new BlockNBTHandler(block);
                            if (handler.isOwner(targetUuid)) {
                                handler.clear();
                                try { handler.applyToOtherContainer(); } catch (RuntimeException ignored) {}
                                HopperEventListener.invalidate(block);
                                ProtectedBlockCache.unmark(block);
                                try { StatHandler.removeContainerByUuid(targetUuid, loc.clone()); }
                                catch (IllegalArgumentException ignored) {}
                                p.sendMessage(Component.text(
                                    stripColor(Translator.get(TranslationKey.DIALOGS__UNLOCK__UNLOCKED))
                                        .replace("{location}", desc), PASTEL_MINT));
                            } else {
                                p.sendMessage(Component.text(
                                    stripColor(Translator.get(TranslationKey.DIALOGS__UNLOCK__NOT_OWNED))
                                        .replace("{player}", targetName), PASTEL_CORAL));
                            }
                        } catch (Exception e) {
                            p.sendMessage(Component.text(
                                stripColor(Translator.get(TranslationKey.DIALOGS__UNLOCK__FAILED)), PASTEL_CORAL));
                        }
                        show(p, backOrigin, targetName, safePage);
                    }
                }
            ));
        }

        List<DialogButton> navButtons = new ArrayList<>();
        if (safePage > 0) {
            int prev = safePage - 1;
            navButtons.add(new DialogButton("prev",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, targetName, prev)));
        }
        if (safePage + 1 < totalPages) {
            int next = safePage + 1;
            navButtons.add(new DialogButton("next",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, targetName, next)));
        }
        buttons.addAll(navButtons);

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            backHint(exitOrigin),
            backAction(player, exitOrigin)
        );

        bridge.showMultiAction(player, title, body, buttons, backBtn, 1);
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
            case INFO: return p -> InfoDialog.show(p, DialogOrigin.ADMIN_MENU);
            default: return p -> {};
        }
    }
}
