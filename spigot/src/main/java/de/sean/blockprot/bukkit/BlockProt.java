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

package de.sean.blockprot.bukkit;

import de.sean.blockprot.bukkit.audit.AuditLogger;
import de.sean.blockprot.bukkit.commands.BlockProtCommand;
import de.sean.blockprot.bukkit.config.DefaultConfig;
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
import de.sean.blockprot.bukkit.tasks.UpdateChecker;
import com.tcoded.folialib.FoliaLib;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
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

/**
 * The main plugin instance of BlockProt.
 */
public final class BlockProt extends JavaPlugin {
    /**
     * bStats plugin ID for BlockProt Reloaded.
     *
     * To get your own ID:
     *  1. Go to https://bstats.org/getting-started
     *  2. Register a new plugin named "BlockProt Reloaded" (Bukkit)
     *  3. Replace the value below with the ID shown in your dashboard.
     *
     * The original BlockProt used ID 9999 — this fork must use a different
     * ID so stats appear under the correct project on bstats.org.
     */
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

    /** Plugin version string cached at startup — avoids repeated getDescription() calls. */
    @Nullable private static String pluginVersion = null;
    /** Plugin authors list cached at startup. */
    @Nullable private static List<String> pluginAuthors = null;

    /** Cross-platform scheduler (Spigot / Paper / Purpur / Pufferfish / Folia). */
    @Nullable private static FoliaLib foliaLib = null;

    private Metrics metrics;

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

    public List<PluginIntegration> getIntegrations() {
        return Collections.unmodifiableList(integrations);
    }

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
            playerProfileCache   = new SQLiteCache(new File(Bukkit.getWorldContainer(), "blockprot_usercache.sqlite"));
            playerProfileService = new CachedProfileService(playerProfileCache);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open SQLite connection to usercache database", e);
        }
        try { registerIntegration(new TownyIntegration());          } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new PlaceholderAPIIntegration()); } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new ViaVersionIntegration());     } catch (NoClassDefFoundError ignored) {}
        for (PluginIntegration integration : integrations) {
            try { integration.load(); } catch (NoClassDefFoundError ignored) {}
        }
    }

    @Override
    public void onEnable() {
        if (isRunningCraftBukkit()) {
            // Load translations minimally so we can emit a translated error message,
            // then register the error listener and abort the rest of onEnable.
            this.saveDefaultConfig();
            this.reloadConfig();
            defaultConfig = new DefaultConfig(this.getConfig(), this.getDataFolder());
            Translator.resetTranslations();
            try {
                InputStream s = this.getResource("lang/" + defaultLanguageFile);
                if (s != null) {
                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(
                        new java.io.BufferedReader(new java.io.InputStreamReader(s, java.nio.charset.StandardCharsets.UTF_8)));
                    Translator.loadFromConfigs(cfg, cfg);
                }
            } catch (Exception ignored) {}
            final var message = Translator.get(TranslationKey.CONSOLE__CRAFTBUKKIT_UNSUPPORTED);
            getLogger().severe(message);
            getServer().getPluginManager().registerEvents(new ErrorEventListener(message), this);
            return;
        }

        // Migrate data from legacy plugin folder names before anything else reads disk.
        migrateFromLegacyFolders();

        foliaLib = new FoliaLib(this);
        foliaLib.getScheduler().runAsync(task -> new UpdateChecker(BlockProt.getPluginVersion()).run());
        MinecraftVersion.disableUpdateCheck();
        this.cleanLegacyConfigKeys();
        this.saveDefaultConfig();
        saveResourceSilent("blocks.yml", false);
        saveResourceSilent("mysql/mysql.yml", false);
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

        BlockProtConsole.beginStartup(this.getLogger());
        StatHandler.enable();

        if (!BlockProt.getPluginVersion().equals(getConfig().getString("last_known_version", ""))) {
            getConfig().set("last_known_version", BlockProt.getPluginVersion());
            saveConfig();
        }

        boolean hasUpgradeData = BackupTask.hasPriorData(this.getDataFolder());
        if (hasUpgradeData && defaultConfig.isBackupsEnabled()) {
            new BackupTask(this.getDataFolder()).run();
        }
        this.mergeMissingConfigKeys();
        saveResourceSilent("worlds.yml", false);
        if (defaultConfig.isWorldsConfigEnabled()) {
            File worldsFile = new File(this.getDataFolder(), "worlds.yml");
            YamlConfiguration worldsDisk = WorldsConfig.scanAndPopulate(worldsFile, this.getConfig(), this.getLogger());
            worldsConfig = new WorldsConfig(worldsDisk);
        }

        hybridDatabase = new HybridDatabase(this);
        hybridDatabase.start(defaultConfig);

        try {
            auditLogger = new AuditLogger(this.getDataFolder());
            BlockProtLogger.log(Translator.get(TranslationKey.CONSOLE__AUDIT_LOGGER_STARTED));
        } catch (Exception e) {
            BlockProtConsole.warn(Translator.get(TranslationKey.CONSOLE__AUDIT_LOGGER_FAILED)
                .replace("{error}", e.getMessage()));
        }

        fileWatcher = new ConfigFileWatcher(this);
        fileWatcher.start();

        int inactivityDays = this.getConfig().getInt("inactivity_cleanup_days", -1);
        if (inactivityDays > 0) {
            foliaLib.getScheduler().runAsync(task -> new InactivityCleanupTask(inactivityDays).run());
        }

        metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new IntegrationBarChart());

        // Enable integrations — one console line per enabled integration.
        for (PluginIntegration integration : integrations) {
            try {
                integration.enable();
                if (integration.isEnabled()) {
                    BlockProtLogger.log("integration", "Enabled: " + integration.name);
                }
            } catch (NoClassDefFoundError ignored) {}
        }

        BlockProtConsole.printStartupBanner(version);

        new BlockProtAPI(this);

        /* Register Listeners */
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

        // ── Pet protection listeners (BlockProt Reloaded) ──────────────────────
        // Always registered so the config toggle is hot-reloadable (/bp reload).
        // Each event handler checks isPetProtectionEnabled() at the top and returns
        // immediately when disabled, adding zero overhead when the feature is off.
        registerEvent(pm, new PetProtectionListener());
        registerEvent(pm, new PetMenuOpenListener());
        // ─────────────────────────────────────────────────────────────────────

        if (defaultConfig.isWorldEditPasteAutolockEnabled()) {
            registerEvent(pm, new WorldEditPasteListener(this));
            BlockProtConsole.info(Translator.get(TranslationKey.CONSOLE__WORLDEDIT_LISTENER_ENABLED));
        }

        Objects.requireNonNull(this.getCommand("blockprot"))
            .setExecutor(new BlockProtCommand());

        if (defaultConfig.isProtectionExpiryEnabled() && defaultConfig.isExpiryScanOnStartup()) {
            foliaLib.getScheduler().runAsync(task -> runExpiryScan());
        }

        foliaLib.getScheduler().runAsync(task -> populateProtectedBlockCache());

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
        this.mergeMissingConfigKeys();
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

        if (defaultConfig.isWorldsConfigEnabled()) {
            File worldsFile = new File(this.getDataFolder(), "worlds.yml");
            YamlConfiguration worldsDisk = WorldsConfig.scanAndPopulate(worldsFile, this.getConfig(), this.getLogger());
            worldsConfig = new WorldsConfig(worldsDisk);
        } else {
            worldsConfig = null;
            BlockProtLogger.log("worlds-scan", "worlds_config_enabled=false; using global config.yml lockable lists.");
        }

        for (PluginIntegration integration : integrations) {
            integration.reload();
        }
    }

    private void registerEvent(@NotNull PluginManager pm, Listener listener) {
        pm.registerEvents(listener, this);
    }

    /**
     * Registers a plugin integration. Logs to the session file only;
     * console output is deferred until after the integration is confirmed enabled.
     */
    void registerIntegration(@NotNull PluginIntegration integration) {
        this.integrations.add(integration);
        BlockProtLogger.log("integration", "Registered: " + integration.name);
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
                getLogger().warning(Translator.get(TranslationKey.CONSOLE__CONFIG_LANGUAGE_MISSING)
                    .replace("{file}", name));
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
                BlockProtLogger.log("lang-merge", resource + " — added missing key: " + key);
            }
        }

        if (added > 0) {
            try {
                diskConfig.save(diskFile);
                BlockProtLogger.log("lang-merge", resource + " — added " + added + " missing key(s).");
            } catch (IOException e) {
                BlockProtConsole.warn(Translator.get(TranslationKey.CONSOLE__LANG_KEYS_SAVE_FAILED)
                    .replace("{file}", resource).replace("{error}", e.getMessage()));
            }
        }
    }

    /**
     * Rewrites config.yml on disk using the bundled JAR template as a base,
     * preserving all values the administrator has already configured.
     * This ensures format, comments, and sections are always clean,
     * and removes obsolete keys (mysql, console, lockable_*) if present.
     */
    private void cleanLegacyConfigKeys() {
        File diskFile = new File(this.getDataFolder(), "config.yml");
        if (!diskFile.exists()) return;

        YamlConfiguration userValues = YamlConfiguration.loadConfiguration(diskFile);

        InputStream jarStream = this.getResource("config.yml");
        if (jarStream == null) return;
        YamlConfiguration template = YamlConfiguration.loadConfiguration(
            new BufferedReader(new InputStreamReader(jarStream, StandardCharsets.UTF_8)));

        // Strategy: start from the user's config and only ADD keys that are missing.
        // This guarantees that every value the admin has set is preserved verbatim,
        // including keys that do not exist in the current template (custom or legacy).
        int added = 0;
        for (String key : template.getKeys(true)) {
            if (template.isConfigurationSection(key)) continue;
            if (EXTERNAL_CONFIG_KEYS.contains(key)) continue;
            if (!userValues.contains(key)) {
                userValues.set(key, template.get(key));
                added++;
            }
        }

        try {
            userValues.save(diskFile);
            BlockProtLogger.log("config-clean",
                "config.yml merged: all user values preserved" +
                (added > 0 ? ", added " + added + " missing key(s) from template." : "."));
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to save config.yml after merge: " + e.getMessage());
        }
    }

    /** Keys managed by separate files — never merged back into config.yml. */
    private static final Set<String> EXTERNAL_CONFIG_KEYS = Set.of(
        "lockable_tile_entities", "lockable_shulker_boxes", "lockable_blocks", "lockable_doors",
        "mysql.enabled", "mysql.host", "mysql.port", "mysql.database",
        "mysql.username", "mysql.password", "mysql.jdbc_url",
        "mysql.pool.maximum_pool_size", "mysql.pool.minimum_idle", "mysql.pool.connection_timeout_ms",
        "console.prefix_color", "console.info_color"
    );

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
                BlockProtLogger.log("config-merge", "config.yml — added missing key: " + key);
            }
        }

        if (added == 0) return;
        try {
            diskConfig.save(diskFile);
            BlockProtLogger.log("config-merge", "config.yml — added " + added + " missing option(s).");
        } catch (IOException e) {
            BlockProtConsole.warn(Translator.get(TranslationKey.CONSOLE__CONFIG_KEYS_SAVE_FAILED)
                .replace("{error}", e.getMessage()));
            BlockProtLogger.log("config-merge", "Failed to save config.yml after merge: " + e.getMessage());
        }
    }

    /**
     * Saves a resource silently — does not log a warning when the file already exists.
     */
    private void saveResourceSilent(@NotNull String name, boolean replace) {
        File dest = new File(this.getDataFolder(), name);
        if (!replace && dest.exists()) return; // already there, skip quietly
        try {
            this.saveResource(name, replace);
        } catch (Exception ignored) {}
    }

    /**
     * Scans all loaded worlds for expired block protections and clears them.
     * Runs async on startup when {@code enable_protection_expiry: true} and
     * {@code expiry_scan_on_startup: true}.
     */
    private void runExpiryScan() {
        if (hybridDatabase == null || !hybridDatabase.isEnabled()) return;
        int cleared = 0;
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            var locations = hybridDatabase.getBlockIndexByWorld(world.getName());
            for (org.bukkit.Location loc : locations) {
                org.bukkit.block.Block block = loc.getBlock();
                if (!BlockProt.getDefaultConfig().isLockable(block.getType())) continue;
                try {
                    de.sean.blockprot.bukkit.nbt.BlockNBTHandler handler =
                        new de.sean.blockprot.bukkit.nbt.BlockNBTHandler(block);
                    if (handler.isExpired()) {
                        handler.clear();
                        handler.applyToOtherContainer();
                        de.sean.blockprot.bukkit.listeners.HopperEventListener.invalidate(block);
                        cleared++;
                    }
                } catch (RuntimeException ignored) {}
            }
        }
        if (cleared > 0) {
            BlockProtLogger.log("expiry-scan", "Cleared " + cleared + " expired protection(s) on startup.");
        }
    }

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
            // No MySQL — iterate all player stat entries to collect protected locations.
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

    private boolean isRunningCraftBukkit() {
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }

    /**
     * Copies data from known legacy plugin folder names into the current data folder.
     *
     * <p>Legacy names checked (in priority order):
     * <ol>
     *   <li>{@code BlockProt}       — original upstream plugin</li>
     *   <li>{@code BlockProtPlus}   — intermediate fork name</li>
     * </ol>
     *
     * <p>Rules:
     * <ul>
     *   <li>Only runs when the current data folder does NOT contain a {@code config.yml}
     *       (i.e. first boot after rename). If the new folder already has data the
     *       migration is skipped entirely to avoid overwriting admin changes.</li>
     *   <li>Files are copied recursively; existing files in the destination are
     *       never overwritten.</li>
     *   <li>The legacy folder is left intact — the admin decides when to remove it.</li>
     *   <li>A marker file {@code .migrated} is written to the source folder after a
     *       successful copy so the migration never runs twice.</li>
     * </ul>
     */
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
                // Copy every file that does NOT already exist in the destination.
                // This preserves any files the new plugin already created (e.g. defaults).
                copyDirectoryContents(legacyFolder.toPath(), this.getDataFolder().toPath());

                // Additionally merge the legacy config.yml into the new one key-by-key
                // so that custom values the admin set are preserved even if the new
                // template has a different structure.
                mergeYamlUserValues(
                    new File(legacyFolder, "config.yml"),
                    new File(this.getDataFolder(), "config.yml")
                );

                // Merge every lang file present in the legacy folder.
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
            } catch (IOException e) {
                pendingMigrationError = e.getMessage();
            }
            break;
        }
    }

    /**
     * Merges user values from {@code src} YAML into {@code dst} YAML.
     * Every key present in {@code src} that is absent in {@code dst} is copied.
     * Keys already in {@code dst} are never overwritten — the destination wins.
     * If {@code dst} does not exist, {@code src} is copied verbatim.
     */
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

    /**
     * Emits the queued migration messages through Translator after translations are loaded.
     * Called immediately after {@link #reloadConfigAndTranslations()}.
     */
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
     * Recursively copies all files from {@code src} into {@code dst}.
     * Existing files in {@code dst} are never overwritten.
     * Directory structure is replicated as needed.
     */
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
                Path target = dst.resolve(src.relativize(file));
                // Do not overwrite files that already exist in the destination.
                if (!Files.exists(target)) {
                    Files.copy(file, target, StandardCopyOption.COPY_ATTRIBUTES);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
