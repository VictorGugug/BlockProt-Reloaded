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

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Runtime Minecraft version detection utility.
 *
 * <p>Works across the classic 1.x numbering and the new year-based
 * numbering introduced in 2026 (26.x, 27.x, …).
 *
 * <p>Usage:
 * <pre>{@code
 * if (VersionCompat.isAtLeast(1, 21, 4)) { ... }
 * if (VersionCompat.is26Family())        { ... }
 * }</pre>
 *
 * @since 1.2.9
 */
public final class VersionCompat {

    /**
     * Returns true if the server is running the new year-based version scheme (26.x, 27.x, …).
     * Under the new scheme the first segment is a two-digit year (≥ 26), not "1".
     */
    public static final boolean NEW_SCHEME;

    public static final int MAJOR;
    public static final int MINOR;
    public static final int PATCH;

    static {
        // Bukkit.getMinecraftVersion() returns e.g. "1.21.4" or "26.1.2"
        String raw = Bukkit.getMinecraftVersion();
        String[] parts = raw.split("\\.");

        int major = 0, minor = 0, patch = 0;
        try {
            if (parts.length >= 1) major = Integer.parseInt(parts[0]);
            if (parts.length >= 2) minor = Integer.parseInt(parts[1]);
            if (parts.length >= 3) patch = Integer.parseInt(parts[2]);
        } catch (NumberFormatException ignored) {}

        MAJOR = major;
        MINOR = minor;
        PATCH = patch;
        // Year-based scheme: first segment ≥ 26 AND ≤ 99 (avoids mistaking a 1.x build)
        NEW_SCHEME = (major >= 26 && major <= 99);
    }

    private VersionCompat() {}

    public static boolean isAtLeast(int major, int minor, int patch) {
        if (MAJOR != major) return MAJOR > major;
        if (MINOR != minor) return MINOR > minor;
        return PATCH >= patch;
    }

    public static boolean isAtLeast(int major, int minor) {
        return isAtLeast(major, minor, 0);
    }

    public static boolean is26Family() {
        return NEW_SCHEME;
    }

    /**
     * Returns true if the server is on Paper 1.21.4 or later.
     * This is relevant because 1.21.4 is when Paper hard-forked from Spigot
     * and introduced typed inventory views ({@code InventoryView} became an interface).
     */
    public static boolean hasTypedInventoryViews() {
        return isAtLeast(1, 21, 4);
    }

    public static boolean isPaper() {
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @NotNull
    public static String getVersionString() {
        return Bukkit.getMinecraftVersion();
    }

    @NotNull
    public static String getDiagnosticString() {
        return String.format("MC %s (%s, %s)",
            getVersionString(),
            NEW_SCHEME ? "year-based " + MAJOR + ".x" : "classic 1.x",
            isPaper() ? "Paper" : "Spigot"
        );
    }
}
