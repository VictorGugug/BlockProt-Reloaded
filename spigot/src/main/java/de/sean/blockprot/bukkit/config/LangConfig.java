package de.sean.blockprot.bukkit.config;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.Translator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LangConfig {
    private static YamlConfiguration config;
    private static File configFile;

    private static final Map<String, String> CODE_TO_FILE = new LinkedHashMap<>();
    private static final Map<String, String> FILE_TO_CODE = new LinkedHashMap<>();

    static {
        for (String file : Translator.DEFAULT_TRANSLATION_FILES) {
            String code = file.replace("translations_", "").replace(".yml", "");
            CODE_TO_FILE.put(code, file);
            FILE_TO_CODE.put(file, code);
        }
    }

    private LangConfig() {}

    public static void reload() {
        BlockProt plugin = BlockProt.getInstance();
        configFile = new File(plugin.getDataFolder(), "lang/lang.yml");
        if (!configFile.exists()) {
            plugin.saveResource("lang/lang.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        migrateIfNeeded();
    }

    private static void migrateIfNeeded() {
        ConfigurationSection section = config.getConfigurationSection("languages");
        if (section == null) return;

        boolean needsMigration = false;
        for (String key : section.getKeys(false)) {
            if (key.contains(".") || section.isConfigurationSection(key)) {
                needsMigration = true;
                break;
            }
        }
        if (!needsMigration) return;

        Map<String, Boolean> migrated = new LinkedHashMap<>();
        for (String code : CODE_TO_FILE.keySet()) {
            String oldKey = CODE_TO_FILE.get(code);
            boolean enabled = false;

            if (section.isBoolean(code)) {
                enabled = section.getBoolean(code);
            } else if (section.contains(oldKey) && section.isBoolean(oldKey)) {
                enabled = section.getBoolean(oldKey);
            } else {
                ConfigurationSection sub = section.getConfigurationSection("translations_" + code);
                if (sub != null && sub.contains("yml")) {
                    enabled = sub.getBoolean("yml", false);
                } else {
                    sub = section.getConfigurationSection(code);
                    if (sub != null && sub.contains("yml")) {
                        enabled = sub.getBoolean("yml", false);
                    }
                }
            }
            migrated.put(code, enabled);
        }

        config.set("languages", null);
        for (Map.Entry<String, Boolean> entry : migrated.entrySet()) {
            config.set("languages." + entry.getKey(), entry.getValue());
        }
        save();
    }

    private static void ensureLoaded() {
        if (config == null) reload();
    }

    public static void save() {
        ensureLoaded();
        if (configFile != null) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                BlockProt.getInstance().getLogger().warning("Could not save lang.yml: " + e.getMessage());
            }
        }
    }

    @NotNull
    public static String getFallbackLanguage() {
        ensureLoaded();
        return config.getString("fallback_language", "translations_en.yml");
    }

    public static boolean isLanguageEnabled(@NotNull String fileName) {
        ensureLoaded();
        String code = FILE_TO_CODE.get(fileName);
        if (code == null) return false;
        return config.getBoolean("languages." + code, false);
    }

    public static boolean isFileListed(@NotNull String fileName) {
        ensureLoaded();
        String code = FILE_TO_CODE.get(fileName);
        if (code == null) return false;
        return config.contains("languages." + code);
    }

    public static void setLanguageEnabled(@NotNull String fileName, boolean enabled) {
        ensureLoaded();
        String code = FILE_TO_CODE.get(fileName);
        if (code == null) return;
        config.set("languages." + code, enabled);
        save();
    }

    @NotNull
    public static String fileNameToCode(@NotNull String fileName) {
        return FILE_TO_CODE.getOrDefault(fileName, fileName);
    }

    @NotNull
    public static String codeToFileName(@NotNull String code) {
        return CODE_TO_FILE.getOrDefault(code, "translations_" + code + ".yml");
    }
}
