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

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resolves player names against a local candidate pool instead of any
 * network-backed profile service. Must be called off the main thread;
 * {@code Bukkit.getOfflinePlayers()} reads the on-disk player cache.
 *
 * <p>The pool always includes every currently online player (independent of
 * the offline-player cache, which can lag a fraction of a second behind a
 * fresh join) merged with every entry in the offline-player cache. This is
 * the single source of truth for "add friend by name" across both block
 * friends and entity/pet friends, so an online player is never missed and a
 * previously-seen offline player is still found.
 */
public final class PlayerLookup {

    private PlayerLookup() {}

    /** Every known player name keyed by UUID, {@code exclude} omitted. */
    @NotNull
    public static Map<UUID, String> candidates(@Nullable UUID exclude) {
        final var candidates = new LinkedHashMap<UUID, String>();

        for (final var op : Bukkit.getOfflinePlayers()) {
            UUID uuid = op.getUniqueId();
            String name = op.getName();
            if (name == null || uuid == null || uuid.equals(exclude)) continue;
            // Mojang (v4/premium) and offline-mode name-derived (v3) UUIDs;
            // v0 covers a handful of legacy/edge-case entries seen in the wild.
            if (uuid.version() == 3 || uuid.version() == 4 || uuid.version() == 0) {
                candidates.put(uuid, name);
            }
        }

        // Online players always win a name collision and are always present,
        // even if the offline-player cache has not been flushed to disk yet.
        for (final var online : Bukkit.getOnlinePlayers()) {
            UUID uuid = online.getUniqueId();
            if (uuid.equals(exclude)) continue;
            candidates.put(uuid, online.getName());
        }

        return candidates;
    }

    /**
     * The single best fuzzy-match candidate at or above {@code minSimilarity},
     * or {@code null} if nothing qualifies.
     */
    @Nullable
    public static Map.Entry<UUID, String> findBestMatch(
            @NotNull String query,
            double minSimilarity,
            @Nullable UUID exclude
    ) {
        Map.Entry<UUID, String> best = null;
        double bestScore = -1;

        for (final var entry : candidates(exclude).entrySet()) {
            double score = StringUtil.similarity(entry.getValue(), query);
            if (score >= minSimilarity && score > bestScore) {
                best = entry;
                bestScore = score;
            }
        }

        return best;
    }

    /**
     * A candidate player identity paired with its similarity score.
     */
    public record ScoredMatch(@NotNull UUID uuid, @NotNull String name, double similarity) {}

    /**
     * All fuzzy-match candidates at or above {@code minSimilarity}, sorted by
     * descending similarity and capped at {@code limit}.
     */
    @NotNull
    public static List<ScoredMatch> findCandidates(
            @NotNull String query,
            double minSimilarity,
            int limit,
            @Nullable UUID exclude
    ) {
        var matches = new ArrayList<ScoredMatch>();
        for (final var entry : candidates(exclude).entrySet()) {
            double score = StringUtil.similarity(entry.getValue(), query);
            if (score >= minSimilarity) {
                matches.add(new ScoredMatch(entry.getKey(), entry.getValue(), score));
            }
        }
        matches.sort(Comparator.comparingDouble(ScoredMatch::similarity).reversed());
        if (matches.size() > limit) return matches.subList(0, limit);
        return matches;
    }
}
