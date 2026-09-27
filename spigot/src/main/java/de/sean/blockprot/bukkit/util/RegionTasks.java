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

import de.sean.blockprot.bukkit.BlockProt;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Routes work to the thread that owns a sender or a location, on Paper and Folia alike.
 */
public final class RegionTasks {

    private RegionTasks() {}

    public static void runFor(@NotNull CommandSender sender, @NotNull Runnable action) {
        if (sender instanceof Player player) {
            BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> action.run());
        } else {
            BlockProt.getFoliaLib().getScheduler().runNextTick(task -> action.run());
        }
    }

    /**
     * Applies {@code mapper} to every location on its owning region thread and completes with the
     * non-null results in input order. A mapper that throws contributes no result.
     */
    @NotNull
    public static <T> CompletableFuture<List<T>> mapLocations(@NotNull List<Location> locations,
                                                             @NotNull Function<Location, T> mapper) {
        List<CompletableFuture<T>> futures = new ArrayList<>(locations.size());
        for (Location location : locations) {
            CompletableFuture<T> result = new CompletableFuture<>();
            futures.add(result);
            if (location.getWorld() == null) {
                result.complete(null);
                continue;
            }
            BlockProt.getFoliaLib().getScheduler().runAtLocation(location, task -> {
                try {
                    result.complete(mapper.apply(location));
                } catch (RuntimeException e) {
                    result.complete(null);
                }
            });
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(ignored -> futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).toList());
    }
}
