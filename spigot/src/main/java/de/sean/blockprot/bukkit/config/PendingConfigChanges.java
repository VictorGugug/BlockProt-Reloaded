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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Main-thread service managing uncommitted configuration edits requested via in-game dialogs.
 */
public final class PendingConfigChanges {

    public static final class PendingEntry {
        private final String key;
        private final String file;
        private final Object originalValue;
        private Object requestedValue;
        private final UUID requestingPlayer;
        private final long requestTime;

        public PendingEntry(@NotNull String key, @NotNull String file, @Nullable Object originalValue,
                            @Nullable Object requestedValue, @Nullable UUID requestingPlayer) {
            this.key = key;
            this.file = file;
            this.originalValue = originalValue;
            this.requestedValue = requestedValue;
            this.requestingPlayer = requestingPlayer;
            this.requestTime = System.currentTimeMillis();
        }

        @NotNull public String getKey() { return key; }
        @NotNull public String getFile() { return file; }
        @Nullable public Object getOriginalValue() { return originalValue; }
        @Nullable public Object getRequestedValue() { return requestedValue; }
        @Nullable public UUID getRequestingPlayer() { return requestingPlayer; }
        public long getRequestTime() { return requestTime; }

        public void setRequestedValue(@Nullable Object requestedValue) {
            this.requestedValue = requestedValue;
        }
    }

    private static final PendingConfigChanges INSTANCE = new PendingConfigChanges();
    private final Map<String, PendingEntry> pendingMap = new LinkedHashMap<>();

    private PendingConfigChanges() {}

    @NotNull
    public static PendingConfigChanges getInstance() {
        return INSTANCE;
    }

    private static String makeCompositeKey(@NotNull String file, @NotNull String key) {
        return file + ":" + key;
    }

    public synchronized void put(@NotNull String file, @NotNull String key, @Nullable Object originalValue,
                                 @Nullable Object requestedValue, @Nullable UUID requestingPlayer) {
        if (Objects.equals(originalValue, requestedValue)) {
            remove(file, key);
            return;
        }

        String composite = makeCompositeKey(file, key);
        PendingEntry existing = pendingMap.get(composite);
        if (existing != null) {
            if (Objects.equals(existing.getOriginalValue(), requestedValue)) {
                pendingMap.remove(composite);
            } else {
                existing.setRequestedValue(requestedValue);
            }
        } else {
            pendingMap.put(composite, new PendingEntry(key, file, originalValue, requestedValue, requestingPlayer));
        }
    }

    public synchronized void remove(@NotNull String file, @NotNull String key) {
        pendingMap.remove(makeCompositeKey(file, key));
    }

    public synchronized void clear() {
        pendingMap.clear();
    }

    public synchronized boolean hasPending() {
        return !pendingMap.isEmpty();
    }

    public synchronized int getPendingCount() {
        return pendingMap.size();
    }

    @Nullable
    public synchronized Object getEffectiveValue(@NotNull String file, @NotNull String key, @Nullable Object actualValue) {
        PendingEntry entry = pendingMap.get(makeCompositeKey(file, key));
        if (entry != null) {
            return entry.getRequestedValue();
        }
        return actualValue;
    }

    public synchronized boolean getEffectiveBoolean(@NotNull String file, @NotNull String key, boolean actualValue) {
        Object val = getEffectiveValue(file, key, actualValue);
        if (val instanceof Boolean b) return b;
        if (val != null) return Boolean.parseBoolean(val.toString());
        return actualValue;
    }

    public synchronized int getEffectiveInt(@NotNull String file, @NotNull String key, int actualValue) {
        Object val = getEffectiveValue(file, key, actualValue);
        if (val instanceof Number n) return n.intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (NumberFormatException ignored) {}
        }
        return actualValue;
    }

    public synchronized double getEffectiveDouble(@NotNull String file, @NotNull String key, double actualValue) {
        Object val = getEffectiveValue(file, key, actualValue);
        if (val instanceof Number n) return n.doubleValue();
        if (val != null) {
            try { return Double.parseDouble(val.toString()); } catch (NumberFormatException ignored) {}
        }
        return actualValue;
    }

    @NotNull
    public synchronized List<PendingEntry> getSortedSnapshot() {
        List<PendingEntry> list = new ArrayList<>(pendingMap.values());
        list.sort(Comparator.comparing(PendingEntry::getFile).thenComparing(PendingEntry::getKey));
        return list;
    }
}
