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
import de.sean.blockprot.bukkit.logger.PluginActivityLog;
import de.sean.blockprot.bukkit.tasks.ConfigFileWatcher;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Central coordinator for executing atomic configuration reloads and logging reports.
 */
public final class ReloadCoordinator {

    private ReloadCoordinator() {}

    @NotNull
    public static ReloadReport commitManual(@Nullable UUID requestingPlayer) {
        return executeReload(ReloadReport.ReloadSource.MANUAL_DIALOG, requestingPlayer);
    }

    @NotNull
    public static ReloadReport commitAutomatic() {
        return executeReload(ReloadReport.ReloadSource.AUTOMATIC, null);
    }

    @NotNull
    public static ReloadReport commitCommand() {
        return executeReload(ReloadReport.ReloadSource.MANUAL_COMMAND, null);
    }

    @NotNull
    public static ReloadReport commitForce(@Nullable UUID requestingPlayer) {
        return executeReload(ReloadReport.ReloadSource.MANUAL_FORCE, requestingPlayer);
    }

    @NotNull
    public static ReloadReport commitInventory() {
        return executeReload(ReloadReport.ReloadSource.MANUAL_INVENTORY, null);
    }

    @NotNull
    public static ReloadReport commitExternal() {
        return executeReload(ReloadReport.ReloadSource.EXTERNAL_FILE, null);
    }

    /**
     * Builds a non-committing diff between the plugin's currently-loaded
     * in-memory config and what is currently on disk. Used to preview
     * external file edits while auto-reload is disabled: at the moment
     * this is called nothing has reloaded yet, so the live in-memory state
     * is an accurate "before" and the on-disk files are an accurate
     * "after", with no writes and no state changes on either side.
     */
    @NotNull
    public static List<ReloadReport.ChangeDiff> previewExternalDiff() {
        BlockProt plugin = BlockProt.getInstance();
        Map<String, Object> beforeSnapshot = captureFullSnapshot(plugin, true);
        Map<String, Object> afterSnapshot = captureFullSnapshot(plugin, false);
        return ReloadReport.compareSnapshots(beforeSnapshot, afterSnapshot);
    }

    private static synchronized ReloadReport executeReload(@NotNull ReloadReport.ReloadSource source, @Nullable UUID actor) {
        BlockProt plugin = BlockProt.getInstance();
        PendingConfigChanges pending = PendingConfigChanges.getInstance();

        boolean captureBeforeFromMemory = source == ReloadReport.ReloadSource.AUTOMATIC
            || source == ReloadReport.ReloadSource.EXTERNAL_FILE;
        Map<String, Object> beforeSnapshot = captureFullSnapshot(plugin, captureBeforeFromMemory);

        try {
            if (pending.hasPending()) {
                writePendingToDisk(plugin, pending);
            }

            ConfigFileWatcher watcher = plugin.getFileWatcher();
            if (watcher != null) {
                watcher.suppressNext();
            }

            plugin.reloadConfigAndTranslations();

            Map<String, Object> afterSnapshot = captureFullSnapshot(plugin, false);
            List<ReloadReport.ChangeDiff> diffs = ReloadReport.compareSnapshots(beforeSnapshot, afterSnapshot);

            pending.clear();

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

    private static void writePendingToDisk(@NotNull BlockProt plugin, @NotNull PendingConfigChanges pending) throws IOException {
        List<PendingConfigChanges.PendingEntry> snapshot = pending.getSortedSnapshot();
        Map<String, Map<String, Object>> filesToUpdate = new HashMap<>();

        for (PendingConfigChanges.PendingEntry entry : snapshot) {
            filesToUpdate.computeIfAbsent(entry.getFile(), f -> new HashMap<>())
                .put(entry.getKey(), entry.getRequestedValue());
        }

        ConfigFileWatcher watcher = plugin.getFileWatcher();

        for (Map.Entry<String, Map<String, Object>> fileEntry : filesToUpdate.entrySet()) {
            String relativePath = fileEntry.getKey();
            File diskFile = new File(plugin.getDataFolder(), relativePath);

            if (watcher != null) {
                watcher.suppressNext();
            }

            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(diskFile);
            for (Map.Entry<String, Object> kv : fileEntry.getValue().entrySet()) {
                yaml.set(kv.getKey(), kv.getValue());
            }
            yaml.save(diskFile);
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
        BlockProtLogger.log("reload", "=== Configuration Reload Started [" + sourceTag + "] ===");

        if (!report.isSuccess()) {
            BlockProtLogger.warn("Reload failed [" + sourceTag + "]: " + report.getErrorMessage());
            PluginActivityLog.logReload(sourceTag, actor, false, 0, report.getErrorMessage());
            return;
        }

        if (report.getDiffs().isEmpty()) {
            BlockProtLogger.logConsole("reload", "Reload completed [" + sourceTag + "]: no configuration values changed.");
        } else {
            boolean simplified = BlockProt.getDefaultConfig().isSimplifiedLogEnabled();
            String origin = describeSource(report.getSource());
            String actorName = describeActor(actor);
            for (ReloadReport.ChangeDiff diff : report.getDiffs()) {
                BlockProtLogger.log("reload", simplified
                    ? "  " + diff.getFile() + ": " + diff.getKey() + " changed from " + diff.getOldValue()
                        + " to " + diff.getNewValue() + " (" + origin + ", " + actorName + ")"
                    : "  " + diff.toString());
            }
            BlockProtLogger.logConsole("reload", "Reload completed [" + sourceTag + "]: " + report.getDiffs().size() + " change(s) activated.");
        }

        PluginActivityLog.logReload(sourceTag, actor, true, report.getDiffs().size(), null);
    }

    @NotNull
    private static String describeSource(@NotNull ReloadReport.ReloadSource source) {
        return switch (source) {
            case AUTOMATIC -> "automatic reload after external file edit";
            case MANUAL_DIALOG -> "in-game dialog";
            case MANUAL_COMMAND -> "console or /bp reload command";
            case MANUAL_FORCE -> "in-game force reload button";
            case MANUAL_INVENTORY -> "in-game inventory menu";
            case EXTERNAL_FILE -> "external file edit";
        };
    }

    @NotNull
    private static String describeActor(@Nullable UUID actor) {
        if (actor == null) return "SERVER";
        org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(actor);
        String name = player.getName();
        return name != null ? name : actor.toString();
    }
}
