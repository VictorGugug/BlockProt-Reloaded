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
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.dialogs.DialogOrigin;
import de.sean.blockprot.bukkit.dialogs.InfoDialog;
import de.sean.blockprot.bukkit.dialogs.UnlockDialog;
import de.sean.blockprot.bukkit.inventories.BpUnlockInventory;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * /bp unlock <player>
 *
 * Opens a GUI showing every block protected by the target player.
 * Left-click a block: view its contents read-only.
 * Right-click a block: remove the protection from that block.
 *
 * Requires {@code blockprot.user.admin}.
 */
public final class AdminUnlockCommand implements CommandExecutor {

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return sender.isOp() || sender.hasPermission(Permissions.USER_ADMIN.key());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__ONLY_PLAYERS));
            return true;
        }

        if (!player.hasPermission(Permissions.USER_ADMIN.key()) && !player.isOp()) {
            ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            return true;
        }

        if (args.length < 2) {
            if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
                InfoDialog.show(player, DialogOrigin.ADMIN_MENU);
                return true;
            }
            ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__BP_UNLOCK_USAGE));
            return true;
        }

        String targetName = args[1];

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getName() == null) {
            ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__BP_UNLOCK_PLAYER_NOT_FOUND)
                .replace("{player}", targetName));
            return true;
        }

        String resolvedName = target.getName() != null ? target.getName() : targetName;

        PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
        StatHandler.getStatisticByUuid(stat, target.getUniqueId());

        if (stat.get().isEmpty()) {
            ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__BP_UNLOCK_NO_BLOCKS)
                .replace("{player}", resolvedName));
            return true;
        }

        if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
            UnlockDialog.show(player, DialogOrigin.ADMIN_MENU, resolvedName, 0);
            return true;
        }

        InventoryState state = InventoryState.builder().build();
        InventoryState.set(player.getUniqueId(), state);

        BpUnlockInventory inv = new BpUnlockInventory();
        player.openInventory(inv.fill(player, resolvedName, stat));
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String alias, @NotNull String[] args) {
        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}