package de.sean.blockprot.bukkit.tasks;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.listeners.HopperEventListener;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class WorldExpiryTask extends BukkitRunnable {

    private static final int BATCH_PER_TICK = 20;

    @Override
    public void run() {
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        if (!cfg.isWorldExpiryEnabled()) return;

        Map<String, String> durations = cfg.getWorldExpiryDurations();
        if (durations.isEmpty()) return;

        for (Map.Entry<String, String> entry : durations.entrySet()) {
            String worldName = entry.getKey();
            String durationStr = entry.getValue();
            if ("0".equals(durationStr) || "-1".equals(durationStr)) continue;

            long durationMs = parseDuration(durationStr);
            if (durationMs <= 0) continue;

            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            scanWorld(world, durationMs);
        }
    }

    private void scanWorld(@NotNull World world, long durationMs) {
        long cutoff = System.currentTimeMillis() - durationMs;
        List<Location> expired = new ArrayList<>();

        Chunk[] chunks = world.getLoadedChunks();
        Set<Material> nonTileTypes = new HashSet<>();
        for (BlockFamilyParser.Family f : BlockFamilyParser.Family.values()) {
            if (f == BlockFamilyParser.Family.ENTITIES) continue;
            nonTileTypes.addAll(BlockFamilyParser.getFamilyMembers(f));
        }

        for (Chunk chunk : chunks) {
            for (BlockState state : chunk.getTileEntities()) {
                Location loc = state.getLocation();
                BlockNBTHandler handler = new BlockNBTHandler(loc.getBlock());
                if (handler.isProtected() && handler.getLockedAt() > 0 && handler.getLockedAt() < cutoff) {
                    expired.add(loc.clone());
                }
            }
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (block.getState() instanceof TileState) continue;
                        if (nonTileTypes.contains(block.getType())) {
                            BlockNBTHandler handler = new BlockNBTHandler(block);
                            if (handler.isProtected() && handler.getLockedAt() > 0 && handler.getLockedAt() < cutoff) {
                                expired.add(block.getLocation().clone());
                            }
                        }
                    }
                }
            }
        }

        if (expired.isEmpty()) return;

        for (Location loc : expired) {
            if (loc.getWorld() == null) continue;
            Block block = loc.getBlock();
            try {
                BlockNBTHandler handler = new BlockNBTHandler(block);
                if (!handler.isProtected()) continue;
                String ownerUuid = handler.getOwner();
                handler.clear();
                handler.applyToOtherContainer();
                HopperEventListener.invalidate(block);
                ProtectedBlockCache.unmark(block);
                if (ownerUuid != null && !ownerUuid.isEmpty()) {
                    try {
                        StatHandler.removeContainerByUuid(UUID.fromString(ownerUuid), loc);
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (RuntimeException ignored) {}
        }

        BlockProtLogger.log("world-expiry",
            "Expired " + expired.size() + " protections in world " + world.getName());
    }

    private static long parseDuration(@NotNull String input) {
        try {
            String s = input.trim().toLowerCase();
            if (s.endsWith("d")) return Long.parseLong(s.substring(0, s.length() - 1)) * 86400000L;
            if (s.endsWith("h")) return Long.parseLong(s.substring(0, s.length() - 1)) * 3600000L;
            if (s.endsWith("m")) return Long.parseLong(s.substring(0, s.length() - 1)) * 60000L;
            if (s.endsWith("s")) return Long.parseLong(s.substring(0, s.length() - 1)) * 1000L;
        } catch (NumberFormatException ignored) {}
        return -1;
    }
}
