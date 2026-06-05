/*
 * Copyright (C) 2021 - 2025 spnda
 * Modifications Copyright (C) 2025 Zaynr (Zar)
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

import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Console helper that routes BlockProt messages through the Bukkit console sender.
 *
 * <p>During startup, messages are buffered via {@link #startupBuffer} and printed
 * together inside the ASCII chest banner at the end of {@code onEnable}.
 * After startup, all methods print immediately as plain text.
 */
public final class BlockProtConsole {

    /**
     * When non-null, startup messages are collected here instead of being
     * printed immediately. Flushed and cleared by {@link #printStartupBanner}.
     */
    @Nullable
    private static List<String> startupBuffer = null;

    /**
     * Plugin logger, set by {@link #beginStartup(java.util.logging.Logger)}.
     * Used to print each banner line so every line gets the standard
     * {@code [HH:MM:SS INFO]: [BlockProt] } prefix, exactly like SkinsRestorer.
     */
    @Nullable
    private static Logger pluginLogger = null;

    private BlockProtConsole() {}

    // -------------------------------------------------------------------------
    // Startup buffer control
    // -------------------------------------------------------------------------

    /** Activates startup buffering. Call at the very start of {@code onEnable}. */
    public static void beginStartup(@NotNull Logger logger) {
        pluginLogger = logger;
        startupBuffer = new ArrayList<>();
    }

    /**
     * Prints a single success line to console and flushes the startup buffer
     * to the session log only. Call at the end of {@code onEnable}.
     */
    public static void printStartupBanner(@NotNull String version) {
        List<String> lines = startupBuffer != null ? startupBuffer : new ArrayList<>();
        startupBuffer = null;

        log(Translator.get(TranslationKey.CONSOLE__STARTUP_COMPLETE)
            .replace("{version}", version));

        for (String line : lines) {
            BlockProtLogger.log("startup", line);
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Informational line — buffered during startup, immediate otherwise. */
    public static void info(@NotNull String message) {
        emit(message);
    }

    /** Success line — buffered during startup, immediate otherwise. */
    public static void success(@NotNull String message) {
        emit(message);
    }

    /**
     * Warning line — always printed immediately via the plugin logger (never buffered).
     * Uses {@code logger.warning()} so the output carries the correct
     * {@code [HH:MM:SS WARN]} timestamp, consistent with info/success lines.
     */
    public static void warn(@NotNull String message) {
        if (pluginLogger != null) {
            pluginLogger.warning(message);
        } else {
            Bukkit.getConsoleSender().sendMessage(Translator.get(TranslationKey.MESSAGES__CONSOLE_WARN_PREFIX) + message);
        }
    }

    /**
     * Logs an integration-enabled confirmation — buffered during startup,
     * immediate otherwise. Prints a single line after the integration is
     * confirmed active; do not also call {@link #integration(String)} for
     * the same plugin to avoid duplicate console output.
     *
     * @param integrationName The plugin id (e.g. "claimchunk").
     */
    public static void integrationEnabled(@NotNull String integrationName) {
        emit(Translator.get(TranslationKey.CONSOLE__INTEGRATION_ENABLED)
            .replace("{name}", integrationName));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Sends one line via the plugin logger (INFO level). */
    private static void log(@NotNull String line) {
        if (pluginLogger != null) {
            pluginLogger.info(line);
        } else {
            Bukkit.getConsoleSender().sendMessage(line);
        }
    }

    /**
     * Buffers the message during startup; prints it immediately (via plugin logger)
     * after startup completes.
     */
    private static void emit(@NotNull String message) {
        if (startupBuffer != null) {
            startupBuffer.add(message);
        } else {
            log(message);
        }
    }
}
