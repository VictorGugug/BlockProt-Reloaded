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

package de.sean.blockprot.bukkit;

import org.jetbrains.annotations.NotNull;

/**
 * Runtime validation of compatibility with the running Minecraft/Paper version.
 *
 * <p>Performs checks on plugin load to detect and warn about known issues or
 * unsupported configurations. All checks are non-fatal; the plugin still loads,
 * but administrators are warned about potential problems.</p>
 *
 * <p>Checks include:
 * <ul>
 *   <li>Is Paper available? (required for ChatInput, optional for core features)</li>
 *   <li>Is Java 21+? (required by this fork's class file target)</li>
 *   <li>Typed inventory views availability (1.21.4+)</li>
 * </ul></p>
 *
 * @since 1.2.9
 */
public final class VersionValidator {

    private VersionValidator() {}

    public static void validateStartup() {
        String javaVersion = System.getProperty("java.version");
        int javaMajor = extractMajorVersion(javaVersion);
        boolean isPaper = VersionCompat.isPaper();
        boolean hasTypedViews = VersionCompat.hasTypedInventoryViews();

        if (javaMajor < 21) {
            warn(Translator.get(TranslationKey.CONSOLE__JAVA_TOO_OLD)
                .replace("{version}", javaVersion));
            BlockProtLogger.warn("Java " + javaVersion + " detected — requires Java 21 or higher.");
        }

        if (!isPaper) {
            warn(Translator.get(TranslationKey.CONSOLE__NOT_PAPER));
            BlockProtLogger.warn("Not running on Paper. Chat-based player search requires Paper/PaperMC. " +
                "Falling back to anvil GUI.");
        }

        if (!hasTypedViews && VersionCompat.isAtLeast(1, 20, 0) && !VersionCompat.is26Family()) {
            warn(Translator.get(TranslationKey.CONSOLE__TYPED_VIEWS_FALLBACK));
            BlockProtLogger.warn("Typed inventory views unavailable (1.21.0–1.21.3). Using fallback methods.");
        }

        if (javaMajor >= 21 && isPaper && (hasTypedViews || !VersionCompat.isAtLeast(1, 20, 0))) {
            BlockProtLogger.log("startup-checks",
                "Java " + javaVersion + " OK | Paper OK | TypedViews " + (hasTypedViews ? "OK" : "N/A"));
        }
    }

    private static int extractMajorVersion(@NotNull String versionString) {
        try {
            String[] parts = versionString.split("\\.");
            if (parts.length > 0) {
                return Integer.parseInt(parts[0]);
            }
        } catch (NumberFormatException ignored) {}
        return -1;
    }

    private static void warn(@NotNull String message) {
        BlockProt.getInstance().getLogger().warning(message);
    }
}
