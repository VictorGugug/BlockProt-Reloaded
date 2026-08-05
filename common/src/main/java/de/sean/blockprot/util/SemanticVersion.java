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

package de.sean.blockprot.util;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * Semantic version with full pre-release awareness.
 *
 * <p>Version format: {@code MAJOR.MINOR.PATCH[-SUFFIX[-N]]}
 *
 * <p>Pre-release order (lowest -> highest within the same numeric version):
 * <ol>
 *   <li>snapshot / bedev (BEDev, with "bdev" kept as a legacy alias; rolling
 *       experimental dev and beta-dev builds)</li>
 *   <li>(no suffix): clean release</li>
 *   <li>patch.N / fix.N / hotfix[.N]: post-release corrections, ranked
 *       ABOVE the clean release so a hotfix is always seen as an update
 *       by servers on its base version</li>
 * </ol>
 *
 * <p>Special suffixes:
 * <ul>
 *   <li>exp: experimental branches, never considered an update</li>
 *   <li>release: legacy tag suffix (e.g. "1.3.3-RELEASE"), normalized to
 *       a clean release; new tags never use it</li>
 * </ul>
 *
 * @since 0.1.11
 */
public class SemanticVersion implements Comparable<SemanticVersion> {

    private static final int RANK_SNAPSHOT = 0; // snapshot / bedev / bdev (BEDev builds)
    private static final int RANK_RELEASE  = 1; // no suffix
    private static final int RANK_HOTFIX   = 2; // patch.N / fix.N / hotfix[.N]

    private final int[] numeric;   // e.g. [1, 3, 0]
    private final String suffix;   // e.g. "bedev.2", "snapshot", "" for release
    private final int suffixRank;  // pre-computed rank
    private final int suffixN;     // numeric part of suffix (bedev.2 -> 2), 0 if absent

    public SemanticVersion(@NotNull final String version) {
        // Split on first '-' only: keeps "SNAPSHOT-3" together as extension.
        int dash = version.indexOf('-');
        String numericPart = dash == -1 ? version : version.substring(0, dash);
        String raw = dash == -1 ? "" : version.substring(dash + 1).toLowerCase(java.util.Locale.ENGLISH);

        String[] numParts = numericPart.split("\\.");
        numeric = new int[numParts.length];
        for (int i = 0; i < numParts.length; i++) {
            try { numeric[i] = Integer.parseInt(numParts[i].trim()); }
            catch (NumberFormatException ignored) {}
        }

        // Normalise suffix: "snapshot-3" and "snapshot" both become "snapshot".
        // Strip trailing numeric counter (e.g. "SNAPSHOT-3" -> "snapshot", "bedev.2" -> "bedev").
        String base = raw.replaceAll("-\\d+$", "").replaceAll("\\.\\d+$", "");

        // Extract numeric part of suffix if present (e.g. bedev.2 -> 2).
        int n = 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[.\\-](\\d+)$").matcher(raw);
        if (m.find()) { try { n = Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {} }
        suffixN = n;
        suffix  = raw;

        suffixRank = switch (base) {
            case "snapshot", "bedev", "bdev" -> RANK_SNAPSHOT; // BEDev (beta-dev) builds
            case "release"                   -> RANK_RELEASE; // legacy tags, normalized
            case "hotfix", "patch", "fix"    -> RANK_HOTFIX;
            default                          -> RANK_RELEASE; // "", "exp", ...
        };
    }

    public boolean isPreRelease() { return suffixRank < RANK_RELEASE; }

    /** Post-release correction (patch/fix/hotfix) of its base version. */
    public boolean isHotfix() { return suffixRank == RANK_HOTFIX; }

    public boolean isExperimental() { return suffix.startsWith("exp"); }

    /**
     * The plain numeric version this version is based on, without any
     * suffix (e.g. "1.3.4-hotfix" -> "1.3.4"). Useful to tell players
     * which release a hotfix corrects.
     */
    @NotNull
    public String baseVersion() {
        return Arrays.stream(numeric)
            .mapToObj(String::valueOf)
            .reduce((a, b) -> a + "." + b)
            .orElse("0");
    }

    @Override
    public int compareTo(@NotNull final SemanticVersion other) {
        // 1. Compare numeric parts.
        int len = Math.max(numeric.length, other.numeric.length);
        for (int i = 0; i < len; i++) {
            int a = i < numeric.length       ? numeric[i]       : 0;
            int b = i < other.numeric.length ? other.numeric[i] : 0;
            if (a != b) return Integer.compare(a, b);
        }
        // 2. Same numeric: compare suffix rank.
        if (suffixRank != other.suffixRank) return Integer.compare(suffixRank, other.suffixRank);
        // 3. Same rank: compare the suffix counter (bedev.1 < bedev.2).
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