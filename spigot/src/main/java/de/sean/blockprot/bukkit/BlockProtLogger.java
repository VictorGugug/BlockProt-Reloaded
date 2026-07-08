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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Session logger for BlockProt.
 *
 * One log file per server start, named blockprot-YYYY-MM-DD_HH-mm-ss.log.
 * Can be disabled via {@code enable_session_log: false} in config.yml.
 *
 * Usage:
 *   BlockProtLogger.init(dataFolder, enabled);  // call from onEnable
 *   BlockProtLogger.log("message");
 *   BlockProtLogger.close();                    // call from onDisable
 */
public final class BlockProtLogger {

    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter LINE_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Nullable private static PrintWriter writer         = null;
    @Nullable private static File        currentLogFile = null;

    private static boolean enabled = true;

    @Nullable private static java.util.function.Function<String, String> secondaryTranslator = null;

    public static void setSecondaryTranslator(@Nullable java.util.function.Function<String, String> fn) {
        secondaryTranslator = fn;
    }

    private BlockProtLogger() {}

    public static void init(@NotNull File dataFolder, boolean sessionLogEnabled) {
        enabled = sessionLogEnabled;
        if (!enabled) return;

        File logsDir = new File(dataFolder, "logs");
        if (!logsDir.exists()) logsDir.mkdirs();

        String timestamp = LocalDateTime.now().format(FILE_FMT);
        currentLogFile = new File(logsDir, "blockprot-" + timestamp + ".log");

        int n = 1;
        while (currentLogFile.exists()) {
            currentLogFile = new File(logsDir, "blockprot-" + timestamp + "_" + n + ".log");
            n++;
        }

        try {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(currentLogFile), StandardCharsets.UTF_8));
            log("=== BlockProt Session Start ===");
        } catch (IOException e) {
            writer = null;
            currentLogFile = null;
            java.util.logging.Logger.getLogger("BlockProt").severe(
                "[BlockProt] CRITICAL: Failed to initialise plugin log file: " + e.getMessage());
        }
    }

    public static void close() {
        if (writer != null) {
            log("=== BlockProt Session End ===");
            writer.flush();
            writer.close();
            writer = null;
        }
    }

    public static void log(@NotNull String message) {
        if (!enabled || writer == null) return;
        String line = "[" + LocalDateTime.now().format(LINE_FMT) + "] " + message;
        if (secondaryTranslator != null) {
            String alt = secondaryTranslator.apply(message);
            if (alt != null && !alt.equals(message)) line += "  |  " + alt;
        }
        writer.println(line);
        writer.flush();
    }

    public static void log(@NotNull String section, @NotNull String message) {
        log("[" + section + "] " + message);
    }

    public static void pass(@NotNull String check) {
        log("PASS: " + check);
    }

    public static void fail(@NotNull String check, @Nullable String reason) {
        log("FAIL: " + check + (reason != null ? " - " + reason : ""));
    }

    public static void warn(@NotNull String message) {
        log("WARN: " + message);
    }

    public static void separator() {
        log("---");
    }

    @Nullable
    public static File getCurrentLogFile() {
        return currentLogFile;
    }
}