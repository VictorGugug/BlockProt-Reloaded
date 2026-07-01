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
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.LocationListEntry;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.bukkit.util.PlayerNameResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Handles {@code /bp transfer <player>} and {@code /bp transfer all <player>}.
 *
 * <p>Single-block: player must look at a block they own.<br>
 * All-blocks: transfers every block in the caller's stat list to the target player.
 *
 * @since 1.2.0
 */
public final class TransferCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__ONLY_PLAYERS)));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__TRANSFER_USAGE)));
            return true;
        }

        handleTransferAll(player, args[1]);
        return true;
    }

    private void handleTransferAll(@NotNull Player player, @NotNull String targetName) {
        resolvePlayer(player, targetName, newOwner -> {
            if (newOwner.getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.MESSAGES__TRANSFER_SELF)));
                return;
            }

            PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
            StatHandler.getStatistic(stat, player);
            List<LocationListEntry> entries = stat.get();

            if (entries.isEmpty()) {
                player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.MESSAGES__TRANSFER_ALL_NO_BLOCKS)));
                return;
            }

            final OfflinePlayer finalNewOwner = newOwner;
            Bukkit.getScheduler().runTask(BlockProt.getInstance(), () -> {
                int transferred = 0;
                for (LocationListEntry entry : entries) {
                    try {
                        Location loc = entry.get();
                        if (loc == null || loc.getWorld() == null) continue;
                        Block block = loc.getBlock();
                        if (!BlockProt.getDefaultConfig().isLockable(block.getType())) continue;
                        BlockNBTHandler handler = new BlockNBTHandler(block);
                        if (!handler.isOwner(player.getUniqueId())) continue;
                        var result = handler.transferOwner(
                            player.getUniqueId().toString(),
                            finalNewOwner.getUniqueId().toString()
                        );
                        if (result.success) {
                            Player onlineTarget = Bukkit.getPlayer(finalNewOwner.getUniqueId());
                            if (onlineTarget != null) {
                                StatHandler.addBlock(onlineTarget, loc);
                            } else {
                                StatHandler.addBlockByUuid(finalNewOwner.getUniqueId(), loc);
                            }
                            transferred++;
                        }
                    } catch (RuntimeException ignored) {}
                }
                String name = finalNewOwner.getName() != null ? finalNewOwner.getName() : targetName;
                player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.MESSAGES__TRANSFER_ALL_SUCCESS)
                        .replace("{count}", String.valueOf(transferred))
                        .replace("{player}", name)));
            });
        });
    }

    private void resolvePlayer(@NotNull Player player, @NotNull String name,
                                @NotNull java.util.function.Consumer<OfflinePlayer> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), () -> {
            OfflinePlayer found = PlayerNameResolver.findOfflinePlayer(name);
            if (found == null) {
                @SuppressWarnings("deprecation")
                OfflinePlayer fallback = Bukkit.getOfflinePlayer(name);
                if (fallback.hasPlayedBefore()) found = fallback;
            }
            if (found == null || found.getUniqueId() == null) {
                final String msg = Translator.get(TranslationKey.MESSAGES__TRANSFER_PLAYER_NOT_FOUND)
                    .replace("{player}", name);
                Bukkit.getScheduler().runTask(BlockProt.getInstance(), () ->
                    player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(msg)));
                return;
            }
            final OfflinePlayer resolved = found;
            Bukkit.getScheduler().runTask(BlockProt.getInstance(), () -> callback.accept(resolved));
        });
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            // Include all offline players that have played before so targets do not need to be online.
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