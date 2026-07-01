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

package de.sean.blockprot.bukkit.storage;

import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory set of all currently protected block locations.
 *
 * <p>Keyed by the same packed long used in {@link de.sean.blockprot.bukkit.listeners.HopperEventListener}.
 * This set is the source of truth for "is this location protected at all?" and is updated
 * on every lock/unlock operation. Its only purpose is to provide an O(1) early-exit in
 * high-frequency event handlers (e.g. {@code InventoryMoveItemEvent}) so that blocks that
 * are not protected never trigger an NBT read.</p>
 *
 * <p>Thread-safe: all mutations use {@link ConcurrentHashMap} and are called from the
 * server main thread, but reads may happen from any thread.</p>
 */
public final class ProtectedBlockCache {

    private static final ConcurrentHashMap<Long, Boolean> PROTECTED = new ConcurrentHashMap<>(256);

    private ProtectedBlockCache() {}

    public static void mark(@NotNull Block block) {
        PROTECTED.put(key(block), Boolean.TRUE);
    }

    public static void unmark(@NotNull Block block) {
        PROTECTED.remove(key(block));
    }

    public static boolean isProtected(@NotNull Block block) {
        return PROTECTED.containsKey(key(block));
    }

    public static void clear() {
        PROTECTED.clear();
    }

    public static int size() {
        return PROTECTED.size();
    }

    private static long key(@NotNull Block block) {
        UUID uid = block.getWorld().getUID();
        long xyz = ((long) block.getX() & 0x3FFFFFFL)
                 | (((long) block.getZ() & 0x3FFFFFFL) << 26)
                 | (((long) (block.getY() + 2048) & 0xFFFL) << 52);
        long k = xyz ^ (uid.getMostSignificantBits() * 0x9e3779b97f4a7c15L);
        k ^= uid.getLeastSignificantBits() * 0x6c62272e07bb0142L;
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= k >>> 33;
        return k;
    }
}