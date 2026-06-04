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
