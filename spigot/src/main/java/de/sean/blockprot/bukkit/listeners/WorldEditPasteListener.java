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

package de.sean.blockprot.bukkit.listeners;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.config.IntegrationConfig;
import de.sean.blockprot.bukkit.events.BlockProtLockEvent;
import de.sean.blockprot.bukkit.integrations.PluginIntegration;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Lightweight WorldEdit paste watcher.
 *
 * <p>WorldEdit's preferred low-level hook is EditSessionEvent, but using a
 * command watcher keeps BlockProt independent from WorldEdit/FAWE internals.
 * When enabled, a delayed bounded scan locks newly pasted lockable blocks near
 * the command sender.</p>
 */
public final class WorldEditPasteListener implements Listener {
    private final BlockProt plugin;

    public WorldEditPasteListener(@NotNull BlockProt plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldEditPasteCommand(@NotNull PlayerCommandPreprocessEvent event) {
        if (!IntegrationConfig.getBoolean("worldedit.paste_autolock.enabled", false)) return;
        if (!isPasteCommand(event.getMessage())) return;

        Player player = event.getPlayer();
        Location origin = player.getLocation().clone();
        long delay = IntegrationConfig.getLong("worldedit.paste_autolock.delay_ticks", 20L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> scanAndLock(player, origin), delay);
        BlockProtLogger.log("worldedit-paste", "Detected paste command from " + player.getName()
            + " at " + format(origin) + "; scheduled bounded NBT scan.");
    }

    private boolean isPasteCommand(@NotNull String message) {
        String normalized = message.trim().toLowerCase();
        return normalized.equals("//paste")
            || normalized.startsWith("//paste ")
            || normalized.equals("/paste")
            || normalized.startsWith("/paste ")
            || normalized.equals("/worldedit:/paste")
            || normalized.startsWith("/worldedit:/paste ")
            || normalized.equals("/worldedit:paste")
            || normalized.startsWith("/worldedit:paste ");
    }

    private void scanAndLock(@NotNull Player player, @NotNull Location origin) {
        World world = origin.getWorld();
        if (world == null || !player.isOnline()) return;
        if (BlockProt.getDefaultConfig().isWorldExcluded(world)) return;

        int radius = IntegrationConfig.getInt("worldedit.paste_autolock.radius", 24);
        int maxBlocks = IntegrationConfig.getInt("worldedit.paste_autolock.max_blocks_per_paste", 5000);
        int locked = 0;
        int scanned = 0;

        int minY = Math.max(world.getMinHeight(), origin.getBlockY() - radius);
        int maxY = Math.min(world.getMaxHeight() - 1, origin.getBlockY() + radius);

        PlayerSettingsHandler settings = new PlayerSettingsHandler(player);
        for (int x = origin.getBlockX() - radius; x <= origin.getBlockX() + radius; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = origin.getBlockZ() - radius; z <= origin.getBlockZ() + radius; z++) {
                    if (++scanned > maxBlocks) {
                        logSummary(player, origin, locked, scanned, true);
                        return;
                    }

                    Block block = world.getBlockAt(x, y, z);
                    if (!BlockProt.getDefaultConfig().isLockable(block.getType(), world)) continue;

                    try {
                        BlockNBTHandler handler = new BlockNBTHandler(block);
                        if (!handler.isNotProtected()) continue;
                        if (!handler.lockBlock(player, BlockProtLockEvent.Cause.WORLDEDIT_PASTE).success) continue;

                        settings.getFriendsStream()
                            .filter(fh -> PluginIntegration.filterFriendByUuidForAll(
                                UUID.fromString(fh.getName()), player, block))
                            .forEach(handler::addFriend);
                        locked++;
                    } catch (RuntimeException ignored) { }
                }
            }
        }
        logSummary(player, origin, locked, scanned, false);
    }

    private void logSummary(@NotNull Player player, @NotNull Location origin, int locked, int scanned, boolean capped) {
        BlockProtLogger.log("worldedit-paste", "Paste scan for " + player.getName()
            + " at " + format(origin) + ": locked=" + locked + ", scanned=" + scanned
            + (capped ? ", stopped=max_blocks_per_paste" : ""));
    }

    private String format(@NotNull Location location) {
        return (location.getWorld() != null ? location.getWorld().getName() : "unknown")
            + " " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }
}