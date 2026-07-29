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
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Mandatory session and activity logger for BlockProt.
 *
 * Automatically mirrors all plugin logger records into the log file.
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
    private static boolean isReentrancyGuard = false;

    @Nullable private static String fileTimestamp = null;
    @Nullable private static Handler mirrorHandler = null;

    private BlockProtLogger() {}

    public static void init(@NotNull File dataFolder, boolean sessionLogEnabled) {
        enabled = true; // Mandatory activity logging
        logsDir = new File(dataFolder, "logs");
        if (!logsDir.exists()) logsDir.mkdirs();

        fileTimestamp = LocalDateTime.now().format(FILE_FMT);
        rotationCount = 0;
        openNewFile();
        registerMirrorHandler();
    }

    private static void registerMirrorHandler() {
        if (mirrorHandler != null) return;
        try {
            Logger pluginLogger = Logger.getLogger("BlockProt");
            mirrorHandler = new Handler() {
                @Override
                public void publish(LogRecord record) {
                    if (record == null || isReentrancyGuard) return;
                    isReentrancyGuard = true;
                    try {
                        String msg = record.getMessage();
                        if (msg != null && !msg.isBlank()) {
                            log("console", "[" + record.getLevel().getName() + "] " + msg);
                        }
                    } finally {
                        isReentrancyGuard = false;
                    }
                }

                @Override public void flush() {}
                @Override public void close() throws SecurityException {}
            };
            pluginLogger.addHandler(mirrorHandler);
        } catch (Exception ignored) {}
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
        if (mirrorHandler != null) {
            try { Logger.getLogger("BlockProt").removeHandler(mirrorHandler); } catch (Exception ignored) {}
            mirrorHandler = null;
        }
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
        BlockProt plugin = BlockProt.getInstance();
        if (plugin == null) return;
        isReentrancyGuard = true;
        try {
            plugin.getLogger().warning(COLOR_STRIP.matcher(message).replaceAll(""));
        } finally {
            isReentrancyGuard = false;
        }
    }

    public static void separator() {
        log("----------------------------------------");
    }

    @Nullable
    public static File getCurrentLogFile() {
        return currentLogFile;
    }
}