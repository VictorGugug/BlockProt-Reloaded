/*
 * Copyright (C) 2021 - 2025 spnda
 * Modifications Copyright (C) 2025 Zaynr (Zar)
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

package de.sean.blockprot.bukkit.config;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The default config of the {@link BlockProt} plugin.
 */
public final class DefaultConfig extends BlockProtConfig {

    private final ArrayList<Material> lockableTileEntities = new ArrayList<>();
    private final ArrayList<Material> shulkerBoxes = new ArrayList<>();
    private final ArrayList<Material> lockableBlocks = new ArrayList<>();
    private final ArrayList<Material> lockableDoors = new ArrayList<>();

    private final ArrayList<InventoryType> lockableInventories = new ArrayList<>(Arrays.asList(
        InventoryType.CHEST, InventoryType.FURNACE, InventoryType.SMOKER, InventoryType.BLAST_FURNACE,
        InventoryType.HOPPER, InventoryType.BARREL, InventoryType.BREWING, InventoryType.SHULKER_BOX,
        InventoryType.ANVIL, InventoryType.DISPENSER, InventoryType.DROPPER, InventoryType.LECTERN,
        InventoryType.GRINDSTONE, InventoryType.STONECUTTER, InventoryType.LOOM,
        InventoryType.CARTOGRAPHY, InventoryType.SMITHING
    ));

    private final HashSet<Material> knownGoodTileEntities = new HashSet<>(Arrays.asList(
        Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST,
        Material.FURNACE, Material.SMOKER, Material.BLAST_FURNACE,
        Material.HOPPER, Material.BARREL, Material.BREWING_STAND, Material.DISPENSER, Material.DROPPER,
        Material.LECTERN, Material.BEEHIVE, Material.BEE_NEST,
        Material.JUKEBOX,
        Material.OAK_SIGN, Material.OAK_WALL_SIGN,
        Material.SPRUCE_SIGN, Material.SPRUCE_WALL_SIGN,
        Material.BIRCH_SIGN, Material.BIRCH_WALL_SIGN,
        Material.JUNGLE_SIGN, Material.JUNGLE_WALL_SIGN,
        Material.ACACIA_SIGN, Material.ACACIA_WALL_SIGN,
        Material.DARK_OAK_SIGN, Material.DARK_OAK_WALL_SIGN,
        Material.CRIMSON_SIGN, Material.CRIMSON_WALL_SIGN,
        Material.WARPED_SIGN, Material.WARPED_WALL_SIGN
    ));

    private final List<String> excludedWorlds;
    private final File dataFolder;
    private YamlConfiguration blocksConfig = null;
    private YamlConfiguration mysqlConfig  = null;
    private static final DateTimeFormatter BACKUP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public DefaultConfig(@NotNull final FileConfiguration config) {
        this(config, null);
    }

    public DefaultConfig(@NotNull final FileConfiguration config, final File dataFolder) {
        super(config);
        this.dataFolder = dataFolder;
        this.excludedWorlds = config.getStringList("excluded_worlds");
        this.removeBlockDefaults();

        if (dataFolder != null) {
            String blocksFilePath = config.getString("blocks_file", "blocks.yml");
            File blocksFile = new File(dataFolder, blocksFilePath);
            try {
                if (blocksFile.exists()) {
                    this.blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
                } else {
                    File parent = blocksFile.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    try {
                        // Only back up when the plugin version has changed (fresh install or upgrade)
                        String storedVersion = config.getString("last_known_version", "");
                        String currentVersion = de.sean.blockprot.bukkit.BlockProt.getPluginVersion();
                        boolean versionChanged = !storedVersion.equals(currentVersion) && !currentVersion.isEmpty();
                        if (versionChanged) {
                            File backupsDir = new File(dataFolder, "backups");
                            if (!backupsDir.exists()) backupsDir.mkdirs();
                            File source = new File(dataFolder, "config.yml");
                            if (source.exists()) {
                                String name = "config-backup-" + LocalDateTime.now().format(BACKUP_FMT) + ".yml";
                                Path target = new File(backupsDir, name).toPath();
                                Files.copy(source.toPath(), target, StandardCopyOption.COPY_ATTRIBUTES);
                                BlockProtLogger.log("config-migration", "Version changed " + storedVersion + " -> " + currentVersion + ", backup: " + target);
                            }
                        }
                    } catch (IOException ioe) {
                        BlockProtLogger.warn("Failed to create config backup: " + ioe.getMessage());
                    }
                    YamlConfiguration bc = new YamlConfiguration();
                    List<?> tEntities = config.getList("lockable_tile_entities");
                    if (tEntities != null) bc.set("lockable_tile_entities", tEntities);
                    List<?> shulkers = config.getList("lockable_shulker_boxes");
                    if (shulkers != null) bc.set("lockable_shulker_boxes", shulkers);
                    List<?> blocks = config.getList("lockable_blocks");
                    if (blocks != null) bc.set("lockable_blocks", blocks);
                    List<?> doors = config.getList("lockable_doors");
                    if (doors != null) bc.set("lockable_doors", doors);
                    try {
                        bc.save(blocksFile);
                        this.blocksConfig = bc;
                        try {
                            config.set("lockable_tile_entities", null);
                            config.set("lockable_shulker_boxes", null);
                            config.set("lockable_blocks", null);
                            config.set("lockable_doors", null);
                            File cfgFile = new File(dataFolder, "config.yml");
                            if (config instanceof YamlConfiguration yc) {
                                yc.save(cfgFile);
                            } else if (BlockProt.getInstance() != null) {
                                BlockProt.getInstance().saveConfig();
                            }
                        } catch (IOException ioe) {
                            BlockProtLogger.warn("Failed to save modified config.yml: " + ioe.getMessage());
                        }
                        BlockProtLogger.log("config-migration", "Extracted block lists to " + blocksFile.getPath());
                    } catch (IOException ioe) {
                        BlockProtLogger.warn("Failed to write blocks file: " + ioe.getMessage());
                    }
                }
            } catch (Exception ex) {
                BlockProtLogger.warn("blocks file handling failed: " + ex.getMessage());
                this.blocksConfig = null;
            }
        }
        this.loadBlocksFromConfig();

        // ── MySQL config ──────────────────────────────────────────────────────
        if (dataFolder != null) {
            File mysqlFile = new File(dataFolder, "mysql/mysql.yml");
            if (mysqlFile.exists()) {
                this.mysqlConfig = YamlConfiguration.loadConfiguration(mysqlFile);
            }
            // If not present, saveResource("mysql/mysql.yml") is called by BlockProt.onEnable()
            // before DefaultConfig is constructed, so it will exist on next load.
        }
    }

    // ── Block lists ───────────────────────────────────────────────────────────

    private void addMaterialIfExists(Collection<Material> set, String... names) {
        for (String name : names) {
            Material m = Material.matchMaterial(name);
            if (m != null) set.add(m);
        }
    }

    private <T extends Enum<?>> void loadBlockListFromConfig(
            @NotNull String key, @NotNull final ArrayList<T> list,
            @NotNull final T[] enumValues, Function<T, Boolean> validateCallback) {
        List<?> configList = null;
        if (this.blocksConfig != null && this.blocksConfig.contains(key))
            configList = this.blocksConfig.getList(key);
        if (configList == null) configList = config.getList(key);
        if (configList == null) return;
        final var stringList = configList.stream()
            .filter(String.class::isInstance).map(String.class::cast)
            .distinct()
            .collect(Collectors.toCollection(ArrayList::new));
        final var newEnumValues = this.loadEnumValuesByName(enumValues, stringList);
        newEnumValues.removeIf((value) -> {
            if (!validateCallback.apply(value)) {
                BlockProt.getInstance().getLogger().warning("Caught invalid value passed to " + key + ": " + value);
                return true;
            }
            return false;
        });
        list.addAll(newEnumValues);
    }

    private void loadBlocksFromConfig() {
        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_19_R1))
            addMaterialIfExists(knownGoodTileEntities, "MANGROVE_SIGN", "MANGROVE_WALL_SIGN");
        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R1)) {
            addMaterialIfExists(knownGoodTileEntities, "CHISELED_BOOKSHELF", "DECORATED_POT");
            addMaterialIfExists(knownGoodTileEntities,
                "OAK_WALL_HANGING_SIGN", "OAK_HANGING_SIGN",
                "SPRUCE_WALL_HANGING_SIGN", "SPRUCE_HANGING_SIGN",
                "BIRCH_WALL_HANGING_SIGN", "BIRCH_HANGING_SIGN",
                "JUNGLE_WALL_HANGING_SIGN", "JUNGLE_HANGING_SIGN",
                "ACACIA_WALL_HANGING_SIGN", "ACACIA_HANGING_SIGN",
                "DARK_OAK_WALL_HANGING_SIGN", "DARK_OAK_HANGING_SIGN",
                "CRIMSON_WALL_HANGING_SIGN", "CRIMSON_HANGING_SIGN",
                "WARPED_WALL_HANGING_SIGN", "WARPED_HANGING_SIGN",
                "MANGROVE_HANGING_SIGN", "MANGROVE_WALL_HANGING_SIGN",
                "CHERRY_SIGN", "CHERRY_WALL_SIGN", "CHERRY_HANGING_SIGN", "CHERRY_WALL_HANGING_SIGN",
                "BAMBOO_SIGN", "BAMBOO_WALL_SIGN", "BAMBOO_HANGING_SIGN", "BAMBOO_WALL_HANGING_SIGN");
        }
        // 1.21+: Crafter (automated crafting block)
        addMaterialIfExists(knownGoodTileEntities, "CRAFTER");
        // The Copper Age (1.21.9 / 26.1+): copper chests + shelves
        addMaterialIfExists(knownGoodTileEntities,
            "COPPER_CHEST", "EXPOSED_COPPER_CHEST", "WEATHERED_COPPER_CHEST", "OXIDIZED_COPPER_CHEST",
            "WAXED_COPPER_CHEST", "WAXED_EXPOSED_COPPER_CHEST", "WAXED_WEATHERED_COPPER_CHEST", "WAXED_OXIDIZED_COPPER_CHEST",
            "COPPER_TRAPPED_CHEST", "EXPOSED_COPPER_TRAPPED_CHEST", "WEATHERED_COPPER_TRAPPED_CHEST", "OXIDIZED_COPPER_TRAPPED_CHEST",
            "WAXED_COPPER_TRAPPED_CHEST", "WAXED_EXPOSED_COPPER_TRAPPED_CHEST", "WAXED_WEATHERED_COPPER_TRAPPED_CHEST", "WAXED_OXIDIZED_COPPER_TRAPPED_CHEST",
            "OAK_SHELF", "SPRUCE_SHELF", "BIRCH_SHELF", "JUNGLE_SHELF", "ACACIA_SHELF",
            "DARK_OAK_SHELF", "MANGROVE_SHELF", "CHERRY_SHELF", "PALE_OAK_SHELF",
            "BAMBOO_SHELF", "CRIMSON_SHELF", "WARPED_SHELF");

        // Validator: accepts any block in knownGoodTileEntities OR any copper chest/shelf name pattern
        java.util.function.Function<Material, Boolean> tileEntityValidator = m -> {
            String name = m.name();
            if (knownGoodTileEntities.contains(m)) return true;
            if (name.contains("COPPER_CHEST") || name.contains("COPPER_TRAPPED_CHEST")) return true;
            if (name.endsWith("_SHELF")) return true;
            if (name.equals("DECORATED_POT") || name.equals("CHISELED_BOOKSHELF") || name.equals("CRAFTER")) return true;
            if (name.equals("JUKEBOX")) return true;
            return false;
        };

        loadBlockListFromConfig("lockable_tile_entities", lockableTileEntities, Material.values(), tileEntityValidator);
        loadBlockListFromConfig("lockable_shulker_boxes", shulkerBoxes, Material.values(), m -> m.toString().contains("SHULKER_BOX"));
        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_16_R3)) {
            // Validator for lockable_blocks: anything not a tile entity, plus specific interactive blocks
            java.util.function.Function<Material, Boolean> blockValidator = m -> {
                if (knownGoodTileEntities.contains(m)) return false; // already a tile entity
                String name = m.name();
                // Allow interactive/utility blocks explicitly
                if (name.equals("DRAGON_EGG")) return true;
                if (name.equals("COMPOSTER") || name.equals("BELL") || name.equals("NOTE_BLOCK")) return true;
                if (name.equals("GRINDSTONE") || name.equals("STONECUTTER") || name.equals("LOOM")) return true;
                if (name.equals("CARTOGRAPHY_TABLE") || name.equals("SMITHING_TABLE") || name.equals("ENCHANTING_TABLE")) return true;
                if (name.contains("CAULDRON")) return true;
                if (name.contains("ANVIL")) return true;
                if (name.contains("FENCE_GATE")) return true;
                if (name.contains("TRAPDOOR")) return true;
                return false;
            };
            loadBlockListFromConfig("lockable_blocks", lockableBlocks, Material.values(), blockValidator);
            loadBlockListFromConfig("lockable_doors", lockableDoors, Material.values(), m -> m.toString().contains("DOOR"));
            lockableBlocks.addAll(lockableDoors);
        }
    }

    // ── General settings ──────────────────────────────────────────────────────

    @Nullable public String getLanguageFile() { return config.getString("language_file"); }

    public boolean shouldReplaceTranslations() {
        return !config.contains("replace_translations") || config.getBoolean("replace_translations");
    }

    public boolean shouldNotifyOpOfUpdates() {
        return config.contains("notify_op_of_updates") && config.getBoolean("notify_op_of_updates");
    }

    public boolean disallowRedstoneOnPlace() {
        return !config.contains("redstone_disallowed_by_default") || config.getBoolean("redstone_disallowed_by_default");
    }

    public boolean isWorldExcluded(World world) {
        if (isWorldsConfigEnabled()) {
            WorldsConfig wc = BlockProt.getWorldsConfig();
            if (wc != null && wc.isWorldDisabled(world)) return true;
        }
        if (listContainsIgnoreCase(excludedWorlds, world.getName())) return true;
        try {
            String keyVal = world.getKey().value();
            if (listContainsIgnoreCase(excludedWorlds, keyVal)) return true;
        } catch (Exception ignored) {}
        return false;
    }

    public boolean isWorldsConfigEnabled() { return config.getBoolean("worlds_config_enabled", false); }

    public boolean isWorldExcluded(InventoryHolder holder) {
        try {
            if (holder instanceof DoubleChest) {
                @Nullable World world = ((DoubleChest) holder).getWorld();
                if (world == null) return true;
                return isWorldExcluded(world);
            }
            return isWorldExcluded(((BlockInventoryHolder) holder).getBlock().getWorld());
        } catch (ClassCastException e) { return true; }
    }

    public boolean lockOnPlaceByDefault() {
        return !config.contains("lock_on_place_by_default") || config.getBoolean("lock_on_place_by_default");
    }

    public boolean publicIsFriendByDefault() {
        return config.contains("public_is_friend_by_default") && config.getBoolean("public_is_friend_by_default");
    }

    @Nullable public String getTranslationFallbackString() {
        return !config.contains("fallback_string") ? "" : config.getString("fallback_string");
    }

    @Nullable public Integer getMaxLockedBlockCount() {
        if (!config.contains("player_max_locked_block_count")) return null;
        int val = config.getInt("player_max_locked_block_count");
        return val > 0 ? val : null;
    }

    public void removeBlockDefaults() {
        Configuration defaults = config.getDefaults();
        if (defaults != null) {
            defaults.set("lockable_tile_entities", null);
            defaults.set("lockable_shulker_boxes", null);
            defaults.set("lockable_blocks", null);
            defaults.set("lockable_doors", null);
            config.setDefaults(defaults);
        }
    }

    public long getLockHintCooldown() {
        return config.contains("lock_hint_cooldown_in_seconds") ? config.getLong("lock_hint_cooldown_in_seconds") : 10;
    }

    /**
     * When {@code use_menus=true} in config, extra CLI commands are disabled and the
     * user menu is the primary interface. When {@code use_menus=false} (default), all
     * extra commands (stats, transfer, timed, etc.) are active.
     */
    public boolean areExtraCommandsEnabled() {
        // use_menus=true  → commands disabled → return false
        // use_menus=false → commands enabled  → return true (default)
        return !config.getBoolean("use_menus", false);
    }

    public boolean shouldEnableAllOptionalFeatures() { return false; } // removed — each feature now has its own config key
    public boolean isLocalizedCommandAliasesEnabled() { return config.getBoolean("localized_command_aliases", true); }

    /**
     * Maximum timed-access duration in seconds. Returns {@code Long.MAX_VALUE} when unlimited.
     * Configured via {@code timed_access_max_duration_days} in config.yml.
     */
    public long getTimedAccessMaxDurationSeconds() {
        int days = config.getInt("timed_access_max_duration_days", 90);
        if (days <= 0) return Long.MAX_VALUE;
        return (long) days * 24 * 60 * 60;
    }
    public boolean isMysqlEnabled() { return mysqlConfig != null && mysqlConfig.contains("mysql.enabled") && mysqlConfig.getBoolean("mysql.enabled"); }

    @NotNull public String getMysqlJdbcUrl() {
        ConfigurationSection src = mysqlConfig != null ? mysqlConfig : config;
        String configured = src.getString("mysql.jdbc_url", "");
        if (configured != null && !configured.isBlank()) return configured;
        return "jdbc:mysql://" + src.getString("mysql.host","127.0.0.1") + ":"
            + src.getInt("mysql.port", 3306) + "/" + src.getString("mysql.database","blockprot")
            + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8";
    }

    @NotNull public String getMysqlUsername() {
        ConfigurationSection src = mysqlConfig != null ? mysqlConfig : config;
        return src.getString("mysql.username","blockprot");
    }
    @NotNull public String getMysqlPassword() {
        ConfigurationSection src = mysqlConfig != null ? mysqlConfig : config;
        return src.getString("mysql.password","");
    }
    public int getMysqlPoolSize() {
        ConfigurationSection src = mysqlConfig != null ? mysqlConfig : config;
        return Math.max(1, src.getInt("mysql.pool.maximum_pool_size", 10));
    }
    public int getMysqlMinimumIdle() {
        ConfigurationSection src = mysqlConfig != null ? mysqlConfig : config;
        return Math.max(0, src.getInt("mysql.pool.minimum_idle", 2));
    }
    public long getMysqlConnectionTimeoutMillis() {
        ConfigurationSection src = mysqlConfig != null ? mysqlConfig : config;
        return Math.max(1000L, src.getLong("mysql.pool.connection_timeout_ms", 10000L));
    }

    public boolean shouldProtectLockedBlocksFromExplosions() {
        return !config.contains("protect_locked_blocks_from_explosions") || config.getBoolean("protect_locked_blocks_from_explosions");
    }
    public boolean shouldBlockProtectedBlockPistonMovement() {
        return !config.contains("block_protected_block_piston_movement") || config.getBoolean("block_protected_block_piston_movement");
    }
    public boolean isWorldEditPasteAutolockEnabled() {
        return config.getBoolean("worldedit_paste_autolock.enabled", false);
    }
    public int    getWorldEditPasteAutolockRadius()       { return Math.max(1, config.getInt("worldedit_paste_autolock.radius", 24)); }
    public int    getWorldEditPasteAutolockMaxBlocks()    { return Math.max(1, config.getInt("worldedit_paste_autolock.max_blocks_per_paste", 5000)); }
    public long   getWorldEditPasteAutolockDelayTicks()   { return Math.max(1L, config.getLong("worldedit_paste_autolock.delay_ticks", 20L)); }

    @NotNull public List<String> getBedrockUsernamePrefixes() {
        return config.contains("bedrock_username_prefixes") ? config.getStringList("bedrock_username_prefixes") : List.of(".", "*", "_");
    }

    public double getFriendSearchSimilarityPercentage() {
        return config.contains("friend_search_similarity") ? config.getDouble("friend_search_similarity") : 0.5;
    }

    public boolean isFriendFunctionalityDisabled() {
        return config.contains("disable_friend_functionality") && config.getBoolean("disable_friend_functionality");
    }

    public boolean shouldClearProtectionOnShulkerBreak()  { return config.getBoolean("clear_protection_on_shulker_break", false); }
    public boolean shouldAllowBreakProtectedBlocks()       { return config.getBoolean("allow_break_protected_blocks", false); }
    public boolean shouldRespectSpawnProtection()          { return !config.contains("respect_spawn_protection") || config.getBoolean("respect_spawn_protection"); }
    public boolean isLockEffectEnabled()                   { return !config.contains("block_lock_effects") || config.getBoolean("block_lock_effects"); }
    public boolean isLockSoundEnabled()                    { return !config.contains("block_lock_sounds") || config.getBoolean("block_lock_sounds"); }
    public boolean isProtectionExpiryEnabled()         { return config.getBoolean("enable_protection_expiry", false); }
    public boolean isExpiryScanOnStartup()              { return config.getBoolean("expiry_scan_on_startup", true); }
    public boolean isSessionLogEnabled()                { return config.getBoolean("enable_session_log", true); }
    public boolean isBackupsEnabled()                   { return config.getBoolean("enable_backups", true); }

    // ── Auto-drop to inventory ────────────────────────────────────────────────

    /** Whether the auto-drop-to-inventory feature is enabled globally. */
    public boolean isAutoDropToInventoryEnabled() {
        if (blocksConfig != null) return blocksConfig.getBoolean("auto_drop_to_inventory.enabled", true);
        return config.getBoolean("auto_drop_to_inventory.enabled", true);
    }

    /** The set of materials that should be delivered to the breaker's inventory. */
    @NotNull
    public Set<Material> getAutoDropToInventoryBlocks() {
        List<?> raw = null;
        if (blocksConfig != null && blocksConfig.contains("auto_drop_to_inventory.blocks"))
            raw = blocksConfig.getList("auto_drop_to_inventory.blocks");
        if (raw == null) raw = config.getList("auto_drop_to_inventory.blocks");
        if (raw == null) return Set.of();
        Set<Material> result = new HashSet<>();
        for (Object o : raw) {
            if (o instanceof String s) {
                Material m = Material.matchMaterial(s);
                if (m != null) result.add(m);
            }
        }
        return result;
    }

    /** True if the given material should auto-drop to the breaker's inventory. */
    public boolean isAutoDropToInventory(@NotNull Material type) {
        return isAutoDropToInventoryEnabled() && getAutoDropToInventoryBlocks().contains(type);
    }

    /** Per-world override: true if the world has auto-drop enabled (defaults to global setting). */
    public boolean isAutoDropToInventoryEnabled(@NotNull World world) {
        WorldsConfig wc = BlockProt.getWorldsConfig();
        if (isWorldsConfigEnabled() && wc != null && wc.hasWorldConfig(world))
            return wc.isAutoDropToInventoryEnabled(world);
        return isAutoDropToInventoryEnabled();
    }

    public String getConsolePrefixColor() { return "§x§8§0§4§0§0§0"; }
    public String getConsoleInfoColor()   { return "§x§D§2§B§4§8§C"; }

    // ─────────────────────────────────────────────────────────────────────────
    // PET PROTECTION — BlockProt Reloaded (BPR)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Master switch for the entire pet protection system.
     * Config key: {@code pet_protection.enabled}  (default: {@code true})
     *
     * @since 1.3.0
     */
    public boolean isPetProtectionEnabled() {
        return config.getBoolean("pet_protection.enabled", false);
    }

    /**
     * When true, newly tamed pets are automatically protected by their owner
     * without the player needing to open the pet settings menu.
     * Config key: {@code pet_protection.auto_protect_on_tame}  (default: {@code true})
     *
     * @since 1.3.0
     */
    public boolean isPetAutoProtectOnTame() {
        return config.getBoolean("pet_protection.auto_protect_on_tame", true);
    }

    /**
     * The item the player must hold in their main hand to open the pet settings menu
     * by right-clicking a tamed animal they own.
     * Config key: {@code pet_protection.menu_item}  (default: {@code STICK})
     *
     * @since 1.3.0
     */
    @NotNull
    public Material getPetMenuItem() {
        String raw = config.getString("pet_protection.menu_item", "STICK");
        Material m = Material.matchMaterial(raw == null ? "STICK" : raw);
        return m == null ? Material.STICK : m;
    }

    /**
     * The chat message sent to a player who tries to interact with a protected pet
     * they do not own.
     * Config key: {@code pet_protection.denied_message}
     *
     * @since 1.3.0
     */
    @NotNull
    public String getPetDeniedMessage() {
        return de.sean.blockprot.bukkit.Translator.get(de.sean.blockprot.bukkit.TranslationKey.MESSAGES__PET_DENIED);
    }

    // ── Block type checks ─────────────────────────────────────────────────────

    public boolean isLockable(Material type) { return isLockableBlock(type) || isLockableTileEntity(type); }

    public boolean isLockable(@NotNull Material type, @NotNull World world) {
        WorldsConfig wc = BlockProt.getWorldsConfig();
        if (isWorldsConfigEnabled() && wc != null && wc.hasWorldConfig(world)) return wc.isLockable(world, type);
        return isLockable(type);
    }

    public boolean isLockableShulkerBox(@NotNull Material type, @NotNull World world) {
        WorldsConfig wc = BlockProt.getWorldsConfig();
        if (isWorldsConfigEnabled() && wc != null && wc.hasWorldConfig(world)) return wc.isLockableShulkerBox(world, type);
        return isLockableShulkerBox(type);
    }

    public boolean isLockableBlock(Material type)       { return lockableBlocks.contains(type); }
    public boolean isLockableTileEntity(Material type)  { return lockableTileEntities.contains(type) || shulkerBoxes.contains(type); }
    public boolean isLockableDoor(Material type)        { return lockableDoors.contains(type); }
    public boolean isLockableShulkerBox(Material type)  { return shulkerBoxes.contains(type); }
    public boolean isLockableInventory(InventoryType t) { return lockableInventories.contains(t); }
}
