/*
 * Copyright (C) 2021 - 2026 spnda
 * Modifications Copyright (C) 2025 - 2026 Zaynr (Zar)
 * This file is part of BlockProt Reloaded <https://github.com/VictorGugug/BlockProt-Reloaded>.
 * Based on BlockProt <https://github.com/spnda/BlockProt>.
 *
 * BlockProt Reloaded is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt Reloaded is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt Reloaded. If not, see <https://www.gnu.org/licenses/>.
 */

package de.sean.blockprot.bukkit.config;

import de.sean.blockprot.bukkit.BlockProt;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public final class IntegrationConfig {
    private static FileConfiguration config;

    private IntegrationConfig() {
    }

    public static void reload() {
        BlockProt plugin = BlockProt.getInstance();
        File file = new File(plugin.getDataFolder(), "integrations.yml");
        if (!file.exists()) {
            plugin.saveResource("integrations.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private static void ensureLoaded() {
        if (config == null) {
            reload();
        }
    }

    @NotNull
    public static YamlConfiguration getSection(@NotNull final String integrationName) {
        ensureLoaded();
        ConfigurationSection section = config.getConfigurationSection(integrationName);
        YamlConfiguration result = new YamlConfiguration();
        if (section != null) {
            for (String key : section.getKeys(true)) {
                if (!section.isConfigurationSection(key)) {
                    result.set(key, section.get(key));
                }
            }
        }
        return result;
    }

    public static boolean getBoolean(@NotNull final String path, final boolean def) {
        ensureLoaded();
        return config.getBoolean(path, def);
    }

    public static int getInt(@NotNull final String path, final int def) {
        ensureLoaded();
        return config.getInt(path, def);
    }

    public static long getLong(@NotNull final String path, final long def) {
        ensureLoaded();
        return config.getLong(path, def);
    }
}
