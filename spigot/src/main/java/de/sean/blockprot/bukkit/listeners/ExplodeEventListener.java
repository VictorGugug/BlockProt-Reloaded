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

package de.sean.blockprot.bukkit.listeners;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.audit.AuditLogger;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ListIterator;
import java.util.UUID;

/**
 * Prevents explosions from destroying protected blocks and writes RAID_EXPLOSION
 * audit entries for every protected block hit by an explosion.
 *
 * <p>Detection and owner notifications are handled by {@link RaidDetectionListener},
 * which runs at {@code LOWEST} priority before this listener removes blocks from the
 * explosion list. Both listeners receive the same block list; removal here does not
 * affect the detection pass because LOWEST fires first.</p>
 */
public class ExplodeEventListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (BlockProt.getDefaultConfig().isWorldExcluded(event.getBlock().getWorld())) return;
        checkBlocks(event.blockList().listIterator(), null);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (BlockProt.getDefaultConfig().isWorldExcluded(event.getEntity().getWorld())) return;
        checkBlocks(event.blockList().listIterator(), event.getEntity());
    }

    private void checkBlocks(ListIterator<Block> it, @Nullable Entity source) {
        if (!BlockProt.getDefaultConfig().shouldProtectLockedBlocksFromExplosions()) return;

        String actorName = resolveActorName(source);
        UUID actorUuid   = (source instanceof Player p) ? p.getUniqueId() : new UUID(0L, 0L);

        while (it.hasNext()) {
            Block b = it.next();
            if (!BlockProt.getDefaultConfig().isLockable(b.getType())) continue;

            BlockNBTHandler handler;
            try {
                handler = new BlockNBTHandler(b);
            } catch (RuntimeException ignored) {
                // Block has no tile entity (e.g. a door or fence gate) — safe to skip.
                continue;
            }

            if (handler.isProtected()) {
                // Remove from explosion list — block is preserved.
                it.remove();

                // Write audit entry for this block.
                AuditLogger audit = BlockProt.getAuditLogger();
                if (audit != null) {
                    audit.log(actorUuid, actorName, b.getLocation(), AuditLogger.Action.RAID_EXPLOSION);
                }

                BlockProtLogger.log("raid-detection",
                    "PROTECTED block saved from explosion: " + b.getType().name()
                    + " at " + locString(b) + " actor=" + actorName);
            } else {
                // Lockable but unprotected — will be destroyed; evict cache.
                HopperEventListener.invalidate(b);

                BlockProtLogger.log("raid-detection",
                    "UNPROTECTED lockable destroyed by explosion: " + b.getType().name()
                    + " at " + locString(b) + " actor=" + actorName);
            }
        }
    }

    private static String resolveActorName(@Nullable Entity source) {
        if (source == null) return "environment";
        if (source instanceof Player p) return p.getName();
        return source.getType().name();
    }

    private static String locString(Block b) {
        String world = b.getWorld().getName();
        return world + " [" + b.getX() + "," + b.getY() + "," + b.getZ() + "]";
    }
}
