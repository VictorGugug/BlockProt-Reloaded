/*
 * Copyright (C) 2025 Zaynr (Zar)
 * This file is part of BlockProt Reloaded <https://github.com/VictorGugug/BlockProt-Reloaded>.
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
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.inventories.BlockLockInventory;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Handles protection for chest boats and storage / hopper minecarts.
 *
 * <p>When a player sneaks and right-clicks one of these entities with an empty
 * hand the BlockProt protection menu opens (identical UX to blocks). Once
 * protected, the inventory is inaccessible to non-owners/friends.</p>
 *
 * <p>Entity destruction (killing the vehicle) by non-owners is blocked via
 * {@link EntityEventListener}.</p>
 */
public final class VehicleProtectionListener implements Listener {

    /**
     * ChestBoat moved to org.bukkit.entity.boat.ChestBoat in 1.21.
     * Loaded once via reflection so we compile against 1.20.6 and still
     * match the entity at runtime on any version.
     */
    private static final Class<?> CHEST_BOAT_CLASS = resolveChestBoatClass();

    private static Class<?> resolveChestBoatClass() {
        // 1.21+ location
        try { return Class.forName("org.bukkit.entity.boat.ChestBoat"); } catch (ClassNotFoundException ignored) {}
        // 1.20.x location
        try { return Class.forName("org.bukkit.entity.ChestBoat"); } catch (ClassNotFoundException ignored) {}
        return null;
    }

    /**
     * Intercepts right-click interactions on chest boats and chest/hopper minecarts.
     * <ul>
     *   <li>Sneaking + empty hand → open protection menu.</li>
     *   <li>Normal right-click on a protected vehicle → deny inventory access to
     *       non-owners/friends.</li>
     * </ul>
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleInteract(@NotNull PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();

        // Only handle chest boats, storage minecarts, and hopper minecarts.
        boolean isChestBoat    = CHEST_BOAT_CLASS != null && CHEST_BOAT_CLASS.isInstance(entity);
        boolean isStorageCart  = entity instanceof StorageMinecart;
        boolean isHopperCart   = entity instanceof HopperMinecart;
        if (!isChestBoat && !isStorageCart && !isHopperCart) return;

        Player player = event.getPlayer();
        if (BlockProt.getDefaultConfig().isWorldExcluded(player.getWorld())) return;

        EntityNBTHandler handler = new EntityNBTHandler(entity);

        if (player.isSneaking()) {
            // Sneaking + empty main hand → open protection menu.
            var mainHand = player.getInventory().getItemInMainHand();
            if (!mainHand.getType().isAir()) return;

            event.setCancelled(true);

            if (!player.hasPermission(Permissions.USER.key())) {
                sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                return;
            }

            InventoryState state = InventoryState.getOrCreate(player.getUniqueId());
            state.entityUUID = entity.getUniqueId();

            var inv = new BlockLockInventory().fillForEntity(player, handler);
            if (inv == null) {
                sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                return;
            }
            player.openInventory(inv);
        } else {
            // Normal right-click → block inventory open if protected and no access.
            if (!handler.isProtected()) return;
            if (handler.canAccess(player.getUniqueId().toString())
                    || player.hasPermission(Permissions.USER_ADMIN.key())) return;
            event.setCancelled(true);
            sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
        }
    }

    private void sendActionBar(@NotNull Player player, @NotNull String text) {
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(text));
    }
}
