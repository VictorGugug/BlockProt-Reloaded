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

    private static final java.util.regex.Pattern COLOR_STRIP =
        java.util.regex.Pattern.compile("(?i)§[0-9a-fk-orx]|§x(?:§[0-9a-f]){6}");

    @Nullable private static PrintWriter writer         = null;
    @Nullable private static File        currentLogFile = null;
    @Nullable private static File        logsDir        = null;

    private static boolean enabled = true;
    private static int rotationCount = 0;
    private static long lastKnownLength = 0;

    @Nullable private static String fileTimestamp = null;

    @Nullable private static java.util.function.Function<String, String> secondaryTranslator = null;

    public static void setSecondaryTranslator(@Nullable java.util.function.Function<String, String> fn) {
        secondaryTranslator = fn;
    }

    private BlockProtLogger() {}

    public static void init(@NotNull File dataFolder, boolean sessionLogEnabled) {
        enabled = sessionLogEnabled;
        if (!enabled) return;

        logsDir = new File(dataFolder, "logs");
        if (!logsDir.exists()) logsDir.mkdirs();

        fileTimestamp = LocalDateTime.now().format(FILE_FMT);
        rotationCount = 0;
        openNewFile();
    }

    private static void openNewFile() {
        if (writer != null) {
            writer.flush();
            writer.close();
            writer = null;
        }
        if (logsDir == null || fileTimestamp == null) return;

        String suffix = rotationCount > 0 ? "_" + rotationCount : "";
        currentLogFile = new File(logsDir, "blockprot-" + fileTimestamp + suffix + ".log");
        int n = 1;
        while (currentLogFile.exists()) {
            suffix = "_" + n;
            currentLogFile = new File(logsDir, "blockprot-" + fileTimestamp + suffix + ".log");
            n++;
        }

        try {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(currentLogFile), StandardCharsets.UTF_8));
            lastKnownLength = 0;
            rotationCount++;
            String startMsg = rotationCount == 1
                ? "=== BlockProt Session Start ==="
                : "=== BlockProt Session Start (rotated) ===";
            log(startMsg);
        } catch (IOException e) {
            writer = null;
            currentLogFile = null;
            java.util.logging.Logger.getLogger("BlockProt").severe(
                "[BlockProt] CRITICAL: Failed to initialise plugin log file: " + e.getMessage());
        }
    }

    private static void checkFile() {
        if (writer == null || currentLogFile == null) return;
        boolean needsRotation = false;
        if (!currentLogFile.exists()) {
            needsRotation = true;
        } else {
            long currentLength = currentLogFile.length();
            if (currentLength < lastKnownLength) {
                needsRotation = true;
            }
        }
        if (needsRotation) {
            openNewFile();
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
        if (!enabled) return;
        checkFile();
        if (writer == null) return;
        String line = "[" + LocalDateTime.now().format(LINE_FMT) + "] "
            + COLOR_STRIP.matcher(message).replaceAll("");
        if (secondaryTranslator != null) {
            String alt = secondaryTranslator.apply(message);
            if (alt != null && !alt.equals(message)) line += "  |  " + alt;
        }
        writer.println(line);
        writer.flush();
        lastKnownLength = currentLogFile != null ? currentLogFile.length() : 0;
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