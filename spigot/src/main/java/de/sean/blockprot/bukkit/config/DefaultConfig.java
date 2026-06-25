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

package de.sean.blockprot.bukkit.config;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
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
import java.util.*;

/**
 * The default config of the {@link BlockProt} plugin.
 */
public final class DefaultConfig extends BlockProtConfig {

    private final ArrayList<Material> lockableTileEntities = new ArrayList<>();
    private final ArrayList<Material> shulkerBoxes = new ArrayList<>();
    private final ArrayList<Material> lockableBlocks = new ArrayList<>();
    private final ArrayList<Material> lockableDoors = new ArrayList<>();
    private final ArrayList<Material> lockableEntities = new ArrayList<>();

    private final ArrayList<Material> inactiveTileEntities = new ArrayList<>();
    private final ArrayList<Material> inactiveShulkerBoxes = new ArrayList<>();
    private final ArrayList<Material> inactiveBlocks = new ArrayList<>();
    private final ArrayList<Material> inactiveDoors = new ArrayList<>();
    private final ArrayList<Material> inactiveEntities = new ArrayList<>();

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
        Material.JUKEBOX, Material.BEACON,
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

    /** True when the constructor auto-converted blocks.yml to modern expression format. */
    private boolean blocksFileWasConverted = false;

    /** Returns true if blocks.yml was converted to modern expression format during this load. */
    public boolean wasBlocksFileConverted() { return blocksFileWasConverted; }

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
                    YamlConfiguration loaded = YamlConfiguration.loadConfiguration(blocksFile);
                    boolean modernMode = config.getBoolean("modern_family_blocks", false);
                    boolean fileIsLegacy = !blocksFileHasFamilyExpressions(loaded);

                    if (modernMode && fileIsLegacy) {
                        YamlConfiguration converted = convertBlocksYmlToModern(loaded);
                        try {
                            converted.save(blocksFile);
                            this.blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
                            this.blocksFileWasConverted = true;
                            BlockProtLogger.log("blocks-convert", "blocks.yml auto-converted to modern family expression format.");
                        } catch (IOException ioe) {
                            BlockProtLogger.warn("blocks.yml modern conversion failed to save: " + ioe.getMessage());
                            this.blocksConfig = loaded;
                        }
                    } else {
                        this.blocksConfig = loaded;
                    }
                    this.patchBlocksFileIfNeeded(blocksFile, this.blocksConfig);
                } else {
                    File parent = blocksFile.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
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

        if (dataFolder != null) {
            File mysqlFile = new File(dataFolder, "mysql/mysql.yml");
            if (mysqlFile.exists()) {
                this.mysqlConfig = YamlConfiguration.loadConfiguration(mysqlFile);
            }
        }
    }

    private void addMaterialIfExists(Collection<Material> set, String... names) {
        for (String name : names) {
            Material m = Material.matchMaterial(name);
            if (m != null) set.add(m);
        }
    }

    /**
     * Loads a block list from blocks.yml (or config.yml fallback).
     * Family expressions ([*], [* -X], [-*X]) are always parsed regardless of the modern_family_blocks flag.
     * The flag only controls whether flat lists are auto-converted to expressions on startup.
     */
    private void loadBlockListFromConfig(
            @NotNull String key, @NotNull final ArrayList<Material> activeList,
            @NotNull final ArrayList<Material> inactiveList,
            @NotNull final BlockFamilyParser.Family family,
            @NotNull final java.util.function.Function<Material, Boolean> validateCallback) {
        Object rawValue = null;
        if (this.blocksConfig != null && this.blocksConfig.contains(key))
            rawValue = this.blocksConfig.get(key);
        if (rawValue == null) rawValue = config.get(key);
        if (rawValue == null) return;

        Set<Material> resolved = new LinkedHashSet<>();

        if (rawValue instanceof List<?> list) {
            for (Object o : list) {
                if (!(o instanceof String s)) continue;
                String trimmed = s.trim();
                if (BlockFamilyParser.isFamilyExpression(trimmed)) {
                    resolved.addAll(BlockFamilyParser.parseFamilyExpression(trimmed, family));
                } else {
                    Material m = Material.matchMaterial(trimmed);
                    if (m != null) resolved.add(m);
                }
            }
        } else if (rawValue instanceof String s) {
            String trimmed = s.trim();
            if (BlockFamilyParser.isFamilyExpression(trimmed)) {
                resolved.addAll(BlockFamilyParser.parseFamilyExpression(trimmed, family));
            } else {
                Material m = Material.matchMaterial(trimmed);
                if (m != null) resolved.add(m);
            }
        }

        for (Material m : resolved) {
            if (validateCallback.apply(m)) activeList.add(m);
        }

        Set<Material> allFamily = BlockFamilyParser.getFamilyMembers(family);
        Set<Material> activeSet = new HashSet<>(resolved);
        for (Material m : allFamily) {
            if (!activeSet.contains(m) && validateCallback.apply(m)) {
                inactiveList.add(m);
            }
        }
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
        addMaterialIfExists(knownGoodTileEntities, "CRAFTER");
        addMaterialIfExists(knownGoodTileEntities,
            "COPPER_CHEST", "EXPOSED_COPPER_CHEST", "WEATHERED_COPPER_CHEST", "OXIDIZED_COPPER_CHEST",
            "WAXED_COPPER_CHEST", "WAXED_EXPOSED_COPPER_CHEST", "WAXED_WEATHERED_COPPER_CHEST", "WAXED_OXIDIZED_COPPER_CHEST",
            "COPPER_TRAPPED_CHEST", "EXPOSED_COPPER_TRAPPED_CHEST", "WEATHERED_COPPER_TRAPPED_CHEST", "OXIDIZED_COPPER_TRAPPED_CHEST",
            "WAXED_COPPER_TRAPPED_CHEST", "WAXED_EXPOSED_COPPER_TRAPPED_CHEST", "WAXED_WEATHERED_COPPER_TRAPPED_CHEST", "WAXED_OXIDIZED_COPPER_TRAPPED_CHEST",
            "OAK_SHELF", "SPRUCE_SHELF", "BIRCH_SHELF", "JUNGLE_SHELF", "ACACIA_SHELF",
            "DARK_OAK_SHELF", "MANGROVE_SHELF", "CHERRY_SHELF", "PALE_OAK_SHELF",
            "BAMBOO_SHELF", "CRIMSON_SHELF", "WARPED_SHELF");

        java.util.function.Function<Material, Boolean> tileEntityValidator = m -> {
            String name = m.name();
            if (knownGoodTileEntities.contains(m)) return true;
            if (name.contains("COPPER_CHEST") || name.contains("COPPER_TRAPPED_CHEST")) return true;
            if (name.endsWith("_SHELF")) return true;
            if (name.equals("DECORATED_POT") || name.equals("CHISELED_BOOKSHELF") || name.equals("CRAFTER")) return true;
            if (name.equals("JUKEBOX")) return true;
            return false;
        };

        loadBlockListFromConfig("lockable_tile_entities", lockableTileEntities, inactiveTileEntities,
            BlockFamilyParser.Family.TILE_ENTITIES, tileEntityValidator);
        loadBlockListFromConfig("lockable_shulker_boxes", shulkerBoxes, inactiveShulkerBoxes,
            BlockFamilyParser.Family.SHULKER_BOXES, m -> m.toString().contains("SHULKER_BOX"));
        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_16_R3)) {
            java.util.function.Function<Material, Boolean> blockValidator = m -> {
                if (knownGoodTileEntities.contains(m)) return false;
                String name = m.name();
                if (name.equals("DRAGON_EGG")) return true;
                if (name.equals("COMPOSTER") || name.equals("BELL") || name.equals("NOTE_BLOCK")) return true;
                if (name.equals("GRINDSTONE") || name.equals("STONECUTTER") || name.equals("LOOM")) return true;
                if (name.equals("CARTOGRAPHY_TABLE") || name.equals("SMITHING_TABLE") || name.equals("ENCHANTING_TABLE") || name.equals("FLETCHING_TABLE")) return true;
                if (name.contains("CAULDRON")) return true;
                if (name.contains("ANVIL")) return true;
                if (name.contains("FENCE_GATE")) return true;
                if (name.contains("TRAPDOOR")) return true;
                return false;
            };
            loadBlockListFromConfig("lockable_blocks", lockableBlocks, inactiveBlocks,
                BlockFamilyParser.Family.BLOCKS, blockValidator);
            loadBlockListFromConfig("lockable_doors", lockableDoors, inactiveDoors,
                BlockFamilyParser.Family.DOORS, m -> m.toString().contains("DOOR"));
            lockableBlocks.addAll(lockableDoors);
        }

        loadBlockListFromConfig("lockable_entities", lockableEntities, inactiveEntities,
            BlockFamilyParser.Family.ENTITIES, m -> {
                String name = m.name();
                return name.contains("CHEST_BOAT") || name.equals("CHEST_MINECART") || name.equals("HOPPER_MINECART")
                    || name.equals("ITEM_FRAME") || name.equals("GLOW_ITEM_FRAME");
            });
    }

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

    public boolean isWorldsConfigEnabled() { return isPerWorldsConfigEnabled(); }
    public boolean isPerWorldsConfigEnabled() {
        if (config.contains("per_worlds_config")) return config.getBoolean("per_worlds_config", false);
        return config.getBoolean("worlds_config_enabled", false);
    }

    public boolean isAutoReloadEnabled() {
        return config.getBoolean("auto_reload_configs", true);
    }

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

    public boolean areExtraCommandsEnabled() {
        return !config.getBoolean("use_menus", false);
    }

    public boolean shouldEnableAllOptionalFeatures() { return false; }
    public boolean isLocalizedCommandAliasesEnabled() { return config.getBoolean("localized_command_aliases", true); }

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
    public boolean isOwnerNotificationsEnabled() { return config.getBoolean("owner_notifications.enabled", true); }
    public boolean isNotifyOnOpen()               { return isOwnerNotificationsEnabled() && config.getBoolean("owner_notifications.notify_on_open", true); }
    public boolean isNotifyOnTake()               { return isOwnerNotificationsEnabled() && config.getBoolean("owner_notifications.notify_on_take", true); }
    public boolean isNotifyOnPlace()              { return isOwnerNotificationsEnabled() && config.getBoolean("owner_notifications.notify_on_place", true); }

    public boolean isSessionLogEnabled()                { return config.getBoolean("enable_session_log", true); }
    public boolean isBackupsEnabled()                   { return config.getBoolean("enable_backups", true); }

    public boolean isAutoDropToInventoryEnabled() {
        if (blocksConfig != null) return blocksConfig.getBoolean("auto_drop_to_inventory.enabled", true);
        return config.getBoolean("auto_drop_to_inventory.enabled", true);
    }

    /**
     * Returns the set of materials that should be delivered to the breaker's inventory.
     *
     * <p>Each entry in the blocks list is resolved as follows:
     * <ul>
     *   <li>Plain material name — resolved directly.</li>
     *   <li>Family expression — resolved against every known family in order.
     *       Each family contributes independently; results are unioned.
     *       This means {@code [*]} expands to every lockable material across all families,
     *       and {@code [-*SHULKERS]} subtracts shulker boxes from the SHULKER_BOXES family
     *       (which is the only family that owns that sub-family), yielding an empty set for
     *       that family — effectively disabling shulker auto-drop.</li>
     * </ul>
     *
     * <p>To disable auto-drop for shulkers while keeping it for everything else, list each
     * family explicitly and omit shulkers, or use:
     * <pre>
     *   blocks:
     *     - '[* -*SHULKERS]'   # all TILE_ENTITIES, none for SHULKER_BOXES (no star there)
     * </pre>
     * Note that {@code [-*SHULKERS]} alone yields an empty set because there is no base
     * inclusion ({@code *}) before the exclusion — the result is intentionally empty.
     */
    @NotNull
    public Set<Material> getAutoDropToInventoryBlocks() {
        List<?> raw = null;
        if (blocksConfig != null && blocksConfig.contains("auto_drop_to_inventory.blocks"))
            raw = blocksConfig.getList("auto_drop_to_inventory.blocks");
        if (raw == null) raw = config.getList("auto_drop_to_inventory.blocks");
        if (raw == null) return Set.of();

        Set<Material> result = new LinkedHashSet<>();
        for (Object o : raw) {
            if (!(o instanceof String s)) continue;
            String trimmed = s.trim();
            if (BlockFamilyParser.isFamilyExpression(trimmed)) {
                for (BlockFamilyParser.Family f : BlockFamilyParser.Family.values()) {
                    result.addAll(BlockFamilyParser.parseFamilyExpressionSilent(trimmed, f));
                }
            } else {
                Material m = Material.matchMaterial(trimmed);
                if (m != null) result.add(m);
            }
        }
        return result;
    }

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
    public String getBlocksFilePath()      { return config.getString("blocks_file", "blocks.yml"); }

    public boolean isEntityProtectionEnabled() {
        return getEntityProtectionBoolean("enabled", false);
    }

    public boolean isEntityProtectionAutoProtectOnTame() {
        return getEntityProtectionBoolean("auto_protect_on_tame", true);
    }

    @NotNull
    public Material getEntityProtectionMenuItem() {
        String raw = getEntityProtectionString("menu_item", "STICK");
        Material m = Material.matchMaterial(raw == null ? "STICK" : raw);
        return m == null ? Material.STICK : m;
    }

    public int getVillagerLocateSeconds() {
        return Math.max(1, Math.min(10, config.getInt("entity_protection.villager_locate_seconds",
            config.getInt("pet_protection.villager_locate_seconds", 6))));
    }

    public boolean isVillagerWorkstationProtectionEnabled() {
        return config.getBoolean("villager_workstation_protection.enabled", true);
    }

    /**
     * Search radius (in blocks, horizontal) used to find a protected workstation near
     * a block being broken or interacted with. Clamped to [0, 8] to bound the cost of
     * the surrounding-block scan.
     */
    public int getVillagerWorkstationProtectionRadius() {
        return Math.max(0, Math.min(8, config.getInt("villager_workstation_protection.radius", 2)));
    }

    /**
     * Vertical search radius (in blocks) used together with {@link #getVillagerWorkstationProtectionRadius()}.
     * Clamped to [0, 4].
     */
    public int getVillagerWorkstationProtectionVerticalRadius() {
        return Math.max(0, Math.min(4, config.getInt("villager_workstation_protection.vertical_radius", 1)));
    }

    private boolean getEntityProtectionBoolean(@NotNull String leaf, boolean def) {
        String modern = "entity_protection." + leaf;
        if (config.contains(modern)) return config.getBoolean(modern, def);
        return config.getBoolean("pet_protection." + leaf, def);
    }

    @Nullable
    private String getEntityProtectionString(@NotNull String leaf, @NotNull String def) {
        String modern = "entity_protection." + leaf;
        if (config.contains(modern)) return config.getString(modern, def);
        return config.getString("pet_protection." + leaf, def);
    }

    @NotNull
    public String getEntityProtectionDeniedMessage() {
        return de.sean.blockprot.bukkit.Translator.get(de.sean.blockprot.bukkit.TranslationKey.MESSAGES__ENTITY_DENIED);
    }

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

    public boolean isLockableEntity(@NotNull Material type) {
        return lockableEntities.contains(type);
    }

    public boolean isLockableEntity(@NotNull Material type, @NotNull World world) {
        WorldsConfig wc = BlockProt.getWorldsConfig();
        if (isWorldsConfigEnabled() && wc != null && wc.hasWorldConfig(world))
            return wc.isLockableEntity(world, type);
        return isLockableEntity(type);
    }

    public boolean isInactive(Material type) {
        return inactiveTileEntities.contains(type) || inactiveShulkerBoxes.contains(type)
            || inactiveBlocks.contains(type) || inactiveDoors.contains(type) || inactiveEntities.contains(type);
    }

    @NotNull public List<Material> getInactiveTileEntities() { return Collections.unmodifiableList(inactiveTileEntities); }
    @NotNull public List<Material> getInactiveShulkerBoxes() { return Collections.unmodifiableList(inactiveShulkerBoxes); }
    @NotNull public List<Material> getInactiveBlocks()       { return Collections.unmodifiableList(inactiveBlocks); }
    @NotNull public List<Material> getInactiveDoors()        { return Collections.unmodifiableList(inactiveDoors); }
    @NotNull public List<Material> getInactiveEntities()     { return Collections.unmodifiableList(inactiveEntities); }

    public boolean isModernFamilyBlocks() { return config.getBoolean("modern_family_blocks", false); }

    /**
     * Returns true if any lockable key in the given blocks.yml config contains a family expression.
     */
    private static boolean blocksFileHasFamilyExpressions(@NotNull YamlConfiguration cfg) {
        for (String key : List.of("lockable_tile_entities", "lockable_shulker_boxes",
                                  "lockable_blocks", "lockable_doors", "lockable_entities")) {
            Object raw = cfg.get(key);
            if (raw instanceof String s && BlockFamilyParser.isFamilyExpression(s.trim())) return true;
            if (raw instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof String s
                    && BlockFamilyParser.isFamilyExpression(s.trim())) return true;
        }
        return false;
    }

    /**
     * Converts a legacy flat-list blocks.yml to modern family expression format.
     * Only called when modern_family_blocks=true and the file is still in legacy format.
     */
    @NotNull
    private static YamlConfiguration convertBlocksYmlToModern(@NotNull YamlConfiguration legacy) {
        YamlConfiguration modern = new YamlConfiguration();

        record KeyFamily(String key, BlockFamilyParser.Family family) {}
        List<KeyFamily> pairs = List.of(
            new KeyFamily("lockable_tile_entities", BlockFamilyParser.Family.TILE_ENTITIES),
            new KeyFamily("lockable_shulker_boxes",  BlockFamilyParser.Family.SHULKER_BOXES),
            new KeyFamily("lockable_blocks",         BlockFamilyParser.Family.BLOCKS),
            new KeyFamily("lockable_doors",          BlockFamilyParser.Family.DOORS),
            new KeyFamily("lockable_entities",       BlockFamilyParser.Family.ENTITIES)
        );

        for (var kf : pairs) {
            if (!legacy.contains(kf.key())) continue;
            Object raw = legacy.get(kf.key());
            Set<Material> active = BlockFamilyParser.parse(raw, kf.family());
            String expr = BlockFamilyParser.toFamilyExpression(active, kf.family());
            if (expr != null) {
                modern.set(kf.key(), List.of(expr));
                BlockProtLogger.log("blocks-convert", kf.key() + " -> " + expr);
            } else {
                modern.set(kf.key(), raw);
            }
        }

        if (legacy.contains("auto_drop_to_inventory")) {
            modern.set("auto_drop_to_inventory.enabled",
                legacy.getBoolean("auto_drop_to_inventory.enabled", true));
            Object dropRaw = legacy.get("auto_drop_to_inventory.blocks");
            if (dropRaw instanceof List<?>) {
                Set<Material> dropActive = BlockFamilyParser.parse(dropRaw, BlockFamilyParser.Family.SHULKER_BOXES);
                Set<Material> allShulkers = BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.SHULKER_BOXES);
                if (dropActive.containsAll(allShulkers)) {
                    modern.set("auto_drop_to_inventory.blocks", List.of("[*-SHULKERS]"));
                } else {
                    String shulkerExpr = BlockFamilyParser.toFamilyExpression(dropActive, BlockFamilyParser.Family.SHULKER_BOXES);
                    modern.set("auto_drop_to_inventory.blocks",
                        shulkerExpr != null ? List.of(shulkerExpr) : dropRaw);
                }
            } else {
                modern.set("auto_drop_to_inventory.blocks", dropRaw);
            }
        }

        return modern;
    }

    /**
     * Overwrites blocksFile with the JAR's bundled blocks.yml resource.
     */
    private static boolean resetBlocksFileFromJar(@NotNull File blocksFile) {
        de.sean.blockprot.bukkit.BlockProt plugin = de.sean.blockprot.bukkit.BlockProt.getInstance();
        if (plugin == null) return false;
        try (java.io.InputStream is = plugin.getResource("blocks.yml")) {
            if (is == null) return false;
            byte[] bytes = is.readAllBytes();
            java.nio.file.Files.write(blocksFile.toPath(), bytes,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            BlockProtLogger.log("blocks-reset", "blocks.yml reset from JAR defaults.");
            return true;
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to reset blocks.yml from JAR: " + e.getMessage());
            return false;
        }
    }

    @Nullable
    private File getBlocksFile() {
        if (dataFolder == null) return null;
        String blocksFilePath = config.getString("blocks_file", "blocks.yml");
        return new File(dataFolder, blocksFilePath);
    }

    private void saveBlocksConfig() {
        File file = getBlocksFile();
        if (file == null || blocksConfig == null) return;
        try {
            blocksConfig.save(file);
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to save blocks.yml: " + e.getMessage());
        }
    }

    private void reloadBlocksAfterToggle() {
        loadBlocksFromConfig();
    }

    @Nullable
    private static String configKeyForMaterial(@NotNull Material material) {
        for (BlockFamilyParser.Family family : BlockFamilyParser.Family.values()) {
            if (BlockFamilyParser.getFamilyMembers(family).contains(material)) {
                return switch (family) {
                    case TILE_ENTITIES -> "lockable_tile_entities";
                    case SHULKER_BOXES -> "lockable_shulker_boxes";
                    case BLOCKS -> "lockable_blocks";
                    case DOORS -> "lockable_doors";
                    case ENTITIES -> "lockable_entities";
                };
            }
        }
        return null;
    }

    @Nullable
    public static String configKeyForFamily(@NotNull BlockFamilyParser.Family family) {
        return switch (family) {
            case TILE_ENTITIES -> "lockable_tile_entities";
            case SHULKER_BOXES -> "lockable_shulker_boxes";
            case BLOCKS -> "lockable_blocks";
            case DOORS -> "lockable_doors";
            case ENTITIES -> "lockable_entities";
        };
    }

    /**
     * Toggles a single material in blocks.yml. Adds it if currently inactive, removes it
     * if currently active. Saves to disk and logs the change. Handles both flat and
     * expression-based entries.
     *
     * @param material the material to toggle
     * @param who      the player who initiated the toggle
     * @return the new active state (true = now lockable, false = now not lockable)
     */
    public synchronized boolean toggleLockable(@NotNull Material material, @NotNull Player who) {
        String configKey = configKeyForMaterial(material);
        if (configKey == null || blocksConfig == null) return isLockable(material);

        List<String> list = new ArrayList<>(blocksConfig.getStringList(configKey));
        String name = material.name();
        String exclusion = "-" + name;
        boolean currentlyActive = isLockable(material) || isLockableEntity(material);

        if (currentlyActive) {
            list.remove(name);
            list.remove(exclusion);
            if (!list.contains(exclusion)) list.add(exclusion);
        } else {
            list.remove(exclusion);
            if (!list.contains(name)) list.add(name);
        }

        blocksConfig.set(configKey, list);
        saveBlocksConfig();
        reloadBlocksAfterToggle();

        BlockProtLogger.log("lockables-toggle",
            (currentlyActive ? "Disabled" : "Enabled") + " " + name + " in " + configKey
                + " (by " + who.getName() + ")");
        if (who.isOnline()) {
            who.sendMessage(Component.text(
                (currentlyActive ? "Disabled" : "Enabled") + " " + name + " in blocks.yml")
                .color(currentlyActive ? NamedTextColor.RED : NamedTextColor.GREEN));
        }

        return !currentlyActive;
    }

    /**
     * Enables an entire family (all its members) in blocks.yml using the appropriate
     * family expression. In flat mode, adds a {@code [*]} or sub-family expression. In
     * modern mode, replaces the list with the expression.
     *
     * @param family   the family to enable
     * @param configKey the config key to modify
     * @param expression the family expression to set
     * @param who      the player who initiated the action
     */
    public synchronized void enableFamily(
            @NotNull BlockFamilyParser.Family family,
            @NotNull String configKey,
            @NotNull String expression,
            @NotNull Player who) {
        if (blocksConfig == null) return;

        List<String> list = new ArrayList<>();
        list.add(expression);
        blocksConfig.set(configKey, list);
        saveBlocksConfig();
        reloadBlocksAfterToggle();

        BlockProtLogger.log("lockables-toggle",
            "Enabled " + family.name() + " via " + expression + " in " + configKey
                + " (by " + who.getName() + ")");
        if (who.isOnline()) {
            who.sendMessage(Component.text(
                "Enabled all " + family.name() + " in blocks.yml")
                .color(NamedTextColor.GREEN));
        }
    }

    /**
     * Patches an existing blocks.yml on disk by adding any keys that are present in the
     * bundled JAR default but absent from the file. Existing keys are never touched.
     *
     * <p>This handles upgrades where a new config key (e.g. {@code lockable_entities}) is
     * added in a newer plugin version. The user's file continues to work unchanged, and the
     * new key appears at the bottom of the file with its JAR default value.
     *
     * <p>Keys patched: {@code lockable_entities}, {@code auto_drop_to_inventory}.
     */
    private void patchBlocksFileIfNeeded(@NotNull File blocksFile, @NotNull YamlConfiguration bc) {
        boolean dirty = false;

        if (!bc.contains("lockable_entities")) {
            bc.set("lockable_entities", Collections.emptyList());
            BlockProtLogger.log("blocks-patch", "blocks.yml: added missing key 'lockable_entities' (empty list).");
            dirty = true;
        }

        if (!bc.contains("auto_drop_to_inventory")) {
            bc.set("auto_drop_to_inventory.enabled", true);
            bc.set("auto_drop_to_inventory.blocks", List.of(
                "SHULKER_BOX", "WHITE_SHULKER_BOX", "ORANGE_SHULKER_BOX", "MAGENTA_SHULKER_BOX",
                "LIGHT_BLUE_SHULKER_BOX", "YELLOW_SHULKER_BOX", "LIME_SHULKER_BOX", "PINK_SHULKER_BOX",
                "GRAY_SHULKER_BOX", "LIGHT_GRAY_SHULKER_BOX", "CYAN_SHULKER_BOX", "PURPLE_SHULKER_BOX",
                "BLUE_SHULKER_BOX", "BROWN_SHULKER_BOX", "GREEN_SHULKER_BOX", "RED_SHULKER_BOX",
                "BLACK_SHULKER_BOX"
            ));
            BlockProtLogger.log("blocks-patch", "blocks.yml: added missing key 'auto_drop_to_inventory' (default shulkers).");
            dirty = true;
        }

        if (dirty) {
            try {
                bc.save(blocksFile);
            } catch (IOException e) {
                BlockProtLogger.warn("blocks.yml patch save failed: " + e.getMessage());
            }
        }
    }

    /**
     * @deprecated No longer called — migration is manual via yml + config flag.
     */
    @Deprecated
    public void migrateLegacyBlocksIfNeeded() {}
}
