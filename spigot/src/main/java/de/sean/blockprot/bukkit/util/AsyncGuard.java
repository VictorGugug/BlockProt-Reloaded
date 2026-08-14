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
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Fails fast when NBT/tile-entity code runs outside its valid execution context.
 */
public final class AsyncGuard {

    private AsyncGuard() {}

    public static void assertSync(@NotNull String operation) {
        if (BlockProt.getFoliaLib().isFolia()) return;
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(operation + " must run on the primary thread");
        }
    }

    public static void assertBlockAccess(@NotNull Block block, @NotNull String operation) {
        if (BlockProt.getFoliaLib().isFolia()) {
            if (!BlockProt.getFoliaLib().getScheduler().isOwnedByCurrentRegion(block)) {
                throw new IllegalStateException(operation + " must run in the block's owning region");
            }
            return;
        }
        assertSync(operation);
    }
}
