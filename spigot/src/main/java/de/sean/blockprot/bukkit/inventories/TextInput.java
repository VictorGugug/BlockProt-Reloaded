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

import de.sean.blockprot.bukkit.VersionCompat;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Single entry point for every text-input prompt in the plugin (friend
 * search, entity friend search, transfer search, block/entity naming, admin
 * config values, world-expiry input, ...).
 *
 * <p>Always resolves to a chat-based prompt: {@link ChatInput} on Paper,
 * {@link LegacyChatInput} everywhere else. An earlier anvil-GUI based
 * approach was removed: on some Spigot builds the anvil inventory has been
 * observed opening and closing in a loop and crashing the client, and chat
 * input has no such failure mode on any server software.
 */
public final class TextInput {

    private TextInput() {}

    public static void open(
            @NotNull Player player,
            @NotNull Plugin plugin,
            @Nullable Consumer<String> onConfirm
    ) {
        if (VersionCompat.isPaper()) {
            ChatInput.open(player, plugin, onConfirm);
        } else {
            LegacyChatInput.open(player, plugin, onConfirm);
        }
    }

    /**
     * Opens the prompt with a field-specific subject (e.g. "Set block name")
     * instead of the generic "Type the player name" wording. Use this for
     * every non-player-search prompt (renaming, config values, durations, ...)
     * so the instruction actually matches what is being entered.
     */
    public static void open(
            @NotNull Player player,
            @NotNull Plugin plugin,
            @NotNull String contextMessage,
            @Nullable Consumer<String> onConfirm
    ) {
        if (VersionCompat.isPaper()) {
            ChatInput.open(player, plugin, contextMessage, onConfirm);
        } else {
            LegacyChatInput.open(player, plugin, contextMessage, onConfirm);
        }
    }
}
