/*
 * Copyright (C) 2021 - 2025 spnda
 * This file is part of BlockProt <https://github.com/spnda/BlockProt>.
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
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.inventories.AdminBlockListInventory;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.LocationListEntry;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.bukkit.util.PlayerNameResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
 * Handles {@code /bp info <player>} — admin command that lists all blocks
 * currently owned by the specified player (player must be online).
 *
 * <p>Requires {@code blockprot.admin} permission or OP.
 *
 * @since 1.2.0
 */
public final class InfoCommand implements CommandExecutor {

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return sender.isOp() || sender.hasPermission(Permissions.USER_ADMIN.key());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!canUseCommand(sender)) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__NO_PERMISSION)));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__ADMIN_INFO_USAGE)));
            return true;
        }

        final String targetName = args[1];

        Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), () -> {
            // Resolve the target player
            OfflinePlayer offlineTarget = PlayerNameResolver.findOfflinePlayer(targetName);
            if (offlineTarget == null) {
                @SuppressWarnings("deprecation")
                OfflinePlayer fallback = Bukkit.getOfflinePlayer(targetName);
                if (fallback.hasPlayedBefore()) offlineTarget = fallback;
            }

            if (offlineTarget == null || offlineTarget.getUniqueId() == null) {
                final String msg = Translator.get(TranslationKey.MESSAGES__ADMIN_INFO_PLAYER_NOT_FOUND)
                    .replace("{player}", targetName);
                Bukkit.getScheduler().runTask(BlockProt.getInstance(), () ->
                    sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(msg)));
                return;
            }

            final OfflinePlayer finalTarget   = offlineTarget;
            final String        displayName   = finalTarget.getName() != null ? finalTarget.getName() : targetName;

            Bukkit.getScheduler().runTask(BlockProt.getInstance(), () -> {
                PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
                StatHandler.getStatisticByUuid(stat, finalTarget.getUniqueId());

                // ── GUI for Player senders ────────────────────────────────
                if (sender instanceof Player player) {
                    InventoryState ns = new InventoryState(null);
                    ns.currentPageIndex = 0;
                    InventoryState.set(player.getUniqueId(), ns);
                    player.openInventory(new AdminBlockListInventory().fill(player, displayName, stat));
                    return;
                }

                // ── Chat output for Console senders ───────────────────────
                List<LocationListEntry> entries = stat.get();
                if (entries.isEmpty()) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        Translator.get(TranslationKey.MESSAGES__ADMIN_INFO_NO_BLOCKS)
                            .replace("{player}", displayName)));
                    return;
                }

                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.MESSAGES__ADMIN_INFO_HEADER)
                        .replace("{player}", displayName)));

                final String entryTemplate = Translator.get(TranslationKey.MESSAGES__ADMIN_INFO_ENTRY);
                for (LocationListEntry entry : entries) {
                    Location loc = entry.get();
                    if (loc.getWorld() == null) continue;
                    try {
                        var block   = loc.getWorld().getBlockAt(loc);
                        var handler = new BlockNBTHandler(block);
                        if (!BlockProt.getDefaultConfig().isLockable(block.getType())) continue;
                        if (!handler.isOwner(finalTarget.getUniqueId())) continue;
                    } catch (RuntimeException ignored) { continue; }

                    String line = entryTemplate
                        .replace("{world}", loc.getWorld().getName())
                        .replace("{x}",     String.valueOf(loc.getBlockX()))
                        .replace("{y}",     String.valueOf(loc.getBlockY()))
                        .replace("{z}",     String.valueOf(loc.getBlockZ()));
                    sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(line));
                }
            });
        });

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!canUseCommand(sender)) return Collections.emptyList();
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                .toList();
        }
        return Collections.emptyList();
    }
}
