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

package de.sean.blockprot.bukkit.admin;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Administrative permission hierarchy tiers.
 */
public enum AdminTier {
    NONE("none", 0),
    T1("t1", 1),
    T2("t2", 2),
    T3("t3", 3),
    OWNER("owner", 4),
    CUSTOM("custom", -1);

    private final String identifier;
    private final int level;

    AdminTier(String identifier, int level) {
        this.identifier = identifier;
        this.level = level;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getLevel() {
        return level;
    }

    public boolean includes(@NotNull AdminTier other) {
        if (this == OWNER) return true;
        if (this == CUSTOM || other == CUSTOM) return false;
        return this.level >= other.level;
    }

    @NotNull
    public static AdminTier fromString(@NotNull String name) {
        String lower = name.trim().toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "t1", "1", "low", "mod" -> T1;
            case "t2", "2", "medium", "helper" -> T2;
            case "t3", "3", "high", "admin" -> T3;
            case "owner", "t4", "4" -> OWNER;
            case "custom" -> CUSTOM;
            case "user", "normal", "none", "0" -> NONE;
            default -> NONE;
        };
    }
}
