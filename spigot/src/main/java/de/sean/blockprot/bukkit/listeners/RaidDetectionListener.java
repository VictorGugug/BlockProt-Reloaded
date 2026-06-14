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
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Detects possible raid attempts by monitoring explosions near protected blocks.
 *
 * <p>When an explosion affects any lockable block (protected or not), the event
 * is classified as a possible raid attempt. A WARNING entry is written to the
 * session log, and:
 * <ul>
 *   <li>If the block owner is online: they receive an in-game action-bar alert
 *       immediately and a chat notification with coordinates and optionally a
 *       teleport link if they have {@code blockprot.blocks.tp}.</li>
 *   <li>If the block owner is offline: the alert is queued and delivered as a
 *       chat message the next time they join. The TP link is included only if
 *       the player has {@code blockprot.blocks.tp} at join time.</li>
 * </ul>
 *
 * <p>This listener does NOT cancel or modify the explosion — that is handled
 * by {@link ExplodeEventListener}. This listener only performs detection and
 * notification.
 *
 * <p>The source entity (the entity that caused the explosion) is resolved from
 * the event. For {@link BlockExplodeEvent} the source is recorded as {@code null}
 * (no player actor). For {@link EntityExplodeEvent} the source entity is included
 * in the log when it is a {@link Player}; otherwise only the entity type is shown.
 */
public final class RaidDetectionListener implements Listener {

    /**
     * Pending raid alerts queued for offline players.
     * Key: owner UUID. Value: list of alert strings to send on next join.
     * Persists across reloads in memory only — not written to disk.
     */
    private static final Map<UUID, java.util.List<String>> pendingAlerts = new HashMap<>();

    /** Returns and clears all pending alerts for the given player UUID. */
    @Nullable
    public static java.util.List<String> popPendingAlerts(@NotNull UUID uuid) {
        return pendingAlerts.remove(uuid);
    }

    @EventHandler(ignoreCancelled = false)
    public void onBlockExplode(@NotNull BlockExplodeEvent event) {
        if (BlockProt.getDefaultConfig().isWorldExcluded(event.getBlock().getWorld())) return;
        checkBlocks(event.blockList(), null, event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = false)
    public void onEntityExplode(@NotNull EntityExplodeEvent event) {
        if (BlockProt.getDefaultConfig().isWorldExcluded(event.getEntity().getWorld())) return;
        checkBlocks(event.blockList(), event.getEntity(), event.getLocation());
    }

    private void checkBlocks(@NotNull List<Block> blocks, @Nullable Entity source, @NotNull Location origin) {
        if (!BlockProt.getInstance().getConfig().getBoolean("raid_detection.enabled", true)) return;
        for (Block block : blocks) {
            if (!BlockProt.getDefaultConfig().isLockable(block.getType())) continue;

            BlockNBTHandler handler;
            try {
                handler = new BlockNBTHandler(block);
            } catch (RuntimeException ignored) {
                continue;
            }

            // Detect any lockable block near an explosion — protected or not.
            Location loc  = block.getLocation();
            String world  = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
            int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
            String material = block.getType().name();

            // Resolve the actor string for the log.
            String actorDisplay = resolveActor(source);

            String logLine = String.format(
                "WARN: [raid-detection] Possible raid — %s at %s [%d, %d, %d] near explosion. Actor: %s",
                material, world, x, y, z, actorDisplay);
            BlockProtLogger.log(logLine);

            if (!handler.isProtected()) continue;

            String ownerUuid = handler.getOwner();
            if (ownerUuid == null || ownerUuid.isBlank()) continue;

            UUID ownerId;
            try {
                ownerId = UUID.fromString(ownerUuid);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            // Build the notification strings using translation keys.
            String alertMsg = Translator.get(TranslationKey.MESSAGES__RAID_ALERT)
                .replace("{block}", material)
                .replace("{world}", world)
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y))
                .replace("{z}", String.valueOf(z))
                .replace("{actor}", actorDisplay);

            String coordsMsg = Translator.get(TranslationKey.MESSAGES__RAID_COORDS)
                .replace("{block}", material)
                .replace("{world}", world)
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y))
                .replace("{z}", String.valueOf(z))
                .replace("{actor}", actorDisplay);

            org.bukkit.OfflinePlayer offlineOwner = Bukkit.getOfflinePlayer(ownerId);
            Player onlineOwner = offlineOwner.isOnline() ? offlineOwner.getPlayer() : null;

            if (onlineOwner != null) {
                sendAlertToOnline(onlineOwner, alertMsg, coordsMsg, loc);
            } else {
                // Queue for delivery on join.
                pendingAlerts.computeIfAbsent(ownerId, k -> new java.util.ArrayList<>())
                    .add(coordsMsg);
            }
        }
    }

    /**
     * Sends an immediate action-bar alert and a chat message to an online owner.
     * Includes a TP click link if the player has {@code blockprot.blocks.tp}.
     */
    private void sendAlertToOnline(@NotNull Player player, @NotNull String alertMsg,
                                   @NotNull String coordsMsg, @NotNull Location loc) {
        // Action bar — short immediate notification.
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(alertMsg));

        // Chat — full coordinates + optional teleport link.
        boolean hasTp = player.hasPermission(Permissions.BLOCKS_TP.key());
        Component chat = buildChatComponent(coordsMsg, hasTp, loc);
        player.sendMessage(chat);
    }

    /**
     * Builds the chat component for the raid alert.
     * When {@code hasTp} is true, a clickable TP link is appended.
     * When false, a plain coordinates-only message is sent.
     */
    @NotNull
    private Component buildChatComponent(@NotNull String coordsMsg, boolean hasTp, @NotNull Location loc) {
        Component base = LegacyComponentSerializer.legacySection().deserialize(coordsMsg);
        if (!hasTp) return base;

        String world = loc.getWorld() != null ? loc.getWorld().getName() : "world";
        String tpLabel = Translator.get(TranslationKey.MESSAGES__RAID_TP_LABEL);
        Component tpLink = LegacyComponentSerializer.legacySection().deserialize(tpLabel)
            .clickEvent(ClickEvent.runCommand(
                String.format("/execute in %s run tp @s %d %d %d",
                    world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())));
        return base.append(Component.space()).append(tpLink);
    }

    /**
     * Returns a human-readable string identifying the explosion source.
     * Returns the player name when the source is a player, the entity type otherwise,
     * and "environment" when no source entity is available.
     */
    @NotNull
    private String resolveActor(@Nullable Entity source) {
        if (source == null) return "environment";
        if (source instanceof Player p) return p.getName();
        return source.getType().name();
    }
}
