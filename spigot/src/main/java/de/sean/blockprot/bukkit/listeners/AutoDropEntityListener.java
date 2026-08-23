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
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Auto-drop to inventory for lockable entities: item frames and chest
 * boats / storage / hopper minecarts. Runs at HIGH, mirroring the block
 * auto-drop decision and the access rules the HIGHEST protection
 * listeners enforce, so blocked breaks never deliver items.
 */
public final class AutoDropEntityListener implements Listener {

    /**
     * ChestBoat moved to {@code org.bukkit.entity.boat.ChestBoat} in 1.21.
     * Resolved once at class-load time via reflection.
     */
    private static final Class<?> CHEST_BOAT_CLASS = resolveChestBoatClass();

    private static Class<?> resolveChestBoatClass() {
        try { return Class.forName("org.bukkit.entity.boat.ChestBoat"); } catch (ClassNotFoundException ignored) {}
        try { return Class.forName("org.bukkit.entity.ChestBoat");      } catch (ClassNotFoundException ignored) {}
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFrameBreak(@NotNull HangingBreakByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;

        Player player = resolvePlayer(event.getRemover());
        if (player == null) return;

        Material mat = frame instanceof GlowItemFrame ? Material.GLOW_ITEM_FRAME : Material.ITEM_FRAME;
        if (!BlockProt.getDefaultConfig().isAutoDropToInventory(mat)) return;
        if (!BlockProt.getDefaultConfig().isAutoDropToInventoryEnabled(frame.getWorld())) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (!frameAccessAllowed(player, frame)) return;

        event.setCancelled(true);
        ItemStack content = frame.getItem();
        if (content.getType() != Material.AIR) {
            addToInventory(player, content);
        }
        addToInventory(player, new ItemStack(mat));
        frame.remove();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDestroy(@NotNull VehicleDestroyEvent event) {
        Entity vehicle = event.getVehicle();
        Material mat = resolveVehicleMaterial(vehicle);
        if (mat == null) return;
        if (!BlockProt.getDefaultConfig().isAutoDropToInventory(mat)) return;
        if (!BlockProt.getDefaultConfig().isAutoDropToInventoryEnabled(vehicle.getWorld())) return;

        Player player = resolvePlayer(event.getAttacker());
        if (player == null) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (!vehicleAccessAllowed(player, vehicle)) return;

        event.setCancelled(true);
        addToInventory(player, new ItemStack(mat));
        if (vehicle instanceof InventoryHolder holder) {
            for (ItemStack item : holder.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    addToInventory(player, item);
                }
            }
        }
        vehicle.remove();
    }

    /**
     * Mirrors {@link de.sean.blockprot.bukkit.listeners.ItemFrameListener#onFrameBreakByPlayer}:
     * a linked frame follows its block's protection, a standalone frame
     * follows its own.
     */
    private boolean frameAccessAllowed(@NotNull Player player, @NotNull ItemFrame frame) {
        EntityNBTHandler handler = new EntityNBTHandler(frame);
        Block linkedBlock = resolveLinkedBlock(handler);
        if (linkedBlock != null) {
            try {
                BlockNBTHandler blockHandler = new BlockNBTHandler(linkedBlock);
                if (!blockHandler.isProtected()) return true;
                return blockHandler.canAccess(player.getUniqueId().toString())
                    || player.hasPermission(Permissions.USER_ADMIN.key());
            } catch (RuntimeException ignored) {
                return true;
            }
        }
        if (!handler.isProtected()) return true;
        return handler.canAccess(player.getUniqueId().toString())
            || player.hasPermission(Permissions.USER_ADMIN.key());
    }

    /**
     * Mirrors {@link de.sean.blockprot.bukkit.listeners.VehicleProtectionListener#canModifyVehicle}:
     * only the owner or an admin may break a protected vehicle.
     */
    private boolean vehicleAccessAllowed(@NotNull Player player, @NotNull Entity vehicle) {
        EntityNBTHandler handler = new EntityNBTHandler(vehicle);
        if (!handler.isProtected()) return true;
        return handler.isOwner(player.getUniqueId().toString())
            || player.hasPermission(Permissions.USER_ADMIN.key());
    }

    /**
     * Resolves the block linked to this frame, if any, encoded as
     * {@code "world,x,y,z"}; returns null when not linked or stale.
     */
    @Nullable
    private Block resolveLinkedBlock(@NotNull EntityNBTHandler handler) {
        String encoded = handler.getLinkedBlock();
        if (encoded.isEmpty()) return null;
        String[] parts = encoded.split(",");
        if (parts.length != 4) return null;
        try {
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return world.getBlockAt(x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private Material resolveVehicleMaterial(@NotNull Entity entity) {
        if (CHEST_BOAT_CLASS != null && CHEST_BOAT_CLASS.isInstance(entity)) {
            try {
                return Material.matchMaterial(entity.getType().name());
            } catch (Exception ignored) {
                return Material.matchMaterial("CHEST_BOAT");
            }
        }
        if (entity instanceof StorageMinecart) return Material.CHEST_MINECART;
        if (entity instanceof HopperMinecart)  return Material.HOPPER_MINECART;
        return null;
    }

    @Nullable
    private Player resolvePlayer(@Nullable Entity attacker) {
        if (attacker instanceof Player p) return p;
        if (attacker instanceof org.bukkit.entity.Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player p) return p;
        }
        return null;
    }

    private void addToInventory(@NotNull Player player, @NotNull ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack overflow : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflow);
        }
    }
}
