package de.sean.blockprot.bukkit.tasks;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Watches the plugin data directory and reloads BlockProt after relevant YAML
 * files change. Reloads are debounced to avoid duplicate reloads while an
 * editor is still writing the file.
 *
 * <h3>Self-write suppression</h3>
 * {@code reloadConfigAndTranslations()} writes several YAML files to disk
 * (merge, clean, lang). Without suppression those writes trigger new
 * {@code ENTRY_MODIFY} events that cause another reload — infinitely.
 *
 * Fix: before any plugin-initiated write, call {@link #suppressNext()} which
 * records the current timestamp. The watcher ignores any event that arrives
 * within {@link #SUPPRESS_WINDOW_MS} of that timestamp.
 * An external editor change takes at least a second to reach the JVM, so
 * a 3-second suppression window is safe.
 */
public final class ConfigFileWatcher implements Runnable {

    private static final long DEBOUNCE_MS      = 2_000;
    /** How long (ms) after a plugin-initiated write to ignore ENTRY_MODIFY events. */
    private static final long SUPPRESS_WINDOW_MS = 3_000;

    private final BlockProt    plugin;
    private final File         watchDir;
    private final AtomicLong   lastEventTime      = new AtomicLong(0);
    private final AtomicLong   lastSuppressTime   = new AtomicLong(0);
    private final AtomicBoolean reloadScheduled   = new AtomicBoolean(false);

    private WatchService watchService;

    public ConfigFileWatcher(@NotNull BlockProt plugin) {
        this.plugin   = plugin;
        this.watchDir = plugin.getDataFolder();
    }

    /**
     * Call this immediately before the plugin writes any config file to disk.
     * Suppresses watch events for {@link #SUPPRESS_WINDOW_MS} milliseconds.
     */
    public void suppressNext() {
        lastSuppressTime.set(System.currentTimeMillis());
    }

    public void start() {
        Thread thread = new Thread(this, "BlockProt-FileWatcher");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        try {
            if (watchService != null) watchService.close();
        } catch (Exception ignored) {}
    }

    @Override
    public void run() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path dir = watchDir.toPath();
            dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            File langDir = new File(watchDir, "lang");
            if (langDir.exists() && langDir.isDirectory()) {
                langDir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            }

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take();

                boolean relevant = false;
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path changed = (Path) event.context();
                    String name  = changed.getFileName().toString();

                    if (name.equals("config.yml") || name.equals("worlds.yml") || name.endsWith(".yml")) {
                        // Ignore events that arrived within the suppress window —
                        // these were written by the plugin itself during a reload.
                        long now = System.currentTimeMillis();
                        if (now - lastSuppressTime.get() < SUPPRESS_WINDOW_MS) continue;
                        relevant = true;
                    }
                }

                if (relevant) {
                    lastEventTime.set(System.currentTimeMillis());
                    scheduleReload();
                }

                if (!key.reset()) break;
            }
        } catch (InterruptedException | ClosedWatchServiceException ignored) {
            // Plugin shutdown — exit cleanly.
        } catch (Exception e) {
            plugin.getLogger().warning(Translator.get(TranslationKey.CONSOLE__FILEWATCHER_ERROR)
                .replace("{error}", e.getMessage()));
        }
    }

    private void scheduleReload() {
        if (!reloadScheduled.compareAndSet(false, true)) return;

        BlockProt.getFoliaLib().getScheduler().runLaterAsync(() -> {
            long timeSinceLastEvent = System.currentTimeMillis() - lastEventTime.get();
            if (timeSinceLastEvent >= DEBOUNCE_MS - 100) {
                plugin.getLogger().info(Translator.get(TranslationKey.CONSOLE__CONFIG_CHANGE_DETECTED));
                new BackupTask(plugin.getDataFolder(), true).run();
                BlockProt.getFoliaLib().getScheduler().runNextTick(t -> {
                    plugin.reloadConfigAndTranslations();
                    plugin.getLogger().info(Translator.get(TranslationKey.CONSOLE__CONFIG_RELOADED));
                });
            }
            reloadScheduled.set(false);
        }, (DEBOUNCE_MS / 50) + 5L);
    }
}
