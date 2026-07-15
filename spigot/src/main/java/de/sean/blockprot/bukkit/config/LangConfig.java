package de.sean.blockprot.bukkit.config;

import de.sean.blockprot.bukkit.BlockProt;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public final class LangConfig {
    private static YamlConfiguration config;
    private static File configFile;

    private LangConfig() {}

    public static void reload() {
        BlockProt plugin = BlockProt.getInstance();
        configFile = new File(plugin.getDataFolder(), "lang/lang.yml");
        if (!configFile.exists()) {
            plugin.saveResource("lang/lang.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
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
        return config.getBoolean("languages." + fileName, false);
    }

    public static boolean isFileListed(@NotNull String fileName) {
        ensureLoaded();
        return config.contains("languages." + fileName);
    }

    public static void setLanguageEnabled(@NotNull String fileName, boolean enabled) {
        ensureLoaded();
        config.set("languages." + fileName, enabled);
        save();
    }
}
