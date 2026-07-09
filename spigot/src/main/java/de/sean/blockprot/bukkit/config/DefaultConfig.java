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
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.tasks.ConfigFileWatcher;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
                    this.patchBlocksFileIfNeeded(blocksFile, this.blocksConfig);
                } else {
                    File parent = blocksFile.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    this.createBlocksFileWithHeader(blocksFile);
                    this.blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
                    BlockProtLogger.log("blocks", "Created empty blocks.yml at " + blocksFile.getPath());
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
        activeList.clear();
        inactiveList.clear();

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
    @Deprecated
    public boolean isWorldEditPasteAutolockEnabled() {
        return config.getBoolean("worldedit_paste_autolock.enabled", false);
    }
    @Deprecated
    public int    getWorldEditPasteAutolockRadius()       { return Math.max(1, config.getInt("worldedit_paste_autolock.radius", 24)); }
    @Deprecated
    public int    getWorldEditPasteAutolockMaxBlocks()    { return Math.max(1, config.getInt("worldedit_paste_autolock.max_blocks_per_paste", 5000)); }
    @Deprecated
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

    public boolean isPistonProtectionEnabled() { return config.getBoolean("piston_protection", true); }
    public boolean shouldClearProtectionOnShulkerBreak()  { return config.getBoolean("clear_protection_on_shulker_break", false); }

    public boolean isSimplifiedHopperLogic() { return config.getBoolean("simplified_hopper_logic", false); }
    public boolean shouldAllowBreakProtectedBlocks()       { return config.getBoolean("allow_break_protected_blocks", false); }
    public boolean shouldRespectSpawnProtection()          { return !config.contains("respect_spawn_protection") || config.getBoolean("respect_spawn_protection"); }
    public boolean isLockEffectEnabled()                   { return !config.contains("block_lock_effects") || config.getBoolean("block_lock_effects"); }
    public boolean isLockSoundEnabled()                    { return !config.contains("block_lock_sounds") || config.getBoolean("block_lock_sounds"); }
    public boolean isWorldExpiryEnabled()               { return config.getBoolean("world_expiry.enabled", false); }
    public int getWorldExpiryCheckInterval()            { return config.getInt("world_expiry.check_interval_minutes", 10); }
    public java.util.Map<String, String> getWorldExpiryDurations() {
        org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection("world_expiry.worlds");
        if (section == null) return java.util.Map.of();
        java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            result.put(key, section.getString(key, "0"));
        }
        return result;
    }
    public void setWorldExpiryDuration(@NotNull String worldName, @NotNull String duration) {
        config.set("world_expiry.worlds." + worldName, duration);
        BlockProt.getInstance().saveConfig();
    }
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
     *   <li>Plain material name: resolved directly.</li>
     *   <li>Family expression: resolved against every known family in order.
     *       Each family contributes independently; results are unioned.
     *       This means {@code [*]} expands to every lockable material across all families,
     *       and {@code [-*SHULKERS]} subtracts shulker boxes from the SHULKER_BOXES family
     *       (which is the only family that owns that sub-family), yielding an empty set for
     *       that family: effectively disabling shulker auto-drop.</li>
     * </ul>
     *
     * <p>To disable auto-drop for shulkers while keeping it for everything else, list each
     * family explicitly and omit shulkers, or use:
     * <pre>
     *   blocks:
     *     - '[* -*SHULKERS]'   # all TILE_ENTITIES, none for SHULKER_BOXES (no star there)
     * </pre>
     * Note that {@code [-*SHULKERS]} alone yields an empty set because there is no base
     * inclusion ({@code *}) before the exclusion: the result is intentionally empty.
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
     * Determines the output format for a blocks.yml key based on the global
     * {@code modern_family_blocks} flag. When false, always uses flat format.
     * When true, uses family expressions.
     */
    private boolean isKeyInExpressionFormat(@NotNull String configKey) {
        if (!isModernFamilyBlocks()) return false;
        if (blocksConfig == null) return true;
        Object raw = blocksConfig.get(configKey);
        if (raw instanceof String s) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) return BlockFamilyParser.isFamilyExpression(trimmed);
        }
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (!(o instanceof String s)) continue;
                String trimmed = s.trim();
                if (trimmed.isEmpty()) continue;
                return BlockFamilyParser.isFamilyExpression(trimmed);
            }
        }
        return true;
    }

    @Nullable
    private File getBlocksFile() {
        if (dataFolder == null) return null;
        String blocksFilePath = config.getString("blocks_file", "blocks.yml");
        return new File(dataFolder, blocksFilePath);
    }

    public static final List<String> BLOCKS_YML_KEY_ORDER = List.of(
        "lockable_tile_entities", "lockable_shulker_boxes",
        "lockable_blocks", "lockable_doors", "lockable_entities",
        "auto_drop_to_inventory"
    );

    private static final List<String> LOCKABLE_LIST_KEYS = List.of(
        "lockable_tile_entities", "lockable_shulker_boxes",
        "lockable_blocks", "lockable_doors", "lockable_entities"
    );

    private static final String AUTO_DROP_BLOCKS_KEY = "auto_drop_to_inventory.blocks";

    /**
     * True for a blocks.yml list entry that carries no material name: a YAML null
     * (blank template line), a blank string, or the legacy "[]" / "[ ]" hint text.
     * Canonical implementation; other classes delegate to this one.
     */
    public static boolean isPlaceholderEntry(@Nullable Object o) {
        if (o == null) return true;
        if (o instanceof String s) {
            String t = s.trim();
            return t.isEmpty() || t.equals("[]") || t.equals("[ ]");
        }
        return false;
    }

    public static void sanitizeBlocksListsForSave(@NotNull YamlConfiguration cfg, boolean modernFormat) {
        sanitizeListsForSave(cfg, "", modernFormat);
    }

    /**
     * Sanitizes lists for saving to disk, supporting an optional key prefix (e.g. for worlds).
     */
    public static void sanitizeListsForSave(@NotNull YamlConfiguration cfg, @NotNull String prefix, boolean modernFormat) {
        List<String> keys = new ArrayList<>(LOCKABLE_LIST_KEYS);
        if (prefix.isEmpty()) {
            keys.add(AUTO_DROP_BLOCKS_KEY);
        }
        for (String k : keys) {
            String key = prefix + k;
            if (!cfg.contains(key)) continue;
            List<?> raw = cfg.getList(key);
            if (raw == null) continue;

            List<Object> real = new ArrayList<>();
            for (Object o : raw) {
                if (!isPlaceholderEntry(o)) real.add(o);
            }

            if (real.isEmpty()) {
                if (k.equals(AUTO_DROP_BLOCKS_KEY)) {
                    cfg.set(key, Collections.emptyList());
                } else {
                    cfg.set(key, modernFormat ? List.of("[]") : Arrays.asList(null, null));
                }
            } else if (real.size() != raw.size()) {
                cfg.set(key, real);
            }
        }
    }

    /**
     * Header shared by both blocks.yml generation paths, so the comment text and the
     * referenced doc path never drift apart from each other or from the shipped resource.
     */
    private static final List<String> BLOCKS_HEADER = List.of(
        "# BlockProt Reloaded -- blocks.yml",
        "# Add block or material names below each list, one per line, replacing the blank lines.",
        "# Run /bp recommended for a ready-made starting selection instead of editing by hand.",
        "# Format: flat names (CHEST) or family expressions ([*-CHEST]).",
        "# See docs/MODERN SYNTAX AND LEGACY/BLOCK_FAMILY_SYNTAX.md for full syntax.",
        "# This file is NEVER modified on startup/reload. Only GUI toggles write here."
    );

    /**
     * Returns a new YamlConfiguration with all keys from the source, but
     * with top-level keys arranged in BLOCKS_YML_KEY_ORDER. Extra keys
     * not in the order list are appended at the end.
     */
    public static YamlConfiguration reorderBlocksKeys(@NotNull YamlConfiguration source) {
        YamlConfiguration result = new YamlConfiguration();
        for (String key : BLOCKS_YML_KEY_ORDER) {
            if (!source.contains(key)) continue;
            Object val = source.get(key);
            if (val instanceof ConfigurationSection section) {
                for (String sk : section.getKeys(false)) {
                    result.set(key + "." + sk, section.get(sk));
                }
            } else {
                result.set(key, val);
            }
        }
        for (String key : source.getKeys(false)) {
            if (BLOCKS_YML_KEY_ORDER.contains(key)) continue;
            Object val = source.get(key);
            if (val instanceof ConfigurationSection section) {
                for (String sk : section.getKeys(false)) {
                    result.set(key + "." + sk, section.get(sk));
                }
            } else {
                result.set(key, val);
            }
        }
        return result;
    }

    private void saveBlocksConfig() {
        File file = getBlocksFile();
        if (file == null || blocksConfig == null) return;
        try {
            de.sean.blockprot.bukkit.BlockProt plugin = de.sean.blockprot.bukkit.BlockProt.getInstance();
            if (plugin != null && plugin.getFileWatcher() != null) {
                plugin.getFileWatcher().suppressNext();
            }
            sanitizeBlocksListsForSave(blocksConfig, isModernFamilyBlocks());
            blocksConfig = reorderBlocksKeys(blocksConfig);
            blocksConfig.save(file);
            prependBlocksHeader(file);
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to save blocks.yml: " + e.getMessage());
        }
    }

    public static void prependBlocksHeader(@NotNull File file) throws IOException {
        List<String> existingLines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        if (!existingLines.isEmpty() && existingLines.get(0).equals(BLOCKS_HEADER.get(0))) {
            cleanNullPlaceholderLines(file);
            return;
        }
        List<String> newLines = new ArrayList<>(BLOCKS_HEADER);
        newLines.add("");
        int skipLimit = BLOCKS_HEADER.size() + 1;
        int skipCount = 0;
        for (String line : existingLines) {
            if (line.startsWith("# BlockProt") || (line.isEmpty() && skipCount < skipLimit)) {
                skipCount++;
                continue;
            }
            newLines.add(line);
        }
        Files.write(file.toPath(), newLines, StandardCharsets.UTF_8);
        cleanNullPlaceholderLines(file);
    }

    /**
     * Replaces any occurrence of "- null" or "- 'null'" in the file with "- " (a blank entry).
     * This is needed because Bukkit YamlConfiguration writes null list elements as "null".
     */
    public static void cleanNullPlaceholderLines(@NotNull File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        List<String> newLines = new ArrayList<>();
        boolean modified = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.equals("- null") || trimmed.equals("- 'null'")) {
                int idx = line.indexOf('-');
                String newLine = line.substring(0, idx + 1);
                newLines.add(newLine);
                modified = true;
            } else {
                newLines.add(line);
            }
        }
        if (modified) {
            Files.write(file.toPath(), newLines, StandardCharsets.UTF_8);
        }
    }

    private void reloadBlocksAfterToggle() {
        loadBlocksFromConfig();
        checkAutoDisableModernFamilyBlocks();
    }

    private void checkAutoDisableModernFamilyBlocks() {
        if (!isModernFamilyBlocks()) return;
        if (blocksConfig == null) return;
        boolean allEmpty = true;
        outer:
        for (String key : LOCKABLE_LIST_KEYS) {
            List<?> list = blocksConfig.getList(key);
            if (list == null) continue;
            for (Object o : list) {
                // Blank template placeholders (null or empty string) carry no material
                // and do not count as configured content.
                if (o instanceof String s && s.isBlank()) continue;
                if (o == null) continue;
                allEmpty = false;
                break outer;
            }
        }
        if (allEmpty) {
            File configFile = de.sean.blockprot.bukkit.BlockProt.getInstance().getDataFolder().toPath()
                .resolve("config.yml").toFile();
            if (configFile.exists()) {
                org.bukkit.configuration.file.YamlConfiguration cfg =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
                cfg.set("modern_family_blocks", false);
                try { cfg.save(configFile); } catch (java.io.IOException ignored) {}
                BlockProtLogger.log("blocks-auto",
                    "All lockable lists empty, disabled modern_family_blocks.");
            }
        }
    }

    /**
     * Returns true if there is at least one non-placeholder entry configured
     * in the lockable block lists of blocks.yml.
     */
    public boolean hasConfiguredBlocks() {
        if (blocksConfig == null) return false;
        for (String key : LOCKABLE_LIST_KEYS) {
            List<?> list = blocksConfig.getList(key);
            if (list == null) continue;
            for (Object o : list) {
                if (!isPlaceholderEntry(o)) {
                    return true;
                }
            }
        }
        return false;
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

        String name = material.name();
        boolean currentlyActive = isLockable(material) || isLockableEntity(material);
        BlockFamilyParser.Family family = familyForMaterial(material);

        if (family != null) {
            Object raw = blocksConfig.get(configKey);
            Set<Material> active = BlockFamilyParser.parse(raw, family);
            if (currentlyActive) {
                active.remove(material);
            } else {
                active.add(material);
            }
            if (isKeyInExpressionFormat(configKey)) {
                String expr = BlockFamilyParser.toFamilyExpression(active, family);
                if (expr != null) {
                    blocksConfig.set(configKey, List.of(expr));
                } else {
                    blocksConfig.set(configKey, active.stream().map(Material::name).sorted().toList());
                }
            } else {
                blocksConfig.set(configKey, active.stream().map(Material::name).sorted().toList());
            }
        }

        saveBlocksConfig();
        reloadBlocksAfterToggle();

        String actionText = currentlyActive
            ? Translator.get(TranslationKey.DISABLED)
            : Translator.get(TranslationKey.ENABLED);
        BlockProtLogger.log("lockables-toggle",
            actionText + " " + name + " in " + configKey
                + " (by " + who.getName() + ")");
        if (who.isOnline()) {
            who.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__LOCKABLES__TOGGLE_FEEDBACK)
                    .replace("{action}", actionText)
                    .replace("{name}", name)));
        }

        return !currentlyActive;
    }

    @Nullable
    private static BlockFamilyParser.Family familyForMaterial(@NotNull Material material) {
        for (BlockFamilyParser.Family family : BlockFamilyParser.Family.values()) {
            if (BlockFamilyParser.getFamilyMembers(family).contains(material)) {
                return family;
            }
        }
        return null;
    }

    /**
     * Toggles an entire family (all its members) in blocks.yml.
     * If all members matching the expression are already active, disables them.
     * Otherwise enables all matching members.
     *
     * @param family     the family to toggle
     * @param configKey  the config key to modify
     * @param expression the family expression to toggle
     * @param who        the player who initiated the action
     */
    public synchronized void toggleFamily(
            @NotNull BlockFamilyParser.Family family,
            @NotNull String configKey,
            @NotNull String expression,
            @NotNull Player who) {
        if (blocksConfig == null) return;

        Object raw = blocksConfig.get(configKey);
        Set<Material> current = BlockFamilyParser.parse(raw, family);
        Set<Material> target = BlockFamilyParser.parse(expression, family);
        boolean allActive = target.stream().allMatch(current::contains);

        if (allActive) {
            current.removeAll(target);
        } else {
            current.addAll(target);
        }

        if (isKeyInExpressionFormat(configKey)) {
            String expr = BlockFamilyParser.toFamilyExpression(current, family);
            if (expr != null) {
                blocksConfig.set(configKey, List.of(expr));
            } else {
                blocksConfig.set(configKey, current.stream().map(Material::name).sorted().toList());
            }
        } else {
            blocksConfig.set(configKey, current.stream().map(Material::name).sorted().toList());
        }
        saveBlocksConfig();
        reloadBlocksAfterToggle();

        String actionText = allActive
            ? Translator.get(TranslationKey.DISABLED)
            : Translator.get(TranslationKey.ENABLED);
        BlockProtLogger.log("lockables-toggle",
            actionText + " " + family.name() + " via " + expression + " in " + configKey
                + " (by " + who.getName() + ")");
        if (who.isOnline()) {
            who.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__LOCKABLES__TOGGLE_FAMILY_FEEDBACK)
                    .replace("{action}", actionText)
                    .replace("{family}", family.name())));
        }
    }

    /**
     * Toggles a single material in the auto-drop block list.
     *
     * @param material the material to toggle
     * @param who      the player who initiated the toggle
     * @return the new auto-drop state for this material
     */
    public synchronized boolean toggleAutoDropMaterial(@NotNull Material material, @NotNull Player who) {
        if (blocksConfig == null) return isAutoDropToInventory(material);

        Set<Material> current = getAutoDropToInventoryBlocks();
        boolean currentlyActive = current.contains(material);
        String name = material.name();

        if (currentlyActive) {
            current.remove(material);
        } else {
            current.add(material);
        }

        writeAutoDropBlocks(current);

        String actionText = currentlyActive
            ? Translator.get(TranslationKey.DISABLED)
            : Translator.get(TranslationKey.ENABLED);
        BlockProtLogger.log("autodrop-toggle",
            actionText + " " + name + " in auto_drop_to_inventory"
                + " (by " + who.getName() + ")");
        if (who.isOnline()) {
            who.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__AUTO_DROP__TOGGLE_FEEDBACK)
                    .replace("{action}", actionText)
                    .replace("{name}", name)));
        }

        return !currentlyActive;
    }

    /**
     * Toggles an entire family in the auto-drop block list.
     * If all family members are already present, removes them all.
     * Otherwise adds all family members.
     *
     * @param family the family to toggle
     * @param who    the player who initiated the toggle
     * @return true if the family is now enabled (all members active)
     */
    public synchronized boolean toggleAutoDropFamily(@NotNull BlockFamilyParser.Family family, @NotNull Player who) {
        if (blocksConfig == null) return false;

        Set<Material> current = getAutoDropToInventoryBlocks();
        Set<Material> members = BlockFamilyParser.getFamilyMembers(family);
        boolean allActive = members.stream().allMatch(current::contains);

        if (allActive) {
            current.removeAll(members);
        } else {
            current.addAll(members);
        }

        writeAutoDropBlocks(current);

        String actionText = allActive
            ? Translator.get(TranslationKey.DISABLED)
            : Translator.get(TranslationKey.ENABLED);
        BlockProtLogger.log("autodrop-toggle",
            actionText + " " + family.name() + " in auto_drop_to_inventory"
                + " (by " + who.getName() + ")");
        if (who.isOnline()) {
            who.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__AUTO_DROP__TOGGLE_FAMILY_FEEDBACK)
                    .replace("{action}", actionText)
                    .replace("{family}", family.name())));
        }

        return !allActive;
    }

    /**
     * Writes the active auto-drop material set back to {@code blocksConfig},
     * using family expressions when {@code modern_family_blocks} is enabled.
     */
    private synchronized void writeAutoDropBlocks(@NotNull Set<Material> active) {
        if (blocksConfig == null) return;

        List<String> list;
        if (isModernFamilyBlocks()) {
            list = new ArrayList<>();

            boolean allActive = true;
            for (BlockFamilyParser.Family f : BlockFamilyParser.Family.values()) {
                if (!active.containsAll(BlockFamilyParser.getFamilyMembers(f))) {
                    allActive = false;
                    break;
                }
            }
            if (allActive) {
                list.add("[*]");
            } else {
                Set<Material> covered = new java.util.LinkedHashSet<>();
                for (BlockFamilyParser.SubFamily sf : BlockFamilyParser.SubFamily.values()) {
                    Set<Material> sfMembers = BlockFamilyParser.getSubFamilyMembers(sf);
                    if (sfMembers.isEmpty()) continue;
                    if (active.containsAll(sfMembers)) {
                        list.add("[*-" + sf.tag + "]");
                        covered.addAll(sfMembers);
                    }
                }
                for (Material m : active.stream().sorted().toList()) {
                    if (!covered.contains(m)) list.add(m.name());
                }
            }
        } else {
            list = active.stream().map(Material::name).sorted().toList();
        }

        blocksConfig.set("auto_drop_to_inventory.blocks", list);
        saveBlocksConfig();
        reloadBlocksAfterToggle();
    }

    /**
     * Returns the representative Material for a family, used in GUI icons.
     */
    @NotNull
    public static Material representativeMaterialForFamily(@NotNull BlockFamilyParser.Family family) {
        return switch (family) {
            case TILE_ENTITIES -> Material.CHEST;
            case SHULKER_BOXES -> Material.SHULKER_BOX;
            case BLOCKS -> Material.ANVIL;
            case DOORS -> Material.OAK_DOOR;
            case ENTITIES -> Material.ITEM_FRAME;
        };
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
            bc.set("auto_drop_to_inventory.blocks", Collections.emptyList());
            BlockProtLogger.log("blocks-patch", "blocks.yml: added missing key 'auto_drop_to_inventory' (disabled by default).");
            dirty = true;
        }

        if (dirty) {
            try {
                bc.save(blocksFile);
                prependBlocksHeader(blocksFile);
            } catch (IOException e) {
                BlockProtLogger.warn("blocks.yml patch save failed: " + e.getMessage());
            }
        }
    }

    /**
     * Creates a fresh blocks.yml with the standard header comments.
     * Used when the file does not exist yet.
     */
    private void createBlocksFileWithHeader(@NotNull File blocksFile) throws IOException {
        List<String> lines = new ArrayList<>(BLOCKS_HEADER);
        lines.add("");
        for (String key : LOCKABLE_LIST_KEYS) {
            lines.add(key + ":");
            lines.add("-");
            lines.add("-");
        }
        lines.add("");
        lines.add("auto_drop_to_inventory:");
        lines.add("  enabled: true");
        lines.add("  blocks: []");
        lines.add("");
        Files.write(blocksFile.toPath(), lines, StandardCharsets.UTF_8);
    }

    @Deprecated
    public void migrateLegacyBlocksIfNeeded() {}
}