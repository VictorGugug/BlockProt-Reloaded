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

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class BlockProtConsole {

    @Nullable
    private static List<StartupLine> startupBuffer = null;

    @Nullable
    private static Logger pluginLogger = null;

    private record StartupLine(String message, boolean isWarning) {}

    private BlockProtConsole() {}

    public static void beginStartup(@NotNull Logger logger) {
        pluginLogger = logger;
        startupBuffer = new ArrayList<>();
    }

    public static void printStartupBanner(@NotNull String version) {
        List<StartupLine> lines = startupBuffer != null ? startupBuffer : new ArrayList<>();
        startupBuffer = null;
        if (pluginLogger == null) return;

        pluginLogger.info("  ██████╗ ██████╗  ██████╗ ");
        pluginLogger.info("  ██╔══██╗██╔══██╗██╔══██╗");
        pluginLogger.info("  ██████╔╝██████╔╝██████╔╝");
        pluginLogger.info("  ██╔══██╗██╔═══╝ ██╔══██╗");
        pluginLogger.info("  ██████╔╝██║     ██║  ██║");
        pluginLogger.info("  ╚═════╝ ╚═╝     ╚═╝  ╚═╝");
        pluginLogger.info("        BlockProt Reloaded");
        pluginLogger.info("            v" + version);

        if (lines.isEmpty()) {
            pluginLogger.info("  No startup messages.");
            return;
        }

        for (StartupLine line : lines) {
            if (line.isWarning()) {
                pluginLogger.warning(line.message());
            } else {
                pluginLogger.info(line.message());
            }
        }
    }

    private static void bootLine(@NotNull String label, @NotNull String status, boolean isLast) {
        String connector = isLast ? "└─" : "├─";
        info("  " + connector + " " + label + "  " + status);
    }

    public static void boot(@NotNull String label, @NotNull String status) {
        bootLine(label, status, false);
    }

    public static void bootLast(@NotNull String label, @NotNull String status) {
        bootLine(label, status, true);
    }

    public static void info(@NotNull String message) {
        emit(message.startsWith("  ") ? message : "  " + message);
    }

    public static void success(@NotNull String message) {
        emit(message.startsWith("  ") ? message : "  " + message);
    }

    public static void warn(@NotNull String message) {
        String prefixed = message.startsWith("  ") ? message : "  " + message;
        if (startupBuffer != null) {
            startupBuffer.add(new StartupLine(prefixed, true));
        } else if (pluginLogger != null) {
            pluginLogger.warning(prefixed);
        } else {
            Bukkit.getConsoleSender().sendMessage(prefixed);
        }
    }

    public static void error(@NotNull String message) {
        String prefixed = message.startsWith("  ") ? message : "  " + message;
        if (startupBuffer != null) {
            startupBuffer.add(new StartupLine(prefixed, true));
        } else if (pluginLogger != null) {
            pluginLogger.severe(prefixed);
        } else {
            Bukkit.getConsoleSender().sendMessage(prefixed);
        }
    }

    public static void integrationEnabled(@NotNull String integrationName) {
        emit("  " + integrationName + " hooked");
    }

    private static void emit(@NotNull String message) {
        if (startupBuffer != null) {
            startupBuffer.add(new StartupLine(message, false));
        } else if (pluginLogger != null) {
            pluginLogger.info(message);
        }
    }
}
