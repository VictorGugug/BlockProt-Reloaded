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
import de.sean.blockprot.bukkit.dialogs.UserMenuDialog;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.inventories.UserMenuInventory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Opens the user GUI. Usage: {@code /bp user}.
 * Requires {@code blockprot.user} (default: true: all players).
 */
public final class UserMenuCommand implements CommandExecutor {

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return sender.hasPermission(Permissions.USER.key()) || sender.hasPermission(Permissions.USER.key());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;
        if (!canUseCommand(sender)) {
            player.sendMessage(Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            return true;
        }
        if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
            UserMenuDialog.show(player);
            return true;
        }
        if (BlockProt.getDefaultConfig().areExtraCommandsEnabled()) return false;

        InventoryState state = new InventoryState(null);
        state.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
        InventoryState.set(player.getUniqueId(), state);
        player.openInventory(new UserMenuInventory().fill(player));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}