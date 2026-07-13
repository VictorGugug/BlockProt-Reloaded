package de.sean.blockprot.bukkit.config;

import de.sean.blockprot.bukkit.BlockProt;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public final class LangConfig {
    private static YamlConfiguration config;

    private LangConfig() {}

    public static void reload() {
        BlockProt plugin = BlockProt.getInstance();
        File file = new File(plugin.getDataFolder(), "lang.yml");
        if (!file.exists()) {
            plugin.saveResource("lang.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private static void ensureLoaded() {
        if (config == null) reload();
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
}
