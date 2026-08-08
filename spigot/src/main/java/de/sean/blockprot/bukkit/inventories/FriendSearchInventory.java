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

package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.bukkit.BlockProt;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Opens a chat input to search for a friend to add.
 */
public class FriendSearchInventory {
    public static void openChatInput(@NotNull final Player requestingPlayer) {
        TextInput.open(
            requestingPlayer,
            BlockProt.getInstance(),
            text -> handleResult(requestingPlayer, text)
        );
    }

    private static void handleResult(@NotNull Player player, String text) {
        if (text == null || text.isBlank()) return;
        Inventory inventory = new FriendSearchResultInventory().fill(player, text);
        if (inventory != null) {
            player.openInventory(inventory);
        }
    }
}