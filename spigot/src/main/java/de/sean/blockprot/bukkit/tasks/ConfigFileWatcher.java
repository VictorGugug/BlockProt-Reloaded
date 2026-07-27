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

package de.sean.blockprot.bukkit.tasks;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.config.ReloadCoordinator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generation-based quiet-period scheduler for external file modifications.
 */
public final class ConfigFileWatcher implements Runnable {

    private final BlockProt plugin;
    private final File watchDir;
    private final AtomicLong currentGeneration = new AtomicLong(0);
    private final AtomicLong lastEventTimestamp = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> suppressedPaths = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private WatchService watchService;

    public ConfigFileWatcher(@NotNull BlockProt plugin) {
        this.plugin = plugin;
        this.watchDir = plugin.getDataFolder();
    }

    public void suppressNext() {
        suppressPath("config.yml");
        suppressPath("lang/lang.yml");
        suppressPath("blocks.yml");
        suppressPath("worlds.yml");
    }

    public void suppressPath(@NotNull String relativePath) {
        suppressedPaths.computeIfAbsent(relativePath, k -> new AtomicLong(0)).incrementAndGet();
    }

    public boolean isRunning() {
        return running.get();
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            Thread thread = new Thread(this, "BlockProt-FileWatcher");
            thread.setDaemon(true);
            thread.start();
        }
    }

    public void stop() {
        running.set(false);
        try {
            if (watchService != null) watchService.close();
        } catch (Exception ignored) {}
    }

    @Override
    public void run() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path dir = watchDir.toPath();
            dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

            File langDir = new File(watchDir, "lang");
            if (langDir.exists() && langDir.isDirectory()) {
                langDir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
            }

            while (!Thread.currentThread().isInterrupted() && running.get()) {
                WatchKey key = watchService.take();

                boolean relevant = false;
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path changed = (Path) event.context();
                    String name = changed.getFileName().toString();

                    if (name.endsWith(".yml")) {
                        String relPath = name;
                        AtomicLong token = suppressedPaths.get(relPath);
                        if (token != null && token.get() > 0) {
                            token.decrementAndGet();
                            continue;
                        }
                        relevant = true;
                    }
                }

                if (relevant) {
                    long gen = currentGeneration.incrementAndGet();
                    lastEventTimestamp.set(System.currentTimeMillis());
                    scheduleQuietPeriod(gen);
                }

                if (!key.reset()) break;
            }
        } catch (InterruptedException | ClosedWatchServiceException ignored) {
        } catch (Exception e) {
            plugin.getLogger().warning(Translator.get(TranslationKey.CONSOLE__FILEWATCHER_ERROR)
                .replace("{error}", e.getMessage()));
        } finally {
            running.set(false);
        }
    }

    private void scheduleQuietPeriod(long targetGen) {
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        int delaySec = cfg != null ? cfg.getAutoReloadDelaySeconds() : 0;
        long delayMs = (long) delaySec * 1000L;

        BlockProt.getFoliaLib().getScheduler().runLaterAsync(() -> {
            if (currentGeneration.get() != targetGen) {
                return;
            }

            long elapsed = System.currentTimeMillis() - lastEventTimestamp.get();
            if (elapsed < delayMs) {
                scheduleQuietPeriod(targetGen);
                return;
            }

            BlockProt.getFoliaLib().getScheduler().runNextTick(t -> {
                if (currentGeneration.get() == targetGen) {
                    if (BlockProt.getDefaultConfig().isAutoReloadEnabled()) {
                        BlockProtLogger.log("filewatch", "Quiet period elapsed. Executing automatic reload...");
                        ReloadCoordinator.commitExternal();
                    } else {
                        BlockProtLogger.log("filewatch", "External changes detected while auto-reload is disabled; queued for manual reload.");
                    }
                }
            });
        }, Math.max(1L, (delayMs / 50L)));
    }
}