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

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Snapshot comparison report for configuration reloads.
 */
public final class ReloadReport {

    public enum ReloadSource {
        AUTOMATIC("automatic"),
        MANUAL_DIALOG("manual-dialog"),
        MANUAL_COMMAND("manual-command"),
        MANUAL_FORCE("manual-force"),
        MANUAL_INVENTORY("manual-inventory"),
        EXTERNAL_FILE("external-file");

        private final String tag;

        ReloadSource(String tag) {
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }
    }

    public static final class ChangeDiff {
        private final String file;
        private final String key;
        private final Object oldValue;
        private final Object newValue;

        public ChangeDiff(@NotNull String file, @NotNull String key, @Nullable Object oldValue, @Nullable Object newValue) {
            this.file = file;
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @NotNull public String getFile() { return file; }
        @NotNull public String getKey() { return key; }
        @Nullable public Object getOldValue() { return oldValue; }
        @Nullable public Object getNewValue() { return newValue; }

        @Override
        public String toString() {
            return "[" + file + "] " + key + ": " + oldValue + " -> " + newValue;
        }
    }

    private final ReloadSource source;
    private final List<ChangeDiff> diffs;
    private final boolean success;
    private final String errorMessage;

    public ReloadReport(@NotNull ReloadSource source, @NotNull List<ChangeDiff> diffs, boolean success, @Nullable String errorMessage) {
        this.source = source;
        this.diffs = Collections.unmodifiableList(diffs);
        this.success = success;
        this.errorMessage = errorMessage;
    }

    @NotNull public ReloadSource getSource() { return source; }
    @NotNull public List<ChangeDiff> getDiffs() { return diffs; }
    public boolean isSuccess() { return success; }
    @Nullable public String getErrorMessage() { return errorMessage; }

    @NotNull
    public static Map<String, Object> captureSnapshot(@NotNull Map<String, Object> map, @NotNull String file) {
        Map<String, Object> result = new TreeMap<>();
        flattenMap("", map, file, result);
        return result;
    }

    private static void flattenMap(String prefix, Map<?, ?> sourceMap, String file, Map<String, Object> out) {
        for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey().toString() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nestedMap) {
                flattenMap(key, nestedMap, file, out);
            } else if (!(value instanceof ConfigurationSection)) {
                out.put(file + ":" + key, value);
            }
        }
    }

    @NotNull
    public static List<ChangeDiff> compareSnapshots(@NotNull Map<String, Object> before, @NotNull Map<String, Object> after) {
        List<ChangeDiff> diffs = new ArrayList<>();
        Set<String> allKeys = new TreeSet<>();
        allKeys.addAll(before.keySet());
        allKeys.addAll(after.keySet());

        for (String composite : allKeys) {
            Object oldVal = before.get(composite);
            Object newVal = after.get(composite);
            if (!Objects.equals(oldVal, newVal)) {
                int colonIdx = composite.indexOf(':');
                String file = colonIdx > 0 ? composite.substring(0, colonIdx) : "config.yml";
                String key = colonIdx > 0 ? composite.substring(colonIdx + 1) : composite;
                diffs.add(new ChangeDiff(file, key, oldVal, newVal));
            }
        }
        return diffs;
    }
}
