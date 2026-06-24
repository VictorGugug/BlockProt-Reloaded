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

package de.sean.blockprot.bukkit.util;

import de.sean.blockprot.bukkit.BlockProt;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Resolves Java and Floodgate/Geyser Bedrock names without forcing admins to
 * hard-code a single Bedrock prefix.
 */
public final class PlayerNameResolver {
    private PlayerNameResolver() {}

    @Nullable
    public static OfflinePlayer findOfflinePlayer(@NotNull String input) {
        for (String candidate : getNameCandidates(input)) {
            Player online = Bukkit.getPlayerExact(candidate);
            if (online != null) return online;

            OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(candidate);
            if (cached != null) return cached;
        }

        for (String candidate : getNameCandidates(input)) {
            @SuppressWarnings("deprecation")
            OfflinePlayer fallback = Bukkit.getOfflinePlayer(candidate);
            if (fallback.hasPlayedBefore()) return fallback;
        }
        return null;
    }

    @NotNull
    public static Set<String> getNameCandidates(@NotNull String input) {
        String trimmed = input.trim();
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (trimmed.isEmpty()) return candidates;

        candidates.add(trimmed);
        for (String prefix : BlockProt.getDefaultConfig().getBedrockUsernamePrefixes()) {
            if (prefix == null) continue;
            if (!prefix.isEmpty() && trimmed.startsWith(prefix)) {
                candidates.add(trimmed.substring(prefix.length()));
            } else if (!prefix.isEmpty()) {
                candidates.add(prefix + trimmed);
            }
        }
        return candidates;
    }
}
