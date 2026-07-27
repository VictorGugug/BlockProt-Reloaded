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

package de.sean.blockprot.bukkit.logger;

import de.sean.blockprot.bukkit.BlockProtLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Activity log facade for recording plugin actions in a categorized form.
 */
public final class PluginActivityLog {

    private PluginActivityLog() {}

    public static void logReload(@NotNull String source, @Nullable UUID actor, boolean success, int changeCount, @Nullable String error) {
        String actorStr = actor != null ? actor.toString() : "SERVER";
        if (success) {
            BlockProtLogger.log("activity", "RELOAD | source=" + source + " | actor=" + actorStr + " | status=SUCCESS | changes=" + changeCount);
        } else {
            BlockProtLogger.log("activity", "RELOAD | source=" + source + " | actor=" + actorStr + " | status=FAILED | error=" + (error != null ? error : "unknown"));
        }
    }

    public static void logCommand(@NotNull String command, @NotNull String actor, boolean success, @Nullable String reason) {
        BlockProtLogger.log("activity", "COMMAND | cmd=" + command + " | actor=" + actor + " | outcome=" + (success ? "SUCCESS" : "DENIED") + (reason != null ? " | reason=" + reason : ""));
    }

    public static void logSettingRequest(@NotNull String file, @NotNull String key, @Nullable Object oldValue, @Nullable Object newValue, @Nullable UUID actor) {
        String actorStr = actor != null ? actor.toString() : "CONSOLE";
        BlockProtLogger.log("activity", "SETTING_REQUEST | file=" + file + " | key=" + key + " | old=" + oldValue + " | new=" + newValue + " | actor=" + actorStr);
    }

    public static void logLanguageToggle(@NotNull String code, boolean enabled, @Nullable UUID actor) {
        String actorStr = actor != null ? actor.toString() : "CONSOLE";
        BlockProtLogger.log("activity", "LANG_TOGGLE | lang=" + code + " | state=" + (enabled ? "ENABLED" : "DISABLED") + " | actor=" + actorStr);
    }

    public static void logProtectionChange(@NotNull String action, @NotNull String target, @NotNull String owner, @Nullable UUID actor) {
        String actorStr = actor != null ? actor.toString() : "SYSTEM";
        BlockProtLogger.log("activity", "PROTECTION | action=" + action + " | target=" + target + " | owner=" + owner + " | actor=" + actorStr);
    }
}
