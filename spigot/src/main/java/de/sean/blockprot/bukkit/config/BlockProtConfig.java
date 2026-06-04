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

package de.sean.blockprot.bukkit.config;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.VersionCompat;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a basic YAML configuration file with some
 * basic utility functions for manipulating common config
 * data.
 */
public abstract class BlockProtConfig {
    /**
     * The FileConfiguration backing this {@link BlockProtConfig} wrapper.
     */
    @NotNull
    protected final FileConfiguration config;

    /**
     * Create a new config.
     *
     * @param config The {@link FileConfiguration} to use.
     */
    public BlockProtConfig(@NotNull final FileConfiguration config) {
        this.config = config;
    }

    /**
     * Checks whether or not the given {@code list} contains the {@code query} String. It checks
     * each item using {@link String#equalsIgnoreCase(String)}.
     *
     * @param list  The list of strings to check.
     * @param query The string to compare to.
     * @return True, if any item of {@code list} qualifies for {@link String#equalsIgnoreCase(String)}
     * with {@code query}.
     */
    protected boolean listContainsIgnoreCase(@NotNull final List<String> list, @NotNull final String query) {
        for (String item : list) {
            if (item.equalsIgnoreCase(query)) return true;
        }
        return false;
    }

    /**
     * Filter a list of enum values by a list of names.
     *
     * <p>Names that don't match any enum constant are silently routed to the session log
     * rather than emitting a console WARNING. This is the expected behaviour when blocks.yml
     * lists materials that were added in a newer MC version than the one currently running
     * (e.g. COPPER_TRAPPED_CHEST listed for 26.1 but server is on 1.21.4).
     *
     * @param enumValues The list of enum values we want to filter.
     * @param names      The list of strings we want to filter by. Warning: This list will be modified.
     * @param <T>        The enum class.
     * @return A set of all {@code <T>} enum values that we found.
     */
    @NotNull
    protected <T extends Enum<?>> Set<T> loadEnumValuesByName(@NotNull final T[] enumValues, @NotNull final ArrayList<String> names) {
        final Set<T> ret = new HashSet<>();
        for (T value : enumValues) {
            if (listContainsIgnoreCase(names, value.name())) {
                ret.add(value);
                names.remove(value.name());
            }
        }
        if (!names.isEmpty()) {
            // Route to session log only — these are blocks defined for a newer MC version
            // than what is currently running, which is normal and expected.
            BlockProtLogger.log("blocks-compat",
                "Skipped " + names.size() + " material(s) not present in MC "
                + VersionCompat.getVersionString() + ": " + names);
        }
        return ret;
    }
}
