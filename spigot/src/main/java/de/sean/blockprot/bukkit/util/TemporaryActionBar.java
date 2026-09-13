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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sends a temporary action bar that stays visible across client fade-out intervals.
 */
public final class TemporaryActionBar {

    private static final Map<UUID, Long> activeTokens = new ConcurrentHashMap<>();
    private static final AtomicLong tokenGenerator = new AtomicLong(0);

    private TemporaryActionBar() {}

    public static void show(@NotNull Player player, @NotNull String message) {
        long ticks = BlockProt.getDefaultConfig() != null ? BlockProt.getDefaultConfig().getActionBarDurationTicks() : 120L;
        show(player, message, ticks);
    }

    public static void show(@NotNull Player player, @NotNull Component component) {
        long ticks = BlockProt.getDefaultConfig() != null ? BlockProt.getDefaultConfig().getActionBarDurationTicks() : 120L;
        show(player, component, ticks);
    }

    public static void show(@NotNull Player player, @NotNull String message, long durationTicks) {
        if (!ComponentMessages.isActionBarSupported()) {
            ComponentMessages.sendLegacyActionBar(player, message);
            return;
        }
        Component component = LegacyComponentSerializer.legacySection().deserialize(message);
        show(player, component, durationTicks);
    }

    public static void show(@NotNull Player player, @NotNull Component component, long durationTicks) {
        UUID uuid = player.getUniqueId();
        long token = tokenGenerator.incrementAndGet();
        activeTokens.put(uuid, token);

        ComponentMessages.sendActionBar(player, component);

        FoliaLib folia = BlockProt.getFoliaLib();
        if (durationTicks <= 0L) {
            return;
        }

        folia.getScheduler().runAtEntityLater(player, () -> {
            Long current = activeTokens.get(uuid);
            if (current != null && current == token) {
                activeTokens.remove(uuid);
                if (player.isOnline() && ComponentMessages.isActionBarSupported()) {
                    ComponentMessages.sendActionBar(player, Component.empty());
                }
            }
        }, durationTicks);

        final long resendInterval = 40L;
        if (durationTicks > resendInterval) {
            final long[] remaining = {durationTicks - resendInterval};
            folia.getScheduler().runAtEntityLater(player, () -> scheduleResend(uuid, player, component, remaining, resendInterval, token, folia), resendInterval);
        }
    }

    public static void cancel(@NotNull UUID uuid) {
        activeTokens.remove(uuid);
        Player online = Bukkit.getPlayer(uuid);
        if (online != null && ComponentMessages.isActionBarSupported()) {
            ComponentMessages.sendActionBar(online, Component.empty());
        }
    }

    private static void scheduleResend(@NotNull UUID uuid, @NotNull Player player,
                                       @NotNull Component component, long[] remaining,
                                       long resendInterval, long token, @NotNull FoliaLib folia) {
        Long currentToken = activeTokens.get(uuid);
        if (currentToken == null || currentToken != token) return;
        if (!player.isOnline()) {
            activeTokens.remove(uuid);
            return;
        }

        ComponentMessages.sendActionBar(player, component);
        remaining[0] -= resendInterval;

        if (remaining[0] > 0) {
            folia.getScheduler().runAtEntityLater(player, () -> scheduleResend(uuid, player, component, remaining, resendInterval, token, folia), resendInterval);
        }
    }
}