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

package de.sean.blockprot.bukkit.integrations;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.IntegrationConfig;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public final class GeyserIntegration extends PluginIntegration {

    private boolean enabled = false;

    public GeyserIntegration() {
        super("geyser", false);
    }

    @Override
    public void reload() {}

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        if (!IntegrationConfig.getBoolean("geyser.enabled", true)) return;
        Plugin geyser = getPlugin();
        if (geyser == null || !geyser.isEnabled()) return;

        enabled = true;
        BlockProtLogger.log("integration",
            Translator.get(TranslationKey.CONSOLE__INTEGRATION_ENABLED).replace("{name}", "Geyser"));
    }

    @Override
    @Nullable
    public Plugin getPlugin() {
        Plugin p = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        if (p == null) p = Bukkit.getPluginManager().getPlugin("geyser");
        if (p == null) p = Bukkit.getPluginManager().getPlugin("Geyser");
        return p;
    }
}
