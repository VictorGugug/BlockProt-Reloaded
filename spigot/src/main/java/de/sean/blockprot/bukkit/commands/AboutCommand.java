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
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor MUTED_GRAY = TextColor.color(0x888888);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xF0A0A0);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xD2B48C);
    private static final TextColor SOFT_BLUE = TextColor.color(0xA0C4E8);

    private static final Component PREFIX = Component.text()
        .append(Component.text("[", TextColor.color(0x555555)))
        .append(Component.text("BlockProt Reloaded"))
        .append(Component.text("] ", TextColor.color(0x555555)))
        .build();

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
        boolean anyIntActive = !activeInts.isEmpty();
        String intText = anyIntActive
            ? activeInts.size() + "/" + allInts.size() + " Active (" + String.join(", ", activeInts) + ")"
            : "0/" + allInts.size() + " Active";

        File currentLog = BlockProtLogger.getCurrentLogFile();
        String logPath = currentLog != null ? currentLog.getName() : "None";

        List<Component> reportLines = List.of(
            row(null, "=== About & System Information ===", PASTEL_MINT, TextDecoration.BOLD),
            row("Plugin Version: ", version, PASTEL_GOLD),
            row("Maintainers: ", "Zaynr (Zar), spnda", MUTED_GRAY),
            row("Server Version: ", serverVer, MUTED_GRAY),
            row("Runtime Engine: ", runtimeVer, MUTED_GRAY),
            row("Java Runtime: ", javaVer, MUTED_GRAY),
            row("Database Engine: ", dbMode, PASTEL_MINT),
            row("Cache Population: ", cacheCount + " protected blocks", PASTEL_GOLD),
            row("Dialog API: ", hasDialogs ? "Available" : "Unavailable", hasDialogs ? PASTEL_MINT : PASTEL_CORAL),
            row("Integrations: ", intText, anyIntActive ? PASTEL_MINT : PASTEL_CORAL),
            row("Audit Logger: ", auditActive ? "Active" : "Disabled", auditActive ? PASTEL_MINT : PASTEL_CORAL),
            row("Backups Mode: ", "Migration-only", PASTEL_GOLD),
            row("Active Log File: ", logPath, MUTED_GRAY)
        );

        for (Component line : reportLines) {
            sender.sendMessage(line);
            BlockProtLogger.log(LEGACY.serialize(line));
        }

        String issuesUrl = "https://github.com/VictorGugug/BlockProt-Reloaded/issues";
        Component reportLink = Component.text()
            .append(PREFIX)
            .append(Component.text("Report Issues: ", SOFT_GRAY))
            .append(Component.text(issuesUrl, SOFT_BLUE)
                .clickEvent(ClickEvent.openUrl(issuesUrl))
                .hoverEvent(HoverEvent.showText(Component.text("Click to open GitHub Issues tracker", SOFT_GRAY))))
            .build();
        sender.sendMessage(reportLink);
        BlockProtLogger.log("Report Issues: " + issuesUrl);

        return true;
    }

    @NotNull
    private static Component row(@Nullable String label, @NotNull String value, @NotNull TextColor valueColor, @NotNull TextDecoration... decorations) {
        var builder = Component.text().append(PREFIX);
        if (label != null) builder.append(Component.text(label, SOFT_GRAY));
        builder.append(Component.text(value, valueColor, decorations));
        return builder.build();
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return null;
    }
}