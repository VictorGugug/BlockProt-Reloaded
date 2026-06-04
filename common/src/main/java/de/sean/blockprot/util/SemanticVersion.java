/*
 * Copyright (C) 2021 - 2025 spnda
 * Modifications Copyright (C) 2025 Zaynr (Zar)
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

package de.sean.blockprot.util;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * Semantic version with full pre-release awareness.
 *
 * <p>Version format: {@code MAJOR.MINOR.PATCH[-SUFFIX[-N]]}
 *
 * <p>Pre-release order (lowest → highest within the same numeric version):
 * <ol>
 *   <li>snapshot / dev (our rolling dev build — formerly "SNAPSHOT")</li>
 *   <li>alpha.N</li>
 *   <li>beta.N</li>
 *   <li>rc.N</li>
 *   <li>(no suffix) — clean release, highest rank</li>
 * </ol>
 *
 * <p>Special suffixes (not ranked above release, treated as equal to their
 * base version for update-check purposes):
 * <ul>
 *   <li>patch.N / fix.N / hotfix.N — post-release corrections</li>
 *   <li>exp — experimental branches, never considered an update</li>
 * </ul>
 *
 * @since 0.1.11
 */
public class SemanticVersion implements Comparable<SemanticVersion> {

    // ── Suffix rank (lower = older / less stable) ─────────────────────────────
    // Suffixes not in this map are treated as RANK_RELEASE (i.e. patch/hotfix/fix/exp).
    private static final int RANK_SNAPSHOT = 0;
    private static final int RANK_ALPHA    = 1;
    private static final int RANK_BETA     = 2;
    private static final int RANK_RC       = 3;
    private static final int RANK_RELEASE  = 4; // no suffix

    private final int[] numeric;   // e.g. [1, 3, 0]
    private final String suffix;   // e.g. "alpha.2", "snapshot", "" for release
    private final int suffixRank;  // pre-computed rank
    private final int suffixN;     // numeric part of suffix (alpha.2 → 2), 0 if absent

    public SemanticVersion(@NotNull final String version) {
        // Split on first '-' only — keeps "SNAPSHOT-3" together as extension.
        int dash = version.indexOf('-');
        String numericPart = dash == -1 ? version : version.substring(0, dash);
        String raw = dash == -1 ? "" : version.substring(dash + 1).toLowerCase(java.util.Locale.ENGLISH);

        String[] numParts = numericPart.split("\\.");
        numeric = new int[numParts.length];
        for (int i = 0; i < numParts.length; i++) {
            try { numeric[i] = Integer.parseInt(numParts[i].trim()); }
            catch (NumberFormatException ignored) {}
        }

        // Normalise suffix — "snapshot-3" and "snapshot" both become "snapshot".
        // Strip trailing numeric counter (e.g. "SNAPSHOT-3" → "snapshot", "alpha.2" → "alpha").
        String base = raw.replaceAll("-\\d+$", "").replaceAll("\\.\\d+$", "");

        // Extract numeric part of suffix if present (e.g. alpha.2 → 2).
        int n = 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[.\\-](\\d+)$").matcher(raw);
        if (m.find()) { try { n = Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {} }
        suffixN = n;
        suffix  = raw;

        suffixRank = switch (base) {
            case "snapshot", "dev" -> RANK_SNAPSHOT;
            case "alpha"           -> RANK_ALPHA;
            case "beta"            -> RANK_BETA;
            case "rc"              -> RANK_RC;
            default                -> RANK_RELEASE; // release, patch, fix, hotfix, exp, ""
        };
    }

    /** Returns {@code true} if this is a pre-release (snapshot/alpha/beta/rc). */
    public boolean isPreRelease() { return suffixRank < RANK_RELEASE; }

    /** Returns {@code true} if this is an experimental build. */
    public boolean isExperimental() { return suffix.startsWith("exp"); }

    @Override
    public int compareTo(@NotNull final SemanticVersion other) {
        // 1. Compare numeric parts.
        int len = Math.max(numeric.length, other.numeric.length);
        for (int i = 0; i < len; i++) {
            int a = i < numeric.length       ? numeric[i]       : 0;
            int b = i < other.numeric.length ? other.numeric[i] : 0;
            if (a != b) return Integer.compare(a, b);
        }
        // 2. Same numeric — compare suffix rank.
        if (suffixRank != other.suffixRank) return Integer.compare(suffixRank, other.suffixRank);
        // 3. Same rank — compare the suffix counter (alpha.1 < alpha.2).
        return Integer.compare(suffixN, other.suffixN);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SemanticVersion)) return false;
        return this.compareTo((SemanticVersion) obj) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(numeric), suffix);
    }

    @Override
    public String toString() {
        String num = Arrays.stream(numeric).mapToObj(String::valueOf).reduce((a, b) -> a + "." + b).orElse("0");
        return suffix.isEmpty() ? num : num + "-" + suffix;
    }
}
