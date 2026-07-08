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

package de.sean.blockprot.bukkit.integrations;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
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
 */
public final class WorldGuardIntegration extends PluginIntegration implements Listener {

    private static final String ENABLE_FLAG_FUNCTIONALITY = "enable_flag_functionality";

    @Nullable
    private static StateFlag ALLOW_BLOCKPROT_FLAG;

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
            var registry = WorldGuard.getInstance().getFlagRegistry();
            StateFlag flag = new StateFlag("allow-blockprot", true);
            registry.register(flag);
            ALLOW_BLOCKPROT_FLAG = flag;
        } catch (FlagConflictException e) {
            var existing = WorldGuard.getInstance().getFlagRegistry().get("allow-blockprot");
            if (existing instanceof StateFlag sf) {
                ALLOW_BLOCKPROT_FLAG = sf;
            }
        } catch (NoClassDefFoundError ignored) {
            ALLOW_BLOCKPROT_FLAG = null;
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

    private boolean isFlagFunctionalityEnabled() {
        return configuration.getBoolean(ENABLE_FLAG_FUNCTIONALITY, true);
    }

    /**
     * True if BlockProt protections are allowed at {@code block}'s location, per
     * the {@code allow-blockprot} region flag. Always true if the flag failed to
     * register, if flag functionality is disabled in config, or if no region
     * covering the block sets the flag to deny.
     */
    private boolean isAllowedByFlag(@NotNull Player who, @NotNull Block block) {
        if (ALLOW_BLOCKPROT_FLAG == null || !isFlagFunctionalityEnabled()) return true;
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        var localPlayer = WorldGuardPlugin.inst().wrapPlayer(who);
        return query.testState(BukkitAdapter.adapt(block.getLocation()), localPlayer, ALLOW_BLOCKPROT_FLAG);
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
