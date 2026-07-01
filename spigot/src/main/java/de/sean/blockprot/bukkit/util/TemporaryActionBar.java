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

package de.sean.blockprot.bukkit.util;

import com.tcoded.folialib.FoliaLib;
import de.sean.blockprot.bukkit.BlockProt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends an action-bar message that persists for a configurable number of ticks,
 * then restores the vanilla empty action bar automatically.
 *
 * <p>Implementation note: the action bar is resent every 40 ticks (2 s) so the
 * client does not fade it out before the configured duration elapses. A scheduled
 * task per player handles this; cancelling the task triggers the restore.
 */
public final class TemporaryActionBar {

    /** Active task handles keyed by player UUID. */
    private static final Map<UUID, Object> activeTasks = new ConcurrentHashMap<>();

    private TemporaryActionBar() {}

    /**
     * Shows {@code message} in the player's action bar for {@code durationTicks} ticks.
     * Replaces any previously running temporary action bar for that player.
     *
     * @param player       recipient
     * @param message      legacy-colour-coded string (e.g. {@code "§aBlock protected!"})
     * @param durationTicks how long to hold the message visible (20 ticks = 1 second)
     */
    public static void show(@NotNull Player player, @NotNull String message, long durationTicks) {
        UUID uuid = player.getUniqueId();
        cancel(uuid);

        Component component = LegacyComponentSerializer.legacySection().deserialize(message);
        FoliaLib folia = BlockProt.getFoliaLib();

        // Resend the message every 40 ticks so the client does not fade it out.
        final long resendInterval = 40L;
        final long[] remaining = {durationTicks};

        // Schedule an immediately-run repeating task via FoliaLib.
        // FoliaLib does not expose a direct "repeating async entity task", so we
        // chain single delayed tasks to stay Folia-safe.
        scheduleResend(uuid, player, component, remaining, resendInterval, folia);
    }

    /**
     * Cancels any active temporary action bar for the given player and clears the bar.
     *
     * @param uuid player UUID
     */
    public static void cancel(@NotNull UUID uuid) {
        activeTasks.remove(uuid);
        // Clear the action bar next tick via a fire-and-forget task.
        Player online = org.bukkit.Bukkit.getPlayer(uuid);
        if (online != null) {
            online.sendActionBar(Component.empty());
        }
    }

    private static void scheduleResend(@NotNull UUID uuid, @NotNull Player player,
                                       @NotNull Component component, long[] remaining,
                                       long resendInterval, @NotNull FoliaLib folia) {
        if (!activeTasks.containsKey(uuid)) return; // was cancelled
        if (!player.isOnline()) { activeTasks.remove(uuid); return; }

        player.sendActionBar(component);
        remaining[0] -= resendInterval;

        if (remaining[0] > 0) {
            // Mark as active (value is just a sentinel).
            activeTasks.put(uuid, Boolean.TRUE);
            folia.getScheduler().runLater(task -> scheduleResend(uuid, player, component, remaining, resendInterval, folia),
                resendInterval);
        } else {
            activeTasks.remove(uuid);
            player.sendActionBar(Component.empty());
        }
    }

    private static void register(@NotNull UUID uuid) {
        activeTasks.put(uuid, Boolean.TRUE);
    }
}