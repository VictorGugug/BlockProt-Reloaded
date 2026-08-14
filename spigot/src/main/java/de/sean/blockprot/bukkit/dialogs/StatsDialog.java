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
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.BlockCountStatistic;
import de.sean.blockprot.bukkit.nbt.stats.LocationListEntry;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_CORAL;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_GOLD;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_MINT;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_PURPLE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_BLUE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_GRAY;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class StatsDialog {

    private StatsDialog() {}

    static final String KEY_PLAYER = "player";
    static final String KEY_GLOBAL = "global";

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        show(player, backOrigin, KEY_PLAYER);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin, @NotNull String pageKey) {
        boolean isPlayerPage = pageKey.equals(KEY_PLAYER);
        TranslationKey titleKey = isPlayerPage
            ? TranslationKey.INVENTORIES__STATISTICS__PLAYER_STATISTICS
            : TranslationKey.INVENTORIES__STATISTICS__GLOBAL_STATISTICS;
        show(player, backOrigin, pageKey, true, titleKey);
    }

    public static void showUserStats(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        show(player, backOrigin, KEY_PLAYER, false, TranslationKey.DIALOGS__STATS__PLACEMENTS_TITLE);
    }

    private static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin,
                             @NotNull String pageKey, boolean showToggle,
                             @NotNull TranslationKey titleKey) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        boolean isPlayerPage = pageKey.equals(KEY_PLAYER);
        boolean isUserStats = titleKey == TranslationKey.DIALOGS__STATS__PLACEMENTS_TITLE;

        Component title = Component.text(
            stripColor(Translator.get(titleKey)),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__STATS__HEADER)), SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.empty()));

        List<DialogButton> buttons = new ArrayList<>();

        if (isPlayerPage) {
            PlayerBlocksStatistic pStat = new PlayerBlocksStatistic();
            StatHandler.getStatistic(pStat, player);
            List<LocationListEntry> entries = pStat.get();
            int total = entries.size();
            Integer limit = BlockProt.getDefaultConfig().getMaxLockedBlockCount();
            String countStr = String.valueOf(total);
            if (limit != null) {
                countStr += "/" + limit;
            }
            body.add(DialogBodyEntry.text(Component.text()
                .append(Component.text(
                    stripColor(Translator.get(TranslationKey.DIALOGS__STATS__YOUR_BLOCKS)), NamedTextColor.WHITE))
                .append(Component.text(countStr, PASTEL_MINT, TextDecoration.BOLD))
                .build()));
            body.add(DialogBodyEntry.text(Component.empty()));

            if (total > 0) {
                Map<Material, List<LocationListEntry>> grouped = new LinkedHashMap<>();
                for (LocationListEntry entry : entries) {
                    Material mat = entry.getItemType();
                    if (mat != Material.AIR) {
                        grouped.computeIfAbsent(mat, k -> new ArrayList<>()).add(entry);
                    }
                }
                for (Map.Entry<Material, List<LocationListEntry>> group : grouped.entrySet()) {
                    Material mat = group.getKey();
                    int count = group.getValue().size();
                    String matName = toHumanReadable(mat);
                    buttons.add(new DialogButton("mat_" + mat.name(),
                        Component.text(matName + " (" + count + ")", NamedTextColor.WHITE),
                        Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_TO_OPEN)), TextColor.color(0x888888)),
                        p -> showMaterialBlocks(p, backOrigin, pageKey, isUserStats, mat, group.getValue())));
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

        if (showToggle) {
            String toggleLabel = isPlayerPage
                ? stripColor(Translator.get(TranslationKey.INVENTORIES__STATISTICS__GLOBAL_STATISTICS))
                : stripColor(Translator.get(TranslationKey.INVENTORIES__STATISTICS__PLAYER_STATISTICS));

            String nextPageKey = isPlayerPage ? KEY_GLOBAL : KEY_PLAYER;

            buttons.add(new DialogButton("toggle",
                Component.text(stripColor(Translator.get(TranslationKey.ICON__STATS)) + toggleLabel, NamedTextColor.WHITE),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__SWITCH_VIEW)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, nextPageKey)
            ));
        }

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            backHint(exitOrigin),
            backAction(player, exitOrigin)
        );

        bridge.showMultiAction(player, title, body, buttons, backBtn, 2);
    }

    private static void showMaterialBlocks(@NotNull Player player, @NotNull DialogOrigin backOrigin,
                                             @NotNull String pageKey, boolean isUserStats,
                                             @NotNull Material mat,
                                             @NotNull List<LocationListEntry> entries) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Component title = Component.text(
            toHumanReadable(mat),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__STATS__YOUR_BLOCKS)) + toHumanReadable(mat),
            SOFT_GRAY)));

        boolean canTp = player.hasPermission(Permissions.BLOCKS_TP.key());
        String tpLore = stripColor(Translator.get(
            canTp ? TranslationKey.INVENTORIES__STATS__LORE_TP
                  : TranslationKey.INVENTORIES__STATS__LORE_NO_TP));

        List<DialogButton> buttons = new ArrayList<>();
        for (LocationListEntry entry : entries) {
            Location loc = entry.get();
            String lockedAgo = entry.getLockedAgoText();
            List<String> contents = entry.getContentsLore();

            // A click only opens the edit dialog when the block is still lockable under
            // the current config; otherwise it falls back to teleporting. The lore must
            // match whichever of those two the click will actually do, not show both.
            boolean willOpen = loc.getWorld() != null
                && BlockProt.getDefaultConfig().isLockable(loc.getBlock().getType(), loc.getWorld());

            List<Component> loreLines = new ArrayList<>();
            if (willOpen) {
                loreLines.add(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_TO_OPEN)), PASTEL_MINT));
            } else if (canTp) {
                loreLines.add(Component.text(tpLore, PASTEL_MINT));
            } else {
                loreLines.add(Component.text(tpLore, SOFT_GRAY));
            }
            if (!lockedAgo.isEmpty()) {
                loreLines.add(Component.text(stripColor(lockedAgo), SOFT_GRAY));
            }
            if (!contents.isEmpty()) {
                loreLines.add(Component.empty());
                for (String line : contents) {
                    loreLines.add(Component.text(stripColor(line), TextColor.color(0x888888)));
                }
            }

            buttons.add(new DialogButton("block_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ(),
                Component.text(entry.getTitle(), NamedTextColor.WHITE),
                Component.join(JoinConfiguration.newlines(), loreLines),
                p -> {
                    if (loc.getWorld() != null) {
                        org.bukkit.block.Block b = loc.getBlock();
                        if (BlockProt.getDefaultConfig().isLockable(b.getType(), b.getWorld())) {
                            BlockLockDialog.showBlock(p, b, isUserStats ? DialogOrigin.USER_MENU : DialogOrigin.ADMIN_MENU);
                            return;
                        }
                    }
                    if (!p.hasPermission(Permissions.BLOCKS_TP.key())) {
                        p.sendMessage(stripColor(Translator.get(TranslationKey.MESSAGES__NO_PERMISSION_TP)));
                        return;
                    }
                    Location tpLoc = entry.get();
                    if (tpLoc.getWorld() != null) {
                        p.teleport(tpLoc.clone().add(0.5, 1.0, 0.5));
                    }
                }));
        }

        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            p -> {
                if (isUserStats) {
                    showUserStats(p, backOrigin);
                } else {
                    show(p, backOrigin, pageKey);
                }
            });

        bridge.showMultiAction(player, title, body, buttons, backBtn, 2);
    }

    static String toHumanReadable(@NotNull Material mat) {
        String raw = mat.name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : raw.toCharArray()) {
            if (c == ' ') { sb.append(c); cap = true; }
            else if (cap) { sb.append(Character.toUpperCase(c)); cap = false; }
            else { sb.append(Character.toLowerCase(c)); }
        }
        return sb.toString();
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
