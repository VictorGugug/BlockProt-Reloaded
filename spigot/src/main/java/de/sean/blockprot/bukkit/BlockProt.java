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

package de.sean.blockprot.bukkit;

import de.sean.blockprot.bukkit.audit.AuditLogger;
import de.sean.blockprot.bukkit.commands.BlockProtCommand;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.config.IntegrationConfig;
import de.sean.blockprot.bukkit.config.WorldsConfig;
import de.sean.blockprot.bukkit.integrations.*;
import de.sean.blockprot.bukkit.listeners.*;
import de.sean.blockprot.bukkit.metrics.IntegrationBarChart;
import de.sean.blockprot.bukkit.nbt.StatHandler;

import de.sean.blockprot.bukkit.storage.HybridDatabase;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import de.sean.blockprot.bukkit.tasks.ConfigFileWatcher;
import de.sean.blockprot.bukkit.tasks.BackupTask;
import de.sean.blockprot.bukkit.tasks.InactivityCleanupTask;
import de.sean.blockprot.bukkit.tasks.WorldExpiryTask;
import de.sean.blockprot.bukkit.tasks.UpdateChecker;
import com.tcoded.folialib.FoliaLib;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.enginehub.squirrelid.cache.ProfileCache;
import org.enginehub.squirrelid.cache.SQLiteCache;
import org.enginehub.squirrelid.resolver.ProfileService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.Objects;

public final class BlockProt extends JavaPlugin {

    public static final int pluginId = 31548; // BlockProt Reloaded on bStats
    public static final String defaultLanguageFile = "translations_en.yml";

    @Nullable private static BlockProt instance;
    @Nullable private static DefaultConfig defaultConfig = null;

    private final ArrayList<PluginIntegration> integrations = new ArrayList<>();

    @Nullable private static SQLiteCache playerProfileCache = null;
    @Nullable private static ProfileService playerProfileService = null;
    @Nullable private static WorldsConfig worldsConfig = null;
    @Nullable private static AuditLogger auditLogger = null;
    @Nullable private static HybridDatabase hybridDatabase = null;
    @Nullable private ConfigFileWatcher fileWatcher = null;

    // State carried from migrateFromLegacyFolders() to flushMigrationLog().
    // Translator is not ready during migration, so messages are deferred.
    @Nullable private String  pendingMigrationLog     = null;
    private          boolean  pendingMigrationSuccess = false;
    @Nullable private String  pendingMigrationError   = null;

    @Nullable private static String pluginVersion = null;
    @Nullable private static List<String> pluginAuthors = null;

    @Nullable private static FoliaLib foliaLib = null;

    private Metrics metrics;

    /** True when this server session is the very first start of BlockProt Reloaded. */
    private static boolean firstStartThisSession = false;

    /**
     * Returns true for the entire first startup session.
     * Useful for showing guides in JoinEventListener.
     */
    public static boolean isFirstStartThisSession() {
        return firstStartThisSession;
    }

    @NotNull
    public static FoliaLib getFoliaLib() {
        assert foliaLib != null;
        return foliaLib;
    }

    @NotNull
    public static BlockProt getInstance() {
        assert instance != null;
        return instance;
    }

    @NotNull
    public static DefaultConfig getDefaultConfig() throws AssertionError {
        assert defaultConfig != null : "default config should not be null.";
        return defaultConfig;
    }

    @Nullable
    public ConfigFileWatcher getFileWatcher() {
        return fileWatcher;
    }

    public List<PluginIntegration> getIntegrations() {
        return Collections.unmodifiableList(integrations);
    }

    private boolean integrationsLoaded = false;
    private boolean integrationsEnabled = false;

    @NotNull public static String getPluginVersion() { return pluginVersion != null ? pluginVersion : ""; }
    @NotNull public static List<String> getPluginAuthors() { return pluginAuthors != null ? pluginAuthors : List.of(); }

    @NotNull public static ProfileCache    getProfileCache()   { assert playerProfileCache   != null; return playerProfileCache; }
    @NotNull public static ProfileService  getProfileService() { assert playerProfileService != null; return playerProfileService; }
    @Nullable public static WorldsConfig   getWorldsConfig()   { return worldsConfig; }
    @Nullable public static AuditLogger    getAuditLogger()    { return auditLogger; }
    @Nullable public static HybridDatabase getHybridDatabase() { return hybridDatabase; }

    @Override
    @SuppressWarnings("deprecation")
    public void onLoad() {
        instance = this;
        pluginVersion = this.getDescription().getVersion();
        pluginAuthors = this.getDescription().getAuthors();
        try {
            // Store the usercache inside the plugin data folder: not next to server.jar.
            File cacheFile = new File(this.getDataFolder(), "blockprot_usercache.sqlite");
            if (!cacheFile.getParentFile().exists()) cacheFile.getParentFile().mkdirs();
            playerProfileCache   = new SQLiteCache(cacheFile);
            playerProfileService = new CachedProfileService(playerProfileCache);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open SQLite connection to usercache database", e);
        }
        try { registerIntegration(new TownyIntegration());          } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new PlaceholderAPIIntegration()); } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new ViaVersionIntegration());     } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new WorldGuardIntegration());     } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new LandsPluginIntegration());    } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new ClaimChunkIntegration());    } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new ResidenceIntegration());     } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new GriefPreventionIntegration()); } catch (NoClassDefFoundError ignored) {}
        for (PluginIntegration integration : integrations) {
            try { integration.load(); } catch (NoClassDefFoundError ignored) {}
        }
        integrationsLoaded = true;
    }

    @Override
    public void onEnable() {
        if (isRunningCraftBukkit()) {
            this.saveDefaultConfig();
            this.reloadConfig();
            defaultConfig = new DefaultConfig(this.getConfig(), this.getDataFolder());
            Translator.resetTranslations();
            try {
                InputStream s = this.getResource("lang/" + defaultLanguageFile);
                if (s != null) {
                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(
                        new java.io.BufferedReader(new java.io.InputStreamReader(s, StandardCharsets.UTF_8)));
                    Translator.loadFromConfigs(cfg, cfg);
                }
            } catch (Exception ignored) {}
            final var message = Translator.get(TranslationKey.CONSOLE__CRAFTBUKKIT_UNSUPPORTED);
            getLogger().severe(message);
            getServer().getPluginManager().registerEvents(new ErrorEventListener(message), this);
            return;
        }

        migrateFromLegacyFolders();
        migrateLegacySqliteFile();

        BlockProtConsole.beginStartup(this.getLogger());

        foliaLib = new FoliaLib(this);
        foliaLib.getScheduler().runAsync(task -> new UpdateChecker(BlockProt.getPluginVersion()).run());
        MinecraftVersion.disableUpdateCheck();
        this.saveDefaultConfig();
        migrateOldLockableListsFromConfigYml();
        saveResourceSilent("blocks.yml", false);
        this.reloadConfigAndTranslations();
        this.flushMigrationLog();

        boolean sessionLogEnabled = defaultConfig.isSessionLogEnabled();
        BlockProtLogger.init(this.getDataFolder(), sessionLogEnabled);
        String version = BlockProt.getPluginVersion();

        BlockProtLogger.log("=== Startup: BlockProt v" + version + " ===");
        BlockProtLogger.log("Server: " + Bukkit.getVersion());
        BlockProtLogger.log("Runtime: " + VersionCompat.getDiagnosticString());
        if (VersionCompat.is26Family()) {
            BlockProtLogger.log("Version scheme: " + VersionCompat.MAJOR + ".x year-based detected.");
        }

        BlockProtConsole.boot(
            Translator.get(TranslationKey.CONSOLE__BOOT_CONFIGURATION),
            Translator.get(TranslationKey.CONSOLE__BOOT_LOADED));

        StatHandler.enable();

        boolean hasUpgradeData = BackupTask.hasPriorData(this.getDataFolder());
        if (hasUpgradeData && defaultConfig.isBackupsEnabled()) {
            new BackupTask(this.getDataFolder()).run();
        }
        saveResourceSilent("worlds.yml", false);
        if (defaultConfig.isPerWorldsConfigEnabled()) {
            File worldsFile = new File(this.getDataFolder(), "worlds.yml");
            YamlConfiguration worldsDisk = WorldsConfig.scanAndPopulate(worldsFile, this.getConfig(), this.getLogger());
            worldsConfig = new WorldsConfig(worldsDisk);
        }

        hybridDatabase = new HybridDatabase(this);
        hybridDatabase.start(defaultConfig);

        try {
            auditLogger = new AuditLogger(this.getDataFolder());
        } catch (Exception e) {
            BlockProtConsole.warn(Translator.get(TranslationKey.CONSOLE__AUDIT_LOGGER_FAILED)
                .replace("{error}", e.getMessage()));
        }

        fileWatcher = new ConfigFileWatcher(this);
        if (defaultConfig.isAutoReloadEnabled()) {
            fileWatcher.start();
        }

        int inactivityDays = this.getConfig().getInt("inactivity_cleanup_days", -1);
        if (inactivityDays > 0) {
            foliaLib.getScheduler().runAsync(task -> new InactivityCleanupTask(inactivityDays).run());
        }

        if (defaultConfig.isWorldExpiryEnabled()) {
            int interval = Math.max(1, defaultConfig.getWorldExpiryCheckInterval());
            new WorldExpiryTask().runTaskTimer(this, 0L, interval * 60L * 20L);
        }

        metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new IntegrationBarChart());

        final PluginManager pm = getServer().getPluginManager();
        registerEvent(pm, new BlockEventListener(this));
        registerEvent(pm, new EntityEventListener());
        registerEvent(pm, new ExplodeEventListener());
        registerEvent(pm, new HopperEventListener());
        registerEvent(pm, new InteractEventListener());
        registerEvent(pm, new InventoryEventListener());
        registerEvent(pm, new JoinEventListener());
        registerEvent(pm, new PistonEventListener());
        registerEvent(pm, new RedstoneEventListener());
        registerEvent(pm, new LockEffectListener());
        registerEvent(pm, new EntityProtectionListener());
        registerEvent(pm, new EntityMenuOpenListener());
        registerEvent(pm, new VillagerWorkstationProtectionListener());
        registerEvent(pm, new ItemFrameListener());
        registerEvent(pm, new VehicleProtectionListener());
        registerEvent(pm, new RaidDetectionListener());
        registerEvent(pm, new WorldEditPasteListener(this));

        BlockProtConsole.boot(
            Translator.get(TranslationKey.CONSOLE__BOOT_LISTENERS),
            Translator.get(TranslationKey.CONSOLE__BOOT_REGISTERED));

        Objects.requireNonNull(this.getCommand("blockprot"))
            .setExecutor(new BlockProtCommand());

        BlockProtConsole.boot(
            Translator.get(TranslationKey.CONSOLE__BOOT_COMMANDS),
            Translator.get(TranslationKey.CONSOLE__BOOT_REGISTERED));

        for (PluginIntegration integration : integrations) {
            try {
                integration.enable();
                if (integration.isEnabled()) {
                    BlockProtConsole.boot(integration.name,
                        Translator.get(TranslationKey.CONSOLE__BOOT_HOOKED));
                } else {
                    BlockProtConsole.bootMuted(integration.name,
                        Translator.get(TranslationKey.CONSOLE__BOOT_NOT_INSTALLED));
                }
            } catch (NoClassDefFoundError ignored) {
                BlockProtConsole.bootMuted(integration.name,
                    Translator.get(TranslationKey.CONSOLE__BOOT_NOT_INSTALLED));
            }
        }
        integrationsEnabled = true;
        new BlockProtAPI(this);

        foliaLib.getScheduler().runAsync(task -> populateProtectedBlockCache());

        BlockProtConsole.bootLast(
            Translator.get(TranslationKey.CONSOLE__BOOT_STARTUP_TIME),
            java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() + " ms");

        // First-run guide: buffered so it prints directly under the banner, before the boot checklist.
        if (isFirstStart() && !defaultConfig.hasConfiguredBlocks()) {
            String guideHeader = Translator.get(TranslationKey.CONSOLE__FIRST_START__HEADER);
            String guideTitle  = Translator.get(TranslationKey.CONSOLE__FIRST_START__TITLE);
            String guideStep1  = Translator.get(TranslationKey.CONSOLE__FIRST_START__STEP1)
                .replace("{command}", "/bp lockables");
            String guideStep2  = Translator.get(TranslationKey.CONSOLE__FIRST_START__STEP2)
                .replace("{file}", "blocks.yml");
            String guideStep3  = Translator.get(TranslationKey.CONSOLE__FIRST_START__STEP3)
                .replace("{url}", "https://github.com/VictorGugug/BlockProt-Reloaded");
            String guideStep4  = Translator.get(TranslationKey.CONSOLE__FIRST_START__STEP4)
                .replace("{command}", "/bp recommended");
            String guideFooter = Translator.get(TranslationKey.CONSOLE__FIRST_START__FOOTER);

            BlockProtLogger.log("startup", guideTitle);
            BlockProtLogger.log("startup", guideStep1);
            BlockProtLogger.log("startup", guideStep2);
            BlockProtLogger.log("startup", guideStep3);
            BlockProtLogger.log("startup", guideStep4);

            BlockProtConsole.guide(guideHeader);
            BlockProtConsole.guide(guideTitle);
            BlockProtConsole.guide(guideStep1);
            BlockProtConsole.guide(guideStep2);
            BlockProtConsole.guide(guideStep3);
            BlockProtConsole.guide(guideStep4);
            BlockProtConsole.guide(guideFooter);
            markFirstStartDone();
        }

        BlockProtConsole.printStartupBanner(version);

        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (!isRunningCraftBukkit()) {
            BlockProtConsole.info(Translator.get(TranslationKey.CONSOLE__SAVING_STATISTICS));
            StatHandler.disable();
            getServer().getOnlinePlayers().forEach(HumanEntity::closeInventory);
        }

        if (fileWatcher    != null) fileWatcher.stop();
        if (auditLogger    != null) auditLogger.close();
        if (hybridDatabase != null) hybridDatabase.close();
        BlockProtLogger.close();
        super.onDisable();
    }

    public void reloadConfigAndTranslations() {
        if (fileWatcher != null) fileWatcher.suppressNext();
        this.cleanLegacyConfigKeys();
        this.mergeMissingConfigKeys();
        this.mergeMissingBlocksKeys();
        this.convertBlocksFormatIfNeeded();
        saveResourceSilent("mysql/mysql.yml", false);
        saveResourceSilent("worlds.yml", false);
        this.reloadConfig();
        defaultConfig = new DefaultConfig(this.getConfig(), this.getDataFolder());

        Translator.resetTranslations();
        Translator.DEFAULT_FALLBACK = defaultConfig.getTranslationFallbackString();

        final String langFolder = "lang/";
        for (String resource : Translator.DEFAULT_TRANSLATION_FILES) {
            File diskFile = new File(this.getDataFolder(), langFolder + resource);
            if (!diskFile.exists()) {
                this.saveResource(langFolder + resource, false);
            } else {
                mergeMissingLangKeys(langFolder, resource, diskFile);
            }
        }

        InputStream defaultLanguageStream = this.getResource(langFolder + defaultLanguageFile);
        if (defaultLanguageStream == null) {
            throw new RuntimeException("Failed to load the default language file. The plugin JAR may be corrupt.");
        }
        YamlConfiguration defaultLanguageConfig = YamlConfiguration.loadConfiguration(
            new BufferedReader(new InputStreamReader(defaultLanguageStream, StandardCharsets.UTF_8)));

        final String fileName = defaultConfig.getLanguageFile() == null
            ? defaultLanguageFile : defaultConfig.getLanguageFile();
        YamlConfiguration wantedConfig = saveAndLoadConfigFile(
            langFolder, fileName, BlockProt.defaultConfig.shouldReplaceTranslations());
        Translator.loadFromConfigs(defaultLanguageConfig, wantedConfig);

        if (defaultConfig.isPerWorldsConfigEnabled()) {
            File worldsFile = new File(this.getDataFolder(), "worlds.yml");
            YamlConfiguration worldsDisk = WorldsConfig.scanAndPopulate(worldsFile, this.getConfig(), this.getLogger());
            worldsConfig = new WorldsConfig(worldsDisk);
        } else {
            worldsConfig = null;
        }

        IntegrationConfig.reload();
        for (PluginIntegration integration : integrations) {
            integration.reload();
        }

        if (fileWatcher != null) {
            boolean shouldRun = defaultConfig.isAutoReloadEnabled();
            boolean isRunning = fileWatcher.isRunning();
            if (shouldRun && !isRunning) {
                fileWatcher.start();
            } else if (!shouldRun && isRunning) {
                fileWatcher.stop();
                BlockProtConsole.info(Translator.get(TranslationKey.CONSOLE__AUTO_RELOAD_DISABLED));
            }
        }
    }

    private void registerEvent(@NotNull PluginManager pm, Listener listener) {
        pm.registerEvents(listener, this);
    }

    void registerIntegration(@NotNull PluginIntegration integration) {
        this.integrations.add(integration);
        BlockProtLogger.log("integration", "Registered: " + integration.name);
        try {
            if (integrationsLoaded) {
                integration.load();
            }
        } catch (NoClassDefFoundError ignored) {}
        try {
            if (integrationsEnabled) {
                integration.enable();
            }
        } catch (NoClassDefFoundError ignored) {}
    }

    @Nullable
    public Plugin getPlugin(String pluginName) {
        return this.getServer().getPluginManager().getPlugin(pluginName);
    }

    @NotNull
    public YamlConfiguration saveAndLoadConfigFile(String folder, String name, boolean replace) {
        final String path = folder + (folder.endsWith("/") ? "" : "/") + name;
        File file = new File(this.getDataFolder(), path);
        if (!file.exists()) {
            try {
                this.saveResource(path, replace);
            } catch (IllegalArgumentException e) {
                // Only warn when it's a lang file; integration configs are optional.
                if (!folder.startsWith("integrations")) {
                    getLogger().warning(Translator.get(TranslationKey.CONSOLE__CONFIG_LANGUAGE_MISSING)
                        .replace("{file}", name));
                }
                return new YamlConfiguration();
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void mergeMissingLangKeys(@NotNull String langFolder, @NotNull String resource, @NotNull File diskFile) {
        InputStream jarStream = this.getResource(langFolder + resource);
        if (jarStream == null) return;

        YamlConfiguration jarConfig  = YamlConfiguration.loadConfiguration(new BufferedReader(new InputStreamReader(jarStream, StandardCharsets.UTF_8)));
        YamlConfiguration diskConfig = YamlConfiguration.loadConfiguration(diskFile);

        int added = 0;
        for (String key : jarConfig.getKeys(true)) {
            if (jarConfig.isConfigurationSection(key)) continue;
            if (EXTERNAL_CONFIG_KEYS.contains(key)) continue;
            if (!diskConfig.contains(key)) {
                diskConfig.set(key, jarConfig.get(key));
                added++;
                BlockProtLogger.log("lang-merge", resource + ": added missing key: " + key);
            }
        }

        if (added > 0) {
            try {
                diskConfig.save(diskFile);
                BlockProtLogger.log("lang-merge", resource + ": added " + added + " missing key(s).");
            } catch (IOException e) {
                BlockProtConsole.warn(Translator.get(TranslationKey.CONSOLE__LANG_KEYS_SAVE_FAILED)
                    .replace("{file}", resource).replace("{error}", e.getMessage()));
            }
        }
    }

    private void cleanLegacyConfigKeys() {
        File diskFile = new File(this.getDataFolder(), "config.yml");
        if (!diskFile.exists()) return;

        YamlConfiguration userValues = YamlConfiguration.loadConfiguration(diskFile);

        InputStream jarStream = this.getResource("config.yml");
        if (jarStream == null) return;
        YamlConfiguration template = YamlConfiguration.loadConfiguration(
            new BufferedReader(new InputStreamReader(jarStream, StandardCharsets.UTF_8)));

        boolean dirty = false;

        // worlds_config_enabled -> per_worlds_config (renamed for clarity)
        if (userValues.contains("worlds_config_enabled") && !userValues.contains("per_worlds_config")) {
            userValues.set("per_worlds_config", userValues.getBoolean("worlds_config_enabled", false));
            userValues.set("worlds_config_enabled", null);
            dirty = true;
            BlockProtLogger.log("config-migrate", "Migrated 'worlds_config_enabled' -> 'per_worlds_config'");
        }
        if (userValues.contains("pet_protection") && !userValues.contains("entity_protection")) {
            userValues.set("entity_protection.enabled", userValues.getBoolean("pet_protection.enabled", false));
            userValues.set("entity_protection.auto_protect_on_tame",
                userValues.getBoolean("pet_protection.auto_protect_on_tame", true));
            userValues.set("entity_protection.menu_item",
                userValues.getString("pet_protection.menu_item", "STICK"));
            userValues.set("entity_protection.villager_locate_seconds",
                userValues.getInt("pet_protection.villager_locate_seconds", 6));
            userValues.set("pet_protection", null);
            dirty = true;
            BlockProtLogger.log("config-migrate", "Migrated 'pet_protection' -> 'entity_protection'");
        }

        int added = 0;
        for (String key : template.getKeys(true)) {
            if (template.isConfigurationSection(key)) continue;
            if (EXTERNAL_CONFIG_KEYS.contains(key)) continue;
            if (!userValues.contains(key)) {
                userValues.set(key, template.get(key));
                added++;
                dirty = true;
            }
        }

        if (!dirty) return;
        try {
            if (fileWatcher != null) fileWatcher.suppressNext();
            userValues.save(diskFile);
            BlockProtLogger.log("config-clean",
                "config.yml merged: all user values preserved" +
                (added > 0 ? ", added " + added + " missing key(s) from template." : "."));
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to save config.yml after merge: " + e.getMessage());
        }
    }

    /** Keys managed by separate files - never merged back into config.yml. */
    private static final Set<String> EXTERNAL_CONFIG_KEYS = Set.of(
        "lockable_tile_entities", "lockable_shulker_boxes", "lockable_blocks", "lockable_doors",
        "lockable_entities", "auto_drop_to_inventory",
        "mysql.enabled", "mysql.host", "mysql.port", "mysql.database",
        "mysql.username", "mysql.password", "mysql.jdbc_url",
        "mysql.pool.maximum_pool_size", "mysql.pool.minimum_idle", "mysql.pool.connection_timeout_ms",
        "console.prefix_color", "console.info_color"
    );

    private void mergeMissingBlocksKeys() {
        String blocksPath = this.getConfig().getString("blocks_file", "blocks.yml");
        File diskFile = new File(this.getDataFolder(), blocksPath);
        if (!diskFile.exists()) return;
        InputStream jarStream = this.getResource("blocks.yml");
        if (jarStream == null) return;

        YamlConfiguration jarConfig = YamlConfiguration.loadConfiguration(new BufferedReader(new InputStreamReader(jarStream, StandardCharsets.UTF_8)));

        YamlConfiguration diskConfig = new YamlConfiguration();
        try {
            diskConfig.load(diskFile);
        } catch (IOException | InvalidConfigurationException ex) {
            handleCorruptBlocksYaml(diskFile, jarConfig, ex);
            return;
        }
        this.processBlocksMerge(diskFile, jarConfig, diskConfig);
    }

    private void processBlocksMerge(@NotNull File diskFile, @NotNull YamlConfiguration jarConfig, @NotNull YamlConfiguration diskConfig) {
        int added = 0;

        for (String key : jarConfig.getKeys(false)) {
            if (!diskConfig.contains(key)) {
                diskConfig.set(key, jarConfig.get(key));
                added++;
                BlockProtLogger.log("blocks-merge", "blocks.yml: added missing section " + key);
            }
        }

        // Clean up any placeholder entries (blank lines, "[]", "[ ]") that ended up
        // mixed in with real material names (e.g. after a broken merge). A list made
        // up entirely of placeholders is left untouched: that is the shipped hint
        // format telling the admin where to add block names, not corruption.
        boolean cleaned = false;
        String[] listKeys = {"lockable_tile_entities", "lockable_shulker_boxes",
            "lockable_blocks", "lockable_doors", "lockable_entities",
            "auto_drop_to_inventory.blocks"};
        for (String key : listKeys) {
            if (!diskConfig.contains(key)) continue;
            List<?> raw = diskConfig.getList(key);
            if (raw == null || raw.isEmpty()) continue;

            boolean hasRealEntry = false;
            for (Object o : raw) {
                if (isPlaceholderEntry(o)) continue;
                hasRealEntry = true;
                break;
            }
            if (!hasRealEntry) continue;

            List<Object> filtered = new ArrayList<>();
            for (Object o : raw) {
                if (isPlaceholderEntry(o)) continue;
                filtered.add(o);
            }
            if (filtered.size() != raw.size()) {
                diskConfig.set(key, filtered);
                cleaned = true;
                BlockProtLogger.log("blocks-clean", "Removed placeholder entries from " + key);
            }
        }

        if (added == 0 && !cleaned) return;
        try {
            if (fileWatcher != null) fileWatcher.suppressNext();
            boolean modern = this.getConfig().getBoolean("modern_family_blocks", false);
            de.sean.blockprot.bukkit.config.DefaultConfig.sanitizeBlocksListsForSave(diskConfig, modern);
            diskConfig = de.sean.blockprot.bukkit.config.DefaultConfig.reorderBlocksKeys(diskConfig);
            diskConfig.save(diskFile);
            de.sean.blockprot.bukkit.config.DefaultConfig.prependBlocksHeader(diskFile);
            if (added > 0) {
                BlockProtLogger.log("blocks-merge", "blocks.yml: merged " + added + " new section(s) from JAR.");
            }
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to save blocks.yml after merge: " + e.getMessage());
        }
    }

    /**
     * True for a blocks.yml list entry that carries no material name. Delegates to
     * {@link DefaultConfig#isPlaceholderEntry(Object)}, the canonical implementation.
     */
    private static boolean isPlaceholderEntry(@Nullable Object o) {
        return DefaultConfig.isPlaceholderEntry(o);
    }

    /**
     * Handles a corrupted blocks.yml: logs a CRITICAL SEVERE entry with the error line,
     * attempts line-by-line recovery, merges any jar defaults that are still missing,
     * saves the result, and logs whether the repair succeeded or failed.
     * If recovery is impossible, directs the admin to file a bug report.
     */
    private void handleCorruptBlocksYaml(@NotNull File diskFile, @NotNull YamlConfiguration jarConfig, @NotNull Exception ex) {
        int errorLine = getErrorLine(ex.getMessage());
        String lineInfo = errorLine > 0 ? " at line " + errorLine : "";
        getLogger().severe("[BlockProt] CRITICAL: blocks.yml is corrupted" + lineInfo + ".");
        getLogger().severe("[BlockProt] Cause: " + ex.getMessage());
        getLogger().severe("[BlockProt] Attempting intelligent repair...");
        BlockProtLogger.log("yaml-repair", "CRITICAL: blocks.yml corrupted" + lineInfo + ". Cause: " + ex.getMessage());

        YamlConfiguration recovered = attemptYamlRecovery(diskFile, jarConfig);
        if (recovered == null) {
            getLogger().severe("[BlockProt] Repair FAILED: unable to recover any data from blocks.yml.");
            getLogger().severe("[BlockProt] Manually restore blocks.yml from backup. Report this bug at: https://github.com/VictorGugug/BlockProt-Reloaded/issues");
            BlockProtLogger.log("yaml-repair", "Repair FAILED. No data could be recovered. Report as bug: https://github.com/VictorGugug/BlockProt-Reloaded/issues");
            return;
        }

        for (String key : jarConfig.getKeys(false)) {
            if (!recovered.contains(key)) recovered.set(key, jarConfig.get(key));
        }

        try {
            if (fileWatcher != null) fileWatcher.suppressNext();
            boolean modern = this.getConfig().getBoolean("modern_family_blocks", false);
            DefaultConfig.sanitizeBlocksListsForSave(recovered, modern);
            recovered = DefaultConfig.reorderBlocksKeys(recovered);
            recovered.save(diskFile);
            DefaultConfig.prependBlocksHeader(diskFile);
            getLogger().warning("[BlockProt] blocks.yml repaired" + lineInfo + ". Corrupted file saved as blocks.yml.corrupted.*. Verify blocks.yml content.");
            BlockProtLogger.log("yaml-repair", "Repair SUCCESS" + lineInfo + ". Corrupted file backed up as blocks.yml.corrupted.*");
        } catch (IOException e) {
            getLogger().severe("[BlockProt] Repair FAILED: could not write repaired blocks.yml: " + e.getMessage());
            getLogger().severe("[BlockProt] Manually fix blocks.yml. Report this bug at: https://github.com/VictorGugug/BlockProt-Reloaded/issues");
            BlockProtLogger.log("yaml-repair", "Repair FAILED: write error: " + e.getMessage() + ". Report as bug.");
        }
    }

    /**
     * Extracts the line number from a YAML error message.
     * Format example: "while parsing a block collection in 'reader', line 2, column 1"
     */
    private int getErrorLine(@Nullable String errorMsg) {
        if (errorMsg == null) return -1;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("line (\\d+)");
        java.util.regex.Matcher m = p.matcher(errorMsg);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /**
     * Attempts to recover valid content from a corrupted blocks.yml file.
     * Reads the file line by line, extracting valid list entries for known keys.
     * Creates a backup of the corrupted file before any modifications.
     */
    @Nullable
    private YamlConfiguration attemptYamlRecovery(@NotNull File corruptedFile, @NotNull YamlConfiguration jarTemplate) {
        backupCorruptedFile(corruptedFile);

        YamlConfiguration recovered = new YamlConfiguration();
        recovered.set("lockable_tile_entities", List.of());
        recovered.set("lockable_shulker_boxes", List.of());
        recovered.set("lockable_blocks", List.of());
        recovered.set("lockable_doors", List.of());
        recovered.set("lockable_entities", List.of());
        recovered.set("auto_drop_to_inventory.enabled", true);
        recovered.set("auto_drop_to_inventory.blocks", List.of());

        String[] listKeys = {"lockable_tile_entities", "lockable_shulker_boxes",
            "lockable_blocks", "lockable_doors", "lockable_entities",
            "auto_drop_to_inventory.blocks"};

        try {
            List<String> lines = Files.readAllLines(corruptedFile.toPath(), StandardCharsets.UTF_8);
            String currentKey = null;
            boolean inAutoDropSection = false;
            List<String> recoveredValues = new ArrayList<>();

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String trimmed = line.trim();

                for (String key : listKeys) {
                    if (trimmed.equals(key + ":") || trimmed.startsWith(key + ":")) {
                        currentKey = key;
                        inAutoDropSection = key.startsWith("auto_drop_to_inventory");
                        if (!trimmed.endsWith(":") || trimmed.length() > key.length() + 1) {
                            String inlineValue = trimmed.substring(trimmed.indexOf(':') + 1).trim();
                            if (!inlineValue.isEmpty() && !inlineValue.equals("[]")) {
                                recoveredValues.add(inlineValue);
                            }
                        }
                        break;
                    }
                }

                if (currentKey != null && (trimmed.startsWith("- ") || (!trimmed.isEmpty() && !trimmed.contains(":")))) {
                    String value = trimmed.startsWith("- ") ? trimmed.substring(2).trim() : trimmed;
                    value = value.replaceAll("^\"|\"$", "");
                    value = value.replaceAll("^'|\'$", "");
                    if (!value.isEmpty() && !value.equals("[]") && !value.equals("-")) {
                        recoveredValues.add(value);
                    }
                }

                boolean isNewKey = false;
                for (String key : jarTemplate.getKeys(false)) {
                    if (trimmed.equals(key + ":")) {
                        isNewKey = true;
                        break;
                    }
                }
                if (isNewKey || (trimmed.contains(":") && !trimmed.startsWith("-") && !trimmed.startsWith("#"))) {
                    if (!recoveredValues.isEmpty() && currentKey != null) {
                        recovered.set(currentKey, new ArrayList<>(recoveredValues));
                        BlockProtLogger.log("blocks-recover", "Recovered " + recoveredValues.size() + " entries for: " + currentKey);
                        recoveredValues.clear();
                    }
                    currentKey = null;
                    inAutoDropSection = false;
                }
            }

            if (!recoveredValues.isEmpty() && currentKey != null) {
                recovered.set(currentKey, new ArrayList<>(recoveredValues));
                BlockProtLogger.log("blocks-recover", "Recovered " + recoveredValues.size() + " entries for: " + currentKey);
            }

            return recovered;
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to read corrupted blocks.yml for recovery: " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a backup of a corrupted config file with timestamp suffix.
     */
    private void backupCorruptedFile(@NotNull File originalFile) {
        try {
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());
            File backup = new File(originalFile.getParentFile(), originalFile.getName() + ".corrupted." + timestamp);
            Files.copy(originalFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            getLogger().warning("[BlockProt] Corrupted blocks.yml backed up as: " + backup.getName());
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to backup corrupted blocks.yml: " + e.getMessage());
        }
    }

    private boolean convertConfigurationListsIfNeeded(@NotNull YamlConfiguration cfg, @NotNull String prefix, boolean modern) {
        Map<String, BlockFamilyParser.Family> keyFamilies = new LinkedHashMap<>();
        keyFamilies.put("lockable_tile_entities", BlockFamilyParser.Family.TILE_ENTITIES);
        keyFamilies.put("lockable_shulker_boxes", BlockFamilyParser.Family.SHULKER_BOXES);
        keyFamilies.put("lockable_blocks", BlockFamilyParser.Family.BLOCKS);
        keyFamilies.put("lockable_doors", BlockFamilyParser.Family.DOORS);
        keyFamilies.put("lockable_entities", BlockFamilyParser.Family.ENTITIES);

        boolean needsConversion = false;
        for (String key : keyFamilies.keySet()) {
            String fullKey = prefix + key;
            if (!cfg.contains(fullKey)) continue;
            Object raw = cfg.get(fullKey);
            if (!(raw instanceof List<?> list)) continue;

            boolean hasRealEntry = false;
            for (Object o : list) {
                if (isPlaceholderEntry(o)) continue;
                hasRealEntry = true;
                if (o instanceof String s) {
                    boolean isExpr = BlockFamilyParser.isFamilyExpression(s.trim());
                    if (isExpr != modern) { needsConversion = true; break; }
                }
            }
            if (needsConversion) break;

            if (!hasRealEntry) {
                boolean alreadyNormalized = modern
                    ? (list.size() == 1 && "[]".equals(String.valueOf(list.get(0)).trim()))
                    : (list.size() == 2 && list.get(0) == null && list.get(1) == null);
                if (!alreadyNormalized) { needsConversion = true; break; }
            }
        }

        if (!needsConversion) return false;

        for (Map.Entry<String, BlockFamilyParser.Family> entry : keyFamilies.entrySet()) {
            String key = entry.getKey();
            String fullKey = prefix + key;
            BlockFamilyParser.Family family = entry.getValue();
            if (!cfg.contains(fullKey)) continue;

            Set<Material> materials = BlockFamilyParser.parse(cfg.get(fullKey), family);
            if (modern) {
                String expr = BlockFamilyParser.toFamilyExpression(materials, family);
                if (expr != null) {
                    cfg.set(fullKey, List.of(expr));
                } else {
                    cfg.set(fullKey, materials.stream().map(Material::name).sorted().toList());
                }
            } else {
                cfg.set(fullKey, materials.stream().map(Material::name).sorted().toList());
            }
        }
        return true;
    }

    private void convertBlocksFormatIfNeeded() {
        File configFile = new File(this.getDataFolder(), "config.yml");
        boolean modern = false;
        if (configFile.exists()) {
            modern = YamlConfiguration.loadConfiguration(configFile).getBoolean("modern_family_blocks", false);
        }

        // 1. Process blocks.yml
        String blocksPath = this.getConfig().getString("blocks_file", "blocks.yml");
        File blocksFile = new File(this.getDataFolder(), blocksPath);
        if (blocksFile.exists()) {
            YamlConfiguration blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
            boolean blocksDirty = convertConfigurationListsIfNeeded(blocksConfig, "", modern);

            // Special check for auto_drop_to_inventory.blocks
            if (blocksConfig.contains(AUTO_DROP_BLOCKS_KEY_LOCAL)) {
                Object raw = blocksConfig.get(AUTO_DROP_BLOCKS_KEY_LOCAL);
                if (raw instanceof List<?> list) {
                    boolean autoDropNeedsConversion = false;
                    if (!modern) {
                        for (Object o : list) {
                            if (o instanceof String s && BlockFamilyParser.isFamilyExpression(s.trim())) {
                                autoDropNeedsConversion = true;
                                break;
                            }
                        }
                    }

                    if (autoDropNeedsConversion) {
                        Set<Material> autoDropMaterials = new LinkedHashSet<>();
                        for (Object o : list) {
                            if (isPlaceholderEntry(o)) continue;
                            if (o instanceof String s) {
                                String trimmed = s.trim();
                                if (BlockFamilyParser.isFamilyExpression(trimmed)) {
                                    for (BlockFamilyParser.Family f : BlockFamilyParser.Family.values()) {
                                        autoDropMaterials.addAll(BlockFamilyParser.parseFamilyExpressionSilent(trimmed, f));
                                    }
                                } else {
                                    Material m = Material.matchMaterial(trimmed);
                                    if (m != null) autoDropMaterials.add(m);
                                }
                            }
                        }
                        blocksConfig.set(AUTO_DROP_BLOCKS_KEY_LOCAL,
                            autoDropMaterials.stream().map(Material::name).sorted().toList());
                        blocksDirty = true;
                    }
                }
            }

            if (blocksDirty) {
                try {
                    if (fileWatcher != null) fileWatcher.suppressNext();
                    de.sean.blockprot.bukkit.config.DefaultConfig.sanitizeListsForSave(blocksConfig, "", modern);
                    blocksConfig = de.sean.blockprot.bukkit.config.DefaultConfig.reorderBlocksKeys(blocksConfig);
                    blocksConfig.save(blocksFile);
                    de.sean.blockprot.bukkit.config.DefaultConfig.prependBlocksHeader(blocksFile);
                    BlockProtLogger.log("blocks-convert",
                        "blocks.yml: converted format to " + (modern ? "family expressions" : "flat names") + ".");
                } catch (IOException e) {
                    BlockProtLogger.warn("Failed to save blocks.yml after format conversion: " + e.getMessage());
                }
            }
        }

        // 2. Process worlds.yml
        File worldsFile = new File(this.getDataFolder(), "worlds.yml");
        if (worldsFile.exists()) {
            YamlConfiguration worldsConfig = YamlConfiguration.loadConfiguration(worldsFile);
            ConfigurationSection worldsSection = worldsConfig.getConfigurationSection("worlds");
            if (worldsSection != null) {
                boolean worldsDirty = false;
                for (String worldName : worldsSection.getKeys(false)) {
                    String prefix = "worlds." + worldName + ".";
                    if (convertConfigurationListsIfNeeded(worldsConfig, prefix, modern)) {
                        de.sean.blockprot.bukkit.config.DefaultConfig.sanitizeListsForSave(worldsConfig, prefix, modern);
                        worldsDirty = true;
                    }
                }
                if (worldsDirty) {
                    try {
                        if (fileWatcher != null) fileWatcher.suppressNext();
                        worldsConfig.save(worldsFile);
                        de.sean.blockprot.bukkit.config.DefaultConfig.cleanNullPlaceholderLines(worldsFile);
                        BlockProtLogger.log("worlds-convert",
                            "worlds.yml: converted format to " + (modern ? "family expressions" : "flat names") + ".");
                    } catch (IOException e) {
                        BlockProtLogger.warn("Failed to save worlds.yml after format conversion: " + e.getMessage());
                    }
                }
            }
        }
    }

    private static final String AUTO_DROP_BLOCKS_KEY_LOCAL = "auto_drop_to_inventory.blocks";

    private void mergeMissingConfigKeys() {
        File diskFile = new File(this.getDataFolder(), "config.yml");
        if (!diskFile.exists()) return;
        InputStream jarStream = this.getResource("config.yml");
        if (jarStream == null) return;

        YamlConfiguration jarConfig  = YamlConfiguration.loadConfiguration(new BufferedReader(new InputStreamReader(jarStream, StandardCharsets.UTF_8)));
        YamlConfiguration diskConfig = YamlConfiguration.loadConfiguration(diskFile);
        int added = 0;
        for (String key : jarConfig.getKeys(true)) {
            if (jarConfig.isConfigurationSection(key)) continue;
            if (EXTERNAL_CONFIG_KEYS.contains(key)) continue;
            if (!diskConfig.contains(key)) {
                diskConfig.set(key, jarConfig.get(key));
                added++;
                BlockProtLogger.log("config-merge", "config.yml: added missing key: " + key);
            }
        }

        if (added == 0) return;
        try {
            if (fileWatcher != null) fileWatcher.suppressNext();
            diskConfig.save(diskFile);
            BlockProtLogger.log("config-merge", "config.yml: added " + added + " missing option(s).");
        } catch (IOException e) {
            BlockProtConsole.warn(Translator.get(TranslationKey.CONSOLE__CONFIG_KEYS_SAVE_FAILED)
                .replace("{error}", e.getMessage()));
            BlockProtLogger.log("config-merge", "Failed to save config.yml after merge: " + e.getMessage());
        }
    }

    private void saveResourceSilent(@NotNull String name, boolean replace) {
        File dest = new File(this.getDataFolder(), name);
        if (!replace && dest.exists()) return; // already there, skip quietly
        try {
            this.saveResource(name, replace);
        } catch (Exception ignored) {}
    }

    /**
     * Populates the in-memory {@link ProtectedBlockCache} on startup.
     *
     * <p>When MySQL is available, iterates the block index returned by
     * {@link HybridDatabase#getBlockIndexByWorld(String)} for every loaded world.
     * Otherwise falls back to iterating all offline player stats entries.
     * Only blocks whose type is currently lockable are marked in the cache.
     */
    private void populateProtectedBlockCache() {
        ProtectedBlockCache.clear();
        int marked = 0;

        if (hybridDatabase != null && hybridDatabase.isEnabled()) {
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (org.bukkit.Location loc : hybridDatabase.getBlockIndexByWorld(world.getName())) {
                    org.bukkit.block.Block block = loc.getBlock();
                    if (BlockProt.getDefaultConfig().isLockable(block.getType())) {
                        ProtectedBlockCache.mark(block);
                        marked++;
                    }
                }
            }
        } else {
            // No MySQL: iterate all player stat entries to collect protected locations.
            if (de.sean.blockprot.bukkit.nbt.StatHandler.isLoaded()) {
                java.util.Set<org.bukkit.Location> seen = new java.util.HashSet<>();
                for (org.bukkit.OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                    de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic pbs =
                        new de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic();
                    de.sean.blockprot.bukkit.nbt.StatHandler.getStatisticByUuid(pbs, op.getUniqueId());
                    for (de.sean.blockprot.bukkit.nbt.stats.LocationListEntry ls : pbs.get()) {
                        try {
                            org.bukkit.Location loc = ls.get();
                            if (loc.getWorld() == null || !seen.add(loc)) continue;
                            org.bukkit.block.Block block = loc.getBlock();
                            if (BlockProt.getDefaultConfig().isLockable(block.getType())) {
                                ProtectedBlockCache.mark(block);
                                marked++;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        BlockProtLogger.log("protected-cache", "Populated ProtectedBlockCache with " + marked + " block(s).");
    }

    /**
     * Returns {@code true} when the server runtime is plain CraftBukkit without Spigot.
     * BlockProt requires Spigot (or Paper/Folia/etc.) and logs a fatal error when running
     * on CraftBukkit alone.
     */
    private boolean isRunningCraftBukkit() {
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }

    private void migrateFromLegacyFolders() {
        if (new File(this.getDataFolder(), "config.yml").exists()) return;

        final String[] legacyNames = {"BlockProt", "BlockProtPlus"};
        final File pluginsDir = this.getDataFolder().getParentFile();
        if (pluginsDir == null) return;

        for (String legacyName : legacyNames) {
            File legacyFolder = new File(pluginsDir, legacyName);
            if (!legacyFolder.isDirectory()) continue;
            if (new File(legacyFolder, ".migrated").exists()) continue;
            if (!new File(legacyFolder, "config.yml").exists()) continue;

            pendingMigrationLog = legacyName;
            try {
                // This preserves any files the new plugin already created (e.g. defaults).
                // config.yml is excluded here and rebuilt below from the current jar
                // template, so migrated servers get the up-to-date structure and
                // comments instead of a raw copy of the old file.
                copyDirectoryContents(legacyFolder.toPath(), this.getDataFolder().toPath());

                // Write the current config.yml template, then overlay every value
                // found in the legacy config.yml on top of it. This preserves the
                // admin's settings, including old-named keys and lockable block
                // lists still embedded in config.yml, while keeping the new
                // structure. Downstream cleanLegacyConfigKeys() and
                // migrateOldLockableListsFromConfigYml() finish the conversion.
                this.saveResource("config.yml", true);
                applyLegacyConfigValues(
                    new File(legacyFolder, "config.yml"),
                    new File(this.getDataFolder(), "config.yml")
                );

                File legacyLang = new File(legacyFolder, "lang");
                File newLang    = new File(this.getDataFolder(), "lang");
                if (legacyLang.isDirectory()) {
                    File[] langFiles = legacyLang.listFiles(
                        f -> f.isFile() && f.getName().endsWith(".yml"));
                    if (langFiles != null) {
                        for (File lf : langFiles) {
                            mergeYamlUserValues(lf, new File(newLang, lf.getName()));
                        }
                    }
                }

                Files.createFile(legacyFolder.toPath().resolve(".migrated"));
                pendingMigrationSuccess = true;
                // A migrated install is not a fresh install; skip the first-start guide.
                markFirstStartDone();
            } catch (IOException e) {
                pendingMigrationError = e.getMessage();
            }
            break;
        }
    }

    /**
     * Overlays every scalar value found in a legacy config.yml onto a freshly
     * written new-format config.yml, overwriting matching keys and adding
     * keys the new template does not have (old-named keys, embedded lockable
     * block lists). The new file's structure and comments are kept; only
     * values change. cleanLegacyConfigKeys() and migrateOldLockableListsFromConfigYml()
     * finish the conversion afterward.
     */
    private static void applyLegacyConfigValues(@NotNull File legacySrc, @NotNull File freshDst) throws IOException {
        if (!legacySrc.exists() || !freshDst.exists()) return;
        YamlConfiguration legacyCfg = YamlConfiguration.loadConfiguration(legacySrc);
        YamlConfiguration freshCfg  = YamlConfiguration.loadConfiguration(freshDst);
        boolean changed = false;
        for (String key : legacyCfg.getKeys(true)) {
            if (legacyCfg.isConfigurationSection(key)) continue;
            freshCfg.set(key, legacyCfg.get(key));
            changed = true;
        }
        if (changed) freshCfg.save(freshDst);
    }

    private static void mergeYamlUserValues(@NotNull File src, @NotNull File dst) throws IOException {
        if (!src.exists()) return;
        YamlConfiguration srcCfg = YamlConfiguration.loadConfiguration(src);
        if (!dst.exists()) {
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            return;
        }
        YamlConfiguration dstCfg = YamlConfiguration.loadConfiguration(dst);
        boolean changed = false;
        for (String key : srcCfg.getKeys(true)) {
            if (srcCfg.isConfigurationSection(key)) continue;
            if (!dstCfg.contains(key)) {
                dstCfg.set(key, srcCfg.get(key));
                changed = true;
            }
        }
        if (changed) dstCfg.save(dst);
    }

    private void flushMigrationLog() {
        if (pendingMigrationLog == null) return;
        String source = pendingMigrationLog;
        String target = this.getDataFolder().getName();
        if (pendingMigrationSuccess) {
            BlockProtConsole.info(Translator.get(TranslationKey.CONSOLE__MIGRATION_START)
                .replace("{source}", source).replace("{target}", target));
            BlockProtConsole.info(Translator.get(TranslationKey.CONSOLE__MIGRATION_DONE)
                .replace("{source}", source));
        } else {
            BlockProtConsole.warn(Translator.get(TranslationKey.CONSOLE__MIGRATION_FAILED)
                .replace("{source}", source)
                .replace("{error}", pendingMigrationError != null ? pendingMigrationError : "unknown"));
        }
        pendingMigrationLog     = null;
        pendingMigrationSuccess = false;
        pendingMigrationError   = null;
    }

    /**
     * Moves the legacy {@code blockprot_usercache.sqlite} from the server root
     * (next to {@code server.jar}) into the plugin data folder.
     * Runs once on startup; if the file has already been moved no-op.
     */
    private void migrateLegacySqliteFile() {
        File legacyFile = new File(Bukkit.getWorldContainer(), "blockprot_usercache.sqlite");
        if (!legacyFile.exists()) return;
        File newFile = new File(this.getDataFolder(), "blockprot_usercache.sqlite");
        if (newFile.exists()) {
            // Already present in new location: remove the orphan from server root.
            if (!legacyFile.delete())
                getLogger().warning("[BlockProt] Could not delete legacy blockprot_usercache.sqlite from server root.");
            return;
        }
        try {
            Files.move(legacyFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            getLogger().info("[BlockProt] Moved blockprot_usercache.sqlite into plugin data folder.");
        } catch (IOException e) {
            getLogger().warning("[BlockProt] Failed to move blockprot_usercache.sqlite: " + e.getMessage());
        }
    }

    /**
     * Migrates lockable block lists from the old config.yml format (pre-1.3.3)
     * to a dedicated blocks.yml file.
     *
     * <p>In BlockProt 1.3.2 and earlier, lockable lists were direct children
     * of config.yml (e.g. {@code lockable_tile_entities: [CHEST, ...]}).
     * Starting from 1.3.3 they live in blocks.yml.
     *
     * <p>This method runs once: if blocks.yml already exists, or if config.yml
     * has no lockable lists, it is a no-op.
     */
    private void migrateOldLockableListsFromConfigYml() {
        File configFile = new File(this.getDataFolder(), "config.yml");
        if (!configFile.exists()) return;

        File blocksFile = new File(this.getDataFolder(), "blocks.yml");
        if (blocksFile.exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);

        String[] lockableKeys = {
            "lockable_tile_entities", "lockable_shulker_boxes",
            "lockable_blocks", "lockable_doors", "lockable_entities"
        };

        boolean hasAny = false;
        for (String key : lockableKeys) {
            Object val = cfg.get(key);
            if (val instanceof List<?> list && !list.isEmpty()) { hasAny = true; break; }
        }
        if (!hasAny) return;

        YamlConfiguration blocks = new YamlConfiguration();
        for (String key : lockableKeys) {
            blocks.set(key, cfg.getList(key, Collections.emptyList()));
        }
        blocks.set("auto_drop_to_inventory.enabled",
            cfg.getBoolean("auto_drop_to_inventory.enabled", true));
        blocks.set("auto_drop_to_inventory.blocks",
            cfg.getList("auto_drop_to_inventory.blocks", Collections.emptyList()));

        try {
            blocks.save(blocksFile);
            BlockProtLogger.log("migrate-blocks",
                "Created blocks.yml with lockable lists migrated from config.yml.");

            boolean changed = false;
            for (String key : lockableKeys) {
                if (cfg.contains(key)) { cfg.set(key, null); changed = true; }
            }
            if (cfg.contains("auto_drop_to_inventory")) {
                cfg.set("auto_drop_to_inventory", null);
                changed = true;
            }
            if (changed) {
                cfg.save(configFile);
                BlockProtLogger.log("migrate-blocks",
                    "Cleaned up lockable lists from config.yml (now in blocks.yml).");
            }
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to migrate lockable lists from config.yml: " + e.getMessage());
        }
    }

    private static boolean isFirstStart() {
        if (firstStartThisSession) return true;
        File marker = new File(getInstance().getDataFolder(), ".first_start_done");
        firstStartThisSession = !marker.exists();
        return firstStartThisSession;
    }

    private static void markFirstStartDone() {
        try {
            new File(getInstance().getDataFolder(), ".first_start_done").createNewFile();
        } catch (IOException ignored) {}
    }

    private static void copyDirectoryContents(@NotNull Path src, @NotNull Path dst) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path target = dst.resolve(src.relativize(dir));
                if (!Files.exists(target)) Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Never copy the migration marker itself.
                if (file.getFileName().toString().equals(".migrated")) return FileVisitResult.CONTINUE;
                // config.yml is rebuilt separately in migrateFromLegacyFolders() from the
                // current jar template with legacy values overlaid, not copied raw.
                if (src.relativize(file).toString().equals("config.yml")) return FileVisitResult.CONTINUE;
                Path target = dst.resolve(src.relativize(file));
                if (!Files.exists(target)) {
                    Files.copy(file, target, StandardCopyOption.COPY_ATTRIBUTES);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}