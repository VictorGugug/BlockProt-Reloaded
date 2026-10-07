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

package de.sean.blockprot.bukkit.tasks;

import com.tcoded.folialib.impl.PlatformScheduler;
import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.listeners.HopperEventListener;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.FriendHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.storage.HybridDatabase;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

/**
 * Removes or restores every protection in a world, one region-owned chunk task at a time.
 */
public final class WorldProtectionEraser {

    private static final int CHUNKS_PER_TICK = 4;

    public static final UUID CONSOLE_KEY = new UUID(0L, 0L);

    public record Snapshot(@NotNull Location location, @NotNull String ownerUuid, @NotNull List<String> friendUuids) {}

    public record UndoBatch(long timestamp, @NotNull String worldName, @NotNull List<Snapshot> snapshots) {}

    private record ChunkJob(int x, int z, @Nullable List<Location> locations) {}

    private static final Map<UUID, List<UndoBatch>> UNDO_HISTORY = new ConcurrentHashMap<>();

    private WorldProtectionEraser() {}

    @NotNull
    public static UUID keyOf(@NotNull CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId() : CONSOLE_KEY;
    }

    @NotNull
    public static List<UndoBatch> getUndoHistory(@NotNull UUID key) {
        List<UndoBatch> batches = UNDO_HISTORY.get(key);
        return batches == null ? List.of() : List.copyOf(batches);
    }

    @Nullable
    public static UndoBatch latestUndo(@NotNull UUID key) {
        List<UndoBatch> batches = getUndoHistory(key);
        return batches.isEmpty() ? null : batches.get(0);
    }

    public static void erase(@NotNull World world, @NotNull UUID undoKey, @NotNull IntConsumer onDone) {
        PlatformScheduler scheduler = BlockProt.getFoliaLib().getScheduler();
        HybridDatabase db = BlockProt.getHybridDatabase();
        if (db != null && db.isEnabled()) {
            scheduler.runAsync(task -> {
                Map<Long, List<Location>> byChunk = new LinkedHashMap<>();
                for (Location loc : db.getBlockIndexByWorld(world.getName())) {
                    byChunk.computeIfAbsent(chunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4), k -> new ArrayList<>()).add(loc);
                }
                List<ChunkJob> jobs = new ArrayList<>();
                byChunk.forEach((key, locs) -> jobs.add(new ChunkJob((int) (key >> 32), (int) (long) key, locs)));
                runErase(world, jobs, undoKey, onDone);
            });
        } else {
            scheduler.runNextTick(task -> {
                List<ChunkJob> jobs = new ArrayList<>();
                for (Chunk chunk : world.getLoadedChunks()) {
                    jobs.add(new ChunkJob(chunk.getX(), chunk.getZ(), null));
                }
                runErase(world, jobs, undoKey, onDone);
            });
        }
    }

    public static void undo(@NotNull UUID undoKey, @NotNull UndoBatch batch, @NotNull IntConsumer onDone) {
        List<UndoBatch> history = UNDO_HISTORY.get(undoKey);
        if (history != null) history.remove(batch);

        Map<Long, List<Snapshot>> byChunk = new LinkedHashMap<>();
        for (Snapshot snap : batch.snapshots()) {
            Location loc = snap.location();
            byChunk.computeIfAbsent(chunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4), k -> new ArrayList<>()).add(snap);
        }

        PlatformScheduler scheduler = BlockProt.getFoliaLib().getScheduler();
        AtomicInteger restored = new AtomicInteger();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        int index = 0;
        for (List<Snapshot> snaps : byChunk.values()) {
            Location anchor = snaps.get(0).location();
            if (anchor.getWorld() == null) continue;
            futures.add(scheduler.runAtLocationLater(anchor, task -> {
                for (Snapshot snap : snaps) {
                    if (restore(snap)) restored.incrementAndGet();
                }
            }, 1L + index++ / CHUNKS_PER_TICK));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .whenComplete((v, e) -> scheduler.runNextTick(task -> onDone.accept(restored.get())));
    }

    private static void runErase(@NotNull World world, @NotNull List<ChunkJob> jobs,
                                 @NotNull UUID undoKey, @NotNull IntConsumer onDone) {
        PlatformScheduler scheduler = BlockProt.getFoliaLib().getScheduler();
        Set<Material> blockTypes = lockableBlockTypes();
        List<Snapshot> snapshots = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < jobs.size(); i++) {
            ChunkJob job = jobs.get(i);
            Location anchor = new Location(world, job.x() << 4, world.getMinHeight(), job.z() << 4);
            futures.add(scheduler.runAtLocationLater(anchor, task -> {
                if (job.locations() != null) {
                    for (Location loc : job.locations()) clear(loc.getBlock(), snapshots);
                } else {
                    scanChunk(world.getChunkAt(job.x(), job.z()), blockTypes, snapshots);
                }
            }, 1L + i / CHUNKS_PER_TICK));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .whenComplete((v, e) -> scheduler.runNextTick(task -> {
                List<Snapshot> result;
                synchronized (snapshots) {
                    result = List.copyOf(snapshots);
                }
                if (!result.isEmpty()) {
                    UNDO_HISTORY.computeIfAbsent(undoKey, k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(0, new UndoBatch(System.currentTimeMillis(), world.getName(), result));
                }
                onDone.accept(result.size());
            }));
    }

    private static void scanChunk(@NotNull Chunk chunk, @NotNull Set<Material> blockTypes,
                                  @NotNull List<Snapshot> snapshots) {
        for (BlockState state : chunk.getTileEntities()) {
            clear(state.getBlock(), snapshots);
        }
        World world = chunk.getWorld();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (blockTypes.contains(block.getType())) clear(block, snapshots);
                }
            }
        }
    }

    @NotNull
    private static Set<Material> lockableBlockTypes() {
        Set<Material> types = EnumSet.noneOf(Material.class);
        for (BlockFamilyParser.Family family : BlockFamilyParser.Family.values()) {
            if (family == BlockFamilyParser.Family.ENTITIES || family == BlockFamilyParser.Family.TILE_ENTITIES) continue;
            types.addAll(BlockFamilyParser.getFamilyMembers(family));
        }
        return types;
    }

    private static void clear(@NotNull Block block, @NotNull List<Snapshot> snapshots) {
        try {
            BlockNBTHandler handler = new BlockNBTHandler(block);
            if (!handler.isProtected()) return;
            String owner = handler.getOwner();
            List<String> friends = handler.getFriends().stream().map(FriendHandler::getName).toList();
            snapshots.add(new Snapshot(block.getLocation(), owner, friends));
            handler.clear();
            try { handler.applyToOtherContainer(); } catch (RuntimeException ignored) {}
            HopperEventListener.invalidate(block);
            ProtectedBlockCache.unmark(block);
            if (!owner.isEmpty()) {
                try { StatHandler.removeContainerByUuid(UUID.fromString(owner), block.getLocation()); }
                catch (IllegalArgumentException ignored) {}
            }
        } catch (RuntimeException e) {
            BlockProtLogger.log("world-eraser", "Failed to erase block at " + block.getLocation() + ": " + e.getMessage());
        }
    }

    private static boolean restore(@NotNull Snapshot snap) {
        if (snap.location().getWorld() == null) return false;
        Block block = snap.location().getBlock();
        try {
            BlockNBTHandler handler = new BlockNBTHandler(block);
            handler.setOwner(snap.ownerUuid());
            for (String friend : snap.friendUuids()) handler.addFriend(friend);
            handler.applyToOtherContainer();
            ProtectedBlockCache.mark(block);
            try { StatHandler.addBlockByUuid(UUID.fromString(snap.ownerUuid()), snap.location().clone()); }
            catch (IllegalArgumentException ignored) {}
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
