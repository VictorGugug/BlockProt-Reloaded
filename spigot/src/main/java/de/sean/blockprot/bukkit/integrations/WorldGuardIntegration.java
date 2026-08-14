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
import de.sean.blockprot.bukkit.events.BlockAccessEvent;
import de.sean.blockprot.bukkit.events.BlockAccessMenuEvent;
import de.sean.blockprot.bukkit.events.BlockLockOnPlaceEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * WorldGuard integration. Registers a custom region flag, {@code allow-blockprot},
 * defaulting to allow. Server admins can set this flag to deny on specific regions
 * (via {@code /rg flag <region> allow-blockprot deny}) to prevent BlockProt
 * protections from being created or accessed inside that region, while everywhere
 * else behaves exactly as if WorldGuard were not installed.
 *
 * <p>The flag is registered in {@link #load()}, which {@code BlockProt.onLoad()}
 * calls for every registered integration; this must happen while WorldGuard is
 * loaded but not yet enabled, per WorldGuard's custom-flag registration timing.
 *
 * <p>All WorldGuard-specific logic lives in {@link WorldGuardSupport}, a separate
 * class loaded lazily: WorldGuard classes cannot be referenced from this class at
 * all, not even inside method bodies, because the JVM eagerly resolves the types
 * participating in cross-name assignability checks while verifying the class, so
 * any direct reference would throw {@code NoClassDefFoundError} at construction
 * time on servers without WorldGuard installed.
 */
public final class WorldGuardIntegration extends PluginIntegration implements Listener {

    private static final String ENABLE_FLAG_FUNCTIONALITY = "enable_flag_functionality";

    private boolean enabled = false;

    public WorldGuardIntegration() {
        super("worldguard");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void load() {
        if (getPlugin() == null) return;
        try {
            WorldGuardSupport.registerFlag();
        } catch (NoClassDefFoundError ignored) {
        }
    }

    @Override
    public void enable() {
        if (!configuration.getBoolean("enabled", true)) return;
        final Plugin plugin = getPlugin();
        if (plugin == null || !plugin.isEnabled()) {
            return;
        }
        this.registerListener(this);
        enabled = true;
    }

    @Nullable
    @Override
    public Plugin getPlugin() {
        return BlockProt.getInstance().getPlugin("WorldGuard");
    }

    /**
     * True if BlockProt protections are allowed at {@code block}'s location, per
     * the {@code allow-blockprot} region flag. Always true when flag functionality
     * is disabled in config or when WorldGuard is not installed.
     */
    private boolean isAllowedByFlag(@NotNull Player who, @NotNull Block block) {
        try {
            if (!configuration.getBoolean(ENABLE_FLAG_FUNCTIONALITY, true)) return true;
            return WorldGuardSupport.isAllowedByFlag(who, block);
        } catch (NoClassDefFoundError ignored) {
            return true;
        }
    }

    @EventHandler
    public void onAccess(@NotNull final BlockAccessEvent event) {
        if (!isAllowedByFlag(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAccessEditMenu(@NotNull final BlockAccessMenuEvent event) {
        if (!isAllowedByFlag(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLockOnPlace(@NotNull final BlockLockOnPlaceEvent event) {
        if (!isAllowedByFlag(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }
}