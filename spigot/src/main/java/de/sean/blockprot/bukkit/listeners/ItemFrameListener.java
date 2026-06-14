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
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Handles protection for Item Frames (normal and glowing).
 *
 * <p>Protection is opt-in per frame: sneaking and right-clicking an empty hand
 * while looking at a frame opens the BlockProt menu (owner assignment / friend list).
 * Once protected:</p>
 * <ul>
 *   <li>Non-owners/friends cannot rotate or swap the item inside the frame.</li>
 *   <li>Non-owners/friends cannot break the frame.</li>
 *   <li>ImageFrame plugin frames with a creator tag are also covered.</li>
 * </ul>
 *
 * <p>Frames use {@link EntityNBTHandler} which stores protection data in the
 * entity's persistent data container. Protection survives chunk reloads and
 * server restarts.</p>
 */
public final class ItemFrameListener implements Listener {

    /**
     * Intercepts right-click interactions on item frames.
     * <ul>
     *   <li>Sneaking + empty hand → open protection menu (owner or anyone for unprotected frames).</li>
     *   <li>Not sneaking, frame is protected → deny interaction to non-owners/friends.</li>
     * </ul>
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrameInteract(@NotNull PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame frame)) return;
        if (!isFrameProtectionEnabled(frame)) return;

        Player player = event.getPlayer();
        if (BlockProt.getDefaultConfig().isWorldExcluded(player.getWorld())) return;

        EntityNBTHandler handler = new EntityNBTHandler(frame);

        if (player.isSneaking()) {
            // Sneaking + empty hand → open protection menu.
            var mainHand = player.getInventory().getItemInMainHand();
            if (!mainHand.getType().isAir()) return;

            event.setCancelled(true);

            if (!player.hasPermission(Permissions.USER.key())) {
                sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                return;
            }

            // Build or open the entity protection menu.
            InventoryState state = InventoryState.getOrCreate(player.getUniqueId());
            state.entityUUID = frame.getUniqueId();

            var inv = new BlockLockInventory().fillForEntity(player, handler);
            if (inv == null) {
                sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                return;
            }
            player.openInventory(inv);
        } else {
            // Normal right-click — block if protected and player is not owner/friend/admin.
            if (!handler.isProtected()) return;
            if (handler.canAccess(player.getUniqueId().toString())
                    || player.hasPermission(Permissions.USER_ADMIN.key())) return;
            event.setCancelled(true);
            sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            AuditLogger audit = BlockProt.getAuditLogger();
            if (audit != null) {
                audit.log(player.getUniqueId(), player.getName(), frame.getLocation(), AuditLogger.Action.ACCESS_DENIED);
            }
            BlockProtLogger.log("entity-protection", "ACCESS_DENIED frame interact: "
                + frame.getType().name() + " entity=" + frame.getUniqueId() + " player=" + player.getName());
        }
    }

    /**
     * Prevents non-owners from breaking a protected item frame.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrameBreakByPlayer(@NotNull HangingBreakByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        if (!isFrameProtectionEnabled(frame)) return;
        if (!(event.getRemover() instanceof Player player)) return;

        if (BlockProt.getDefaultConfig().isWorldExcluded(player.getWorld())) return;

        EntityNBTHandler handler = new EntityNBTHandler(frame);
        if (!handler.isProtected()) return;

        if (!handler.canAccess(player.getUniqueId().toString())
                && !player.hasPermission(Permissions.USER_ADMIN.key())) {
            event.setCancelled(true);
            sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            BlockProtLogger.log("entity-protection", "ACCESS_DENIED frame break: "
                + frame.getType().name() + " entity=" + frame.getUniqueId() + " player=" + player.getName());
        }
    }

    /**
     * Prevents non-owners from damaging (shooting) a protected item frame.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrameDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        if (!isFrameProtectionEnabled(frame)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        if (BlockProt.getDefaultConfig().isWorldExcluded(player.getWorld())) return;

        EntityNBTHandler handler = new EntityNBTHandler(frame);
        if (!handler.isProtected()) return;

        if (!handler.canAccess(player.getUniqueId().toString())
                && !player.hasPermission(Permissions.USER_ADMIN.key())) {
            event.setCancelled(true);
            sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            BlockProtLogger.log("entity-protection", "ACCESS_DENIED frame damage: "
                + frame.getType().name() + " entity=" + frame.getUniqueId() + " player=" + player.getName());
        }
    }

    private boolean isFrameProtectionEnabled(@NotNull ItemFrame frame) {
        Material mat = frame instanceof GlowItemFrame ? Material.GLOW_ITEM_FRAME : Material.ITEM_FRAME;
        return BlockProt.getDefaultConfig().isLockableEntity(mat, frame.getWorld());
    }

    private void sendActionBar(@NotNull Player player, @NotNull String text) {
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(text));
    }
}
