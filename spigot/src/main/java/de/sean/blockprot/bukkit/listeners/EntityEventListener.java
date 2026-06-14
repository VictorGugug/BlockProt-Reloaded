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
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.jetbrains.annotations.NotNull;

public final class EntityEventListener implements Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(@NotNull final EntityChangeBlockEvent event) {
        Material blockType = event.getBlock().getType();
        if (blockType == Material.AIR || blockType == Material.CAVE_AIR || blockType == Material.VOID_AIR) return;

        Entity entity = event.getEntity();
        if (entity instanceof FallingBlock) {
            Material mat = ((FallingBlock) entity).getBlockData().getMaterial();

            if (mat.toString().contains("ANVIL") &&
                BlockProt.getDefaultConfig().isLockableBlock(mat)) {
                if (BlockProt.getDefaultConfig().isLockable(blockType)) {
                    BlockNBTHandler handler = new BlockNBTHandler(event.getBlock());
                    if (handler.isProtected()) event.setCancelled(true);
                }
            }
        } else if (entity instanceof Enderman) {
            if (BlockProt.getDefaultConfig().isLockable(blockType)) {
                BlockNBTHandler handler = new BlockNBTHandler(event.getBlock());
                if (handler.isProtected()) event.setCancelled(true);
            }
        } else if (BlockProt.getDefaultConfig().isLockable(blockType)) {
            BlockNBTHandler handler = new BlockNBTHandler(event.getBlock());
            if (handler.isProtected()) event.setCancelled(true);
        }
    }

    /**
     * Prevents Chest Minecarts from entering a column directly below a protected chest.
     * A chest minecart on the rails under a chest can pull items without going through
     * the hopper pipeline, bypassing {@link HopperEventListener}.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStorageMinecartEnter(@NotNull VehicleEnterEvent event) {
        if (!(event.getVehicle() instanceof StorageMinecart)) return;
        // Check the block directly above the minecart location for a protected chest
        Block above = event.getVehicle().getLocation().getBlock().getRelative(0, 1, 0);
        if (BlockProt.getDefaultConfig().isLockableTileEntity(above.getType())) {
            BlockNBTHandler handler;
            try { handler = new BlockNBTHandler(above); } catch (RuntimeException ignored) { return; }
            if (handler.isProtected()) event.setCancelled(true);
        }
    }
}
