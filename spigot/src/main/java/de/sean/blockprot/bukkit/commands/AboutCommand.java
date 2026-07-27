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

package de.sean.blockprot.bukkit.commands;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.VersionCompat;
import de.sean.blockprot.bukkit.integrations.PluginIntegration;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import de.sean.blockprot.bukkit.dialogs.AboutDialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays full plugin version, maintainer info, runtime state, and diagnostic info.
 */
public class AboutCommand implements CommandExecutor {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String senderName = (sender instanceof Player p) ? p.getName() : "Console";
        BlockProtLogger.log("command", senderName + " executed /bp about");

        if (sender instanceof Player player && BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
            AboutDialog.show(player);
            return true;
        }

        String version = BlockProt.getPluginVersion();
        String serverVer = Bukkit.getVersion();
        String runtimeVer = VersionCompat.getDiagnosticString();
        String javaVer = System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")";
        
        boolean dbActive = BlockProt.getHybridDatabase() != null && BlockProt.getHybridDatabase().isEnabled();
        String dbMode = dbActive ? "MySQL" : "SQLite (blockprot_usercache.sqlite)";
        int cacheCount = ProtectedBlockCache.size();
        boolean hasDialogs = VersionCompat.hasDialogApi();
        boolean auditActive = BlockProt.getAuditLogger() != null;

        List<String> activeInts = new ArrayList<>();
        List<PluginIntegration> allInts = BlockProt.getInstance().getIntegrations();
        for (PluginIntegration pi : allInts) {
            if (pi.isEnabled()) activeInts.add(pi.name);
        }
        String intText = activeInts.isEmpty()
            ? "§c0/" + allInts.size() + " Active"
            : "§a" + activeInts.size() + "/" + allInts.size() + " Active (" + String.join(", ", activeInts) + ")";

        File currentLog = BlockProtLogger.getCurrentLogFile();
        String logPath = currentLog != null ? currentLog.getName() : "None";

        List<String> reportLines = List.of(
            "§8[§rBlockProt Reloaded§8] §a=== About & System Information ===",
            "§8[§rBlockProt Reloaded§8] §7Plugin Version: §e" + version,
            "§8[§rBlockProt Reloaded§8] §7Maintainers: §eZaynr (Zar), spnda",
            "§8[§rBlockProt Reloaded§8] §7Server Version: §e" + serverVer,
            "§8[§rBlockProt Reloaded§8] §7Runtime Engine: §e" + runtimeVer,
            "§8[§rBlockProt Reloaded§8] §7Java Runtime: §e" + javaVer,
            "§8[§rBlockProt Reloaded§8] §7Database Engine: §e" + dbMode,
            "§8[§rBlockProt Reloaded§8] §7Cache Population: §e" + cacheCount + " protected blocks",
            "§8[§rBlockProt Reloaded§8] §7Dialog API: " + (hasDialogs ? "§aAvailable" : "§cUnavailable"),
            "§8[§rBlockProt Reloaded§8] §7Integrations: " + intText,
            "§8[§rBlockProt Reloaded§8] §7Audit Logger: " + (auditActive ? "§aActive" : "§cDisabled"),
            "§8[§rBlockProt Reloaded§8] §7Backups Mode: §eMigration-only",
            "§8[§rBlockProt Reloaded§8] §7Active Log File: §e" + logPath
        );

        for (String l : reportLines) {
            sender.sendMessage(LEGACY.deserialize(l));
            BlockProtLogger.log(l);
        }

        Component reportLink = LEGACY.deserialize("§8[§rBlockProt Reloaded§8] §7Report Issues: §bhttps://github.com/VictorGugug/BlockProt-Reloaded/issues")
            .clickEvent(ClickEvent.openUrl("https://github.com/VictorGugug/BlockProt-Reloaded/issues"))
            .hoverEvent(HoverEvent.showText(LEGACY.deserialize("§7Click to open GitHub Issues tracker")));
        sender.sendMessage(reportLink);
        BlockProtLogger.log("Report Issues: https://github.com/VictorGugug/BlockProt-Reloaded/issues");

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return null;
    }
}