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

import org.jetbrains.annotations.NotNull;

/**
 * Shared string utilities used across multiple inventory classes.
 */
public final class StringUtil {

    private StringUtil() {}

    /**
     * Computes the Levenshtein edit distance between two character sequences.
     * Optimized to use a single row of memory instead of a full matrix.
     *
     * @param a first sequence
     * @param b second sequence
     * @return edit distance
     */
    public static int levenshtein(@NotNull CharSequence a, @NotNull CharSequence b) {
        if (a.isEmpty()) return b.length();
        if (b.isEmpty()) return a.length();

        int[] mem = new int[b.length() + 1];
        for (int i = 0; i <= b.length(); i++) mem[i] = i;

        for (int i = 1; i <= a.length(); i++) {
            int[] cur = new int[b.length() + 1];
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(mem[j] + 1, cur[j - 1] + 1), mem[j - 1] + cost);
            }
            mem = cur;
        }

        return mem[b.length()];
    }

    /**
     * Returns a similarity score between 0.0 (completely different) and 1.0 (identical).
     * Based on normalized Levenshtein distance.
     *
     * @param a first string
     * @param b second string
     * @return similarity in [0.0, 1.0]
     */
    public static double similarity(@NotNull String a, @NotNull String b) {
        String longer  = a.length() >= b.length() ? a : b;
        String shorter = a.length() <  b.length() ? a : b;
        int len = longer.length();
        if (len == 0) return 1.0;
        return (len - levenshtein(longer, shorter)) / (double) len;
    }
}
