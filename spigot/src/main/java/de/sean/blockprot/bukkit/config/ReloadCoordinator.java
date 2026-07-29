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
import de.sean.blockprot.bukkit.BlockProtConsole;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.logger.PluginActivityLog;
import de.sean.blockprot.bukkit.tasks.ConfigFileWatcher;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * Central coordinator for executing atomic configuration reloads and logging reports.
 */
public final class ReloadCoordinator {

    private ReloadCoordinator() {}

    @NotNull
    public static ReloadReport commitAutomatic() {
        return executeReload(ReloadReport.ReloadSource.AUTOMATIC, null);
    }

    @NotNull
    public static ReloadReport commitCommand() {
        return executeReload(ReloadReport.ReloadSource.MANUAL_COMMAND, null);
    }

    @NotNull
    public static ReloadReport commitExternal() {
        return executeReload(ReloadReport.ReloadSource.EXTERNAL_FILE, null);
    }

    private static synchronized ReloadReport executeReload(@NotNull ReloadReport.ReloadSource source, @Nullable UUID actor) {
        BlockProt plugin = BlockProt.getInstance();

        boolean captureBeforeFromMemory = source == ReloadReport.ReloadSource.AUTOMATIC
            || source == ReloadReport.ReloadSource.EXTERNAL_FILE;
        Map<String, Object> beforeSnapshot = captureFullSnapshot(plugin, captureBeforeFromMemory);

        try {
            ConfigFileWatcher watcher = plugin.getFileWatcher();
            if (watcher != null) {
                watcher.suppressNext();
            }

            plugin.reloadConfigAndTranslations();

            Map<String, Object> afterSnapshot = captureFullSnapshot(plugin, false);
            List<ReloadReport.ChangeDiff> diffs = ReloadReport.compareSnapshots(beforeSnapshot, afterSnapshot);

            ReloadReport report = new ReloadReport(source, diffs, true, null);
            logReport(report, actor);

            return report;
        } catch (Exception ex) {
            BlockProtLogger.warn("Reload failed (" + source.getTag() + "): " + ex.getMessage());
            ReloadReport report = new ReloadReport(source, Collections.emptyList(), false, ex.getMessage());
            logReport(report, actor);
            return report;
        }
    }

    private static Map<String, Object> captureFullSnapshot(@NotNull BlockProt plugin, boolean fromLiveMemory) {
        Map<String, Object> fullMap = new HashMap<>();

        if (fromLiveMemory) {
            DefaultConfig live = BlockProt.getDefaultConfig();
            fullMap.putAll(ReloadReport.captureSnapshot(live.getBukkitConfig().getValues(true), "config.yml"));

            YamlConfiguration liveBlocks = live.getBlocksConfig();
            if (liveBlocks != null) {
                fullMap.putAll(ReloadReport.captureSnapshot(liveBlocks.getValues(true), "blocks.yml"));
            }

            YamlConfiguration liveLang = LangConfig.getConfig();
            if (liveLang != null) {
                fullMap.putAll(ReloadReport.captureSnapshot(liveLang.getValues(true), "lang/lang.yml"));
            }

            return fullMap;
        }

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (configFile.exists()) {
            YamlConfiguration c = YamlConfiguration.loadConfiguration(configFile);
            fullMap.putAll(ReloadReport.captureSnapshot(c.getValues(true), "config.yml"));
        }

        File langFile = new File(plugin.getDataFolder(), "lang/lang.yml");
        if (langFile.exists()) {
            YamlConfiguration l = YamlConfiguration.loadConfiguration(langFile);
            fullMap.putAll(ReloadReport.captureSnapshot(l.getValues(true), "lang/lang.yml"));
        }

        File blocksFile = new File(plugin.getDataFolder(), "blocks.yml");
        if (blocksFile.exists()) {
            YamlConfiguration b = YamlConfiguration.loadConfiguration(blocksFile);
            fullMap.putAll(ReloadReport.captureSnapshot(b.getValues(true), "blocks.yml"));
        }

        return fullMap;
    }

    private static void logReport(@NotNull ReloadReport report, @Nullable UUID actor) {
        String sourceTag = report.getSource().getTag();

        if (!report.isSuccess()) {
            BlockProtLogger.log("reload", "=== Reload Failed [" + sourceTag + "] ===");
            BlockProtLogger.log("reload", "  " + report.getErrorMessage());
            BlockProtLogger.log("reload", "=== Reload Failed [" + sourceTag + "] ===");
            BlockProtConsole.info("Reload failed.");
            PluginActivityLog.logReload(sourceTag, actor, false, 0, report.getErrorMessage());
            return;
        }

        BlockProtLogger.log("reload", "=== Reload Started [" + sourceTag + "] ===");
        for (ReloadReport.ChangeDiff diff : report.getDiffs()) {
            BlockProtLogger.log("reload", "  " + diff.getFile() + ": " + diff.getKey()
                + " changed from " + diff.getOldValue() + " to " + diff.getNewValue());
        }

        int count = report.getDiffs().size();
        BlockProtLogger.log("reload", "=== Reload Completed [" + sourceTag + "]: " + count + " change(s) ===");
        BlockProtConsole.info("Reload completed.");

        PluginActivityLog.logReload(sourceTag, actor, true, count, null);
    }
}
