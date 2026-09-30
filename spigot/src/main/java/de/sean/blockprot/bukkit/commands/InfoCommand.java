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

package de.sean.blockprot.bukkit.commands;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.admin.AdminAction;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.dialogs.DialogOrigin;
import de.sean.blockprot.bukkit.dialogs.InfoDialog;
import de.sean.blockprot.bukkit.inventories.AdminBlockListInventory;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.inventories.PlayerListInventory;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.LocationListEntry;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import de.sean.blockprot.bukkit.util.RegionTasks;
import de.sean.blockprot.bukkit.util.PlayerNameResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Handles {@code /bp info [player]}.
 *
 * <ul>
 *   <li>No argument + Player sender: opens {@link PlayerListInventory} (all players, sortable).</li>
 *   <li>With player name: opens {@link AdminBlockListInventory} for that specific player.</li>
 *   <li>Console sender: always requires a player name argument.</li>
 * </ul>
 *
 * Requires the {@code INFO} admin action: OP or {@code blockprot.user.admin}, or tier T1 and above with admin tiers enabled.
 */
public final class InfoCommand implements CommandExecutor {

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return AdminTierManager.hasPermission(sender, AdminAction.INFO);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!canUseCommand(sender)) {
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            return true;
        }

        if (args.length < 2) {
            if (sender instanceof Player player) {
                if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
                    InfoDialog.show(player, DialogOrigin.NONE);
                    return true;
                }
                InventoryState state = new InventoryState(null);
                state.origin = InventoryState.MenuOrigin.NONE;
                InventoryState.set(player.getUniqueId(), state);
                new PlayerListInventory().open(player);
            } else {
                ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__ADMIN_INFO_USAGE));
            }
            return true;
        }

        final String targetName = args[1];

        BlockProt.getFoliaLib().getScheduler().runAsync(asyncTask -> {
            OfflinePlayer offlineTarget = PlayerNameResolver.findOfflinePlayer(targetName);
            if (offlineTarget == null) {
                @SuppressWarnings("deprecation")
                OfflinePlayer fallback = Bukkit.getOfflinePlayer(targetName);
                if (fallback.hasPlayedBefore()) offlineTarget = fallback;
            }

            if (offlineTarget == null || offlineTarget.getUniqueId() == null) {
                final String msg = Translator.get(TranslationKey.MESSAGES__ADMIN_INFO_PLAYER_NOT_FOUND)
                    .replace("{player}", targetName);
                RegionTasks.runFor(sender, () -> ComponentMessages.sendLegacy(sender, msg));
                return;
            }

            final OfflinePlayer finalTarget = offlineTarget;
            final String displayName = finalTarget.getName() != null ? finalTarget.getName() : targetName;

            RegionTasks.runFor(sender, () -> {
                PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
                StatHandler.getStatisticByUuid(stat, finalTarget.getUniqueId());

                if (sender instanceof Player player && !BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
                    InventoryState ns = new InventoryState(null);
                    ns.currentPageIndex = 0;
                    ns.origin = InventoryState.MenuOrigin.NONE;
                    InventoryState.set(player.getUniqueId(), ns);
                    player.openInventory(new AdminBlockListInventory().fill(player, displayName, stat));
                    return;
                }

                List<Location> locations = stat.get().stream().map(LocationListEntry::get).toList();
                RegionTasks.mapLocations(locations, loc -> {
                    var block = loc.getBlock();
                    if (!BlockProt.getDefaultConfig().isLockable(block.getType())) return null;
                    return new BlockNBTHandler(block).isOwner(finalTarget.getUniqueId()) ? loc : null;
                }).thenAccept(owned -> RegionTasks.runFor(sender, () -> {
                    if (owned.isEmpty()) {
                        ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__ADMIN_INFO_NO_BLOCKS)
                            .replace("{player}", displayName));
                        return;
                    }
                    ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__ADMIN_INFO_HEADER)
                        .replace("{player}", displayName));
                    final String entryTemplate = Translator.get(TranslationKey.MESSAGES__ADMIN_INFO_ENTRY);
                    for (Location loc : owned) {
                        ComponentMessages.sendLegacy(sender, entryTemplate
                            .replace("{world}", loc.getWorld().getName())
                            .replace("{x}",     String.valueOf(loc.getBlockX()))
                            .replace("{y}",     String.valueOf(loc.getBlockY()))
                            .replace("{z}",     String.valueOf(loc.getBlockZ())));
                    }
                }));
            });
        });

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!canUseCommand(sender)) return Collections.emptyList();
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            return java.util.Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(op -> op.getName() != null && op.getName().toLowerCase().startsWith(prefix))
                .map(op -> op.getName())
                .sorted()
                .limit(20)
                .toList();
        }
        return Collections.emptyList();
    }
}