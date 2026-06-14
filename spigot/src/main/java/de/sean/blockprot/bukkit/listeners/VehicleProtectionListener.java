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
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.audit.AuditLogger;
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
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles protection for chest boats and storage / hopper minecarts.
 *
 * <p>When a player sneaks and right-clicks one of these entities with an empty
 * hand the BlockProt protection menu opens (identical UX to blocks). Once
 * protected, the inventory is inaccessible to non-owners/friends.</p>
 *
 * <p>Entity destruction (killing the vehicle) by non-owners is blocked.</p>
 *
 * <p>Hopper-pipeline extraction is also blocked: when a hopper or hopper minecart
 * attempts to pull items from a protected storage/hopper minecart or chest boat,
 * the {@link InventoryMoveItemEvent} is cancelled.</p>
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

        boolean isChestBoat    = CHEST_BOAT_CLASS != null && CHEST_BOAT_CLASS.isInstance(entity);
        boolean isStorageCart  = entity instanceof StorageMinecart;
        boolean isHopperCart   = entity instanceof HopperMinecart;
        if (!isChestBoat && !isStorageCart && !isHopperCart) return;

        Player player = event.getPlayer();
        if (BlockProt.getDefaultConfig().isWorldExcluded(player.getWorld())) return;

        EntityNBTHandler handler = new EntityNBTHandler(entity);

        if (player.isSneaking()) {
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
            BlockProtLogger.log("entity-protection", "OPENED protection menu for: "
                + entity.getType().name() + " entity=" + entity.getUniqueId() + " player=" + player.getName());
        } else {
            if (!handler.isProtected()) return;
            if (handler.canAccess(player.getUniqueId().toString())
                    || player.hasPermission(Permissions.USER_ADMIN.key())) return;
            event.setCancelled(true);
            sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            AuditLogger audit = BlockProt.getAuditLogger();
            if (audit != null) {
                audit.log(player.getUniqueId(), player.getName(), entity.getLocation(), AuditLogger.Action.ACCESS_DENIED);
            }
            BlockProtLogger.log("entity-protection", "ACCESS_DENIED interact: "
                + entity.getType().name() + " entity=" + entity.getUniqueId() + " player=" + player.getName());
        }
    }

    /**
     * Blocks hopper pipelines from extracting items out of protected storage vehicles.
     *
     * <p>Hoppers and hopper minecarts can pull items from the inventory of a
     * StorageMinecart, HopperMinecart, or ChestBoat via {@link InventoryMoveItemEvent}
     * without triggering {@link PlayerInteractEntityEvent}. This handler intercepts
     * that pipeline and cancels it when the source entity is protected.</p>
     *
     * <p>Note: the source inventory holder for an entity-backed inventory is the entity
     * itself (which implements InventoryHolder). We retrieve it and check NBT.</p>
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopperPullFromVehicle(@NotNull InventoryMoveItemEvent event) {
        InventoryHolder sourceHolder = event.getSource().getHolder();
        if (sourceHolder == null) return;

        // Only care if the source is a protected vehicle type.
        if (!(sourceHolder instanceof Entity entity)) return;
        if (!isProtectedVehicleType(entity)) return;

        if (entity.getWorld() != null && BlockProt.getDefaultConfig().isWorldExcluded(entity.getWorld())) return;

        EntityNBTHandler handler;
        try {
            handler = new EntityNBTHandler(entity);
        } catch (Exception ignored) {
            return;
        }

        if (!handler.isProtected()) return;

        // Block all hopper/minecart pipeline extraction from protected vehicles.
        event.setCancelled(true);
        BlockProtLogger.log("entity-protection", "BLOCKED hopper pipeline extraction from "
            + entity.getType().name() + " entity=" + entity.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleDamage(@NotNull VehicleDamageEvent event) {
        Entity vehicle = event.getVehicle();
        if (!isProtectedVehicleType(vehicle)) return;

        EntityNBTHandler handler = new EntityNBTHandler(vehicle);
        if (!handler.isProtected()) return;

        Entity attacker = event.getAttacker();
        if (!canModifyVehicle(attacker, vehicle, handler)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleDestroy(@NotNull VehicleDestroyEvent event) {
        Entity vehicle = event.getVehicle();
        if (!isProtectedVehicleType(vehicle)) return;

        EntityNBTHandler handler = new EntityNBTHandler(vehicle);
        if (!handler.isProtected()) return;

        Entity attacker = event.getAttacker();
        if (!canModifyVehicle(attacker, vehicle, handler)) {
            event.setCancelled(true);
        }
    }

    private boolean isProtectedVehicleType(@NotNull Entity entity) {
        boolean isChestBoat    = CHEST_BOAT_CLASS != null && CHEST_BOAT_CLASS.isInstance(entity);
        boolean isStorageCart  = entity instanceof StorageMinecart;
        boolean isHopperCart   = entity instanceof HopperMinecart;
        return isChestBoat || isStorageCart || isHopperCart;
    }

    private boolean canModifyVehicle(@Nullable Entity attacker, @NotNull Entity vehicle, @NotNull EntityNBTHandler handler) {
        if (attacker == null) {
            BlockProtLogger.log("entity-protection", "BLOCKED non-entity modification: "
                + vehicle.getType().name() + " entity=" + vehicle.getUniqueId());
            return false;
        }

        Player player = resolvePlayer(attacker);
        if (player == null) {
            BlockProtLogger.log("entity-protection", "BLOCKED non-player modification by " + attacker.getType().name()
                + " on " + vehicle.getType().name() + " entity=" + vehicle.getUniqueId());
            return false;
        }

        String playerUuid = player.getUniqueId().toString();
        if (handler.isOwner(playerUuid) || player.hasPermission(Permissions.USER_ADMIN.key())) {
            BlockProtLogger.log("entity-protection", "ALLOWED modification by owner/admin " + player.getName()
                + " on " + vehicle.getType().name() + " entity=" + vehicle.getUniqueId());
            return true;
        }

        sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
        AuditLogger audit = BlockProt.getAuditLogger();
        if (audit != null) {
            audit.log(player.getUniqueId(), player.getName(), vehicle.getLocation(), AuditLogger.Action.ACCESS_DENIED);
        }
        BlockProtLogger.log("entity-protection", "ACCESS_DENIED break/damage by " + player.getName()
            + " on " + vehicle.getType().name() + " entity=" + vehicle.getUniqueId());
        return false;
    }

    @Nullable
    private Player resolvePlayer(@NotNull Entity attacker) {
        if (attacker instanceof Player p) return p;
        if (attacker instanceof org.bukkit.entity.Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player p) return p;
        }
        return null;
    }

    private void sendActionBar(@NotNull Player player, @NotNull String text) {
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(text));
    }
}
