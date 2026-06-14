package de.sean.blockprot.bukkit.config;

import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Granular per-world configuration read from worlds.yml.
 *
 * Resolution hierarchy for a given world when worlds_config_enabled=true:
 *   1. If the world appears in worlds.yml with enabled: false, no protection applies.
 *   2. If the world appears in worlds.yml with enabled: true, its own lists are used exclusively.
 *   3. If the world does not appear in worlds.yml, the global config.yml lists are used.
 *
 * The scanAndPopulate() method scans loaded worlds at startup and adds missing entries
 * with enabled: true and lists inherited from the global config.yml. Existing entries are never overwritten.
 */
public final class WorldsConfig {

    private record WorldEntry(boolean enabled, Set<Material> tileEntities,
                              Set<Material> blocks, Set<Material> shulkerBoxes, Set<Material> doors,
                              Set<Material> entities, boolean autoDropEnabled) {}

    private final Map<String, WorldEntry> worlds = new HashMap<>();

    // -------------------------------------------------------------------------
    // Build from YamlConfiguration (already loaded from disk)
    // -------------------------------------------------------------------------

    public WorldsConfig(@NotNull YamlConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("worlds");
        if (section == null) return;

        for (String name : section.getKeys(false)) {
            ConfigurationSection ws = section.getConfigurationSection(name);
            if (ws == null) continue;

            boolean enabled = ws.getBoolean("enabled", false);
            WorldEntry entry = new WorldEntry(
                enabled,
                loadMaterials(ws, "lockable_tile_entities", BlockFamilyParser.Family.TILE_ENTITIES),
                loadMaterials(ws, "lockable_blocks", BlockFamilyParser.Family.BLOCKS),
                loadMaterials(ws, "lockable_shulker_boxes", BlockFamilyParser.Family.SHULKER_BOXES),
                loadMaterials(ws, "lockable_doors", BlockFamilyParser.Family.DOORS),
                loadMaterials(ws, "lockable_entities", BlockFamilyParser.Family.ENTITIES),
                ws.getBoolean("auto_drop_to_inventory_enabled", true)
            );
            worlds.put(name.toLowerCase(), entry);

            // On 26.x, Minecraft uses NamespacedKey values for world storage.
            // Register an alias so lookups succeed whether the admin wrote "world"
            // or the server returns "minecraft:world" from world.getKey().asString().
            // e.g. "world" also registers "minecraft:world"; "world_nether" -> "minecraft:the_nether" is NOT added
            // automatically here — only exact name-based aliases are safe without a live World instance.
            // The resolveEntry() method handles the fallback at query time.
            String namespacedAlias = "minecraft:" + name.toLowerCase();
            worlds.putIfAbsent(namespacedAlias, entry);
        }
    }

    // -------------------------------------------------------------------------
    // Scan and auto-populate (non-destructive)
    // -------------------------------------------------------------------------

    /**
     * Keys that every world entry must have, in canonical order.
     * Used both when creating new entries and when patching existing ones
     * that are missing keys added by newer plugin versions.
     */
    private static final List<String> REQUIRED_WORLD_KEYS = List.of(
        "enabled",
        "auto_drop_to_inventory_enabled",
        "lockable_tile_entities",
        "lockable_shulker_boxes",
        "lockable_blocks",
        "lockable_doors",
        "lockable_entities"
    );

    /**
     * Scans worlds loaded on the server and either creates missing world entries or
     * patches existing entries that are missing keys (e.g. {@code lockable_entities}
     * introduced in a newer plugin version).
     *
     * <ul>
     *   <li>New world  — full entry written with all required keys inherited from blocks.yml.</li>
     *   <li>Existing world missing keys — only the absent keys are appended; all present
     *       values are left exactly as the admin configured them.</li>
     *   <li>Existing world with all keys — skipped entirely (no write).</li>
     * </ul>
     *
     * @param file         The worlds.yml file on disk.
     * @param globalConfig The global config.yml, used to inherit default block lists.
     * @param logger       Plugin logger for console messages.
     * @return             The updated configuration (so BlockProt can reload the object).
     */
    @NotNull
    public static YamlConfiguration scanAndPopulate(
            @NotNull File file,
            @NotNull FileConfiguration globalConfig,
            @NotNull Logger logger) {

        YamlConfiguration disk;
        if (file.exists()) {
            disk = new YamlConfiguration();
            try {
                disk.load(file);
            } catch (InvalidConfigurationException | java.io.IOException e) {
                logger.warning(Translator.get(TranslationKey.CONSOLE__WORLDS_YML_SYNTAX_ERROR)
                    .replace("{error}", e.getMessage()));
                BlockProtLogger.log("worlds-repair", "Syntax error in worlds.yml: " + e.getMessage());
                backupBrokenFile(file, logger);
                try {
                    var stream = de.sean.blockprot.bukkit.BlockProt.getInstance().getResource("worlds.yml");
                    if (stream != null) {
                        disk = YamlConfiguration.loadConfiguration(new java.io.BufferedReader(
                            new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)));
                        disk.save(file);
                        logger.info(Translator.get(TranslationKey.CONSOLE__WORLDS_YML_RESTORED));
                    } else {
                        disk = new YamlConfiguration();
                    }
                } catch (Exception ex) {
                    logger.warning(Translator.get(TranslationKey.CONSOLE__WORLDS_YML_RESTORE_FAILED)
                        .replace("{error}", ex.getMessage()));
                    disk = new YamlConfiguration();
                }
            }
        } else {
            disk = new YamlConfiguration();
        }

        List<World> serverWorlds = Bukkit.getWorlds();
        int added   = 0;
        int patched = 0;

        BlockProtLogger.log("worlds-scan", Translator.get(TranslationKey.WORLDS__SCAN_START)
            .replace("{count}", String.valueOf(serverWorlds.size())));

        de.sean.blockprot.bukkit.config.DefaultConfig dc = null;
        try { dc = de.sean.blockprot.bukkit.BlockProt.getDefaultConfig(); } catch (AssertionError ignored) {}
        final de.sean.blockprot.bukkit.config.DefaultConfig defaultConfig = dc;

        for (World world : serverWorlds) {
            String key = "worlds." + world.getName();

            if (!disk.contains(key)) {
                // ── New world: write every required key ───────────────────────
                disk.set(key + ".enabled", true);
                disk.set(key + ".auto_drop_to_inventory_enabled", true);
                if (defaultConfig != null) {
                    for (String listKey : List.of("lockable_tile_entities", "lockable_shulker_boxes",
                                                  "lockable_blocks", "lockable_doors", "lockable_entities")) {
                        copyFromBlocksConfig(disk, key, listKey, defaultConfig, globalConfig);
                    }
                } else {
                    for (String listKey : List.of("lockable_tile_entities", "lockable_shulker_boxes",
                                                  "lockable_blocks", "lockable_doors", "lockable_entities")) {
                        copyList(disk, globalConfig, key, listKey);
                    }
                }
                BlockProtLogger.log("worlds-scan",
                    "  + '" + world.getName() + "' added (enabled: true, lists inherited from blocks.yml)");
                added++;
            } else {
                // ── Existing world: patch any missing keys only ───────────────
                ConfigurationSection ws = disk.getConfigurationSection(key);
                if (ws == null) continue;

                boolean patching = false;
                for (String required : REQUIRED_WORLD_KEYS) {
                    if (ws.contains(required)) continue; // already set — do not touch

                    patching = true;
                    if (required.equals("enabled")) {
                        disk.set(key + ".enabled", true);
                    } else if (required.equals("auto_drop_to_inventory_enabled")) {
                        disk.set(key + ".auto_drop_to_inventory_enabled", true);
                    } else {
                        // Block-list key
                        if (defaultConfig != null) {
                            copyFromBlocksConfig(disk, key, required, defaultConfig, globalConfig);
                        } else {
                            copyList(disk, globalConfig, key, required);
                        }
                    }
                    BlockProtLogger.log("worlds-scan",
                        "  ~ '" + world.getName() + "' patched missing key: " + required);
                }
                if (patching) patched++;
            }
        }

        boolean dirty = added > 0 || patched > 0;
        if (dirty) {
            try {
                disk.save(file);
            } catch (IOException e) {
                logger.warning(Translator.get(TranslationKey.CONSOLE__WORLDS_YML_SAVE_FAILED)
                    .replace("{error}", e.getMessage()));
            }
        }

        String summary = Translator.get(TranslationKey.WORLDS__SCAN_COMPLETE)
            .replace("{count}", String.valueOf(serverWorlds.size()))
            .replace("{added}", String.valueOf(added));
        if (patched > 0) summary += " (" + patched + " patched)";
        logger.info(summary);
        BlockProtLogger.log("worlds-scan", summary);

        return disk;
    }

    private static void copyList(@NotNull YamlConfiguration target, @NotNull FileConfiguration source,
                                  @NotNull String worldKey, @NotNull String listKey) {
        List<?> list = source.getList(listKey);
        if (list != null && !list.isEmpty()) {
            target.set(worldKey + "." + listKey, list);
        } else {
            target.set(worldKey + "." + listKey, Collections.emptyList());
        }
    }

    private static void copyFromBlocksConfig(@NotNull YamlConfiguration target, @NotNull String worldKey,
                                              @NotNull String listKey,
                                              @NotNull de.sean.blockprot.bukkit.config.DefaultConfig dc,
                                              @NotNull FileConfiguration fallback) {
        de.sean.blockprot.bukkit.BlockProt bp = de.sean.blockprot.bukkit.BlockProt.getInstance();
        File blocksFile = new File(bp.getDataFolder(),
            bp.getConfig().getString("blocks_file", "blocks.yml"));
        if (blocksFile.exists()) {
            YamlConfiguration bc = YamlConfiguration.loadConfiguration(blocksFile);
            Object raw = bc.get(listKey);
            if (raw != null) {
                target.set(worldKey + "." + listKey, raw instanceof List ? raw : List.of(raw.toString()));
                return;
            }
        }
        List<?> list = fallback.getList(listKey);
        target.set(worldKey + "." + listKey, list != null ? list : Collections.emptyList());
    }

    // -------------------------------------------------------------------------
    // Materials
    // -------------------------------------------------------------------------

    private Set<Material> loadMaterials(@NotNull ConfigurationSection section, @NotNull String key,
                                        @NotNull BlockFamilyParser.Family family) {
        Set<Material> result = new HashSet<>();
        Object raw = section.get(key);
        if (raw == null) return result;
        result.addAll(BlockFamilyParser.parse(raw, family));
        return result;
    }

    // -------------------------------------------------------------------------
    // Public queries
    // -------------------------------------------------------------------------

    /**
     * Resolves a consistent lookup key for a World that works on both classic (1.x)
     * and year-based (26.x) naming schemes.
     *
     * Strategy:
     *  1. Try world.getName() — always present, matches admin config most of the time.
     *  2. If not found, try world.getKey().value() — the NamespacedKey value, which is
     *     the stable identifier Paper 26.1 uses internally after the world-storage rework.
     *
     * This means admins can continue using plain world names ("world", "world_nether") in
     * worlds.yml, and we transparently fall back to the key-based lookup when needed.
     */
    @Nullable
    private WorldEntry resolveEntry(@NotNull World world) {
        WorldEntry e = worlds.get(world.getName().toLowerCase());
        if (e != null) return e;
        try {
            String keyValue = world.getKey().value().toLowerCase();
            e = worlds.get(keyValue);
            if (e != null) return e;
            String fullKey = world.getKey().asString().toLowerCase();
            return worlds.get(fullKey);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Returns true if the world has an entry in worlds.yml with enabled: true. */
    public boolean hasWorldConfig(@NotNull World world) {
        WorldEntry e = resolveEntry(world);
        return e != null && e.enabled();
    }

    /** Returns true if the world has an entry in worlds.yml with enabled: false (no protection). */
    public boolean isWorldDisabled(@NotNull World world) {
        WorldEntry e = resolveEntry(world);
        return e != null && !e.enabled();
    }

    public boolean isLockable(@NotNull World world, @NotNull Material type) {
        WorldEntry e = resolveEntry(world);
        if (e == null || !e.enabled()) return false;
        return e.tileEntities().contains(type) || e.blocks().contains(type)
            || e.shulkerBoxes().contains(type) || e.doors().contains(type)
            || e.entities().contains(type);
    }

    public boolean isLockableTileEntity(@NotNull World world, @NotNull Material type) {
        WorldEntry e = resolveEntry(world);
        return e != null && e.enabled() && e.tileEntities().contains(type);
    }

    public boolean isLockableBlock(@NotNull World world, @NotNull Material type) {
        WorldEntry e = resolveEntry(world);
        return e != null && e.enabled() && (e.blocks().contains(type) || e.doors().contains(type));
    }

    public boolean isLockableShulkerBox(@NotNull World world, @NotNull Material type) {
        WorldEntry e = resolveEntry(world);
        return e != null && e.enabled() && e.shulkerBoxes().contains(type);
    }

    public boolean isLockableDoor(@NotNull World world, @NotNull Material type) {
        WorldEntry e = resolveEntry(world);
        return e != null && e.enabled() && e.doors().contains(type);
    }

    public boolean isLockableEntity(@NotNull World world, @NotNull Material type) {
        WorldEntry e = resolveEntry(world);
        return e != null && e.enabled() && e.entities().contains(type);
    }

    /** Per-world auto-drop switch. Defaults to true when the world has no entry. */
    public boolean isAutoDropToInventoryEnabled(@NotNull World world) {
        WorldEntry e = resolveEntry(world);
        return e == null || e.autoDropEnabled();
    }

    // -------------------------------------------------------------------------
    // Internal repair helpers
    // -------------------------------------------------------------------------

    /**
     * Saves a copy of a broken config file next to itself with a timestamp suffix
     * so the admin can inspect or restore it.
     *
     * <p>Example output: {@code worlds.yml.2026-05-16_14-30-broken}</p>
     *
     * <p>If the copy fails (disk full, permissions, etc.) a warning is logged
     * and execution continues normally — the repair must not be blocked by the
     * backup step.</p>
     */
    private static void backupBrokenFile(@NotNull File file, @NotNull Logger logger) {
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new java.util.Date());
        File broken = new File(file.getParentFile(), file.getName() + "." + timestamp + "-broken");
        try {
            java.nio.file.Files.copy(file.toPath(), broken.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            logger.warning("[BlockProt] Broken worlds.yml saved to: " + broken.getName()
                + " — review it to recover your per-world settings.");
            BlockProtLogger.log("worlds-repair", "Broken file backed up to: " + broken.getAbsolutePath());
        } catch (java.io.IOException ex) {
            logger.warning("[BlockProt] Could not back up broken worlds.yml: " + ex.getMessage()
                + " — the file will be overwritten without a backup.");
            BlockProtLogger.log("worlds-repair", "Backup of broken file failed: " + ex.getMessage());
        }
    }
}
