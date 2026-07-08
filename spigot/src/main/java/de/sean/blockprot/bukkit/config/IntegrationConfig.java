package de.sean.blockprot.bukkit.config;

import de.sean.blockprot.bukkit.BlockProt;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public final class IntegrationConfig {
    private static FileConfiguration config;

    private IntegrationConfig() {
    }

    public static void reload() {
        BlockProt plugin = BlockProt.getInstance();
        File file = new File(plugin.getDataFolder(), "integrations.yml");
        if (!file.exists()) {
            plugin.saveResource("integrations.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private static void ensureLoaded() {
        if (config == null) {
            reload();
        }
    }

    @NotNull
    public static YamlConfiguration getSection(@NotNull final String integrationName) {
        ensureLoaded();
        ConfigurationSection section = config.getConfigurationSection(integrationName);
        YamlConfiguration result = new YamlConfiguration();
        if (section != null) {
            for (String key : section.getKeys(true)) {
                if (!section.isConfigurationSection(key)) {
                    result.set(key, section.get(key));
                }
            }
        }
        return result;
    }

    public static boolean getBoolean(@NotNull final String path, final boolean def) {
        ensureLoaded();
        return config.getBoolean(path, def);
    }

    public static int getInt(@NotNull final String path, final int def) {
        ensureLoaded();
        return config.getInt(path, def);
    }

    public static long getLong(@NotNull final String path, final long def) {
        ensureLoaded();
        return config.getLong(path, def);
    }
}
