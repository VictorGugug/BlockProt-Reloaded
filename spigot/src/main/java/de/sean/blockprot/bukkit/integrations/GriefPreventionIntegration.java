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
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class GriefPreventionIntegration extends PluginIntegration implements Listener {

    private static final String RESTRICT_ACCESS = "restrict_access_to_claim_owner";

    private boolean enabled = false;

    public GriefPreventionIntegration() {
        super("griefprevention");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        if (!configuration.getBoolean("enabled", true)) return;
        Plugin plugin = getPlugin();
        if (plugin == null || !plugin.isEnabled()) return;
        try {
            this.registerListener(this);
            enabled = true;
        } catch (NoClassDefFoundError | IllegalStateException ignored) {
        }
    }

    @Nullable
    @Override
    public Plugin getPlugin() {
        return BlockProt.getInstance().getPlugin("GriefPrevention");
    }

    private boolean shouldRestrictAccessFully() {
        return configuration.getBoolean(RESTRICT_ACCESS, false);
    }

    @Nullable
    private UUID getClaimOwner(@NotNull Block block) {
        try {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(block.getLocation(), false, null);
            if (claim != null && !claim.isAdminClaim()) {
                return claim.getOwnerID();
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isOwner(@NotNull Player player, @NotNull Block block) {
        UUID owner = getClaimOwner(block);
        return owner != null && owner.equals(player.getUniqueId());
    }

    @EventHandler
    public void onAccess(@NotNull final BlockAccessEvent event) {
        if (!shouldRestrictAccessFully()) return;
        if (!isOwner(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAccessEditMenu(@NotNull final BlockAccessMenuEvent event) {
        if (!shouldRestrictAccessFully()) return;
        if (!isOwner(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLockOnPlace(@NotNull final BlockLockOnPlaceEvent event) {
        if (!isOwner(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }
}
