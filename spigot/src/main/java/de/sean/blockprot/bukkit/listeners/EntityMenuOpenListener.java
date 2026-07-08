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
import de.sean.blockprot.bukkit.entities.EntityProtectionHandler;
import de.sean.blockprot.bukkit.inventories.EntitySettingsInventory;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Opens the {@link EntitySettingsInventory} when a player right-clicks their own
 * tamed entity while holding a <strong>stick</strong> (or the item configured via
 * {@code entity_protection.menu_item} in config.yml).
 *
 * <p>The trigger item defaults to {@link Material#STICK} so it does not conflict
 * with normal interactions (feeding, sitting, etc.) which require specific items
 * or an empty hand.
 *
 * <p>Renamed from {@code PetMenuOpenListener} as part of the pet -> entity protection
 * rename. Behaviour is unchanged.
 */
public final class EntityMenuOpenListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEntityEvent event) {
        if (!BlockProt.getDefaultConfig().isEntityProtectionEnabled()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Tameable)) return;

        Player player = event.getPlayer();
        Material menuItem = BlockProt.getDefaultConfig().getEntityProtectionMenuItem();
        if (player.getInventory().getItemInMainHand().getType() != menuItem) return;

        EntityProtectionHandler handler = new EntityProtectionHandler(clicked);
        boolean isOwner = handler.isOwner(player.getUniqueId())
            || (((Tameable) clicked).getOwnerUniqueId() != null
                && ((Tameable) clicked).getOwnerUniqueId().equals(player.getUniqueId()));
        boolean isAdmin = player.hasPermission("blockprot.admin");
        if (!isOwner && !isAdmin) {
            player.sendMessage(BlockProt.getDefaultConfig().getEntityProtectionDeniedMessage());
            event.setCancelled(true);
            return;
        }

        InventoryState state = InventoryState.getOrCreate(player.getUniqueId());
        state.setEntityProtectionId(clicked.getUniqueId());

        Inventory inv = new EntitySettingsInventory().fill(player, clicked);
        if (inv != null) {
            event.setCancelled(true); // prevent vanilla interaction (sitting toggle, etc.)
            player.openInventory(inv);
        }
    }
}